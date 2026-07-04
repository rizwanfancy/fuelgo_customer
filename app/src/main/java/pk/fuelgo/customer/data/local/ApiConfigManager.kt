package pk.fuelgo.customer.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.apiConfigDataStore by preferencesDataStore(name = "fuelgo_api_config")

/**
 * Persists the FuelGo API server address at runtime — deliberately NOT a compile-time
 * constant, since the backend can move (different Wi-Fi, different PC, staging vs
 * local) without needing a new app build. Edited from the "API Configuration" screen,
 * reachable from the overflow menu on the Login screen and inside the app's top bar.
 */
class ApiConfigManager(private val context: Context) {

    private object Keys {
        val BASE_URL = stringPreferencesKey("base_url")
        val TENANT_SLUG = stringPreferencesKey("tenant_slug")
    }

    companion object {
        const val DEFAULT_TENANT_SLUG = "fuelgo"

        /** Example shown as placeholder text in the config screen — never used as a live value. */
        const val EXAMPLE_BASE_URL = "http://192.168.1.42:5086/api"

        /**
         * A syntactically valid but unreachable "anchor" URL Retrofit is built against once,
         * at process start. [pk.fuelgo.customer.data.remote.DynamicBaseUrlInterceptor] rewrites
         * every outgoing request to the real, currently configured server before it hits the
         * network — see that class for why this two-step approach is needed.
         */
        const val RETROFIT_ANCHOR_URL = "http://fuelgo.invalid/"

        fun normalize(rawUrl: String): String {
            var value = rawUrl.trim()
            if (value.isNotEmpty() && !value.contains("://")) {
                value = "http://$value"
            }
            while (value.endsWith("/")) {
                value = value.dropLast(1)
            }
            return value
        }
    }

    val baseUrlFlow: Flow<String?> = context.apiConfigDataStore.data.map { it[Keys.BASE_URL] }

    val tenantSlugFlow: Flow<String> =
        context.apiConfigDataStore.data.map { it[Keys.TENANT_SLUG]?.takeIf { slug -> slug.isNotBlank() } ?: DEFAULT_TENANT_SLUG }

    val isConfiguredFlow: Flow<Boolean> = baseUrlFlow.map { !it.isNullOrBlank() }

    suspend fun currentBaseUrl(): String? = baseUrlFlow.first()
    suspend fun currentTenantSlug(): String = tenantSlugFlow.first()
    suspend fun isConfigured(): Boolean = !currentBaseUrl().isNullOrBlank()

    suspend fun saveBaseUrl(rawUrl: String) {
        val normalized = normalize(rawUrl)
        context.apiConfigDataStore.edit { it[Keys.BASE_URL] = normalized }
    }

    suspend fun saveTenantSlug(slug: String) {
        val normalized = slug.trim().ifBlank { DEFAULT_TENANT_SLUG }
        context.apiConfigDataStore.edit { it[Keys.TENANT_SLUG] = normalized }
    }

    suspend fun clear() {
        context.apiConfigDataStore.edit { it.clear() }
    }
}
