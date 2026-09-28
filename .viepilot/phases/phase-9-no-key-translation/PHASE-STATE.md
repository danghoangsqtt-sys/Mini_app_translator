# Phase State — Phase 9: No-Key On-Device Translation

- **Status**: in_progress
- **Tasks**: 10/12 complete
- **Current task**: 9.12 — Bounded recent-peer persistence (`in_progress`)
- **Created**: 2026-09-23 via `/vp-brainstorm` → `/vp-crystallize` → `/vp-evolve`
- **Target**: `1.3.0`; no versionCode change until a release candidate is approved
- **PM**: current task owner
- **Coder**: Codex (`vp-auto` inline execution)

| Task | Status | Control point |
|---|---|---|
| 9.1 — Launch/onboarding/Bluetooth stabilization | done — PM automated/emulator PASS | Physical-phone confirmation consolidated into 9.7 |
| 9.2 — Engine contracts and capability model | done — PM PASS | `87e9875`; 66 JVM tests; lint 0/136; explicit factories, session isolation, no runtime switch |
| 9.3 — ML Kit translation/model management | done — PM PASS | `384b114`; 99 JVM tests; 10 API 36 tests; lint 0/136; genuine post-relaunch offline translation proof |
| 9.4 — Android SpeechRecognizer engine | done — PM PASS | `83fdeb2`; 122 JVM tests; 16 API 36 tests; lint 0/136; bounded lifecycle and exact-once cleanup |
| 9.5 — Conversation/WalkieTalkie integration | done — PM automated/emulator PASS | `ab6094c`; 137 JVM tests; 19 API 36 tests; lint 0/136; ON_DEVICE default with explicit Walkie source direction |
| 9.6 — Legacy Cloud opt-in and migration UX | done — PM automated/emulator PASS | `8a8882b`; 143 JVM, 19 API 36 tests, lint 0/136; `BUG-024`/`BUG-025` resolved |
| 9.7 — Full QA and release-candidate gate | blocked by 9.9–9.12; release NO-GO | Resume only after the deep-audit blockers pass; then same two phones must confirm the complete fix set |
| 9.8 — Audit stabilization hotfixes | done — PM automated/API 36 PASS | `ce265bd`; 154 JVM, 21 API 36 instrumentation, lint 0/136; persisted at `origin/master` through `5c9c70e`; 9.7 physical gate resumed |
| 9.9 — Production privacy and bounded Bluetooth ingress | done — automated/API 36/release PASS | `6ff92b8`; 163 JVM, 23 instrumentation, lint 0/119; R8 DEX privacy gate PASS; physical interoperability remains 9.7 |
| 9.10 — Permission and foreground-service lifecycle correction | done — automated/API 36 PASS | `cc08c4f`; 166 JVM, 23 instrumentation, lint 0/122; physical API/device matrix remains 9.7 |
| 9.11 — Cancellable asynchronous service binding | done — automated/API 36 PASS | `066002c`; 174 JVM, 24 instrumentation, lint 0/122; exact pending/active ownership |
| 9.12 — Bounded recent-peer persistence | in_progress | `BUG-030`; bounded/coalescing serial work with visible deterministic failures |

## PM control rules

- Terra may implement only the currently assigned task.
- No push, merge, release tag, phase-complete tag, worktree deletion, or version bump without explicit PM instruction.
- Commit only files belonging to the task; pre-existing `.viepilot/debug/` and brainstorm changes are PM-owned.
- A task is not PASS merely because Gradle succeeds; PM must review the diff and evidence.

## Task 9.1 evidence

- **Implementation commit**: `8a0a97a8627f2839487cf56a3cfba8f63f781092`
- **JVM tests**: 54 passed, 0 failures/errors.
- **Lint**: 0 errors, 136 warnings; no new suppression and below the 139-warning pre-task baseline.
- **Instrumentation**: 7/7 tests passed on Pixel 7a API 36 emulator.
- **Runtime smoke**: fresh install; keyless Notice/Profile; Nearby Devices deny path; grant/relaunch path; pairing search; no crash/ANR/FATAL.
- **APK**: `app/build/outputs/apk/debug/app-debug.apk` produced successfully.
- **Deferred manual gate**: physical phone and two-phone Bluetooth/SCO evidence remains mandatory in Task 9.7.

## Task 9.2 evidence

- **Implementation commit**: `87e9875b7d5469f59340f654f0e682c09aff114b`, persisted to `origin/master`.
- **Scope proof**: exactly 18 new files under the engine boundary and its JVM tests; existing consumers and credential/build/resource files untouched.
- **PM review**: required session-bound recognizer callbacks, cancel/close cleanup, explicit PCM versus engine-capture semantics, and thread-safe output close before PASS.
- **Verification**: 66 JVM tests passed; lint 0 errors/136 warnings; debug APK 11,183,277 bytes.
- **Behavior**: no runtime selection change; legacy translation cancellation remains callback-only because the existing HTTP request is not abortable.

