package pk.fuelgo.customer.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pk.fuelgo.customer.ui.components.ErrorBanner
import pk.fuelgo.customer.ui.components.FuelGoPrimaryButton
import pk.fuelgo.customer.ui.components.FuelGoTextField
import pk.fuelgo.customer.ui.components.PillChip
import pk.fuelgo.customer.ui.components.SuccessBanner

@Composable
fun SignupForm(state: SignupUiState, viewModel: AuthViewModel, apiConfigured: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Create your FuelGo account", style = MaterialTheme.typography.titleLarge)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillChip(
                text = "Individual",
                selected = state.accountType == "Individual",
                onClick = { viewModel.updateSignup { it.copy(accountType = "Individual") } },
            )
            PillChip(
                text = "Business",
                selected = state.accountType == "Business",
                onClick = { viewModel.updateSignup { it.copy(accountType = "Business") } },
            )
        }

        FuelGoTextField(
            value = state.email,
            onValueChange = { v -> viewModel.updateSignup { it.copy(email = v) } },
            label = "Email address",
            keyboardType = KeyboardType.Email,
        )

        FuelGoTextField(
            value = state.password,
            onValueChange = { v -> viewModel.updateSignup { it.copy(password = v) } },
            label = "Password (min. 8 characters)",
            isPassword = true,
            keyboardType = KeyboardType.Password,
        )

        FuelGoTextField(
            value = state.mobileNumber,
            onValueChange = { v -> viewModel.updateSignup { it.copy(mobileNumber = v) } },
            label = "Mobile number (03XXXXXXXXX)",
            keyboardType = KeyboardType.Phone,
            supportingText = "e.g. 03001234567",
        )

        AnimatedVisibility(visible = state.accountType == "Individual") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FuelGoTextField(
                    value = state.name,
                    onValueChange = { v -> viewModel.updateSignup { it.copy(name = v) } },
                    label = "Full name",
                )
                FuelGoTextField(
                    value = state.city,
                    onValueChange = { v -> viewModel.updateSignup { it.copy(city = v) } },
                    label = "City",
                )
                FuelGoTextField(
                    value = state.address,
                    onValueChange = { v -> viewModel.updateSignup { it.copy(address = v) } },
                    label = "Delivery address",
                )
                FuelGoTextField(
                    value = state.cnic,
                    onValueChange = { v -> viewModel.updateSignup { it.copy(cnic = v) } },
                    label = "CNIC",
                    supportingText = "Format: 12345-1234567-1",
                )
            }
        }

        AnimatedVisibility(visible = state.accountType == "Business") {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                FuelGoTextField(
                    value = state.businessName,
                    onValueChange = { v -> viewModel.updateSignup { it.copy(businessName = v) } },
                    label = "Business name",
                )
                FuelGoTextField(
                    value = state.location,
                    onValueChange = { v -> viewModel.updateSignup { it.copy(location = v) } },
                    label = "Business location",
                )
                FuelGoTextField(
                    value = state.concernPersonName,
                    onValueChange = { v -> viewModel.updateSignup { it.copy(concernPersonName = v) } },
                    label = "Concerned person's name",
                )
                FuelGoTextField(
                    value = state.ntnNumber,
                    onValueChange = { v -> viewModel.updateSignup { it.copy(ntnNumber = v) } },
                    label = "NTN number",
                )
            }
        }

        state.errorMessage?.let { ErrorBanner(it) }
        state.successMessage?.let { SuccessBanner(it) }

        FuelGoPrimaryButton(
            text = "Create Account",
            onClick = { viewModel.register() },
            enabled = apiConfigured,
            isLoading = state.isSubmitting,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 24.dp),
        )
    }
}
