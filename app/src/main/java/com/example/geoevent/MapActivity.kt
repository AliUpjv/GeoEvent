package com.example.geoevent

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.geoevent.data.OsmMapProvider
import com.example.geoevent.domain.MapProvider
import com.example.geoevent.ui.AddEventActivity
import com.example.geoevent.ui.EventViewModel
import com.example.geoevent.util.ConnectivityReceiver
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import com.example.geoevent.ui.AuthActivity
import com.example.geoevent.data.FirebaseAuthRepository
import com.example.geoevent.domain.Event
import com.example.geoevent.domain.MapMarkerItem
import com.example.geoevent.ui.EventDetailActivity
class MapActivity : AppCompatActivity() {
    /* c'est le fichier qui gere les fonctions de l'application en ce qui concerne la map*/

    private val mapProvider: MapProvider = OsmMapProvider()
    private val viewModel: EventViewModel by viewModels()
    private lateinit var locationManager: LocationManager
    private lateinit var connectivityReceiver: ConnectivityReceiver
    private var userLat = 48.8566
    private var userLng = 2.3522

    // Dernier point tapé sur la carte par l'utilisateur (sert au bouton + pour
    // créer un event à cet endroit plutôt qu'à sa position GPS courante).
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null

    // Mémorise la dernière liste reçue de Firestore pour pouvoir retrouver
    // l'event complet à partir de son id quand on clique sur son marker
    // (setEventMarkers ne renvoie que l'id, pas l'objet Event en entier).
    private var latestEvents: List<Event> = emptyList()

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startLocationUpdates() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapProvider.initialize(this)
        mapProvider.showMap(findViewById(R.id.mapContainer))
        mapProvider.centerOn(userLat, userLng, 12.0)
        checkLocationPermission()
        setupConnectivityReceiver()

        // Le rôle est transmis par AuthActivity au login (ou récupéré depuis Firestore
        // si la session était déjà ouverte) ; sans ce branchement, viewModel.userRole
        // resterait toujours bloqué sur "user" et un admin ne pourrait jamais
        // supprimer l'événement de quelqu'un d'autre.
        val userRole = intent.getStringExtra("userRole") ?: "user"
        viewModel.setUserRole(userRole)

        lifecycleScope.launch {
            viewModel.events.collect { events ->
                latestEvents = events

                val items = events.map { event ->
                    MapMarkerItem(
                        id = event.id,
                        lat = event.lat,
                        lng = event.lng,
                        title = event.title,
                        size = event.likes + 1
                    )
                }

                // setEventMarkers gère elle-même le clustering : si plusieurs events
                // sont proches au zoom actuel, ils sont regroupés en un seul marker
                // avec leur nombre ; sinon chaque event a son propre marker.
                mapProvider.setEventMarkers(items) { clickedEventId ->
                    val event = latestEvents.find { it.id == clickedEventId } ?: return@setEventMarkers
                    val intent = Intent(this@MapActivity, EventDetailActivity::class.java)
                    intent.putExtra("eventId", event.id)
                    intent.putExtra("title", event.title)
                    intent.putExtra("description", event.description)
                    intent.putExtra("userEmail", event.userEmail)
                    intent.putExtra("userId", event.userId)
                    intent.putExtra("userRole", viewModel.userRole.value)
                    intent.putExtra("likes", event.likes)
                    startActivity(intent)
                }
            }
        }
        viewModel.loadEvents()

        mapProvider.setOnMapClickListener { lat, lng ->
            selectedLat = lat
            selectedLng = lng
            (mapProvider as? OsmMapProvider)?.showSelectionMarker(lat, lng)

            val intent = Intent(this, AddEventActivity::class.java)
            intent.putExtra("lat", lat)
            intent.putExtra("lng", lng)
            startActivity(intent)
        }
// un exemple de comment on relie l'application au visuel de l'utilisateur
        findViewById<FloatingActionButton>(R.id.fabAddEvent).setOnClickListener {
            // priorité au dernier point tapé sur la carte, sinon position GPS courante
            val lat = selectedLat ?: userLat
            val lng = selectedLng ?: userLng
            val intent = Intent(this, AddEventActivity::class.java)
            //intent sert a passer des données entre les écrans
            intent.putExtra("lat", lat)
            intent.putExtra("lng", lng)
            startActivity(intent)
        }
        findViewById<FloatingActionButton>(R.id.fabLogout).setOnClickListener {
            val authRepo = FirebaseAuthRepository()
            authRepo.logout()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
        findViewById<Button>(R.id.btnZoomIn).setOnClickListener { mapProvider.zoomIn() }
        findViewById<Button>(R.id.btnZoomOut).setOnClickListener { mapProvider.zoomOut() }
        val authRepo = FirebaseAuthRepository()
        findViewById<FloatingActionButton>(R.id.fabProfile).setOnClickListener {
            val email = authRepo.getCurrentUserEmail() ?: "Non connecté"
            android.app.AlertDialog.Builder(this)
                .setTitle("Mon profil")
                .setMessage("Connecté en tant que :\n$email")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun setupConnectivityReceiver() {
        connectivityReceiver = ConnectivityReceiver { connected ->
            val banner = findViewById<TextView>(R.id.tvOfflineBanner)
            banner.visibility = if (connected) View.GONE else View.VISIBLE
        }
        @Suppress("DEPRECATION")
        registerReceiver(connectivityReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) startLocationUpdates()
        else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun startLocationUpdates() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 5000L, 10f
                ) { location ->
                    userLat = location.latitude
                    userLng = location.longitude
                    mapProvider.centerOn(userLat, userLng, 15.0)
                    viewModel.loadEvents(userLat, userLng, 50.0)
                }
            } catch (e: SecurityException) { e.printStackTrace() }
        }
    }

    override fun onResume() { super.onResume(); (mapProvider as? OsmMapProvider)?.onResume() }

    // Permet de zoomer/dézoomer au clavier (utile sur émulateur ou tablette avec
    // clavier physique). On intercepte au niveau dispatchKeyEvent plutôt que onKeyDown
    // pour être sûr de capter la touche même si une vue enfant (la carte) la consomme.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_NUMPAD_ADD, KeyEvent.KEYCODE_EQUALS -> {
                    mapProvider.zoomIn()
                    return true
                }
                KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> {
                    mapProvider.zoomOut()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onPause() {
        super.onPause()
        (mapProvider as? OsmMapProvider)?.onPause()
        if (::locationManager.isInitialized) locationManager.removeUpdates {}
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::connectivityReceiver.isInitialized) unregisterReceiver(connectivityReceiver)
    }
}