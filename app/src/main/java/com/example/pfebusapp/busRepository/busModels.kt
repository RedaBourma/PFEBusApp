package com.example.pfebusapp.busRepository

import com.google.firebase.firestore.GeoPoint

data class Bus (
    val num: String = "",
    val immatriculation: String = "",
    val marque: String = "",
    val nbrPlace: Int = 0,
    val status: String = "",
    val position: GeoPoint = GeoPoint(0.0, 0.0), // [latitude, longitude]
    val trajet: List<GeoPoint> = listOf() // List of [latitude, longitude] positions
){

    // Helper functions
//    fun getPositionLatitude(): Double = if (position.size >= 1) position[0] else 0.0
//    fun getPositionLongitude(): Double = if (position.size >= 2) position[1] else 0.0
    
    // Get a readable description of the status
//    fun getStatusDescription(): String {
//        return when (status.lowercase()) {
//            "retour" -> "En retour"
//            "aller" -> "En route"
//            "arret" -> "À l'arrêt"
//            else -> status
//        }
//    }
}