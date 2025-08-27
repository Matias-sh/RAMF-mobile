package com.cocido.ramf.ui.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.cocido.ramf.R
import com.cocido.ramf.utils.AuthManager

class SplashActivity : AppCompatActivity() {
    
    companion object {
        private const val SPLASH_DELAY = 2000L // 2 seconds
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        // Initialize AuthManager
        AuthManager.initialize(this)
        
        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DELAY)
    }
    
    private fun navigateToNextScreen() {
        val intent = if (AuthManager.isUserLoggedIn()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, LoginActivity::class.java)
        }
        
        startActivity(intent)
        finish()
        
        // Add smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}