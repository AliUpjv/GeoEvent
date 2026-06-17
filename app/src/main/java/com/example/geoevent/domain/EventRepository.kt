package com.example.geoevent.domain

/**
 * c'est Interface qui définit QUOI faire avec les événements
 *Si demain on veut remplacer Firebase par une autre base de données,
 * on crée juste une nouvelle classe qui implémente cette interface.
 * "suspend" = asynchrone donc elle peut attendre une réponse réseau sans bloquer l'interface utilisateur.
 */
interface EventRepository {
    // Récupère tous les événements une seule fois
    suspend fun getEvents(): List<Event>
    //recup les events en temps reel
    fun getEventsRealtime(onUpdate: (List<Event>) -> Unit)
    //ajoute un events
    suspend fun addEvent(event: Event): String
//supprime un event
    suspend fun deleteEvent(eventId: String)
 //incremente le compteur de like d'un event
    suspend fun likeEvent(eventId: String)
}
