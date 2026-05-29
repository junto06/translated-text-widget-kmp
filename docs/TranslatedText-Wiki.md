# TranslatedText Widget Wiki

This page describes how the SDK is structured, how to integrate the translated
text widgets, and how to replace or extend the translation provider.

## Goals

TranslatedText Widget is designed around three separate concerns:

- Core translation behavior in `shared/core`
- Shared Compose text widget in `shared/compose-ui`
- Native SwiftUI text widget in `shared/ios-widget`

The core module does not depend on Compose or SwiftUI. The SDK is initialized
once as a singleton and accessed globally by the widget layer.

## Module Overview

### `shared/core`

Contains the public SDK:

- `TranslationSDK`
- `TranslationSDK.Builder`
- `TranslationApi`
- Built-in providers in `com.sdk.translation.api`:
  - `GoogleTranslateApi`
  - `ChatCompletionsTranslationApi` (abstract base for OpenAI-compatible APIs)
  - `OpenAiTranslationApi(apiKey, model = "gpt-4o-mini")`
  - `DeepSeekTranslationApi(apiKey, model = "deepseek-chat")`
- cache and error handling
- translation models
- shared `TranslationViewModel`

The extension point for custom providers is:

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

`TranslationSDK` depends on this interface, not on any provider directly.

### `shared/compose-ui`

Contains the Compose text widget:

- `TranslatedText`
- Android ViewModel wrapper

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

The iOS sample initializes the SDK in the Swift `App` entry point and consumes
the native SwiftUI widget through `TranslationSDKUI.TranslatedText`.

### `sampleIosCompose`

The Compose sample target is sample-only and consumes the production widget
through the `ComposeSampleUI` framework and its `MainViewControllerKt.MainViewController()`
entry point. `MainViewController.kt` does not initialize the SDK — it only
returns the Compose UI. SDK initialization is handled by the Swift `App` entry
point alongside the SwiftUI sample.

## Provider Model

### Built-In Providers

#### Google Translate

A thin Google Translate REST client. The host app owns the API key boundary and
passes the key into `TranslationSDK.Builder.apiKey(...)`. The SDK does not
discover keys from environment variables, Gradle files, Info.plist, or bundled
config.

```kotlin
TranslationSDK.init(
    TranslationSDK.Builder()
        .apiKey("YOUR_GOOGLE_TRANSLATE_API_KEY")
        .defaultLanguage("de")
        .build()
)
```

#### OpenAI and DeepSeek

`OpenAiTranslationApi` and `DeepSeekTranslationApi` are ready-to-use
implementations of `ChatCompletionsTranslationApi`, an abstract base that handles
the OpenAI-compatible chat completions request format.

```kotlin
import com.sdk.translation.api.OpenAiTranslationApi
import com.sdk.translation.api.DeepSeekTranslationApi

// OpenAI (defaults to gpt-4o-mini)
TranslationSDK.init(
    TranslationSDK.Builder()
        .translationApi(OpenAiTranslationApi(apiKey = "YOUR_OPENAI_API_KEY"))
        .defaultLanguage("de")
        .build()
)

// DeepSeek (defaults to deepseek-chat)
TranslationSDK.init(
    TranslationSDK.Builder()
        .translationApi(DeepSeekTranslationApi(apiKey = "YOUR_DEEPSEEK_API_KEY"))
        .defaultLanguage("de")
        .build()
)

// Explicit model selection
TranslationSDK.init(
    TranslationSDK.Builder()
        .translationApi(OpenAiTranslationApi(apiKey = "YOUR_OPENAI_API_KEY", model = "gpt-4o"))
        .defaultLanguage("de")
        .build()
)
```

When `translationApi(...)` is provided, `apiKey(...)` is not required and the
Google provider is not used.

### Custom Provider

```kotlin
TranslationSDK.init(
    TranslationSDK.Builder()
        .translationApi(MyTranslationApi())
        .defaultLanguage("de")
        .build()
)
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

## LLM Provider Shape

The built-in `ChatCompletionsTranslationApi` base class handles the
OpenAI-compatible chat completions format. To target any other OpenAI-compatible
endpoint (e.g. a locally-run model via Ollama), subclass it directly:

```kotlin
class OllamaTranslationApi(
    apiKey: String = "ollama",
    model: String = "llama3"
) : ChatCompletionsTranslationApi(
    apiKey = apiKey,
    model = model,
    baseUrl = "http://localhost:11434/v1/chat/completions"
) {
    override val providerName = "ollama"
}
```

When building a custom LLM provider that targets a different API entirely, follow
these prompt constraints:

- Ask for translation only.
- Preserve meaning and tone.
- Avoid adding explanations.
- For batch requests, return exactly one translated string per input.
- Keep output order identical to input order.

## Secret Management

### Android

Add your API key to `local.properties` (gitignored):

```
deepseek.api.key=YOUR_KEY
```

`sampleAndroid/build.gradle.kts` reads this value and injects it into
`BuildConfig.DEEPSEEK_API_KEY`. For CI environments, the same build script
falls back to `System.getenv("DEEPSEEK_API_KEY")` when the property is absent
from `local.properties`.

```kotlin
// In Application.onCreate
TranslationSDK.init(
    TranslationSDK.Builder()
        .translationApi(DeepSeekTranslationApi(apiKey = BuildConfig.DEEPSEEK_API_KEY))
        .build()
)
```

### iOS

Copy `iosApp/Secrets.swift.template` to `iosApp/Secrets.swift` (gitignored)
and fill in your key:

```swift
let deepseekApiKey = "YOUR_KEY"
```

Both Xcode targets reference `Secrets.swift`. Do not commit this file.

## Android Integration

Initialize the SDK once in `Application.onCreate()` and close it in
`Application.onTerminate()`. No SDK reference is needed at the widget call site.

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        TranslationSDK.init(
            TranslationSDK.Builder()
                .translationApi(DeepSeekTranslationApi(apiKey = BuildConfig.DEEPSEEK_API_KEY))
                .defaultLanguage("de")
                .build()
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        TranslationSDK.close()
    }
}
```

