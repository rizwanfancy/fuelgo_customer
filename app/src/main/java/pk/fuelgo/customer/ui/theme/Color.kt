package pk.fuelgo.customer.ui.theme

import androidx.compose.ui.graphics.Color

// Brand palette — matches Project/FuelGo_Project_Context.md "Visual System"
val FuelPrimary = Color(0xFF000F22) // deep industrial blue
val FuelPrimaryContainer = Color(0xFF0A2540)
val FuelAction = Color(0xFFFE6431) // orange — buttons, CTAs, active states
val FuelBackground = Color(0xFFF8F9FF)
val FuelSurface = Color(0xFFFFFFFF)
val FuelSurfaceLow = Color(0xFFF1F3FA)
val FuelBorder = Color(0xFFC4C6CE)
val FuelMuted = Color(0xFF64748B)
val FuelTextPrimary = Color(0xFF10131A)

// Status colors
val StatusSuccess = Color(0xFF107A45)
val StatusSuccessBg = Color(0xFFDFF7E9)
val StatusWarning = Color(0xFFF97316)
val StatusWarningBg = Color(0xFFFFF3CD)
val StatusInfo = Color(0xFF3E89FC)
val StatusInfoBg = Color(0xFFCFE2FF)
val StatusDanger = Color(0xFFEF4444)
val StatusDangerBg = Color(0xFFFFF0F0)

// Fuel type accent colors — matches Angular fuel-order component's FUEL_META map
val FuelColorMS = Color(0xFFFE6431)
val FuelColorHSD = Color(0xFF0A2540)
val FuelColorHOBC = Color(0xFF3E89FC)
val FuelColorSKO = Color(0xFFA45A00)
val FuelColorLPG = Color(0xFF107A45)
val FuelColorDefault = Color(0xFF64748B)

fun fuelColorForCode(code: String?): Color = when (code?.uppercase()) {
    "MS" -> FuelColorMS
    "HSD" -> FuelColorHSD
    "HOBC" -> FuelColorHOBC
    "SKO" -> FuelColorSKO
    "LPG" -> FuelColorLPG
    else -> FuelColorDefault
}
