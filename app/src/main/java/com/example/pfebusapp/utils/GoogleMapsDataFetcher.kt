package com.example.pfebusapp.utils

import android.content.Context
import android.util.Log
import com.example.pfebusapp.BuildConfig
import com.example.pfebusapp.busRepository.RouteLocationsRepository
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceTypes
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Utility to fetch location data from Google Maps APIs
 */
class GoogleMapsDataFetcher(private val context: Context) {
    private val TAG = "GoogleMapsDataFetcher"
    
    // Initialize Places client
    private val placesClient: PlacesClient by lazy {
        if (!Places.isInitialized()) {
            Places.initialize(context, BuildConfig.MAPS_API_KEY)
        }
        Places.createClient(context)
    }
    
    // Expanded Guelmim region bounds (approximately 50km radius)
    private val guelmimRegionBounds = LatLngBounds(
        LatLng(28.6865, -10.3572), // Southwest corner - expanded
        LatLng(29.2865, -9.7572)   // Northeast corner - expanded
    )
    
    // Firestore reference
    private val db = FirebaseFirestore.getInstance()
    
    /**
     * Fetch places from the Google Places API for a specific region and save to Firebase
     */
    suspend fun fetchAndSavePlacesForRegion(onProgress: (current: Int, total: Int) -> Unit): List<RouteLocationsRepository.Location> {
        val locations = mutableListOf<RouteLocationsRepository.Location>()
        val token = AutocompleteSessionToken.newInstance()
        
        try {
            // Define place types to search for - expanded list with more types
            val placeTypes = listOf(
                "locality",          // Cities, towns
                "sublocality",       // Districts, neighborhoods
                "neighborhood",      // Smaller areas within a city
                "bus_station",       // Bus stations
                "transit_station",   // Transit stations (includes bus stops)
                "school",            // Schools
                "university",        // Universities
                "point_of_interest", // General points of interest
                "establishment"      // General establishments (includes schools, businesses)
            )
            
            var totalProcessed = 0
            val totalToProcess = placeTypes.size
            
            // Query each place type
            for (placeType in placeTypes) {
                try {
                    Log.d(TAG, "Searching for place type: $placeType")
                    
                    // Create request for this place type
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setCountries("MA") // Morocco only
                        .setLocationBias(RectangularBounds.newInstance(guelmimRegionBounds))
                        .setTypesFilter(listOf(placeType))
                        .setSessionToken(token)
                        .build()
                    
                    // Execute request
                    val response = placesClient.findAutocompletePredictions(request).await()
                    Log.d(TAG, "Found ${response.autocompletePredictions.size} predictions for $placeType")
                    
                    // Process each prediction
                    for (prediction in response.autocompletePredictions) {
                        try {
                            // Get details for this place
                            val place = getPlaceDetails(prediction)
                            if (place != null) {
                                // Convert to our location format
                                val location = convertPlaceToLocation(place, prediction, placeType)
                                locations.add(location)
                                
                                // Save to Firebase
                                saveLocationToFirebase(location)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting details for place: ${prediction.getPrimaryText(null)}", e)
                        }
                    }
                    
                    totalProcessed++
                    onProgress(totalProcessed, totalToProcess)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error searching for place type: $placeType", e)
                }
            }
            
            Log.d(TAG, "Completed fetching ${locations.size} locations")
            return locations
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching places", e)
            return emptyList()
        }
    }
    
    /**
     * Get detailed place information using its ID
     */
    private suspend fun getPlaceDetails(prediction: AutocompletePrediction): Place? {
        try {
            // Define the place fields to return
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.ADDRESS_COMPONENTS,
                Place.Field.TYPES
            )
            
            // Create the fetch request
            val request = FetchPlaceRequest.newInstance(prediction.placeId, placeFields)
            
            // Execute the request
            val response = placesClient.fetchPlace(request).await()
            return response.place
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching place details for ${prediction.getFullText(null)}", e)
            return null
        }
    }
    
    /**
     * Convert Place object to our Location format
     */
    private fun convertPlaceToLocation(
        place: Place,
        prediction: AutocompletePrediction,
        placeType: String
    ): RouteLocationsRepository.Location {
        // Extract city and region from address components if available
        val address = place.address ?: ""
        val addressParts = address.split(", ")
        
        // Determine city and region based on address structure
        val city = if (addressParts.size >= 2) addressParts[addressParts.size - 2] else "Guelmim" // Default to Guelmim if not specified
        val region = if (addressParts.isNotEmpty()) addressParts.last() else "Guelmim-Oued Noun" // Default to region if not specified
        
        // Map Google place type to our type system - simplified approach to avoid type issues
        val placeTypesString = place.types?.joinToString(",") ?: ""
        val mappedType = when {
            placeTypesString.contains("locality") -> "city"
            placeTypesString.contains("sublocality") -> "district"
            placeTypesString.contains("neighborhood") -> "neighborhood"
            placeTypesString.contains("bus_station") -> "bus_stop"
            placeTypesString.contains("transit_station") -> "bus_stop"
            placeTypesString.contains("school") -> "school"
            placeTypesString.contains("university") -> "university"
            placeTypesString.contains("establishment") -> "establishment"
            else -> placeType
        }
        
        return RouteLocationsRepository.Location(
            id = place.id ?: "",
            name = place.name ?: prediction.getPrimaryText(null).toString(),
            type = mappedType,
            latitude = place.latLng?.latitude ?: 0.0,
            longitude = place.latLng?.longitude ?: 0.0,
            city = city,
            region = region
        )
    }
    
