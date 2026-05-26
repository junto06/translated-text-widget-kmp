package com.sdk.translation

import com.russhwolf.settings.Settings
import com.sdk.translation.api.GoogleTranslateApi
import com.sdk.translation.api.TranslationApi
import com.sdk.translation.cache.TranslationCache
import com.sdk.translation.errors.NoOpTranslationErrorReporter
import com.sdk.translation.errors.TranslationApiException
import com.sdk.translation.errors.TranslationCacheException
import com.sdk.translation.errors.TranslationErrorCode
import com.sdk.translation.errors.TranslationErrorReport
import com.sdk.translation.errors.TranslationErrorReporter
import com.sdk.translation.errors.TranslationSdkClosedException
import com.sdk.translation.errors.TranslationSdkException
import com.sdk.translation.models.TranslationResult
import io.ktor.utils.io.errors.IOException
import kotlinx.serialization.SerializationException

/**
 * Main Translation SDK class.
 * Manages translation requests with caching.
 *
 * IMPORTANT: Call close() when done with the SDK to release HTTP client resources.
 */
class TranslationSDK private constructor(
    private val api: TranslationApi,
    private val cache: TranslationCache,
    private var defaultTargetLanguage: String,
    private val errorReporter: TranslationErrorReporter
) : Closeable {
    private var isClosed = false

    class Builder {
        private var apiKey: String = ""
        private var defaultLanguage: String = "en"
        private var settings: Settings? = null
        private var cacheVersion: String = "default"
        private var cacheTtlMillis: Long? = null
        private var errorReporter: TranslationErrorReporter = NoOpTranslationErrorReporter
        private var translationApi: TranslationApi? = null

        fun apiKey(key: String) = apply { apiKey = key }
        fun translationApi(api: TranslationApi) = apply { translationApi = api }
        fun defaultLanguage(lang: String) = apply { defaultLanguage = lang }
        fun settings(s: Settings) = apply { settings = s }
        fun errorReporter(reporter: TranslationErrorReporter) = apply { errorReporter = reporter }
        fun cacheVersion(version: String) = apply { cacheVersion = version }
        fun cacheTtlMillis(ttlMillis: Long?) = apply {
            require(ttlMillis == null || ttlMillis >= 0) { "Cache TTL must be null or non-negative" }
            cacheTtlMillis = ttlMillis
        }

        fun build(): TranslationSDK {
            val currentApi = translationApi ?: run {
                require(apiKey.isNotBlank()) { "API key must not be blank" }
                GoogleTranslateApi(apiKey)
            }
            require(cacheVersion.isNotBlank()) { "Cache version must not be blank" }
            val currentSettings = settings ?: InMemorySettings()
            return TranslationSDK(
                api = currentApi,
                cache = TranslationCache(
                    settings = currentSettings,
                    cacheVersion = cacheVersion,
                    cacheTtlMillis = cacheTtlMillis
                ),
                defaultTargetLanguage = defaultLanguage,
                errorReporter = errorReporter
            )
        }
    }

    @Throws(Exception::class)
    suspend fun translate(
        text: String,
        targetLanguage: String = defaultTargetLanguage
    ): TranslationResult = translateBatch(listOf(text), targetLanguage).first()

    @Throws(Exception::class)
    suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: String = defaultTargetLanguage
    ): List<TranslationResult> {
        checkOpen()

        val results = MutableList<TranslationResult?>(texts.size) { null }
        val missingTexts = mutableListOf<String>()
        val missingIndexes = mutableListOf<Int>()

        texts.forEachIndexed { index, text ->
            val cached = cacheGet(text, targetLanguage)
            if (cached != null) {
                results[index] = TranslationResult(
                    originalText = text,
                    translatedText = cached,
                    detectedSourceLanguage = null,
                    targetLanguage = targetLanguage
                )
            } else {
                missingTexts += text
                missingIndexes += index
            }
        }

        if (missingTexts.isNotEmpty()) {
            val translatedItems = runCatching {
                api.translateBatch(missingTexts, targetLanguage)
            }.getOrElse { error ->
                throw reportAndWrap(error, targetLanguage)
            }

            translatedItems.forEachIndexed { missingTextIndex, response ->
                val originalText = missingTexts[missingTextIndex]
                cachePut(originalText, targetLanguage, response.translatedText)
                results[missingIndexes[missingTextIndex]] = TranslationResult(
                    originalText = originalText,
                    translatedText = response.translatedText,
                    detectedSourceLanguage = response.detectedSourceLanguage,
                    targetLanguage = targetLanguage
                )
            }
        }

        return results.map { requireNotNull(it) }
    }

    fun setDefaultLanguage(lang: String) {
        defaultTargetLanguage = lang
    }

    /**
     * Closes the HTTP client and releases resources.
     * Call this when the SDK is no longer needed (e.g., app shutdown, view model cleanup).
     */
    override fun close() {
        if (isClosed) {
            return
        }
        isClosed = true
        api.close()
    }

    private fun checkOpen() {
        if (isClosed) {
            val error = TranslationSdkClosedException(
                report = errorReport(
                    code = TranslationErrorCode.SdkClosed,
                    message = "TranslationSDK is closed"
                )
            )
            errorReporter.report(error)
            throw error
        }
    }

    private fun cacheGet(text: String, targetLanguage: String): String? =
        runCatching {
            cache.get(text, targetLanguage)
        }.getOrElse { error ->
            reportCacheError("Translation cache read failed", targetLanguage, error)
            null
        }

    private fun cachePut(text: String, targetLanguage: String, translated: String) {
        runCatching {
            cache.put(text, targetLanguage, translated)
        }.onFailure { error ->
            reportCacheError("Translation cache write failed", targetLanguage, error)
        }
    }

    private fun reportCacheError(message: String, targetLanguage: String, cause: Throwable) {
        errorReporter.report(
            TranslationCacheException(
                report = errorReport(
                    code = TranslationErrorCode.CacheError,
                    message = message,
                    targetLanguage = targetLanguage
                ),
                message = message,
                cause = cause
            )
        )
    }

    private fun reportAndWrap(error: Throwable, targetLanguage: String): TranslationSdkException {
        val sdkException = when (error) {
            is TranslationApiException -> error
            is TranslationSdkException -> error
            is IOException -> sdkException(
                code = TranslationErrorCode.NetworkError,
                message = "Translation network request failed",
                targetLanguage = targetLanguage,
                cause = error
            )
            is SerializationException -> sdkException(
                code = TranslationErrorCode.SerializationError,
                message = "Translation API response could not be parsed",
                targetLanguage = targetLanguage,
                cause = error
            )
            else -> sdkException(
                code = TranslationErrorCode.Unknown,
                message = "Translation request failed",
                targetLanguage = targetLanguage,
                cause = error
            )
        }

        errorReporter.report(sdkException)
        return sdkException
    }

    private fun sdkException(
        code: TranslationErrorCode,
        message: String,
        targetLanguage: String,
        cause: Throwable
    ): TranslationSdkException =
        TranslationSdkException(
            code = code,
            report = errorReport(
                code = code,
                message = message,
                targetLanguage = targetLanguage
            ),
            message = message,
            cause = cause
        )

    private fun errorReport(
        code: TranslationErrorCode,
        message: String,
        targetLanguage: String? = null,
        httpStatus: Int? = null
    ): TranslationErrorReport =
        TranslationErrorReport(
            code = code,
            message = message,
            sdkVersion = SdkInfo.VERSION,
            provider = api.providerName,
            httpStatus = httpStatus,
            targetLanguage = targetLanguage
        )
}

