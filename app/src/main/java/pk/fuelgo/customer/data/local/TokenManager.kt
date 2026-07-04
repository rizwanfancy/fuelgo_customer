package pk.fuelgo.customer.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import pk.fuelgo.customer.data.remote.dto.AuthResponseDto

private val Context.authDataStore by preferencesDataStore(name = "fuelgo_auth")

/**
 * Persists the JWT access/refresh token pair plus a small snapshot of the logged-in user,
 * mirroring what the Angular app keeps in localStorage (fuelgo.auth.token / .session).
 */
class TokenManager(private val context: Context) {

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PHONE = stringPreferencesKey("user_phone")
        val USER_ROLE_CODE = stringPreferencesKey("user_role_code")
    }

    val accessTokenFlow: Flow<String?> = context.authDataStore.data.map { it[Keys.ACCESS_TOKEN] }
    val isLoggedInFlow: Flow<Boolean> = accessTokenFlow.map { !it.isNullOrBlank() }
    val userNameFlow: Flow<String?> = context.authDataStore.data.map { it[Keys.USER_NAME] }
    val userEmailFlow: Flow<String?> = context.authDataStore.data.map { it[Keys.USER_EMAIL] }

    suspend fun accessToken(): String? = accessTokenFlow.first()

    suspend fun refreshToken(): String? =
        context.authDataStore.data.map { it[Keys.REFRESH_TOKEN] }.first()

    suspend fun saveSession(auth: AuthResponseDto) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = auth.accessToken
            prefs[Keys.REFRESH_TOKEN] = auth.refreshToken
            prefs[Keys.USER_ID] = auth.user.id
            prefs[Keys.USER_EMAIL] = auth.user.email
            prefs[Keys.USER_PHONE] = auth.user.phoneNumber ?: ""
            prefs[Keys.USER_ROLE_CODE] = auth.user.userType?.code ?: ""
            val first = auth.user.firstName.orEmpty()
            val last = auth.user.lastName.orEmpty()
            val fullName = "$first $last".trim()
            prefs[Keys.USER_NAME] = fullName.ifBlank { auth.user.email }
        }
    }

    suspend fun updateTokens(accessToken: String, refreshToken: String) {
        context.authDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }
}
