package com.example.pfebusapp.pages

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.pfebusapp.AuthState
import com.example.pfebusapp.AuthViewModel
import com.example.pfebusapp.uiComponents.BottomNavigationBar
import com.example.pfebusapp.userRepository.RegistredUser
import com.example.pfebusapp.userRepository.UserRepository
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun HomePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {

    val authState = authViewModel.authState.observeAsState()
    var userData by remember { mutableStateOf<RegistredUser?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
//    val ctx = LocalContext.current

    LaunchedEffect(authState.value) {
//        val user = UserRepository()
//        user.createUser(ctx, "reda@gmail.com", "Reda", "Reda", Date(), "123456789")

        when(authState.value){
            is AuthState.Unauthenticated -> {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> Unit
        }
    }

    //Todo : add states system, user view model, navigation, nav bar
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

//    var userData : RegistredUser? = null
//    UserRepository().getUserData(
//        userId = authViewModel.currentUserId?.toString() ?: "",
//        onSuccess = { user ->
//            if (user != null){
//                userData = user
//            }else{
//                Log.d("userHome", "user is null")
//            }
//        },
//        onFailure = {exception ->
//           Log.e("userHome", "failed to get user data", exception)
//        }
//    )

    Scaffold (
       bottomBar = {
           BottomNavigationBar(navController = navController)
       }
    ) { paddingValues ->
        Column(
            modifier = modifier.fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 28.9865, -10.0572
            val Guelmim = LatLng(28.9865, -10.0572)
//            val moroccoMarkerState = rememberMarkerState(position = Guelmim)
            val cameraPositionState = rememberCameraPositionState {
                position = CameraPosition.fromLatLngZoom(Guelmim, 15f)
            }
            GoogleMap(
                modifier = Modifier
                    .fillMaxSize(),
                cameraPositionState = cameraPositionState
            ){
//                Box(
//                   modifier = Modifier
//                       .padding()
//                ){
//
//                }
            }


//            Text(
//                text = authViewModel.getCurrentUser()?.uid ?: "NULL",
//                style = MaterialTheme.typography.headlineMedium,
//                color = MaterialTheme.colorScheme.primary
//            )
//            Text(
//                text = "Page d'accueil",
//                style = MaterialTheme.typography.displaySmall,
//                color = MaterialTheme.colorScheme.onBackground
//            )
//
//            when {
//                isLoading -> {
//                    CircularProgressIndicator(
//                        color = MaterialTheme.colorScheme.primary
//                    )
//                    Text(
//                        text = "Chargement des données utilisateur...",
//                        modifier = Modifier.padding(top = 16.dp),
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onBackground
//                    )
//                }
//                errorMessage != null -> {
//                    Text(
//                        text = "Erreur: $errorMessage",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.error
//                    )
//                }
//                userData != null -> {
//                    Text(
//                        text = userData!!.nom,
//                        style = MaterialTheme.typography.headlineLarge,
//                        color = MaterialTheme.colorScheme.onBackground
//                    )
//                    Text(
//                        text = userData!!.prenom,
//                        style = MaterialTheme.typography.bodyLarge,
//                        color = MaterialTheme.colorScheme.onBackground
//                    )
//                    Text(
//                        text = userData!!.email,
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onBackground
//                    )
//                }
//                else -> {
//                    Text(
//                        text = "Aucune donnée utilisateur disponible",
//                        style = MaterialTheme.typography.bodyMedium,
//                        color = MaterialTheme.colorScheme.onBackground
//                    )
//                }
//            }
//
//            Spacer(modifier = modifier.height(24.dp))
//            TextButton(
//                onClick = {
//                    authViewModel.signout()
//                },
//                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
//                    contentColor = MaterialTheme.colorScheme.primary
//                )
//            ) {
//                Text(
//                    text = "Se déconnecter",
//                    style = MaterialTheme.typography.labelLarge
//                )
//            }
//
//            TextButton(
//                onClick = {
//                    navController.navigate("busRoutes")
//                },
//                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
//                    contentColor = MaterialTheme.colorScheme.primary
//                )
//            ) {
//                Text(
//                    text = "Itinéraires de bus",
//                    style = MaterialTheme.typography.labelLarge
//                )
//            }
        }
    }
}