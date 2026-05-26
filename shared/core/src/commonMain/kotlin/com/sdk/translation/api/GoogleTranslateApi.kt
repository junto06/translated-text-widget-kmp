package com.sdk.translation.api

import com.sdk.translation.SdkInfo
import com.sdk.translation.errors.TranslationApiException
import com.sdk.translation.errors.TranslationErrorCode
import com.sdk.translation.errors.TranslationErrorReport
import com.sdk.translation.models.GoogleTranslateResponse
import com.sdk.translation.models.TranslationItem
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

private const val BASE_URL = "https://translation.googleapis.com/language/translate/v2"

class GoogleTranslateApi(private val apiKey: String) : TranslationApi {

    override val providerName: String = "google"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = LogLevel.NONE
        }
    }

    override suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String?
    ): TranslationItem = translateBatch(
        texts = listOf(text),
        targetLanguage = targetLanguage,
        sourceLanguage = sourceLanguage
    ).first()

    override suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: String,
        sourceLanguage: String?
    ): List<TranslationItem> {
        val httpResponse = client.get(BASE_URL) {
            parameter("key", apiKey)
            texts.forEach { parameter("q", it) }
            parameter("target", targetLanguage)
            sourceLanguage?.let { parameter("source", it) }
            parameter("format", "text")
        }
        if (!httpResponse.status.isSuccess()) {
            throw httpResponse.toApiException(targetLanguage)
        }

        val response: GoogleTranslateResponse = httpResponse.body()
        return response.data.translations
    }

    override fun close() = client.close()

    private suspend fun HttpResponse.toApiException(targetLanguage: String): TranslationApiException {
        val responseText = bodyAsText()
        val code = errorCode(status, responseText)
        val message = "Translation API request failed with HTTP ${status.value}"
        return TranslationApiException(
            statusCode = status.value,
            report = TranslationErrorReport(
                code = code,
                message = message,
                sdkVersion = SdkInfo.VERSION,
                provider = providerName,
                httpStatus = status.value,
                targetLanguage = targetLanguage
            ),
            message = message
        )
    }

    private fun errorCode(status: HttpStatusCode, responseText: String): TranslationErrorCode {
        val lowerResponse = responseText.lowercase()
        return when {
            status == HttpStatusCode.BadRequest && "key" in lowerResponse -> TranslationErrorCode.ApiInvalidKey
            status == HttpStatusCode.Unauthorized -> TranslationErrorCode.ApiInvalidKey
            status == HttpStatusCode.Forbidden && "quota" in lowerResponse -> TranslationErrorCode.ApiQuotaExceeded
            status == HttpStatusCode.Forbidden && "key" in lowerResponse -> TranslationErrorCode.ApiInvalidKey
            status == HttpStatusCode.TooManyRequests -> TranslationErrorCode.ApiQuotaExceeded
            else -> TranslationErrorCode.ApiHttpError
        }
    }
}
