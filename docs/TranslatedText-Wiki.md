# TranslatedText Widget Wiki

This page describes how the SDK is structured, how to integrate the translated
text widgets, and how to replace Google Translate with another provider.

## Goals

TranslatedText Widget is designed around three separate concerns:

- Core translation behavior in `shared/core`
- Shared Compose text widget in `shared/compose-ui`
- Native SwiftUI text widget in `shared/ios-widget`

The core module does not depend on Compose or SwiftUI. Widget layers receive a
`TranslationSDK` instance and ask it for translations before rendering text.

## Module Overview

### `shared/core`

Contains the public SDK:

- `TranslationSDK`
- `TranslationSDK.Builder`
- `TranslationApi`
- `GoogleTranslateApi`
- cache and error handling
- translation models
- shared `TranslationViewModel`

The important extension point is:

```kotlin
interface TranslationApi : Closeable {
    val providerName: String

    suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: String,
        sourceLanguage: String? = null
    ): List<TranslationItem>
}
```

`TranslationSDK` depends on this interface, not on Google directly.

### `shared/compose-ui`

Contains the Compose text widget:

- `TranslatedText`
- `TranslationSampleScreen`
- `rememberTranslationSDK`
- Android ViewModel wrapper
- iOS Compose entry point through `MainViewController`

Use this module when the host app wants a Compose `TranslatedText` widget on
Android or Compose Multiplatform on iOS.

### `shared/ios-widget`

Contains the native SwiftUI package:

- package name: `TranslationSDKUI`
- public view: `TranslatedText`

The SwiftUI widget accepts a translation closure instead of owning the Kotlin SDK
directly. This keeps the widget lightweight and makes it usable with any
translation backend.

### `iosApp`

The iOS sample consumes both iOS options:

- SwiftUI tab: native `TranslationSDKUI.TranslatedText`
- Compose tab: `ComposeUI.MainViewControllerKt.MainViewController()`

## Provider Model

The SDK supports two provider paths.

### Built-In Google Provider

```kotlin
val sdk = TranslationSDK.Builder()
    .apiKey("YOUR_GOOGLE_TRANSLATE_API_KEY")
    .defaultLanguage("de")
    .build()
```

This creates `GoogleTranslateApi` internally.

### Custom Provider

```kotlin
val sdk = TranslationSDK.Builder()
    .translationApi(MyTranslationApi())
    .defaultLanguage("de")
    .build()
```

When `translationApi(...)` is provided, `apiKey(...)` is not required.

## Implementing a Custom Provider

A provider only needs to return `TranslationItem` values in the same order as
the input texts.

```kotlin
import com.sdk.translation.api.TranslationApi
import com.sdk.translation.models.TranslationItem

class MyTranslationApi(
    private val client: MyLlmClient
) : TranslationApi {
    override val providerName = "my-llm"

    override suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: String,
        sourceLanguage: String?
    ): List<TranslationItem> {
        return texts.map { text ->
            val translated = client.translate(
                text = text,
                targetLanguage = targetLanguage,
                sourceLanguage = sourceLanguage
            )

            TranslationItem(
                translatedText = translated,
                detectedSourceLanguage = sourceLanguage
            )
        }
    }

    override fun close() {
        client.close()
    }
}
```

Recommended provider behavior:

- Preserve input order in `translateBatch`.
- Return one `TranslationItem` per input string.
- Throw an exception when the provider fails.
- Override `providerName` so error reports identify the provider.
- Close HTTP clients or streaming clients in `close()`.

## OpenAI or DeepSeek Integration Shape

The SDK does not hard-code an LLM request format. A provider can call any service
as long as it maps the response back into `TranslationItem`.

Typical LLM prompt constraints:

- Ask for translation only.
- Preserve meaning and tone.
- Avoid adding explanations.
- For batch requests, return exactly one translated string per input.
- Keep output order identical to input order.

Example skeleton:

```kotlin
class OpenAiTranslationApi(
    private val client: OpenAiClient
) : TranslationApi {
    override val providerName = "openai"

    override suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: String,
        sourceLanguage: String?
    ): List<TranslationItem> {
        val translatedTexts = client.translateBatch(
            texts = texts,
            targetLanguage = targetLanguage,
            sourceLanguage = sourceLanguage
        )

        return translatedTexts.map { translated ->
            TranslationItem(translatedText = translated)
        }
    }
}
```

