# TranslatedText Widget

TranslatedText Widget is a Kotlin Multiplatform translation SDK with a
ready-to-use text widget for showing translated dynamic content on Android and
iOS.

It includes:

- `shared/core`: translation SDK, provider interface, cache, models, errors
- `shared/compose-ui`: Jetpack Compose and Compose Multiplatform text widget
- `shared/ios-widget`: native SwiftUI widget as a Swift Package
- `sampleAndroid`: Android sample app
- `iosApp`: iOS sample app with SwiftUI and Compose tabs

The SDK ships with Google Translate support, but the core API is provider-based.
Developers can plug in OpenAI, DeepSeek, another LLM, or an internal translation
service by implementing `TranslationApi`.

## Quick Start

### Google Translate

```kotlin
val sdk = TranslationSDK.Builder()
    .apiKey("YOUR_GOOGLE_TRANSLATE_API_KEY")
    .defaultLanguage("de")
    .build()
```

On iOS:

```swift
let sdk = TranslationSDK.Builder()
    .apiKey(key: "YOUR_GOOGLE_TRANSLATE_API_KEY")
    .defaultLanguage(lang: "de")
    .build()
```

### Custom Translation Provider

Use `translationApi(...)` when you want the SDK to call your own provider.

```kotlin
import com.sdk.translation.api.TranslationApi
import com.sdk.translation.models.TranslationItem

class OpenAiTranslationApi : TranslationApi {
    override val providerName = "openai"

    override suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: String,
        sourceLanguage: String?
    ): List<TranslationItem> {
        return texts.map { text ->
            TranslationItem(
                translatedText = translateWithOpenAi(text, targetLanguage),
                detectedSourceLanguage = sourceLanguage
            )
        }
    }
}

val sdk = TranslationSDK.Builder()
    .translationApi(OpenAiTranslationApi())
    .defaultLanguage("de")
    .build()
```

`apiKey(...)` is only required for the built-in Google implementation. If you
provide `translationApi(...)`, the SDK uses your provider instead.

## Android Compose Widget

```kotlin
val sdk = rememberTranslationSDK(
    apiKey = "YOUR_GOOGLE_TRANSLATE_API_KEY",
    defaultLanguage = "de"
)

TranslatedText(
    rawText = "Hello World",
    sdk = sdk,
    translationRequired = true,
    targetLanguage = "de"
)
```

With a custom provider:

```kotlin
val sdk = rememberTranslationSDK(
    translationApi = OpenAiTranslationApi(),
    defaultLanguage = "de"
)
```

## iOS SwiftUI Widget

The native SwiftUI widget lives in `shared/ios-widget` and is consumed as a Swift
Package named `TranslationSDKUI`.

```swift
import TranslationSDK
import TranslationSDKUI
```

```swift
TranslatedText(
    rawText: "Hello World",
    translationRequired: true,
    targetLanguage: "de",
    translate: { text, targetLanguage, completion in
        sdk.translate(text: text, targetLanguage: targetLanguage) { result, _ in
            completion(result?.translatedText)
        }
    }
)
```

## iOS Compose Widget

The iOS sample also embeds the Compose Multiplatform widget:

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

## Sample Apps

Android:

```bash
./gradlew :sampleAndroid:assembleDebug
```

iOS:

```bash
xcodebuild -project iosApp/TranslatedTextWidget.xcodeproj \
  -scheme "TranslatedText Widget" \
  -sdk iphonesimulator \
  -configuration Debug build \
  CODE_SIGNING_ALLOWED=NO
```

Before testing real translations, replace `YOUR_GOOGLE_TRANSLATE_API_KEY` in:

- `sampleAndroid/src/main/kotlin/com/sdk/android/MainActivity.kt`
- `iosApp/iosApp/ContentView.swift`
- `shared/compose-ui/src/iosMain/kotlin/com/sdk/translation/compose/MainViewController.kt`

## Documentation

See [docs/TranslatedText-Wiki.md](docs/TranslatedText-Wiki.md) for detailed
architecture, provider implementation, platform integration, caching, and build
notes.

## Language Codes

Use BCP-47 language tags such as `en`, `de`, `fr`, `es`, `ja`, `zh`, and `ar`.
