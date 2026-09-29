# Task 9.7 QA Evidence — 2026-09-27, updated 2026-09-29

## Decision

**PARTIAL / RELEASE NO-GO.** Gate A passes on the available API 36 emulator. Two physical attempts exposed `BUG-026` and then `BUG-032`; both now have deterministic API 36 reproductions and code/emulator fixes, but the latest recovery still requires confirmation on the same two phones. Gates B, C, and D otherwise remain incomplete due missing API 23/31/34 runtime images or devices, human accessibility/product review, publisher/controller confirmation, release signing material, version approval, and explicit release approval.

The 2026-09-29 physical retest failed again: Conversation terminated immediately after connection, and a fresh-install WalkieTalkie activation also terminated then replayed on relaunch. API 36 deterministically reproduced `BUG-032`: with `RECORD_AUDIO` denied, the app called `startForegroundService()` but deliberately skipped `startForeground()`, causing `ForegroundServiceDidNotStartInTimeException`. Recovery commit `a9d04d7` now requests microphone permission before service launch, makes denial recoverable, and prevents both foreground-service starts when their declared-type permissions are absent. `BUG-033` is resolved with the supplied transparent icon. Code/API 36 evidence is PASS; no emulator result overrides the still-required physical retest.

Task 9.7 and Phase 9 remain `in_progress` at 11/12 tasks. No version bump, signed-release claim, completion tag, release tag, push, or publication is authorized by this evidence.

## Provenance

- Evidence time: 2026-09-27 13:29 ICT / 2026-09-27 06:29 UTC, with the final regression repeated after the discovered documentation fix.
- Branch: `master`.
- Planning baseline: `3db5ec19a2d98955b9754207272c3abd8b92c22a` (`mini-app-translator-vp-p9-t7`).
- QA defect fix: `3b16037315033510751f5f1f6350c3fe31d25448`.
- Fix scope: README capability wording plus a focused regression assertion; no shipping source/resource/Gradle/version/signing change.
- Physical-device crash hotfix: `24ee245e3cb9ceb5c3bf605f15e823f132d5437e` (`BUG-026`).
- Hotfix scope: add `FLAG_IMMUTABLE` to the Conversation/Walkie foreground-notification `PendingIntent` and add an API 36 instrumentation regression; no resource/Gradle/version/signing change.
- 2026-09-29 incident planning/state commit: `3b10ccba48a52cb7ea2ff7f989b5dda0695e30f2`.
- 2026-09-29 permission/icon recovery: `a9d04d73621276f7d5390d25e9e2ec49f21e136e` (`BUG-032`, `BUG-033`).
- Recovery scope: caller-side foreground-service permission gates, fragment grant/denial retry lifecycle, null-safe pre-service teardown, exact supplied Walkie icon/resource labels, and focused JVM contracts; no Gradle, manifest, version, signing, tag, or push action.
- Pre-gate repository state: planning baseline synchronized with `origin/master`, ahead/behind `0/0`, with only the pre-existing untracked `.viepilot/debug/`.

## Environment inventory

- Host Android SDK: `C:\Users\Admin\AppData\Local\Android\Sdk`.
- Attached device: `emulator-5554`, AVD `Pixel_7a`, model `sdk_gphone16k_x86_64`, Android 16 / API 36.
- Available AVD names: `Pixel_7a`, `medium_phone`; both installed system-image families are API 36 (`google_apis_playstore/x86_64` and `google_apis_ps16k/x86_64`).
- Android platform directories exist for API 23, 29, 31, 34, 36, and 36.1, but platform directories are not runnable devices. No API 23/31/34 system image or attached device was available.
- No physical Android phone was attached.
- No `.jks`, `.keystore`, or `.p12` release signing file and no active Gradle release signing configuration was found. Commented debug-keystore examples in `gradle.properties` are not release signing.

## Gate A — PASS

Latest clean commands after the 2026-09-29 recovery:

```powershell
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug verifyReleasePrivacy --console=plain --no-daemon
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
git diff --check
```

Results:

- JVM: 189 tests, 0 failures, 0 errors, 0 skipped.
- Instrumentation: 24 tests, 0 failures, 0 errors, 0 skipped on Pixel 7a API 36.
- Lint: 0 errors, 122 warnings.
- `git diff --check`: clean.
- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.
- APK size: 79,279,141 bytes.
- APK SHA-256: `2DF29D15487D3EEE730B5EA0678AB0D9131436265913DFD4F0FD331F6C35E619`.
- Unsigned release APK: 71,003,381 bytes; SHA-256 `605E5B375A19BC7A8D936F42A4F731D2A84BEEE9EA13FDD38433DDFDD62F0C5E`.
- Release R8 DEX privacy verification: PASS.

The instrumentation suite includes production on-device engine composition/lifecycle, Android SpeechRecognizer behavior, model prepare/translate/delete, a separate translation using an already downloaded model without another download, model-management UI opening, credential encryption/migration/delete, and database/service lifecycle coverage.

### API 36 runtime smoke

