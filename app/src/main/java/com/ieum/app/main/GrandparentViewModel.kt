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

data class GrandparentUiState(
    val uid: String = "",
    val userName: String = "",
    val userGender: String = "female",
    val groupId: String? = null, // null=로딩, ""=미가입
    val inviteCode: String = "",
    val hasFamilyMembers: Boolean? = null, // null=로딩
    val childName: String? = null,
    val grandchildName: String? = null,
    val messageCount: Int = 0
)

class GrandparentViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val groupRepo: GroupRepository = GroupRepository(),
    private val messageRepo: MessageRepository = MessageRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GrandparentUiState())
    val uiState: StateFlow<GrandparentUiState> = _uiState.asStateFlow()

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
                    userGender = data.gender,
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
            // 초대코드
            groupRepo.getInviteCode(groupId).onSuccess { code ->
                _uiState.value = _uiState.value.copy(inviteCode = code)
            }

            // 가족 멤버
            groupRepo.getGroupMembers(groupId).onSuccess { members ->
                _uiState.value = _uiState.value.copy(
                    hasFamilyMembers = members.hasChild || members.hasGrandchild,
                    childName = members.childName,
                    grandchildName = members.grandchildName
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(hasFamilyMembers = false)
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
