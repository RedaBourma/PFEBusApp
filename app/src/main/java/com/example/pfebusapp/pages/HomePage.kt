package com.example.pfebusapp.pages

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.pfebusapp.AuthState
import com.example.pfebusapp.AuthViewModel
import com.example.pfebusapp.busRepository.Bus
import com.example.pfebusapp.uiComponents.BottomNavigationBar
import com.example.pfebusapp.userRepository.RegistredUser
import com.example.pfebusapp.userRepository.UserRepository
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.example.pfebusapp.busRepository.BusRepository
import com.example.pfebusapp.uiComponents.CustomBottomSheet
import com.example.pfebusapp.firebase.FirestoreHelper
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val authState = authViewModel.authState.observeAsState()
    var userData by remember { mutableStateOf<RegistredUser?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showBottomSheet by remember { mutableStateOf(true) }
    var buses by remember { mutableStateOf<List<Bus>?>(null) }
    var isLoadingBuses by remember { mutableStateOf(true) }
    var busDataSource by remember { mutableStateOf("") }

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

    Log.d("HomePage", "Before LaunchedEffect - initializing")

    LaunchedEffect(Unit) {
        buses = BusRepository().getAllBusData()
        Log.d("HomePage", "Buses fetched: $buses")
        isLoadingBuses = false
    }
    
//    Log.d("HomePage", "After LaunchedEffect - buses: ${buses?.size ?: 0}")

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
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // Add markers for each bus
//                buses?.forEach { bus ->
//                    if (bus.position.size >= 2) {
//                        val busPosition = LatLng(bus.getPositionLatitude(), bus.getPositionLongitude())
//                        Marker(
//                            state = MarkerState(position = busPosition),
//                            title = "Bus ${bus.num}",
//                            snippet = "${bus.marque} - ${bus.getStatusDescription()}"
//                        )
//                    }
//                }
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
                Text(
                    "Buses Available: ${buses?.size ?: 0}${if (busDataSource.isNotEmpty()) " ($busDataSource)" else ""}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(16.dp)
                )
                
                if (isLoadingBuses) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .height(4.dp)
                    )
                    Text(
                        "Loading buses",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else if (buses == null || buses!!.isEmpty()) {
                    Text(
                        "No buses available",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn {
                        items(buses!!) { bus ->
                            ListItem(
                                headlineContent = {
                                    Text(
                                        "Bus ${bus.num} - ${bus.marque}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                },
                                supportingContent = {
                                    Column {
                                        Text(
                                            "Status: mnb3d",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "Places: ${bus.nbrPlace} | Trajet: ${bus.trajet.size} points",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                leadingContent = {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when (bus.status.lowercase()) {
                                                    "retour" -> MaterialTheme.colorScheme.primaryContainer
                                                    "aller" -> MaterialTheme.colorScheme.secondaryContainer
                                                    "arret" -> MaterialTheme.colorScheme.tertiaryContainer
                                                    else -> MaterialTheme.colorScheme.primaryContainer
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = "Bus icon",
                                            tint = when (bus.status.lowercase()) {
                                                "retour" -> MaterialTheme.colorScheme.primary
                                                "aller" -> MaterialTheme.colorScheme.secondary
                                                "arret" -> MaterialTheme.colorScheme.tertiary
                                                else -> MaterialTheme.colorScheme.primary
                                            },
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                },
                                trailingContent = {
                                    Text(
                                        bus.immatriculation,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
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
