package com.sdk.translation.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdk.translation.TranslationSDK

@Composable
fun TranslationSampleScreen(
    sdk: TranslationSDK,
    modifier: Modifier = Modifier
) {
    MaterialTheme {
        Surface(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(
                    text = "TranslatedText Widget",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Tap See translation below each label to load and reveal translated text.",
                    style = MaterialTheme.typography.bodyMedium
                )

                SampleItem {
                    TranslatedText(
                        rawText = "Welcome to the TranslatedText Widget sample app.",
                        sdk = sdk,
                        translationRequired = false,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                SampleItem {
                    TranslatedText(
                        rawText = "This label starts in English and can be translated on demand.",
                        sdk = sdk,
                        translationRequired = true,
                        targetLanguage = "es"
                    )
                }

                SampleItem {
                    TranslatedText(
                        rawText = "You can customize the link text for your product.",
                        sdk = sdk,
                        translationRequired = true,
                        targetLanguage = "fr",
                        seeTranslationText = "Show French",
                        hideTranslationText = "Hide French"
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleItem(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        content()
        HorizontalDivider()
    }
}
