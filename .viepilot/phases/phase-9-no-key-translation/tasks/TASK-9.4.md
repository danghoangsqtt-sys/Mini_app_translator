# Task 9.4 — Implement lifecycle-safe Android SpeechRecognizer engine

**Status**: in_progress — contract locked by PM on 2026-09-24; assigned to TERRA 5.6

## Objective

Implement the Task 9.2 `SpeechRecognitionEngine` boundary with the official Android `SpeechRecognizer` API. Each request is one bounded microphone-owned utterance. Prefer the API 31+ on-device recognizer when the platform reports it available; otherwise use the system recognizer with an honest capability disclosure that it may require or use network access. This task creates and tests the engine only: runtime registration and Conversation/WalkieTalkie integration remain Task 9.5.

## Concrete Outcome

- `AndroidSpeechRecognitionEngine` implements the existing contract without changing it and accepts only `AudioInputMode.ENGINE_CAPTURE`.
- A small client/factory seam isolates Android framework calls so session, timeout, fallback, cancellation, error mapping, and exact-once cleanup are covered by deterministic JVM tests.
- Every `SpeechRecognizer` create/listen/stop/cancel/destroy call is marshalled to the application main thread.
- One recognizer instance belongs to one generation/session and is destroyed exactly once on final result, error, cancellation, timeout, failed startup, or engine close.
- Recognition is one-shot and bounded by a 30-second engine watchdog. There is no automatic restart or continuous-listening loop.
- API 31-only availability/construction calls are isolated behind an SDK guard. On-device construction may fall back once to the system recognizer only before listening starts; an active/failed recognition session is never silently retried.
- The manifest declares Android 11+ package visibility for `android.speech.RecognitionService`.

## Paths

### Documentation modified before shipping code

- `.viepilot/phases/phase-9-no-key-translation/tasks/TASK-9.4.md`
- `.viepilot/phases/phase-9-no-key-translation/PHASE-STATE.md`
- `.viepilot/TRACKER.md`
- `.viepilot/HANDOFF.json`
- `.viepilot/ROADMAP.md`

### Application file modified

- `app/src/main/AndroidManifest.xml`

### Application files created

- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/MainThreadScheduler.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/AndroidMainThreadScheduler.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/SpeechRecognizerClient.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/SpeechRecognizerClientFactory.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/AndroidSpeechRecognizerClient.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/AndroidSpeechRecognizerClientFactory.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/AndroidSpeechErrorMapper.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/AndroidSpeechRecognitionEngine.java`

### Test files created

- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/AndroidSpeechErrorMapperTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/AndroidSpeechRecognitionEngineTest.java`
- `app/src/androidTest/java/nie/translator/rtranslatordevedition/voice_translation/engines/speech/AndroidSpeechRecognitionInstrumentedTest.java`

No other file may be created or modified without stopping for PM approval.

## File-Level Plan

