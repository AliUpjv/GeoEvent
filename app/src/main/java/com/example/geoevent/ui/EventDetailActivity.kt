package com.example.geoevent.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.geoevent.R
import com.example.geoevent.data.FirebaseAuthRepository
import com.example.geoevent.data.FirestoreEventRepository
import com.example.geoevent.domain.AuthRepository
import com.example.geoevent.domain.EventRepository
import kotlinx.coroutines.launch

class EventDetailActivity : AppCompatActivity() {

    private val eventRepo: EventRepository = FirestoreEventRepository()
    private val authRepo: AuthRepository = FirebaseAuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_detail)

        val eventId = intent.getStringExtra("eventId") ?: return
        val title = intent.getStringExtra("title") ?: ""
        val description = intent.getStringExtra("description") ?: ""
        val imageBase64 = intent.getStringExtra("imageUrl") ?: ""
        val userEmail = intent.getStringExtra("userEmail") ?: ""
        val userRole = intent.getStringExtra("userRole") ?: "user"
        val eventUserId = intent.getStringExtra("userId") ?: ""
        val likes = intent.getIntExtra("likes", 0)

        findViewById<TextView>(R.id.tvTitle).text = title
        findViewById<TextView>(R.id.tvDescription).text = description
        findViewById<TextView>(R.id.tvAuthor).text = "Par $userEmail"
        findViewById<TextView>(R.id.tvLikes).text = "$likes ❤️"

        if (imageBase64.isNotEmpty()) {
            try {
                val bytes = Base64.decode(imageBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                findViewById<ImageView>(R.id.ivEventImage).setImageBitmap(bitmap)
            } catch (e: Exception) { e.printStackTrace() }
        }

        val currentUid = authRepo.getCurrentUserId()
        val canDelete = userRole == "admin" || currentUid == eventUserId
        findViewById<Button>(R.id.btnDelete).visibility =
            if (canDelete) View.VISIBLE else View.GONE

        findViewById<Button>(R.id.btnLike).setOnClickListener {
            lifecycleScope.launch {
                try {
                    eventRepo.likeEvent(eventId)
                    val current = findViewById<TextView>(R.id.tvLikes).text
                        .toString().replace(" ❤️", "").toIntOrNull() ?: 0
                    findViewById<TextView>(R.id.tvLikes).text = "${current + 1} ❤️"
                    Toast.makeText(this@EventDetailActivity, "Aimé !", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this@EventDetailActivity, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.btnDelete).setOnClickListener {
            lifecycleScope.launch {
                try {
                    eventRepo.deleteEvent(eventId)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@EventDetailActivity, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish() // referme l'écran et revient à la carte
        }
    }
}