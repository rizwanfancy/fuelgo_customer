package pk.fuelgo.customer.ui.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pk.fuelgo.customer.data.local.ApiConfigManager
import pk.fuelgo.customer.ui.components.ErrorBanner
import pk.fuelgo.customer.ui.components.FuelGoOutlinedButton
import pk.fuelgo.customer.ui.components.FuelGoTextField
import pk.fuelgo.customer.ui.components.SuccessBanner
import pk.fuelgo.customer.ui.fuelGoViewModel

/**
 * "API Configuration" — reachable from the ⋮ menu on the Login screen and the app's top
 * bar. Lets the tester point this build at any FuelGo backend (different Wi-Fi, PC, or
 * staging server) without needing a new APK.
 */
@Composable
fun ApiConfigDialog(onDismiss: () -> Unit) {
    val viewModel = fuelGoViewModel { ApiConfigViewModel(it.apiConfigManager, it.orderRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Point this app at your FuelGo backend. Saved on this device only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FuelGoTextField(
                    value = state.baseUrlInput,
                    onValueChange = viewModel::setBaseUrlInput,
                    label = "Server base URL",
                    keyboardType = KeyboardType.Uri,
                    supportingText = "e.g. ${ApiConfigManager.EXAMPLE_BASE_URL}",
                )
                FuelGoTextField(
                    value = state.tenantSlugInput,
                    onValueChange = viewModel::setTenantSlugInput,
                    label = "Tenant slug",
                    supportingText = "Default: ${ApiConfigManager.DEFAULT_TENANT_SLUG}",
                )

                state.testResultMessage?.let { message ->
                    if (state.testResultIsSuccess == true) SuccessBanner(message) else ErrorBanner(message)
                }
                state.saveMessage?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FuelGoOutlinedButton(
                        text = if (state.isTesting) "Testing…" else "Test connection",
                        onClick = { viewModel.testConnection() },
                        enabled = !state.isTesting,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.save() }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}