- `AndroidManifest.xml`: add only a top-level `<queries>` entry for the `android.speech.RecognitionService` action, as required for apps targeting Android 11+; do not change permissions, services, activities, package identity, or backup policy.
- `MainThreadScheduler.java`: define the minimal test seam for immediate main-thread execution, delayed watchdog scheduling, and callback removal. It must not retain an Activity or Service.
- `AndroidMainThreadScheduler.java`: implement that seam with `Handler(Looper.getMainLooper())`; run immediately when already on the main thread, post otherwise, and support removing the exact timeout runnable.
- `SpeechRecognizerClient.java`: define a package-local wrapper contract for start, stop, cancel, destroy, and partial/final/error events. It must expose no app callback and own no cross-session state.
- `SpeechRecognizerClientFactory.java`: define availability and construction methods for on-device and system clients so JVM tests can force every API/fallback path without a real microphone or recognizer service.
- `AndroidSpeechRecognizerClient.java`: adapt one platform `SpeechRecognizer`; install its `RecognitionListener` before listening; build `ACTION_RECOGNIZE_SPEECH` with free-form language model, the request BCP-47 tag, partial results enabled, one maximum result, and `EXTRA_PREFER_OFFLINE=true`; parse the first hypothesis and matching confidence defensively; make destroy idempotent; never log recognized text.
- `AndroidSpeechRecognizerClientFactory.java`: use the application context only; check `isRecognitionAvailable` for the system service; on API 31+ check `isOnDeviceRecognitionAvailable` before `createOnDeviceSpeechRecognizer`; keep every API 31 symbol behind a real SDK guard/helper; construct clients only on the main thread.
- `AndroidSpeechErrorMapper.java`: map every documented recognizer error to a stable sanitized `EngineError`, preserving the platform integer in `legacyReasons` but never exception text or recognized content. Network/server errors map to `NETWORK`; permission to `PERMISSION`; busy/too-many-requests to `BUSY`; unsupported/unavailable language to `UNSUPPORTED_LANGUAGE`/`MISSING_DEPENDENCY`; unsupported support/download operations to `UNSUPPORTED`; audio/client/disconnect/no-match/speech-timeout/unknown failures to recoverable `INTERNAL_FAILURE` details.
- `AndroidSpeechRecognitionEngine.java`: implement one active generation at a time; reject `PCM_STREAM`, closed, unavailable, and overlap cases deterministically; marshal all client operations and callbacks through the scheduler; prefer on-device construction and permit one pre-listening construction fallback to system; never retry after listening starts; deliver partial callbacks with `EngineOperation.dispatch`, final/error with `complete`; detach the exact session before user callbacks; cancel the exact watchdog; cancel/destroy exactly once; make `finish`, operation cancellation, and `close` idempotent and race-safe. `submitAudio` always returns false because this engine owns microphone capture. `getSupportedLanguages()` returns an immutable empty list because the cross-version platform API has no truthful synchronous supported-language enumeration; per-language failures remain recoverable. Capability detail must distinguish on-device availability from the system fallback and explicitly say the fallback may use network.
- `AndroidSpeechErrorMapperTest.java`: cover all public `SpeechRecognizer` error codes supported by compileSdk 36, unknown codes, stable categories/details, and defensive preservation of the raw code.
- `AndroidSpeechRecognitionEngineTest.java`: use fake scheduler/factory/client objects to cover on-device preference, API/availability fallback, on-device construction failure before listening, no fallback after listening, absent service, `PCM_STREAM` rejection, main-thread scheduling, request forwarding, partial/final delivery, empty results, confidence handling, overlap rejection, stale-generation isolation, `finish` exact-once stop, operation cancellation, 30-second timeout, close-before-start, close-during-session, late callback suppression, callback-outside-lock behavior, and exact-once cancel/destroy cleanup.
- `AndroidSpeechRecognitionInstrumentedTest.java`: on the connected API 36 target, verify capability probing does not crash, client construction/destruction occurs on the main thread when a recognizer is available, intent extras contain the requested BCP-47 language and offline preference, and immediate cancel/close leaves no callback or lifecycle crash. Use assumptions for a genuinely absent system recognizer; do not claim microphone transcription quality or offline recognition from a synthetic test.

## Platform and Behavior Contract

1. Android documents that every `SpeechRecognizer` method must run on the main application thread and every instance must be destroyed. The engine enforces both even when public contract methods are called from worker threads.
2. API 31+ on-device availability is checked before on-device construction. API 23–30 must never verify/load/call an API 31 method.
3. System-recognizer fallback is not described as offline. `EXTRA_PREFER_OFFLINE` is only a preference and may be ignored by the installed recognizer implementation.
4. One start produces at most one terminal callback. Partial results may occur zero or more times before the terminal event and are ignored after cancel, timeout, close, or generation replacement.
5. The 30-second watchdog bounds a session even when the recognizer service never calls back. Natural endpointer completion or `finish()` may end it earlier.
6. `finish(operation)` calls `stopListening()` once and waits for the matching final/error callback or watchdog. It does not destroy early and does not start a replacement session.
7. Operation cancellation and engine close call `cancel()` then `destroy()` on the owned client once. Terminal success/error destroys without a redundant cancel.
8. A failure to create the preferred on-device client may use the system client only if the system service was already reported available and listening has not started. Runtime error callbacks never trigger hidden retries or network escalation.
9. User callbacks run on the main thread, outside engine locks, and only while the same session identity and `EngineOperation` remain active.
10. No recognized text, language utterance, raw `Bundle`, exception message, or microphone data may be logged.

## Best Practices and Invariants

- API 23 remains supported; Java source/target remain 8.
- Use the application context only; never retain Activity, Fragment, View, or Service references.
- Prefer final fields, immutable/defensive collections, explicit session identity, and small package-private test seams.
- Do not invoke platform methods or user callbacks while holding the engine lock.
- Catch only expected platform/runtime failures at the boundary, map them to sanitized domain errors, and clean up before callback delivery.
- No blanket lint suppression. If a targeted API annotation/suppression is demonstrably required after a real SDK guard, scope and explain it narrowly.
- Do not add dependencies, resources, UI strings, model-download UI, runtime selectors, analytics, logs, credentials, or new permissions.

