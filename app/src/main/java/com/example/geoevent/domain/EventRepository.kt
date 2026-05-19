package com.example.geoevent.domain
interface EventRepository {
    suspend fun getEvents(): List<Event>
    fun getEventsRealtime(onUpdate: (List<Event>) -> Unit)
    suspend fun addEvent(event: Event): String
    suspend fun deleteEvent(eventId: String)
    suspend fun likeEvent(eventId: String)
}
