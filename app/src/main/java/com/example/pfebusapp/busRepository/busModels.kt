package com.example.pfebusapp.busRepository

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.GeoPoint

data class BusStop(
    val name: String = "",
    val location: GeoPoint = GeoPoint(0.0, 0.0),
//    val arrivalTime: String = "", // Optional: for future use
//    val isTerminal: Boolean = false // Optional: to mark start/end stops
)

data class Bus(
    val num: String = "",
    val immatriculation: String = "",
    val marque: String = "",
    val nbrPlace: Int = 0,
    val status: String = "",
    val position: GeoPoint = GeoPoint(0.0, 0.0),
    val trajet: List<Map<String, GeoPoint>> = listOf(),
    val busStop: DocumentReference? = null,
    val busStops: List<DocumentReference> = listOf()
) {
    fun getPosLatitude(): Double = position.latitude
    fun getPosLongitude(): Double = position.longitude

    // Helper function to get just the locations from the stops
//    fun getTrajetLocations(): List<GeoPoint> = trajet.map { it.location }

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

// Helper function to convert string coordinates to GeoPoint
fun String.toGeoPoint(): GeoPoint {
    return try {
        // Remove the degree symbols and split by comma
        val cleanStr = this.replace("°", "").replace("[", "").replace("]", "")
        val parts = cleanStr.split(",")
        if (parts.size == 2) {
            val lat = parts[0].trim().replace("N", "").toDouble()
            val lng = parts[1].trim().replace("W", "").replace("E", "").toDouble()
            // If it's West longitude, make it negative
            val finalLng = if (parts[1].contains("W")) -lng else lng
            GeoPoint(lat, finalLng)
        } else {
            GeoPoint(0.0, 0.0)
        }
    } catch (e: Exception) {
        GeoPoint(0.0, 0.0)
    }
}