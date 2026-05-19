package com.example.geoevent.data

import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import com.example.geoevent.domain.MapProvider
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

class OsmMapProvider : MapProvider {
    private lateinit var mapView: MapView

    override fun initialize(context: Context) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    override fun showMap(container: ViewGroup) {
        mapView = MapView(container.context)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        container.addView(mapView)
    }

    override fun centerOn(lat: Double, lng: Double, zoom: Double) {
        val point = GeoPoint(lat, lng)
        mapView.controller.setZoom(zoom)
        mapView.controller.setCenter(point)
    }

    override fun addMarker(lat: Double, lng: Double, title: String, size: Int): Any {
        val marker = Marker(mapView)
        marker.position = GeoPoint(lat, lng)
        marker.title = title
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(marker)
        mapView.invalidate()
        return marker
    }

    override fun removeAllMarkers() {
        mapView.overlays.clear()
        mapView.invalidate()
    }

    override fun setOnMapClickListener(listener: (Double, Double) -> Unit) {
        val overlay = object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                val point = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())
                listener(point.latitude, point.longitude)
                return true
            }
        }
        mapView.overlays.add(overlay)
    }

    fun onResume() = mapView.onResume()
    fun onPause() = mapView.onPause()
}