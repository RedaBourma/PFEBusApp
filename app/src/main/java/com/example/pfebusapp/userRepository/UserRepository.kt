package com.example.pfebusapp.userRepository

import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.example.pfebusapp.AuthState
import com.example.pfebusapp.AuthViewModel
import com.example.pfebusapp.firebase.FirestoreHelper
import com.google.android.gms.tasks.Task
import com.google.api.Context
import com.google.firebase.Firebase
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.database
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class UserRepository {

    private val firestoreHelper = FirestoreHelper()
    private val db = firestoreHelper.getDb()

    fun createUser(
        context: android.content.Context,
        email: String,
        name: String,
        prenom: String,
        dtNaiss: Date,
        tel: String
    ){
        val user = AuthViewModel().getCurrentUser()
        val userData = RegistredUser(
            id = user?.uid ?: throw IllegalStateException("User ID is null"),
            email = email,
            nom = name,
            prenom = prenom,
            dtNaiss = dtNaiss,
            tel = tel,
        )
        try {
            db.collection(FirestoreHelper.USERS_COLLECTION)
                .add(userData)
//                .await()
        }catch (e: Exception){
                Log.d("user sa7bi", e.message.toString())
                Toast.makeText(context, "Error: User not created",
                    Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUserInfo(userId: String): Task<QuerySnapshot> {
        return db.collection(FirestoreHelper.USERS_COLLECTION)
//            .document(userId)
            .whereEqualTo("id", userId)
            .get()
    }

    fun getUserData(userId: String, onSuccess: (RegistredUser?) -> Unit, onFailure: (Exception) -> Unit){
        getUserInfo(userId)
            .addOnSuccessListener { snapshot ->
                val user = snapshot.toObjects(RegistredUser::class.java).firstOrNull()
                if (user != null){
                    onSuccess(user)
                }else{
                    Log.d("userrep", "user not found")
                    onSuccess(null)
                }
            }
            .addOnFailureListener{e ->
                Log.e("userrep", "user not found haha", e)
                onFailure(e)
            }
    }
    
    fun updateUserData(
        userId: String,
        nom: String,
        prenom: String,
        dtNaiss: Date,
        tel: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        // First get the document ID based on the user ID
        db.collection(FirestoreHelper.USERS_COLLECTION)
            .whereEqualTo("id", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onFailure(Exception("User document not found"))
                    return@addOnSuccessListener
                }
                
                // Update the document with the new data
                val docId = snapshot.documents.first().id
                val updates = hashMapOf(
                    "nom" to nom,
                    "prenom" to prenom,
                    "dtNaiss" to dtNaiss,
                    "tel" to tel
                )
                
                db.collection(FirestoreHelper.USERS_COLLECTION)
                    .document(docId)
                    .update(updates as Map<String, Any>)
                    .addOnSuccessListener {
                        Log.d("UserRepository", "User data updated successfully")
                        onSuccess()
                    }
                    .addOnFailureListener { e ->
                        Log.e("UserRepository", "Error updating user data", e)
                        onFailure(e)
                    }
            }
            .addOnFailureListener { e ->
                Log.e("UserRepository", "Error finding user document", e)
                onFailure(e)
            }
    }
}