## Android Integration

Use the Compose helper when the SDK should be created and closed with the
Composable lifecycle.

```kotlin
val sdk = rememberTranslationSDK(
    apiKey = "YOUR_GOOGLE_TRANSLATE_API_KEY",
    defaultLanguage = "de"
)
```

Custom provider:

```kotlin
val sdk = rememberTranslationSDK(
    translationApi = MyTranslationApi(client),
    defaultLanguage = "de"
)
```

Render translated text:

```kotlin
TranslatedText(
    rawText = "This label can be translated.",
    sdk = sdk,
    translationRequired = true,
    targetLanguage = "fr",
    seeTranslationText = "Show French",
    hideTranslationText = "Hide French"
)
```

## iOS SwiftUI Integration

The SwiftUI package exposes a native SwiftUI view:

```swift
import TranslationSDK
import TranslationSDKUI
```

Create the Kotlin SDK:

```swift
let sdk = TranslationSDK.Builder()
    .apiKey(key: "YOUR_GOOGLE_TRANSLATE_API_KEY")
    .defaultLanguage(lang: "de")
    .build()
```

Connect it to the SwiftUI widget:

```swift
TranslatedText(
    rawText: "This label can be translated.",
    translationRequired: true,
    targetLanguage: "fr",
    seeTranslationText: "Voir la traduction",
    hideTranslationText: "Masquer la traduction",
    translate: { text, targetLanguage, completion in
        sdk.translate(text: text, targetLanguage: targetLanguage) { result, _ in
            completion(result?.translatedText)
        }
    }
)
```

The SwiftUI widget uses a closure so it can also call a native iOS translation
service directly if the app does not want to route through Kotlin.

## iOS Compose Integration

The Compose widget framework exposes an iOS view controller:

```swift
import ComposeUI
import SwiftUI
import UIKit

struct ComposeWidgetSample: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
    }
}
```

The sample app shows this in a separate tab from the native SwiftUI widget.

## Caching Behavior

`TranslationSDK` checks cache before calling the provider. Cache keys include the
source text and target language. If a cached translation exists, no provider call
is made.

Builder options:

```kotlin
TranslationSDK.Builder()
    .cacheVersion("default")
    .cacheTtlMillis(null)
```

Use `cacheVersion(...)` to invalidate old cached values after changing provider
behavior, prompt rules, or translation quality settings.

Use `cacheTtlMillis(...)` to expire translations after a fixed time.

## Error Handling

Use `TranslationErrorReporter` to receive SDK errors:

```kotlin
import com.sdk.translation.errors.TranslationErrorReporter
import com.sdk.translation.errors.TranslationSdkException

val sdk = TranslationSDK.Builder()
    .apiKey("YOUR_GOOGLE_TRANSLATE_API_KEY")
    .errorReporter(
        object : TranslationErrorReporter {
            override fun report(error: TranslationSdkException) {
                println(error.report)
            }
        }
    )
    .build()
```

Error reports include:

- error code
- message
- SDK version
- provider name
- HTTP status when available
- target language when available

## Lifecycle

Call `close()` when the SDK is no longer needed:

```kotlin
sdk.close()
```

`rememberTranslationSDK(...)` handles this automatically with `DisposableEffect`.

On iOS, close the SDK from the owning view model or app lifecycle object when it
is no longer needed.

## Build Commands

Android sample:

```bash
./gradlew :sampleAndroid:assembleDebug
```

Shared Kotlin verification:

```bash
./gradlew :shared:core:compileAndroidMain \
  :shared:compose-ui:compileAndroidMain \
  :shared:core:linkDebugFrameworkIosSimulatorArm64 \
  :shared:compose-ui:linkDebugFrameworkIosSimulatorArm64
```

iOS sample:

```bash
xcodebuild -project iosApp/TranslatedTextWidget.xcodeproj \
  -scheme "TranslatedText Widget" \
  -sdk iphonesimulator \
  -configuration Debug build \
  CODE_SIGNING_ALLOWED=NO
```

## Current Notes

- The iOS sample links both `TranslationSDK.framework` and `ComposeUI.framework`.
- `shared/ios-widget` is a Swift Package and does not depend directly on the
  Kotlin framework.
- The Compose iOS build may emit an ICU deployment-target warning from Compose
  dependencies. The app still builds successfully.
