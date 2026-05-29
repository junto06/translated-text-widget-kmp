# Changelog

## [0.2.0] - 2026-05-29

### Added
- **Translation prefetch** — `TranslatedText` now starts translating immediately on composition/appearance so the result is ready when the user taps, rather than fetching on demand.
- **SDK default language fallback** — `targetLanguage` on `TranslatedText` is now optional (`null` / `nil`). When omitted, the SDK's configured `defaultLanguage` is used instead of hardcoding `"en"`.
- `TranslationSDK.defaultLanguage` public property to read the configured default language.

### Fixed
- `targetLanguage = "en"` hardcoded default on the widget overriding the SDK-level `defaultLanguage` setting.

---

## [0.1.1] - 2026-05-27

### Added
- **Built-in LLM providers** in `shared/core` (`com.sdk.translation.api`):
  - `ChatCompletionsTranslationApi` — abstract base for any OpenAI-compatible endpoint
  - `OpenAiTranslationApi(apiKey, model = "gpt-4o-mini")`
  - `DeepSeekTranslationApi(apiKey, model = "deepseek-chat")`
- `TranslationSDK` singleton (`init` / `close` / `isInitialized` / `instance`) — SDK no longer needs to be passed as a parameter to each `TranslatedText` widget.
- `context(...)` on `TranslationSDK.Builder` — Android uses `SharedPreferencesSettings`; falls back to `InMemorySettings` when context is null.
- `TranslationErrorReporter` and `errorReporter(...)` on the Builder for receiving SDK errors.
- `forceTranslate` on `TranslationViewModel` to overwrite a cached entry.
- `translateBatch` and `hasTranslation` helpers on `TranslationViewModel`.
- **Secret management** for sample apps:
  - Android: `local.properties` → `BuildConfig.DEEPSEEK_API_KEY` with `DEEPSEEK_API_KEY` environment variable as CI fallback.
  - iOS: `iosApp/Secrets.swift` (gitignored) with `Secrets.swift.template` committed as reference.

### Changed
- `OpenAiTranslationApi` and `DeepSeekTranslationApi` moved from `sampleAndroid` into `shared/core` so both Android and iOS can use them without duplication.
- iOS SDK initialization moved entirely into the Swift `App` entry point (`AppSDK` / `@StateObject`). `MainViewController.kt` no longer initializes the SDK — it only returns the Compose UI.
- iOS SwiftUI `TranslatedText` translate closure now uses `TranslationSDK.companion.instance` (singleton) instead of a locally held `sdk` reference.
- `TranslatedText` composable no longer accepts `sdk: TranslationSDK` as a parameter — uses the singleton internally.
- Cache key changed from raw text to `"$targetLanguage:${rawText.hashCode()}_${rawText.length}"` to avoid collisions and reduce key length.
- `Platform.kt` renamed to `Settings.kt`; actuals renamed accordingly.
- `rememberTranslationSDK()` removed — no longer needed with singleton pattern.
- `TranslationViewModel` no longer implements `Closeable`; `TranslationSDK` close responsibility belongs to the app level.
- `sampleAndroid` no longer declares its own Ktor or serialization dependencies — provided transitively by `:shared:core`.

### Fixed
- Uncaught `TranslationApiException` crashing Android when HTTP error occurs inside `coroutineScope.launch` — added `catch` in both `translate()` and `forceTranslate()`.
- HTTP error response deserialized as `ChatResponse` before status check — now checks `isSuccess()` first and throws `TranslationApiException` with body text.
- `InternalSdk::sdk.isInitialized` not accessible from companion — fixed by adding `val isInitialized` inside `InternalSdk` object.

---

## [0.1.0] - 2026-05-26

### Added
- Initial release.
- `TranslationSDK` with `GoogleTranslateApi` built-in provider.
- `TranslationApi` interface for custom providers.
- `TranslationCache` with persistent storage via `multiplatform-settings` (SharedPreferences on Android, NSUserDefaults on iOS).
- Cache TTL and cache version invalidation via `Builder`.
- `TranslatedText` Compose widget (`shared/compose-ui`) for Android and Compose Multiplatform on iOS.
- Native SwiftUI `TranslatedText` widget (`shared/ios-widget`) as a Swift Package (`TranslationSDKUI`).
- `sampleAndroid` — Android sample app.
- `iosApp` — iOS sample with SwiftUI native widget and Compose Multiplatform tab.
- `sampleIosCompose` — standalone iOS Compose sample target.
