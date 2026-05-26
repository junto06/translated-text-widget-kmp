package com.sdk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sdk.translation.compose.TranslationSampleScreen
import com.sdk.translation.compose.rememberTranslationSDK

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val sdk = rememberTranslationSDK(
                apiKey = "YOUR_GOOGLE_TRANSLATE_API_KEY",
                defaultLanguage = "de"
            )
            TranslationSampleScreen(sdk = sdk)
        }
    }
}