## Task 9.3 evidence

- **Implementation commit**: `384b114ec22ed8a5137072d501676dd4c032fa46`.
- **Dependencies**: official `com.google.mlkit:translate:17.0.3` and bundled `com.google.mlkit:language-id:17.0.6`; no API key, Google Services plugin, Firebase, or Cloud fallback.
- **Verification**: 99 JVM tests passed; 10 Pixel 7a API 36 instrumentation tests passed; lint 0 errors/136 warnings; debug APK 79,095,051 bytes, SHA-256 `E3CDB5263CC0D84914F11D644D81300D8F3384788C6FE45FD758FBC11002DEA7`.
- **Offline proof**: app/test APKs were installed once; Italian was prepared online; app and test packages were force-stopped; airplane mode was enabled and Wi-Fi/mobile data disabled until no route existed and ping returned `Network is unreachable`; the separate translate-without-download test passed in 0.632 seconds. Network was restored and both cellular and Wi-Fi networks returned `VALIDATED`.
- **Scope**: Settings-only model management; Conversation, WalkieTalkie, runtime selection, credentials, application ID, and version remain unchanged.

## Task 9.4 evidence

- **Implementation commit**: `83fdeb250445a0a316cbb45a61c3f5aef15d027d`.
- **Verification**: 122 JVM tests passed; 16 Pixel 7a API 36 instrumentation tests passed with no skips; lint 0 errors/136 warnings; `git diff --check` clean.
- **APK**: 79,095,115 bytes, SHA-256 `E62AA75200F83C024F125AC753F2121AA133E00F2944701990363F7345EF37A8`.
- **Lifecycle proof**: main-thread platform calls, one recognizer per session, stale/late callback suppression, 30-second watchdog removal, close/cancel race coverage, no retry after listening starts, and exact-once cleanup including teardown exceptions.
- **Scope**: exactly 12 locked files changed; no Gradle, resource, UI/service, credential, ML Kit, contract, version, or runtime-selection change.
- **Limitations**: the system recognizer may use network and may ignore the offline preference; truthful synchronous language enumeration is unavailable; runtime integration remains Task 9.5.

## Task 9.5 evidence

- **Implementation commit**: `ab6094c71e8b6f4415185fedbb741bdbb77e2b0d`, persisted to `origin/master`; parent/planning baseline `faf12f1c06c6c8b07cec9c0314e264caef72ebfa`.
- **Scope**: exactly 25 locked application/resource/test files; 1,122 insertions and 601 deletions; no Gradle, manifest, credentials, legacy Cloud adapter, Bluetooth transport, core engine contract, app identity/version, or implementation-time ViePilot state change.
- **Verification**: 137 JVM tests and 19 Pixel 7a API 36 instrumentation tests passed with no failures/errors/skips; lint 0 errors/136 warnings; `git diff --check` clean.
- **APK**: 79,115,541 bytes, SHA-256 `0439A7DED87D7AE7F9C33E97AD8E6E604207C0C277B82EA5C71DDF0168FD83B9`.
- **Behavior**: Conversation and WalkieTalkie now explicitly compose ON_DEVICE engines; each tap owns one bounded recognition turn; Walkie uses a persistent explicit source direction; incoming Conversation translation is FIFO-isolated; payload framing remains compatible; terminal/generation guards suppress duplicate, stale, and post-close work; one service TTS instance remains shared.
- **Persistence gate**: remote `refs/heads/master`, `origin/master`, upstream, and `HEAD` all resolve to `ab6094c`; ahead/behind `0/0`. PM-owned untracked `.viepilot/debug/` remains untouched.
- **Deferred**: legacy Cloud opt-in/migration is Task 9.6; physical speech/offline quality and two-phone Bluetooth/SCO evidence remain Task 9.7.

## Task 9.6 evidence

- **Planning baseline / implementation**: `fb18fec` / `8a8882b`, persisted to `origin/master`.
- **Scope**: 18 files; narrow Settings/service fixes, EN/IT migration copy/privacy, current architecture docs, and two focused source-contract test classes. No engine/controller/Bluetooth/Gradle/version/signing change.
- **Behavior**: ordinary Settings no longer constructs the legacy Cloud translator; legacy credential management is visibly optional/advanced and cannot select runtime automatically; null service restarts stop non-sticky before reading extras; missing-TTS dispatch uses the correct key and no longer falls through.
- **Verification**: 143 JVM tests and 19 Pixel 7a API 36 instrumentation tests passed; lint 0 errors/136 warnings; `git diff --check` clean.
- **APK**: 79,115,707 bytes; SHA-256 `BD98BB5C24BA08DE8287A899449C0C084457058A681E6ADCC2AF9F0A7F5BFD2A`.
- **Release boundary**: technical privacy disclosure is current, but publisher/controller identity and legal approval remain human gates. Task 9.7 still owns physical API/device, two-phone Bluetooth/SCO, TalkBack, signing, version/tag, and release approval.

