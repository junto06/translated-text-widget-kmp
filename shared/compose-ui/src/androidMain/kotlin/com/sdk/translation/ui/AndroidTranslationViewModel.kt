package com.sdk.translation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewModelScope
import com.sdk.translation.TranslationSDK
import com.sdk.translation.viewmodel.TranslationViewModel

/**
 * Android wrapper around the shared TranslationViewModel.
 * This integrates with Android's ViewModel lifecycle, ensuring
 * the ViewModel survives configuration changes like device rotation.
 */
internal class AndroidTranslationViewModel(sdk: TranslationSDK) : ViewModel() {
    internal val sharedViewModel = TranslationViewModel(sdk, viewModelScope)

    // Delegate all public APIs to the shared ViewModel
    val translations = sharedViewModel.translations

    fun translate(key: String, text: String, targetLang: String) {
        sharedViewModel.translate(key, text, targetLang)
    }

    fun getTranslation(key: String) = sharedViewModel.getTranslation(key)

    fun getTranslationState(key: String) = sharedViewModel.getTranslationState(key)

    fun forceTranslate(key: String, text: String, targetLang: String) {
        sharedViewModel.forceTranslate(key, text, targetLang)
    }

    fun translateBatch(items: List<Triple<String, String, String>>) {
        sharedViewModel.translateBatch(items)
    }

    fun clearTranslations() {
        sharedViewModel.clearTranslations()
    }

    fun hasTranslation(key: String): Boolean {
        return sharedViewModel.hasTranslation(key)
    }
}

internal class AndroidTranslationViewModelFactory(
    private val sdk: TranslationSDK
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        if (modelClass.isAssignableFrom(AndroidTranslationViewModel::class.java)) {
            return AndroidTranslationViewModel(sdk) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
