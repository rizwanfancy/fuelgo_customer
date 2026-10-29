package pk.fuelgo.customer.ui.order

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import pk.fuelgo.customer.ui.components.FuelGoOutlinedButton
import pk.fuelgo.customer.ui.components.PillChip
import pk.fuelgo.customer.ui.components.SectionCard
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val scheduleFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy · hh:mm a")

/** "Deliver now" vs "Schedule for later" picker. A null [scheduledAt] means deliver now. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryTimingCard(scheduledAt: LocalDateTime?, onChange: (LocalDateTime?) -> Unit) {
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var pickedDate by remember { mutableStateOf<java.time.LocalDate?>(null) }

    SectionCard {
        Text("Delivery timing", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillChip("Deliver now", selected = scheduledAt == null, onClick = { onChange(null) })
            PillChip("Schedule", selected = scheduledAt != null, onClick = { showDate = true })
        }
        if (scheduledAt != null) {
            Spacer(Modifier.height(12.dp))
            FuelGoOutlinedButton(
                text = scheduledAt.format(scheduleFormat),
                onClick = { showDate = true },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    // DatePicker yields UTC-midnight millis; read it back in UTC to keep the chosen day.
                    pickedDate = state.selectedDateMillis?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    showDate = false
                    if (pickedDate != null) showTime = true
                }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Cancel") } },
        ) { DatePicker(state = state) }
    }

    val date = pickedDate
    if (showTime && date != null) {
        val initial = LocalTime.now().plusHours(1)
        val time = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute)
        AlertDialog(
            onDismissRequest = { showTime = false },
            title = { Text("Delivery time") },
            text = { TimePicker(state = time) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(LocalDateTime.of(date, LocalTime.of(time.hour, time.minute)))
                    showTime = false
                }) { Text("Set") }
            },
            dismissButton = { TextButton(onClick = { showTime = false }) { Text("Cancel") } },
        )
    }
}

/** Optional exact drop-pin: uses the device location and sends it with the order. */
@Composable
fun DeliveryLocationCard(
    latitude: Double?,
    longitude: Double?,
    error: String?,
    onLocation: (Double, Double) -> Unit,
    onClear: () -> Unit,
    onError: (String) -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) fetchLocation(context, onLocation, onError) else onError("Location permission denied.")
    }

    SectionCard {
        Text("Exact location (optional)", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
        Text(
            "Pin your current position so the driver can find you precisely.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        if (latitude != null && longitude != null) {
            Text("Pinned: ${"%.5f".format(latitude)}, ${"%.5f".format(longitude)}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            FuelGoOutlinedButton(text = "Remove pin", onClick = onClear, modifier = Modifier.fillMaxWidth())
        } else {
            FuelGoOutlinedButton(
                text = "Use my current location",
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) fetchLocation(context, onLocation, onError) else launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@SuppressLint("MissingPermission") // Caller checks ACCESS_FINE_LOCATION before invoking.
private fun fetchLocation(context: Context, onLocation: (Double, Double) -> Unit, onError: (String) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val provider = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).firstOrNull {
        runCatching { manager.isProviderEnabled(it) }.getOrDefault(false)
    }
    if (provider == null) {
        onError("Turn on location services and try again.")
        return
    }
    LocationManagerCompat.getCurrentLocation(manager, provider, null, ContextCompat.getMainExecutor(context)) { location ->
        if (location != null) onLocation(location.latitude, location.longitude) else onError("Couldn't get your location. Try again outdoors.")
    }
}
