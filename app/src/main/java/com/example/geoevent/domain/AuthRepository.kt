package com.example.geoevent.domain

/**
 *l'Interface qui définit toutes les opérations liées à l'authentification.
 * Firebase Auth gère uniquement l'IDENTITÉ (qui on est).
 */
interface AuthRepository {
    suspend fun login(email: String, password: String): String
    //enregistre un nouvel utilisateur dans firebase avec le role user par defaut
    suspend fun register(email: String, password: String): String
    //on utilise cette methode afin de recuperer le role stocké dans firestore séparement
    suspend fun getUserRole(uid: String): String
    //déconnexion
    fun logout()
    fun getCurrentUserId(): String?
    fun getCurrentUserEmail(): String?
}
