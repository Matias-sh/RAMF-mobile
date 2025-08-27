package com.cocido.ramf.ui.activities

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.cocido.ramf.R
import com.cocido.ramf.utils.AuthManager

class UserProfileActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var profileImage: ImageView
    private lateinit var tvUserName: TextView
    private lateinit var tvUserEmail: TextView
    private lateinit var tvUserRole: TextView
    private lateinit var tvUserId: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        initViews()
        setupListeners()
        loadUserData()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        profileImage = findViewById(R.id.profileImage)
        tvUserName = findViewById(R.id.tvUserName)
        tvUserEmail = findViewById(R.id.tvUserEmail)
        tvUserRole = findViewById(R.id.tvUserRole)
        tvUserId = findViewById(R.id.tvUserId)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadUserData() {
        val user = AuthManager.getCurrentUser()
        user?.let {
            tvUserName.text = "${it.firstName} ${it.lastName}"
            tvUserEmail.text = it.email
            tvUserRole.text = it.role
            tvUserId.text = "Correo: ${it.email}"
            
            Glide.with(this)
                .load(it.avatar)
                .circleCrop()
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .into(profileImage)
        }
    }
}