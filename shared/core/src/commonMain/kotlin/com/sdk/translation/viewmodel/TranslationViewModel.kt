package com.sdk.translation.viewmodel

import com.sdk.translation.TranslationSDK
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Shared ViewModel for managing translations state across all platforms.
 * Preserves translations in memory without resending requests on configuration changes
 * (e.g., device rotation on Android).
 *
 * Usage on Android: Integrate with AndroidViewModel or use with viewModelScope.
 * Usage on iOS: Create and manage lifecycle manually or use a lifecycle manager.
 */
class TranslationViewModel(
    private val sdk: TranslationSDK,
    private val coroutineScope: CoroutineScope
) {
    private val _translations = MutableStateFlow<Map<String, String>>(emptyMap())
    private val inFlightKeys = MutableStateFlow<Set<String>>(emptySet())
    private val requestMutex = Mutex()
    private val translationStates = mutableMapOf<String, StateFlow<String?>>()

    /**
     * Public read-only StateFlow of all translations.
     * Emits immediately with current state to new collectors.
     */
    val translations: StateFlow<Map<String, String>> = _translations.asStateFlow()

    /**
     * Translate text and store the result by key.
     * If already translated under that key, no new request is made.
     *
     * @param key Unique identifier for this translation (e.g., "greeting", "title")
     * @param text The text to translate
     * @param targetLang Target language code (e.g., "es", "fr")
     */
    fun translate(key: String, text: String, targetLang: String) {
        coroutineScope.launch {
            val shouldTranslate = requestMutex.withLock {
                if (_translations.value.containsKey(key) || inFlightKeys.value.contains(key)) {
                    false
                } else {
                    inFlightKeys.update { it + key }
                    true
                }
            }

            if (!shouldTranslate) {
                return@launch
            }

            try {
                runCatching {
                    val result = sdk.translate(text, targetLang)
                    _translations.update { it + (key to result.translatedText) }
                }.onFailure { error ->
                    println("Translation failed for key=$key: ${error.message}")
                }
            } finally {
                inFlightKeys.update { it - key }
            }
        }
    }

    /**
     * Get the translation for a specific key as a Flow.
     * Returns null values for keys not yet translated.
     *
     * @param key The translation key
     * @return Flow<String?> emitting the translation value
     */
    fun getTranslation(key: String): Flow<String?> = _translations
        .map { it[key] }
        .distinctUntilChanged()

    /**
     * Get the translation for a specific key as a StateFlow.
     * Ensures immediate emission of the current value to new collectors.
     *
     * @param key The translation key
     * @return StateFlow<String?> emitting the translation value
     */
    fun getTranslationState(key: String): StateFlow<String?> {
        return translationStates.getOrPut(key) {
            getTranslation(key)
                .stateIn(
                    scope = coroutineScope,
                    started = SharingStarted.Eagerly,
                    initialValue = _translations.value[key]
                )
        }
    }

    /**
     * Force translate a key, overwriting any cached translation.
     *
     * @param key Unique identifier for this translation
     * @param text The text to translate
     * @param targetLang Target language code
     */
    fun forceTranslate(key: String, text: String, targetLang: String) {
        coroutineScope.launch {
            requestMutex.withLock {
                inFlightKeys.update { it + key }
            }

            try {
                runCatching {
                    val result = sdk.translate(text, targetLang)
                    _translations.update { it + (key to result.translatedText) }
                }.onFailure { error ->
                    println("Force translation failed for key=$key: ${error.message}")
                }
            } finally {
                inFlightKeys.update { it - key }
            }
        }
    }

    /**
     * Batch translate multiple texts.
     * Each key is only translated if not already present.
     *
     * @param items List of Triple<key, text, targetLanguage>
     */
    fun translateBatch(items: List<Triple<String, String, String>>) {
        items.forEach { (key, text, lang) ->
            translate(key, text, lang)
        }
    }

    /**
     * Clear all cached translations.
     */
    fun clearTranslations() {
        _translations.update { emptyMap() }
    }

    /**
     * Check if a translation already exists for a key.
     */
    fun hasTranslation(key: String): Boolean {
        return _translations.value.containsKey(key)
    }
}
