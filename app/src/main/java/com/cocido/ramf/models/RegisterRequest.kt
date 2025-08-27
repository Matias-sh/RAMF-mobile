package com.cocido.ramf.models

data class RegisterRequest(
    val name: String,
    val lastName: String,
    val email: String,
    val password: String
)