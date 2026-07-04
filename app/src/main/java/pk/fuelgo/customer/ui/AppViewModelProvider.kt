package pk.fuelgo.customer.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import pk.fuelgo.customer.AppContainer
import pk.fuelgo.customer.FuelGoApp

/** Reads the process-wide [AppContainer] held by [FuelGoApp]. */
@Composable
fun rememberAppContainer(): AppContainer {
    val context = LocalContext.current.applicationContext as FuelGoApp
    return context.container
}

/**
 * Builds a Compose-scoped ViewModel wired to the app's repositories, without needing
 * Hilt. Usage: `val vm = fuelGoViewModel { AuthViewModel(it.authRepository) }`.
 */
@Composable
inline fun <reified VM : ViewModel> fuelGoViewModel(crossinline creator: (AppContainer) -> VM): VM {
    val container = rememberAppContainer()
    val factory = viewModelFactory {
        initializer { creator(container) }
    }
    return viewModel(factory = factory)
}
