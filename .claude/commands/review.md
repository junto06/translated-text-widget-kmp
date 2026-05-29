---
description: Code review for TranslatedText Widget — KMP, Compose, Swift
argument-hint: "[file or PR description]"
---

You are reviewing code for a Kotlin Multiplatform SDK (TranslatedText Widget) targeting Android and iOS. Review the staged/unstaged changes or the file passed as $ARGUMENTS.

Start by running:
```
!`git diff HEAD`
```

Then review against these criteria:

## Correctness
- No logic errors or off-by-one issues
- Coroutine launch blocks have `catch` for exceptions — uncaught exceptions in `coroutineScope.launch` crash Android
- State flows emit correct initial values
- `LaunchedEffect` keys are stable and correct

## KMP / Kotlin
- `expect/actual` pairs are complete for all targets (androidMain, iosMain)
- No Android-only imports in `commonMain`
- Shared code does not reference platform types directly
- `TranslationSDK.isInitialized` checked before accessing `instance`

## Swift Interop
- Kotlin `init` on companion objects is exposed as `doInit` in Swift — not called as `init`
- Kotlin default parameter values are NOT bridged to Swift — all parameters must be passed explicitly from Swift call sites. This rule applies to Kotlin code only; Swift default parameter values (`= nil`, `= "en"`) are perfectly valid in Swift files.
- `companion object` functions accessed as `Type.companion.functionName()` in Swift
- Kotlin non-null `String` maps to Swift `String` (not `String?`) — no nil crash risk on non-null Kotlin properties

## Compose
- `remember` keys match the data they depend on
- Side effects use the right effect handler (`LaunchedEffect` vs `DisposableEffect` vs `SideEffect`)
- No heavy work done directly in composition
- Use `remember(key)` for values derived from parameters — `derivedStateOf` is for values derived from Compose `State` objects, not from parameters

## Cache / Storage
- There are two independent cache layers — do not confuse them:
  - **In-memory**: `TranslationViewModel._translations` (a `Map<String, String>` StateFlow). Key format: `"$targetLanguage:${rawText.hashCode()}_${rawText.length}"`. Rebuilt fresh on every app start — `String.hashCode()` is stable and content-based on both JVM and Kotlin/Native for strings, making it safe as an in-memory key.
  - **Persistent**: `TranslationCache` (SharedPreferences on Android, NSUserDefaults on iOS). Key format: `"translation_${lang}_${stableHash(text)}"` using a custom FNV-64 hash — always stable across restarts. The in-memory `hashCode()` never touches persistent storage.
- `cacheVersion` bumped only when the **persistent** cache entry shape (the stored JSON fields) changes — not when the in-memory key format changes

## Exception Handling
- `TranslationViewModel.translate()` and `forceTranslate()` already contain `catch (_: Exception) {}` inside the coroutine — they cannot surface exceptions to callers or crash the app. Do not flag `LaunchedEffect { viewModel.translate(...) }` as missing a try-catch.
- `TranslationSDK.isInitialized` is always checked at the top of `TranslatedText` before `instance` is accessed — do not flag subsequent `instance` accesses in the same scope as unguarded.

## Swift Thread Safety
- SwiftUI `@State` mutations and `onAppear` both execute on the main thread. Guards like `guard translatedText == nil, !isLoading` are always evaluated serially — there is no concurrent access risk.

## Security
- No API keys hardcoded in committed files
- Keys come from `BuildConfig`, `local.properties`, `Secrets.swift`, or environment variables

## API / Provider
- `translateBatch` returns exactly one `TranslationItem` per input text, in the same order
- HTTP status checked with `isSuccess()` before deserializing the response body
- `close()` releases HTTP client resources

## General
- No unused imports or dead code
- Public API changes are reflected in CHANGELOG.md under the correct version
- Breaking changes bump the minor version (x.Y.0), patches bump the patch (x.y.Z)

After reviewing, provide:
1. **Summary** — what the change does
2. **Issues** — bugs, risks, or violations of the above (with file:line references)
3. **Suggestions** — non-blocking improvements
4. **Verdict** — Approve / Request Changes
