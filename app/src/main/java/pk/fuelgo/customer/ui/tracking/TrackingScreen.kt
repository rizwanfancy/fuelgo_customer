package pk.fuelgo.customer.ui.tracking

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pk.fuelgo.customer.data.remote.dto.OrderDto
import pk.fuelgo.customer.ui.components.EmptyState
import pk.fuelgo.customer.ui.components.ErrorBanner
import pk.fuelgo.customer.ui.components.FuelGoOutlinedButton
import pk.fuelgo.customer.ui.components.LoadingState
import pk.fuelgo.customer.ui.components.SectionCard
import pk.fuelgo.customer.ui.components.StatusChip
import pk.fuelgo.customer.ui.fuelGoViewModel
import pk.fuelgo.customer.ui.theme.FuelAction
import pk.fuelgo.customer.ui.theme.StatusSuccess

@Composable
fun TrackingScreen(onGoToOrder: () -> Unit) {
    val viewModel = fuelGoViewModel { TrackingViewModel(it.orderRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCancelConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val order = state.activeOrder
        when {
            state.isLoading -> LoadingState("Loading active order…")
            order == null -> EmptyState(
                title = "No active delivery",
                message = "You don't have a fuel order in progress right now.",
                actionLabel = "Place an order",
                onAction = onGoToOrder,
            )
            else -> {
                ActiveOrderCard(order = order)

                StatusTimeline(order.status)

                SectionCard {
                    if (!order.driverName.isNullOrBlank()) {
                        InfoRow(icon = Icons.Filled.Person, text = "Driver: ${order.driverName}")
                    }
                    InfoRow(icon = Icons.Filled.LocationOn, text = "${order.deliveryAddress}, ${order.deliveryCity}")
                    Spacer(Modifier.height(8.dp))
                    FuelGoOutlinedButton(
                        text = "Open delivery location in Maps",
                        onClick = { openInMaps(context, order) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (order.status == "Pending") {
                    FuelGoOutlinedButton(
                        text = if (state.isCancelling) "Cancelling…" else "Cancel this order",
                        onClick = { showCancelConfirm = true },
                        enabled = !state.isCancelling,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Text(
                    "Auto-refreshes every 15 seconds.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.errorMessage?.let { ErrorBanner(it) }
        state.actionMessage?.let {
            Text(it, color = StatusSuccess, fontWeight = FontWeight.Bold)
        }
    }

    if (showCancelConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel order?") },
            text = { Text("This will cancel your current fuel order.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showCancelConfirm = false
                    viewModel.cancelActiveOrder()
                }) { Text("Yes, cancel") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showCancelConfirm = false }) { Text("Keep order") }
            },
        )
    }
}

@Composable
private fun ActiveOrderCard(order: OrderDto) {
    SectionCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column {
                Text(order.orderNumber, style = MaterialTheme.typography.titleLarge)
                Text(
                    "${order.fuelTypeName} · ${order.quantityLitres.toInt()} L",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(order.status)
        }
    }
}

@Composable
private fun StatusTimeline(currentStatus: String) {
    val currentIndex = ORDER_STATUS_STEPS.indexOf(currentStatus)
    SectionCard {
        Text("Delivery progress", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
        ORDER_STATUS_STEPS.forEachIndexed { index, step ->
            val done = index < currentIndex
            val active = index == currentIndex
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            when {
                                done -> StatusSuccess
                                active -> FuelAction
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("${index + 1}", color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        STATUS_STEP_LABELS[step] ?: step,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) FuelAction else MaterialTheme.colorScheme.onSurface,
                    )
                    if (active) {
                        Text("Current status", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun openInMaps(context: android.content.Context, order: OrderDto) {
    val lat = order.deliveryLatitude
    val lng = order.deliveryLongitude
    val label = Uri.encode("${order.deliveryAddress}, ${order.deliveryCity}")
    val uri = if (lat != 0.0 || lng != 0.0) {
        Uri.parse("geo:$lat,$lng?q=$lat,$lng($label)")
    } else {
        Uri.parse("geo:0,0?q=$label")
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        // No maps app installed — silently ignore, matches other best-effort UX in this app.
    }
}
