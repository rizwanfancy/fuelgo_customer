package pk.fuelgo.customer.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pk.fuelgo.customer.data.remote.ApiResult
import pk.fuelgo.customer.data.remote.dto.OrderDto
import pk.fuelgo.customer.data.repository.OrderRepository

val ORDER_STATUS_FILTERS = listOf("All", "Pending", "Assigned", "EnRoute", "Arrived", "Delivered", "Cancelled")

data class HistoryStats(
    val total: Int = 0,
    val totalSpent: Double = 0.0,
    val delivered: Int = 0,
    val pending: Int = 0,
)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val orders: List<OrderDto> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: String = "All",
    val errorMessage: String? = null,
    val selectedOrder: OrderDto? = null,
    val ratingOrder: OrderDto? = null,
    val ratingValue: Int = 0,
    val ratingFeedback: String = "",
    val isSubmittingRating: Boolean = false,
    val cancellingOrderId: String? = null,
    val actionMessage: String? = null,
) {
    val filteredOrders: List<OrderDto>
        get() {
            val q = searchQuery.trim().lowercase()
            return orders.filter { order ->
                val statusOk = statusFilter == "All" || order.status == statusFilter
                val searchOk = q.isBlank() ||
                    listOf(order.orderNumber, order.fuelTypeName, order.deliveryAddress, order.deliveryCity, order.paymentMethod)
                        .joinToString(" ").lowercase().contains(q)
                statusOk && searchOk
            }
        }

    val stats: HistoryStats
        get() = HistoryStats(
            total = orders.size,
            totalSpent = orders.filter { it.status == "Delivered" }.sumOf { it.totalAmount },
            delivered = orders.count { it.status == "Delivered" },
            pending = orders.count { it.status == "Pending" },
        )
}

class HistoryViewModel(private val orderRepository: OrderRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = orderRepository.getMyOrders()) {
                is ApiResult.Success -> {
                    val sorted = result.data.sortedByDescending { it.createdAt ?: "" }
                    _uiState.update { it.copy(isLoading = false, orders = sorted) }
                }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun setSearchQuery(value: String) = _uiState.update { it.copy(searchQuery = value) }
    fun setStatusFilter(value: String) = _uiState.update { it.copy(statusFilter = value) }

    fun openReceipt(order: OrderDto) = _uiState.update { it.copy(selectedOrder = order) }
    fun closeReceipt() = _uiState.update { it.copy(selectedOrder = null) }

    fun openRating(order: OrderDto) =
        _uiState.update { it.copy(ratingOrder = order, ratingValue = 0, ratingFeedback = "") }

    fun closeRating() = _uiState.update { it.copy(ratingOrder = null) }
    fun setRatingValue(value: Int) = _uiState.update { it.copy(ratingValue = value) }
    fun setRatingFeedback(value: String) = _uiState.update { it.copy(ratingFeedback = value) }

    fun submitRating() {
        val order = _uiState.value.ratingOrder ?: return
        val rating = _uiState.value.ratingValue
        if (rating <= 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingRating = true) }
            val feedback = _uiState.value.ratingFeedback
            when (val result = orderRepository.rateOrder(order.id, rating, feedback)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isSubmittingRating = false, ratingOrder = null, actionMessage = "Thanks for rating your delivery!")
                    }
                    load()
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmittingRating = false, actionMessage = result.message) }
            }
        }
    }

    fun cancelOrder(order: OrderDto) {
        viewModelScope.launch {
            _uiState.update { it.copy(cancellingOrderId = order.id) }
            when (val result = orderRepository.cancelOrder(order.id, "Cancelled by customer")) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            cancellingOrderId = null,
                            actionMessage = "Order cancelled.",
                            orders = state.orders.map { if (it.id == order.id) it.copy(status = "Cancelled") else it },
                        )
                    }
                }
                is ApiResult.Error -> _uiState.update { it.copy(cancellingOrderId = null, actionMessage = result.message) }
            }
        }
    }

    fun consumeActionMessage() = _uiState.update { it.copy(actionMessage = null) }
}
