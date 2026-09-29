# Task 9.13 — Privacy-safe crash diagnostics and error visibility

**Status**: in_progress — implementation and local quality gates PASS; git persistence gate pending
**Request**: `ENH-006`
**Depends on**: audit baseline `5c9de7a`; Task 9.7 remains blocked

## Objective

Make the next runtime failure diagnosable without collecting conversation content or requiring live ADB. Record only bounded lifecycle/error metadata, expose the last Android process-exit reason on API 30+, and let the user export a sanitized diagnostic text file from Settings.

## Paths

- `app/src/main/java/nie/translator/rtranslatordevedition/Global.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/GeneralService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/api_management/ApiManagementFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/tools/services_communication/ServiceCommunicator.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationActivity.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/diagnostics/DiagnosticEvent.java` (new)
- `app/src/main/java/nie/translator/rtranslatordevedition/diagnostics/DiagnosticState.java` (new)
- `app/src/main/java/nie/translator/rtranslatordevedition/diagnostics/ProcessExitRecord.java` (new)
- `app/src/main/java/nie/translator/rtranslatordevedition/diagnostics/Api30ProcessExitReader.java` (new)
- `app/src/main/java/nie/translator/rtranslatordevedition/diagnostics/DiagnosticReport.java` (new)
- `app/src/main/java/nie/translator/rtranslatordevedition/diagnostics/AppDiagnostics.java` (new)
- `app/src/main/java/nie/translator/rtranslatordevedition/settings/SettingsFragment.java`
- `app/src/main/res/xml/preferences.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-it/strings.xml`
- `app/src/test/java/nie/translator/rtranslatordevedition/diagnostics/DiagnosticStateTest.java` (new)
- `app/src/test/java/nie/translator/rtranslatordevedition/diagnostics/ProcessExitRecordTest.java` (new)
- `app/src/test/java/nie/translator/rtranslatordevedition/diagnostics/DiagnosticReportTest.java` (new)
- `app/src/test/java/nie/translator/rtranslatordevedition/diagnostics/DiagnosticPrivacySourceTest.java` (new)
- `app/src/androidTest/java/nie/translator/rtranslatordevedition/diagnostics/AppDiagnosticsInstrumentedTest.java` (new)

## Implementation Notes

- `diagnostics/DiagnosticEvent.java`: define the only accepted mode, lifecycle-stage, operation, and error-category values; events contain a sequence number and enums only.
- `diagnostics/DiagnosticState.java`: own a synchronized fixed-size event ring plus overflow-safe counters; encode/decode a strict versioned process summary capped at 128 UTF-8 bytes and reject malformed/free-form summaries.
- `diagnostics/ProcessExitRecord.java` and `Api30ProcessExitReader.java`: map API 30+ `ApplicationExitInfo` reason/status/importance/timestamp and the validated prior process summary without reading descriptions, traces, process names, PSS/RSS, or identifiers; API 23–29 returns `UNAVAILABLE`.
- `diagnostics/DiagnosticReport.java`: render a fixed-key UTF-8 report with app version, API/device class, permission states, selected mode, current bounded events/counters, and mapped prior exit; run a deny-list privacy guard before returning bytes.
- `diagnostics/AppDiagnostics.java` and `Global.java`: initialize once at process start, expose enum-only record calls, update `ActivityManager.setProcessStateSummary()` on API 30+, and provide a bounded report/export API. Any platform failure is contained and represented only by a sanitized category.
- `GeneralService.java` and `ServiceCommunicator.java`: replace remote IPC `printStackTrace()` paths with a sanitized diagnostic event, clear the dead Messenger, and never retain or log exception text.
- `ApiManagementFragment.java` and `SettingsFragment.java`: replace same-process Messenger round trips with direct Handler messages. Settings also owns the SAF `ACTION_CREATE_DOCUMENT` result, writes from the application context, and posts localized success/failure feedback without retaining the URI.
- `VoiceTranslationActivity.java` and `VoiceTranslationService.java`: record caller start and service promotion failures with fixed mode/stage/operation/category values while preserving current recovery behavior. Promotion ordering itself remains Task 9.14.
- `preferences.xml` and localized strings: add a Diagnostics category and export action only; do not request storage, logcat, account, or network permissions.
- JVM tests: prove platform-reason mapping, strict summary bounds/parsing, bounded ring behavior, fixed-schema privacy, and source-level absence of raw exception/trace/device-ID access in the diagnostics package.
- API 36 instrumentation: prove process initialization, enum-only recording, bounded sanitized export bytes, and an `ACTION_CREATE_DOCUMENT` intent without new permissions or network access.

