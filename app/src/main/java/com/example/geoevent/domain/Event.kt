package com.example.geoevent.domain
data class Event(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val imageUrl: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0
)