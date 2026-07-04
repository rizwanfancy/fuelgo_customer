package pk.fuelgo.customer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import pk.fuelgo.customer.ui.auth.AuthScreen
import pk.fuelgo.customer.ui.home.HomeScreen
import pk.fuelgo.customer.ui.splash.SplashScreen

/**
 * Top-level graph: Splash decides whether a saved session exists, then routes to
 * either the Auth flow (Login/Signup) or the Home flow (bottom-nav with
 * Order/Tracking/My Orders/Profile).
 */
@Composable
fun FuelGoNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(
                onResult = { loggedIn ->
                    val destination = if (loggedIn) Routes.HOME else Routes.AUTH
                    navController.navigate(destination) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.AUTH) {
            AuthScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.AUTH) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onLoggedOut = {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }
    }
}
