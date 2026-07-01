package com.ieum.app.chat

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
    }
}
