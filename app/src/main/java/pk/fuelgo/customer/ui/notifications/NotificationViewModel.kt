package pk.fuelgo.customer.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pk.fuelgo.customer.data.remote.ApiResult
import pk.fuelgo.customer.data.remote.dto.NotificationDto
import pk.fuelgo.customer.data.repository.NotificationRepository

private const val NOTIFICATION_POLL_MS = 30_000L

data class NotificationUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val items: List<NotificationDto> = emptyList(),
    val errorMessage: String? = null,
) {
    val unreadCount: Int get() = items.count { !it.isRead }
}

/** Owned by HomeScreen so the top-bar badge and the notifications screen share one list. */
class NotificationViewModel(private val repository: NotificationRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        // ponytail: polling, not push; FCM needs a Firebase project plus a backend sender.
        pollJob = viewModelScope.launch {
            while (isActive) {
                fetch()
                delay(NOTIFICATION_POLL_MS)
            }
        }
    }

    fun refresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            fetch()
        }
    }

    private suspend fun fetch() {
        when (val result = repository.getAll()) {
            is ApiResult.Success -> _uiState.update {
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    items = result.data.sortedByDescending { n -> n.createdAt ?: "" },
                    errorMessage = null,
                )
            }
            is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = result.message) }
        }
    }

    fun markRead(id: String) {
        _uiState.update { s -> s.copy(items = s.items.map { if (it.id == id) it.copy(isRead = true) else it }) }
        viewModelScope.launch { repository.markRead(id) }
    }

    fun markAllRead() {
        _uiState.update { s -> s.copy(items = s.items.map { it.copy(isRead = true) }) }
        viewModelScope.launch { repository.markAllRead() }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
