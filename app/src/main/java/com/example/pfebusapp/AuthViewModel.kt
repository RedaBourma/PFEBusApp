package com.example.pfebusapp

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

// TODO: anonymous auth, error messages (fields)
class AuthViewModel : ViewModel() {

   private val auth : FirebaseAuth = FirebaseAuth.getInstance()

   private val _authState = MutableLiveData<AuthState>()
   val authState : LiveData<AuthState> = _authState

   val currentUserId: String?
      get() = auth.currentUser?.uid

   fun getCurrentUser(): FirebaseUser?{
      if (auth.currentUser != null) {
         return auth.currentUser;
      }
      return null
   }

   init {
       checkAuthstatus()
   }

   fun checkAuthstatus(){
      val user = auth.currentUser
      if(user == null) {
         _authState.value = AuthState.Unauthenticated
      }else{
         if(user.isAnonymous){
            _authState.value = AuthState.AnonymouslyAuthenticated
         }else{
            _authState.value = AuthState.Authenticated
         }
      }
   }

   fun login(email: String, password: String){
      if(email.isEmpty() || password.isEmpty()){
         _authState.value = AuthState.Error("Email and password cannot be empty")
         return
      }
      _authState.value = AuthState.Loading
      auth.signInWithEmailAndPassword(email, password)
         .addOnCompleteListener{ task ->
            if (task.isSuccessful){
               _authState.value = AuthState.Authenticated
            }else{
               _authState.value = AuthState.Error(task.exception?.message ?: "Unknown error")
            }
         }
   }

   fun signup(email: String, password: String, onSuccess: () -> Unit) {
      if(email.isEmpty() || password.isEmpty()){
         _authState.value = AuthState.Error("Email and password cannot be empty")
         return
      }
      _authState.value = AuthState.Loading
      auth.createUserWithEmailAndPassword(email, password)
         .addOnCompleteListener{ task ->
            if (task.isSuccessful){
               //TODO: create user in database
               _authState.value = AuthState.Authenticated
               onSuccess()
            }else{
               _authState.value = AuthState.Error(task.exception?.message ?: "Unknown error")
            }
      }
   }

   fun signInAnonymously() {
//      _authState.value = AuthState.Loading

      auth.signInAnonymously()
         .addOnCompleteListener { task ->
           if (task.isSuccessful){
              _authState.value = AuthState.AnonymouslyAuthenticated
           }else{
              _authState.value = AuthState.Error(task.exception?.message?: "Anonymous sign in failed")
           }
         }

   }

   //to implement later
   fun linkAnonymousAccountWithCredentials(email: String, password: String){

   }

   fun getAnonymousUserId(): String? {
     val user = auth.currentUser
      if(user != null && user.isAnonymous){
         return user.uid
      }
      return null
   }

   fun signout(){
      auth.signOut()
      _authState.value = AuthState.Unauthenticated
   }
}

sealed class AuthState{
   object Authenticated : AuthState()
   object AnonymouslyAuthenticated : AuthState()
   object Unauthenticated : AuthState()
   object Loading : AuthState()
   data class Error(val message : String) : AuthState()
}