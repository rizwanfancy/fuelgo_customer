package pk.fuelgo.customer.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pk.fuelgo.customer.ui.components.ErrorBanner
import pk.fuelgo.customer.ui.config.ApiConfigDialog
import pk.fuelgo.customer.ui.fuelGoViewModel
import pk.fuelgo.customer.ui.rememberAppContainer
import pk.fuelgo.customer.ui.theme.FuelAction
import pk.fuelgo.customer.ui.theme.FuelPrimary

@Composable
fun AuthScreen(onLoginSuccess: () -> Unit) {
    val container = rememberAppContainer()
    val viewModel = fuelGoViewModel { AuthViewModel(it.authRepository) }
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    val signupState by viewModel.signupState.collectAsStateWithLifecycle()
    val isApiConfigured by container.apiConfigManager.isConfiguredFlow.collectAsStateWithLifecycle(initialValue = true)

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    var showApiConfigDialog by remember { mutableStateOf(false) }
    var hasAutoPromptedConfig by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(loginState.isLoggedIn) {
        if (loginState.isLoggedIn) onLoginSuccess()
    }

    LaunchedEffect(signupState.successMessage) {
        if (signupState.successMessage != null) {
            viewModel.prefillLoginUsername(signupState.email)
            selectedTab = 0
        }
    }

    // First launch (or a fresh install): nudge the tester to point the app at their
    // backend before they try to sign in against nothing.
    LaunchedEffect(isApiConfigured) {
        if (!isApiConfigured && !hasAutoPromptedConfig) {
            showApiConfigDialog = true
            hasAutoPromptedConfig = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Box {
            AuthHeader()

            Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp)) {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More options", tint = Color.White)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("API Configuration") },
                        onClick = {
                            showMenu = false
                            showApiConfigDialog = true
                        },
                    )
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Sign In", fontWeight = FontWeight.Bold) },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Create Account", fontWeight = FontWeight.Bold) },
            )
        }

        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!isApiConfigured) {
                ErrorBanner("Server not configured yet. Tap ⋮ above and set your FuelGo API address under API Configuration.")
            }

            if (selectedTab == 0) {
                LoginForm(state = loginState, viewModel = viewModel, apiConfigured = isApiConfigured)
            } else {
                SignupForm(state = signupState, viewModel = viewModel, apiConfigured = isApiConfigured)
            }
        }
    }

    if (showApiConfigDialog) {
        ApiConfigDialog(onDismiss = { showApiConfigDialog = false })
    }
}

@Composable
private fun AuthHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FuelPrimary)
            .padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(FuelAction, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.LocalGasStation, contentDescription = null, tint = Color.White)
        }
        Text(
            "FuelGo",
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            "On-demand fuel delivery for Karachi",
            color = Color.White.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }
}
