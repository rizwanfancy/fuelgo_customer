package pk.fuelgo.customer.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import pk.fuelgo.customer.R
import pk.fuelgo.customer.ui.rememberAppContainer
import pk.fuelgo.customer.ui.theme.FuelPrimary

@Composable
fun SplashScreen(onResult: (loggedIn: Boolean) -> Unit) {
    val container = rememberAppContainer()
    var animationStarted by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.85f,
        animationSpec = tween(durationMillis = 500),
        label = "splash-logo-scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "splash-logo-alpha",
    )

    LaunchedEffect(Unit) {
        animationStarted = true
        // Small minimum splash time so the logo animation is actually visible even when
        // the session check below resolves instantly.
        delay(700)
        val loggedIn = container.authRepository.isLoggedIn.first()
        onResult(loggedIn)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(FuelPrimary),
        contentAlignment = Alignment.Center,
    ) {
        // Faint full-bleed brand watermark, matching the web portal's dark login backdrop.
        Image(
            painter = painterResource(id = R.drawable.fuelgo_watermark),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.10f,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha),
        ) {
            Image(
                painter = painterResource(id = R.drawable.fuelgo_logo_full),
                contentDescription = "FuelGo",
                modifier = Modifier.size(width = 220.dp, height = 120.dp),
            )
            Text(
                "ON-DEMAND FUEL DELIVERY PLATFORM",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.padding(top = 40.dp).size(28.dp),
            )
        }
    }
}
