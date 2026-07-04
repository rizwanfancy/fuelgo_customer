package pk.fuelgo.customer.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pk.fuelgo.customer.data.remote.ApiResult
import pk.fuelgo.customer.data.remote.dto.CustomerProfileDto
import pk.fuelgo.customer.data.repository.AuthRepository
import pk.fuelgo.customer.data.repository.CustomerRepository

data class ProfileUiState(
    val isLoading: Boolean = true,
    val profile: CustomerProfileDto? = null,
    val errorMessage: String? = null,
    val loggedOut: Boolean = false,
    val showChangePassword: Boolean = false,
    val currentPassword: String = "",
    val newPassword: String = "",
    val isChangingPassword: Boolean = false,
    val changePasswordMessage: String? = null,
)

class ProfileViewModel(
    private val customerRepository: CustomerRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = customerRepository.getProfile()) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, profile = result.data) }
                is ApiResult.Error -> _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { it.copy(loggedOut = true) }
        }
    }

    fun openChangePassword() =
        _uiState.update { it.copy(showChangePassword = true, currentPassword = "", newPassword = "", changePasswordMessage = null) }

    fun closeChangePassword() = _uiState.update { it.copy(showChangePassword = false) }
    fun setCurrentPassword(value: String) = _uiState.update { it.copy(currentPassword = value) }
    fun setNewPassword(value: String) = _uiState.update { it.copy(newPassword = value) }

    fun submitChangePassword() {
        val s = _uiState.value
        if (s.currentPassword.isBlank() || s.newPassword.length < 8) {
            _uiState.update { it.copy(changePasswordMessage = "Enter your current password and a new password of at least 8 characters.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isChangingPassword = true, changePasswordMessage = null) }
            when (val result = authRepository.changePassword(s.currentPassword, s.newPassword)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isChangingPassword = false, showChangePassword = false, changePasswordMessage = null)
                }
                is ApiResult.Error -> _uiState.update { it.copy(isChangingPassword = false, changePasswordMessage = result.message) }
            }
        }
    }
}
