package com.example.pfebusapp

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MyApplication : Application() {
    companion object {
        private const val TAG = "MyApplication"
    }

    override fun onCreate() {
        super.onCreate()
        try {
            Log.d(TAG, "Initializing Firebase...")
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized successfully")
            
            // Check if Firebase Auth is working
            try {
                val auth = FirebaseAuth.getInstance()
                Log.d(TAG, "Firebase Auth initialized: ${auth.currentUser?.uid ?: "No user logged in"}")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing Firebase Auth", e)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }
    }
} 