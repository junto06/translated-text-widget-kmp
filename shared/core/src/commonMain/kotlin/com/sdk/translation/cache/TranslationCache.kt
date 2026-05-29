package com.sdk.translation.cache

import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


class TranslationCache(
    private val settings: Settings,
    private val cacheVersion: String,
    private val cacheTtlMillis: Long?
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun get(text: String, targetLang: String): String? =
        readBucket(text, targetLang)
            .firstOrNull {
                it.text == text &&
                    it.targetLang == targetLang &&
                    it.cacheVersion == cacheVersion &&
                    !isExpired(it)
            }
            ?.translated

    fun put(text: String, targetLang: String, translated: String) {
        val bucket = readBucket(text, targetLang)
        val now = Clock.System.now().toEpochMilliseconds()
        val updated = bucket
            .filterNot {
                it.text == text &&
                    it.targetLang == targetLang &&
                    it.cacheVersion == cacheVersion
            }
            .plus(
                CacheEntry(
                    text = text,
                    targetLang = targetLang,
                    translated = translated,
                    cacheVersion = cacheVersion,
                    createdAtMillis = now
                )
            )

        settings.putString(cacheKey(text, targetLang), json.encodeToString(cacheEntryListSerializer, updated))
    }

    fun clear() {
        settings.clear()
    }

    private fun readBucket(text: String, lang: String): List<CacheEntry> {
        val cached = settings.getStringOrNull(cacheKey(text, lang)) ?: return emptyList()
        return runCatching {
            json.decodeFromString(cacheEntryListSerializer, cached)
        }.getOrDefault(emptyList())
    }

    private fun cacheKey(text: String, lang: String): String =
        "translation_${lang}_${stableHash(text)}"

    private fun stableHash(value: String): String {
        var hash = FNV_64_OFFSET_BASIS
        value.forEach { char ->
            hash = hash xor char.code.toLong()
            hash *= FNV_64_PRIME
        }
        return hash.toULong().toString(radix = 16)
    }

    private fun isExpired(entry: CacheEntry): Boolean {
        val ttl = cacheTtlMillis ?: return false
        return Clock.System.now().toEpochMilliseconds() - entry.createdAtMillis > ttl
    }

    @Serializable
    private data class CacheEntry(
        val text: String,
        val targetLang: String,
        val translated: String,
        val cacheVersion: String,
        val createdAtMillis: Long
    )

    private companion object {
        const val FNV_64_OFFSET_BASIS = -3750763034362895579L
        const val FNV_64_PRIME = 1099511628211L
        val cacheEntryListSerializer = ListSerializer(CacheEntry.serializer())
    }
}
