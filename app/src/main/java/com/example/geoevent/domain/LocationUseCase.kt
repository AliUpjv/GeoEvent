package com.example.geoevent.domain

import kotlin.math.*
/**
 * Contient la logique de calcul de distance GPS et de filtrage d'événements.
 *c'est un usecase et donc definit les fonctions
 *
 * Contient la formule haversine : l'algorithme standard pour calculer
 * la distance réelle en km entre deux points GPS sur une sphère (la Terre).
 */
class LocationUseCase {
    // Calcule la distance en kilomètres entre deux points GPS.
    fun distanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
    //Filtre une liste d'événements pour ne garder que ceux
    //qui sont dans un rayon maximal autour de l'utilisateur.
    fun filterByDistance(
        events: List<Event>, userLat: Double,
        userLng: Double, maxKm: Double
    ): List<Event> {
        return events.filter {
            distanceKm(userLat, userLng, it.lat, it.lng) <= maxKm
        }
    }
}