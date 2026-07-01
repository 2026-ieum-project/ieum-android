package com.ieum.app.diary

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.ieum.app.storage.OracleStorageUploader
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DiaryRepository(
    private val db: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val uploader: OracleStorageUploader = OracleStorageUploader()
) {

    /**
     * 영상 일기를 업로드하고 DB에 저장한다.
     */
    suspend fun uploadDiary(
        groupId: String,
        senderId: String,
        senderName: String,
        videoBytes: ByteArray,
        mimeType: String = "video/mp4"
    ): Result<Unit> = try {
        val ref = db.reference.child("diaries").child(groupId).push()
        val diaryId = ref.key ?: throw IllegalStateException("일기 ID 생성 실패")

        val extension = when (mimeType) {
            "video/webm" -> "webm"
            "video/3gpp" -> "3gp"
            else -> "mp4"
        }
        val objectName = "diaries/$groupId/$diaryId.$extension"

        val url = uploader.upload(
            data = videoBytes,
            objectName = objectName,
            contentType = mimeType
        ).getOrThrow()

        val diary = Diary(
            id = diaryId,
            senderId = senderId,
            senderName = senderName,
            videoUrl = url,
            timestamp = System.currentTimeMillis()
        )
        ref.setValue(diary).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * 그룹의 영상 일기 목록을 실시간 감지한다 (최신순).
     */
    fun observeDiaries(groupId: String): Flow<List<Diary>> = callbackFlow {
        val ref = db.reference.child("diaries").child(groupId)
            .orderByChild("timestamp")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val diaries = snapshot.children.mapNotNull { child ->
                    child.getValue(Diary::class.java)?.copy(id = child.key ?: "")
                }.reversed() // 최신순
                trySend(diaries)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}
