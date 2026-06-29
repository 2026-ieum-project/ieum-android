package com.ieum.app.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieum.app.data.repository.MessageRepository
import com.ieum.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val groupId: String = "",
    val userName: String = "",
    val uid: String = "",
    val messages: List<Message> = emptyList(),
    val textInput: String = "",
    val isLoading: Boolean = true,
    val isLoadingOlder: Boolean = false,
    val hasOlderMessages: Boolean = true,
    val isSending: Boolean = false
)

class ChatViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val messageRepo: MessageRepository = MessageRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // 중복 메시지 방지용 ID 셋
    private val messageIds = mutableSetOf<String>()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        val uid = userRepo.currentUid ?: return
        _uiState.value = _uiState.value.copy(uid = uid)

        viewModelScope.launch {
            userRepo.getUserData(uid).onSuccess { data ->
                _uiState.value = _uiState.value.copy(
                    groupId = data.groupId,
                    userName = data.name
                )
                if (data.groupId.isNotEmpty()) {
                    loadInitialMessages(data.groupId)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun loadInitialMessages(groupId: String) {
        viewModelScope.launch {
            messageRepo.loadInitialMessages(groupId).onSuccess { messages ->
                messageIds.addAll(messages.map { it.id })
                _uiState.value = _uiState.value.copy(
                    messages = messages,
                    isLoading = false,
                    hasOlderMessages = messages.size >= MessageRepository.PAGE_SIZE
                )
                // 최신 메시지의 timestamp 이후로 실시간 리스너 시작
                val sinceTimestamp = messages.lastOrNull()?.timestamp ?: 0L
                observeNewMessages(groupId, sinceTimestamp)
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun observeNewMessages(groupId: String, sinceTimestamp: Long) {
        viewModelScope.launch {
            messageRepo.observeNewMessages(groupId, sinceTimestamp).collect { message ->
                // 중복 방지: 이미 있는 메시지는 무시
                if (messageIds.add(message.id)) {
                    val current = _uiState.value.messages
                    _uiState.value = _uiState.value.copy(
                        messages = current + message
                    )
                }
            }
        }
    }

    /**
     * 위로 스크롤 시 이전 메시지 로드
     */
    fun loadOlderMessages() {
        val state = _uiState.value
        if (state.isLoadingOlder || !state.hasOlderMessages || state.groupId.isEmpty()) return

        val oldestTimestamp = state.messages.firstOrNull()?.timestamp ?: return
        _uiState.value = state.copy(isLoadingOlder = true)

        viewModelScope.launch {
            messageRepo.loadOlderMessages(state.groupId, oldestTimestamp)
                .onSuccess { olderMessages ->
                    val newMessages = olderMessages.filter { messageIds.add(it.id) }
                    _uiState.value = _uiState.value.copy(
                        messages = newMessages + _uiState.value.messages,
                        isLoadingOlder = false,
                        hasOlderMessages = olderMessages.size >= MessageRepository.PAGE_SIZE
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isLoadingOlder = false)
                }
        }
    }

    fun onTextInputChange(text: String) {
        _uiState.value = _uiState.value.copy(textInput = text)
    }

    fun sendTextMessage() {
        val state = _uiState.value
        if (state.textInput.isBlank() || state.groupId.isEmpty()) return

        val text = state.textInput.trim()
        _uiState.value = state.copy(textInput = "")

        viewModelScope.launch {
            messageRepo.sendTextMessage(
                groupId = state.groupId,
                senderId = state.uid,
                senderName = state.userName,
                content = text
            )
        }
    }

    fun sendImageMessage(imageBytes: ByteArray, mimeType: String = "image/jpeg") {
        val state = _uiState.value
        if (state.groupId.isEmpty()) return

        _uiState.value = state.copy(isSending = true)

        viewModelScope.launch {
            messageRepo.sendImageMessage(
                groupId = state.groupId,
                senderId = state.uid,
                senderName = state.userName,
                imageBytes = imageBytes,
                mimeType = mimeType
            )
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }

    fun sendVoiceMessage(audioBytes: ByteArray) {
        val state = _uiState.value
        if (state.groupId.isEmpty()) return

        _uiState.value = state.copy(isSending = true)

        viewModelScope.launch {
            messageRepo.sendVoiceMessage(
                groupId = state.groupId,
                senderId = state.uid,
                senderName = state.userName,
                audioBytes = audioBytes
            )
            _uiState.value = _uiState.value.copy(isSending = false)
        }
    }
}
