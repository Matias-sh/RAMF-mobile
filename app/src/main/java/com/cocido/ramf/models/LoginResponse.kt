package com.cocido.ramf.models

data class LoginResponse(
    val token: String,
    val user: User // Aquí almacenamos la información del usuario
)
