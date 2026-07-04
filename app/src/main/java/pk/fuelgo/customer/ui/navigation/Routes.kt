package pk.fuelgo.customer.ui.navigation

/** Central route registry for both the root nav graph and the Home bottom-nav graph. */
object Routes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val HOME = "home"

    // Bottom-nav destinations, nested inside HOME.
    const val DASHBOARD = "dashboard"
    const val ORDER = "order"
    const val TRACKING = "tracking"
    const val HISTORY = "history"
    const val PROFILE = "profile"
}
