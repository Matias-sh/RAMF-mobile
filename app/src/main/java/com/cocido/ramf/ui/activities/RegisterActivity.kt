package com.cocido.ramf.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cocido.ramf.R
import com.cocido.ramf.models.LoginResponse
import com.cocido.ramf.models.RegisterRequest
import com.cocido.ramf.network.RetrofitClient
import com.cocido.ramf.utils.AuthManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "RegisterActivity"
    }

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLoginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        Log.d(TAG, "onCreate: RegisterActivity started")

        AuthManager.initialize(this)

        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLoginLink = findViewById(R.id.tvLoginLink)

        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (validateFields(firstName, lastName, email, password)) {
                performRegister(firstName, lastName, email, password)
            }
        }

        tvLoginLink.setOnClickListener {
            goToLoginActivity()
        }
    }

    private fun validateFields(firstName: String, lastName: String, email: String, password: String): Boolean {
        if (firstName.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese su nombre", Toast.LENGTH_SHORT).show()
            return false
        }

        if (lastName.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese su apellido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese su email", Toast.LENGTH_SHORT).show()
            return false
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Por favor, ingrese un email válido", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Por favor, ingrese su contraseña", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun performRegister(firstName: String, lastName: String, email: String, password: String) {
        val authService = RetrofitClient.authService
        val registerRequest = RegisterRequest(firstName, lastName, email, password)

        authService.register(registerRequest).enqueue(object : Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val token = response.body()?.token
                    val user = response.body()?.user

                    if (!token.isNullOrEmpty() && user != null) {
                        Toast.makeText(this@RegisterActivity, "Cuenta creada exitosamente", Toast.LENGTH_SHORT).show()
                        AuthManager.saveUserSession(user, token)
                        goToMainActivity()
                    } else {
                        Toast.makeText(this@RegisterActivity, "Error: Token vacío", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "Datos inválidos. Verifique la información ingresada."
                        409 -> "El email ya está registrado. Use otro email."
                        else -> "Error en registro: ${response.message()}"
                    }
                    Toast.makeText(this@RegisterActivity, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@RegisterActivity, "Error en registro: ${t.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun goToLoginActivity() {
        Log.d(TAG, "goToLoginActivity: Navigating to LoginActivity")
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun goToMainActivity() {
        Log.d(TAG, "goToMainActivity: Navigating to MainActivity")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}