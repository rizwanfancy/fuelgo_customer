package pk.fuelgo.customer.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pk.fuelgo.customer.data.remote.ApiResult
import pk.fuelgo.customer.data.remote.dto.RegisterCustomerRequest
import pk.fuelgo.customer.data.repository.AuthRepository

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
)

data class SignupUiState(
    val accountType: String = "Individual", // "Individual" | "Business"
    val email: String = "",
    val password: String = "",
    val mobileNumber: String = "",
    // Individual-only
    val name: String = "",
    val city: String = "Karachi",
    val address: String = "",
    val cnic: String = "",
    // Business-only
    val businessName: String = "",
    val location: String = "",
    val concernPersonName: String = "",
    val ntnNumber: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

private val mobileRegex = Regex("^03\\d{9}$")
private val cnicRegex = Regex("^\\d{5}-\\d{7}-\\d{1}$")

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState: StateFlow<LoginUiState> = _loginState.asStateFlow()

    private val _signupState = MutableStateFlow(SignupUiState())
    val signupState: StateFlow<SignupUiState> = _signupState.asStateFlow()

    fun updateLogin(transform: (LoginUiState) -> LoginUiState) {
        _loginState.update { transform(it).copy(errorMessage = null) }
    }

    fun prefillLoginUsername(value: String) {
        _loginState.update { it.copy(username = value) }
    }

    fun login() {
        val state = _loginState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _loginState.update { it.copy(errorMessage = "Enter your email or mobile number, and your password.") }
            return
        }
        viewModelScope.launch {
            _loginState.update { it.copy(isSubmitting = true, errorMessage = null) }
            when (val result = authRepository.login(state.username.trim(), state.password)) {
                is ApiResult.Success -> _loginState.update { it.copy(isSubmitting = false, isLoggedIn = true) }
                is ApiResult.Error -> _loginState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
            }
        }
    }

    fun updateSignup(transform: (SignupUiState) -> SignupUiState) {
        _signupState.update { transform(it).copy(errorMessage = null) }
    }

    fun resetSignupSuccess() {
        _signupState.update { it.copy(successMessage = null) }
    }

    fun register() {
        val s = _signupState.value
        val validationError = validateSignup(s)
        if (validationError != null) {
            _signupState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _signupState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val isIndividual = s.accountType == "Individual"
            val request = RegisterCustomerRequest(
                accountType = s.accountType,
                email = s.email.trim(),
                password = s.password,
                name = if (isIndividual) s.name.trim() else null,
                city = if (isIndividual) s.city.trim() else null,
                address = if (isIndividual) s.address.trim() else null,
                mobileNumber = s.mobileNumber.trim(),
                fuelType = null,
                cnic = if (isIndividual) s.cnic.trim() else null,
                businessName = if (!isIndividual) s.businessName.trim() else null,
                location = if (!isIndividual) s.location.trim() else null,
                concernPersonName = if (!isIndividual) s.concernPersonName.trim() else null,
                ntnNumber = if (!isIndividual) s.ntnNumber.trim() else null,
                latitude = null,
                longitude = null,
            )
            when (val result = authRepository.register(request)) {
                is ApiResult.Success -> _signupState.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = "Account created! You can sign in now.",
                    )
                }
                is ApiResult.Error -> _signupState.update { it.copy(isSubmitting = false, errorMessage = result.message) }
            }
        }
    }

    private fun validateSignup(s: SignupUiState): String? {
        if (s.email.isBlank() || !s.email.contains("@")) return "Enter a valid email address."
        if (s.password.length < 8) return "Password must be at least 8 characters."
        if (!mobileRegex.matches(s.mobileNumber.trim())) {
            return "Enter a valid mobile number (11 digits, starting with 03), e.g. 03001234567."
        }
        return if (s.accountType == "Individual") {
            when {
                s.name.isBlank() || s.city.isBlank() || s.address.isBlank() ->
                    "Please complete your name, city, and address."
                !cnicRegex.matches(s.cnic.trim()) -> "Enter a valid CNIC in the format 12345-1234567-1."
                else -> null
            }
        } else {
            if (s.businessName.isBlank() || s.location.isBlank() || s.concernPersonName.isBlank() || s.ntnNumber.isBlank()) {
                "Please complete all business fields."
            } else {
                null
            }
        }
    }
}
