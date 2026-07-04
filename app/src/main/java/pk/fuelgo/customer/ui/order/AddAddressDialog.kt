package pk.fuelgo.customer.ui.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import pk.fuelgo.customer.ui.components.ErrorBanner
import pk.fuelgo.customer.ui.components.FuelGoTextField

@Composable
fun AddAddressDialog(
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (label: String, addressLine: String, area: String, city: String) -> Unit,
) {
    var label by remember { mutableStateOf("Home") }
    var addressLine by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Karachi") }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Add delivery address") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FuelGoTextField(value = label, onValueChange = { label = it }, label = "Label (e.g. Home, Office)")
                FuelGoTextField(value = addressLine, onValueChange = { addressLine = it }, label = "Address line")
                FuelGoTextField(value = area, onValueChange = { area = it }, label = "Area / neighbourhood")
                FuelGoTextField(value = city, onValueChange = { city = it }, label = "City")
                errorMessage?.let { ErrorBanner(it) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = { onSave(label, addressLine, area, city) },
            ) {
                Text(if (isSaving) "Saving…" else "Save address")
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