    /**
     * Save a location to Firebase
     */
    private suspend fun saveLocationToFirebase(location: RouteLocationsRepository.Location) {
        try {
            // Convert location to map for Firestore
            val locationMap = mapOf(
                "name" to location.name,
                "type" to location.type,
                "latitude" to location.latitude,
                "longitude" to location.longitude,
                "city" to location.city,
                "region" to location.region
            )
            
            // Check if location already exists to avoid duplicates
            val existingDoc = db.collection("locations")
                .whereEqualTo("name", location.name)
                .whereEqualTo("latitude", location.latitude)
                .whereEqualTo("longitude", location.longitude)
                .limit(1)
                .get()
                .await()
            
            if (existingDoc.isEmpty) {
                // Add new location
                db.collection("locations")
                    .add(locationMap)
                    .await()
                Log.d(TAG, "Saved location: ${location.name}")
            } else {
                Log.d(TAG, "Location already exists: ${location.name}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error saving location to Firebase: ${location.name}", e)
        }
    }
    
    /**
     * Search for specific places by keyword in the region
     */
    suspend fun searchSpecificPlaces(keywords: List<String>, onProgress: (current: Int, total: Int) -> Unit): List<RouteLocationsRepository.Location> {
        val locations = mutableListOf<RouteLocationsRepository.Location>()
        val token = AutocompleteSessionToken.newInstance()
        
        try {
            var totalProcessed = 0
            val totalToProcess = keywords.size
            
            // Search for each keyword
            for (keyword in keywords) {
                try {
                    Log.d(TAG, "Searching for keyword: $keyword")
                    
                    // Create request for this keyword
                    val request = FindAutocompletePredictionsRequest.builder()
                        .setQuery(keyword)
                        .setCountries("MA") // Morocco only
                        .setLocationBias(RectangularBounds.newInstance(guelmimRegionBounds))
                        .setSessionToken(token)
                        .build()
                    
                    // Execute request
                    val response = placesClient.findAutocompletePredictions(request).await()
                    Log.d(TAG, "Found ${response.autocompletePredictions.size} predictions for '$keyword'")
                    
                    // Process each prediction
                    for (prediction in response.autocompletePredictions) {
                        try {
                            // Skip places outside our target area (check in prediction text)
                            if (!isInTargetArea(prediction.getFullText(null).toString())) {
                                continue
                            }
                            
                            // Get details for this place
                            val place = getPlaceDetails(prediction)
                            if (place != null) {
                                // Verify the place is within our bounds
                                if (place.latLng != null && isInBounds(place.latLng!!)) {
                                    // Convert to our location format
                                    val location = convertPlaceToLocation(place, prediction, "poi")
                                    locations.add(location)
                                    
                                    // Save to Firebase
                                    saveLocationToFirebase(location)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error getting details for place: ${prediction.getPrimaryText(null)}", e)
                        }
                    }
                    
                    totalProcessed++
                    onProgress(totalProcessed, totalToProcess)
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Error searching for keyword: $keyword", e)
                }
            }
            
            Log.d(TAG, "Completed fetching ${locations.size} specific locations")
            return locations
            
        } catch (e: Exception) {
            Log.e(TAG, "Error searching for specific places", e)
            return emptyList()
        }
    }
    
    /**
     * Check if a place name contains references to our target area
     */
    private fun isInTargetArea(fullText: String): Boolean {
        val targetAreas = listOf(
            "Guelmim", "Guelmin", "Guelmine", "Bouizakaren", "Bouzakaren", 
            "Tighmert", "Asrir", "Fask", "Taghjijt", "Assa-Zag", "Tan-Tan",
            "Oued Noun", "Sidi Ifni"
        )
        
        return targetAreas.any { area -> 
            fullText.contains(area, ignoreCase = true)
        }
    }
    
    /**
     * Check if coordinates are within our target bounds
     */
    private fun isInBounds(latLng: LatLng): Boolean {
        return guelmimRegionBounds.contains(latLng)
    }
    
    /**
     * Get common points of interest in Guelmim region
     */
    fun getCommonPOIKeywords(): List<String> {
        return listOf(
            // Transportation
            "gare routière guelmim", // Bus station
            "grand taxi guelmim",    // Taxi stations
            "arrêt bus guelmim",     // Bus stops
            "station service guelmim", // Gas stations
            
            // Education
            "école guelmim",         // Schools
            "école primaire guelmim", // Primary schools
            "collège guelmim",       // Middle schools
            "lycée guelmim",         // High schools
            "université guelmim",    // Universities
            "EST guelmim",           // EST School
            "ENSA guelmim",          // Engineering school
            "faculté guelmim",       // Faculty
            
            // Public services
            "hopital guelmim",       // Hospitals
            "centre de santé guelmim", // Health centers
            "pharmacie guelmim",     // Pharmacies
            "banque guelmim",        // Banks
            "administration guelmim", // Administration buildings
            "poste guelmim",         // Post offices
            "police guelmim",        // Police stations
            "commissariat guelmim",  // Police stations
            
            // Commerce
            "marché guelmim",        // Markets
            "souk guelmim",          // Souks (markets)
            "supermarché guelmim",   // Supermarkets
            "centre commercial guelmim", // Shopping centers
            
            // Food & Accommodation
            "restaurant guelmim",    // Restaurants
            "café guelmim",          // Cafes
            "hotel guelmim",         // Hotels
            
            // Leisure
            "parc guelmim",          // Parks
            "stade guelmim",         // Stadium
            "jardin guelmim",        // Gardens
            "terrain de sport guelmim", // Sports grounds
            
            // Specific landmarks
            "place hassan II guelmim", // Main square
            "centre ville guelmim"   // City center
        )
    }
} 