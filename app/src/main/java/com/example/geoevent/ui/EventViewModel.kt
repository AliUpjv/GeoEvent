package com.example.geoevent.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.geoevent.data.FirestoreEventRepository
import com.example.geoevent.domain.Event
import com.example.geoevent.domain.EventRepository
import com.example.geoevent.domain.GetEventsUseCase
import com.example.geoevent.domain.LocationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EventViewModel : ViewModel() {

    private val repository: EventRepository = FirestoreEventRepository()
    private val locationUseCase = LocationUseCase()
    private val getEventsUseCase = GetEventsUseCase(repository, locationUseCase)

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    private val _userRole = MutableStateFlow("user")
    val userRole: StateFlow<String> = _userRole

    fun loadEvents(
        userLat: Double? = null,
        userLng: Double? = null,
        maxKm: Double = 50.0
    ) {
        getEventsUseCase.getEventsRealtime(
            onUpdate = { _events.value = it },
            userLat = userLat,
            userLng = userLng,
            maxDistanceKm = maxKm
        )
    }

    fun setUserRole(role: String) { _userRole.value = role }

    fun likeEvent(eventId: String) {
        viewModelScope.launch { repository.likeEvent(eventId) }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch { repository.deleteEvent(eventId) }
    }
}
