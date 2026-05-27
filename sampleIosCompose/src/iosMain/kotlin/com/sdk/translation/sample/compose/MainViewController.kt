package com.sdk.translation.sample.compose

import androidx.compose.ui.window.ComposeUIViewController
import com.sdk.translation.compose.rememberTranslationSDK
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        val sdk = rememberTranslationSDK(
            apiKey = "YOUR_GOOGLE_TRANSLATE_API_KEY",
            defaultLanguage = "de"
        )

        TranslationSampleScreen(sdk = sdk)
    }
}
