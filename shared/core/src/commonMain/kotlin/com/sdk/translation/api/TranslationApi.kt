package com.sdk.translation.api

import com.sdk.translation.Closeable
import com.sdk.translation.models.TranslationItem

interface TranslationApi : Closeable {
    val providerName: String
        get() = "custom"

    suspend fun translate(
        text: String,
        targetLanguage: String,
        sourceLanguage: String? = null
    ): TranslationItem = translateBatch(
        texts = listOf(text),
        targetLanguage = targetLanguage,
        sourceLanguage = sourceLanguage
    ).first()

    suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: String,
        sourceLanguage: String? = null
    ): List<TranslationItem>

    override fun close() = Unit
}
