package pk.fuelgo.customer.ui.history

import androidx.compose.foundation.background
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import pk.fuelgo.customer.ui.components.CancelOrderDialog
import pk.fuelgo.customer.ui.components.ErrorBanner
import pk.fuelgo.customer.ui.components.RefreshBox
import pk.fuelgo.customer.ui.components.RefreshOnResume
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pk.fuelgo.customer.data.remote.dto.OrderDto
import pk.fuelgo.customer.ui.components.EmptyState
import pk.fuelgo.customer.ui.components.FuelGoOutlinedButton
import pk.fuelgo.customer.ui.components.FuelGoPrimaryButton
import pk.fuelgo.customer.ui.components.LoadingState
import pk.fuelgo.customer.ui.components.PillChip
import pk.fuelgo.customer.ui.components.SectionCard
import pk.fuelgo.customer.ui.components.StatusChip
import pk.fuelgo.customer.ui.fuelGoViewModel
import pk.fuelgo.customer.ui.theme.FuelAction
import java.text.NumberFormat
import java.util.Locale

private val pkrFormat = NumberFormat.getNumberInstance(Locale("en", "PK")).apply { maximumFractionDigits = 0 }
private fun pkr(amount: Double) = "PKR ${pkrFormat.format(amount)}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onGoToOrder: () -> Unit) {
    val viewModel = fuelGoViewModel { HistoryViewModel(it.orderRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    RefreshOnResume(viewModel::refresh)

    RefreshBox(isRefreshing = state.isRefreshing, onRefresh = viewModel::refresh) {
        when {
            state.isLoading -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                LoadingState("Loading your orders…")
            }
            state.orders.isEmpty() -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                state.errorMessage?.let { ErrorBanner(it, modifier = Modifier.padding(16.dp), onRetry = { viewModel.load() }) }
                EmptyState(
                    title = "No orders yet",
                    message = "Place your first fuel order to see it here.",
                    actionLabel = "Order fuel now",
                    onAction = onGoToOrder,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Text(
                        "All your fuel orders, receipts, and payment history.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                state.errorMessage?.let { msg -> item { ErrorBanner(msg, onRetry = { viewModel.load() }) } }
                item { StatsRow(state.stats) }
                item {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::setSearchQuery,
                        placeholder = { Text("Search by order #, fuel type, address…") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ORDER_STATUS_FILTERS) { statusOption ->
                            PillChip(
                                text = statusOption,
                                selected = state.statusFilter == statusOption,
                                onClick = { viewModel.setStatusFilter(statusOption) },
                            )
                        }
                    }
                }
                if (state.fuelOptions.size > 2) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.fuelOptions) { fuel ->
                                PillChip(
                                    text = fuel,
                                    selected = state.fuelFilter == fuel,
                                    onClick = { viewModel.setFuelFilter(fuel) },
                                )
                            }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FuelGoOutlinedButton(
                            text = dateRangeLabel(state.dateFrom, state.dateTo),
                            onClick = { showDatePicker = true },
                        )
                        FuelGoOutlinedButton(
                            text = "Export",
                            onClick = { shareText(context, "FuelGo orders", ordersCsv(state.filteredOrders)) },
                            enabled = state.filteredOrders.isNotEmpty(),
                        )
                        if (state.hasActiveFilters) {
                            TextButton(onClick = viewModel::clearFilters) { Text("Clear") }
                        }
                    }
                }
                item {
                    Text(
                        "${state.filteredOrders.size} of ${state.orders.size} orders",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (state.filteredOrders.isEmpty()) {
                    item { EmptyState(title = "No results", message = "Try adjusting your search or filters.") }
                } else {
                    items(state.filteredOrders, key = { it.id }) { order ->
                        OrderRow(
                            order = order,
                            isCancelling = state.cancellingOrderId == order.id,
                            onClick = { viewModel.openReceipt(order) },
                            onRate = { viewModel.openRating(order) },
                            onCancel = { viewModel.askCancel(order) },
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    state.selectedOrder?.let { order ->
        ReceiptDialog(
            order = order,
            onDismiss = { viewModel.closeReceipt() },
            onShare = { shareText(context, "Receipt ${order.orderNumber}", receiptText(order)) },
        )
    }

    state.cancelTarget?.let { target ->
        CancelOrderDialog(
            orderNumber = target.orderNumber,
            onDismiss = { viewModel.askCancel(null) },
            onConfirm = { reason -> viewModel.cancelOrder(target, reason) },
        )
    }

    if (showDatePicker) {
        val pickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setDateRange(
                        pickerState.selectedStartDateMillis?.let(::millisToLocalDate),
                        pickerState.selectedEndDateMillis?.let(::millisToLocalDate),
                    )
                    showDatePicker = false
                }) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setDateRange(null, null)
                    showDatePicker = false
                }) { Text("Clear") }
            },
        ) {
            DateRangePicker(state = pickerState, modifier = Modifier.height(500.dp))
        }
    }

    state.ratingOrder?.let { ratingOrder ->
        RatingDialog(
            orderNumber = ratingOrder.orderNumber,
            ratingValue = state.ratingValue,
            feedback = state.ratingFeedback,
            isSubmitting = state.isSubmittingRating,
            onRatingChange = viewModel::setRatingValue,
            onFeedbackChange = viewModel::setRatingFeedback,
            onDismiss = { viewModel.closeRating() },
            onSubmit = { viewModel.submitRating() },
        )
    }
}

