package com.example.geoevent.ui

import com.example.geoevent.domain.AuthRepository
import com.example.geoevent.data.FirebaseAuthRepository
import com.example.geoevent.MapActivity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.geoevent.R
import kotlinx.coroutines.launch

class AuthActivity : AppCompatActivity() {
    private val authRepo: AuthRepository = FirebaseAuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (authRepo.getCurrentUserId() != null) { goToMap(); return }
        setContentView(R.layout.activity_auth)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail).text.toString()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()
            lifecycleScope.launch {
                try { authRepo.login(email, password); goToMap() }
                catch (e: Exception) {
                    Toast.makeText(this@AuthActivity,
                        "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail).text.toString()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()
            lifecycleScope.launch {
                try { authRepo.register(email, password); goToMap() }
                catch (e: Exception) {
                    Toast.makeText(this@AuthActivity,
                        "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun goToMap() {
        startActivity(Intent(this, MapActivity::class.java))
        finish()
    }
}