package pk.fuelgo.customer.ui.tracking

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
import pk.fuelgo.customer.config.AppConfig
import pk.fuelgo.customer.data.remote.ApiResult
import pk.fuelgo.customer.data.remote.dto.OrderDto
import pk.fuelgo.customer.data.repository.OrderRepository

val ORDER_STATUS_STEPS = listOf("Pending", "Assigned", "EnRoute", "Arrived", "Delivered")

val STATUS_STEP_LABELS = mapOf(
    "Pending" to "Order placed",
    "Assigned" to "Driver assigned",
    "EnRoute" to "Driver en route",
    "Arrived" to "Driver arrived",
    "Delivered" to "Delivered",
)

private val INACTIVE_STATUSES = setOf("Delivered", "Cancelled", "Failed")

data class TrackingUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val showCancelDialog: Boolean = false,
    val activeOrder: OrderDto? = null,
    val errorMessage: String? = null,
    val isCancelling: Boolean = false,
    val actionMessage: String? = null,
)

class TrackingViewModel(private val orderRepository: OrderRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        startPolling()
    }

    private fun startPolling() {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (isActive) {
                loadOnce()
                delay(AppConfig.ORDER_POLL_INTERVAL_MS)
            }
        }
    }

    /** Pull-to-refresh: shows the indicator while reloading. */
    fun refreshNow() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadOnce()
        }
    }

    fun retry() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch { loadOnce() }
    }

    fun setCancelDialog(show: Boolean) = _uiState.update { it.copy(showCancelDialog = show) }

    private suspend fun loadOnce() {
        when (val result = orderRepository.getMyOrders()) {
            is ApiResult.Success -> {
                val active = result.data.firstOrNull { it.status !in INACTIVE_STATUSES }
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, activeOrder = active, errorMessage = null) }
            }
            is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = result.message) }
        }
    }

    fun cancelActiveOrder(reason: String) {
        val order = _uiState.value.activeOrder ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isCancelling = true, showCancelDialog = false) }
            when (val result = orderRepository.cancelOrder(order.id, reason)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isCancelling = false, actionMessage = "Order cancelled.") }
                    loadOnce()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isCancelling = false, actionMessage = result.message) }
            }
        }
    }

    fun consumeActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }

    override fun onCleared() {
        pollJob?.cancel()
        super.onCleared()
    }
}
