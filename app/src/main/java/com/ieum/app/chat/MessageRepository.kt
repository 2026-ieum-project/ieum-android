package com.ieum.app.chat

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ieum.app.storage.OracleStorageUploader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MessageRepository(private val groupId: String) {

    private val messagesRef = FirebaseDatabase.getInstance().reference
        .child("messages")
        .child(groupId)

    private val uploader = OracleStorageUploader()

    fun sendTextMessage(
        senderId: String,
        senderName: String,
        content: String,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        val ref = messagesRef.push()
        val message = Message(
            id = ref.key ?: return,
            senderId = senderId,
            senderName = senderName,
            type = Message.TYPE_TEXT,
            content = content,
            timestamp = System.currentTimeMillis()
        )
        ref.setValue(message)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { e -> onResult(false, e.message) }
    }

    fun sendVoiceMessage(
        senderId: String,
        senderName: String,
        audioBytes: ByteArray,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        val ref = messagesRef.push()
        val messageId = ref.key ?: return
        val objectName = "voices/$groupId/$messageId.m4a"

        CoroutineScope(Dispatchers.IO).launch {
            val result = uploader.upload(
                data = audioBytes,
                objectName = objectName,
                contentType = "audio/mp4"
            )

            result.fold(
                onSuccess = { url ->
                    val message = Message(
                        id = messageId,
                        senderId = senderId,
                        senderName = senderName,
                        type = Message.TYPE_VOICE,
                        content = url,
                        timestamp = System.currentTimeMillis()
                    )
                    ref.setValue(message)
                        .addOnSuccessListener { onResult(true, null) }
                        .addOnFailureListener { e -> onResult(false, e.message) }
                },
                onFailure = { e ->
                    onResult(false, e.message)
                }
            )
        }
    }

    fun listenMessages(onMessages: (List<Message>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { child ->
                    child.getValue(Message::class.java)?.copy(id = child.key ?: "")
                }
                onMessages(messages)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        messagesRef.orderByChild("timestamp").addValueEventListener(listener)
        return listener
    }

    fun removeListener(listener: ValueEventListener) {
        messagesRef.removeEventListener(listener)
    }
}
