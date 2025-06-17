package com.example.pfebusapp.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pfebusapp.R
import com.example.pfebusapp.busRepository.Bus
import com.example.pfebusapp.busRepository.BusRepository
import com.example.pfebusapp.location.LocationManager
import com.example.pfebusapp.places.PlacesManager
import com.example.pfebusapp.routes.RouteFinder
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "BusRoutesViewModel"

// Wrapper class that can represent either an API place or our local place
data class PlaceItem(
    val id: String,
    val primaryText: String,
    val secondaryText: String,
    val fullText: String,
    val isFromApi: Boolean,
    val type: String = ""
) {
    companion object {
        // Convert from AutocompletePrediction to our PlaceItem
        fun fromAutocompletePrediction(prediction: AutocompletePrediction): PlaceItem {
            return PlaceItem(
                id = prediction.placeId,
                primaryText = prediction.getPrimaryText(null).toString(),
                secondaryText = prediction.getSecondaryText(null).toString(),
                fullText = prediction.getFullText(null).toString(),
                isFromApi = true
            )
        }
        
        // Convert from our LocationSuggestion to PlaceItem
        fun fromLocationSuggestion(suggestion: PlacesManager.LocationSuggestion): PlaceItem {
            return PlaceItem(
                id = suggestion.id,
                primaryText = suggestion.primaryText,
                secondaryText = suggestion.secondaryText,
                fullText = suggestion.name,
                isFromApi = false,
                type = suggestion.type
            )
        }
    }
}

data class BusRoutesState(
    val currentLocation: String = "",
    val currentLatLng: LatLng? = null,
    val currentLocationResults: List<PlaceItem> = emptyList(),
    val showCurrentLocationResults: Boolean = false,
    val destination: String = "",
    val destinationLatLng: LatLng? = null,
    val searchResults: List<PlaceItem> = emptyList(),
    val showSearchResults: Boolean = false,
    val buses: List<Bus>? = null,
    val suggestedRoutes: List<RouteFinder.Route>? = null,
    val isLoading: Boolean = false,
    val isLoadingCurrentLocationResults: Boolean = false,
    val isLoadingDestinationResults: Boolean = false,
    val isLoadingRoutes: Boolean = false,
    val error: String? = null
)

class BusRoutesViewModel(private val context: Context) : ViewModel() {
    private val locationManager = LocationManager(context)
    private val placesManager = PlacesManager(context)
    private val routeFinder = RouteFinder()
    private val busRepository = BusRepository()

    private val _state = MutableStateFlow(BusRoutesState())
    val state: StateFlow<BusRoutesState> = _state.asStateFlow()

    init {
        loadBuses()
    }

