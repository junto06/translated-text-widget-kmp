package com.sdk.translation.models

import kotlinx.serialization.Serializable

data class TranslationRequest(
    val text: String,
    val targetLanguage: String,
    val sourceLanguage: String? = null
)

data class TranslationResult(
    val originalText: String,
    val translatedText: String,
    val detectedSourceLanguage: String?,
    val targetLanguage: String
)

@Serializable
data class GoogleTranslateResponse(
    val data: TranslateData
)

@Serializable
data class TranslateData(
    val translations: List<TranslationItem>
)

@Serializable
data class TranslationItem(
    val translatedText: String,
    val detectedSourceLanguage: String? = null
)
