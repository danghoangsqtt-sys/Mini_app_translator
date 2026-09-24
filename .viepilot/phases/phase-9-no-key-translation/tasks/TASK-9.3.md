# Task 9.3 — Implement ML Kit translation and model management

## Objective

Implement the default on-device text translation boundary with Google ML Kit Translation and bundled ML Kit Language Identification. Translation must identify the source language, normalize it to an ML Kit-supported language, require every non-English model to have been explicitly downloaded, and then translate without credentials. Add a Settings model-management surface for listing, downloading, and deleting models with a Wi-Fi-only default and honest indeterminate progress. Do not integrate the engine into Conversation, WalkieTalkie, recognition, or runtime selection in this task.

## Concrete Outcome

- `MlKitTextTranslationEngine` implements the Task 9.2 `TextTranslationEngine` contract without changing that contract.
- `TranslationModelManager` owns immutable model state, explicit download/delete operations, duplicate-operation suppression, and lifecycle-safe callbacks.
- ML Kit SDK access is isolated behind small callback-based wrappers so mapper, state, cancellation, and session behavior can be tested on the JVM without a device, network, or real model.
- Settings exposes “On-device translation models”, downloaded/offline status, per-language download/delete actions, and an explicit Wi-Fi-only versus any-network choice.
- English is always represented as `BUILT_IN`; it is never passed to an ML Kit remote-model download/delete API.
- Instrumentation covers supported-language enumeration, online model preparation/translation/status, auxiliary model deletion, and a separate translate-without-download path for real offline proof.

## Paths

### Documentation modified before shipping code

- `.viepilot/phases/phase-9-no-key-translation/tasks/TASK-9.3.md`
- `.viepilot/phases/phase-9-no-key-translation/PHASE-STATE.md`

### Application files modified

- `app/build.gradle`
- `app/src/main/java/nie/translator/rtranslatordevedition/settings/SettingsFragment.java`
- `app/src/main/res/xml/preferences.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-it/strings.xml`

### Application files created

- `app/src/main/java/nie/translator/rtranslatordevedition/settings/TranslationModelsPreference.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/AsyncResultCallback.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/MlKitErrorMapper.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/MlKitLanguageMapper.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/MlKitRemoteModelClient.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/MlKitTextTranslationEngine.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/MlKitTranslationClientFactory.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/ModelDownloadPolicy.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/RemoteModelClient.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/TranslationClientFactory.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/TranslationModel.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/TranslationModelManager.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/TranslationModelState.java`
- `app/src/main/res/layout/dialog_translation_models.xml`
- `app/src/main/res/layout/component_row_translation_model.xml`

### Test files created

- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/MlKitErrorMapperTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/MlKitLanguageMapperTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/MlKitTextTranslationEngineTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/TranslationModelManagerTest.java`
- `app/src/androidTest/java/nie/translator/rtranslatordevedition/voice_translation/engines/translation/MlKitTranslationInstrumentedTest.java`

No other file may be created or modified without stopping for PM approval.

## File-Level Plan

- `app/build.gradle`: add only the officially documented `com.google.mlkit:translate:17.0.3` and bundled `com.google.mlkit:language-id:17.0.6` dependencies. Do not add Google Services, Firebase, credentials, repositories, or version changes.
- `AsyncResultCallback.java`: define the minimal success/failure callback shared by test seams; never expose a raw Google `Task` to core lifecycle logic.
- `MlKitLanguageMapper.java`: snapshot `TranslateLanguage.getAllLanguages()`, normalize BCP-47/`CustomLocale` values to supported tags, lower country-specific locales to a supported language tag, reject null/blank/`und`/unsupported values, and return immutable supported-language collections.
- `MlKitErrorMapper.java`: map stable ML Kit error codes to sanitized `EngineError` categories without copying exception text or user input into details.
- `RemoteModelClient.java`: define the testable model-store boundary for availability, downloaded-model listing, download, and deletion.
- `MlKitRemoteModelClient.java`: adapt `RemoteModelManager.isModelDownloaded()`, `getDownloadedModels()`, `download()`, and `deleteDownloadedModel()`; build `DownloadConditions` from the explicit policy; reject English before constructing `TranslateRemoteModel`; return defensive immutable sets.
- `TranslationClientFactory.java`: define closeable language-identifier and translator clients whose asynchronous results are fakeable in JVM tests.
- `MlKitTranslationClientFactory.java`: adapt `LanguageIdentification.getClient()` and `Translation.getClient(TranslatorOptions)`; attach listeners without logging input/output; close each ML Kit client exactly once.
- `ModelDownloadPolicy.java`: define `WIFI_ONLY` and `ANY_NETWORK`, with `WIFI_ONLY` as the caller/UI default.
- `TranslationModelState.java`: define the immutable public state vocabulary `BUILT_IN`, `NOT_DOWNLOADED`, `QUEUED`, `DOWNLOADING`, `DOWNLOADED`, `DELETING`, and `FAILED`.
- `TranslationModel.java`: hold one immutable language-tag/state/error snapshot; validate constructor input and expose no mutable collection or array.
- `TranslationModelManager.java`: list/refresh model state, publish immutable snapshots outside locks, suppress duplicate same-language work, keep distinct download/delete operation identities, map late callbacks to the initiating operation only, make cancellation callback-only because Google `Task` has no transport cancellation here, reject English download/delete, and close idempotently.
- `MlKitTextTranslationEngine.java`: implement source detection → tag normalization → required non-English model checks → translator creation → translation. Source-equals-target returns the original text after detection without model access. Only one translate session is active; overlap is rejected as `BUSY`. Cancel/close detaches the exact session, cancels pending wrapper operations logically, closes identifier/translator clients once, and suppresses late callbacks.
- `TranslationModelsPreference.java`: render the model dialog, persist an explicit network-policy choice with Wi-Fi-only default, show all supported languages and their states, provide per-row download/delete actions, show only indeterminate progress, display recoverable errors in-place, disable English actions, and detach/close callbacks when the preference/Fragment UI is destroyed.
- `SettingsFragment.java`: initialize the model preference and close/detach it in `onDestroyView()` without retaining an Activity context beyond the view lifecycle.
- `preferences.xml`: add the “On-device translation models” entry without adding a runtime-engine selector.
- `strings.xml` and `values-it/strings.xml`: add complete English/Italian titles, disclosures, states, actions, policy labels, and recoverable errors. Copy must say models are stored on-device, work offline after download, quality varies by language, and translation uses Google ML Kit.
- `dialog_translation_models.xml`: provide disclosure text, network policy controls, list/empty/error areas, and an indeterminate operation indicator; no deprecated `ProgressDialog`.
- `component_row_translation_model.xml`: provide language, state, indeterminate row progress, and a localized download/delete action with accessible touch targets.
- `MlKitLanguageMapperTest.java`: cover supported tags, country-specific fallback, `CustomLocale`, `und`, null/blank, unsupported tags, immutable results, and English mapping.
- `MlKitErrorMapperTest.java`: cover missing model, network, unsupported, cancelled, resource/busy, and unknown mappings with sanitized details.
- `TranslationModelManagerTest.java`: cover English built-in behavior, every state transition, duplicate-download suppression, Wi-Fi-only/any-network forwarding, distinct delete identity, immutable snapshots, cancellation/close late-callback suppression, and no callback/API call while holding manager locks.
- `MlKitTextTranslationEngineTest.java`: cover detection flow, missing required source/target models, source-equals-target bypass, unsupported/`und`, busy rejection, stale-session isolation, cancel/close late-callback suppression, and exactly-once client closure.
- `MlKitTranslationInstrumentedTest.java`: use actual ML Kit APIs to list supported languages; online, download Italian and translate a short stable English phrase; confirm `isModelDownloaded`; download/delete an auxiliary non-English model and confirm deletion; separately translate with the already-downloaded Italian model without invoking download so adb-controlled offline execution can prove persistence.

## Best Practices and Invariants

- API 23 remains supported; Java source/target remain 8.
- Do not change `TextTranslationEngine`, engine registry, factory composition, Conversation, WalkieTalkie, Recognizer, credential handling, package/application ID, or app version.
- Use only documented Google ML Kit Android APIs. No HTML scraping, unofficial Translate endpoint, Cloud key, service account, Google Services plugin, or Firebase.
- Never log source text, translated text, detected content, credentials, or raw exception messages that could contain user content.
- Do not invoke user callbacks or Google APIs while holding an internal lock.
- All public collections are immutable snapshots or defensive copies.
- ML Kit exposes completion but no reliable byte/percentage progress for translation model downloads; UI progress is therefore indeterminate only.
- The bundled language-identification dependency is selected so source detection is immediately available and does not introduce another user-managed download.

## Lifecycle and Cancellation Rules

1. Every public asynchronous operation has a unique `EngineOperation`; model downloads and deletes additionally carry a monotonic manager operation identity.
2. Cancellation removes the operation from callback eligibility. It does not claim to cancel the underlying ML Kit `Task`; that transport limitation is documented in code and UI behavior.
3. A callback is accepted only when the manager/engine is open, the operation is active, and its identity still matches the currently registered operation/session.
4. Internal locks protect state/identity only. Client API calls and user callbacks run after releasing locks.
5. Each request owns its `LanguageIdentifier` and, when needed, its `Translator`. Both are closed once on success, failure, cancellation, or engine close.
6. Engine/manager close is idempotent, cancels active logical operations, clears observers, and prevents all later callbacks.
7. Settings view destruction dismisses the dialog, clears UI observers, and closes the preference-owned manager so no Activity/view reference is reached by late callbacks.

## Model State Machine

```text
English: BUILT_IN (terminal for management; download/delete rejected)

