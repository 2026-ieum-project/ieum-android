package com.ieum.app.storage

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OracleStorageUploader(
    private val functions: FirebaseFunctions =
        FirebaseFunctions.getInstance("asia-northeast3")
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Oracle Object Storage에 파일 업로드
     *
     * 쓰기 권한 PAR을 앱에 내장하지 않고, Cloud Function(createUploadUrl)에서
     * 해당 오브젝트 전용 단기 쓰기 PAR을 발급받아 업로드한다.
     *
     * @param data 업로드할 바이트 배열
     * @param objectName 저장할 오브젝트 이름 (e.g. "voices/groupId/messageId.m4a")
     * @param contentType MIME 타입 (e.g. "audio/mp4")
     * @return 업로드된 오브젝트의 읽기용 URL
     */
    suspend fun upload(
        data: ByteArray,
        objectName: String,
        contentType: String = "audio/mp4"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uploadUrl = requestUploadUrl(objectName)

            val requestBody = data.toRequestBody(contentType.toMediaType())
            val request = Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(OracleObjectStorageConfig.getObjectUrl(objectName))
                } else {
                    Result.failure(
                        RuntimeException("업로드 실패: ${response.code} ${response.message}")
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cloud Function에서 단일 오브젝트 전용 단기 쓰기 PAR URL을 발급받는다.
     */
    private suspend fun requestUploadUrl(objectName: String): String {
        val result = functions.getHttpsCallable("createUploadUrl")
            .call(mapOf("objectName" to objectName))
            .await()

        val uploadUrl = (result.data as? Map<*, *>)?.get("uploadUrl") as? String
        require(!uploadUrl.isNullOrBlank()) { "업로드 URL 발급에 실패했습니다." }
        return uploadUrl
    }
}
