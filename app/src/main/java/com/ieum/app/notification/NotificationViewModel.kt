package com.ieum.app.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieum.app.chat.Message
import com.ieum.app.data.repository.MessageRepository
import com.ieum.app.data.repository.UserRepository
import com.ieum.app.diary.DiaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class NotificationItem(
    val id: String,
    val kind: String, // Message.TYPE_* 또는 KIND_DIARY
    val senderName: String,
    val preview: String,
    val timestamp: Long,
    val unread: Boolean
)

data class NotificationUiState(
    val loading: Boolean = true,
    val items: List<NotificationItem> = emptyList()
)

class NotificationViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val messageRepo: MessageRepository = MessageRepository(),
    private val diaryRepo: DiaryRepository = DiaryRepository()
) : ViewModel() {

    companion object {
        const val KIND_DIARY = "diary"
    }

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val uid = userRepo.currentUid ?: run {
            _uiState.value = NotificationUiState(loading = false)
            return
        }
        viewModelScope.launch {
            val groupId = userRepo.getUserData(uid).getOrNull()?.groupId
            if (groupId.isNullOrEmpty()) {
                _uiState.value = NotificationUiState(loading = false)
                return@launch
            }

            val lastRead = messageRepo.getLastRead(groupId, uid)

            // 다른 가족이 보낸 최근 메시지 + 영상일기를 합쳐 최신순으로 보여준다
            val messages = messageRepo.loadInitialMessages(groupId)
                .getOrDefault(emptyList())
                .filter { it.senderId != uid }
                .map {
                    NotificationItem(
                        id = it.id,
                        kind = it.type,
                        senderName = it.senderName,
                        preview = previewOf(it),
                        timestamp = it.timestamp,
                        unread = it.timestamp > lastRead
                    )
                }

            val diaries = runCatching { diaryRepo.observeDiaries(groupId).first() }
                .getOrDefault(emptyList())
                .filter { it.senderId != uid }
                .map {
                    NotificationItem(
                        id = it.id,
                        kind = KIND_DIARY,
                        senderName = it.senderName,
                        preview = "영상일기를 남겼어요",
                        timestamp = it.timestamp,
                        unread = false
                    )
                }

            _uiState.value = NotificationUiState(
                loading = false,
                items = (messages + diaries).sortedByDescending { it.timestamp }
            )
        }
    }

    private fun previewOf(message: Message): String = when (message.type) {
        Message.TYPE_TEXT -> message.content
        Message.TYPE_VOICE -> "음성 메시지를 보냈어요"
        Message.TYPE_IMAGE -> "사진을 보냈어요"
        Message.TYPE_VIDEO -> "영상 메시지를 보냈어요"
        else -> "새 메시지"
    }
}
