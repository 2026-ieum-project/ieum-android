package com.ieum.app.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OracleStorageUploader {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Oracle Object Storage에 파일 업로드 (PAR 방식)
     *
     * @param data 업로드할 바이트 배열
     * @param objectName 저장할 오브젝트 이름 (e.g. "voices/groupId/messageId.m4a")
     * @param contentType MIME 타입 (e.g. "audio/mp4")
     * @return 업로드된 오브젝트의 공개 URL
     */
    suspend fun upload(
        data: ByteArray,
        objectName: String,
        contentType: String = "audio/mp4"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val parUrl = OracleObjectStorageConfig.PAR_URL
            require(parUrl.isNotBlank()) { "Oracle PAR URL이 설정되지 않았습니다." }

            val uploadUrl = "${parUrl.trimEnd('/')}/$objectName"

            val requestBody = data.toRequestBody(contentType.toMediaType())
            val request = Request.Builder()
                .url(uploadUrl)
                .put(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val publicUrl = OracleObjectStorageConfig.getObjectUrl(objectName)
                Result.success(publicUrl)
            } else {
                Result.failure(
                    RuntimeException("업로드 실패: ${response.code} ${response.message}")
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
