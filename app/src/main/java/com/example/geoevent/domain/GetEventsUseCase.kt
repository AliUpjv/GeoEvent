package com.example.geoevent.domain

/**
 * Orchestre la récupération des événements en combinant
 *        le repository (source de données) et le LocationUseCase (filtrage par distance).
 *
 * Ici : récupérer les événements, les filtrer par distance, les trier par popularité.
 *
 * Il reçoit ses dépendances en paramètre
 */
class GetEventsUseCase(
    // Le repository fournit les données
    private val repository: EventRepository,
    // Le locationUseCase s'occupe des calculs de distance
    private val locationUseCase: LocationUseCase
) {
    fun getEventsRealtime(
        onUpdate: (List<Event>) -> Unit,
        userLat: Double? = null,
        userLng: Double? = null,
        maxDistanceKm: Double = Double.MAX_VALUE
    ) {
        repository.getEventsRealtime { events ->
            val filtered = if (userLat != null && userLng != null) {
                locationUseCase.filterByDistance(events, userLat, userLng, maxDistanceKm)
            } else events
            onUpdate(filtered.sortedByDescending { it.likes })
        }
    }
}
