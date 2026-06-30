package com.ieum.app.storage

import com.ieum.app.BuildConfig

object OracleObjectStorageConfig {
    val NAMESPACE: String = BuildConfig.ORACLE_NAMESPACE
    val BUCKET_NAME: String = BuildConfig.ORACLE_BUCKET_NAME
    val REGION: String = BuildConfig.ORACLE_REGION
    val PAR_URL: String = BuildConfig.ORACLE_PAR_URL

    fun getObjectUrl(objectName: String): String {
        return "${PAR_URL.trimEnd('/')}/$objectName"
    }
}
