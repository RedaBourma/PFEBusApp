package com.example.pfebusapp.pages

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.pfebusapp.AuthViewModel
import com.example.pfebusapp.busRepository.Bus
import com.example.pfebusapp.busRepository.BusRepository
import com.example.pfebusapp.uiComponents.BottomNavigationBar

@Composable
fun BusRoutesPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
//    var buses by rememberSaveable { mutableStateOf<List<Bus>?>(null) }
//
//    LaunchedEffect(Unit) {
//        buses = BusRepository().getAllBusData()
//        Log.d("zb", "Buses fetched: $buses")
//    }
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        Column(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Text(
                text = "Itinéraires de Bus",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )

//            for (bus in buses ?: emptyList()) {
//                Log.d("BusRoutesPage", "Bus: $bus")
//                Text(
//                    text = "Bus ${bus.num}, trajet: ${bus.trajet},\n" +
//                            " marque: ${bus.marque}, immatriculation: ${bus.immatriculation}",
//                    style = MaterialTheme.typography.titleLarge,
//                    color = MaterialTheme.colorScheme.onBackground
//                )
//                Spacer(
//                    modifier = Modifier.padding(8.dp)
//                )
//            }
//
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