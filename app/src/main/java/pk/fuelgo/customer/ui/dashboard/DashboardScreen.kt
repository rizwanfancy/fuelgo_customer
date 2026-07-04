package pk.fuelgo.customer.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pk.fuelgo.customer.ui.components.ErrorBanner
import pk.fuelgo.customer.ui.components.FuelGoPrimaryButton
import pk.fuelgo.customer.ui.components.HorizontalBarChart
import pk.fuelgo.customer.ui.components.LoadingState
import pk.fuelgo.customer.ui.components.SectionCard
import pk.fuelgo.customer.ui.components.VerticalBarChart
import pk.fuelgo.customer.ui.fuelGoViewModel
import pk.fuelgo.customer.ui.theme.FuelAction
import pk.fuelgo.customer.ui.theme.FuelPrimary
import pk.fuelgo.customer.ui.theme.StatusInfo
import java.text.NumberFormat
import java.util.Locale

private val pkrFormat = NumberFormat.getNumberInstance(Locale("en", "PK")).apply { maximumFractionDigits = 0 }
private fun pkr(amount: Double) = "PKR ${pkrFormat.format(amount)}"

@Composable
fun DashboardScreen(onGoToOrder: () -> Unit, onGoToTracking: () -> Unit) {
    val viewModel = fuelGoViewModel { DashboardViewModel(it.orderRepository, it.customerRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) {
        LoadingState("Loading your dashboard…", modifier = Modifier.fillMaxSize())
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Hi ${state.greetingName},", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Here's a snapshot of your FuelGo activity.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.errorMessage?.let { ErrorBanner(it) }

        state.activeOrder?.let { order ->
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = FuelAction)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active delivery — ${order.orderNumber}", fontWeight = FontWeight.Bold)
                        Text(
                            "${order.fuelTypeName} · ${order.status}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FuelGoPrimaryButton(text = "Track", onClick = onGoToTracking)
                }
            }
        }

        StatsGrid(state)

        SectionCard {
            Text("Orders by status", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 14.dp))
            HorizontalBarChart(data = state.statusBreakdown, barColor = StatusInfo)
        }

        SectionCard {
            Text("Spend — last 6 months", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 14.dp))
            VerticalBarChart(
                data = state.monthlySpend,
                barColor = FuelAction,
                valueFormatter = { value -> if (value >= 1000) "${(value / 1000).toInt()}k" else value.toInt().toString() },
            )
        }

        FuelGoPrimaryButton(
            text = "Place a new order",
            onClick = onGoToOrder,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
    }
}

@Composable
private fun StatsGrid(state: DashboardUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("Total Orders", state.totalOrders.toString(), FuelPrimary, Modifier.weight(1f))
            StatTile("Total Spent", pkr(state.totalSpentPkr), FuelAction, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("Delivered", state.deliveredCount.toString(), StatusInfo, Modifier.weight(1f))
            StatTile("Active", state.activeCount.toString(), FuelAction, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(
            value,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleLarge,
            color = accent,
        )
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
