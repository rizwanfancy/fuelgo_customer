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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

val ORDER_STATUS_FILTERS = listOf("All", "Pending", "Assigned", "EnRoute", "Arrived", "Delivered", "Cancelled")

data class HistoryStats(
    val total: Int = 0,
    val totalSpent: Double = 0.0,
    val delivered: Int = 0,
    val pending: Int = 0,
)

data class HistoryUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val fuelFilter: String = "All",
    val dateFrom: LocalDate? = null,
    val dateTo: LocalDate? = null,
    val cancelTarget: OrderDto? = null,
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
                val fuelOk = fuelFilter == "All" || order.fuelTypeName == fuelFilter
                val day = orderDate(order)
                val dateOk = (dateFrom == null || (day != null && !day.isBefore(dateFrom))) &&
                    (dateTo == null || (day != null && !day.isAfter(dateTo)))
                statusOk && searchOk && fuelOk && dateOk
            }
        }

    val fuelOptions: List<String>
        get() = listOf("All") + orders.map { it.fuelTypeName }.filter { it.isNotBlank() }.distinct().sorted()

    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() || statusFilter != "All" || fuelFilter != "All" || dateFrom != null || dateTo != null

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

    fun refresh() {
        val s = _uiState.value
        if (s.isLoading || s.isRefreshing) return
        load(silent = true)
    }

    fun load(silent: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = !silent, isRefreshing = silent, errorMessage = null) }
            when (val result = orderRepository.getMyOrders()) {
                is ApiResult.Success -> {
                    val sorted = result.data.sortedByDescending { it.createdAt ?: "" }
                    _uiState.update { it.copy(isLoading = false, isRefreshing = false, orders = sorted) }
                }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, isRefreshing = false, errorMessage = result.message) }
            }
        }
    }

    fun setFuelFilter(value: String) = _uiState.update { it.copy(fuelFilter = value) }
    fun setDateRange(from: LocalDate?, to: LocalDate?) = _uiState.update { it.copy(dateFrom = from, dateTo = to) }
    fun clearFilters() = _uiState.update {
        it.copy(searchQuery = "", statusFilter = "All", fuelFilter = "All", dateFrom = null, dateTo = null)
    }
    fun askCancel(order: OrderDto?) = _uiState.update { it.copy(cancelTarget = order) }

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

    fun cancelOrder(order: OrderDto, reason: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cancellingOrderId = order.id, cancelTarget = null) }
            when (val result = orderRepository.cancelOrder(order.id, reason)) {
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

private fun orderDate(order: OrderDto): LocalDate? {
    val raw = order.createdAt ?: return null
    return runCatching { OffsetDateTime.parse(raw).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() }
        .recoverCatching { LocalDate.parse(raw.take(10)) }
        .getOrNull()
}
