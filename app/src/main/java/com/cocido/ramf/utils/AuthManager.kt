package com.cocido.ramf.utils

import android.content.Context
import android.content.SharedPreferences
// import androidx.security.crypto.EncryptedSharedPreferences
// import androidx.security.crypto.MasterKey
import com.cocido.ramf.models.User
import com.cocido.ramf.network.RetrofitClient
import com.google.gson.Gson

object AuthManager {
    private const val PREFS_NAME = "auth_prefs_encrypted"
    private const val TOKEN_KEY = "auth_token"
    private const val USER_KEY = "user_data"
    private const val LOGIN_TIMESTAMP_KEY = "login_timestamp"
    private const val TOKEN_EXPIRY_HOURS = 24L
    
    private var encryptedSharedPref: SharedPreferences? = null
    
    fun initialize(context: Context) {
        // Usando SharedPreferences normal por ahora
        encryptedSharedPref = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun saveUserSession(user: User, token: String) {
        val userJson = Gson().toJson(user)
        
        encryptedSharedPref?.edit()?.apply {
            putString(TOKEN_KEY, token)
            putString(USER_KEY, userJson)
            putLong(LOGIN_TIMESTAMP_KEY, System.currentTimeMillis())
            apply()
        }
        
        // Configurar token en RetrofitClient
        RetrofitClient.setAuthToken(token)
    }
    
    fun getAuthToken(): String? {
        return encryptedSharedPref?.getString(TOKEN_KEY, null)
    }
    
    fun getCurrentUser(): User? {
        val userJson = encryptedSharedPref?.getString(USER_KEY, null)
        return if (userJson != null) {
            try {
                Gson().fromJson(userJson, User::class.java)
            } catch (e: Exception) {
                null
            }
        } else null
    }
    
    fun isUserLoggedIn(): Boolean {
        val token = getAuthToken()
        val user = getCurrentUser()
        val loginTimestamp = encryptedSharedPref?.getLong(LOGIN_TIMESTAMP_KEY, 0L) ?: 0L
        
        // Si no hay usuario, no está logueado
        if (user == null) return false
        
        // Si no hay token pero hay usuario, permitir (caso Google Sign-In)
        if (token.isNullOrEmpty()) return true
        
        // Verificar si el token ha expirado (solo si hay token)
        val currentTime = System.currentTimeMillis()
        val tokenAge = currentTime - loginTimestamp
        val tokenExpiryTime = TOKEN_EXPIRY_HOURS * 60 * 60 * 1000 // 24 horas en milisegundos
        
        return tokenAge < tokenExpiryTime
    }
    
    fun logout() {
        encryptedSharedPref?.edit()?.apply {
            remove(TOKEN_KEY)
            remove(USER_KEY)
            remove(LOGIN_TIMESTAMP_KEY)
            apply()
        }
        
        // Limpiar token de RetrofitClient
        RetrofitClient.setAuthToken(null)
    }
    
    fun refreshTokenIfNeeded(): Boolean {
        // Aquí podrías implementar la lógica para refrescar el token
        // si tu backend lo soporta
        return false
    }
}