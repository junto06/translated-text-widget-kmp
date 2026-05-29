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
- Kotlin default parameter values are NOT bridged to Swift — all parameters must be passed explicitly
- `companion object` functions accessed as `Type.companion.functionName()` in Swift

## Compose
- `remember` keys match the data they depend on
- Side effects use the right effect handler (`LaunchedEffect` vs `DisposableEffect` vs `SideEffect`)
- No heavy work done directly in composition

## Cache / Storage
- Cache keys are stable across app restarts (no memory addresses, no random values)
- `cacheVersion` bumped when cache entry shape changes

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
