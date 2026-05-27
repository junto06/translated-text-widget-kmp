package com.sdk.translation.compose

import androidx.compose.runtime.Composable
import com.sdk.translation.TranslationSDK
import com.sdk.translation.viewmodel.TranslationViewModel

@Composable
internal expect fun rememberTranslationViewModel(sdk: TranslationSDK): TranslationViewModel
