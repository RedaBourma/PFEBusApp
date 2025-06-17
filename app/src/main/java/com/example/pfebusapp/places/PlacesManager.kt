package com.example.pfebusapp.places

import android.content.Context
import android.util.Log
import com.example.pfebusapp.BuildConfig
import com.example.pfebusapp.busRepository.RouteLocationsRepository
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PlacesManager(private val context: Context) {
    private val TAG = "PlacesManager"
    private val placesClient: PlacesClient by lazy {
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.MAPS_API_KEY)
        }
        Places.createClient(context)
    }

    // Initialize the route locations repository
    private val routeLocationsRepository = RouteLocationsRepository()

    // Expanded Guelmim bounds to include nearby villages (approximately 70km radius for better coverage)
    private val guelmimRegionBounds = LatLngBounds(
        LatLng(28.5865, -10.4572), // Southwest corner - further expanded
        LatLng(29.3865, -9.6572)   // Northeast corner - further expanded
    )

    // Our custom location suggestion class that doesn't extend AutocompletePrediction
    data class LocationSuggestion(
        val id: String,
        val name: String,
        val primaryText: String,
        val secondaryText: String,
        val coordinates: LatLng,
        val type: String = "", // Added type field to help categorize results
        val distance: Double = 0.0 // Distance from user's current location if available
    )

    // Combined search function that returns standard AutocompletePrediction from API
    suspend fun searchPlaces(query: String): List<AutocompletePrediction> = withContext(Dispatchers.IO) {
        if (query.isEmpty()) return@withContext emptyList()
        
        // If the query doesn't explicitly include "guelmim" and it's not a short common word,
        // try to enhance it to prioritize local results
        val enhancedQuery = if (!query.contains("guelmim", ignoreCase = true) && 
                               query.length > 3 && 
                               !listOf("bus", "taxi", "stop", "gare").contains(query.lowercase())) {
            "$query guelmim"
        } else {
            query
        }
        
        val results = mutableListOf<AutocompletePrediction>()
        
        try {
            Log.d(TAG, "Searching for places with query: '$enhancedQuery'")
            
            // Try first search with ESTABLISHMENT type filter (for schools, businesses, etc.)
            val establishmentRequest = FindAutocompletePredictionsRequest.builder()
                .setQuery(enhancedQuery)
                .setLocationBias(RectangularBounds.newInstance(guelmimRegionBounds))
                .setCountries("MA") // Restrict to Morocco
                .setTypeFilter(TypeFilter.ESTABLISHMENT) // Search for establishments first
                .build()
            
            val establishmentResponse = placesClient.findAutocompletePredictions(establishmentRequest).await()
            Log.d(TAG, "Found ${establishmentResponse.autocompletePredictions.size} establishment results")
            results.addAll(establishmentResponse.autocompletePredictions)
            
            // Try with address type filter
            val addressRequest = FindAutocompletePredictionsRequest.builder()
                .setQuery(enhancedQuery)
                .setLocationBias(RectangularBounds.newInstance(guelmimRegionBounds))
                .setCountries("MA")
                .setTypeFilter(TypeFilter.ADDRESS) // For specific addresses
                .build()
            
            val addressResponse = placesClient.findAutocompletePredictions(addressRequest).await()
            Log.d(TAG, "Found ${addressResponse.autocompletePredictions.size} address results")
            
            // Add unique results that aren't already in our list
            val existingPlaceIds = results.map { it.placeId }.toSet()
            results.addAll(addressResponse.autocompletePredictions.filter { 
                it.placeId !in existingPlaceIds 
            })
            
            // Only if we still have few results, also try geocode search
            if (results.size < 3) {
                val geocodeRequest = FindAutocompletePredictionsRequest.builder()
                    .setQuery(enhancedQuery)
                    .setLocationBias(RectangularBounds.newInstance(guelmimRegionBounds))
                    .setCountries("MA")
                    .setTypeFilter(TypeFilter.GEOCODE) // For addresses, cities, etc.
                    .build()
                
                val geocodeResponse = placesClient.findAutocompletePredictions(geocodeRequest).await()
                Log.d(TAG, "Found ${geocodeResponse.autocompletePredictions.size} geocode results")
                
                val updatedPlaceIds = results.map { it.placeId }.toSet()
                results.addAll(geocodeResponse.autocompletePredictions.filter { 
                    it.placeId !in updatedPlaceIds 
                })
            }
            
            // If we found results, return them
            if (results.isNotEmpty()) {
                Log.d(TAG, "API returned ${results.size} combined results")
                return@withContext results
            }
            
            // Last try with original query if enhanced query didn't work
            if (enhancedQuery != query) {
                val fallbackRequest = FindAutocompletePredictionsRequest.builder()
                    .setQuery(query)
                    .setLocationBias(RectangularBounds.newInstance(guelmimRegionBounds))
                    .setCountries("MA")
                    .build()
                
                val fallbackResponse = placesClient.findAutocompletePredictions(fallbackRequest).await()
                if (fallbackResponse.autocompletePredictions.isNotEmpty()) {
                    Log.d(TAG, "Fallback API search returned ${fallbackResponse.autocompletePredictions.size} results")
                    return@withContext fallbackResponse.autocompletePredictions
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching places via API: ${e.message}", e)
        }
        
        // Return empty list if API fails - we use fallback in the ViewModel
        return@withContext emptyList()
    }
    
    // Search local places using our repository instead of hardcoded values
    suspend fun searchLocalPlaces(query: String): List<LocationSuggestion> = withContext(Dispatchers.IO) {
        if (query.isEmpty()) return@withContext emptyList()
        
        Log.d(TAG, "Searching local places with query: '$query'")
        val results = mutableListOf<LocationSuggestion>()
        
        try {
            // First try exact matches
            val exactMatchLocations = routeLocationsRepository.searchLocationsByName(query)
            
            if (exactMatchLocations.isNotEmpty()) {
                Log.d(TAG, "Found ${exactMatchLocations.size} exact match locations")
                results.addAll(convertToSuggestions(exactMatchLocations))
            }
            
            // Check for school/educational institution searches
            val schoolKeywords = listOf("école", "ecole", "school", "lycée", "lycee", "collège", 
                                       "college", "est", "ensa", "université", "universite", 
                                       "formation", "centre")
            
            val isSchoolSearch = schoolKeywords.any { 
                query.contains(it, ignoreCase = true) || query.split(" ").any { word -> word.equals(it, ignoreCase = true) } 
            }
            
            // For school searches, add educational institutions if we don't have enough results
            if (isSchoolSearch && results.size < 2) {
                Log.d(TAG, "Adding educational institutions for school search")
                val educationalLocations = routeLocationsRepository.getEducationalInstitutions()
                
                // Filter for more relevance if needed
                val filteredEducational = if (query.length > 3) {
                    educationalLocations.filter { loc -> 
                        loc.name.contains(query, ignoreCase = true) ||
                        query.split(" ").any { word -> 
                            word.length > 3 && loc.name.contains(word, ignoreCase = true)
                        }
                    }
                } else {
                    educationalLocations
                }
                
                // Add these if we found any matching, otherwise add all educational institutions
                if (filteredEducational.isNotEmpty()) {
                    Log.d(TAG, "Adding ${filteredEducational.size} filtered educational locations")
                    
                    // Add only those not already in results
                    val existingIds = results.map { it.id.removePrefix("local_") }.toSet()
                    val newLocations = filteredEducational.filter { it.id !in existingIds }
                    
                    results.addAll(convertToSuggestions(newLocations))
                } else if (results.isEmpty()) {
                    Log.d(TAG, "Adding all ${educationalLocations.size} educational locations")
                    results.addAll(convertToSuggestions(educationalLocations))
                }
            }
            
            // For very short queries, add all locations of specific types that contain the query
            if (query.length <= 3 && results.size < 5) {
                Log.d(TAG, "Adding locations for short query")
                
                // Get common location types
                val commonLocationTypes = listOf("bus_stop", "poi", "school", "university")
                val typedLocations = commonLocationTypes.flatMap { type ->
                    routeLocationsRepository.getLocationsOfType(type)
                }.filter { location ->
                    location.name.contains(query, ignoreCase = true) || 
                    location.city.contains(query, ignoreCase = true)
                }.take(10)
                
                if (typedLocations.isNotEmpty()) {
                    Log.d(TAG, "Adding ${typedLocations.size} typed locations")
                    
                    // Add only those not already in results
                    val existingIds = results.map { it.id.removePrefix("local_") }.toSet()
                    val newLocations = typedLocations.filter { it.id !in existingIds }
                    
                    results.addAll(convertToSuggestions(newLocations))
                }
            }
            
            // If we still have no results, try word matching
            if (results.isEmpty() && query.length > 3) {
                Log.d(TAG, "Trying word-based matching")
                val allLocations = routeLocationsRepository.getAllLocations()
                
                val words = query.lowercase().split(" ").filter { it.length > 3 }
                if (words.isNotEmpty()) {
                    val wordMatches = allLocations.filter { location ->
                        words.any { word ->
                            location.name.lowercase().contains(word) || 
                            location.city.lowercase().contains(word)
                        }
                    }
                    
                    if (wordMatches.isNotEmpty()) {
                        Log.d(TAG, "Found ${wordMatches.size} word matches")
                        results.addAll(convertToSuggestions(wordMatches))
                    }
                }
            }
            
            // If we still have no results, add default locations
            if (results.isEmpty()) {
                Log.d(TAG, "Using default locations as no matching results found")
                results.addAll(convertToSuggestions(routeLocationsRepository.getDefaultLocations()))
            }
            
            return@withContext results
            
        } catch (e: Exception) {
            Log.e(TAG, "Error searching locations from repository: ${e.message}", e)
            
            // If repository search fails, use default locations
            Log.d(TAG, "Using default locations due to error")
            return@withContext convertToSuggestions(routeLocationsRepository.getDefaultLocations())
        }
    }
    
    // Helper to convert Location objects to LocationSuggestion objects
    private fun convertToSuggestions(locations: List<RouteLocationsRepository.Location>): List<LocationSuggestion> {
        return locations.map { location ->
            val parts = location.name.split(",", limit = 2)
            val primary = parts[0].trim()
            val secondary = if (parts.size > 1) parts[1].trim() else location.city
            
            LocationSuggestion(
                id = "local_${location.id}",
                name = location.name,
                primaryText = primary,
                secondaryText = secondary,
                coordinates = location.toLatLng(),
                type = location.type
            )
        }
    }

    suspend fun getPlaceDetails(placeId: String): LatLng? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Getting place details for ID: $placeId")
            
            // Check if this is one of our local locations
            if (placeId.startsWith("local_")) {
                val locationId = placeId.substringAfter("local_")
                
                // First check repository locations
                val locations = routeLocationsRepository.getAllLocations()
                val matchingLocation = locations.find { it.id == locationId }
                
                if (matchingLocation != null) {
                    val coordinates = matchingLocation.toLatLng()
                    Log.d(TAG, "Found repository location: ${matchingLocation.name} at $coordinates")
                    return@withContext coordinates
                }
                
                // Then check default locations
                val defaultLocation = routeLocationsRepository.getDefaultLocations().find { it.id == locationId }
                if (defaultLocation != null) {
                    val coordinates = defaultLocation.toLatLng()
                    Log.d(TAG, "Found default location: ${defaultLocation.name} at $coordinates")
                    return@withContext coordinates
                }
                
                Log.w(TAG, "Could not find location with ID: $locationId")
                return@withContext null
            }
            
            // Otherwise use the Places API
            val placeFields = listOf(
                Place.Field.LAT_LNG, 
                Place.Field.NAME, 
                Place.Field.ADDRESS,
                Place.Field.TYPES
            )
            
            val placeRequest = FetchPlaceRequest.builder(placeId, placeFields).build()
            val placeResponse = placesClient.fetchPlace(placeRequest).await()
            
            val place = placeResponse.place
            val coordinates = place.latLng
            if (coordinates != null) {
                Log.d(TAG, "Got coordinates from API: $coordinates for ${place.name}")
                
                // Optionally save this place to our repository for future use
                try {
                    val placeName = place.name ?: "Unknown Place"
                    val placeAddress = place.address ?: ""
                    
                    // Generate a unique ID for this place
                    val uniqueId = "api_${UUID.randomUUID().toString().substring(0, 8)}"
                    
                    // Determine the type based on Place types
                    val placeType = determineTypeFromPlaceTypes(place)
                    
                    // Extract city from address
                    val addressParts = placeAddress.split(",")
                    val city = if (addressParts.size >= 2) {
                        addressParts[addressParts.size - 2].trim()
                    } else {
                        "Guelmim" // Default to Guelmim if we can't extract city
                    }
                    
                    val newLocation = RouteLocationsRepository.Location(
                        id = uniqueId,
                        name = placeName,
                        type = placeType,
                        latitude = coordinates.latitude,
                        longitude = coordinates.longitude,
                        city = city,
                        region = "Guelmim-Oued Noun" // Default region
                    )
                    
                    // Save in background
                    routeLocationsRepository.addLocation(newLocation)
                    Log.d(TAG, "Saved new location to repository: $placeName")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving place to repository: ${e.message}")
                    // Continue even if saving fails
                }
                
                return@withContext coordinates
            }
            
            Log.w(TAG, "Place has no coordinates: ${place.name}")
            return@withContext null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching place details: ${e.message}", e)
            return@withContext null
        }
    }
    
    // Helper function to determine a place type from Google Place types
    private fun determineTypeFromPlaceTypes(place: Place): String {
        val types = place.types ?: return "poi" // Default to point of interest
        
        return when {
            types.contains(Place.Type.SCHOOL) -> "school"
            types.contains(Place.Type.UNIVERSITY) -> "university"
            types.contains(Place.Type.BUS_STATION) -> "bus_stop"
            types.contains(Place.Type.TRANSIT_STATION) -> "bus_stop"
            types.contains(Place.Type.POINT_OF_INTEREST) -> "poi"
            types.contains(Place.Type.ESTABLISHMENT) -> "establishment"
            types.contains(Place.Type.LOCALITY) -> "city"
            types.contains(Place.Type.SUBLOCALITY) -> "district"
            types.contains(Place.Type.NEIGHBORHOOD) -> "neighborhood"
            else -> "poi" // Default
        }
    }
} 