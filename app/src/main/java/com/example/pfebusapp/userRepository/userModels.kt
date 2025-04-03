package com.example.pfebusapp.userRepository

import java.util.Date

data class RegistredUser(
    val id: String,
    val email: String,
    val nom: String,
    val prenom: String,
    val dtNaiss: Date,
    val tel: String
){
    constructor(): this("", "", "","",Date(),"")
}

data class AnonymousUser(
    val TemporaryID: String,
)