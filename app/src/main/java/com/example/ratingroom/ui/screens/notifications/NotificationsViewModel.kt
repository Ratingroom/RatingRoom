package com.example.ratingroom.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ratingroom.data.models.Notification
import com.example.ratingroom.repository.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class NotificationsUIState(
    val isLoading: Boolean = true,
    val items: List<Notification> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: NotificationsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(NotificationsUIState())
    val ui: StateFlow<NotificationsUIState> = _ui.asStateFlow()

    init { observe() }

    private fun observe() {
        viewModelScope.launch {
            repo.observeMyNotifications().collectLatest { list ->
                val mapped = list.map { m ->
                    Notification(
                        id = (m["id"] as? String) ?: "",
                        type = (m["type"] as? String) ?: "",
                        actorUserId = (m["actorUserId"] as? String) ?: "",
                        actorName = m["actorName"] as? String,
                        reviewId = m["reviewId"] as? String,
                        movieId = (m["movieId"] as? Number)?.toInt()
                            ?: (m["movieId"] as? String)?.toIntOrNull(),
                        createdAt = (m["createdAt"] as? Number)?.toLong() ?: 0L,
                        seen = (m["seen"] as? Boolean) ?: false
                    )
                }
                _ui.value = NotificationsUIState(isLoading = false, items = mapped)
            }
        }
    }

    fun markAllSeen() {
        viewModelScope.launch {
            repo.markAllSeen()
        }
    }

    fun markSeen(id: String) {
        viewModelScope.launch {
            repo.markSeen(id)
        }
    }
}
