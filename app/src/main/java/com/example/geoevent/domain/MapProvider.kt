package com.example.geoevent.domain

import android.content.Context
import android.view.ViewGroup

interface MapProvider {
    fun initialize(context: Context)
    fun showMap(container: ViewGroup)
    fun centerOn(lat: Double, lng: Double, zoom: Double)
    fun addMarker(lat: Double, lng: Double, title: String, size: Int = 1): Any
    fun removeAllMarkers()
    fun setOnMapClickListener(listener: (Double, Double) -> Unit)
}