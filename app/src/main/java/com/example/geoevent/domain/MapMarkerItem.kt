package com.example.geoevent.domain

/**
 * Représente un point à afficher sur la carte (un événement), sous une forme
 * qui ne dépend d'aucune librairie de cartographie précise (osmdroid, Google Maps...).
 *
 * C'est ce que MapActivity envoie à MapProvider.setEventMarkers() : la carte
 * reçoit juste des coordonnées + une taille, elle n'a pas besoin de connaître
 * la classe Event en entier.
 */
data class MapMarkerItem(
    val id: String,
    val lat: Double,
    val lng: Double,
    val title: String,
    // taille du marker individuel (basée sur le nombre de likes + 1)
    val size: Int
)