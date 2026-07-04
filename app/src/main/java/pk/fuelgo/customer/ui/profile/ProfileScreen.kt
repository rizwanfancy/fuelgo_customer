package pk.fuelgo.customer.ui.profile

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pk.fuelgo.customer.ui.components.ErrorBanner
import pk.fuelgo.customer.ui.components.FuelGoOutlinedButton
import pk.fuelgo.customer.ui.components.FuelGoPrimaryButton
import pk.fuelgo.customer.ui.components.FuelGoTextField
import pk.fuelgo.customer.ui.components.LoadingState
import pk.fuelgo.customer.ui.components.SectionCard
import pk.fuelgo.customer.ui.components.AvatarCircle
import pk.fuelgo.customer.ui.fuelGoViewModel

@Composable
fun ProfileScreen(onLoggedOut: () -> Unit) {
    val viewModel = fuelGoViewModel { ProfileViewModel(it.customerRepository, it.authRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.loggedOut) {
        if (state.loggedOut) onLoggedOut()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.isLoading) {
            LoadingState("Loading your profile…")
        } else {
            val profile = state.profile
            if (profile != null) {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarCircle(initials = initialsFor(profile.firstName, profile.lastName))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("${profile.firstName} ${profile.lastName}".trim(), style = MaterialTheme.typography.titleMedium)
                            Text(profile.customerType, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    ProfileRow("Email", profile.email)
                    profile.phoneNumber?.let { ProfileRow("Phone", it) }
                    if (!profile.companyName.isNullOrBlank()) ProfileRow("Company", profile.companyName)
                    ProfileRow("Approval status", if (profile.isApproved) "Approved" else "Pending review")
                }

                if (profile.addresses.isNotEmpty()) {
                    SectionCard {
                        Text("Saved addresses", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        profile.addresses.forEach { addr ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text(addr.label, fontWeight = FontWeight.Bold)
                                Text("${addr.addressLine}, ${addr.city}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else if (state.errorMessage != null) {
                ErrorBanner(state.errorMessage.orEmpty())
            }

            SectionCard {
                FuelGoOutlinedButton(text = "Change password", onClick = { viewModel.openChangePassword() }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                FuelGoPrimaryButton(text = "Log Out", onClick = { viewModel.logout() }, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (state.showChangePassword) {
        ChangePasswordDialog(
            currentPassword = state.currentPassword,
            newPassword = state.newPassword,
            isSubmitting = state.isChangingPassword,
            errorMessage = state.changePasswordMessage,
            onCurrentPasswordChange = viewModel::setCurrentPassword,
            onNewPasswordChange = viewModel::setNewPassword,
            onDismiss = { viewModel.closeChangePassword() },
            onSubmit = { viewModel.submitChangePassword() },
        )
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ChangePasswordDialog(
    currentPassword: String,
    newPassword: String,
    isSubmitting: Boolean,
    errorMessage: String?,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change password") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FuelGoTextField(
                    value = currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = "Current password",
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                )
                FuelGoTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = "New password (min. 8 characters)",
                    isPassword = true,
                    keyboardType = KeyboardType.Password,
                )
                errorMessage?.let { ErrorBanner(it) }
            }
        },
        confirmButton = {
            TextButton(enabled = !isSubmitting, onClick = onSubmit) {
                Text(if (isSubmitting) "Saving…" else "Save")
            }
        },
        dismissButton = { TextButton(enabled = !isSubmitting, onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun initialsFor(firstName: String, lastName: String): String {
    val parts = listOf(firstName, lastName).filter { it.isNotBlank() }
    if (parts.isEmpty()) return "FG"
    return parts.joinToString("") { it.first().uppercaseChar().toString() }
}
