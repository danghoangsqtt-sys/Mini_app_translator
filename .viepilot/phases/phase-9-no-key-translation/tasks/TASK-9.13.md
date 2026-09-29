# Task 9.13 — Privacy-safe crash diagnostics and error visibility

**Status**: planned — next executable task
**Request**: `ENH-006`
**Depends on**: audit baseline `5c9de7a`; Task 9.7 remains blocked

## Objective

Make the next runtime failure diagnosable without collecting conversation content or requiring live ADB. Record only bounded lifecycle/error metadata, expose the last Android process-exit reason on API 30+, and let the user export a sanitized diagnostic text file from Settings.

## Paths

- `app/src/main/java/nie/translator/rtranslatordevedition/Global.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/GeneralService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/api_management/ApiManagementFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/tools/services_communication/ServiceCommunicator.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/diagnostics/**` (new)
- `app/src/main/java/nie/translator/rtranslatordevedition/settings/SettingsFragment.java`
- `app/src/main/res/xml/preferences.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-it/strings.xml`
- Focused tests under `app/src/test/` and `app/src/androidTest/`

## Plan

1. Add a small diagnostics boundary with enums for mode, lifecycle stage, error category, and last operation. Never accept free-form recognized/translated text, peer identity/address, credential fields, audio, model content, URI, or throwable messages.
2. On API 30+, write a process-state summary no larger than 128 bytes and read/deduplicate the latest `ApplicationExitInfo` record after relaunch. On API 23–29, report process-exit history as unavailable rather than inventing an exit cause.
3. Replace `printStackTrace()`-only and generic voice-service/IPC failure paths with sanitized categories plus the existing user-visible recoverable error where applicable. Do not upload telemetry or request new permissions.
4. Add a Settings action that uses Storage Access Framework `ACTION_CREATE_DOCUMENT` to export a plain-text snapshot containing app/build/API/device class, permission booleans, selected mode, bounded lifecycle milestones, queue counters, and the last exit reason.
5. Add privacy guards that reject sensitive keys/tags and release-artifact checks proving diagnostic code contains no payload/peer/credential logging.

## Acceptance criteria

- [ ] A tester can export one sanitized diagnostic file after relaunch without ADB, storage permission, account, or network.
- [ ] API 30+ distinguishes at least crash, ANR, low-memory, permission change, user stop, and unknown using platform reason codes; older APIs state that the reason is unavailable.
- [ ] Process-state data is at most 128 bytes and contains enums/counters only.
- [ ] No diagnostic path persists or exports conversation text, peer identifiers, credentials, audio, model content, exception messages, or raw stack traces.
- [ ] Release-reachable `printStackTrace()`-only paths in scope are replaced with observable sanitized handling.
- [ ] JVM/instrumentation/privacy/release gates pass with no new permissions or remote telemetry dependency.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug assembleRelease verifyReleasePrivacy --console=plain --no-daemon
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
git diff --check
```

## Forbidden changes

- No analytics/crash-reporting SDK, backend upload, logcat-reading permission, credential access, database schema change, version bump, tag, push, or release claim.
- Do not include raw `ApplicationExitInfo` traces or human-readable system descriptions in the exported file; map only stable reason/status fields.
- Do not touch `.viepilot/debug/`.
