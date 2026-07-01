package com.ieum.app.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieum.app.data.repository.GroupRepository
import com.ieum.app.data.repository.MessageRepository
import com.ieum.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChildUiState(
    val uid: String = "",
    val userName: String = "",
    val groupId: String? = null,
    val grandparentName: String? = null,
    val hasGrandparent: Boolean? = null,
    val inviteCode: String = "",
    val messageCount: Int = 0
)

class ChildViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val groupRepo: GroupRepository = GroupRepository(),
    private val messageRepo: MessageRepository = MessageRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildUiState())
    val uiState: StateFlow<ChildUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val uid = userRepo.currentUid ?: return
        _uiState.value = _uiState.value.copy(uid = uid)

        viewModelScope.launch {
            userRepo.getUserData(uid).onSuccess { data ->
                _uiState.value = _uiState.value.copy(
                    userName = data.name,
                    groupId = data.groupId
                )
                if (data.groupId.isNotEmpty()) {
                    loadGroupData(data.groupId)
                    observeUnreadCount(data.groupId, uid)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(groupId = "")
            }
        }
    }

    private fun loadGroupData(groupId: String) {
        viewModelScope.launch {
            groupRepo.getInviteCode(groupId).onSuccess { code ->
                _uiState.value = _uiState.value.copy(inviteCode = code)
            }

            groupRepo.getGroupMembers(groupId).onSuccess { members ->
                _uiState.value = _uiState.value.copy(
                    hasGrandparent = members.hasGrandparent,
                    grandparentName = members.grandparentName
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(hasGrandparent = false)
            }
        }
    }

    private fun observeUnreadCount(groupId: String, uid: String) {
        viewModelScope.launch {
            messageRepo.observeUnreadCount(groupId, uid).collect { count ->
                _uiState.value = _uiState.value.copy(messageCount = count)
            }
        }
    }
}
