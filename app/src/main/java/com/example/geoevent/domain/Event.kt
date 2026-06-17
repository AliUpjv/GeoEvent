package com.example.geoevent.domain
/** *
 *  Modèle de données principal de l'application.
 * Représente un événement géolocalisé créé par un utilisateur.
 *
 * C'est une class de données Kotlin : Kotlin génère automatiquement
 * equals(), hashCode() et copy() — on n'a pas besoin de les écrire à la main.
 */
data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    //cordonnées GPSs
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val imageUrl: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0
)