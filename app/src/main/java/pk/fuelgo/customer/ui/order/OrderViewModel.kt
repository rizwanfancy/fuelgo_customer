package pk.fuelgo.customer.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pk.fuelgo.customer.config.AppConfig
import pk.fuelgo.customer.data.remote.ApiResult
import pk.fuelgo.customer.data.remote.dto.CustomerAddressDto
import pk.fuelgo.customer.data.remote.dto.FuelTypeDto
import pk.fuelgo.customer.data.repository.CustomerRepository
import pk.fuelgo.customer.data.repository.OrderRepository

val QUICK_QUANTITIES = listOf(20, 45, 100, 250, 500, 1000)

val PAYMENT_METHODS = listOf(
    "COD" to "Cash on Delivery",
    "JazzCash" to "JazzCash",
    "Easypaisa" to "Easypaisa",
    "BankTransfer" to "Bank Transfer",
    "CorporateCredit" to "Corporate Credit",
)

data class OrderUiState(
    val isLoading: Boolean = true,
    val fuelTypes: List<FuelTypeDto> = emptyList(),
    val addresses: List<CustomerAddressDto> = emptyList(),
    val selectedFuelTypeId: String? = null,
    val quantityText: String = "45",
    val paymentMethod: String = "COD",
    val selectedAddressId: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showAddAddressDialog: Boolean = false,
    val isAddingAddress: Boolean = false,
    val addAddressError: String? = null,
) {
    val quantityLitres: Double get() = quantityText.toDoubleOrNull() ?: 0.0
    val selectedFuel: FuelTypeDto? get() = fuelTypes.find { it.id == selectedFuelTypeId }
    val fuelSubtotal: Double get() = (selectedFuel?.pricePerLitre ?: 0.0) * quantityLitres
    val total: Double get() = fuelSubtotal + AppConfig.DELIVERY_FEE_PKR
    val canSubmit: Boolean
        get() = !isSubmitting && selectedFuelTypeId != null && selectedAddressId != null && quantityLitres >= 5
}

class OrderViewModel(
    private val orderRepository: OrderRepository,
    private val customerRepository: CustomerRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderUiState())
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val fuelResult = orderRepository.getFuelTypes()
            val addressResult = customerRepository.getAddresses()

            val fuelTypes = (fuelResult as? ApiResult.Success)?.data ?: emptyList()
            val addresses = (addressResult as? ApiResult.Success)?.data?.filter { !it.isDeleted } ?: emptyList()

            val error = when {
                fuelResult is ApiResult.Error -> fuelResult.message
                addressResult is ApiResult.Error -> addressResult.message
                else -> null
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    fuelTypes = fuelTypes,
                    addresses = addresses,
                    selectedFuelTypeId = it.selectedFuelTypeId ?: fuelTypes.firstOrNull()?.id,
                    selectedAddressId = it.selectedAddressId
                        ?: addresses.find { addr -> addr.isDefault }?.id
                        ?: addresses.firstOrNull()?.id,
                    errorMessage = error,
                )
            }
        }
    }

    fun selectFuel(id: String) {
        _uiState.update { it.copy(selectedFuelTypeId = id, successMessage = null) }
    }

    fun setQuantityText(text: String) {
        if (text.length <= 5 && text.all { it.isDigit() || it == '.' }) {
            _uiState.update { it.copy(quantityText = text, successMessage = null) }
        }
    }

    fun setQuickQuantity(qty: Int) {
        _uiState.update { it.copy(quantityText = qty.toString(), successMessage = null) }
    }

    fun setPaymentMethod(method: String) {
        _uiState.update { it.copy(paymentMethod = method, successMessage = null) }
    }

    fun selectAddress(id: String) {
        _uiState.update { it.copy(selectedAddressId = id, successMessage = null) }
    }

    fun openAddAddressDialog() = _uiState.update { it.copy(showAddAddressDialog = true, addAddressError = null) }
    fun closeAddAddressDialog() = _uiState.update { it.copy(showAddAddressDialog = false) }

    fun addAddress(label: String, addressLine: String, area: String, city: String) {
        if (addressLine.isBlank() || city.isBlank()) {
            _uiState.update { it.copy(addAddressError = "Address line and city are required.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAddingAddress = true, addAddressError = null) }
            val isFirst = _uiState.value.addresses.isEmpty()
            when (
                val result = customerRepository.addAddress(
                    label = label.ifBlank { "Home" },
                    addressLine = addressLine,
                    area = area.ifBlank { city },
                    city = city,
                    isDefault = isFirst,
                )
            ) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isAddingAddress = false,
                            showAddAddressDialog = false,
                            addresses = it.addresses + result.data,
                            selectedAddressId = result.data.id,
                        )
                    }
                }
                is ApiResult.Error -> _uiState.update { it.copy(isAddingAddress = false, addAddressError = result.message) }
            }
        }
    }

    fun submitOrder() {
        val s = _uiState.value
        val fuelId = s.selectedFuelTypeId
        val addressId = s.selectedAddressId
        if (fuelId == null) {
            _uiState.update { it.copy(errorMessage = "Please select a fuel type.") }
            return
        }
        if (addressId == null) {
            _uiState.update { it.copy(errorMessage = "Please select or add a delivery address.") }
            return
        }
        if (s.quantityLitres < 5) {
            _uiState.update { it.copy(errorMessage = "Minimum order quantity is 5 litres.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }
            when (
                val result = orderRepository.placeOrder(
                    fuelTypeId = fuelId,
                    quantityLitres = s.quantityLitres,
                    deliveryAddressId = addressId,
                    paymentMethod = s.paymentMethod,
                )
            ) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = "Order placed! Order #${result.data.orderNumber}. Track it from the Tracking tab.",
                    )
                }
                is ApiResult.Error -> _uiState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
            }
        }
    }

    fun consumeSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }
}
