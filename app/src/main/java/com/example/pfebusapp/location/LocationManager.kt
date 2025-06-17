package com.example.pfebusapp.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.tasks.await

class LocationManager(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    suspend fun getCurrentLocation(): LatLng? {
        return try {
            val location = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            location?.let { LatLng(it.latitude, it.longitude) }
        } catch (e: Exception) {
            Log.e("LocationManager", "Error getting location", e)
            null
        }
    }

    suspend fun getAddressFromLocation(latLng: LatLng): String {
        return try {
            val addresses = android.location.Geocoder(context, java.util.Locale.getDefault())
                .getFromLocation(latLng.latitude, latLng.longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0) ?: "Unknown Location"
        } catch (e: Exception) {
            Log.e("LocationManager", "Error getting address", e)
            "Unknown Location"
        }
    }

    companion object {
        const val LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION
    }
} 