@file:Suppress("FunctionName")

package com.sdk.translation.compose

import androidx.compose.ui.window.ComposeUIViewController
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
