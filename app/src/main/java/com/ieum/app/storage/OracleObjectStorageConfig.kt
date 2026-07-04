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
}
