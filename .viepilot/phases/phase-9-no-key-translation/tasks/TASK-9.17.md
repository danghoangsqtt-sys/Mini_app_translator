# Task 9.17 — Executable APK crash-regression qualification gate

**Status**: planned — blocks another physical installation
**Request**: `ENH-013`
**Depends on**: Tasks 9.13–9.16

## Objective

Turn the repeated manual failure routes into one reproducible APK-level gate that builds, installs, upgrades, drives real permission/service lifecycles, waits beyond platform deadlines, inspects process/service state, and refuses to hand off an APK if any fatal, ANR, unexpected exit, or orphan service is observed.

## Paths

- `scripts/qa/voice-service-qualification.ps1` (new)
- `app/src/androidTest/java/nie/translator/rtranslatordevedition/voice_translation/VoiceServiceQualificationInstrumentedTest.java` (new)
- Narrow supporting Android-test fixtures under `app/src/androidTest/`
- `app/build.gradle` only for deterministic QA tasks/test options
- `.gitignore` only for generated qualification reports/baseline APKs
- `.viepilot/phases/phase-9-no-key-translation/evidence/TASK-9.17-APK-QUALIFICATION.md` (summary evidence only)
- Focused JVM source/contract tests under `app/src/test/`

## Required matrix

1. Fresh install: persisted Pairing, denied microphone, denied Nearby where applicable, grant, permanent denial, revoke/regrant, force-stop/relaunch.
2. Persisted modes: Conversation and Walkie cold launch with denied/granted prerequisites; hold each run beyond the foreground-service deadline.
3. Real services: start, bind, cancel-before-bind, recreate during permission, background/foreground, stop, repeated start/stop, and process relaunch.
4. Upgrade: install a captured pre-Task-9.13 baseline APK, seed Conversation/Walkie persisted state through test instrumentation, install the candidate with `-r`, and verify safe migration/recovery without clearing app data.
5. Stress: message-before-snapshot, stalled translation/output queues, bounded history restore, and service teardown while work is pending.

## Plan

1. Capture and hash the baseline debug APK before shipping-code implementation begins; keep generated baseline/candidate APKs and raw reports under ignored `app/build/` paths.
2. Add instrumentation hooks that exercise production Activity/Service classes and persist only test-owned state. No test-only component or bypass may ship in the main manifest/source set.
3. Add a PowerShell orchestrator using the configured Android SDK `adb`: build APKs, install/uninstall/upgrade, grant/revoke permissions, run named instrumentation cases, wait at least 10 seconds for foreground-start cases, inspect `dumpsys activity services`, and capture bounded logcat/process-exit evidence.
4. Fail on `FATAL EXCEPTION`, `ForegroundServiceDidNotStartInTimeException`, foreground-service ANR, instrumentation crash, unexpected PID death, orphan Conversation/Walkie service, nonzero test failure, or missing expected foreground type.
5. Sanitize reports: include commit/APK hash/device/API/test/state counters only; exclude speech/translation text, peer identity/address, credentials, audio, raw stack traces, and stable device identifiers.
6. Require API 36 to pass before producing the user handoff APK. Run API 23/31/34 when a runnable image/device exists and record unavailable lanes as BLOCKED, never PASS. Physical Bluetooth/SCO still belongs to Task 9.7.

## Acceptance criteria

- [ ] One documented command runs the complete available-device gate from build through report and returns nonzero on any blocked mandatory API 36 or failed condition.
- [ ] Fresh-install and upgrade-with-state paths cover both Conversation and Walkie permission/service lifecycles.
- [ ] Each foreground start is held beyond 10 seconds with process survival, expected service type, and no fatal/ANR/orphan service.
- [ ] The gate proves Task 9.14–9.16 regression scenarios against production classes, not only fakes/source strings.
- [ ] Generated evidence is privacy-safe, reproducible, ignored where raw, and summarized with APK SHA-256.
- [ ] Only after this task passes may Task 9.7 request another same-two-phone physical installation.

## Verification

```powershell
.\scripts\qa\voice-service-qualification.ps1 -Api 36 -RequireApi36
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug assembleRelease verifyReleasePrivacy --console=plain --no-daemon
git diff --check
```

## Forbidden changes

- No claim that emulator automation replaces physical Bluetooth/SCO, no test-only bypass in `main`, no raw unsanitized log attachment in Git, no automatic SDK/system-image download without approval, and no version bump/tag/push/release claim.
- Do not touch `.viepilot/debug/`.
