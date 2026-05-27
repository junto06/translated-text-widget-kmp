package com.sdk.translation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.russhwolf.settings.Settings
import com.sdk.translation.TranslationSDK
import com.sdk.translation.api.TranslationApi
import com.sdk.translation.errors.TranslationErrorReporter

@Composable
fun rememberTranslationSDK(
    apiKey: String = "",
    defaultLanguage: String = "en",
    translationApi: TranslationApi? = null,
    settings: Settings? = null,
    cacheVersion: String = "default",
    cacheTtlMillis: Long? = null,
    errorReporter: TranslationErrorReporter? = null
): TranslationSDK {
    val sdk = remember(
        apiKey,
        defaultLanguage,
        translationApi,
        settings,
        cacheVersion,
        cacheTtlMillis,
        errorReporter
    ) {
        TranslationSDK.Builder()
            .apiKey(apiKey)
            .defaultLanguage(defaultLanguage)
            .apply {
                translationApi?.let { translationApi(it) }
                settings?.let { settings(it) }
                errorReporter?.let { errorReporter(it) }
            }
            .cacheVersion(cacheVersion)
            .cacheTtlMillis(cacheTtlMillis)
            .build()
    }

    DisposableEffect(sdk) {
        onDispose {
            sdk.close()
        }
    }

    return sdk
}
