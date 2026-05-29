# TranslatedText Widget

TranslatedText Widget is a Kotlin Multiplatform translation SDK with a
ready-to-use text widget for showing translated dynamic content on Android and
iOS.

It includes:

- `shared/core`: translation SDK, provider interface, built-in LLM providers, cache, models, errors
- `shared/compose-ui`: Jetpack Compose and Compose Multiplatform text widget
- `shared/ios-widget`: native SwiftUI widget as a Swift Package
- `sampleAndroid`: Android sample app
- `iosApp`: iOS sample app with SwiftUI
- `sampleIosCompose`: iOS Compose sample target

The SDK ships with Google Translate and built-in OpenAI-compatible LLM providers,
but the core API is provider-based. Developers can plug in any service by
implementing `TranslationApi`.

## Quick Start

### 1. Initialize the SDK once

Initialize the SDK once before any `TranslatedText` widget is used, for example
in `Application.onCreate()` on Android or at app startup on iOS.

#### With Google Translate

```kotlin
TranslationSDK.init(
    TranslationSDK.Builder()
        .apiKey("YOUR_GOOGLE_TRANSLATE_API_KEY")
        .defaultLanguage("de")
        .build()
)
```

#### With a built-in LLM provider

```kotlin
import com.sdk.translation.api.OpenAiTranslationApi
import com.sdk.translation.api.DeepSeekTranslationApi

// OpenAI
TranslationSDK.init(
    TranslationSDK.Builder()
        .translationApi(OpenAiTranslationApi(apiKey = "YOUR_OPENAI_API_KEY"))
        .defaultLanguage("de")
        .build()
)

// DeepSeek
TranslationSDK.init(
    TranslationSDK.Builder()
        .translationApi(DeepSeekTranslationApi(apiKey = "YOUR_DEEPSEEK_API_KEY"))
        .defaultLanguage("de")
        .build()
)
```

Call `TranslationSDK.close()` when the SDK is no longer needed (e.g.
`Application.onTerminate()`) to release the underlying HTTP client.

### 2. Use the widget

No SDK reference is needed at the call site:

```kotlin
TranslatedText(
    rawText = "Hello World",
    translationRequired = true,
    targetLanguage = "de"
)
```

If `TranslationSDK.init()` has not been called, `TranslatedText` renders the
raw text silently rather than crashing.

### API key management

The built-in Google provider requires a Google Translate API key passed into
`TranslationSDK.Builder.apiKey(...)`. The built-in LLM providers require an
API key passed directly into their constructor. The SDK does not read API keys
from environment variables, Gradle files, Info.plist, or bundled config by
itself.

Do not commit real API keys. Load them from your app's own secret boundary:

- **Android**: add `deepseek.api.key=YOUR_KEY` (or your chosen key name) to
  `local.properties` (which is gitignored). Read it into `BuildConfig` via
  `sampleAndroid/build.gradle.kts`. For CI, set the value as an environment
  variable (e.g. `DEEPSEEK_API_KEY`) and read it with
  `System.getenv("DEEPSEEK_API_KEY")` as a fallback.
- **iOS**: copy `iosApp/Secrets.swift.template` to `iosApp/Secrets.swift`
  (gitignored) and set `let deepseekApiKey = "YOUR_KEY"`. Both Xcode targets
  reference this file.

### Custom Translation Provider

Use `translationApi(...)` when you want the SDK to call your own provider.

```kotlin
import com.sdk.translation.api.TranslationApi
import com.sdk.translation.models.TranslationItem

class MyTranslationApi : TranslationApi {
    override val providerName = "my-llm"

    override suspend fun translateBatch(
        texts: List<String>,
        targetLanguage: String,
        sourceLanguage: String?
    ): List<TranslationItem> {
        return texts.map { text ->
            TranslationItem(
                translatedText = translateWithMyService(text, targetLanguage),
                detectedSourceLanguage = sourceLanguage
            )
        }
    }
}

TranslationSDK.init(
    TranslationSDK.Builder()
        .translationApi(MyTranslationApi())
        .defaultLanguage("de")
        .build()
)
```

`apiKey(...)` is only required for the built-in Google implementation. If you
provide `translationApi(...)`, the SDK uses your provider instead and does not
require a Google key.

## Android Compose Widget

```kotlin
// Application.onCreate
TranslationSDK.init(
    TranslationSDK.Builder()
        .translationApi(DeepSeekTranslationApi(apiKey = BuildConfig.DEEPSEEK_API_KEY))
        .defaultLanguage("de")
        .build()
)

// Any composable
TranslatedText(
    rawText = "Hello World",
    translationRequired = true,
    targetLanguage = "de"
)
```

## iOS SwiftUI Widget

The native SwiftUI widget lives in `shared/ios-widget` and is consumed as a Swift
Package named `TranslationSDKUI`.

```swift
import TranslationSDK
import TranslationSDKUI
```

The SDK is initialized once in the Swift `App` entry point using a `StateObject`:

```swift
private final class AppSDK: ObservableObject {
    init() {
        TranslationSDK.companion.doInit(sdk:
            TranslationSDK.Builder()
                .translationApi(api: DeepSeekTranslationApi(apiKey: deepseekApiKey, model: "deepseek-chat"))
                .build()
        )
    }
    deinit { TranslationSDK.companion.close() }
}

@main struct TranslatedTextWidgetApp: App {
    @StateObject private var appSdk = AppSDK()
    var body: some Scene { WindowGroup { ContentView() } }
}
```

> Note: the Kotlin `init` method is exposed in Swift as `doInit(sdk:)`. Kotlin
> default parameter values are not bridged to Swift, so all parameters (such as
> `model:`) must be passed explicitly.

Use the singleton instance in the translate closure passed to the widget:

```swift
TranslatedText(
    rawText: "Hello World",
    translationRequired: true,
    targetLanguage: "de",
    translate: { text, targetLanguage, completion in
        TranslationSDK.companion.instance.translate(text: text, targetLanguage: targetLanguage) { result, _ in
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

Before building, add your API key to `local.properties`:

```
deepseek.api.key=YOUR_KEY
```

iOS:

```bash
# Copy the secrets template and fill in your key
cp iosApp/Secrets.swift.template iosApp/Secrets.swift

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

## Documentation

See [docs/TranslatedText-Wiki.md](docs/TranslatedText-Wiki.md) for detailed
architecture, provider implementation, platform integration, caching, and build
notes.

## Language Codes

Use BCP-47 language tags such as `en`, `de`, `fr`, `es`, `ja`, `zh`, and `ar`.
