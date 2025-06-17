package com.example.pfebusapp.pages

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pfebusapp.AuthViewModel
import com.example.pfebusapp.R
import com.example.pfebusapp.busRepository.Bus
import com.example.pfebusapp.location.LocationManager
import com.example.pfebusapp.uiComponents.BottomNavigationBar
import com.example.pfebusapp.viewmodels.BusRoutesViewModel
import com.example.pfebusapp.viewmodels.PlaceItem
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.compose.*

@Composable
private fun SearchResultsList(
    placeItems: List<PlaceItem>,
    onPlaceSelected: (PlaceItem) -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .heightIn(max = 250.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (placeItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aucun résultat trouvé. Essayez d'autres termes de recherche.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    placeItems.forEach { place ->
                        TextButton(
                            onClick = { onPlaceSelected(place) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icon based on place type
                                val iconRes = when {
                                    place.type.equals("bus_stop", ignoreCase = true) -> R.drawable.ic_bus
                                    place.type.equals("school", ignoreCase = true) || 
                                    place.type.equals("university", ignoreCase = true) -> R.drawable.ic_school
                                    place.type.equals("city", ignoreCase = true) -> R.drawable.ic_city
                                    else -> R.drawable.ic_location
                                }
                                
                                Icon(
                                    painter = painterResource(id = iconRes),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Text(
                                        text = place.primaryText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                    )
                                    Text(
                                        text = place.secondaryText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BusRoutesPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    val viewModel: BusRoutesViewModel = viewModel { BusRoutesViewModel(context) }
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    // Colors for the different route types
    val walkingColor = Color(0xFF7986CB) // Indigo
    val busRouteColor = Color(0xFFEF5350) // Red
    val transferColor = Color(0xFFFFA726) // Orange

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.getCurrentLocation()
        }
    }

    val Guelmim = LatLng(28.9865, -10.0572)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(Guelmim, 15f)
    }

    // Update camera when current location changes
    LaunchedEffect(state.currentLatLng) {
        state.currentLatLng?.let {
            cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(it, 15f))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
            ) {
                // Draw current location marker
                state.currentLatLng?.let { latLng ->
                    Marker(
                        state = MarkerState(position = latLng),
                        title = stringResource(R.string.marker_current_location),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
                    )
                }

                // Draw destination marker
                state.destinationLatLng?.let { latLng ->
                    Marker(
                        state = MarkerState(position = latLng),
                        title = stringResource(R.string.marker_destination),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }

                // Draw suggested routes
                state.suggestedRoutes?.forEach { route ->
                    if (route.segments.isNotEmpty()) {
                        route.segments.forEach { segment ->
                            // Draw a walking line from current location to first bus stop
                            if (state.currentLatLng != null && route.segments.indexOf(segment) == 0) {
                                // Walking path: dashed indigo line
                                Polyline(
                                    points = listOf(
                                        state.currentLatLng!!,
                                        LatLng(segment.startStop.latitude, segment.startStop.longitude)
                                    ),
                                    color = walkingColor,
                                    width = 5f,
                                    pattern = listOf(Dash(15f), Gap(10f)),
                                    jointType = JointType.ROUND,
                                    startCap = RoundCap(),
                                    endCap = RoundCap()
                                )
                                
                                // Add a "walking" marker with distance information
                                val walkingDistance = calculateDistance(
                                    GeoPoint(state.currentLatLng!!.latitude, state.currentLatLng!!.longitude),
                                    segment.startStop
                                )
                                
                                val midPoint = LatLng(
                                    (state.currentLatLng!!.latitude + segment.startStop.latitude) / 2,
                                    (state.currentLatLng!!.longitude + segment.startStop.longitude) / 2
                                )
                                
                                Marker(
                                    state = MarkerState(position = midPoint),
                                    title = "Marche à pied",
                                    snippet = "${(walkingDistance/1000).toInt()} km (${(walkingDistance/80).toInt()} min)",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                                    alpha = 0.7f
                                )
                            }
                            
                            // Draw the bus route
                            segment.bus.trajet.forEach { stopMap ->
                                // For each trajet (route), collect all points
                                val points = stopMap.values.map { geoPoint ->
                                    LatLng(geoPoint.latitude, geoPoint.longitude)
                                }
                                if (points.size >= 2) {
                                    // Bus route: solid red line
                                    Polyline(
                                        points = points,
                                        color = busRouteColor,
                                        width = 8f,
                                        jointType = JointType.ROUND,
                                        startCap = RoundCap(),
                                        endCap = RoundCap()
                                    )
                                }
                                
                                // Add markers for each bus stop along the route
                                stopMap.forEach { (stopName, stopPoint) ->
                                    Marker(
                                        state = MarkerState(position = LatLng(stopPoint.latitude, stopPoint.longitude)),
                                        title = stopName,
                                        snippet = stringResource(R.string.marker_bus_stop, segment.bus.num),
                                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                                    )
                                }
                            }
                            
                            // Draw a walking line from last bus stop to destination
                            if (state.destinationLatLng != null && 
                                route.segments.indexOf(segment) == route.segments.size - 1) {
                                // Walking path: dashed indigo line
                                Polyline(
                                    points = listOf(
                                        LatLng(segment.endStop.latitude, segment.endStop.longitude),
                                        state.destinationLatLng!!
                                    ),
                                    color = walkingColor,
                                    width = 5f,
                                    pattern = listOf(Dash(15f), Gap(10f)),
                                    jointType = JointType.ROUND,
                                    startCap = RoundCap(),
                                    endCap = RoundCap()
                                )
                                
                                // Add a "walking" marker with distance information
                                val walkingDistance = calculateDistance(
                                    segment.endStop,
                                    GeoPoint(state.destinationLatLng!!.latitude, state.destinationLatLng!!.longitude)
                                )
                                
                                val midPoint = LatLng(
                                    (state.destinationLatLng!!.latitude + segment.endStop.latitude) / 2,
                                    (state.destinationLatLng!!.longitude + segment.endStop.longitude) / 2
                                )
                                
                                Marker(
                                    state = MarkerState(position = midPoint),
                                    title = "Marche à pied",
                                    snippet = "${(walkingDistance/1000).toInt()} km (${(walkingDistance/80).toInt()} min)",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                                    alpha = 0.7f
                                )
                            }
                            
                            // Draw a line between transfer points if needed
                            if (route.segments.size > 1 && 
                                route.segments.indexOf(segment) < route.segments.size - 1) {
                                val nextSegment = route.segments[route.segments.indexOf(segment) + 1]
                                // Transfer: dashed orange line
                                Polyline(
                                    points = listOf(
                                        LatLng(segment.endStop.latitude, segment.endStop.longitude),
                                        LatLng(nextSegment.startStop.latitude, nextSegment.startStop.longitude)
                                    ),
                                    color = transferColor,
                                    width = 5f,
                                    pattern = listOf(Dash(20f), Gap(15f)),
                                    jointType = JointType.ROUND,
                                    startCap = RoundCap(),
                                    endCap = RoundCap()
                                )
                                
                                // Add a "transfer" marker
                                val transferDistance = calculateDistance(segment.endStop, nextSegment.startStop)
                                
                                val midPoint = LatLng(
                                    (segment.endStop.latitude + nextSegment.startStop.latitude) / 2,
                                    (segment.endStop.longitude + nextSegment.startStop.longitude) / 2
                                )
                                
                                Marker(
                                    state = MarkerState(position = midPoint),
                                    title = "Correspondance",
                                    snippet = "${(transferDistance/1000).toInt()} km (${(transferDistance/80).toInt()} min)",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                                    alpha = 0.7f
                                )
                            }
                        }
                    }
                }
            }
        }

        // Search and location inputs panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                .padding(16.dp)
                .zIndex(1f)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current Location Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = state.currentLocation,
                            onValueChange = { 
                                viewModel.searchCurrentLocation(it)
                            },
                            label = { Text(stringResource(R.string.current_location)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            enabled = !state.isLoading,
                            trailingIcon = {
                                if (state.isLoadingCurrentLocationResults) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(onClick = {
                                        if (state.currentLocation.isNotEmpty()) {
                                            viewModel.searchCurrentLocation(state.currentLocation)
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = stringResource(R.string.search_location)
                                        )
                                    }
                                }
                            }
                        )
                        
                        if (state.showCurrentLocationResults || state.isLoadingCurrentLocationResults) {
                            SearchResultsList(
                                placeItems = state.currentLocationResults,
                                onPlaceSelected = { viewModel.selectCurrentPlace(it) },
                                isLoading = state.isLoadingCurrentLocationResults,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 56.dp)
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = {
                            when {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    LocationManager.LOCATION_PERMISSION
                                ) == PackageManager.PERMISSION_GRANTED -> {
                                    viewModel.getCurrentLocation()
                                }
                                else -> locationPermissionLauncher.launch(LocationManager.LOCATION_PERMISSION)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        enabled = !state.isLoading
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = stringResource(R.string.get_current_location),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Destination Search
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = state.destination,
                        onValueChange = { 
                            viewModel.searchPlaces(it)
                        },
                        label = { Text(stringResource(R.string.destination)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !state.isLoading,
                        trailingIcon = {
                            if (state.isLoadingDestinationResults) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = {
                                    if (state.destination.isNotEmpty()) {
                                        viewModel.searchPlaces(state.destination)
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = stringResource(R.string.search_destination)
                                    )
                                }
                            }
                        }
                    )

                    if (state.showSearchResults || state.isLoadingDestinationResults) {
                        SearchResultsList(
                            placeItems = state.searchResults,
                            onPlaceSelected = { viewModel.selectPlace(it) },
                            isLoading = state.isLoadingDestinationResults,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 56.dp)
                        )
                    }
                }
                
                                
                // Find Routes Button
                Button(
                    onClick = {
                        if (state.currentLatLng != null && state.destinationLatLng != null) {
                            viewModel.findRoutes(state.currentLatLng!!, state.destinationLatLng!!)
                        } else {
                            viewModel.showError(context.getString(R.string.error_select_locations))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.find_bus_routes),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
        
        // Display route information if available
        state.suggestedRoutes?.let { routes ->
            if (routes.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                        .zIndex(1f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.routes_found, routes.size),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val bestRoute = routes.first()
                        Text(
                            text = stringResource(
                                R.string.best_route, 
                                bestRoute.segments.size,
                                bestRoute.numberOfTransfers,
                                (bestRoute.totalWalkingDistance / 1000).toInt()
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Legend for route colors
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Bus route legend
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 24.dp, height = 4.dp)
                                        .background(color = busRouteColor)
                                )
                                Text(
                                    text = "Trajet en bus",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            // Walking legend
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 24.dp, height = 4.dp)
                                        .background(
                                            color = walkingColor,
                                            shape = DashedShape(dashLength = 4f, gapLength = 4f)
                                        )
                                )
                                Text(
                                    text = "À pied",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            // Transfer legend
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 24.dp, height = 4.dp)
                                        .background(
                                            color = transferColor,
                                            shape = DashedShape(dashLength = 6f, gapLength = 4f)
                                        )
                                )
                                Text(
                                    text = "Correspondance",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        
                        Divider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        
                        Text(
                            text = "Itinéraire:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        bestRoute.segments.forEachIndexed { index, segment ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Segment number with circle
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                // Segment info
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    if (segment.bus.busNumber == "Walk") {
                                        Text(
                                            text = "Marche à pied (${(segment.busTravelDistance/1000).toInt()} km)",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                        )
                                    } else {
                                        Text(
                                            text = "Bus ${segment.bus.busNumber} - ${segment.bus.busName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                        )
                                        
                                        Text(
                                            text = "De: ${segment.startStopName} → À: ${segment.endStopName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            
                            // Add transfer info if not the last segment
                            if (index < bestRoute.segments.size - 1) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, top = 2.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.dp)
                                            .height(24.dp)
                                            .background(color = transferColor)
                                    )
                                    
                                    Spacer(modifier = Modifier.width(18.dp))
                                    
                                    Text(
                                        text = "Correspondance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Loading indicator
        if (state.isLoadingRoutes) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.finding_routes),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // Error message
        state.error?.let { error ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            ) {
                Text(error)
            }
        }

        // Bottom navigation
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

// Shape for dashed lines in the legend
private class DashedShape(val dashLength: Float, val gapLength: Float) : androidx.compose.ui.graphics.Shape {
    override fun createOutline(size: androidx.compose.ui.geometry.Size, layoutDirection: androidx.compose.ui.unit.LayoutDirection, density: androidx.compose.ui.unit.Density): androidx.compose.ui.graphics.Outline {
        val path = androidx.compose.ui.graphics.Path()
        var x = 0f
        while (x < size.width) {
            path.moveTo(x, 0f)
            path.lineTo(x + dashLength, 0f)
            path.lineTo(x + dashLength, size.height)
            path.lineTo(x, size.height)
            path.close()
            x += dashLength + gapLength
        }
        return androidx.compose.ui.graphics.Outline.Generic(path)
    }
}

// Helper function to calculate distance
private fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Float {
    val results = FloatArray(1)
    Location.distanceBetween(
        point1.latitude, point1.longitude,
        point2.latitude, point2.longitude,
        results
    )
    return results[0]
}

// Helper function to check if a point is near another
private fun isNearPoint(point1: GeoPoint, point2: GeoPoint, maxDistanceMeters: Double = 1000.0): Boolean {
    val results = FloatArray(1)
    Location.distanceBetween(
        point1.latitude, point1.longitude,
        point2.latitude, point2.longitude,
        results
    )
    return results[0] <= maxDistanceMeters
}

private fun findOptimalRoute(buses: List<Bus>, start: GeoPoint, end: GeoPoint): List<Bus> {
    // This is a simple implementation that finds buses that pass near both points
    // You might want to implement a more sophisticated algorithm that considers:
    // - Multiple bus transfers
    // - Walking distance
    // - Bus frequency
    // - Time of day
    // - Traffic conditions
    
    return buses.filter { bus ->
        var hasStartPoint = false
        var hasEndPoint = false
        
        bus.trajet.forEach { stopMap ->
            stopMap.values.forEach { geoPoint ->
                if (isNearPoint(geoPoint, start)) hasStartPoint = true
                if (isNearPoint(geoPoint, end)) hasEndPoint = true
            }
        }
        
        hasStartPoint && hasEndPoint
    }
}