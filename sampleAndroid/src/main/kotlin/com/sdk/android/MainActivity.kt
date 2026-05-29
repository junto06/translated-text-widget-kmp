package com.sdk.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.sdk.translation.TranslationSDK
import com.sdk.translation.api.DeepSeekTranslationApi
import com.sdk.translation.errors.TranslationErrorReporter
import com.sdk.translation.errors.TranslationSdkException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TranslationSDK.init(
            TranslationSDK.Builder()
                .translationApi(DeepSeekTranslationApi(apiKey = BuildConfig.DEEPSEEK_API_KEY))
                .context(applicationContext)
                .errorReporter(object : TranslationErrorReporter {
                    override fun report(error: TranslationSdkException) {
                        // Handle translation errors here
                        println("Translation error: ${error.message} ${error}")
                        error.printStackTrace()
                    }
                })
                .build()
        )
        setContent {
            TranslationSampleScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        TranslationSDK.close()
    }
}
