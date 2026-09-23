# Remediation plan — audit follow-up (historical baseline 2026-09-20)

> **Current-state update (2026-09-23):** this document preserves the original baseline below. The current app is Mini Conversation 1.2.0/versionCode 15 on AGP 8.13.2, Gradle 8.13, compile/target SDK 36. Phase 3 Cluster A and Phase 5 code are implemented, while their physical-device, accessibility, signing, and release gates remain pending human validation. See `TRACKER.md` and the phase states for live status.

## Historical baseline and scope

- Current Android runtime is `1.1.2` (`versionCode 13`, target SDK 29). Phase 1 task 1.6 is implemented; no runtime version or release tag has changed.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `assembleRelease` completed on 2026-09-20. Fourteen local unit tests passed after task 1.6. Lint still reports 5 errors and 119 warnings because `abortOnError` is disabled; the release artifact is unsigned.
- Task 1.6 passed manual SAF validation on the API 36 `medium_phone` emulator. Preserve the user's uncommitted image changes and do not treat a successful Gradle build as a release gate.

## Execution order and gates

| Order | Existing phase/tasks | Deliverable | Exit gate |
|---|---|---|---|
| 1 | Phase 1, task 1.6 (`BUG-006`) — done | Explicit system document picker; bounded, validated import into encrypted credential storage; remove broad storage permissions. | JVM regression tests and API 36 emulator validation for picker open/cancel, invalid rejection, and valid import persistence. |
| 2 | Phase 2, tasks 2.1–2.6 (`BUG-007`–`BUG-012`) | Shared Room instance, deterministic token/service lifecycles; resolve crypto and chart findings after reachability checks. | Focused tests for each reachable defect; existing data preserved or migration/recovery documented. |
| 3 | Phase 3, tasks 3.1–3.6 | API 36-compatible toolchain, Bluetooth permissions, exported components, foreground-service types, dependency refresh, and blocking lint gate. | Tasks 3.1/3.2/3.4/3.5 are integrated together; tests, lint, debug/release builds and device matrix pass. |
| 4 | Phase 5, tasks 5.1/5.6 | Repair README drift and collect end-to-end/release evidence with the planned product/UI work. | Two-phone conversation, one-phone walkie-talkie, credential, TTS, background/restore, accessibility and signed release checks. |

Phase 4 remains an independent hygiene backlog; its proposed tasks do not block the first remediation pass unless their reachability or impact is reclassified during execution.

## Cross-task constraints

- Do not raise `targetSdk` alone. Android 12+ Bluetooth permissions and explicit exported declarations, plus Android 14+ foreground-service types and matching permissions, must be reviewed and shipped in the same integration change.
- For `BUG-008`, first establish whether `Tools.encript/decript` has persisted-data consumers. The current source search found only helper definitions; remove dead helpers if safe, or design versioned AEAD migration if data actually exists. Do not migrate `CredentialStore` again: it already uses Keystore-backed AES-GCM.
- For `BUG-012`, first establish whether any first-party UI uses GraphView second scale. If unreachable, document why and retire/guard the branch; if reachable, add a reproduction and fix it. Do not assume an active user-facing crash from a vendored branch alone.
- Resolve the five lint errors before changing lint to block builds; review the 119 warnings by severity rather than suppressing them wholesale. The gRPC dependency errors may be removed by task 3.3. Record a justified baseline for any remaining warning.
- Do not copy production service-account JSON into tests, screenshots, logs, build outputs, or a release artifact. Before public distribution, make an explicit authentication architecture decision: retain user-supplied keys only with a documented risk model and narrow scopes, or use a service-mediated alternative. This decision is not made by this plan.
- Keep the Java/XML Views architecture and application ID during remediation. Preserve upgrade compatibility and user data; release signing keys must remain outside the repository.

## Verification matrix

1. Host: `gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease`; fail the gate on test failures, lint errors, or build errors. Inspect the merged manifest and release artifact.
2. Device: `connectedDebugAndroidTest` on available API levels; minimum manual matrix API 23, 31, 34 and 36, with at least one physical Bluetooth two-phone pair. An emulator alone does not validate Bluetooth/audio transport.
3. Workflows: first launch, valid/invalid/oversize key import and deletion, migration/backup exclusion, Conversation and WalkieTalkie, permission grant/deny/revoke, reconnect, screen lock/background/foreground, process recreation, and TTS language fallback.
4. Release: signed artifact from an external secret source, versionCode increment at actual release, no credential leakage, resolved high-severity findings, updated README/privacy text, and human sign-off on device results.

## Version policy

Keep `1.1.2` while only planning. A bug-fix-only release should propose a PATCH version (for example `1.1.3`) with a new versionCode when built for distribution; the already planned product/UI feature release remains `1.2.0`. The final version and code are selected at release time under `SYSTEM-RULES.md`.
