package com.ieum.app.notification

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FcmTokenManager {

    /**
     * 현재 FCM 토큰을 가져와 users/{uid}/fcmToken에 저장한다.
     * 로그인 후 호출해야 한다.
     */
    suspend fun registerToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            FirebaseDatabase.getInstance().reference
                .child("users").child(uid).child("fcmToken")
                .setValue(token)
                .await()
        } catch (_: Exception) {
            // 토큰 저장 실패는 치명적이지 않으므로 무시
        }
    }

    /**
     * 로그아웃 시 토큰을 삭제하여 알림을 받지 않도록 한다.
     */
    suspend fun unregisterToken() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            FirebaseDatabase.getInstance().reference
                .child("users").child(uid).child("fcmToken")
                .removeValue()
                .await()
        } catch (_: Exception) {}
    }
}
