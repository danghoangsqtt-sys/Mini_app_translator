# Task 9.7 — Full regression and release-candidate gate

**Status**: in_progress — Gate A PASS after `BUG-026` hotfix; physical two-phone retest, accessibility, legal-owner, signing, and release approval remain mandatory
**Depends on**: Tasks 9.1–9.6 persisted through PM state commit `a229d14`
**Owner split**: PM owns the gate, evidence, release decision, state, version/signing/tag/push; implementation changes are allowed only for defects proven during this task

## Objective

Prove the no-key flow across the supported Android range and two physical phones, record a reproducible release-candidate evidence package, and refuse release approval until every mandatory human/device/signing gate is satisfied.

## Locked release decision

- A passing API 36 emulator run is automated evidence, not a substitute for API 23/31/34 or physical-device validation.
- Two physical Android phones are mandatory for Bluetooth discovery/connection, SCO/audio routing, Conversation exchange, and alternating WalkieTalkie turns.
- Publisher/controller identity, privacy contact, legal approval, release keystore access, version assignment, signed artifact, release tag, and publication require explicit maintainer input.
- Do not invent legal identity, generate or replace a production signing key, bump the version, create a release tag, or publish an unsigned/debug artifact.
- Any shipping-code defect found here must receive a focused regression test and a separately reviewable fix before the affected gate can pass.

## Locked paths

- `.viepilot/phases/phase-9-no-key-translation/tasks/TASK-9.7.md`
- `.viepilot/phases/phase-9-no-key-translation/PHASE-STATE.md`
- `.viepilot/phases/phase-9-no-key-translation/evidence/TASK-9.7-QA.md`
- `.viepilot/TRACKER.md`
- `.viepilot/HANDOFF.json`
- `app/build/outputs/apk/debug/app-debug.apk` (generated evidence only; never commit)
- `app/build/reports/tests/testDebugUnitTest/` (generated evidence only; never commit)
- `app/build/reports/lint-results-debug.html` and `app/build/reports/lint-results-debug.xml` (generated evidence only; never commit)
- `app/build/outputs/androidTest-results/connected/` (generated evidence only; never commit)

No application source, resource, Gradle, README, privacy, or CHANGELOG path is in scope unless a reproduced defect or separately approved release action makes that edit necessary.

## File-Level Plan

- `TASK-9.7.md`: lock the gate boundary, exact evidence, blocked conditions, and forbidden release actions.
- `PHASE-STATE.md`: move 9.7 to `in_progress`, then record automated results without incrementing 6/7 until every mandatory gate passes.
- `evidence/TASK-9.7-QA.md`: record environment inventory, exact commands, test/lint counts, APK size/hash/install result, runtime smokes, matrix status, defects, and blockers.
- `TRACKER.md`: keep the project-level phase/task status and decision log synchronized with the evidence.
- `HANDOFF.json`: expose the exact resume point and the human inputs still required.

## Best practices

- Preserve evidence provenance: include UTC/local timestamps, Git SHA, device serial/model/API, exact command, result, and artifact hash.
- Treat skipped, unavailable, or unobserved behavior as BLOCKED/NOT RUN, never PASS.
- Use a fresh app-data/install path for onboarding and credential-free smoke; restore any emulator network, permission, font-scale, and accessibility setting changed during testing.
- Keep generated reports/APKs out of Git and leave pre-existing `.viepilot/debug/` untouched.
- Re-run the narrowest relevant test after a defect fix, then repeat the complete blocking gate.
- Keep Java 8/minSdk 23 compatibility and never weaken current secret/privacy safeguards to make a test pass.

## Gate A — Automated repository and API 36 emulator evidence

