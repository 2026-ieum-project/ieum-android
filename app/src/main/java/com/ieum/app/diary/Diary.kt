package com.ieum.app.diary

data class Diary(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val videoUrl: String = "",
    val timestamp: Long = 0L
)
