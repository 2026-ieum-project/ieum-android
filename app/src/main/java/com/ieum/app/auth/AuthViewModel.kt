package com.ieum.app.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieum.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Splash ──

sealed interface SplashDestination {
    data object Loading : SplashDestination
    data object Login : SplashDestination
    data class RoleMain(val role: String) : SplashDestination
}

class SplashViewModel(
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        checkAuth()
    }

    private fun checkAuth() {
        val uid = userRepo.currentUid
        if (uid == null) {
            _destination.value = SplashDestination.Login
        } else {
            viewModelScope.launch {
                val role = userRepo.getUserRole(uid).getOrDefault("")
                if (role.isEmpty()) {
                    _destination.value = SplashDestination.Login
                } else {
                    _destination.value = SplashDestination.RoleMain(role)
                }
            }
        }
    }
}

// ── Login ──

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val errorMessage: String = "",
    val isLoading: Boolean = false,
    val navigateToRole: String? = null
)

class LoginViewModel(
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun onPasswordChange(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun login() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.value = state.copy(isLoading = true, errorMessage = "")

        viewModelScope.launch {
            userRepo.login(state.email.trim(), state.password)
                .onSuccess { uid ->
                    val role = userRepo.getUserRole(uid).getOrDefault("")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        navigateToRole = role.ifEmpty { "login" }
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "로그인 실패"
                    )
                }
        }
    }

    fun onNavigated() {
        _uiState.value = _uiState.value.copy(navigateToRole = null)
    }
}

// ── Register ──

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val selectedRole: String = "grandparent",
    val selectedGender: String = "female",
    val errorMessage: String = "",
    val isLoading: Boolean = false,
    val navigateToRole: String? = null
)

class RegisterViewModel(
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun onNameChange(name: String) { _uiState.value = _uiState.value.copy(name = name) }
    fun onEmailChange(email: String) { _uiState.value = _uiState.value.copy(email = email) }
    fun onPasswordChange(pw: String) { _uiState.value = _uiState.value.copy(password = pw) }
    fun onRoleChange(role: String) { _uiState.value = _uiState.value.copy(selectedRole = role) }
    fun onGenderChange(gender: String) { _uiState.value = _uiState.value.copy(selectedGender = gender) }

    fun register() {
        val state = _uiState.value
        if (state.isLoading) return
        _uiState.value = state.copy(isLoading = true, errorMessage = "")

        viewModelScope.launch {
            userRepo.register(
                name = state.name,
                email = state.email.trim(),
                password = state.password,
                role = state.selectedRole,
                gender = state.selectedGender
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    navigateToRole = state.selectedRole
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "회원가입 실패"
                )
            }
        }
    }

    fun onNavigated() {
        _uiState.value = _uiState.value.copy(navigateToRole = null)
    }
}

// ── ForgotPassword ──

data class ForgotPasswordUiState(
    val email: String = "",
    val message: String = "",
    val isError: Boolean = false,
    val isSent: Boolean = false,
    val isLoading: Boolean = false
)

class ForgotPasswordViewModel(
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email)
    }

    fun sendReset() {
        val state = _uiState.value
        if (state.isLoading || state.isSent) return
        _uiState.value = state.copy(isLoading = true, message = "")

        viewModelScope.launch {
            userRepo.sendPasswordReset(state.email)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = "재설정 링크를 이메일로 보냈습니다.",
                        isError = false,
                        isSent = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        message = e.message ?: "이메일 전송 실패",
                        isError = true
                    )
                }
        }
    }
}