Best practices: keep Android API 30 references isolated behind the SDK guard, use application context only, close output streams with try-with-resources, use defensive immutable snapshots, never pass conversation/peer/credential/audio/model values into diagnostics, and keep all failures recoverable.

Expected verification: all existing and new JVM/instrumentation tests pass, lint reports zero errors, debug and unsigned release assemble, `verifyReleasePrivacy` passes, and `git diff --check` is clean.

## Acceptance criteria

- [x] A tester can export one sanitized diagnostic file after relaunch without ADB, storage permission, account, or network.
- [x] API 30+ distinguishes at least crash, ANR, low-memory, permission change, user stop, and unknown using platform reason codes; older APIs state that the reason is unavailable.
- [x] Process-state data is at most 128 bytes and contains enums/counters only.
- [x] No diagnostic path persists or exports conversation text, peer identifiers, credentials, audio, model content, exception messages, or raw stack traces.
- [x] Release-reachable `printStackTrace()`-only paths in scope are replaced with observable sanitized handling.
- [x] JVM/instrumentation/privacy/release gates pass with no new permissions or remote telemetry dependency.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug assembleRelease verifyReleasePrivacy --console=plain --no-daemon
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
git diff --check
```

## Local implementation evidence

- Implementation commit: `489e569` (`21 files changed, 1,386 insertions, 30 deletions`). The local task checkpoint remains `mini-app-translator-vp-p9-t13`; no done/release tag was created.
- The process-wide diagnostics boundary retains at most 16 enum-only events, writes a strict `D1` summary capped at 128 UTF-8 bytes, and reads only mapped `ApplicationExitInfo` reason/status/importance/timestamp plus that validated summary.
- Settings exports a fixed-schema report through `ACTION_CREATE_DOCUMENT`. The report is capped at 8 KiB and includes only build/API/device class, permission states, selected mode, counters/events, and mapped exit metadata.
- Scoped IPC and foreground-service failure paths now emit sanitized enum categories; exception messages, stack traces, conversation/translation text, peer identity, credentials, audio, and model contents are neither retained nor exported.
- Final host gate: 201 JVM tests passed; lint 0 errors/122 warnings (unchanged baseline); debug and unsigned release APKs assembled; `verifyReleasePrivacy` passed; `git diff --check` passed.
- Final Pixel 7a API 36 gate: 26 instrumentation tests passed with no failures or skips. One earlier full-suite attempt was interrupted by emulator memory pressure during the pre-existing online ML Kit test; that test then passed alone 3/3 and the complete suite passed twice after recovery, including the final-code run.
- Final debug APK: 79,370,448 bytes, SHA-256 `497EF8EF079F7044FBA9B50087855BCE9F58CBC136E5C497A46386E3F50F271C`.
- Final unsigned release APK: 71,006,077 bytes, SHA-256 `A47DC92E8D7A9781224F6D8723ACBFB79754649E5D5874281A40ADB4A31DCE5C`.
- The task remains `in_progress` because `master` contains pre-existing and current unpushed commits. Per the ViePilot persistence gate and task prohibition, no push was performed and Task 9.14 was not started.

## Forbidden changes

- No analytics/crash-reporting SDK, backend upload, logcat-reading permission, credential access, database schema change, version bump, push, release/phase tag, or release claim. The local ViePilot task checkpoint tag is allowed.
- Do not include raw `ApplicationExitInfo` traces or human-readable system descriptions in the exported file; map only stable reason/status fields.
- Do not touch `.viepilot/debug/`.
