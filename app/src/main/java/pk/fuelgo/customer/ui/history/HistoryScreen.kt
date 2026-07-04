package pk.fuelgo.customer.ui.history

import androidx.compose.foundation.background
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

@Composable
fun HistoryScreen(onGoToOrder: () -> Unit) {
    val viewModel = fuelGoViewModel { HistoryViewModel(it.orderRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "All your fuel orders, receipts, and payment history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.isLoading) {
            LoadingState("Loading your orders…")
            return@Column
        }

        if (state.orders.isEmpty()) {
            EmptyState(
                title = "No orders yet",
                message = "Place your first fuel order to see it here.",
                actionLabel = "Order fuel now",
                onAction = onGoToOrder,
            )
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatsRow(state.stats)

            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                placeholder = { Text("Search by order #, fuel type, address…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ORDER_STATUS_FILTERS) { statusOption ->
                    PillChip(
                        text = statusOption,
                        selected = state.statusFilter == statusOption,
                        onClick = { viewModel.setStatusFilter(statusOption) },
                    )
                }
            }

            if (state.filteredOrders.isEmpty()) {
                EmptyState(title = "No results", message = "Try adjusting your search or filter.")
            } else {
                state.filteredOrders.forEach { order ->
                    OrderRow(
                        order = order,
                        isCancelling = state.cancellingOrderId == order.id,
                        onClick = { viewModel.openReceipt(order) },
                        onRate = { viewModel.openRating(order) },
                        onCancel = { viewModel.cancelOrder(order) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    state.selectedOrder?.let { order ->
        ReceiptDialog(order = order, onDismiss = { viewModel.closeReceipt() })
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
private fun ReceiptDialog(order: OrderDto, onDismiss: () -> Unit) {
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
