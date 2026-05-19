package com.example.geoevent

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.geoevent.data.OsmMapProvider
import com.example.geoevent.domain.MapProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

// Classes fictives ou manquantes requises par le code du guide pour compiler
import androidx.lifecycle.ViewModel
import com.example.geoevent.util.ConnectivityReceiver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
class EventViewModel : ViewModel() {
    private val _events = MutableStateFlow<List<com.example.geoevent.domain.Event>>(emptyList())
    val events: StateFlow<List<com.example.geoevent.domain.Event>> = _events
    fun loadEvents(lat: Double = 0.0, lng: Double = 0.0, maxKm: Double = 0.0) {}
}
class AddEventActivity : AppCompatActivity()

class MapActivity : AppCompatActivity() {
    private val mapProvider: MapProvider = OsmMapProvider()
    private val viewModel: EventViewModel by viewModels()
    private lateinit var locationManager: LocationManager
    private var userLat = 48.8566
    private var userLng = 2.3522

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) startLocationUpdates() }
    private lateinit var connectivityReceiver: ConnectivityReceiver
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapProvider.initialize(this)
        mapProvider.showMap(findViewById(R.id.mapContainer))
        mapProvider.centerOn(userLat, userLng, 12.0)
        checkLocationPermission()

        lifecycleScope.launch {
            viewModel.events.collect { events ->
                mapProvider.removeAllMarkers()
                events.forEach { event ->
                    mapProvider.addMarker(event.lat, event.lng,
                        event.title, event.likes + 1)
                }
            }
        }
        viewModel.loadEvents()

        findViewById<FloatingActionButton>(R.id.fabAddEvent).setOnClickListener {
            val intent = Intent(this, AddEventActivity::class.java)
            intent.putExtra("lat", userLat)
            intent.putExtra("lng", userLng)
            startActivity(intent)
        }
        connectivityReceiver = ConnectivityReceiver { connected ->
            val banner = findViewById<TextView>(R.id.tvOfflineBanner)
            banner.visibility = if (connected) View.GONE else View.VISIBLE
        }
        registerReceiver(connectivityReceiver,
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )

    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
            startLocationUpdates()
        else
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun startLocationUpdates() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // On vérifie une dernière fois les permissions pour calmer Android Studio
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            try {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 5000L, 10f
                ) { location ->
                    userLat = location.latitude
                    userLng = location.longitude
                    mapProvider.centerOn(userLat, userLng, 15.0)
                    viewModel.loadEvents(userLat, userLng, 50.0)
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
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
        unregisterReceiver(connectivityReceiver)
    }
}