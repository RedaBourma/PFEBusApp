package com.example.pfebusapp.busRepository

import android.annotation.SuppressLint
import android.util.Log
import com.example.pfebusapp.firebase.FirestoreHelper
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for handling locations, points of interest, and route stops in Guelmim
 */
class RouteLocationsRepository {
    private val TAG = "RouteLocationsRepository"
    private val firestoreHelper = FirestoreHelper()
    private val db = firestoreHelper.getDb()
    private val LOCATIONS_COLLECTION = "locations"
    
    // Data class for locations
    data class Location(
        val id: String = UUID.randomUUID().toString().substring(0, 8),
        val name: String = "",
        val type: String = "poi", // poi, bus_stop, school, university, hospital, etc.
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val city: String = "Guelmim",
        val region: String = "Guelmim-Oued Noun",
        val address: String = "",
        val description: String = ""
    ) {
        fun toLatLng(): LatLng = LatLng(latitude, longitude)
        fun toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)
    }
    
    /**
     * Get all locations from the database
     */
    @SuppressLint("LongLogTag")
    suspend fun getAllLocations(): List<Location> {
        return try {
            val snapshot = db.collection(LOCATIONS_COLLECTION).get().await()
            val locations = snapshot.toObjects(Location::class.java)
            Log.d(TAG, "Fetched ${locations.size} locations from Firestore")
            
            if (locations.isEmpty()) {
                // Return default locations if none found in database
                getDefaultLocations()
            } else {
                locations
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching locations", e)
            getDefaultLocations()
        }
    }
    
    /**
     * Get locations of a specific type
     */
    suspend fun getLocationsOfType(type: String): List<Location> {
        return try {
            val snapshot = db.collection(LOCATIONS_COLLECTION)
                .whereEqualTo("type", type)
                .get()
                .await()
            
            snapshot.toObjects(Location::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching locations of type $type", e)
            emptyList()
        }
    }
    
    /**
     * Get educational institutions (schools and universities)
     */
    suspend fun getEducationalInstitutions(): List<Location> {
        return try {
            val snapshot = db.collection(LOCATIONS_COLLECTION)
                .whereIn("type", listOf("school", "university"))
                .get()
                .await()
            
            val results = snapshot.toObjects(Location::class.java)
            Log.d(TAG, "Fetched ${results.size} educational institutions")
            
            if (results.isEmpty()) {
                // Filter schools from default locations
                getDefaultLocations().filter { 
                    it.type == "school" || it.type == "university" 
                }
            } else {
                results
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching educational institutions", e)
            getDefaultLocations().filter { 
                it.type == "school" || it.type == "university" 
            }
        }
    }
    
    /**
     * Search locations by name or address
     */
    suspend fun searchLocationsByName(query: String): List<Location> {
        if (query.length < 2) return emptyList()
        
        // Try to get from Firestore first
        try {
            val snapshot = db.collection(LOCATIONS_COLLECTION)
                .get()
                .await()
            
            // Firestore doesn't support contains queries directly, so we filter here
            val results = snapshot.toObjects(Location::class.java)
                .filter { location ->
                    location.name.contains(query, ignoreCase = true) ||
                    location.address.contains(query, ignoreCase = true) ||
                    location.description.contains(query, ignoreCase = true)
                }
                .sortedBy { it.name }
            
            if (results.isNotEmpty()) {
                return results
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching locations by name", e)
        }
        
        // Fall back to searching in default locations
        return getDefaultLocations()
            .filter { location ->
                location.name.contains(query, ignoreCase = true) ||
                location.address.contains(query, ignoreCase = true) ||
                location.description.contains(query, ignoreCase = true)
            }
            .sortedBy { it.name }
    }
    
    /**
     * Add a new location to the database
     */
    fun addLocation(location: Location) {
        db.collection(LOCATIONS_COLLECTION)
            .add(location)
            .addOnSuccessListener {
                Log.d(TAG, "Location added with ID: ${it.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding location", e)
            }
    }
    
    /**
     * Get default locations for Guelmim
     * This serves as a fallback when database access fails or during testing
     */
    fun getDefaultLocations(): List<Location> {
        return listOf(
            // Educational institutions
            Location(
                id = "school1",
                name = "École Supérieure de Technologie (EST) de Guelmim",
                type = "university",
                latitude = 28.9830,
                longitude = -10.0625,
                address = "Route d'Asrir, Guelmim",
                description = "École d'ingénierie et de technologie"
            ),
            Location(
                id = "school2",
                name = "Faculté Polydisciplinaire de Guelmim",
                type = "university",
                latitude = 28.9870,
                longitude = -10.0595,
                address = "Av. Mohamed VI, Guelmim",
                description = "Université Ibn Zohr"
            ),
            Location(
                id = "school3",
                name = "Lycée Mohammed V",
                type = "school",
                latitude = 28.9845,
                longitude = -10.0550,
                address = "Av. Hassan II, Guelmim",
                description = "Lycée secondaire"
            ),
            Location(
                id = "school4",
                name = "Lycée Technique",
                type = "school",
                latitude = 28.9855,
                longitude = -10.0565,
                address = "Guelmim",
                description = "Lycée technique"
            ),
            
            // Bus stops
            Location(
                id = "stop1",
                name = "Gare Routière de Guelmim",
                type = "bus_stop",
                latitude = 28.9865,
                longitude = -10.0572,
                address = "Route de Tan-Tan, Guelmim",
                description = "Station centrale de bus"
            ),
            Location(
                id = "stop2",
                name = "Arrêt Centre-Ville",
                type = "bus_stop",
                latitude = 28.9875,
                longitude = -10.0582,
                address = "Centre-ville, Guelmim",
                description = "Arrêt de bus du centre-ville"
            ),
            Location(
                id = "stop3",
                name = "Arrêt Hôpital",
                type = "bus_stop",
                latitude = 28.9895,
                longitude = -10.0560,
                address = "Près de l'hôpital, Guelmim",
                description = "Arrêt de bus desservant l'hôpital"
            ),
            
            // Points of interest
            Location(
                id = "poi1",
                name = "Hôpital Provincial de Guelmim",
                type = "hospital",
                latitude = 28.9900,
                longitude = -10.0555,
                address = "Av. Mohammed V, Guelmim",
                description = "Hôpital principal de la ville"
            ),
            Location(
                id = "poi2",
                name = "Souk Guelmim",
                type = "poi",
                latitude = 28.9880,
                longitude = -10.0590,
                address = "Centre-ville, Guelmim",
                description = "Marché traditionnel"
            ),
            Location(
                id = "poi3",
                name = "Place Al Mechouar",
                type = "poi",
                latitude = 28.9870,
                longitude = -10.0575,
                address = "Centre-ville, Guelmim",
                description = "Place centrale de la ville"
            ),
            
            // Notable streets and districts
            Location(
                id = "str1",
                name = "Avenue Mohammed V",
                type = "street",
                latitude = 28.9875,
                longitude = -10.0565,
                address = "Guelmim",
                description = "Artère principale de la ville"
            ),
            Location(
                id = "str2",
                name = "Avenue Hassan II",
                type = "street",
                latitude = 28.9845,
                longitude = -10.0550,
                address = "Guelmim",
                description = "Avenue importante"
            ),
            Location(
                id = "str3",
                name = "Quartier Administratif",
                type = "district",
                latitude = 28.9880,
                longitude = -10.0545,
                address = "Guelmim",
                description = "Zone des bâtiments administratifs"
            ),
            Location(
                id = "str4",
                name = "Quartier Tagadirt",
                type = "district",
                latitude = 28.9825,
                longitude = -10.0585,
                address = "Guelmim",
                description = "Quartier résidentiel"
            ),
            Location(
                id = "str5",
                name = "Boulevard Al Massira",
                type = "street",
                latitude = 28.9855,
                longitude = -10.0600,
                address = "Guelmim",
                description = "Boulevard commercial"
            ),
            
            // Additional landmarks
            Location(
                id = "lnd1",
                name = "Mosquée Mohammed VI",
                type = "landmark",
                latitude = 28.9865,
                longitude = -10.0555,
                address = "Centre-ville, Guelmim",
                description = "Grande mosquée de la ville"
            ),
            Location(
                id = "lnd2",
                name = "Centre Culturel de Guelmim",
                type = "landmark",
                latitude = 28.9850,
                longitude = -10.0570,
                address = "Guelmim",
                description = "Centre d'activités culturelles"
            ),
            Location(
                id = "lnd3",
                name = "Stade Municipal",
                type = "landmark",
                latitude = 28.9840,
                longitude = -10.0610,
                address = "Guelmim",
                description = "Stade sportif"
            ),
            
            // Commercial locations
            Location(
                id = "com1",
                name = "Centre Commercial Jawharat",
                type = "commercial",
                latitude = 28.9860,
                longitude = -10.0585,
                address = "Avenue Mohammed V, Guelmim",
                description = "Centre commercial"
            ),
            Location(
                id = "com2",
                name = "Supermarché Marjane",
                type = "commercial",
                latitude = 28.9910,
                longitude = -10.0535,
                address = "Entrée de la ville, Guelmim",
                description = "Grand supermarché"
            )
        )
    }
} 