package com.example.geoevent.domain

interface AuthRepository {
    suspend fun login(email: String, password: String): String
    suspend fun register(email: String, password: String): String
    suspend fun getUserRole(uid: String): String
    fun logout()
    fun getCurrentUserId(): String?
    fun getCurrentUserEmail(): String?
}