## Task 9.7 partial evidence

- **Planning baseline / QA fix**: `3db5ec1` / `3b16037`; the only fix removes the stale README claim that Bluetooth Low Energy is required and adds a focused regression guard.
- **Physical finding / hotfix**: both real phones connected then entered a reproducible Android 12+ crash loop because the service notification `PendingIntent` lacked a mutability flag. `BUG-026` is fixed in code at `24ee245` with `FLAG_IMMUTABLE` and focused instrumentation; same-device retest is pending.
- **Gate A**: 143 JVM tests and 20 Pixel 7a API 36 instrumentation tests passed; lint 0 errors/136 warnings; persisted-Conversation relaunch with denied/granted permissions no longer produced FATAL.
- **APK**: 79,115,725 bytes; SHA-256 `D80109A70D47DF99B35D94CBB20C9A37ACF5DD1603F3D8876B8DB4CA83AC560D`.
- **Release NO-GO**: API 23/31/34 lack runnable devices/images; two-phone hotfix/Conversation/Bluetooth-SCO, physical speech, complete TalkBack/visual QA, controller identity/legal approval, release signing, version assignment, signed artifact, tag, and publication remain incomplete.
- **Canonical evidence**: `evidence/TASK-9.7-QA.md`.

## Task 9.8 implementation evidence

- **Planning / implementation**: `c2bc8fe` / `ce265bd`; verification state `5c9c70e`; fast-forward persisted to `origin/master`. No tag/version/signing change.
- **Requests**: `BUG-027` state drift resolved; `BUG-028` stale mode restore, `BUG-029` binding ownership, and `BUG-030` recent-peer ordering are code/emulator resolved pending physical Task 9.7 regression.
- **Verification**: 154 JVM tests and 21 Pixel 7a API 36 instrumentation tests passed; lint 0 errors/136 warnings; `git diff --check` clean.
- **APK**: 79,115,713 bytes; SHA-256 `0842D8B157148801110D3AACEEC1B1CFAADA3B9896C8015CB5551F32064E42C7`.
- **Persistence gate**: implementation/evidence is persisted through `5c9c70e`; the final closeout state is also pushed with `HEAD == origin/master`, ahead/behind `0/0`. Task 9.8 is done and control returns to 9.7. Physical two-phone confirmation remains mandatory for release.

## Task 9.10 implementation evidence

- **Planning / implementation**: local task baseline tag `mini-app-translator-vp-p9-t10`; implementation `cc08c4f`. No push, release tag, version, signing, or physical-device claim.
- **Permissions**: API 31+ requests exactly Bluetooth scan/connect/advertise; API 29–30 retains fine location; API 23–28 retains coarse location. Unused Wi-Fi, Nearby Wi-Fi, and background-location declarations were removed.
- **Foreground lifecycle**: both modes launch with `ContextCompat.startForegroundService`; the base service promotes in `onStartCommand`, contains start failures, stays foreground across bind/rebind, and removes its notification on teardown. Conversation uses microphone plus connected-device types; Walkie uses microphone only.
- **Verification**: 166 JVM tests and 23 Pixel 7a API 36 instrumentation tests passed; lint 0 errors/122 warnings; `git diff --check` clean; debug APK assembled. API 23/31/34 and two-phone permission/Bluetooth confirmation remain Task 9.7.

## Task 9.11 implementation evidence

- **Planning / implementation**: local baseline tag `mini-app-translator-vp-p9-t11`; implementation `066002c`. No push, version, signing, or release action.
- **Ownership**: each Fragment receives an immediate idempotent handle instead of a communicator ID `0`; cancellation clears Fragment callbacks and covers pending language lookup, foreground start, framework bind, registered connection, and late framework delivery.
- **Cleanup**: bind and registration are atomic against cancellation; only registered work is unbound; false/null/dead binding and canceled pre-connect starts release exact ownership and stop an otherwise orphan foreground service.
- **Verification**: 174 JVM tests, 24 Pixel 7a API 36 instrumentation tests, lint 0 errors/122 warnings, debug assembly PASS, and clean `git diff --check`. The new device test proves exact-once unbind and suppression of a late `onServiceConnected` callback.
