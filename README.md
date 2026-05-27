# TranslatedText Widget

TranslatedText Widget is a Kotlin Multiplatform translation SDK with a
ready-to-use text widget for showing translated dynamic content on Android and
iOS.

It includes:

- `shared/core`: translation SDK, provider interface, cache, models, errors
- `shared/compose-ui`: Jetpack Compose and Compose Multiplatform text widget
- `shared/ios-widget`: native SwiftUI widget as a Swift Package
- `sampleAndroid`: Android sample app
- `iosApp`: iOS sample app with SwiftUI
- `sampleIosCompose`: iOS Compose sample target

The SDK ships with Google Translate support, but the core API is provider-based.
Developers can plug in OpenAI, DeepSeek, another LLM, or an internal translation
service by implementing `TranslationApi`.

## Quick Start

### Google Translate

The built-in Google provider requires the host app to pass a Google Translate
API key into `TranslationSDK.Builder.apiKey(...)` or `rememberTranslationSDK(...)`.
The SDK does not read API keys from environment variables, Gradle files,
Info.plist, or bundled config by itself.

Do not commit real API keys. Load them from your app's own secret boundary, such
as Android `local.properties`/`BuildConfig`, CI secrets, a backend-issued token,
or an iOS build setting/config file that is excluded from source control.

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
provide `translationApi(...)`, the SDK uses your provider instead and does not
require a Google key.

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

xcodebuild -project iosApp/TranslatedTextWidget.xcodeproj \
  -scheme "TranslatedText Compose" \
  -sdk iphonesimulator \
  -configuration Debug build \
  CODE_SIGNING_ALLOWED=NO
```

Before testing real translations, replace `YOUR_GOOGLE_TRANSLATE_API_KEY` from a
local secret source in:

- `sampleAndroid/src/main/kotlin/com/sdk/android/MainActivity.kt`
- `iosApp/iosApp/ContentView.swift`
- `sampleIosCompose/src/iosMain/kotlin/com/sdk/translation/sample/compose/MainViewController.kt`

## Documentation

See [docs/TranslatedText-Wiki.md](docs/TranslatedText-Wiki.md) for detailed
architecture, provider implementation, platform integration, caching, and build
notes.

## Language Codes

Use BCP-47 language tags such as `en`, `de`, `fr`, `es`, `ja`, `zh`, and `ar`.
