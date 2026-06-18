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
// ce fichier permet de detecter les clicks sur les boutons et les champs
//et recupere les données entrées par l'utilisateur puis appelle firebaseAuthRepository pour manipuler les données et les envoyer
class AuthActivity : AppCompatActivity() {
    private val authRepo: AuthRepository = FirebaseAuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si une session est déjà ouverte (l'utilisateur ne s'est pas déconnecté
        // depuis la dernière fois), on récupère quand même son rôle à jour dans
        // Firestore avant d'aller sur la carte, sinon le rôle admin ne serait
        // jamais transmis pour les sessions déjà ouvertes.
        val currentUid = authRepo.getCurrentUserId()
        if (currentUid != null) {
            lifecycleScope.launch {
                val role = authRepo.getUserRole(currentUid)
                goToMap(role)
            }
            return
        }

        setContentView(R.layout.activity_auth)

        findViewById<Button>(R.id.btnLogin).setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail).text.toString()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()
            lifecycleScope.launch {
                try {
                    val role = authRepo.login(email, password)
                    goToMap(role)
                } catch (e: Exception) {
                    Toast.makeText(this@AuthActivity,
                        "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        findViewById<Button>(R.id.btnRegister).setOnClickListener {
            val email = findViewById<EditText>(R.id.etEmail).text.toString()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()
            lifecycleScope.launch {
                try {
                    val role = authRepo.register(email, password)
                    goToMap(role)
                } catch (e: Exception) {
                    Toast.makeText(this@AuthActivity,
                        "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // On transmet le rôle à MapActivity via l'intent : c'est ce qui permettra
    // au bouton "Supprimer" d'EventDetailActivity de s'afficher pour un admin
    // même sur les événements créés par quelqu'un d'autre.
    private fun goToMap(role: String) {
        val intent = Intent(this, MapActivity::class.java)
        intent.putExtra("userRole", role)
        startActivity(intent)
        finish()
    }
}