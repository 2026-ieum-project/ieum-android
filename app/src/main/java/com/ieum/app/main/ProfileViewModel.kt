package com.ieum.app.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieum.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userName: String = "",
    val userRole: String = "",
    val isLoggedOut: Boolean = false
)

class ProfileViewModel(
    private val userRepo: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val uid = userRepo.currentUid ?: return
        viewModelScope.launch {
            userRepo.getUserData(uid).onSuccess { data ->
                _uiState.value = _uiState.value.copy(
                    userName = data.name,
                    userRole = data.role
                )
            }
        }
    }

    fun logout() {
        userRepo.logout()
        _uiState.value = _uiState.value.copy(isLoggedOut = true)
    }
}
