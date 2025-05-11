package com.example.pfebusapp.firebase

import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore


class FirestoreHelper {
    private val db: FirebaseFirestore = Firebase.firestore


    fun getDb(): FirebaseFirestore {
        return db
    }

    companion object {
        const val BUS_COLLECTION = "bus"
        const val USERS_COLLECTION = "users"
    }

}