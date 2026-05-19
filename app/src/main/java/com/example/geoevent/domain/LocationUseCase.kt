package com.example.geoevent.domain

import kotlin.math.*

class LocationUseCase {
    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun filterByDistance(
        events: List<Event>, userLat: Double,
        userLng: Double, maxKm: Double
    ): List<Event> {
        return events.filter {
            distanceKm(userLat, userLng, it.lat, it.lng) <= maxKm
        }
    }
}