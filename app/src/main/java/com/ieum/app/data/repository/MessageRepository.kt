package com.ieum.app.data.repository

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ieum.app.chat.Message
import com.ieum.app.storage.OracleStorageUploader
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MessageRepository(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val uploader: OracleStorageUploader = OracleStorageUploader()
) {
    companion object {
        const val PAGE_SIZE = 20
    }

    /**
     * 최초 로드: 최신 메시지 [PAGE_SIZE]개를 가져온다.
     */
    suspend fun loadInitialMessages(groupId: String): Result<List<Message>> = try {
        val snapshot = db.reference.child("messages").child(groupId)
            .orderByChild("timestamp")
            .limitToLast(PAGE_SIZE)
            .get()
            .await()

        val messages = snapshot.children.mapNotNull { child ->
            child.getValue(Message::class.java)?.copy(id = child.key ?: "")
        }
        Result.success(messages)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 이전 메시지 로드: [cursorTimestamp] 이전의 메시지 [PAGE_SIZE]개를 가져온다.
     */
    suspend fun loadOlderMessages(
        groupId: String,
        cursorTimestamp: Long
    ): Result<List<Message>> = try {
        val snapshot = db.reference.child("messages").child(groupId)
            .orderByChild("timestamp")
            .endBefore(cursorTimestamp.toDouble())
            .limitToLast(PAGE_SIZE)
            .get()
            .await()

        val messages = snapshot.children.mapNotNull { child ->
            child.getValue(Message::class.java)?.copy(id = child.key ?: "")
        }
        Result.success(messages)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 새 메시지 실시간 감지: [sinceTimestamp] 이후에 추가되는 메시지만 수신한다.
     * ChildEventListener를 사용하여 개별 추가만 감지 (전체 리로드 방지).
     */
    fun observeNewMessages(groupId: String, sinceTimestamp: Long): Flow<Message> = callbackFlow {
        val ref = db.reference.child("messages").child(groupId)
            .orderByChild("timestamp")
            .startAfter(sinceTimestamp.toDouble())

        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                    ?.copy(id = snapshot.key ?: "") ?: return
                trySend(message)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addChildEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    fun observeMessageCount(groupId: String): Flow<Int> = callbackFlow {
        val ref = db.reference.child("messages").child(groupId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.childrenCount.toInt())
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun sendTextMessage(
        groupId: String,
        senderId: String,
        senderName: String,
        content: String
    ): Result<Unit> = try {
        val ref = db.reference.child("messages").child(groupId).push()
        val message = Message(
            id = ref.key ?: throw IllegalStateException("메시지 ID 생성 실패"),
            senderId = senderId,
            senderName = senderName,
            type = Message.TYPE_TEXT,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        ref.setValue(message).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendImageMessage(
        groupId: String,
        senderId: String,
        senderName: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): Result<Unit> = try {
        val ref = db.reference.child("messages").child(groupId).push()
        val messageId = ref.key ?: throw IllegalStateException("메시지 ID 생성 실패")
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val objectName = "images/$groupId/$messageId.$extension"

        val url = uploader.upload(
            data = imageBytes,
            objectName = objectName,
            contentType = mimeType
        ).getOrThrow()

        val message = Message(
            id = messageId,
            senderId = senderId,
            senderName = senderName,
            type = Message.TYPE_IMAGE,
            content = url,
            timestamp = System.currentTimeMillis()
        )
        ref.setValue(message).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun sendVoiceMessage(
        groupId: String,
        senderId: String,
        senderName: String,
        audioBytes: ByteArray
    ): Result<Unit> = try {
        val ref = db.reference.child("messages").child(groupId).push()
        val messageId = ref.key ?: throw IllegalStateException("메시지 ID 생성 실패")
        val objectName = "voices/$groupId/$messageId.m4a"

        val url = uploader.upload(
            data = audioBytes,
            objectName = objectName,
            contentType = "audio/mp4"
        ).getOrThrow()

        val message = Message(
            id = messageId,
            senderId = senderId,
            senderName = senderName,
            type = Message.TYPE_VOICE,
            content = url,
            timestamp = System.currentTimeMillis()
        )
        ref.setValue(message).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
