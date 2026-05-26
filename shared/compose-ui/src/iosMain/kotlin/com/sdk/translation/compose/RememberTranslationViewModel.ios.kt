package com.sdk.translation.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.sdk.translation.TranslationSDK
import com.sdk.translation.viewmodel.TranslationViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import platform.Foundation.NSLock

private class CachedTranslationViewModel(
    val scope: CoroutineScope,
    val viewModel: TranslationViewModel
) {
    var references: Int = 0
}

private val cacheLock = NSLock()
private val cachedViewModels = mutableMapOf<TranslationSDK, CachedTranslationViewModel>()

private inline fun <T> withCacheLock(block: () -> T): T {
    cacheLock.lock()
    return try {
        block()
    } finally {
        cacheLock.unlock()
    }
}

@Composable
internal actual fun rememberTranslationViewModel(sdk: TranslationSDK): TranslationViewModel {
    val cached = remember(sdk) {
        withCacheLock {
            cachedViewModels.getOrPut(sdk) {
                val scope = MainScope()
                CachedTranslationViewModel(
                    scope = scope,
                    viewModel = TranslationViewModel(sdk, scope)
                )
            }
        }
    }

    DisposableEffect(cached) {
        withCacheLock {
            cached.references += 1
        }

        onDispose {
            val shouldCancel = withCacheLock {
                cached.references -= 1
                if (cached.references == 0) {
                    cachedViewModels.remove(sdk)
                    true
                } else {
                    false
                }
            }

            if (shouldCancel) {
                cached.scope.cancel()
            }
        }
    }

    return cached.viewModel
}
