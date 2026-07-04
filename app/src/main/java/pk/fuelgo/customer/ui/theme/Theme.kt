package pk.fuelgo.customer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FuelGoColorScheme = lightColorScheme(
    primary = FuelPrimary,
    onPrimary = Color.White,
    primaryContainer = FuelPrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = FuelAction,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBD0),
    onSecondaryContainer = FuelAction,
    background = FuelBackground,
    onBackground = FuelTextPrimary,
    surface = FuelSurface,
    onSurface = FuelTextPrimary,
    surfaceVariant = FuelSurfaceLow,
    onSurfaceVariant = FuelMuted,
    outline = FuelBorder,
    error = StatusDanger,
    onError = Color.White,
    errorContainer = StatusDangerBg,
)

// Mostly 8dp radius; larger bottom-sheet style containers use 16-24dp — mirrors the web app.
private val FuelGoShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

@Composable
fun FuelGoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FuelGoColorScheme,
        typography = FuelGoTypography,
        shapes = FuelGoShapes,
        content = content,
    )
}
