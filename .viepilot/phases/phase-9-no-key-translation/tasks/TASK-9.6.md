# Task 9.6 — Make legacy Cloud optional and complete migration UX

**Status**: done — PM automated/emulator PASS 2026-09-26
**Depends on**: Task 9.5 at `ab6094c` and its PM state commit
**Owner split**: TERRA implements the locked application/docs scope; PM reviews evidence and updates ViePilot state

## Objective

Remove Cloud setup from the default journey, preserve an explicit legacy/advanced mode where safe, and synchronize privacy, pricing, attribution, and settings copy.

## Locked product decision

- The only default runtime is Android SpeechRecognizer + ML Kit Translation + Android TextToSpeech.
- No automatic or error-triggered Cloud fallback is allowed.
- Existing credential import/delete may remain solely as clearly labeled **Legacy Cloud (advanced / optional)** migration management.
- Task 9.6 does **not** expose a functional runtime Cloud-mode selector. The retained legacy speech adapter requires PCM input while the current controllers use engine-owned capture; reconnecting it needs a separately approved design.
- No service-account key, access token, or private-key fixture may be bundled in a shipping APK.

## Paths

- `app/src/main/java/nie/translator/rtranslatordevedition/settings/SupportTtsQualityPreference.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/settings/SettingsFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieService.java`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-it/strings.xml`
- `README.md`
- `privacy/Privacy_Policy_en.md`
- `privacy/Privacy_Policy_it.md`
- `.viepilot/PROJECT-CONTEXT.md`
- `.viepilot/PROJECT-META.md`
- `.viepilot/AI-GUIDE.md`
- `.viepilot/ARCHITECTURE.md`
- `.viepilot/architecture/system-overview.mermaid`
- `app/src/test/java/nie/translator/rtranslatordevedition/MigrationUxContractTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/ServiceRestartContractTest.java`
- `CHANGELOG.md`

## File-Level Plan

- `SupportTtsQualityPreference.java`: remove the unused legacy Cloud `Translator` field/import/construction so binding ordinary Settings cannot request a Cloud token.
- `SettingsFragment.java`: make the missing-TTS message key consistent with the handler and prevent fall-through into the generic error callback (`BUG-025`).
- `VoiceTranslationService.java`: define a non-sticky, null-intent-safe restart policy and preserve normal notification startup (`BUG-024`).
- `WalkieTalkieService.java`: delegate null-intent recovery before reading language extras; preserve normal first/second-language application (`BUG-024`).
- EN/IT strings: label credential management as optional legacy/advanced tooling; remove obsolete pricing/tutorial claims and automatic-Cloud implications.
- README/privacy/architecture/project-context/project-meta/AI guide/Mermaid: document the implemented on-device default, truthful system-recognizer/model-download network behavior, Bluetooth data flow, and optional local legacy credential storage.
- `MigrationUxContractTest.java`: source/resource/doc guards for no default legacy construction, corrected Settings dispatch, current fork privacy links, and prohibited obsolete Cloud-default/pricing claims.
- `ServiceRestartContractTest.java`: source contract covering null-intent guards and explicit `START_NOT_STICKY` restart behavior for both services.
- `CHANGELOG.md`: record the implemented migration UX and audit bug fixes under `[Unreleased]`; do not bump `versionCode`/`versionName`.

## Best practices

- Keep Android service restart behavior explicit; never dereference framework callback inputs before null validation.
- Keep the default path credential-free and prevent hidden network/Cloud fallback.
- Preserve encrypted credential import/delete and backup exclusions without expanding credential scope.
- Treat privacy edits as factual technical disclosure; flag publisher/controller identity for human legal review rather than inventing legal ownership.
- Add focused regression tests for every fixed audit defect and keep Java 8/API 23 compatibility.

## Required implementation

1. Remove the unused legacy `Translator` construction from `SupportTtsQualityPreference`; opening or rebinding Settings must not request an API token.
2. Reframe credential-management UI as optional legacy/advanced migration tooling. It must not appear as a prerequisite for onboarding, Conversation, WalkieTalkie, model management, or ordinary Settings use.
3. Preserve safe encrypted credential deletion/import behavior already delivered by Phase 1; do not weaken validation, encrypted storage, or backup exclusions.
4. Remove obsolete Cloud trial, billing, hourly-price, and niedev setup claims from active EN/IT resources and user-facing documentation.
5. Rewrite README and privacy disclosures to describe actual behavior: on-device ML Kit models, Android/system speech capability and possible network use, Android TTS, Bluetooth transport, and optional legacy credential handling.
6. Synchronize current architecture/project-context docs and the required Mermaid sidecar with the implemented on-device default.
7. Add focused source/resource regression tests for the default path and prohibited obsolete claims.
8. Correct the Settings missing-TTS event key/fall-through defect from `BUG-025`, or remove that dormant path if call-graph proof shows it is no longer reachable.
9. Fix `BUG-024`: null-intent service restarts must stop safely with explicit non-sticky behavior and must not read missing notification/language extras.

## Acceptance criteria

- [x] Fresh/default path contains no Cloud key requirement or billing marketing.
- [x] Legacy Cloud is clearly labeled opt-in and never auto-selected for a keyless user.
- [x] No shared credential or secret is present in source, resources, APK, or test fixtures.
- [x] Existing encrypted credential can be removed safely and is not uploaded.
- [x] Privacy/attribution/network disclosures match actual engine behavior.
- [x] Resource/source tests prevent reintroduction of obsolete RTranslator/$300 copy.
- [x] Opening/binding ordinary Settings does not instantiate the legacy Cloud `Translator` or request an API token.
- [x] README, privacy EN/IT, project context, architecture doc, and Mermaid sidecar agree on the on-device default and its truthful network limitations.
- [x] Legacy credential controls are visibly advanced/optional and do not claim that Cloud mode is automatically active.
- [x] Secret scan distinguishes fake/validator markers from shipping assets and finds no real credential/private key in source, resources, APK, or Git diff.
- [x] `BUG-025` has exactly one correct terminal UI action and focused regression coverage.
- [x] `BUG-024` is covered for both services; a null restart intent cannot crash or synthesize invalid session state.

## Forbidden changes

- No changes to core speech/translation contracts or Task 9.5 mode controllers. The only permitted service edits are the narrow `BUG-024` start-intent guards.
- No Cloud runtime selector, Recorder/PCM reintegration, hidden fallback, or `translate.google.com` scraping.
- No Bluetooth framing/transport, Gradle dependency, application ID, versionCode/versionName, signing, or Phase 9.7 device-matrix changes.
- Do not modify `.viepilot/debug/`, PM state, tags, or remote branches during implementation.

## Verification gate

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
git diff --check <planning-baseline>...HEAD
```

Evidence must include changed-file scope, test counts, lint error/warning counts, APK size/SHA-256, a source/resource scan for obsolete claims and default-path legacy construction, and final Git status. Implementation is one local commit above the PM planning baseline; no push/tag/state update until PM review.

## Completion evidence

- Planning baseline: `fb18fec`; implementation commit: `8a8882b` (18 files, 399 insertions, 336 deletions), persisted to `origin/master`.
- JVM: 143 tests, 0 failures/errors/skips.
- Instrumentation: 19 Pixel 7a API 36 tests, 0 failures/errors/skips.
- Lint: 0 errors, 136 warnings; debug APK assembled successfully.
- APK: 79,115,707 bytes; SHA-256 `BD98BB5C24BA08DE8287A899449C0C084457058A681E6ADCC2AF9F0A7F5BFD2A`.
- Source/resource scan found no obsolete active pricing/onboarding claim and no Settings-side `new Translator(...)` construction.
- APK scan found only the expected private-key **format marker** compiled from `ServiceAccountCredentialValidator`; no key body, key ID, client email, Google API key, or OAuth access token was present.
- Privacy files are now factual technical disclosures. Publisher/controller identity and jurisdiction-specific legal approval remain a human release gate in Task 9.7.
