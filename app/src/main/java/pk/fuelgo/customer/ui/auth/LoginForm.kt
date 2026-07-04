package pk.fuelgo.customer.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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

@Composable
fun LoginForm(state: LoginUiState, viewModel: AuthViewModel, apiConfigured: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "Welcome back",
            style = MaterialTheme.typography.titleLarge,
        )

        FuelGoTextField(
            value = state.username,
            onValueChange = { value -> viewModel.updateLogin { it.copy(username = value) } },
            label = "Email or mobile number",
            keyboardType = KeyboardType.Email,
        )

        FuelGoTextField(
            value = state.password,
            onValueChange = { value -> viewModel.updateLogin { it.copy(password = value) } },
            label = "Password",
            isPassword = true,
            keyboardType = KeyboardType.Password,
        )

        state.errorMessage?.let { ErrorBanner(it) }

        FuelGoPrimaryButton(
            text = "Sign In",
            onClick = { viewModel.login() },
            enabled = apiConfigured,
            isLoading = state.isSubmitting,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        )

        Text(
            "New customer accounts are activated immediately for local testing — sign in right after creating one.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
