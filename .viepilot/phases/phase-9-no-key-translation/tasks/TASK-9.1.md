# Task 9.1 — Stabilize launch, onboarding, and Bluetooth capability handling

## Objective

Make a fresh install reach and remain in the main UI without requiring a Google Cloud credential, remove the obsolete RTranslator/300 USD onboarding presentation, and replace the false BLE capability block with accurate non-fatal handling. This task does **not** implement ML Kit or SpeechRecognizer.

## Paths

### Allowed application paths

- `app/src/main/java/nie/translator/rtranslatordevedition/LoadingActivity.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/access/AccessActivity.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/access/NoticeFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/access/UserDataFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationActivity.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/PairingFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/communication/BluetoothCapabilityEvaluator.java`
- `app/src/main/res/layout/fragment_notice.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-it/strings.xml`
- `app/src/test/java/nie/translator/rtranslatordevedition/OnboardingNoKeyContractTest.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/communication/BluetoothCapabilityEvaluatorTest.java`

### PM-owned state paths

- `.viepilot/phases/phase-9-no-key-translation/PHASE-STATE.md`
- `.viepilot/TRACKER.md`
- `.viepilot/HANDOFF.json`
- `.viepilot/ROADMAP.md`
- `CHANGELOG.md`

Any additional application path requires a written justification and PM approval before editing.

## File-level plan

1. Inspect the complete startup chain (`LoadingActivity` → `AccessActivity`/`UserDataFragment` → `VoiceTranslationActivity`) and record the exact failure mechanism before changing navigation.
2. Replace the Notice content with durable no-key/on-device-first wording. Remove the active RTranslator wordmark and Google Cloud promotional presentation while retaining upstream attribution in legal/about documentation.
3. Ensure completion of first-run profile setup launches the main activity safely and cannot leave the task with no foreground activity if destination creation fails.
4. Add a small, pure capability evaluator that separates: permissions missing, Bluetooth disabled/unavailable, required discovery unsupported, and library error. Do not equate multiple-advertisement support with all BLE support.
5. Update pairing UI to show actionable, non-fatal states. Do not claim the phone has no BLE unless actual hardware capability is absent.
6. Add focused JVM/source contract tests for stale wording/logo references, credential-free first launch, and the capability decision table.

## Required behavior

- Fresh install can pass Notice/Profile and remain on the main screen with no service-account key.
- Missing credential may disable legacy Cloud operations, but must not close the app or force a credential import screen.
- The Notice screen contains no `$300`, `first year`, RTranslator active logo, or instruction to configure Cloud before use.
- Upstream copyright/license attribution remains intact outside active product branding.
- Permission denial and Bluetooth-off states remain recoverable.
- Do not bypass runtime Bluetooth permission checks.
- Do not edit or vendor `BluetoothCommunicator` dependency code in this task.

## Forbidden changes

- No ML Kit dependency or translation implementation.
- No SpeechRecognizer implementation.
- No `/m` web scraping.
- No version bump, push, merge, or tag.
- No mass resource rename or package/applicationId change.
- No blanket exception swallowing, lint suppression, or test deletion.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain
```

Expected:

- All unit tests pass.
- Lint reports zero errors; warning count must not increase without explanation.
- Debug APK is produced.
- Emulator/API 36: clear app data, launch, finish onboarding, return to launcher and reopen; main screen remains accessible.
- Emulator/API 36: deny/regrant Nearby Devices and microphone permissions without a process crash.
- Pairing screen does not display the false “no Bluetooth Low Energy” claim solely because multiple advertisements are unsupported.

## Evidence report required from Terra

- Root cause with file/line references.
- `git diff --stat` and exact files changed.
- Test command summaries and counts.
- Screenshot or precise runtime observations for fresh launch and pairing state.
- Remaining risks and manual tests not performed.
- Local commit SHA.

Stop after the local commit and report to PM. Do not start Task 9.2.

