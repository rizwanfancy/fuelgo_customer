package pk.fuelgo.customer.data.remote

import pk.fuelgo.customer.data.remote.dto.AddAddressRequest
import pk.fuelgo.customer.data.remote.dto.ApiMessageDto
import pk.fuelgo.customer.data.remote.dto.AuthResponseDto
import pk.fuelgo.customer.data.remote.dto.CancelOrderRequest
import pk.fuelgo.customer.data.remote.dto.ChangePasswordRequest
import pk.fuelgo.customer.data.remote.dto.CustomerAddressDto
import pk.fuelgo.customer.data.remote.dto.CustomerProfileDto
import pk.fuelgo.customer.data.remote.dto.FuelTypeDto
import pk.fuelgo.customer.data.remote.dto.LoginRequest
import pk.fuelgo.customer.data.remote.dto.OrderDto
import pk.fuelgo.customer.data.remote.dto.PlaceOrderRequest
import pk.fuelgo.customer.data.remote.dto.RateOrderRequest
import pk.fuelgo.customer.data.remote.dto.RefreshTokenRequest
import pk.fuelgo.customer.data.remote.dto.RegisterCustomerRequest
import pk.fuelgo.customer.data.remote.dto.RegisterCustomerResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

// Endpoint surface used by the customer app, matching the controllers under
// Backend/FuelGo.Api/Controllers. The tenant "slug" header and Authorization
// bearer token are attached by NetworkModule's interceptors, so none of these
// calls need to pass them explicitly.
interface ApiService {

    // --- Auth ---------------------------------------------------------
    @POST("Auth/login")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponseDto>

    @POST("Auth/refresh-token")
    suspend fun refreshToken(@Body body: RefreshTokenRequest): Response<AuthResponseDto>

    @POST("Auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): Response<ApiMessageDto>

    // --- Customers ------------------------------------------------------
    @POST("customers/register")
    suspend fun registerCustomer(@Body body: RegisterCustomerRequest): Response<RegisterCustomerResponseDto>

    @GET("customers/me")
    suspend fun getMyProfile(): Response<CustomerProfileDto>

    @GET("customers/me/addresses")
    suspend fun getMyAddresses(): Response<List<CustomerAddressDto>>

    @POST("customers/me/addresses")
    suspend fun addMyAddress(@Body body: AddAddressRequest): Response<CustomerAddressDto>

    // --- Orders -----------------------------------------------------------
    @GET("orders/fuel-types")
    suspend fun getFuelTypes(): Response<List<FuelTypeDto>>

    @POST("orders/place")
    suspend fun placeOrder(@Body body: PlaceOrderRequest): Response<OrderDto>

    @GET("orders/my")
    suspend fun getMyOrders(): Response<List<OrderDto>>

    @GET("orders/{id}")
    suspend fun getOrder(@Path("id") id: String): Response<OrderDto>

    @PATCH("orders/{id}/cancel")
    suspend fun cancelOrder(@Path("id") id: String, @Body body: CancelOrderRequest): Response<ApiMessageDto>

    @POST("orders/{id}/rate")
    suspend fun rateOrder(@Path("id") id: String, @Body body: RateOrderRequest): Response<ApiMessageDto>
}