refresh: NOT_DOWNLOADED <-> DOWNLOADED
download: NOT_DOWNLOADED|FAILED -> QUEUED -> DOWNLOADING -> DOWNLOADED
                                      \-> FAILED
delete:   DOWNLOADED|FAILED -> DELETING -> NOT_DOWNLOADED
                                  \-> FAILED
cancel/close: underlying Task may continue, but no further public state/callback is emitted
```

`QUEUED` means the download request has been accepted and conditions are being applied. `DOWNLOADING` means ML Kit has returned an active download task; it is not a byte-level progress claim. Duplicate same-language operations are rejected deterministically and never start a second ML Kit call.

## Metered-Network Policy

- `WIFI_ONLY` is the persisted/default choice and maps to `new DownloadConditions.Builder().requireWifi().build()`.
- `ANY_NETWORK` maps to `new DownloadConditions.Builder().build()` only after the user explicitly selects the any-network radio option.
- `translate()` never downloads a model. Any absent required model returns `MISSING_DEPENDENCY` and leaves download initiation to the Settings UI.

## Verification

### JVM

```powershell
.\gradlew.bat testDebugUnitTest --console=plain --no-daemon
```

Expected: all existing 66 tests plus the new mapper/error/model-manager/engine cases pass; no sleep-, network-, microphone-, Bluetooth-, credential-, or real-model dependency.

### Static/build gate

```powershell
git diff --check
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon
```

Expected: build succeeds, lint has 0 errors and no more than the 136-warning baseline unless PM approves a documented exception, and `app/build/outputs/apk/debug/app-debug.apk` exists.

### Instrumentation and manual model UX

```powershell
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
```

When an emulator/device is connected, expected: supported-language listing, real non-English download, short meaningful translation, downloaded-state check, and safe auxiliary-model deletion pass. Manually open Settings and confirm the Wi-Fi-only default, explicit any-network selection, indeterminate progress, recoverable error display, offline disclosure, and disabled English actions.

### Offline device proof

1. Online: run the instrumentation preparation path for English → Italian, record the pair/result, and confirm Italian is downloaded.
2. Force-stop the app/test process.
3. Disable emulator/device network using an adb-supported method and independently confirm it is offline.
4. Run only the translate-without-download instrumentation path; record success and verify logs show no failed network request.
5. Restore network in a `finally`-style cleanup even if the proof fails.

If a real emulator/device or verifiable network isolation is unavailable, report Task 9.3 as `PARTIAL`; unit/instrumentation simulation cannot substitute for this proof.

## Forbidden Changes

- No `translate.google.com/m`, HTML scraping, unofficial API, copied AGPL code from `D:\DataAdmin\vp-pdf`, API key, Cloud credential, or shared secret.
- No edits to `api_management/`, credential flows, legacy Cloud implementation, ConversationService, WalkieTalkieService, RecognizerService, Task 9.4–9.7, Phase 3 execution logs, or `.viepilot/debug/`.
- No runtime ON_DEVICE registration, composite `EngineFactory`, speech-recognition stub/null, or runtime engine selector.
- No Google Services Gradle plugin, Firebase, package/application ID change, version bump, blanket lint suppression, existing-test modification/removal, push, tag, merge, rebase, or history rewrite.
- Do not mark Task 9.3 `done` or `PASS` in this delivery.

## Required Evidence

- Gate 1 HEAD/origin/status/divergence output.
- Official Google documentation links and the verified dependency versions.
- Documentation commit SHA and implementation commit SHA.
- Exact changed-file list and `git diff --stat`.
- Architecture explanation for identification → model checks → translation, state machine/network policy, duplicate/session isolation, and client closure.
- JVM and instrumentation test counts/results; lint errors/warnings; APK path/size.
- Online download and genuine offline translation proof, including network disable/restore method, or an explicit `PARTIAL` limitation.
- Grep/diff proof that forbidden endpoints, credentials, version/application ID, Conversation/WalkieTalkie/runtime selection, and out-of-scope files were not changed.

## Acceptance Criteria

- [ ] Uses official ML Kit Translation and Language Identification dependencies and supported APIs.
- [ ] No API key or Cloud project is required.
- [ ] Missing model is a recoverable state with explicit user action and honest indeterminate progress.
- [ ] Downloaded model translates a supported pair while the device is genuinely offline.
- [ ] Translators/identifiers close exactly once; concurrent/cancelled work cannot leak callbacks or cross sessions.
- [ ] Required on-device/offline/quality/Google ML Kit disclosures are localized in English and Italian.
- [ ] JVM, instrumentation (when a device exists), lint, APK, scope, and security gates are recorded for PM review.
- [ ] Task remains `in_progress`/not PASS until PM reviews the diff and evidence.
