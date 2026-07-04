package pk.fuelgo.customer.ui.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import pk.fuelgo.customer.R
import pk.fuelgo.customer.ui.rememberAppContainer
import pk.fuelgo.customer.ui.theme.FuelAction
import pk.fuelgo.customer.ui.theme.FuelPrimary

@Composable
fun SplashScreen(onResult: (loggedIn: Boolean) -> Unit) {
    val container = rememberAppContainer()
    var animationStarted by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0.6f,
        animationSpec = tween(durationMillis = 450),
        label = "splash-logo-scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (animationStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 450),
        label = "splash-logo-alpha",
    )

    LaunchedEffect(Unit) {
        animationStarted = true
        // Small minimum splash time so the logo animation is actually visible even when
        // the session check below resolves instantly.
        delay(600)
        val loggedIn = container.authRepository.isLoggedIn.first()
        onResult(loggedIn)
    }

    Box(
        modifier = Modifier.fillMaxSize().background(FuelPrimary),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                    .background(FuelAction, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "FuelGo logo",
                    modifier = Modifier.size(72.dp),
                )
            }
            Text(
                "FuelGo",
                color = Color.White,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(top = 20.dp).graphicsLayer(alpha = alpha),
            )
            Text(
                "On-demand fuel delivery for Karachi",
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp).graphicsLayer(alpha = alpha),
            )
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.dp,
                modifier = Modifier.padding(top = 32.dp).size(28.dp),
            )
        }
    }
}
