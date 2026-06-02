package com.example.geoevent.domain

class GetEventsUseCase(
    private val repository: EventRepository,
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
