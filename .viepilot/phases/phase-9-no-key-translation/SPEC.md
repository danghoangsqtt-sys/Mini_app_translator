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

## Tasks

| Task | Scope | Exit gate |
|---|---|---|
| 9.1 | Stabilize launch/onboarding and correct Bluetooth capability handling | App reaches main UI without Cloud key; misleading branding/pricing removed; unsupported BLE state is non-fatal |
| 9.2 | Introduce engine contracts, capability model, and dependency boundaries | Existing Cloud path compiles behind interfaces; no behavior switch yet |
| 9.3 | Implement ML Kit translation and language-model management | Focused tests plus online-download/offline-use device proof |
| 9.4 | Implement Android SpeechRecognizer engine | API 23–36 guards, lifecycle cleanup, bounded utterances, error mapping tested |
| 9.5 | Integrate default engines into Conversation and WalkieTalkie | Both modes work without Cloud credentials on supported devices |
| 9.6 | Make legacy Cloud explicitly optional and finish migration UI/privacy copy | Default onboarding contains no key/billing requirement; no credential bundled |
| 9.7 | Full regression, device matrix, APK/release-candidate gate | Automated suite + physical two-phone evidence + PM approval |

## Phase acceptance criteria

- [ ] A fresh install can finish onboarding and enter the app without a Cloud account.
- [ ] At least one supported language pair completes speech → translation → TTS without a service-account key.
- [ ] A downloaded ML Kit model works in airplane mode.
- [ ] Missing/unsupported capabilities never terminate the foreground activity.
- [ ] Bluetooth pairing states are accurate and recoverable.
- [ ] API 23, 31, 34, and 36 checks are recorded; physical two-phone Bluetooth evidence exists.
- [ ] `clean testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest` passes with zero lint errors.
- [ ] PM reviews the diff and test evidence before any tag, push, or release claim.

## Verification baseline

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain
.\gradlew.bat connectedDebugAndroidTest --console=plain
```

Manual tests are specified in Task 9.7. Release signing remains an external maintainer gate.

