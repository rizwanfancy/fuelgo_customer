package pk.fuelgo.customer.data.repository

import pk.fuelgo.customer.data.remote.ApiResult
import pk.fuelgo.customer.data.remote.ApiService
import pk.fuelgo.customer.data.remote.dto.AddAddressRequest
import pk.fuelgo.customer.data.remote.dto.CustomerAddressDto
import pk.fuelgo.customer.data.remote.dto.CustomerProfileDto
import pk.fuelgo.customer.data.remote.safeApiCall

class CustomerRepository(private val api: ApiService) {

    suspend fun getProfile(): ApiResult<CustomerProfileDto> =
        safeApiCall { api.getMyProfile() }

    suspend fun getAddresses(): ApiResult<List<CustomerAddressDto>> =
        safeApiCall { api.getMyAddresses() }

    suspend fun addAddress(
        label: String,
        addressLine: String,
        area: String,
        city: String,
        isDefault: Boolean,
        latitude: Double? = null,
        longitude: Double? = null,
    ): ApiResult<CustomerAddressDto> = safeApiCall {
        api.addMyAddress(
            AddAddressRequest(
                label = label,
                addressLine = addressLine,
                area = area,
                city = city,
                isDefault = isDefault,
                latitude = latitude,
                longitude = longitude,
            ),
        )
    }
}