Render translated text anywhere in your Compose tree:

```kotlin
TranslatedText(
    rawText = "This label can be translated.",
    translationRequired = true,
    targetLanguage = "fr",
    seeTranslationText = "Show French",
    hideTranslationText = "Hide French"
)
```

If `TranslationSDK.init()` has not been called, `TranslatedText` renders the raw
text silently rather than crashing. `TranslationSDK.isInitialized` can be
checked before rendering if explicit guard behavior is needed.

## iOS SwiftUI Integration

The SwiftUI package exposes a native SwiftUI view:

```swift
import TranslationSDK
import TranslationSDKUI
```

### SDK initialization

Initialize the SDK in the Swift `App` entry point using a `StateObject` so
the lifecycle is tied to the app's lifetime:

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

Two important Swift/Kotlin interop notes:

- The Kotlin `init` method is exposed in Swift as `doInit(sdk:)` to avoid
  collision with Swift's initializer keyword.
- Kotlin default parameter values are not bridged to Swift, so all parameters
  (such as `model:`) must be passed explicitly when calling from Swift.

`MainViewController.kt` does not initialize the SDK; it only returns the Compose
UI. SDK initialization always belongs in the Swift `App` entry point.

### Using the widget

Pass the SDK singleton through the translate closure:

```swift
TranslatedText(
    rawText: "This label can be translated.",
    translationRequired: true,
    targetLanguage: "fr",
    seeTranslationText: "Voir la traduction",
    hideTranslationText: "Masquer la traduction",
    translate: { text, targetLanguage, completion in
        TranslationSDK.companion.instance.translate(text: text, targetLanguage: targetLanguage) { result, _ in
            completion(result?.translatedText)
        }
    }
)
```

The SwiftUI widget uses a closure so it can also call a native iOS translation
service directly if the app does not want to route through Kotlin.

## Caching Behavior

`TranslationSDK` checks cache before calling the provider. Cache keys include the
hash and length of the source text combined with the target language. If a cached
translation exists, no provider call is made.

The cache is persistent, not in-memory only. `TranslationCache` stores
JSON-encoded entries through `multiplatform-settings`:

- Android: `SharedPreferencesSettings` backed by the private
  `translation_sdk` `SharedPreferences` file
- iOS: `NSUserDefaultsSettings` backed by `NSUserDefaults.standardUserDefaults`

There is no DataStore, file cache, or in-memory `LruCache` layer in the current
implementation. Cached translations survive app cold starts until they are
cleared, expired by TTL, or invalidated by changing the cache version.

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

TranslationSDK.init(
    TranslationSDK.Builder()
        .translationApi(DeepSeekTranslationApi(apiKey = BuildConfig.DEEPSEEK_API_KEY))
        .errorReporter(
            object : TranslationErrorReporter {
                override fun report(error: TranslationSdkException) {
                    recordTranslationError(error.report)
                }
            }
        )
        .build()
)
```

The shared `TranslationViewModel` does not log failures. Failed requests are
reported by `TranslationSDK` through the configured `TranslationErrorReporter`;
the view model only keeps its in-memory state unchanged when a request fails.

Error reports include:

- error code
- message
- SDK version
- provider name
- HTTP status when available
- target language when available

## Lifecycle

The SDK is a singleton initialized once and closed once at the app level.

```kotlin
// App start
TranslationSDK.init(sdk)

// App teardown
TranslationSDK.close()
```

`TranslationSDK.close()` is safe to call even if `init()` was never called.
It releases the underlying HTTP client owned by the active `TranslationApi`.

Platform-specific coroutine scope teardown:

- Android Compose: `viewModelScope` is cancelled automatically by the Android
  framework when `AndroidTranslationViewModel.onCleared()` fires.
- iOS Compose: the cached `MainScope` is cancelled when the last Compose
  reference to the view model is disposed.
- iOS SwiftUI: the SDK is owned and closed by the `AppSDK` object held in the
  Swift `App` entry point via `@StateObject`.

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

iOS sample targets:

```bash
# Copy the secrets template and fill in your key before building
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

## Current Notes

- The iOS sample links `TranslationSDK.framework` and the native SwiftUI package.
- The Compose sample target links `TranslationSDK.framework`, `ComposeUI.framework`,
  and `ComposeSampleUI.framework`.
- `shared/ios-widget` is a Swift Package and does not depend directly on the
  Kotlin framework.
- SDK initialization for both the SwiftUI and Compose sample targets lives
  exclusively in the Swift `App` entry point. `MainViewController.kt` does not
  perform any SDK initialization.
- `iosApp/Secrets.swift` is gitignored. Copy from `Secrets.swift.template` and
  add the file to both Xcode targets before building.
