package com.example.pfebusapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.pfebusapp.AuthState
import com.example.pfebusapp.AuthViewModel
import com.example.pfebusapp.pages.BusRoutesPage
import com.example.pfebusapp.pages.HomePage
import com.example.pfebusapp.pages.LoginPage
import com.example.pfebusapp.pages.ProfilePage
import com.example.pfebusapp.pages.SignupPage

@Composable
fun MyAppNavigation(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val authState by authViewModel.authState.observeAsState()

    // Handle initial auth state and navigation
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            }
            is AuthState.Unauthenticated -> {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
            else -> {}
        }
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginPage(modifier, navController, authViewModel)
        }
        composable("signup") {
            SignupPage(modifier, navController, authViewModel)
        }
        navigation(startDestination = "home", route = "main") {
            composable("home") {
                HomePage(modifier, navController, authViewModel)
            }
            composable("busRoutes") {
                BusRoutesPage(modifier, navController, authViewModel)
            }
            composable("profile") {
                ProfilePage(modifier, navController, authViewModel)
            }
        }
    }
}