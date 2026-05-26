package com.sdk.translation.errors

enum class TranslationErrorCode {
    ApiHttpError,
    ApiInvalidKey,
    ApiQuotaExceeded,
    NetworkError,
    SerializationError,
    CacheError,
    SdkClosed,
    Unknown
}

data class TranslationErrorReport(
    val code: TranslationErrorCode,
    val message: String,
    val sdkVersion: String,
    val provider: String = "google",
    val httpStatus: Int? = null,
    val targetLanguage: String? = null
)

open class TranslationSdkException(
    val code: TranslationErrorCode,
    val report: TranslationErrorReport,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

class TranslationApiException(
    val statusCode: Int,
    report: TranslationErrorReport,
    message: String,
    cause: Throwable? = null
) : TranslationSdkException(report.code, report, message, cause)

class TranslationCacheException(
    report: TranslationErrorReport,
    message: String,
    cause: Throwable? = null
) : TranslationSdkException(TranslationErrorCode.CacheError, report, message, cause)

class TranslationSdkClosedException(
    report: TranslationErrorReport
) : TranslationSdkException(TranslationErrorCode.SdkClosed, report, report.message)

interface TranslationErrorReporter {
    fun report(error: TranslationSdkException)
}

internal object NoOpTranslationErrorReporter : TranslationErrorReporter {
    override fun report(error: TranslationSdkException) = Unit
}
