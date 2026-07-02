package com.ieum.app.diary

import androidx.annotation.Keep

// Firebase RTDB가 리플렉션으로 역직렬화하므로 R8 난독화에서 제외
@Keep
data class Diary(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val videoUrl: String = "",
    val timestamp: Long = 0L
)
