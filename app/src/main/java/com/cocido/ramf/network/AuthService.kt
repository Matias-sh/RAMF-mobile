package com.cocido.ramf.network

import com.cocido.ramf.models.LoginRequest
import com.cocido.ramf.models.LoginResponse
import com.cocido.ramf.models.RegisterRequest
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AuthService {
    @POST("auth/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
    
    @POST("auth/register")
    fun register(@Body request: RegisterRequest): Call<LoginResponse>
    
    @POST("auth/logout")
    fun logout(@Header("Authorization") token: String): Call<Void>
    
    @POST("auth/refresh")
    fun refreshToken(@Header("Authorization") token: String): Call<LoginResponse>
    
    @POST("auth/google")
    fun googleLogin(@Body googleToken: Map<String, String>): Call<LoginResponse>
}
