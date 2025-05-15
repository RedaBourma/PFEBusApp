package com.example.pfebusapp.utils

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint

class Converts {
    fun convertGeoPointListToLatLngList(geoPoints: List<GeoPoint>): List<LatLng> {
        return geoPoints.map { geoPoint ->
            LatLng(geoPoint.latitude, geoPoint.longitude)
        }
    }
}