# Phase 9 — No-Key On-Device Translation

**Mode**: Add Feature + release-blocker remediation  
**Target**: Mini Conversation `1.3.0` (`versionCode` to be assigned only when a release candidate is cut)  
**Status**: in_progress  
**Depends on**: Phase 8 Tasks 8.1–8.2 canonical repository baseline  
**Supersedes for release**: unsigned/unusable `1.2.0` candidate; do not publish it  
**Moves former Phase 9**: Wi-Fi Hotspot becomes Phase 10

## Goal

Deliver a launchable, testable Mini Conversation that does not require end users to configure Google Cloud. The default speech/translation path uses supported device APIs, while legacy Cloud behavior is isolated behind an explicit advanced choice.

## Architecture decision

| Layer | Default implementation | Fallback/legacy |
|---|---|---|
| Speech input | Android `SpeechRecognizer`; on-device when available | System recognizer with truthful network disclosure; legacy Cloud opt-in during migration |
| Text translation | Google ML Kit Translation with downloaded models | Legacy Cloud opt-in; no `/m` scraping |
| Speech output | Android TextToSpeech | Existing error handling |
| Transport | Existing Bluetooth | Wi-Fi remains Phase 10 |
| Credentials | None for default mode | Legacy user credential only; no shared key in APK |
| Diagnostics | Local, user-exported sanitized snapshot; API 30+ process-exit reason | No telemetry/backend, payload, peer identity, credential, audio, or raw trace collection |

## Invariants

- API 23 remains the minimum supported SDK.
- Do not call API 31-only speech methods without SDK and availability guards.
- Do not start indefinite continuous `SpeechRecognizer` sessions; use bounded utterances/turns.
- Every recognizer instance must be destroyed with its lifecycle owner.
- ML Kit model downloads must expose progress/failure and respect metered-network policy.
- Non-English translation quality limitations and on-device attribution must be disclosed where required.
- No shipping code may call `translate.google.com/m` or parse Google Translate HTML.
- Do not copy code from the AGPL-3.0 `vp-pdf` repository.
- Do not close Phase 3/5 physical-device gates using emulator-only evidence.
- A foreground service must promote before TTS, recognizer, translation, wake-lock, Bluetooth-helper, or controller initialization.
- Permission-result and UI callback ownership must survive Activity/Fragment recreation and reject stale generations.
- Every release-reachable queue and Binder snapshot must have count and byte bounds with recoverable overflow.
- No new physical APK handoff is allowed until the executable APK qualification gate passes on the available mandatory API 36 lane.

## Tasks

| Task | Scope | Exit gate |
|---|---|---|
| 9.1 | Stabilize launch/onboarding and correct Bluetooth capability handling | App reaches main UI without Cloud key; misleading branding/pricing removed; unsupported BLE state is non-fatal |
| 9.2 | Introduce engine contracts, capability model, and dependency boundaries | Existing Cloud path compiles behind interfaces; no behavior switch yet |
| 9.3 | Implement ML Kit translation and language-model management | Focused tests plus online-download/offline-use device proof |
| 9.4 | Implement Android SpeechRecognizer engine | API 23–36 guards, lifecycle cleanup, bounded utterances, error mapping tested |
| 9.5 | Integrate default engines into Conversation and WalkieTalkie | Both modes work without Cloud credentials on supported devices |
| 9.6 | Make legacy Cloud explicitly optional, finish migration UI/privacy copy, and close audit blockers `BUG-024`/`BUG-025` | Default journey contains no key/billing requirement; service restart and Settings dispatch regressions covered; no credential bundled |
| 9.7 | Full regression, device matrix, APK/release-candidate gate | Automated suite + physical two-phone evidence + PM approval |
| 9.8 | Remediate audit findings `BUG-027`–`BUG-030` before resuming the physical gate | State consistency, safe mode restore, exact service binding, serialized recent-peer persistence, focused regression tests |
| 9.9 | Remove production data logging and bound the Bluetooth ingress/image pipeline (`ENH-007`, `BUG-031`) | Reviewed local transport source has bounded reassembly; malformed/oversized input is dropped; release DEX contains no sensitive log tags |
| 9.10 | Correct the Android 12+ permission matrix and foreground-service launch contract (`BUG-013`, `BUG-015`) | Bluetooth-only permission requests on API 31+; prompt typed foreground promotion; lifecycle tests pass |
| 9.11 | Make asynchronous service connection initiation cancellable (`BUG-029`) | Fragment stop cancels pre-bind/late-bind work for both modes; failed starts/binds leave no orphan service or callback |
| 9.12 | Bound and coalesce recent-peer persistence (`BUG-030`) | Fixed-capacity serial work, deterministic rejection/failure behavior, and burst/Room-failure regression coverage |
| 9.13 | Add privacy-safe process-exit/lifecycle diagnostics and user export (`ENH-006`) | API 30+ exit reason plus bounded sanitized export; no telemetry or sensitive content |
| 9.14 | Make voice services promotion-first and permission ownership recreation-safe (`BUG-015`, `BUG-032`) | Foreground deadline, denial/grant/recreate, initialization failure, and teardown runtime tests pass |
| 9.15 | Make voice UI snapshot/callback delivery generation-safe (`BUG-034`) | Message-before-attributes and callback-after-destroy races are deterministic, lossless, and stale-safe |
| 9.16 | Bound runtime queues and Binder history (`BUG-035`) | Count/byte caps, backpressure, stress tests, and ≤128 KiB attribute snapshots pass |
| 9.17 | Add executable APK crash-regression qualification (`ENH-013`) | Fresh/upgrade permission and real-service lifecycle gate passes on API 36 before physical handoff |

## Phase acceptance criteria

- [ ] A fresh install can finish onboarding and enter the app without a Cloud account.
- [ ] At least one supported language pair completes speech → translation → TTS without a service-account key.
- [ ] A downloaded ML Kit model works in airplane mode.
- [ ] Missing/unsupported capabilities never terminate the foreground activity.
- [ ] Bluetooth pairing states are accurate and recoverable.
- [ ] API 23, 31, 34, and 36 checks are recorded; physical two-phone Bluetooth evidence exists.
- [x] `clean testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest` passes with zero lint errors on the available API 36 emulator.
- [ ] PM reviews the diff and test evidence before any tag, push, or release claim.
- [ ] Residual `BUG-015`/`BUG-032`, new `BUG-034`/`BUG-035`, and `ENH-006`/`ENH-013` are resolved through Tasks 9.13–9.17 before the two-phone Task 9.7 retest is accepted.
- [x] Release bytecode contains no Conversation payload/peer-identifier logging, and the active transport bounds incomplete and oversized peer-controlled input before reassembly.
- [ ] The API 36 APK qualification gate passes fresh-install and upgrade-with-persisted-state paths with no fatal, ANR, unexpected process death, or orphan voice service.

## Verification baseline

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain
.\gradlew.bat connectedDebugAndroidTest --console=plain
.\scripts\qa\voice-service-qualification.ps1 -Api 36 -RequireApi36
```

APK qualification is specified in Task 9.17. Manual two-phone tests remain in Task 9.7 and may resume only after 9.17 passes. Release signing remains an external maintainer gate.
