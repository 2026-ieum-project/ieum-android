package com.ieum.app.storage

import com.ieum.app.BuildConfig

object OracleObjectStorageConfig {
    val NAMESPACE: String = BuildConfig.ORACLE_NAMESPACE
    val BUCKET_NAME: String = BuildConfig.ORACLE_BUCKET_NAME
    val REGION: String = BuildConfig.ORACLE_REGION

    // 읽기 전용 PAR (목록 조회 비활성 필수). 업로드는 Cloud Function이 발급하는 단기 쓰기 PAR 사용.
    val READ_PAR_URL: String = BuildConfig.ORACLE_READ_PAR_URL

    fun getObjectUrl(objectName: String): String {
        return "${READ_PAR_URL.trimEnd('/')}/$objectName"
    }

    /**
     * DB에 저장된 미디어 URL을 현재 읽기 PAR 기준 URL로 재조립한다.
     * 메시지에는 전송 시점의 PAR가 포함된 전체 URL이 저장되므로,
     * PAR를 교체하면 저장된 URL 그대로는 만료되어 읽을 수 없다.
     */
    fun resolveReadUrl(storedUrl: String): String {
        val marker = "/b/$BUCKET_NAME/o/"
        val idx = storedUrl.indexOf(marker)
        return when {
            idx >= 0 -> getObjectUrl(storedUrl.substring(idx + marker.length))
            // PAR 미설정 상태로 전송되어 경로만 저장된 메시지
            !storedUrl.startsWith("http") -> getObjectUrl(storedUrl.trimStart('/'))
            else -> storedUrl
        }
    }
}
