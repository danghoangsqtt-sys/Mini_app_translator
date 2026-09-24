# Task 9.5 — Integrate no-key engines into Conversation and WalkieTalkie

**Status**: in_progress — strict contract locked by PM on 2026-09-24; assigned to TERRA 5.6

## Objective

Make `EngineType.ON_DEVICE` the default runtime path for Conversation and WalkieTalkie without requiring a Google Cloud credential. Each microphone tap starts one bounded Android `SpeechRecognizer` turn; completion, cancellation, error, language change, mute, or service teardown ends that turn without automatic restart. Conversation preserves the existing Bluetooth wire format. WalkieTalkie replaces the unsafe two-recognizer PCM fan-out with an explicit source-language direction selected by the user for each turn.

Legacy Cloud classes and recognizer services remain present but dormant. Their explicit opt-in/migration UX belongs to Task 9.6.

## Locked PM Decisions

1. **WalkieTalkie direction**: use an explicit two-option source-language toggle. The selected side is the recognition language; the other side is the translation target. Never run two recognizers or claim automatic bilingual recognition.
2. **Turn behavior**: the mic is push-once, one bounded utterance per explicit user action. A terminal callback returns the mic UI to idle. There is no continuous or automatic recognition restart.
3. **Language catalog**: expose ML Kit translation-supported locales as candidate languages. Do not claim they are a synchronous SpeechRecognizer support intersection; actual recognizer failure is recoverable at turn start/runtime.
4. **Missing models**: do not download automatically. Fail the affected operation with actionable model-management guidance while leaving the activity/service alive and retryable.
5. **Speech output completion**: retain the existing single `TTS` instance and `UtteranceProgressListener` as the utterance-completion seam. Do not change the Task 9.2 `SpeechOutputEngine` contract, whose success means queue acceptance only.
6. **Scope approval**: `Global.java`, stable app error mapping, localized strings, and the WalkieTalkie layout are allowed because they are required to remove the default Cloud dependency and expose an honest direction choice.

## Concrete Outcome

- `Global` no longer constructs a Cloud `Translator` on application startup or calls Cloud to populate the default language list.
- Each running mode owns one service-scoped ON_DEVICE engine family: one Android speech-recognition engine, one ML Kit translation engine, and one Android speech-output adapter wrapping the base service's single `TTS` instance.
- Conversation uses the device user's language for recognition. Partial text is preview-only; only the current final result is sent over Bluetooth.
- Incoming Conversation messages are decoded with the existing framing, translated to the local language through ML Kit, then shown/spoken only if their generation remains current.
- WalkieTalkie exposes and restores an explicit source-language direction, recognizes only that language, translates to the opposite language, and never binds/fans PCM into the two legacy recognizer services on the default path.
- Typed text remains supported: Conversation sends it with the local language; WalkieTalkie detects its language with ML Kit and translates to the opposite selected language when supported.
- Stop, mute, terminal result, language/direction change, replacement, and destroy cancel the relevant operations; stale callbacks cannot update UI, send Bluetooth payloads, start translation, or speak.
- Engine failures become stable recoverable service/UI outcomes. The default path never opens a Google Cloud key dialog.

## Paths

### Documentation modified before shipping code

- `.viepilot/phases/phase-9-no-key-translation/tasks/TASK-9.5.md`
- `.viepilot/phases/phase-9-no-key-translation/PHASE-STATE.md`
- `.viepilot/TRACKER.md`
- `.viepilot/HANDOFF.json`
- `.viepilot/ROADMAP.md`

### Application files modified

- `app/src/main/java/nie/translator/rtranslatordevedition/Global.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/tools/ErrorCodes.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieFragment.java`
- `app/src/main/res/layout/fragment_walkie_talkie.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-it/strings.xml`

### Application files created

- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/ondevice/OnDeviceEngineFactory.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/EngineServiceErrorMapper.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/engines/EngineTurnCoordinator.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationPayloadCodec.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationOnDeviceController.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieOnDeviceController.java`

### Test files created

- `app/src/test/java/nie/translator/rtranslatordevedition/GlobalOnDeviceLanguagesTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/ondevice/OnDeviceEngineFactoryTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/EngineServiceErrorMapperTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/engines/EngineTurnCoordinatorTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationPayloadCodecTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationOnDeviceControllerTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieOnDeviceControllerTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/OnDeviceDefaultSourceTest.java`
- `app/src/androidTest/java/nie/translator/rtranslatordevedition/voice_translation/OnDeviceModeInstrumentedTest.java`

No other file may be created or modified without stopping for PM approval.

## File-Level Plan

- `Global.java`: remove eager Cloud `Translator` construction and use the existing public `MlKitLanguageMapper` catalog for an immutable/defensive, display-locale-sorted candidate list. Preserve saved-language normalization and callback shape. Do not download models or require credentials.
- `ErrorCodes.java`: add only stable non-Cloud codes required for actionable on-device model/capability/language/busy failures. Reuse `MISSING_MIC_PERMISSION` for microphone permission.
- `VoiceTranslationService.java`: replace Recorder-owned default capture with one-turn engine lifecycle; create/reuse exactly one `TTS`, wrap it once, and make the engine adapter its sole shutdown owner. Preserve foreground/mute/sound state and the existing `UtteranceProgressListener` completion behavior. Provide protected seams for child services to start/cancel turns and map errors without exposing UI callbacks under locks.
- `VoiceTranslationFragment.java`: render new on-device errors as recoverable messages/actions and return the mic to idle after each terminal turn. The default ON_DEVICE path must never show or navigate to API-key setup. Retain legacy-key cases only for the future explicit legacy path.
- `OnDeviceEngineFactory.java`: implement `EngineFactory` with `EngineType.ON_DEVICE`; create `AndroidSpeechRecognitionEngine`, `MlKitTextTranslationEngine`, and `AndroidSpeechOutputEngine` around the injected application/service context and single existing `TTS`. It must not construct credentials, Cloud clients, Recorder, Activity, Fragment, or View.
- `EngineServiceErrorMapper.java`: map sanitized `EngineError.Category` plus operation kind to stable service error codes. Treat teardown `CANCELLED`/`CLOSED` as non-user-facing where appropriate; never forward exception text, recognized content, PCM, or raw bundles.
- `EngineTurnCoordinator.java`: Java-only generation/operation owner shared by the mode controllers. Admit side effects only for the current open generation; cancel recognition/detection/translation on replacement, mute, language/direction change, or close; dispatch callbacks outside its lock; close each engine exactly once. Serialize ML Kit work rather than provoking `BUSY`; queued work must remain service-lifetime bounded and be cleared on close.
- `ConversationPayloadCodec.java`: extract and defensively validate the current wire format `text + languageCode + languageCode.length()` without changing emitted bytes/text. Malformed input must produce a recoverable error, not substring/number crashes.
- `ConversationOnDeviceController.java`: Java-testable orchestration for one outgoing recognition turn and serialized incoming translations. Partial results are preview-only; a current final result emits one outbound payload event. Incoming same-language text may pass through; other supported text translates once. Generation checks precede every output event.
- `ConversationService.java`: become Android/Bluetooth glue around `ConversationOnDeviceController`; remove direct `Translator`, `Recognizer`, and Recorder construction/use. Preserve communicator callbacks, message appearance, wake lock, SCO handling, and payload compatibility. Destroy controller/engines and suppress late work before Bluetooth/TTS teardown.
- `WalkieTalkieOnDeviceController.java`: Java-testable single-recognizer pipeline with explicit first/second source direction, opposite target selection, typed-text detection, serialized translation, exact terminal behavior, and generation invalidation on either language/direction change.
- `WalkieTalkieService.java`: remove the default child-service binding, PCM fan-out, dual-result queues, and confidence arbitration. Keep the legacy recognizer service classes untouched/dormant. Add communicator commands/callback state for selected source direction, and wire controller results to the existing message/TTS channel.
- `fragment_walkie_talkie.xml`: add an accessible Material two-option single-selection control beneath the two language selectors. Both targets must be at least 48dp; labels update to the selected language names; checked state must be visually clear in day/night themes using existing semantic colors.
- `WalkieTalkieFragment.java`: drive/restore the source-direction control through the service communicator, update labels after language changes, and load only the ML Kit candidate language catalog. Do not imply automatic language detection for speech.
- `strings.xml` / `values-it/strings.xml`: add matching English/Italian copy for source-direction labeling and actionable on-device errors/model guidance. Do not add Cloud billing/key language to the default flow.
- Tests: use fake engines and synchronous schedulers. Assert generation isolation, cancellation, exact-once close, callback-outside-lock re-entry, one recognizer per turn, no PCM submission, translation serialization, typed-text behavior, same-language pass-through, error mapping, malformed payload recovery, exact Bluetooth codec compatibility, explicit Walkie direction, and absence of direct `new Translator`, `new Recognizer`, or `new Recorder` in the two default mode services.
- `OnDeviceModeInstrumentedTest.java`: API 36 lifecycle/UI smoke only: construct/start/cancel/close mode-facing seams, verify the Walkie direction control is single-selection and restorable, and verify no late callback/lifecycle crash. Do not claim microphone transcription, offline quality, Bluetooth interoperability, or physical-device completion.

## Runtime and Lifecycle Contract

1. Default composition explicitly selects `EngineType.ON_DEVICE`; no credential lookup or Cloud client is allowed on service startup.
2. One explicit mic action creates at most one bounded `ENGINE_CAPTURE` operation. A second start replaces/cancels the previous generation; no PCM is submitted and no Recorder runs beside Android SpeechRecognizer.
3. Terminal recognition, user stop/mute, language/direction change, or engine failure returns the mic UI to idle. Starting another turn requires another explicit user action.
4. Conversation outbound partial results never cross Bluetooth. Exactly one current final result may be encoded and sent.
5. Conversation payload encoding remains byte/text compatible with the legacy peer format. Decode validates empty, non-digit length suffix, impossible lengths, and invalid language tags without crashing.
6. WalkieTalkie always displays a selected source direction. Changing either language or the direction invalidates current recognition and downstream translation before applying the new state.
7. ML Kit operations are serialized. Incoming Conversation work is preserved in arrival order while the service is alive; Walkie replacement/language-change work may discard superseded generations. All pending work is cleared on close.
8. A missing translation model never triggers an implicit download. The user receives guidance to Settings model management and may retry after preparation.
9. SpeechRecognizer language support is not fabricated. ML Kit locales are candidate pairs; speech failure for a particular installed recognizer/language is normalized and recoverable.
10. TTS owns one physical instance per service. Queue acceptance and utterance completion remain distinct; only the existing progress listener may resume/reset microphone behavior after actual completion.
11. Every callback checks service/controller generation before UI, Bluetooth, translation, or TTS effects. User callbacks and engine calls are never made while holding controller locks.
12. `onDestroy()` marks the service/controller closed first, cancels active operations, removes callbacks, closes engines exactly once, stops Bluetooth/SCO helpers, and cannot be undone by late callbacks.

## Verification

### JVM/static/build gate

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon
git diff --check
```