- Definitive fresh install: `pm uninstall --user 0` returned `Success`; package absence was verified; streamed APK install returned `Success`.
- Credential-free cold launch reached `.access.AccessActivity`; no FATAL or app ANR matched logcat.
- Force-stop/relaunch created a new process and returned to `.access.AccessActivity`; no FATAL or app ANR matched logcat.
- `pm clear` fresh-data launch succeeded without a credential.
- Runtime permission cycle for `RECORD_AUDIO`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, and `NEARBY_WIFI_DEVICES`: denied launch, grant launch, revoke launch, and re-grant launch all produced no FATAL/app ANR. The emulator was restored to fresh-install denied permissions after this cycle.
- Light and dark initial Notice screen smokes at font scale 2.0 launched without crash. Visual inspection found the required notice text and Forward control visible without clipping on that screen. Font scale was restored to 1.0 and night mode to `auto`.
- TalkBack is installed on the emulator, but navigation, spoken labels, focus order, and the complete 200% font screen matrix require human observation and therefore remain Gate C, not PASS.
- Pre-fix reproduction: forcing persisted preference `fragment=1` on API 36 produced a main-thread FATAL `IllegalArgumentException` at `VoiceTranslationActivity.buildNotification`: target S+ requires `FLAG_IMMUTABLE` or `FLAG_MUTABLE`. This matches the physical sequence because connection success stores the Conversation fragment before its service notification is built.
- Post-fix reproduction: the same persisted-Conversation launch remained alive without FATAL both before and after granting microphone/Nearby permissions. The focused notification test and full 20-test instrumentation suite passed.
- `BUG-032` pre-fix reproduction: persisted WalkieTalkie plus revoked `RECORD_AUDIO` terminated after the foreground-service deadline with `ForegroundServiceDidNotStartInTimeException` originating from `VoiceTranslationActivity.startWalkieTalkieService()`.
- `BUG-032` denied-path recovery: the updated APK displayed the microphone request before service start, remained alive beyond nine seconds with no service record or FATAL, returned to Pairing after denial, and survived a force-stop/relaunch without re-entering WalkieTalkie. Exercising denial also exposed a null-communicator `ButtonMic.deactivate()` crash, which was fixed and re-run successfully.
- `BUG-032` granted-path recovery: after granting `RECORD_AUDIO`, the Walkie action started `.WalkieTalkieService` as a foreground service with type `0x80` (microphone), remained alive beyond the deadline, and logged no FATAL.
- `BUG-033` visual/resource recovery: the bottom-right Pairing action visibly renders the reporter-supplied Walkie icon through `srcCompat`; the packaged/source PNG bytes match, retain alpha, and the old opaque drawable is removed.

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

## Gate C — FAILED TWICE; LATEST CODE/API 36 RECOVERY PASS, PHYSICAL RETEST REQUIRED

- PASS on API 36 emulator: on-device engine instrumentation; ML Kit model prepare/translate/delete; already-downloaded-model translation; initial-screen light/dark/200% font crash/clip smoke.
- FAIL on the first two-phone attempt: both phones discovered each other and connected, then both apps terminated immediately; reopening repeated the termination loop.
- Root cause reproduced: `TaskStackBuilder.getPendingIntent` was called with only `FLAG_UPDATE_CURRENT` while targeting SDK 36. Android 12+ throws before Conversation service startup, and the persisted Conversation fragment re-entered that crash on every relaunch.
- Code/emulator hotfix: commit `24ee245` adds `FLAG_IMMUTABLE`; deterministic API 36 reproduction and regression pass. The same two phones must install the new APK and confirm connect, relaunch recovery, disconnect/reconnect, Conversation exchange, and Bluetooth/SCO before this gate can pass.
- FAIL on the second two-phone attempt: Conversation again terminated immediately after connection; after reinstall, WalkieTalkie activation also terminated and persisted into a relaunch loop. The bottom-right Walkie action rendered no visible glyph.
- Root cause reproduced for the second attempt: both modes could call `startForegroundService()` before microphone permission was granted. The base service then declined foreground promotion, leaving Android's deadline armed until `ForegroundServiceDidNotStartInTimeException`. The old Walkie PNG was an opaque white rectangle.
- Latest code/API 36 recovery: commit `a9d04d7` adds caller and Fragment permission gates, safe denial recovery, and the supplied transparent icon. Denied/granted/relaunch smokes plus the full automated gate pass. The same two phones must confirm this artifact before either defect can be accepted for Gate C.
- BLOCKED: physical microphone/speech quality and recognizer present/absent behavior.
- BLOCKED: human TalkBack navigation/labels/focus order and complete-screen 200% font/adaptive-icon/splash review.
- BLOCKED: physical low-storage and user-driven model download failure/cancel/delete behavior.

## Gate D — BLOCKED

- Publisher/data-controller identity, privacy contact, jurisdiction wording, and legal approval have not been supplied/approved.
- Production signing material/configuration is unavailable; no signing key was generated or replaced.
- `versionName` remains the existing NO-GO candidate `1.2.0` with `versionCode 15`; target `1.3.0` has not been assigned.
- No signed artifact, certificate fingerprint, release tag, publication, or final release approval exists.

## Required continuation

> **Current physical-retest candidate (2026-09-29):** debug APK 79,279,141 bytes, SHA-256 `2DF29D15487D3EEE730B5EA0678AB0D9131436265913DFD4F0FD331F6C35E619`. It includes the foreground-service permission recovery and supplied Walkie icon at implementation `a9d04d7`. Automated gate: 189 JVM tests, 24 API 36 instrumentation tests, lint 0 errors/122 warnings, debug/unsigned release assembly and R8 privacy PASS. Commits remain local; no push was authorized.

1. Install the debug APK with SHA-256 `2DF29D15487D3EEE730B5EA0678AB0D9131436265913DFD4F0FD331F6C35E619` on both phones. Grant microphone when first entering a voice mode, verify the bottom-right Walkie icon, then repeat Conversation connection and Walkie activation. If either app terminates, capture sanitized logs from both phones before reopening.
2. Continue two-phone Conversation text/audio, Bluetooth/SCO, WalkieTalkie direction, permission recovery, and offline-model checks after connection remains stable.
3. Provide or authorize runnable API 23, 31, and 34 devices/system images.
4. Perform the human TalkBack/200% font/light-dark/icon/splash review on representative physical hardware.
5. Supply/approve publisher-controller identity and privacy contact for legal review, then authorize release signing/version actions only after Gates B/C pass.
