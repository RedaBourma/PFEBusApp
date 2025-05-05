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
        title = "Accueil",
        icon = androidx.compose.material.icons.Icons.Default.Home,
        contetnDescription = "Naviguer vers l'écran d'accueil"
    )
    object BusRoutes : BottomBarScreen(
        route = "busRoutes",
        title = "Itinéraires",
        icon = androidx.compose.material.icons.Icons.Default.Place,
        contetnDescription = "Naviguer vers l'écran des itinéraires de bus"
    )
    object Profile : BottomBarScreen(
        route = "profile",
        title = "Profil",
        icon = androidx.compose.material.icons.Icons.Default.Person,
        contetnDescription = "Naviguer vers l'écran de profil"
    )

    companion object{
        fun getAllScreens() = listOf(Home, BusRoutes, Profile)
    }
}