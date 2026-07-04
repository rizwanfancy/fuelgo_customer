package pk.fuelgo.customer.data.remote

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import pk.fuelgo.customer.BuildConfig
import pk.fuelgo.customer.config.AppConfig
import pk.fuelgo.customer.data.local.ApiConfigManager
import pk.fuelgo.customer.data.local.TokenManager
import pk.fuelgo.customer.data.remote.dto.RefreshTokenRequest
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Small hand-rolled DI container for the network layer (no Hilt/Koin dependency needed
 * for an app this size). Instantiate once from FuelGoApp and reuse everywhere.
 *
 * Retrofit is built once against a fake anchor URL ([ApiConfigManager.RETROFIT_ANCHOR_URL]);
 * [DynamicBaseUrlInterceptor] re-roots every request onto whatever server address the user
 * has actually configured, read fresh from [ApiConfigManager] on every call. This is what
 * lets the API server address be changed at runtime from the app's UI instead of being
 * baked into the APK at build time.
 */
class NetworkModule(
    private val tokenManager: TokenManager,
    private val apiConfigManager: ApiConfigManager,
) {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    /** Adds the tenant slug header FuelGo's multi-tenant backend expects on every call. */
    private val tenantHeaderInterceptor = Interceptor { chain ->
        val tenantSlug = runBlocking { apiConfigManager.currentTenantSlug() }
        val request = chain.request().newBuilder()
            .header("slug", tenantSlug)
            .build()
        chain.proceed(request)
    }

    /** Attaches the current access token, if any, as a Bearer header. */
    private val authHeaderInterceptor = Interceptor { chain ->
        val token = runBlocking { tokenManager.accessToken() }
        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val dynamicBaseUrlInterceptor = DynamicBaseUrlInterceptor(apiConfigManager)

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    }

    /**
     * A plain client (no auth/refresh logic) used only to call the refresh-token endpoint
     * itself — using the main client here would recurse back into the authenticator.
     */
    private val refreshOnlyClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(tenantHeaderInterceptor)
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(AppConfig.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private val refreshOnlyApi: ApiService by lazy {
        buildRetrofit(refreshOnlyClient).create(ApiService::class.java)
    }

    /** Handles automatic access-token refresh whenever the API answers 401. */
    private val tokenAuthenticator = object : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            // Never retry more than once — avoids infinite loops if the refresh itself fails.
            if (responseCount(response) >= 2) return null

            val refreshToken = runBlocking { tokenManager.refreshToken() }
            if (refreshToken.isNullOrBlank()) return null

            return try {
                val refreshResponse = runBlocking { refreshOnlyApi.refreshToken(RefreshTokenRequest(refreshToken)) }
                val body = if (refreshResponse.isSuccessful) refreshResponse.body() else null
                if (body != null) {
                    runBlocking { tokenManager.updateTokens(body.accessToken, body.refreshToken) }
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${body.accessToken}")
                        .build()
                } else {
                    runBlocking { tokenManager.clear() }
                    null
                }
            } catch (_: Exception) {
                null
            }
        }

        private fun responseCount(response: Response): Int {
            var result = 1
            var prior = response.priorResponse
            while (prior != null) {
                result++
                prior = prior.priorResponse
            }
            return result
        }
    }

    private val mainClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(tenantHeaderInterceptor)
            .addInterceptor(authHeaderInterceptor)
            .addInterceptor(dynamicBaseUrlInterceptor)
            .addInterceptor(loggingInterceptor)
            .authenticator(tokenAuthenticator)
            .connectTimeout(AppConfig.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    private fun buildRetrofit(client: OkHttpClient): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(ApiConfigManager.RETROFIT_ANCHOR_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    val apiService: ApiService by lazy {
        buildRetrofit(mainClient).create(ApiService::class.java)
    }
}
