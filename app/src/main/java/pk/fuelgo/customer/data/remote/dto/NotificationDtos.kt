package pk.fuelgo.customer.data.remote.dto

import kotlinx.serialization.Serializable

// Mirrors Backend/FuelGo.Application/Notifications/INotificationService.cs
@Serializable
data class NotificationDto(
    val id: String,
    val orderId: String? = null,
    val type: String = "",
    val title: String = "",
    val body: String = "",
    val isRead: Boolean = false,
    val createdAt: String? = null,
)
