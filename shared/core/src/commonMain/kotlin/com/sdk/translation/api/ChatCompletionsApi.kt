package com.sdk.translation.api

import com.sdk.translation.SdkInfo
import com.sdk.translation.errors.TranslationApiException
import com.sdk.translation.errors.TranslationErrorCode
import com.sdk.translation.errors.TranslationErrorReport
import com.sdk.translation.models.TranslationItem
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

private const val SYSTEM_PROMPT =
    "You are a professional translator. Translate text accurately, preserving meaning and tone. " +
    "Return only the requested JSON array format with no explanations."

abstract class ChatCompletionsTranslationApi(
    private val apiKey: String,
    private val model: String,
    private val baseUrl: String
) : TranslationApi {

    private val client = HttpClient {
        install(ContentNegotiation) { json(json) }
    }

    override suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: String,
        sourceLanguage: String?
    ): List<TranslationItem> {
        val httpResponse = client.post(baseUrl) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            setBody(
                ChatRequest(
                    model = model,
                    messages = listOf(
                        Message(role = "system", content = SYSTEM_PROMPT),
                        Message(role = "user", content = buildPrompt(texts, targetLanguage, sourceLanguage))
                    )
                )
            )
        }
        if (!httpResponse.status.isSuccess()) {
            val responseText = httpResponse.bodyAsText()
            throw TranslationApiException(
                statusCode = httpResponse.status.value,
                report = TranslationErrorReport(
                    code = TranslationErrorCode.ApiHttpError,
                    message = "Translation API request failed with HTTP ${httpResponse.status.value}",
                    sdkVersion = SdkInfo.VERSION,
                    provider = providerName,
                    httpStatus = httpResponse.status.value,
                    targetLanguage = targetLanguage
                ),
                message = "Translation API request failed with HTTP ${httpResponse.status.value}: $responseText"
            )
        }
        val content = httpResponse.body<ChatResponse>().choices.first().message.content
        return parseTranslations(content, texts.size)
    }

    private fun buildPrompt(texts: List<String>, targetLanguage: String, sourceLanguage: String?): String {
        val from = sourceLanguage?.let { " from $it" } ?: ""
        return buildString {
            appendLine("Translate the following ${texts.size} text(s)$from to $targetLanguage.")
            appendLine("Return ONLY a JSON array of translated strings in the same order.")
            appendLine("Example: [\"translation1\", \"translation2\"]")
            appendLine()
            texts.forEachIndexed { i, text -> appendLine("${i + 1}. $text") }
        }
    }

    private fun parseTranslations(content: String, expectedCount: Int): List<TranslationItem> =
        runCatching {
            json.decodeFromString<List<String>>(content.trim())
                .map { TranslationItem(translatedText = it) }
        }.getOrElse {
            List(expectedCount) { TranslationItem(translatedText = content.trim()) }
        }

    override fun close() = client.close()
}

class OpenAiTranslationApi(
    apiKey: String,
    model: String = "gpt-4o-mini"
) : ChatCompletionsTranslationApi(
    apiKey = apiKey,
    model = model,
    baseUrl = "https://api.openai.com/v1/chat/completions"
) {
    override val providerName = "openai"
}

class DeepSeekTranslationApi(
    apiKey: String,
    model: String = "deepseek-chat"
) : ChatCompletionsTranslationApi(
    apiKey = apiKey,
    model = model,
    baseUrl = "https://api.deepseek.com/chat/completions"
) {
    override val providerName = "deepseek"
}

@Serializable
private data class ChatRequest(
    val model: String,
    val messages: List<Message>
)

@Serializable
private data class Message(
    val role: String,
    val content: String
)

@Serializable
private data class ChatResponse(
    val choices: List<Choice>
)

@Serializable
private data class Choice(
    val message: Message
)
