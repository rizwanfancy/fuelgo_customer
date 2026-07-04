package pk.fuelgo.customer.data.remote.dto

import kotlinx.serialization.Serializable

// Mirrors Backend/FuelGo.Application/Customers/CustomerDtos.cs (RegisterCustomerRequest record).
// All fields are sent every time; unused ones for the chosen account type are left null.
@Serializable
data class RegisterCustomerRequest(
    val accountType: String, // "Individual" | "Business"
    val email: String,
    val password: String,
    val name: String? = null,
    val city: String? = null,
    val address: String? = null,
    val mobileNumber: String,
    val fuelType: String? = null,
    val cnic: String? = null,
    val businessName: String? = null,
    val location: String? = null,
    val concernPersonName: String? = null,
    val ntnNumber: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class RegisterCustomerResponseDto(
    val customerId: String,
    val userId: String,
    val accountType: String,
    val mobileNumber: String,
    val isApproved: Boolean,
    val message: String,
)

@Serializable
data class CustomerAddressDto(
    val id: String,
    val label: String = "",
    val addressLine: String = "",
    val area: String = "",
    val city: String = "",
    val isDefault: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isDeleted: Boolean = false,
)

@Serializable
data class CustomerProfileDto(
    val customerId: String,
    val userId: String,
    val customerType: String,
    val email: String,
    val phoneNumber: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val companyName: String? = null,
    val ntn: String? = null,
    val isApproved: Boolean = false,
    val creditLimit: Double? = null,
    val outstandingBalance: Double? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val addresses: List<CustomerAddressDto> = emptyList(),
)

@Serializable
data class AddAddressRequest(
    val label: String,
    val addressLine: String,
    val area: String,
    val city: String,
    val isDefault: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