## Verification

### JVM and static/build gate

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon
git diff --check
```

Expected: all existing 99 JVM tests plus the new engine/error-mapper cases pass; lint reports 0 errors and no unexplained increase over the 136-warning baseline; `app/build/outputs/apk/debug/app-debug.apk` exists.

### Instrumentation

```powershell
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
```

Expected on the available Pixel 7a API 36 emulator: all existing 10 Phase 9 tests and the new SpeechRecognizer lifecycle/intent smoke cases pass. A recognizer-absent assumption is acceptable only for the platform-construction subcase and must be reported explicitly; JVM absence/recovery tests still must pass.

### Scope and safety proof

```powershell
git status --short
git diff --name-status <planning-commit>..HEAD
git diff --stat <planning-commit>..HEAD
git diff <planning-commit>..HEAD -- app/build.gradle app/src/main/res app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode app/src/main/java/nie/translator/rtranslatordevedition/api_management
```

Expected: only the one manifest file and the explicitly listed speech engine/tests changed; the final scoped diff command is empty.

## Forbidden Changes

- No edits to the Task 9.2 contracts/core value objects, engine registry, `EngineFactory`, legacy adapters, ML Kit translation package, Settings/UI/resources, Gradle/dependencies, credentials, `api_management`, Conversation, WalkieTalkie, recognizer services, package/application ID, app version, or Phase 3 logs.
- No runtime ON_DEVICE factory registration or behavior switch; that is Task 9.5.
- No automatic recognition loop, segmented session, background retry, model-download trigger, API 33 support-query feature, or automatic retry from an active on-device request to a potentially network-backed system request.
- No fake supported-language list, no promise that the fallback is offline, and no claim that emulator construction proves recognition quality or offline speech.
- No modification/removal of existing tests, blanket lint suppression, push, tag, merge, rebase, history rewrite, release action, worktree creation/deletion, or auxiliary `qa-*`/`qa-gradle-*` directory creation.
- Do not touch `.viepilot/debug/` or modify PM-owned ViePilot state after the planning checkpoint.
- Do not mark Task 9.4 `done` or `PASS` in this delivery.

## Required Evidence from Terra

- Gate 0 output: exact HEAD, `origin/master`, upstream, status, and ahead/behind counts before edits. Stop if HEAD is not the PM planning commit or if any tracked residue exists.
- Official Android documentation links used for `SpeechRecognizer`, `RecognizerIntent.EXTRA_PREFER_OFFLINE`, `RecognitionListener`, and Android 11 package visibility.
- Implementation commit SHA, exact changed-file list, `git diff --stat`, and proof there is exactly one local implementation commit above the planning baseline.
- Architecture summary covering main-thread dispatch, session identity, watchdog, fallback boundary, result parsing, error mapping, and exact-once cleanup.
- JVM test count/result; instrumentation count/result and any assumption/skip; lint errors/warnings; APK path/size/SHA-256.
- Scope proof that Gradle, resources, services/UI, credentials, ML Kit code, engine contracts, app identity/version, and PM state were untouched.
- Explicit limitations: system fallback may use network; offline preference can be ignored; no synchronous truthful supported-language enumeration; no runtime integration until Task 9.5.

## Acceptance Criteria

- [ ] Existing `SpeechRecognitionEngine` is implemented for bounded engine-owned microphone capture without changing the contract.
- [ ] API 31-only calls are guarded and API 23 remains build/runtime compatible.
- [ ] On-device recognition is preferred only when reported available; safe pre-listening system fallback is truthful and tested.
- [ ] All platform calls and public callbacks occur on the main thread; session generations suppress stale/late callbacks.
- [ ] Final/error/cancel/timeout/close paths cancel the watchdog and destroy the owned recognizer exactly once.
- [ ] Permission denial, recognizer absence, busy state, unsupported/unavailable language, no match, timeout, and unknown errors are recoverable normalized outcomes.
- [ ] No indefinite or automatically restarted recognition loop exists; every session is capped at 30 seconds.
- [ ] Manifest package visibility, JVM coverage, API 36 instrumentation smoke, lint, APK, scope, and security evidence are recorded.
- [ ] No runtime consumer/default-engine switch, UI/resource/dependency/version/credential change, or out-of-scope filesystem artifact is introduced.
- [ ] PM reviews the diff and evidence before marking the task PASS.
