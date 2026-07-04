package pk.fuelgo.customer.data.repository

import pk.fuelgo.customer.data.remote.ApiResult
import pk.fuelgo.customer.data.remote.ApiService
import pk.fuelgo.customer.data.remote.dto.ApiMessageDto
import pk.fuelgo.customer.data.remote.dto.CancelOrderRequest
import pk.fuelgo.customer.data.remote.dto.FuelTypeDto
import pk.fuelgo.customer.data.remote.dto.OrderDto
import pk.fuelgo.customer.data.remote.dto.PlaceOrderRequest
import pk.fuelgo.customer.data.remote.dto.RateOrderRequest
import pk.fuelgo.customer.data.remote.safeApiCall

class OrderRepository(private val api: ApiService) {

    suspend fun getFuelTypes(): ApiResult<List<FuelTypeDto>> =
        safeApiCall { api.getFuelTypes() }

    suspend fun placeOrder(
        fuelTypeId: String,
        quantityLitres: Double,
        deliveryAddressId: String,
        paymentMethod: String,
    ): ApiResult<OrderDto> = safeApiCall {
        api.placeOrder(
            PlaceOrderRequest(
                fuelTypeId = fuelTypeId,
                quantityLitres = quantityLitres,
                deliveryAddressId = deliveryAddressId,
                paymentMethod = paymentMethod,
            ),
        )
    }

    suspend fun getMyOrders(): ApiResult<List<OrderDto>> =
        safeApiCall { api.getMyOrders() }

    suspend fun getOrder(id: String): ApiResult<OrderDto> =
        safeApiCall { api.getOrder(id) }

    suspend fun cancelOrder(id: String, reason: String): ApiResult<ApiMessageDto> =
        safeApiCall { api.cancelOrder(id, CancelOrderRequest(reason)) }

    suspend fun rateOrder(id: String, rating: Int, feedback: String?): ApiResult<ApiMessageDto> =
        safeApiCall { api.rateOrder(id, RateOrderRequest(rating, feedback?.takeIf { it.isNotBlank() })) }
}
