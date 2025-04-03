package com.example.pfebusapp.pages

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.pfebusapp.AuthState
import com.example.pfebusapp.AuthViewModel
import com.example.pfebusapp.uiComponents.DatePickerTextField
import com.example.pfebusapp.uiComponents.PhoneNumberTextField
import com.example.pfebusapp.userRepository.UserRepository
import kotlinx.coroutines.runBlocking
import java.util.Date

@Composable
fun SignupPage(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var passwordConf by remember { mutableStateOf("") }
    var dtNaiss by remember { mutableStateOf(Date()) }
    var tel by remember { mutableStateOf("") }

    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    LaunchedEffect(authState.value) {
        when(authState.value){
            is AuthState.Authenticated -> navController.navigate("home")
            is AuthState.Error -> Toast.makeText(context,
                (authState.value as AuthState.Error).message, Toast.LENGTH_SHORT).show()
            else -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ){
        Text(text = "SignUp Page")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            singleLine = true,
            value = name,
            onValueChange = {
                name = it
            },
            label = {
                Text(text = "Name")
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            singleLine = true,
            value = lastName,
            onValueChange = {
                lastName = it
            },
            label = {
                Text(text = "Last Name")
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            singleLine = true,
            value = email,
            onValueChange = {
                email = it
            },
            label = {
                Text(text = "Email")
            }
        )
        //TODO: password visibility icon
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            singleLine = true,
            value = password,
            onValueChange = {
                password = it
            },
            label = {
                Text(text = "Password")
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            singleLine = true,
            value = passwordConf,
            onValueChange = {
                passwordConf = it
            },
            label = {
                Text(text = "Confirm Password")
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        DatePickerTextField(modifier = Modifier, label = "date naissance",
            onDateSelected = { date ->
                dtNaiss = date
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        PhoneNumberTextField(
            value = tel,
            onValueChange = {
                tel = it
            },
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            try {
                authViewModel.signup(email, password){
                    val userRepo = UserRepository()
                    userRepo.createUser(context, email, name, lastName, dtNaiss, tel)
                }
            }catch (e : Exception){
               Log.d("zmla", e.message.toString())
            }

//            createUser(email, name, lastName, dtNaiss, tel)
            },
            enabled = authState.value != AuthState.Loading
        ){
            Text(text = "SignUp")
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(
            onClick = {
                navController.navigate("Login")
            }) {
            Text(text = "Already have an account, Login")
        }
    }
}