1. Confirm clean canonical `master`, `HEAD == origin/master` at the planning start point, and only the pre-existing `.viepilot/debug/` untracked state.
2. Run:

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
git diff --check
```

3. Record JVM/instrumentation counts, lint errors/warnings, debug APK byte size and SHA-256.
4. Fresh-install the debug APK on the API 36 emulator without any credential, cold-launch it, relaunch it, force-stop/relaunch it, and check for FATAL/ANR.
5. Exercise microphone/Nearby deny, grant, revoke, and re-grant paths where the emulator/runtime exposes them; record unsupported paths as NOT RUN.
6. Record the already implemented no-key/on-device source guards and scan active UI/docs for obsolete RTranslator, `$300`, or false BLE-required claims.

## Gate B — Supported Android version matrix

- API 23, 31, 34, and 36: fresh launch, onboarding/relaunch, process recreation, permissions appropriate to that SDK, and a bounded Conversation/Walkie lifecycle smoke.
- A platform directory alone is insufficient; each row requires an attached device or runnable emulator system image and recorded serial/model/API.
- Installing large SDK system images requires separate maintainer approval. Missing images/devices remain BLOCKED.

## Gate C — Mandatory physical-device and product QA

- Two physical phones: discovery, permission denial/recovery, connect/disconnect/reconnect, background/foreground, Bluetooth Conversation exchange, SCO/audio routing, and alternating WalkieTalkie source directions.
- Speech recognizer present/absent and truthful system-fallback disclosure.
- Model download success/failure/cancel/delete, low-storage behavior, and airplane-mode translation with a previously downloaded model.
- Light/dark, adaptive icon/splash, TalkBack navigation/labels/focus order, and 200% font without clipped critical controls.
- No active RTranslator branding, obsolete `$300` claim, false BLE gate, hidden Cloud fallback, or credential requirement.

## Gate D — Legal, signing, and release approval

- Maintainer supplies/approves publisher/data-controller identity, privacy contact, applicable jurisdiction copy, and legal sign-off.
- Maintainer supplies/authorizes the production release signing configuration; no key material is committed or printed in evidence.
- PM reviews all diffs/evidence and confirms zero unresolved Critical/High defects.
- Only after explicit approval: assign the next `versionCode` and target `versionName 1.3.0`, build/verify the signed artifact, record certificate/artifact fingerprints, update release notes/state, create the release tag, and push/publish as separately authorized.

## Acceptance criteria

- [x] Gate A passes with zero test failures, zero lint errors, exact artifact/install evidence, and no unresolved Critical/High defect in the exercised scope.
- [ ] Gate B passes on API 23, 31, 34, and 36 with per-device evidence.
- [ ] Gate C passes on two physical phones, including Bluetooth/SCO, no-key speech/translation, offline-model, accessibility, and visual checks.
- [ ] Gate D has approved legal identity/privacy copy, authorized signing, reviewed version assignment, signed artifact evidence, and explicit release approval.
- [ ] PM reviews the final diff, state files, evidence, and Git/remote/tag state.

## Forbidden changes

- No release PASS, phase completion, version bump, signed-release claim, release tag, or publication while any Gate B/C/D row is BLOCKED or NOT RUN.
- No production keystore generation/replacement, secret material in Git/logs, force push, rebase, history rewrite, or deletion of `.viepilot/debug/`.
- No broad refactor, dependency upgrade, Wi-Fi Hotspot work, Cloud runtime selector/fallback, or unrelated backlog cleanup.

## Completion evidence

Gate A passed again on 2026-09-28 after reproducing and fixing `BUG-026`, the Android 12+ Conversation notification `PendingIntent` crash reported on two physical phones: 143 JVM tests, 20 API 36 instrumentation tests, lint 0 errors/136 warnings, focused persisted-Conversation relaunch smokes with denied and granted permissions, and debug APK SHA-256 `D80109A70D47DF99B35D94CBB20C9A37ACF5DD1603F3D8876B8DB4CA83AC560D`. Gate C requires the same two phones to confirm the hotfix and continue Bluetooth/SCO QA; Gates B/D remain blocked. Canonical detail is in `evidence/TASK-9.7-QA.md`. Task progress remains 6/7 until all four gates pass.
