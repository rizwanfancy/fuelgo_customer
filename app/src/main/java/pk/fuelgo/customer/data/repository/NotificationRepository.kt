package pk.fuelgo.customer.data.repository

import pk.fuelgo.customer.data.remote.ApiResult
import pk.fuelgo.customer.data.remote.ApiService
import pk.fuelgo.customer.data.remote.dto.NotificationDto
import pk.fuelgo.customer.data.remote.safeApiCall

class NotificationRepository(private val api: ApiService) {
    suspend fun getAll(): ApiResult<List<NotificationDto>> = safeApiCall { api.getNotifications() }
    suspend fun markRead(id: String): ApiResult<Unit> = safeApiCall { api.markNotificationRead(id) }
    suspend fun markAllRead(): ApiResult<Unit> = safeApiCall { api.markAllNotificationsRead() }
}
