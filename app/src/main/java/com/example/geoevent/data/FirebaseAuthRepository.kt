package com.example.geoevent.data

import com.example.geoevent.domain.AuthRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
class FirebaseAuthRepository : AuthRepository {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    override suspend fun login(email: String, password: String): String {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return getUserRole(result.user!!.uid)
    }

    override suspend fun register(email: String, password: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user!!.uid
        db.collection("users").document(uid)
            .set(mapOf("email" to email, "role" to "user")).await()
        return "user"
    }

    override suspend fun getUserRole(uid: String): String {
        val doc = db.collection("users").document(uid).get().await()
        return doc.getString("role") ?: "user"
    }

    override fun logout() = auth.signOut()
    override fun getCurrentUserId() = auth.currentUser?.uid
    override fun getCurrentUserEmail() = auth.currentUser?.email
}
