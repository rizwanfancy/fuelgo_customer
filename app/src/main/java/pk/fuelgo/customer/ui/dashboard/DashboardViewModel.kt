package pk.fuelgo.customer.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pk.fuelgo.customer.data.remote.ApiResult
import pk.fuelgo.customer.data.remote.dto.OrderDto
import pk.fuelgo.customer.data.repository.CustomerRepository
import pk.fuelgo.customer.data.repository.OrderRepository
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val ORDER_STATUSES_FOR_CHART = listOf("Pending", "Assigned", "EnRoute", "Arrived", "Delivered", "Cancelled")
private val monthLabelFormatter = DateTimeFormatter.ofPattern("MMM")

private val INACTIVE_STATUSES = setOf("Delivered", "Cancelled", "Failed")

data class DashboardUiState(
    val isLoading: Boolean = true,
    val greetingName: String = "there",
    val totalOrders: Int = 0,
    val totalSpentPkr: Double = 0.0,
    val deliveredCount: Int = 0,
    val activeCount: Int = 0,
    val statusBreakdown: List<Pair<String, Int>> = emptyList(),
    val monthlySpend: List<Pair<String, Double>> = emptyList(),
    val activeOrder: OrderDto? = null,
    val errorMessage: String? = null,
)

class DashboardViewModel(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val ordersResult = orderRepository.getMyOrders()
            val profileResult = customerRepository.getProfile()

            val orders = (ordersResult as? ApiResult.Success)?.data ?: emptyList()
            val greetingName = (profileResult as? ApiResult.Success)?.data?.firstName?.takeIf { it.isNotBlank() } ?: "there"

            val delivered = orders.filter { it.status == "Delivered" }
            val active = orders.filter { it.status !in INACTIVE_STATUSES }

            val statusBreakdown = ORDER_STATUSES_FOR_CHART.map { status ->
                status to orders.count { it.status == status }
            }

            val error = (ordersResult as? ApiResult.Error)?.message

            _uiState.update {
                it.copy(
                    isLoading = false,
                    greetingName = greetingName,
                    totalOrders = orders.size,
                    totalSpentPkr = delivered.sumOf { o -> o.totalAmount },
                    deliveredCount = delivered.size,
                    activeCount = active.size,
                    statusBreakdown = statusBreakdown,
                    monthlySpend = computeMonthlySpend(delivered),
                    activeOrder = active.firstOrNull(),
                    errorMessage = error,
                )
            }
        }
    }

    private fun computeMonthlySpend(deliveredOrders: List<OrderDto>): List<Pair<String, Double>> {
        val currentMonth = YearMonth.now()
        val months = (5 downTo 0).map { offset -> currentMonth.minusMonths(offset.toLong()) }
        val totals = LinkedHashMap<YearMonth, Double>()
        months.forEach { totals[it] = 0.0 }

        deliveredOrders.forEach { order ->
            val yearMonth = parseYearMonth(order.createdAt) ?: return@forEach
            if (totals.containsKey(yearMonth)) {
                totals[yearMonth] = (totals[yearMonth] ?: 0.0) + order.totalAmount
            }
        }

        return months.map { month -> month.format(monthLabelFormatter) to (totals[month] ?: 0.0) }
    }

    private fun parseYearMonth(raw: String?): YearMonth? {
        if (raw.isNullOrBlank()) return null
        val viaOffset = runCatching { YearMonth.from(OffsetDateTime.parse(raw)) }.getOrNull()
        if (viaOffset != null) return viaOffset
        return runCatching { YearMonth.from(LocalDateTime.parse(raw)) }.getOrNull()
    }
}
