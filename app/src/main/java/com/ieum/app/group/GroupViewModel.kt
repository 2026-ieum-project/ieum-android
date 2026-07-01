package com.ieum.app.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieum.app.data.repository.GroupRepository
import com.ieum.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── CreateGroup ──

data class CreateGroupUiState(
    val inviteCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val isCreated: Boolean = false
)

class CreateGroupViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val groupRepo: GroupRepository = GroupRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateGroupUiState())
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    fun createGroup() {
        val uid = userRepo.currentUid ?: return
        val state = _uiState.value
        if (state.isLoading) return

        _uiState.value = state.copy(isLoading = true, errorMessage = "")

        viewModelScope.launch {
            groupRepo.createGroup(uid)
                .onSuccess { code ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        inviteCode = code,
                        isCreated = true
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "그룹 생성 실패"
                    )
                }
        }
    }
}

// ── JoinGroup ──

data class JoinGroupUiState(
    val inviteCode: String = "",
    val userRole: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val isJoined: Boolean = false
)

class JoinGroupViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val groupRepo: GroupRepository = GroupRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(JoinGroupUiState())
    val uiState: StateFlow<JoinGroupUiState> = _uiState.asStateFlow()

    init {
        loadUserRole()
    }

    private fun loadUserRole() {
        val uid = userRepo.currentUid ?: return
        viewModelScope.launch {
            userRepo.getUserRole(uid).onSuccess { role ->
                _uiState.value = _uiState.value.copy(userRole = role)
            }
        }
    }

    fun onInviteCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(inviteCode = code.uppercase().take(6))
    }

    fun joinGroup() {
        val uid = userRepo.currentUid ?: return
        val state = _uiState.value
        if (state.isLoading || state.userRole.isEmpty()) return

        _uiState.value = state.copy(isLoading = true, errorMessage = "")

        viewModelScope.launch {
            groupRepo.joinGroup(uid, state.userRole, state.inviteCode.trim())
                .onSuccess {
                    _uiState.value = _uiState.value.copy(isLoading = false, isJoined = true)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "참여 실패"
                    )
                }
        }
    }
}
