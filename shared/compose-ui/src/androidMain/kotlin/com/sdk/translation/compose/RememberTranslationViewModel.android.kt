package com.sdk.translation.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sdk.translation.TranslationSDK
import com.sdk.translation.ui.AndroidTranslationViewModel
import com.sdk.translation.ui.AndroidTranslationViewModelFactory
import com.sdk.translation.viewmodel.TranslationViewModel

@Composable
internal actual fun rememberTranslationViewModel(sdk: TranslationSDK): TranslationViewModel {
    val androidViewModel: AndroidTranslationViewModel = viewModel(
        key = "TranslationViewModel:${System.identityHashCode(sdk)}",
        factory = AndroidTranslationViewModelFactory(sdk)
    )
    return androidViewModel.sharedViewModel
}
