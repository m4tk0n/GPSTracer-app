package com.example.cycletracker.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.cycletracker.R
import com.example.cycletracker.SessionManager
import com.example.cycletracker.network.LoginRequest
import com.example.cycletracker.network.RetrofitClient
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.isLoggedIn(this)) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val tvError = findViewById<TextView>(R.id.tvError)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                tvError.text = "Vyplň e-mail i heslo"
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            tvError.text = ""

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.api.login(LoginRequest(email, password))
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        SessionManager.saveSession(this@LoginActivity, body.token, body.username)
                        goToMain()
                    } else {
                        tvError.text = when (response.code()) {
                            401 -> "Nesprávný e-mail nebo heslo"
                            403 -> "Účet ještě není ověřený (zkontroluj e-mail)"
                            else -> "Přihlášení se nepovedlo (${response.code()})"
                        }
                        btnLogin.isEnabled = true
                    }
                } catch (e: Exception) {
                    tvError.text = "Nepodařilo se spojit se serverem"
                    btnLogin.isEnabled = true
                }
            }
        }
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
