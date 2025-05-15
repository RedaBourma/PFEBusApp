package com.example.pfebusapp.busRepository

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint

data class Bus (
    val num: String = "",
    val immatriculation: String = "",
    val marque: String = "",
    val nbrPlace: Int = 0,
    val status: String = "",
    val position: GeoPoint = GeoPoint(0.0, 0.0), // [latitude, longitude]
    val trajet: List<GeoPoint> = listOf() // List of [latitude, longitude] positions
//    val trajet: List<LatLng> = listOf() // List of [latitude, longitude] positions
){


    fun getPosLatitude(): Double = position.latitude
    fun getPosLongitude(): Double = position.longitude

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