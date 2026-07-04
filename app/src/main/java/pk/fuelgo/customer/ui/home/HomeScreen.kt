package pk.fuelgo.customer.ui.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import pk.fuelgo.customer.ui.config.ApiConfigDialog
import pk.fuelgo.customer.ui.dashboard.DashboardScreen
import pk.fuelgo.customer.ui.history.HistoryScreen
import pk.fuelgo.customer.ui.navigation.Routes
import pk.fuelgo.customer.ui.order.OrderScreen
import pk.fuelgo.customer.ui.profile.ProfileScreen
import pk.fuelgo.customer.ui.theme.FuelAction
import pk.fuelgo.customer.ui.theme.FuelPrimary
import pk.fuelgo.customer.ui.tracking.TrackingScreen

private data class BottomTab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val bottomTabs = listOf(
    BottomTab(Routes.DASHBOARD, "Dashboard", Icons.Filled.Dashboard),
    BottomTab(Routes.ORDER, "Order", Icons.Filled.ShoppingCart),
    BottomTab(Routes.TRACKING, "Tracking", Icons.Filled.LocalShipping),
    BottomTab(Routes.HISTORY, "My Orders", Icons.Filled.Receipt),
    BottomTab(Routes.PROFILE, "Profile", Icons.Filled.Person),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onLoggedOut: () -> Unit) {
    val navController = rememberNavController()
    var showMenu by remember { mutableStateOf(false) }
    var showApiConfigDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            val title = bottomTabs.firstOrNull { it.route == currentRoute }?.label ?: "FuelGo"

            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FuelPrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
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
                },
            )
        },
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination

            NavigationBar {
                bottomTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FuelAction,
                            selectedTextColor = FuelAction,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    onGoToOrder = { navController.navigate(Routes.ORDER) { launchSingleTop = true } },
                    onGoToTracking = { navController.navigate(Routes.TRACKING) { launchSingleTop = true } },
                )
            }
            composable(Routes.ORDER) { OrderScreen() }
            composable(Routes.TRACKING) {
                TrackingScreen(onGoToOrder = {
                    navController.navigate(Routes.ORDER) { launchSingleTop = true }
                })
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onGoToOrder = {
                    navController.navigate(Routes.ORDER) { launchSingleTop = true }
                })
            }
            composable(Routes.PROFILE) { ProfileScreen(onLoggedOut = onLoggedOut) }
        }
    }

    if (showApiConfigDialog) {
        ApiConfigDialog(onDismiss = { showApiConfigDialog = false })
    }
}
