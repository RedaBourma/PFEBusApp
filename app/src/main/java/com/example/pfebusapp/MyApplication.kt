package com.example.pfebusapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyApplication : Application() {
    companion object {
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()
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
} 