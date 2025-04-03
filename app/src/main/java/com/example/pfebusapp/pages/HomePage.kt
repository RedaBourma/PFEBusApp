package com.example.pfebusapp.pages

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.navigation.NavController
import com.example.pfebusapp.AuthState
import com.example.pfebusapp.AuthViewModel
import com.example.pfebusapp.userRepository.RegistredUser
import com.example.pfebusapp.userRepository.UserRepository
import java.util.Date

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
            is AuthState.Unauthenticated -> navController.navigate("login")
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
                    errorMessage = "User not found"
                    Log.d("userHome", "user is null")
                }
            },
            onFailure = {exception ->
                isLoading = false
                errorMessage = "Failed to load user data"
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

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = authViewModel.getCurrentUser()?.uid ?: "NULL", fontSize = 32.sp, color = Color.Blue)
       Text(text = "Home page", fontSize = 32.sp)

        when {
            isLoading -> {
                CircularProgressIndicator()
                Text(text = "Loading user data...", Modifier.padding(top = 16.dp))
            }
            errorMessage != null -> {
                Text(text = "Error: $errorMessage", color = Color.Red)
            }
            userData != null -> {
                Text(text = userData!!.nom, fontSize = 32.sp)
                Text(text = userData!!.prenom, fontSize = 16.sp)
                Text(text = userData!!.email, fontSize = 16.sp)
            }
            else -> {
                Text(text = "No user data available")
            }
        }

        Spacer(modifier = modifier.height(24.dp))
        TextButton(onClick = {
            authViewModel.signout()
        }) {
            Text(text = "Sign out")
        }
    }
}