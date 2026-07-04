package pk.fuelgo.customer.data.remote

import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import pk.fuelgo.customer.data.remote.dto.ApiMessageDto
import retrofit2.Response
import java.io.IOException

/** Simple success/error wrapper so ViewModels never have to deal with exceptions directly. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val message: String) : ApiResult<Nothing>
}

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Runs a Retrofit suspend call, converting network failures, HTTP errors, and the API's
 * `{ "message": "..." }` error envelope into a single [ApiResult].
 */
suspend fun <T> safeApiCall(block: suspend () -> Response<T>): ApiResult<T> {
    return try {
        val response = block()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                ApiResult.Success(body)
            } else {
                @Suppress("UNCHECKED_CAST")
                ApiResult.Success(Unit as T)
            }
        } else {
            ApiResult.Error(extractErrorMessage(response.errorBody(), response.code()))
        }
    } catch (e: IOException) {
        val fallback = "Can't reach the FuelGo server. Check your Wi-Fi and the server address under API Configuration."
        ApiResult.Error(e.message?.takeIf { it.isNotBlank() } ?: fallback)
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Something went wrong. Please try again.")
    }
}

private fun extractErrorMessage(errorBody: ResponseBody?, httpCode: Int): String {
    val raw = errorBody?.string()
    if (!raw.isNullOrBlank()) {
        val parsed = runCatching { errorJson.decodeFromString(ApiMessageDto.serializer(), raw) }.getOrNull()
        val message = parsed?.message
        if (!message.isNullOrBlank()) return message
    }
    return when (httpCode) {
        401 -> "Invalid credentials. Please sign in again."
        403 -> "You don't have permission to do that."
        404 -> "That resource could not be found."
        in 500..599 -> "Server error ($httpCode). Please try again shortly."
        else -> "Request failed ($httpCode)."
    }
}

/** Extracts the ApiMessageDto's message from a successful response, when present. */
val ApiMessageDto?.textOrDefault: String
    get() = this?.message?.takeIf { it.isNotBlank() } ?: "Done."
