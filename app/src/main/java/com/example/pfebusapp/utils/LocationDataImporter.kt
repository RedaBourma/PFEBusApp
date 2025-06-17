package com.example.pfebusapp.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.pfebusapp.busRepository.RouteLocationsRepository
import com.example.pfebusapp.firebase.FirestoreHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Utility class to import location data into Firestore
 * This is used to load locations for the first time or refresh the database
 */
class LocationDataImporter(private val context: Context) {
    private val TAG = "LocationDataImporter"
    private val firestoreHelper = FirestoreHelper()
    private val db = firestoreHelper.getDb()
    
    // Hardcoded location data to import into Firestore
    private val defaultLocations = listOf(
        // Cities
        mapOf(
            "name" to "Guelmim, Morocco",
            "type" to "city",
            "latitude" to 28.9865,
            "longitude" to -10.0572,
            "city" to "Guelmim",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Bouzakaren, Morocco",
            "type" to "city",
            "latitude" to 29.1053,
            "longitude" to -10.0122,
            "city" to "Bouzakaren",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Taghjijt, Morocco",
            "type" to "city",
            "latitude" to 29.1126,
            "longitude" to -9.3759,
            "city" to "Taghjijt",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Asrir, Morocco",
            "type" to "village",
            "latitude" to 28.9442,
            "longitude" to -10.1494,
            "city" to "Asrir",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Targa Wassay, Morocco",
            "type" to "village",
            "latitude" to 28.8900,
            "longitude" to -10.1800,
            "city" to "Targa Wassay",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Fask, Morocco",
            "type" to "village",
            "latitude" to 28.8609,
            "longitude" to -10.2497,
            "city" to "Fask",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Tighmert, Morocco",
            "type" to "village",
            "latitude" to 29.0600,
            "longitude" to -10.1400,
            "city" to "Tighmert",
            "region" to "Guelmim-Oued Noun"
        ),
        
        // Neighborhoods in Guelmim
        mapOf(
            "name" to "Al Masira, Guelmim",
            "type" to "neighborhood",
            "latitude" to 28.9805,
            "longitude" to -10.0518,
            "city" to "Guelmim",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Tagnout, Guelmim",
            "type" to "neighborhood",
            "latitude" to 28.9709,
            "longitude" to -10.0688,
            "city" to "Guelmim",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Hay Rahma, Guelmim",
            "type" to "neighborhood",
            "latitude" to 28.9954,
            "longitude" to -10.0613,
            "city" to "Guelmim",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Centre Ville, Guelmim",
            "type" to "neighborhood",
            "latitude" to 28.9885,
            "longitude" to -10.0572,
            "city" to "Guelmim",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Hay El Wahda, Guelmim",
            "type" to "neighborhood",
            "latitude" to 28.9743,
            "longitude" to -10.0436,
            "city" to "Guelmim",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "El Amal, Guelmim",
            "type" to "neighborhood",
            "latitude" to 28.9912,
            "longitude" to -10.0611,
            "city" to "Guelmim",
            "region" to "Guelmim-Oued Noun"
        ),
        
        // Other towns in the region
        mapOf(
            "name" to "Abaynou, Morocco",
            "type" to "village",
            "latitude" to 29.0998,
            "longitude" to -9.6551,
            "city" to "Abaynou",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Ifrane Atlas Saghir, Morocco",
            "type" to "village",
            "latitude" to 29.3299,
            "longitude" to -10.1977,
            "city" to "Ifrane Atlas Saghir",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Laqsabi-Tagoust, Morocco",
            "type" to "village",
            "latitude" to 28.9964,
            "longitude" to -9.8428,
            "city" to "Laqsabi-Tagoust",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Sidi Ifni, Morocco",
            "type" to "city",
            "latitude" to 29.3798,
            "longitude" to -10.1726,
            "city" to "Sidi Ifni",
            "region" to "Guelmim-Oued Noun"
        ),
        mapOf(
            "name" to "Tan-Tan, Morocco",
            "type" to "city",
            "latitude" to 28.4381,
            "longitude" to -11.1329,
            "city" to "Tan-Tan",
            "region" to "Tan-Tan"
        ),
        mapOf(
            "name" to "Assa, Morocco",
            "type" to "city",
            "latitude" to 28.6091,
            "longitude" to -9.4284,
            "city" to "Assa",
            "region" to "Assa-Zag"
        ),
        mapOf(
            "name" to "Zag, Morocco",
            "type" to "city",
            "latitude" to 28.1341,
            "longitude" to -9.3317,
            "city" to "Zag",
            "region" to "Assa-Zag"
        )
    )
    
    /**
     * Import all default locations into Firestore
     * This will add all hardcoded locations to the database
     */
    fun importDefaultLocations(onComplete: (success: Boolean, count: Int) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting import of ${defaultLocations.size} locations")
                
                // Check if collection exists and has data
                val existingDocs = db.collection("locations")
                    .limit(1)
                    .get()
                    .await()
                
                if (!existingDocs.isEmpty) {
                    Log.d(TAG, "Location collection already has data")
                    // If you want to clear and reimport, uncomment this block
                    /*
                    val allDocs = db.collection("locations")
                        .get()
                        .await()
                    
                    for (doc in allDocs) {
                        db.collection("locations")
                            .document(doc.id)
                            .delete()
                            .await()
                    }
                    Log.d(TAG, "Deleted ${allDocs.size()} existing locations")
                    */
                }
                
                var successCount = 0
                
                // Add all locations
                for (location in defaultLocations) {
                    try {
                        db.collection("locations")
                            .add(location)
                            .await()
                        successCount++
                    } catch (e: Exception) {
                        Log.e(TAG, "Error adding location: ${location["name"]}", e)
                    }
                }
                
                Log.d(TAG, "Successfully imported $successCount locations")
                
                // Update UI on main thread
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(successCount > 0, successCount)
                    Toast.makeText(
                        context,
                        "Imported $successCount locations successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error importing locations", e)
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    onComplete(false, 0)
                    Toast.makeText(
                        context,
                        "Failed to import locations: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
} 