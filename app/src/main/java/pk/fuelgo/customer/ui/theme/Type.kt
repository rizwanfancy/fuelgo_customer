package pk.fuelgo.customer.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// The web portal uses the "Inter" font. Inter isn't bundled in this project (no font
// files to ship), so we fall back to the platform default sans-serif, which is visually
// close. Drop Inter *.ttf files into res/font and swap FontFamily.Default below to use it.
private val FuelGoFontFamily = FontFamily.SansSerif

val FuelGoTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FuelGoFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = FuelGoFontFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = FuelGoFontFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FuelGoFontFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontFamily = FuelGoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    bodyLarge = TextStyle(fontFamily = FuelGoFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FuelGoFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = FuelGoFontFamily, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = FuelGoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = FuelGoFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = FuelGoFontFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.5.sp),
)
