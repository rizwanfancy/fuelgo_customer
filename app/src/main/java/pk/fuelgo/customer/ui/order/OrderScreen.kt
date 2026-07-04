package pk.fuelgo.customer.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pk.fuelgo.customer.config.AppConfig
import pk.fuelgo.customer.data.remote.dto.FuelTypeDto
import pk.fuelgo.customer.ui.components.EmptyState
import pk.fuelgo.customer.ui.components.ErrorBanner
import pk.fuelgo.customer.ui.components.FuelGoOutlinedButton
import pk.fuelgo.customer.ui.components.FuelGoPrimaryButton
import pk.fuelgo.customer.ui.components.LoadingState
import pk.fuelgo.customer.ui.components.SectionCard
import pk.fuelgo.customer.ui.components.SuccessBanner
import pk.fuelgo.customer.ui.fuelGoViewModel
import pk.fuelgo.customer.ui.theme.FuelAction
import pk.fuelgo.customer.ui.theme.FuelPrimary
import pk.fuelgo.customer.ui.theme.fuelColorForCode
import java.text.NumberFormat
import java.util.Locale

private val pkrFormat = NumberFormat.getNumberInstance(Locale("en", "PK")).apply { maximumFractionDigits = 0 }
private fun pkr(amount: Double) = "PKR ${pkrFormat.format(amount)}"

@Composable
fun OrderScreen() {
    val viewModel = fuelGoViewModel { OrderViewModel(it.orderRepository, it.customerRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    if (state.isLoading) {
        LoadingState("Loading fuel types and prices…", modifier = Modifier.fillMaxSize())
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Select fuel type, quantity, address, and payment method.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SectionCard {
            Text("1. Fuel type", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
            if (state.fuelTypes.isEmpty()) {
                Text("No fuel types available right now.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.fuelTypes, key = { it.id }) { fuel ->
                        FuelTypeCard(
                            fuel = fuel,
                            selected = fuel.id == state.selectedFuelTypeId,
                            onClick = { viewModel.selectFuel(fuel.id) },
                        )
                    }
                }
            }
        }

        SectionCard {
            Text("2. Quantity (litres)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                QUICK_QUANTITIES.forEach { qty ->
                    QuantityChip(
                        label = "${qty}L",
                        selected = state.quantityText == qty.toString(),
                        onClick = { viewModel.setQuickQuantity(qty) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.quantityText,
                onValueChange = viewModel::setQuantityText,
                label = { Text("Custom quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard {
            Text("3. Delivery address", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
            if (state.addresses.isEmpty()) {
                Text(
                    "You don't have a saved address yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.addresses.forEach { addr ->
                        AddressRow(
                            label = addr.label,
                            line = "${addr.addressLine}, ${addr.city}",
                            isDefault = addr.isDefault,
                            selected = addr.id == state.selectedAddressId,
                            onClick = { viewModel.selectAddress(addr.id) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            FuelGoOutlinedButton(
                text = "+ Add new address",
                onClick = { viewModel.openAddAddressDialog() },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        SectionCard {
            Text("4. Payment method", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
            Column {
                PAYMENT_METHODS.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.setPaymentMethod(value) }
                            .padding(vertical = 4.dp),
                    ) {
                        RadioButton(
                            selected = state.paymentMethod == value,
                            onClick = { viewModel.setPaymentMethod(value) },
                        )
                        Text(label)
                    }
                }
            }
        }

        SectionCard {
            Text("Order summary", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
            SummaryLine("Volume", "${state.quantityLitres.toInt()} L")
            SummaryLine("Unit price", pkr(state.selectedFuel?.pricePerLitre ?: 0.0))
            SummaryLine("Fuel subtotal", pkr(state.fuelSubtotal))
            SummaryLine("Delivery fee", pkr(AppConfig.DELIVERY_FEE_PKR.toDouble()))
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(FuelPrimary, RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Estimated total", color = Color.White.copy(alpha = 0.8f))
                Text(pkr(state.total), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = MaterialTheme.typography.titleLarge.fontSize)
            }

            Spacer(Modifier.height(12.dp))
            state.errorMessage?.let { ErrorBanner(it, modifier = Modifier.padding(bottom = 8.dp)) }
            state.successMessage?.let { SuccessBanner(it, modifier = Modifier.padding(bottom = 8.dp)) }

            FuelGoPrimaryButton(
                text = if (state.isSubmitting) "Placing order…" else "Confirm Order",
                onClick = { viewModel.submitOrder() },
                enabled = state.canSubmit,
                isLoading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }

    if (state.showAddAddressDialog) {
        AddAddressDialog(
            isSaving = state.isAddingAddress,
            errorMessage = state.addAddressError,
            onDismiss = { viewModel.closeAddAddressDialog() },
            onSave = { label, line, area, city -> viewModel.addAddress(label, line, area, city) },
        )
    }
}

@Composable
private fun FuelTypeCard(fuel: FuelTypeDto, selected: Boolean, onClick: () -> Unit) {
    val accent = fuelColorForCode(fuel.code)
    Column(
        modifier = Modifier
            .width(140.dp)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) accent else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(14.dp),
            )
            .background(
                if (selected) accent.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                RoundedCornerShape(14.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(modifier = Modifier.size(30.dp).background(accent, CircleShape)) {}
            if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Text(fuel.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Text(fuel.code, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(pkr(fuel.pricePerLitre) + " / " + (fuel.unitTypeSymbol ?: "L"), fontWeight = FontWeight.Bold, color = accent)
    }
}

@Composable
private fun QuantityChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) FuelAction else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(label, color = fg, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AddressRow(label: String, line: String, isDefault: Boolean, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp),
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            if (isDefault) Icons.Filled.Home else Icons.Filled.LocationOn,
            contentDescription = null,
            tint = if (selected) FuelAction else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            Text(line, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = FuelAction)
    }
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
    }
}