Expected: all existing 122 JVM tests plus focused Task 9.5 tests pass; lint has 0 errors and no unexplained increase over the 136-warning baseline; the debug APK exists.

### Instrumentation gate

```powershell
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
```

Expected on Pixel 7a API 36: all existing 16 tests and new lifecycle/direction smoke tests pass with no unexplained skip.

### Scope/default-path proof

```powershell
git status --short
git diff --name-status <planning-commit>..HEAD
git diff --check <planning-commit>..HEAD
rg -n "new (Translator|Recognizer|Recorder)" app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationService.java app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieService.java
```

Expected: only locked paths changed; the final `rg` command has no match; Gradle, manifest, credentials/API management, Bluetooth transport implementation, Task 9.2 contracts, Task 9.3/9.4 engines, app identity/version, and PM state are untouched by the implementation commit.

## Forbidden Changes

- No `SpeechRecognitionEngine`, `TextTranslationEngine`, `SpeechOutputEngine`, `EngineFactory`, `EngineOperation`, or core value-object contract change.
- No edits/deletion of legacy Cloud adapters, `RecognizerService`, `FirstLanguageRecognizerService`, `SecondLanguageRecognizerService`, or their manifest declarations; they remain dormant for Task 9.6.
- No Cloud default/fallback, credential prompt, hidden retry to Cloud, API-key requirement, bundled credential, or `translate.google.com` scraping.
- No automatic ML Kit model download, metered-policy bypass, fake SpeechRecognizer language list, two simultaneous recognizers, PCM fan-out, continuous recognition loop, or automatic post-terminal restart.
- No Bluetooth transport/protocol change, dependency/Gradle/manifest change, permission addition, application/package ID change, version bump, release/signing action, or Phase 3/5/9.7 completion claim.
- No broad refactor of unrelated Settings, API management, database, Bluetooth communicator, UI theme, or legacy services.
- No modification/removal of existing tests, blanket lint suppression, push, tag, merge, rebase, history rewrite, worktree creation/deletion, or auxiliary `qa-*`/`qa-gradle-*` directory.
- Do not touch `.viepilot/debug/` or modify PM-owned ViePilot state after the planning checkpoint.
- Do not mark Task 9.5 `done` or `PASS` in the implementation delivery.