private class InMemorySettings : Settings {
    private val values = mutableMapOf<String, Any>()

    override val keys: Set<String>
        get() = values.keys

    override val size: Int
        get() = values.size

    override fun clear() {
        values.clear()
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    override fun hasKey(key: String): Boolean = values.containsKey(key)

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int =
        values[key] as? Int ?: defaultValue

    override fun getIntOrNull(key: String): Int? = values[key] as? Int

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun getLong(key: String, defaultValue: Long): Long =
        values[key] as? Long ?: defaultValue

    override fun getLongOrNull(key: String): Long? = values[key] as? Long

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getString(key: String, defaultValue: String): String =
        values[key] as? String ?: defaultValue

    override fun getStringOrNull(key: String): String? = values[key] as? String

    override fun putFloat(key: String, value: Float) {
        values[key] = value
    }

    override fun getFloat(key: String, defaultValue: Float): Float =
        values[key] as? Float ?: defaultValue

    override fun getFloatOrNull(key: String): Float? = values[key] as? Float

    override fun putDouble(key: String, value: Double) {
        values[key] = value
    }

    override fun getDouble(key: String, defaultValue: Double): Double =
        values[key] as? Double ?: defaultValue

    override fun getDoubleOrNull(key: String): Double? = values[key] as? Double

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean =
        values[key] as? Boolean ?: defaultValue

    override fun getBooleanOrNull(key: String): Boolean? = values[key] as? Boolean
}
