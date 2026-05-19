package com.example.geoevent.data

import com.example.geoevent.domain.Event
import com.example.geoevent.domain.EventRepository
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.tasks.await
class FirestoreEventRepository : EventRepository {
    private val db = Firebase.firestore
    private val collection = db.collection("events")

    override suspend fun getEvents(): List<Event> {
        return collection.get().await().documents.mapNotNull {
            it.toObject(Event::class.java)?.copy(id = it.id)
        }
    }

    override fun getEventsRealtime(onUpdate: (List<Event>) -> Unit) {
        collection.addSnapshotListener { snapshot, _ ->
            val events = snapshot?.documents?.mapNotNull {
                it.toObject(Event::class.java)?.copy(id = it.id)
            } ?: emptyList()
            onUpdate(events)
        }
    }

    override suspend fun addEvent(event: Event): String {
        val ref = collection.add(event).await()
        return ref.id
    }

    override suspend fun deleteEvent(eventId: String) {
        collection.document(eventId).delete().await()
    }

    override suspend fun likeEvent(eventId: String) {
        db.runTransaction { transaction ->
            val ref = collection.document(eventId)
            val current = transaction.get(ref).getLong("likes") ?: 0
            transaction.update(ref, "likes", current + 1)
        }.await()
    }
}