// DatePicker returns UTC-midnight millis, so convert in UTC to keep the picked calendar day.
private fun millisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

private fun dateRangeLabel(from: LocalDate?, to: LocalDate?): String = when {
    from == null && to == null -> "Date range"
    to == null -> "From $from"
    else -> "$from → $to"
}

private fun receiptText(order: OrderDto): String = buildString {
    appendLine("FuelGo receipt ${order.orderNumber}")
    appendLine("Fuel: ${order.fuelTypeName} x ${order.quantityLitres.toInt()} L @ ${pkr(order.pricePerLitre)}")
    appendLine("Payment: ${order.paymentMethod}")
    appendLine("Status: ${order.status}")
    appendLine("Address: ${order.deliveryAddress}, ${order.deliveryCity}")
    order.driverName?.takeIf { it.isNotBlank() }?.let { appendLine("Driver: $it") }
    order.deliveredAt?.takeIf { it.isNotBlank() }?.let { appendLine("Delivered: ${it.take(16).replace("T", " ")}") }
    append("Total: ${pkr(order.totalAmount)}")
}

private fun ordersCsv(orders: List<OrderDto>): String {
    fun cell(v: String) = "\"" + v.replace("\"", "\"\"") + "\""
    val header = "Order,Date,Fuel,Litres,Total,Payment,Status,Address"
    val rows = orders.map {
        listOf(
            it.orderNumber, it.createdAt?.take(10) ?: "", it.fuelTypeName, it.quantityLitres.toInt().toString(),
            it.totalAmount.toInt().toString(), it.paymentMethod, it.status, "${it.deliveryAddress}, ${it.deliveryCity}",
        ).joinToString(",") { c -> cell(c) }
    }
    return (listOf(header) + rows).joinToString("\n")
}

// ponytail: shares plain text; a real PDF export needs PdfDocument + FileProvider, add if text isn't enough.
private fun shareText(context: Context, subject: String, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, subject))
}

@Composable
private fun StatsRow(stats: HistoryStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard("Orders", stats.total.toString(), Modifier.weight(1f))
        StatCard("Spent", pkr(stats.totalSpent), Modifier.weight(1f))
        StatCard("Delivered", stats.delivered.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(value, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OrderRow(
    order: OrderDto,
    isCancelling: Boolean,
    onClick: () -> Unit,
    onRate: () -> Unit,
    onCancel: () -> Unit,
) {
    SectionCard(modifier = Modifier.padding(0.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(order.orderNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${order.fuelTypeName} · ${order.quantityLitres.toInt()} L",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(order.createdAt?.take(10) ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusChip(order.status)
                Spacer(Modifier.height(6.dp))
                Text(pkr(order.totalAmount), fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FuelGoOutlinedButton(text = "View receipt", onClick = onClick)
            if (order.status == "Delivered") {
                FuelGoOutlinedButton(text = "Rate", onClick = onRate)
            }
            if (order.status == "Pending") {
                FuelGoOutlinedButton(text = if (isCancelling) "Cancelling…" else "Cancel", onClick = onCancel, enabled = !isCancelling)
            }
        }
    }
}

@Composable
private fun ReceiptDialog(order: OrderDto, onDismiss: () -> Unit, onShare: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receipt — ${order.orderNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ReceiptLine("Fuel type", order.fuelTypeName)
                ReceiptLine("Quantity", "${order.quantityLitres.toInt()} L")
                ReceiptLine("Rate", pkr(order.pricePerLitre))
                ReceiptLine("Payment", order.paymentMethod)
                ReceiptLine("Status", order.status)
                ReceiptLine("Address", "${order.deliveryAddress}, ${order.deliveryCity}")
                if (!order.driverName.isNullOrBlank()) ReceiptLine("Driver", order.driverName)
                if (!order.deliveredAt.isNullOrBlank()) ReceiptLine("Delivered at", order.deliveredAt.take(16).replace("T", " "))
                Spacer(Modifier.height(6.dp))
                ReceiptLine("Total", pkr(order.totalAmount), bold = true)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = { TextButton(onClick = onShare) { Text("Share receipt") } },
    )
}

@Composable
private fun ReceiptLine(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium)
    }
}

@Composable
private fun RatingDialog(
    orderNumber: String,
    ratingValue: Int,
    feedback: String,
    isSubmitting: Boolean,
    onRatingChange: (Int) -> Unit,
    onFeedbackChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate your delivery") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Order $orderNumber", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= ratingValue) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = "Rate $star",
                            tint = FuelAction,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(32.dp)
                                .clickable { onRatingChange(star) },
                        )
                    }
                }
                OutlinedTextField(
                    value = feedback,
                    onValueChange = onFeedbackChange,
                    placeholder = { Text("Optional feedback…") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            FuelGoPrimaryButton(
                text = if (isSubmitting) "Submitting…" else "Submit rating",
                onClick = onSubmit,
                enabled = ratingValue > 0 && !isSubmitting,
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