## Required Evidence from Terra

- Gate 0: exact branch, HEAD, `origin/master`, upstream, status, ahead/behind, and start tag before edits; stop on any mismatch or tracked residue.
- One implementation commit above the PM planning baseline; exact SHA, parent, file list, and diff stat.
- Architecture summary covering factory/composition ownership, one-turn mic behavior, Walkie direction, translation serialization, TTS completion, generation isolation, error mapping, and exact-once teardown.
- JVM and instrumentation counts/results/skips; lint errors/warnings; APK path/size/SHA-256; `git diff --check` result.
- Tests proving payload compatibility, malformed input recovery, stale/late suppression, language/direction replacement, callback re-entry, no dual recognizer/PCM path, and no direct Cloud construction in default services.
- Scope proof for Gradle, manifest, credentials/API management, Bluetooth transport, engine contracts/implementations, app identity/version, legacy services, and ViePilot state.
- Explicit limitations: installed recognizer language support is runtime-dependent; ML Kit candidate list is not a speech-support guarantee; models are never auto-downloaded; emulator smoke does not prove recognition quality, offline speech, or physical Bluetooth interoperability.

## Acceptance Criteria

- [ ] Fresh/default Conversation and WalkieTalkie startup does not construct or require Cloud Translator/Recognizer, Recorder, or credentials.
- [ ] `EngineType.ON_DEVICE` is explicitly composed and selected; one service owns/closes one engine family and one TTS instance.
- [ ] Every mic action is one bounded `ENGINE_CAPTURE` turn with no PCM fan-out, second recognizer, continuous loop, or automatic restart.
- [ ] WalkieTalkie exposes a persistent, accessible explicit source-language direction and always translates to the opposite selected language.
- [ ] Conversation outbound final/partial behavior and Bluetooth payload framing remain compatible and malformed inbound payloads are recoverable.
- [ ] ML Kit work is serialized; missing models and capability/language/permission/busy failures are actionable and non-fatal without hidden Cloud fallback.
- [ ] Stop/mute/language/direction/replacement/destroy invalidate active work; stale callbacks cannot update UI, send, translate, or speak.
- [ ] TTS queue acceptance and actual completion remain correctly separated, with exact-once shutdown and no microphone restart after service close.
- [ ] JVM, source guard, API 36 instrumentation, lint, APK, scope, and security/privacy evidence are recorded.
- [ ] PM reviews the single implementation commit and evidence before any PASS, tag, or push.
