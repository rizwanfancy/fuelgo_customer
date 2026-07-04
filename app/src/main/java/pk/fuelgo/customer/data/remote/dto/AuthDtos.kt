package pk.fuelgo.customer.data.remote.dto

import kotlinx.serialization.Serializable

// Mirrors Backend/FuelGo.Application/Auth/AuthDtos.kt (LoginRequest record)
@Serializable
data class LoginRequest(
    val tenantSlug: String,
    val username: String,
    val password: String,
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String,
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)

// Mirrors AuthResponse record returned by POST /api/auth/login and /api/auth/refresh-token
@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresAt: String? = null,
    val refreshTokenExpiresAt: String? = null,
    val user: UserProfileDto,
)

// Mirrors UserProfileDto in Backend/FuelGo.Application/Tenancy/TenantDtos.cs
@Serializable
data class UserProfileDto(
    val id: String,
    val tenantId: String,
    val email: String,
    val phoneNumber: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val userType: UserTypeDto? = null,
    val roles: List<String> = emptyList(),
)

@Serializable
data class UserTypeDto(
    val id: String? = null,
    val name: String? = null,
    val code: String? = null,
    val description: String? = null,
    val isSystemType: Boolean? = null,
    val createdAt: String? = null,
)

/** Generic `{ "message": "..." }` envelope the API returns on 400/401/404 responses. */
@Serializable
data class ApiMessageDto(
    val message: String? = null,
)
