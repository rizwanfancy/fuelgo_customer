package pk.fuelgo.customer.data.remote.dto

import kotlinx.serialization.Serializable

// Mirrors Backend/FuelGo.Application/Orders/OrderDtos.cs

@Serializable
data class FuelTypeDto(
    val id: String,
    val code: String = "",
    val name: String = "",
    val pricePerLitre: Double = 0.0,
    val description: String? = null,
    val unitTypeId: String? = null,
    val unitTypeName: String? = null,
    val unitTypeSymbol: String? = null,
)

@Serializable
data class PlaceOrderRequest(
    val fuelTypeId: String,
    val quantityLitres: Double,
    val deliveryAddressId: String,
    val paymentMethod: String,
    val scheduledAt: String? = null,
    val deliveryLatitude: Double? = null,
    val deliveryLongitude: Double? = null,
)

@Serializable
data class OrderDto(
    val id: String,
    val orderNumber: String = "",
    val customerId: String = "",
    val fuelTypeName: String = "",
    val fuelTypeCode: String = "",
    val quantityLitres: Double = 0.0,
    val pricePerLitre: Double = 0.0,
    val totalAmount: Double = 0.0,
    val status: String = "Pending",
    val paymentMethod: String = "",
    val deliveryAddress: String = "",
    val deliveryCity: String = "",
    val deliveryLatitude: Double = 0.0,
    val deliveryLongitude: Double = 0.0,
    val scheduledAt: String? = null,
    val eta: String? = null,
    val deliveredAt: String? = null,
    val createdAt: String? = null,
    val driverName: String? = null,
    val cancellationReason: String? = null,
    // Not returned by the current backend OrderDto — kept optional for forward-compatibility.
    val customerRating: Int? = null,
)

@Serializable
data class CancelOrderRequest(
    val reason: String,
)

@Serializable
data class RateOrderRequest(
    val rating: Int,
    val feedback: String? = null,
)
