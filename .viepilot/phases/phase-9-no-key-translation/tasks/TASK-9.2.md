# Task 9.2 — Introduce engine contracts and capability model

## Objective

Create a small, testable dependency boundary for text translation, speech recognition, and speech output. Wrap the current `Translator`, `Recognizer`, and `TTS` implementations behind that boundary while preserving the exact current runtime selection and credential behavior. This task is architecture-only: it must not activate ML Kit, Android `SpeechRecognizer`, or any new engine in the app's services/UI.

## Allowed application paths

Only the following new files may be created:

- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/EngineType.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/CapabilityState.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/EngineCapability.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/EngineError.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/EngineOperation.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/TextTranslationEngine.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/SpeechRecognitionEngine.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/SpeechOutputEngine.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/EngineFactory.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/EngineFactoryRegistry.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/legacy/LegacyErrorMapper.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/legacy/LegacyCloudTextTranslationEngine.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/legacy/LegacyCloudSpeechRecognitionEngine.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/legacy/AndroidSpeechOutputEngine.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/legacy/LegacyCloudEngineFactory.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/EngineOperationTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/EngineFactoryRegistryTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/legacy/LegacyErrorMapperTest.java`

Do not edit existing application classes in Task 9.2. If an existing class proves impossible to wrap by composition, stop and report the exact blocker; do not widen scope autonomously.

## PM-owned state paths

- `.viepilot/phases/phase-9-no-key-translation/tasks/TASK-9.2.md`
- `.viepilot/phases/phase-9-no-key-translation/PHASE-STATE.md`
- `.viepilot/TRACKER.md`
- `.viepilot/HANDOFF.json`
- `.viepilot/ROADMAP.md`
- `CHANGELOG.md`

Terra must not edit PM-owned state, `.viepilot/debug/`, any Phase 3 execution log, or any task other than 9.2.

## Required contract design

1. `EngineType` distinguishes `ON_DEVICE` from `LEGACY_CLOUD`; it does not itself select a default.
2. `EngineCapability` is an immutable value object backed by `CapabilityState`. It must represent at least: available, temporarily unavailable, setup/download required, unsupported, and closed. Capability/detail values must not contain secrets.
3. `EngineError` is an immutable domain error with a stable category covering at least: cancelled, network, authentication/credential, quota, unsupported language, missing dependency/model, permission, busy, and internal failure. Preserve legacy reason codes/value for compatibility without exposing Cloud classes in the public engine interfaces.
4. `EngineOperation` has thread-safe, idempotent cancellation and terminal completion. Cancellation suppresses late callbacks. For a legacy request that cannot interrupt its underlying network call, clearly document that cancellation is callback cancellation only; do not claim transport cancellation.
5. `TextTranslationEngine` exposes capability, text translation, language detection if supported, supported-language discovery, cancellation handles, and lifecycle close. Its public signatures may use existing neutral domain types such as `CustomLocale`, but not `Translator`, `CloudApiResult`, or Cloud listeners.
6. `SpeechRecognitionEngine` exposes capability, supported languages, bounded recognition lifecycle (`start`, audio input where required, `finish`, `cancel`, `close`) and normalized result/error callbacks. The interface must permit the later Android recognizer to own microphone capture while still permitting the legacy adapter to accept PCM chunks; model this explicitly instead of forcing a fake implementation.
7. `SpeechOutputEngine` exposes capability, language selection/speak, stop, and close without leaking Android `TextToSpeech` constants through the interface.
8. `EngineFactory` creates the three engine interfaces. `EngineFactoryRegistry` performs explicit `EngineType` lookup only: no silent fallback from on-device to Cloud and no process-global mutable singleton.
9. Legacy adapters use composition over the existing `Translator`, `Recognizer`, and `TTS`. They translate callbacks/errors into the new contracts and protect callbacks with `EngineOperation`. They must not copy Cloud implementation logic or fetch credentials themselves.
10. `LegacyCloudEngineFactory` is the only new class allowed to construct these legacy adapters. No existing UI/service is migrated in this task; integration and the default-engine switch remain Task 9.5.

## Best-practice constraints

- Java 8 and API 23 compatibility; no API-level behavior change.
- Prefer final fields, defensive copies for arrays/collections, null validation, and deterministic state transitions.
- No Android `Context`, `Service`, `Handler`, Cloud API class, or `TextToSpeech` type in the five core value/lifecycle classes (`EngineType`, `CapabilityState`, `EngineCapability`, `EngineError`, `EngineOperation`).
- Callbacks must be delivered at most once for one-shot operations, never after cancellation/close, and must not be invoked while holding an internal lock.
- `close()`/`cancel()` must be idempotent. A closed engine reports `CLOSED` and rejects new work with a normalized error.
- Do not swallow exceptions, add blanket lint suppression, add mutable public fields, log recognized/translated user content, or include credentials in logs/errors.
- Do not add dependencies or modify Gradle in this task.

## Required tests

- `EngineOperationTest`: cancellation is idempotent; completion wins only once; cancelled operations reject late completion/callback delivery; cancel action runs at most once.
- `EngineFactoryRegistryTest`: exact explicit selection; duplicate registration is rejected; missing type returns a deterministic failure; no implicit legacy fallback.
- `LegacyErrorMapperTest`: every known `ErrorCodes` value maps to the intended domain category, unknown/empty reasons map to internal failure, and original codes/value are defensively preserved.

Tests must be JVM tests and must not perform network, credential, microphone, Bluetooth, or model-download work.

## Acceptance criteria

- [ ] All three engine contracts and immutable capability/error models compile on the existing Java/Android baseline.
- [ ] Existing Cloud translation/recognition and Android TTS are wrapped by composition behind the new contracts.
- [ ] Factory selection is explicit and has no hidden Cloud fallback.
- [ ] Legacy error mapping and callback-only cancellation semantics are documented and tested.
- [ ] No existing UI/service/runtime consumer changes, no behavior switch, and no credential regression.
- [ ] No dependency, manifest, resource, version, or build-configuration changes.
- [ ] Full unit/lint/debug build passes with zero lint errors and no unexplained warning increase from the 136-warning baseline.

## Verification

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain
```

Additionally record:

```powershell
git diff --check
git status --short
```

## Evidence report required from Terra

- Contract/API summary and why it supports Tasks 9.3–9.5 without switching runtime behavior.
- Exact files changed and `git diff --stat`.
- Unit-test count, lint error/warning count, APK path/size.
- Proof that `app/build.gradle`, manifest/resources, existing consumers, and credential code were untouched.
- Any adapter limitation, especially callback-only cancellation for the legacy network translation path.
- Local implementation commit SHA.

Stop after one local implementation commit and report to PM. Do not push, tag, merge, edit ViePilot state, migrate consumers, or start Task 9.3.
