package com.example.geoevent

import android.Manifest
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
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
class MapActivity : AppCompatActivity() {

    private val mapProvider: MapProvider = OsmMapProvider()
    private val viewModel: EventViewModel by viewModels()
    private lateinit var locationManager: LocationManager
    private lateinit var connectivityReceiver: ConnectivityReceiver
    private var userLat = 48.8566
    private var userLng = 2.3522

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

        lifecycleScope.launch {
            viewModel.events.collect { events ->
                mapProvider.removeAllMarkers()
                events.forEach { event ->
                    mapProvider.addMarker(event.lat, event.lng, event.title, event.likes + 1)
                }
            }
        }
        viewModel.loadEvents()

        mapProvider.setOnMapClickListener { lat, lng ->
            val intent = Intent(this, AddEventActivity::class.java)
            intent.putExtra("lat", lat)
            intent.putExtra("lng", lng)
            startActivity(intent)
        }

        findViewById<FloatingActionButton>(R.id.fabAddEvent).setOnClickListener {
            val intent = Intent(this, AddEventActivity::class.java)
            intent.putExtra("lat", userLat)
            intent.putExtra("lng", userLng)
            startActivity(intent)
        }
        findViewById<FloatingActionButton>(R.id.fabLogout).setOnClickListener {
            val authRepo = FirebaseAuthRepository()
            authRepo.logout()
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
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
