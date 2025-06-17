package com.example.pfebusapp

import android.app.Application
import android.util.Log
import com.example.pfebusapp.utils.LocationDataImporter
import com.example.pfebusapp.utils.GuelmimDataImporter
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    companion object {
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()
        initializeFirebase()
        initializeLocationData()
    }
    
    private fun initializeFirebase() {
        try {
            Log.d(TAG, "Initializing Firebase...")
            
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps(this).isEmpty()) {
                Log.d(TAG, "No Firebase app found, initializing...")
                FirebaseApp.initializeApp(this)
            } else {
                Log.d(TAG, "Firebase app already initialized")
                val app = FirebaseApp.getInstance()
                val options = app.options
                Log.d(TAG, "Firebase project ID: ${options.projectId}")
                Log.d(TAG, "Firebase application ID: ${options.applicationId}")
            }
            
            Log.d(TAG, "Firebase initialized successfully")
            
            // Check if Firebase Auth is working
            try {
                val auth = FirebaseAuth.getInstance()
                Log.d(TAG, "Firebase Auth initialized: ${auth.currentUser?.uid ?: "No user logged in"}")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Firebase Auth", e)
            }
            
            // Check if Firestore is working
            try {
                val db = FirebaseFirestore.getInstance()
                Log.d(TAG, "Firebase Firestore initialized")
                
                // Test a simple query
                db.collection("test")
                    .limit(1)
                    .get()
                    .addOnSuccessListener {
                        Log.d(TAG, "Firestore test query successful")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Firestore test query failed", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Firestore", e)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }
    }
    
    private fun initializeLocationData() {
        try {
            Log.d(TAG, "Initializing location data...")
            val locationImporter = LocationDataImporter(this)
            
            // Import default locations if needed
            locationImporter.importDefaultLocations { success, count ->
                if (success) {
                    Log.d(TAG, "Successfully imported $count locations")
                    
                    // After basic locations are imported, import more comprehensive Guelmim data
                    importGuelmimData()
                } else {
                    Log.e(TAG, "Failed to import locations")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing location data", e)
        }
    }
    
    private fun importGuelmimData() {
        // Check if data was already imported
        val prefs = getSharedPreferences("import_data", MODE_PRIVATE)
        val importComplete = prefs.getBoolean("import_complete", false)
        val lastImportTime = prefs.getString("last_import_time", null)
        
        // Temporarily force import regardless of previous status
        // if (importComplete) {
        //     Log.d(TAG, "Guelmim data import was already completed on $lastImportTime")
        //     return
        // }
        
        Log.d(TAG, "Starting one-time Guelmim data import...")
        
        // Run in background to avoid blocking the UI
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val importer = GuelmimDataImporter(applicationContext)
                val imported = importer.importAllData { progress, message ->
                    Log.d(TAG, "Import progress: $progress - $message")
                }
                
                Log.d(TAG, "Guelmim data import complete with $imported locations")
                
                // Save the import status regardless of the number of imported locations
                prefs.edit()
                    .putBoolean("import_complete", true)
                    .putString("last_import_time", java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date()))
                    .apply()
                
                Log.d(TAG, "Import marked as complete in preferences")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during Guelmim data import", e)
                
                // Reset last import time on error so it can be retried next time
                prefs.edit()
                    .remove("last_import_time")
                    .apply()
            }
        }
    }
} 