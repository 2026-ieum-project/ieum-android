package com.ieum.app.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ieum.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DiaryUiState(
    val uid: String = "",
    val userName: String = "",
    val groupId: String = "",
    val diaries: List<Diary> = emptyList(),
    val isLoading: Boolean = true,
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean = false,
    val errorMessage: String = ""
)

class DiaryViewModel(
    private val userRepo: UserRepository = UserRepository(),
    private val diaryRepo: DiaryRepository = DiaryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiaryUiState())
    val uiState: StateFlow<DiaryUiState> = _uiState.asStateFlow()

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
                    observeDiaries(data.groupId)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            }.onFailure {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private fun observeDiaries(groupId: String) {
        viewModelScope.launch {
            diaryRepo.observeDiaries(groupId).collect { diaries ->
                _uiState.value = _uiState.value.copy(
                    diaries = diaries,
                    isLoading = false
                )
            }
        }
    }

    fun uploadVideo(videoBytes: ByteArray, mimeType: String = "video/mp4") {
        val state = _uiState.value
        if (state.groupId.isEmpty() || state.isUploading) return

        _uiState.value = state.copy(isUploading = true, uploadSuccess = false, errorMessage = "")

        viewModelScope.launch {
            diaryRepo.uploadDiary(
                groupId = state.groupId,
                senderId = state.uid,
                senderName = state.userName,
                videoBytes = videoBytes,
                mimeType = mimeType
            ).onSuccess {
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    uploadSuccess = true
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(
                    isUploading = false,
                    errorMessage = e.message ?: "업로드 실패"
                )
            }
        }
    }

    fun clearUploadSuccess() {
        _uiState.value = _uiState.value.copy(uploadSuccess = false)
    }
}
