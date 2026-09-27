# Task 9.7 QA Evidence — 2026-09-27

## Decision

**PARTIAL / RELEASE NO-GO.** Gate A passes on the available API 36 emulator. Gates B, C, and D remain mandatory and blocked by missing API 23/31/34 runtime images or devices, two physical phones, human accessibility/product review, publisher/controller confirmation, release signing material, version approval, and explicit release approval.

Task 9.7 and Phase 9 remain `in_progress` at 6/7 tasks. No version bump, signed-release claim, completion tag, release tag, or publication is authorized by this evidence.

## Provenance

- Evidence time: 2026-09-27 13:29 ICT / 2026-09-27 06:29 UTC, with the final regression repeated after the discovered documentation fix.
- Branch: `master`.
- Planning baseline: `3db5ec19a2d98955b9754207272c3abd8b92c22a` (`mini-app-translator-vp-p9-t7`).
- QA defect fix: `3b16037315033510751f5f1f6350c3fe31d25448`.
- Fix scope: README capability wording plus a focused regression assertion; no shipping source/resource/Gradle/version/signing change.
- Pre-gate repository state: planning baseline synchronized with `origin/master`, ahead/behind `0/0`, with only the pre-existing untracked `.viepilot/debug/`.

## Environment inventory

- Host Android SDK: `C:\Users\Admin\AppData\Local\Android\Sdk`.
- Attached device: `emulator-5554`, AVD `Pixel_7a`, model `sdk_gphone16k_x86_64`, Android 16 / API 36.
- Available AVD names: `Pixel_7a`, `medium_phone`; both installed system-image families are API 36 (`google_apis_playstore/x86_64` and `google_apis_ps16k/x86_64`).
- Android platform directories exist for API 23, 29, 31, 34, 36, and 36.1, but platform directories are not runnable devices. No API 23/31/34 system image or attached device was available.
- No physical Android phone was attached.
- No `.jks`, `.keystore`, or `.p12` release signing file and no active Gradle release signing configuration was found. Commented debug-keystore examples in `gradle.properties` are not release signing.

## Gate A — PASS

Final commands after the QA fix:

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
git diff --check
```

Results:

- JVM: 143 tests, 0 failures, 0 errors, 0 skipped.
- Instrumentation: 19 tests, 0 failures, 0 errors, 0 skipped on Pixel 7a API 36.
- Lint: 0 errors, 136 warnings.
- `git diff --check`: clean.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- APK size: 79,115,707 bytes.
- APK SHA-256: `BD98BB5C24BA08DE8287A899449C0C084457058A681E6ADCC2AF9F0A7F5BFD2A`.

The instrumentation suite includes production on-device engine composition/lifecycle, Android SpeechRecognizer behavior, model prepare/translate/delete, a separate translation using an already downloaded model without another download, model-management UI opening, credential encryption/migration/delete, and database/service lifecycle coverage.

### API 36 runtime smoke

- Definitive fresh install: `pm uninstall --user 0` returned `Success`; package absence was verified; streamed APK install returned `Success`.
- Credential-free cold launch reached `.access.AccessActivity`; no FATAL or app ANR matched logcat.
- Force-stop/relaunch created a new process and returned to `.access.AccessActivity`; no FATAL or app ANR matched logcat.
- `pm clear` fresh-data launch succeeded without a credential.
- Runtime permission cycle for `RECORD_AUDIO`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, and `NEARBY_WIFI_DEVICES`: denied launch, grant launch, revoke launch, and re-grant launch all produced no FATAL/app ANR. The emulator was restored to fresh-install denied permissions after this cycle.
- Light and dark initial Notice screen smokes at font scale 2.0 launched without crash. Visual inspection found the required notice text and Forward control visible without clipping on that screen. Font scale was restored to 1.0 and night mode to `auto`.
- TalkBack is installed on the emulator, but navigation, spoken labels, focus order, and the complete 200% font screen matrix require human observation and therefore remain Gate C, not PASS.

### Source/content guard

- No `new Translator`, `new Recognizer`, or `new Recorder` construction exists in the two default Conversation/Walkie service files.
- No active `$300`/Cloud-credit claim was found in app resources, README, or privacy files.
- Active `app_name` remains `Mini Conversation`; README occurrences of RTranslator are upstream attribution, not runtime branding.
- QA found stale README wording that incorrectly listed Bluetooth Low Energy as a Conversation requirement. Commit `3b16037` replaces it with the actual Bluetooth/Nearby-permission prerequisite and adds a regression assertion to `MigrationUxContractTest`.
- Post-fix scans find no exact obsolete `Bluetooth Low Energy cho Conversation mode` statement.

## Gate B — BLOCKED/PARTIAL

| API | Environment | Result |
|---|---|---|
| 23 | No attached device or installed system image | BLOCKED — not run |
| 31 | No attached device or installed system image | BLOCKED — not run |
| 34 | No attached device or installed system image | BLOCKED — not run |
| 36 | Pixel 7a AVD / Android 16 | PASS for automated tests, fresh install, relaunch, force-stop recreation, and permission-cycle crash smoke |

Installing additional large SDK images was not implicitly authorized. A complete supported-version PASS requires recorded runs on API 23, 31, and 34.

## Gate C — BLOCKED/PARTIAL

- PASS on API 36 emulator: on-device engine instrumentation; ML Kit model prepare/translate/delete; already-downloaded-model translation; initial-screen light/dark/200% font crash/clip smoke.
- BLOCKED: two physical phones for discovery, permission recovery, connect/disconnect/reconnect, background/foreground, Conversation exchange, Bluetooth/SCO audio routing, and alternating WalkieTalkie turns.
- BLOCKED: physical microphone/speech quality and recognizer present/absent behavior.
- BLOCKED: human TalkBack navigation/labels/focus order and complete-screen 200% font/adaptive-icon/splash review.
- BLOCKED: physical low-storage and user-driven model download failure/cancel/delete behavior.

## Gate D — BLOCKED

- Publisher/data-controller identity, privacy contact, jurisdiction wording, and legal approval have not been supplied/approved.
- Production signing material/configuration is unavailable; no signing key was generated or replaced.
- `versionName` remains the existing NO-GO candidate `1.2.0` with `versionCode 15`; target `1.3.0` has not been assigned.
- No signed artifact, certificate fingerprint, release tag, publication, or final release approval exists.

## Required continuation

1. Provide or authorize runnable API 23, 31, and 34 devices/system images.
2. Connect two physical Android phones with USB debugging for the Bluetooth/SCO and speech/product matrix.
3. Perform the human TalkBack/200% font/light-dark/icon/splash review on representative physical hardware.
4. Supply/approve publisher-controller identity and privacy contact for legal review.
5. Supply/authorize release signing and explicitly approve version assignment and release actions only after Gates B/C pass.
