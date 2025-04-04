package com.example.pfebusapp.uiManagment

import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomBarScreen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val contetnDescription: String? = null
){
    object Home : BottomBarScreen(
        route = "home",
        title = "Home",
        icon = androidx.compose.material.icons.Icons.Default.Home,
        contetnDescription = "Navigate to home screen"
    )
    object BusRoutes : BottomBarScreen(
        route = "busRoutes",
        title = "Bus Routes",
        icon = androidx.compose.material.icons.Icons.Default.Place,
        contetnDescription = "Navigate to bus routes screen"
    )
    object Profile : BottomBarScreen(
        route = "profile",
        title = "Profile",
        icon = androidx.compose.material.icons.Icons.Default.Person,
        contetnDescription = "Navigate to profile screen"
    )

    companion object{
        fun getAllScreens() = listOf(Home, BusRoutes, Profile)
    }
}