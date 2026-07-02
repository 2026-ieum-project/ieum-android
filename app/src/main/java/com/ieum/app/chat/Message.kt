package com.ieum.app.chat

import androidx.annotation.Keep

// Firebase RTDB가 리플렉션으로 역직렬화하므로 R8 난독화에서 제외
@Keep
data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val type: String = TYPE_TEXT,
    val content: String = "",
    val timestamp: Long = 0L
) {
    companion object {
        const val TYPE_TEXT = "text"
        const val TYPE_VOICE = "voice"
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"
    }
}
