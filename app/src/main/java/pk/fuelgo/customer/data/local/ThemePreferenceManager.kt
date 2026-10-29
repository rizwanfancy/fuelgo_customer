package pk.fuelgo.customer.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "fuelgo_theme")

enum class ThemeMode { System, Light, Dark }

/** Persists the user's light/dark/system choice. */
class ThemePreferenceManager(private val context: Context) {
    private val key = stringPreferencesKey("theme_mode")

    val modeFlow: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        prefs[key]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.System
    }

    suspend fun setMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[key] = mode.name }
    }
}
