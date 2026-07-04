package pk.fuelgo.customer.data.repository

import kotlinx.coroutines.flow.Flow
import pk.fuelgo.customer.data.local.ApiConfigManager
import pk.fuelgo.customer.data.local.TokenManager
import pk.fuelgo.customer.data.remote.ApiResult
import pk.fuelgo.customer.data.remote.ApiService
import pk.fuelgo.customer.data.remote.dto.ApiMessageDto
import pk.fuelgo.customer.data.remote.dto.AuthResponseDto
import pk.fuelgo.customer.data.remote.dto.ChangePasswordRequest
import pk.fuelgo.customer.data.remote.dto.LoginRequest
import pk.fuelgo.customer.data.remote.dto.RegisterCustomerRequest
import pk.fuelgo.customer.data.remote.dto.RegisterCustomerResponseDto
import pk.fuelgo.customer.data.remote.safeApiCall

class AuthRepository(
    private val api: ApiService,
    private val tokenManager: TokenManager,
    private val apiConfigManager: ApiConfigManager,
) {
    val isLoggedIn: Flow<Boolean> = tokenManager.isLoggedInFlow
    val userName: Flow<String?> = tokenManager.userNameFlow
    val userEmail: Flow<String?> = tokenManager.userEmailFlow

    suspend fun login(username: String, password: String): ApiResult<AuthResponseDto> {
        val tenantSlug = apiConfigManager.currentTenantSlug()
        val result = safeApiCall {
            api.login(LoginRequest(tenantSlug = tenantSlug, username = username, password = password))
        }
        if (result is ApiResult.Success) {
            tokenManager.saveSession(result.data)
        }
        return result
    }

    suspend fun register(request: RegisterCustomerRequest): ApiResult<RegisterCustomerResponseDto> =
        safeApiCall { api.registerCustomer(request) }

    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<ApiMessageDto> =
        safeApiCall { api.changePassword(ChangePasswordRequest(currentPassword, newPassword)) }

    suspend fun logout() {
        tokenManager.clear()
    }
}
