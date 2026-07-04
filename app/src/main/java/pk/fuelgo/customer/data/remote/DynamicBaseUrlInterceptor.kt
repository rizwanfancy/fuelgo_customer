package pk.fuelgo.customer.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import pk.fuelgo.customer.data.local.ApiConfigManager
import java.io.IOException

/**
 * Retrofit needs a real, syntactically valid base URL at build time — but this app lets
 * the user change the server address at runtime from the "API Configuration" screen, and
 * Retrofit doesn't support swapping its base URL after construction.
 *
 * The trick: [pk.fuelgo.customer.data.remote.NetworkModule] builds Retrofit against a fake,
 * unreachable "anchor" host ([ApiConfigManager.RETROFIT_ANCHOR_URL]). Every `@GET`/`@POST`
 * endpoint path Retrofit resolves against that anchor is only ever used to figure out the
 * *relative* path (e.g. `/Auth/login`, `/orders/place`) — this interceptor then throws that
 * anchor away and re-roots the exact same relative path (plus any query string) onto
 * whatever server address is currently saved in [ApiConfigManager].
 */
class DynamicBaseUrlInterceptor(private val apiConfigManager: ApiConfigManager) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        val configuredBaseUrl = runBlocking { apiConfigManager.currentBaseUrl() }
        if (configuredBaseUrl.isNullOrBlank()) {
            throw IOException("The FuelGo server address hasn't been configured yet. Open the ⋮ menu and set it under API Configuration.")
        }

        val relativePath = original.url.encodedPath // e.g. "/Auth/login" — resolved against the anchor, so this IS the real relative endpoint path
        val relativeQuery = original.url.encodedQuery
        val fullUrlString = buildString {
            append(configuredBaseUrl)
            append(relativePath)
            if (!relativeQuery.isNullOrEmpty()) {
                append('?')
                append(relativeQuery)
            }
        }

        val newHttpUrl = fullUrlString.toHttpUrlOrNull()
            ?: throw IOException("The saved FuelGo server address (\"$configuredBaseUrl\") isn't a valid URL. Fix it under API Configuration.")

        val newRequest = original.newBuilder().url(newHttpUrl).build()
        return try {
            chain.proceed(newRequest)
        } catch (e: IOException) {
            // Re-throw with the exact address we attempted baked into the message, so the
            // on-screen error tells you precisely what the app tried to reach — no need for
            // logcat/adb to see it.
            throw IOException("Could not reach $newHttpUrl — ${e.message}", e)
        }
    }
}
