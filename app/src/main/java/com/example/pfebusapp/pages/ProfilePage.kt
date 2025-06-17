package com.example.pfebusapp.pages

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.pfebusapp.AuthViewModel
import com.example.pfebusapp.R
import com.example.pfebusapp.uiComponents.BottomNavigationBar
import com.example.pfebusapp.uiComponents.DatePickerTextField
import com.example.pfebusapp.uiComponents.PhoneNumberTextField
import com.example.pfebusapp.userRepository.RegistredUser
import com.example.pfebusapp.userRepository.UserRepository
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfilePage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // User data state
    var userData by remember { mutableStateOf<RegistredUser?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Edit profile state
    var showEditDialog by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var editNom by remember { mutableStateOf("") }
    var editPrenom by remember { mutableStateOf("") }
    var editDtNaiss by remember { mutableStateOf(Date()) }
    var editTel by remember { mutableStateOf("") }
    
    // Add states for password change
    var showPasswordDialog by remember { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isChangingPassword by remember { mutableStateOf(false) }
    var passwordErrorMessage by remember { mutableStateOf<String?>(null) }
    
    // Format date
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    // Load user data
    LaunchedEffect(authViewModel.getCurrentUser()) {
        isLoading = true
        errorMessage = null
        
        val userId = authViewModel.getCurrentUser()?.uid
        if (userId.isNullOrEmpty()) {
            errorMessage = "Utilisateur non connecté"
            isLoading = false
            return@LaunchedEffect
        }
        
        UserRepository().getUserData(
            userId = userId,
            onSuccess = { user ->
                isLoading = false
                if (user != null) {
                    userData = user
                    
                    // Pre-populate edit fields
                    editNom = user.nom
                    editPrenom = user.prenom
                    editDtNaiss = user.dtNaiss
                    editTel = user.tel
                } else {
                    errorMessage = "Utilisateur non trouvé"
                    Log.d("ProfilePage", "User data is null")
                }
            },
            onFailure = { exception ->
                isLoading = false
                errorMessage = "Échec du chargement des données utilisateur"
                Log.e("ProfilePage", "Failed to get user data", exception)
            }
        )
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Profile header
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userData?.let { "${it.prenom.firstOrNull() ?: ""}${it.nom.firstOrNull() ?: ""}" } ?: "?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = userData?.let { "${it.prenom} ${it.nom}" } ?: "Chargement...",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = userData?.email ?: "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Loading or error state
            if (isLoading) {
                CircularProgressIndicator()
            } else if (errorMessage != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage ?: "Une erreur est survenue",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // User information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Informations personnelles",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Name
                        InfoRow(
                            icon = Icons.Default.Person,
                            label = "Nom complet",
                            value = "${userData?.prenom} ${userData?.nom}"
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Email
                        InfoRow(
                            icon = Icons.Default.Email,
                            label = "Email",
                            value = userData?.email ?: ""
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Date of birth
                        InfoRow(
                            icon = Icons.Default.DateRange,
                            label = "Date de naissance",
                            value = userData?.dtNaiss?.let { dateFormat.format(it) } ?: ""
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // Phone
                        InfoRow(
                            icon = Icons.Default.Phone,
                            label = "Téléphone",
                            value = userData?.tel ?: ""
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Password change button
                        Button(
                            onClick = { 
                                showPasswordDialog = true
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                                passwordErrorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Modifier mon mot de passe")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Modifier mon profil")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Logout button
            OutlinedButton(
                onClick = {
                    authViewModel.signout()
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Se déconnecter")
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // For bottom navigation
        }
        
        // Password Change Dialog
        if (showPasswordDialog) {
            AlertDialog(
                onDismissRequest = { 
                    if (!isChangingPassword) {
                        showPasswordDialog = false
                        passwordErrorMessage = null
                    }
                },
                title = { Text("Modifier le mot de passe") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Current password
                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = { currentPassword = it },
                            label = { Text("Mot de passe actuel") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            enabled = !isChangingPassword,
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        
                        // New password
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("Nouveau mot de passe") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            enabled = !isChangingPassword,
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        
                        // Confirm password
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirmer le mot de passe") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            enabled = !isChangingPassword,
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        
                        if (passwordErrorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = passwordErrorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        if (isChangingPassword) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Validate inputs
                            passwordErrorMessage = null
                            isChangingPassword = true
                            
                            if (currentPassword.isBlank()) {
                                passwordErrorMessage = "Veuillez entrer votre mot de passe actuel"
                                isChangingPassword = false
                                return@Button
                            }
                            
                            if (newPassword.isBlank()) {
                                passwordErrorMessage = "Veuillez entrer un nouveau mot de passe"
                                isChangingPassword = false
                                return@Button
                            }
                            
                            if (newPassword.length < 6) {
                                passwordErrorMessage = "Le mot de passe doit contenir au moins 6 caractères"
                                isChangingPassword = false
                                return@Button
                            }
                            
                            if (newPassword != confirmPassword) {
                                passwordErrorMessage = "Les mots de passe ne correspondent pas"
                                isChangingPassword = false
                                return@Button
                            }
                            
                            coroutineScope.launch {
                                try {
                                    val user = authViewModel.getCurrentUser()
                                    if (user == null || userData?.email.isNullOrEmpty()) {
                                        passwordErrorMessage = "Utilisateur non connecté"
                                        isChangingPassword = false
                                        return@launch
                                    }
                                    
                                    // Re-authenticate the user
                                    val credential = EmailAuthProvider.getCredential(userData!!.email, currentPassword)
                                    
                                    user.reauthenticate(credential)
                                        .addOnSuccessListener {
                                            // Re-authentication successful, now change password
                                            user.updatePassword(newPassword)
                                                .addOnSuccessListener {
                                                    isChangingPassword = false
                                                    showPasswordDialog = false
                                                    
                                                    // Show success message
                                                    Toast.makeText(
                                                        context,
                                                        "Mot de passe modifié avec succès",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .addOnFailureListener { exception ->
                                                    Log.e("ProfilePage", "Failed to update password", exception)
                                                    passwordErrorMessage = "Échec de la mise à jour du mot de passe: ${exception.message ?: "Erreur inconnue"}"
                                                    isChangingPassword = false
                                                }
                                        }
                                        .addOnFailureListener { exception ->
                                            Log.e("ProfilePage", "Failed to re-authenticate", exception)
                                            passwordErrorMessage = "Mot de passe actuel incorrect"
                                            isChangingPassword = false
                                        }
                                } catch (e: Exception) {
                                    Log.e("ProfilePage", "Exception during password change", e)
                                    passwordErrorMessage = "Erreur: ${e.message ?: "Erreur inconnue"}"
                                    isChangingPassword = false
                                }
                            }
                        },
                        enabled = !isChangingPassword && currentPassword.isNotEmpty() && 
                                newPassword.isNotEmpty() && confirmPassword.isNotEmpty()
                    ) {
                        Text("Modifier")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            if (!isChangingPassword) {
                                showPasswordDialog = false
                                passwordErrorMessage = null
                            }
                        },
                        enabled = !isChangingPassword
                    ) {
                        Text("Annuler")
                    }
                }
            )
        }
        
        // Edit Profile Dialog
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { 
                    if (!isSaving) {
                        showEditDialog = false
                        errorMessage = null
                    }
                },
                title = { Text("Modifier le profil") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = editNom,
                            onValueChange = { editNom = it },
                            label = { Text("Nom") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            enabled = !isSaving
                        )
                        
                        OutlinedTextField(
                            value = editPrenom,
                            onValueChange = { editPrenom = it },
                            label = { Text("Prénom") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            enabled = !isSaving
                        )
                        
                        DatePickerTextField(
                            modifier = Modifier.fillMaxWidth(),
                            label = "Date de naissance",
                            initialDate = editDtNaiss,
                            onDateSelected = { editDtNaiss = it },
                            enabled = !isSaving
                        )
                        
                        PhoneNumberTextField(
                            value = editTel,
                            onValueChange = { editTel = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Téléphone",
                            enabled = !isSaving
                        )
                        
                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        if (isSaving) {
                            Spacer(modifier = Modifier.height(8.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Implement profile update functionality
                            isSaving = true
                            errorMessage = null
                            
                            val userId = authViewModel.getCurrentUser()?.uid
                            if (userId.isNullOrEmpty()) {
                                errorMessage = "Utilisateur non connecté"
                                isSaving = false
                                return@Button
                            }
                            
                            coroutineScope.launch {
                                try {
                                    // Check required fields
                                    if (editNom.isBlank() || editPrenom.isBlank() || editTel.isBlank()) {
                                        errorMessage = "Veuillez remplir tous les champs obligatoires"
                                        isSaving = false
                                        return@launch
                                    }
                                    
                                    UserRepository().updateUserData(
                                        userId = userId,
                                        nom = editNom.trim(),
                                        prenom = editPrenom.trim(),
                                        dtNaiss = editDtNaiss,
                                        tel = editTel.trim(),
                                        onSuccess = {
                                            // Update local user data
                                            userData = userData?.copy(
                                                nom = editNom.trim(),
                                                prenom = editPrenom.trim(),
                                                dtNaiss = editDtNaiss,
                                                tel = editTel.trim()
                                            )
                                            isSaving = false
                                            showEditDialog = false
                                            errorMessage = null
                                        },
                                        onFailure = { exception ->
                                            Log.e("ProfilePage", "Failed to update profile", exception)
                                            errorMessage = "Échec de la mise à jour : ${exception.message ?: "Erreur inconnue"}"
                                            isSaving = false
                                        }
                                    )
                                } catch (e: Exception) {
                                    Log.e("ProfilePage", "Exception during profile update", e)
                                    errorMessage = "Erreur : ${e.message ?: "Erreur inconnue"}"
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving && editNom.isNotEmpty() && editPrenom.isNotEmpty() && editTel.isNotEmpty()
                    ) {
                        Text("Enregistrer")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { 
                            if (!isSaving) {
                                showEditDialog = false
                                errorMessage = null
                            }
                        },
                        enabled = !isSaving
                    ) {
                        Text("Annuler")
                    }
                }
            )
        }
        
        // Bottom navigation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .zIndex(2f)
        ) {
            BottomNavigationBar(navController = navController)
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}