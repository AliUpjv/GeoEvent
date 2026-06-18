package com.example.geoevent.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.geoevent.R
import com.example.geoevent.data.FirebaseAuthRepository
import com.example.geoevent.data.FirestoreEventRepository
import com.example.geoevent.domain.AuthRepository
import com.example.geoevent.domain.Event
import com.example.geoevent.domain.EventRepository
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class AddEventActivity : AppCompatActivity() {

    private val eventRepo: EventRepository = FirestoreEventRepository()
    private val authRepo: AuthRepository = FirebaseAuthRepository()
    private var selectedImageBase64: String = ""
    private var currentLat = 48.8566
    private var currentLng = 2.3522

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = uriToBitmap(it)
            if (bitmap != null) {
                selectedImageBase64 = bitmapToBase64(bitmap)
                findViewById<ImageView>(R.id.ivPreview).setImageBitmap(bitmap)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_event)

        currentLat = intent.getDoubleExtra("lat", 48.8566)
        currentLng = intent.getDoubleExtra("lng", 2.3522)
        findViewById<TextView>(R.id.tvPosition).text =
            "Position : %.4f, %.4f".format(currentLat, currentLng)

        findViewById<Button>(R.id.btnPickImage).setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        findViewById<Button>(R.id.btnSave).setOnClickListener { saveEvent() }
        findViewById<Button>(R.id.btnCancel).setOnClickListener {
            finish() // ferme l'écran et retourne à la carte
        }
        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish() // retourne à la carte sans publier l'événement
        }
    }

    private fun saveEvent() {
        val title = findViewById<EditText>(R.id.etTitle).text.toString().trim()
        val description = findViewById<EditText>(R.id.etDescription).text.toString().trim()
        if (title.isBlank()) {
            Toast.makeText(this, "Le titre est obligatoire", Toast.LENGTH_SHORT).show()
            return
        }
        lifecycleScope.launch {
            try {
                val event = Event(
                    title = title,
                    description = description,
                    lat = currentLat,
                    lng = currentLng,
                    imageUrl = selectedImageBase64,
                    userId = authRepo.getCurrentUserId() ?: "",
                    userEmail = authRepo.getCurrentUserEmail() ?: ""
                )
                eventRepo.addEvent(event)
                Toast.makeText(this@AddEventActivity, "Événement publié !", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(this@AddEventActivity, "Erreur : ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val original = BitmapFactory.decodeStream(inputStream)
            Bitmap.createScaledBitmap(original, 800, 600, true)
        } catch (e: Exception) { null }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }
}