    private fun loadBuses() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true) }
                Log.d(TAG, "Loading buses...")
                
                withContext(Dispatchers.IO) {
                    busRepository.getAllBusData(
                        onSuccess = { fetchedBuses ->
                            Log.d(TAG, "Loaded ${fetchedBuses.size} buses")
                            _state.update { 
                                it.copy(
                                    buses = fetchedBuses,
                                    isLoading = false
                                )
                            }
                        },
                        onFailure = { exception ->
                            Log.e(TAG, "Failed to load buses: ${exception.message}", exception)
                            _state.update { 
                                it.copy(
                                    error = context.getString(R.string.error_bus_data),
                                    isLoading = false
                                )
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading buses: ${e.message}", e)
                _state.update { 
                    it.copy(
                        error = context.getString(R.string.error_bus_data),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                Log.d(TAG, "Getting current location...")
                val location = locationManager.getCurrentLocation()
                location?.let { latLng ->
                    val address = locationManager.getAddressFromLocation(latLng)
                    Log.d(TAG, "Got current location: $address, $latLng")
                    _state.update { 
                        it.copy(
                            currentLatLng = latLng,
                            currentLocation = address,
                            isLoading = false
                        )
                    }
                    // If we have both locations, find routes
                    _state.value.destinationLatLng?.let { dest ->
                        findRoutes(latLng, dest)
                    }
                } ?: run {
                    Log.w(TAG, "Current location is null - device location unavailable")
                    // Show a fallback location in Guelmim as example
                    val fallbackLatLng = LatLng(28.9865, -10.0572)
                    _state.update { 
                        it.copy(
                            currentLatLng = fallbackLatLng,
                            currentLocation = "Guelmim, Morocco (Default)",
                            isLoading = false
                        )
                    }
                    showError(context.getString(R.string.error_location_fallback))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location: ${e.message}", e)
                _state.update { 
                    it.copy(
                        error = e.message ?: context.getString(R.string.error_location_fallback),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun searchCurrentLocation(query: String) {
        // First update the current location text immediately to keep the UI responsive
        _state.update { 
            it.copy(
                currentLocation = query,
                isLoadingCurrentLocationResults = query.length > 2
            )
        }
        
        if (query.length <= 2) {
            Log.d(TAG, "Skipping location search - query too short: $query")
            _state.update { 
                it.copy(
                    currentLocationResults = emptyList(),
                    showCurrentLocationResults = false,
                    isLoadingCurrentLocationResults = false
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Searching current location for: $query")
                
                // Combine results from both sources
                val apiResults = placesManager.searchPlaces(query)
                    .map { PlaceItem.fromAutocompletePrediction(it) }
                
                val localResults = placesManager.searchLocalPlaces(query)
                    .map { PlaceItem.fromLocationSuggestion(it) }
                
                // Combine and prioritize results
                val combinedResults = if (localResults.isNotEmpty()) {
                    // If there are local results, prioritize them
                    localResults + apiResults.filter { apiItem ->
                        // Filter out API items with same primary text as local items
                        localResults.none { localItem -> 
                            localItem.primaryText.equals(apiItem.primaryText, ignoreCase = true)
                        }
                    }
                } else {
                    apiResults + localResults
                }.take(15) // Limit to avoid overwhelming UI
                
                Log.d(TAG, "Found ${combinedResults.size} location results (${apiResults.size} API, ${localResults.size} local)")
                
                // Only update search results, not current location field (already updated)
                _state.update { 
                    it.copy(
                        currentLocationResults = combinedResults,
                        showCurrentLocationResults = combinedResults.isNotEmpty(),
                        isLoadingCurrentLocationResults = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching places: ${e.message}", e)
                _state.update { 
                    it.copy(
                        error = e.message,
                        isLoadingCurrentLocationResults = false
                    )
                }
            }
        }
    }
    
    fun selectCurrentPlace(place: PlaceItem) {
        viewModelScope.launch {
            _state.update { 
                it.copy(
                    currentLocation = place.fullText,
                    showCurrentLocationResults = false,
                    isLoading = true
                )
            }

            try {
                Log.d(TAG, "Getting details for selected place: ${place.fullText}")
                val latLng = placesManager.getPlaceDetails(place.id)
                if (latLng != null) {
                    Log.d(TAG, "Got location details: $latLng")
                    _state.update { 
                        it.copy(
                            currentLatLng = latLng,
                            isLoading = false
                        )
                    }
                    // If we have both locations, find routes
                    _state.value.destinationLatLng?.let { dest ->
                        findRoutes(latLng, dest)
                    }
                } else {
                    Log.e(TAG, "Received null coordinates for place: ${place.fullText}")
                    _state.update { 
                        it.copy(
                            error = context.getString(R.string.error_coordinates),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting place details: ${e.message}", e)
                _state.update { 
                    it.copy(
                        error = e.message ?: context.getString(R.string.error_coordinates),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun searchPlaces(query: String) {
        // First update the destination text immediately to keep the UI responsive
        _state.update { 
            it.copy(
                destination = query,
                isLoadingDestinationResults = query.length > 2
            )
        }
        
        if (query.length <= 2) {
            Log.d(TAG, "Skipping destination search - query too short: $query")
            _state.update { 
                it.copy(
                    searchResults = emptyList(),
                    showSearchResults = false,
                    isLoadingDestinationResults = false
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                Log.d(TAG, "Searching destination for: $query")
                
                // Combine results from both sources
                val apiResults = placesManager.searchPlaces(query)
                    .map { PlaceItem.fromAutocompletePrediction(it) }
                
                val localResults = placesManager.searchLocalPlaces(query)
                    .map { PlaceItem.fromLocationSuggestion(it) }
                
                // Combine and prioritize results
                val combinedResults = if (localResults.isNotEmpty()) {
                    // If there are local results, prioritize them
                    localResults + apiResults.filter { apiItem ->
                        // Filter out API items with same primary text as local items
                        localResults.none { localItem -> 
                            localItem.primaryText.equals(apiItem.primaryText, ignoreCase = true)
                        }
                    }
                } else {
                    apiResults + localResults
                }.take(15) // Limit to avoid overwhelming UI
                
                Log.d(TAG, "Found ${combinedResults.size} destination results (${apiResults.size} API, ${localResults.size} local)")
                
                // Only update search results, not destination field (already updated)
                _state.update { 
                    it.copy(
                        searchResults = combinedResults,
                        showSearchResults = combinedResults.isNotEmpty(),
                        isLoadingDestinationResults = false
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error searching places: ${e.message}", e)
                _state.update { 
                    it.copy(
                        error = e.message,
                        isLoadingDestinationResults = false
                    )
                }
            }
        }
    }

    fun selectPlace(place: PlaceItem) {
        viewModelScope.launch {
            _state.update { 
                it.copy(
                    destination = place.fullText,
                    showSearchResults = false,
                    isLoading = true
                )
            }

            try {
                Log.d(TAG, "Getting details for selected destination: ${place.fullText}")
                val latLng = placesManager.getPlaceDetails(place.id)
                if (latLng != null) {
                    Log.d(TAG, "Got destination details: $latLng")
                    _state.update { 
                        it.copy(
                            destinationLatLng = latLng,
                            isLoading = false
                        )
                    }
                    // If we have both locations, find routes
                    _state.value.currentLatLng?.let { current ->
                        findRoutes(current, latLng)
                    }
                } else {
                    Log.e(TAG, "Received null coordinates for destination: ${place.fullText}")
                    _state.update { 
                        it.copy(
                            error = context.getString(R.string.error_coordinates),
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting place details: ${e.message}", e)
                _state.update { 
                    it.copy(
                        error = e.message ?: context.getString(R.string.error_coordinates),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun findRoutes(start: LatLng, end: LatLng) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingRoutes = true) }
            try {
                Log.d(TAG, "Finding routes from $start to $end")
                val startPoint = GeoPoint(start.latitude, start.longitude)
                val endPoint = GeoPoint(end.latitude, end.longitude)
                
                _state.value.buses?.let { buses ->
                    if (buses.isEmpty()) {
                        Log.e(TAG, "No buses available to find routes")
                        _state.update { 
                            it.copy(
                                error = context.getString(R.string.error_no_buses),
                                isLoadingRoutes = false
                            )
                        }
                        return@let
                    }
                    
                    Log.d(TAG, "Finding route using ${buses.size} buses")
                    val routes = withContext(Dispatchers.Default) {
                        routeFinder.findOptimalRoute(buses, startPoint, endPoint)
                    }
                    
                    if (routes.isEmpty()) {
                        Log.d(TAG, "No routes found between these locations")
                        _state.update { 
                            it.copy(
                                error = context.getString(R.string.error_no_routes),
                                isLoadingRoutes = false
                            )
                        }
                    } else {
                        Log.d(TAG, "Found ${routes.size} routes")
                        for (route in routes) {
                            Log.d(TAG, "Route with ${route.segments.size} segments, " +
                                    "${route.numberOfTransfers} transfers, " +
                                    "${route.totalWalkingDistance} meters walking")
                        }
                        _state.update { 
                            it.copy(
                                suggestedRoutes = routes,
                                isLoadingRoutes = false
                            )
                        }
                    }
                } ?: run {
                    Log.e(TAG, "Buses data is null")
                    _state.update { 
                        it.copy(
                            error = context.getString(R.string.error_bus_data),
                            isLoadingRoutes = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error finding routes: ${e.message}", e)
                _state.update { 
                    it.copy(
                        error = e.message ?: context.getString(R.string.error_no_routes),
                        isLoadingRoutes = false
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    fun showError(message: String) {
        Log.d(TAG, "Showing error: $message")
        _state.update { it.copy(error = message) }
    }
} 