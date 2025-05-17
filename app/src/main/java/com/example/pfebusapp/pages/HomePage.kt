package com.example.pfebusapp.pages

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.pfebusapp.AuthState
import com.example.pfebusapp.AuthViewModel
import com.example.pfebusapp.busRepository.Bus
import com.example.pfebusapp.busRepository.BusRepository
import com.example.pfebusapp.uiComponents.BottomNavigationBar
import com.example.pfebusapp.uiComponents.CustomBottomSheet
import com.example.pfebusapp.uiComponents.ExpandableCard
import com.example.pfebusapp.userRepository.RegistredUser
import com.example.pfebusapp.userRepository.UserRepository
import com.example.pfebusapp.utils.Converts
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val authState = authViewModel.authState.observeAsState()
    var userData by remember { mutableStateOf<RegistredUser?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(true) }
    var buses by remember { mutableStateOf<List<Bus>?>(null) }
    var isLoadingBuses by remember { mutableStateOf(true) }
    var selectedBus by remember { mutableStateOf<Bus?>(null) }
    var ctx = LocalContext.current
    var errorMessageBuses by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState.value) {
        when(authState.value){
            is AuthState.Unauthenticated -> {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> Unit
        }
    }

    //
    LaunchedEffect(authViewModel.getCurrentUser()) {
        isLoading = true
        UserRepository().getUserData(
            userId = authViewModel.getCurrentUser()?.uid ?: "",
            onSuccess = { user ->
                isLoading = false
                if (user != null){
                    userData = user
                }else{
                    errorMessage = "Utilisateur non trouvé"
                    Log.d("userHome", "user is null")
                }
            },
            onFailure = {exception ->
                isLoading = false
                errorMessage = "Échec du chargement des données utilisateur"
               Log.e("userHome", "failed to get user data", exception)
            }
        )
    }
    //

    Log.d("HomePage", "Before LaunchedEffect - initializing")

    LaunchedEffect(Unit) {
        isLoadingBuses = true
        errorMessageBuses = null
        try {
            BusRepository().getAllBusData(
                onSuccess = { fetchedBuses ->
                    buses = fetchedBuses
                    isLoadingBuses = false
                    Log.d("HomePage", "Buses fetched successfully: ${fetchedBuses.size} buses")
                },
                onFailure = { exception ->
                    isLoadingBuses = false
                    errorMessageBuses = "Failed to load buses: ${exception.message}"
                    Log.e("HomePage", "Failed to fetch buses", exception)
                }
            )
        } catch (e: Exception) {
            isLoadingBuses = false
            errorMessageBuses = "Error loading buses: ${e.message}"
            Log.e("HomePage", "Exception while fetching buses", e)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content - Map
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Add a diagnostic button at the top

            // 28.9865, -10.0572
            val Guelmim = LatLng(28.9865, -10.0572)
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(Guelmim, 15f)
            }
            LaunchedEffect(selectedBus?.trajet) {
                selectedBus?.trajet?.let { routeList ->
                    if (routeList.isNotEmpty()) {
                        val latLngBounds = LatLngBounds.builder().apply {
                            routeList.forEach { stopMap ->
                                stopMap.forEach { (_, geoPoint) ->
                                    include(LatLng(geoPoint.latitude, geoPoint.longitude))
                                }
                            }
                        }.build()
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100), 100)
                        cameraPositionState.animate(CameraUpdateFactory.scrollBy(0f, 400f), 500)
                    }
                }
            }
            // Add a key that changes with the selected bus to force content recomposition
            val mapContentKey = remember(selectedBus?.num) { selectedBus?.num ?: "no_bus_selected" }
            
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(mapType = MapType.NORMAL, isTrafficEnabled = true)
            ) {
                // Use LaunchedEffect to track selected bus changes
                LaunchedEffect(selectedBus?.num) {
                    // This will trigger recomposition of the map content when selectedBus changes
                    Log.d("HomePage", "Selected bus changed to: ${selectedBus?.num ?: "none"}")
                }

                // Add markers for each bus
                buses?.forEach { bus ->
                    if (bus.position.latitude != 0.0 && bus.position.longitude != 0.0) {
                        Marker(
                            state = MarkerState(position = LatLng(bus.position.latitude, bus.position.longitude)),
                            title = "Bus ${bus.num}",
                            snippet = "${bus.marque} - ${bus.status}"
                        )
                    }
                }

                // Only draw route and stops for the selected bus
                selectedBus?.let { bus ->
                    Log.d("HomePage", "Drawing route for bus ${bus.num}")
                    val routePoints = mutableListOf<LatLng>()
                    
                    // Collect all points for the route
                    bus.trajet.forEach { stopMap ->
                        stopMap.forEach { (_, geoPoint) ->
                            routePoints.add(LatLng(geoPoint.latitude, geoPoint.longitude))
                        }
                    }
                    
                    // Draw the route line only if we have points
                    if (routePoints.size >= 2) {
                        Polyline(
                            points = routePoints,
                            color = MaterialTheme.colorScheme.primary,
                            width = 8f
                        )
                        
                        // Add markers for each stop
                        bus.trajet.forEachIndexed { index, stopMap ->
                            stopMap.forEach { (stopName, geoPoint) ->
                                Marker(
                                    state = MarkerState(position = LatLng(geoPoint.latitude, geoPoint.longitude)),
                                    title = stopName,
                                    snippet = if (index == 0 || index == bus.trajet.size - 1) 
                                                "Terminal Stop" 
                                             else 
                                                "Bus Stop"
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom bottom sheet - in the middle layer
        CustomBottomSheet(
            visible = showBottomSheet,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(1f)
        ) {
            Column(modifier = Modifier.padding(bottom = 70.dp)) {
                if (isLoadingBuses) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(4.dp)
                    )
                    Text(
                        "Loading buses...",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else if (errorMessageBuses != null) {
                    Text(
                        text = errorMessageBuses!!,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                } else if (buses == null || buses!!.isEmpty()) {
                    Text(
                        "No buses available",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )
                    LazyColumn {
                        items(buses!!.sortedBy { it.num }) { bus ->
                            ExpandableCard(
                                bus = bus,
                                title = bus.num,
                                description = bus.marque,
                                selectedBus = selectedBus,
                                onBusSelected = { clickedBus ->
                                    // Clear the current selection first
                                    if (selectedBus == clickedBus) {
                                        selectedBus = null
                                        Log.d("HomePage", "Bus deselected: ${clickedBus.num}")
                                    } else {
                                        // Set the new selection
                                        selectedBus = clickedBus
                                        Log.d("HomePage", "Bus selected: ${clickedBus.num}, trajet size: ${clickedBus.trajet.size}")
                                    }
                                }
                            )
                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom navigation - always on top layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .zIndex(2f)
        ) {
            BottomNavigationBar(navController = navController)
        }
    }
}
