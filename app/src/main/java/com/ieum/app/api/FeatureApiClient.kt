package com.ieum.app.api

import com.ieum.app.BuildConfig
import com.ieum.app.keystroke.KeystrokeAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object FeatureApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val baseUrl get() = BuildConfig.IEUM_SERVER_URL.trimEnd('/')

    suspend fun sendKeystroke(userId: String, features: KeystrokeAnalyzer.Features): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val keystrokeJson = JSONObject().apply {
                    put("inter_key_interval", features.interKeyInterval)
                    put("word_pause_duration", features.wordPauseDuration)
                    put("correction_time", features.correctionTime)
                    put("chars_per_minute", features.charsPerMinute)
                }

                val body = JSONObject().apply {
                    put("user_id", userId)
                    put("date", today)
                    put("keystroke", keystrokeJson)
                }.toString()

                val request = Request.Builder()
                    .url("$baseUrl/api/features")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    Result.success(Unit)
                } else {
                    Result.failure(RuntimeException("서버 오류: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
