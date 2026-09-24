# Phase State — Phase 9: No-Key On-Device Translation

- **Status**: in_progress
- **Tasks**: 3/7 complete
- **Current task**: 9.4 — Android SpeechRecognizer engine (`in_progress`; strict contract locked and assigned to TERRA 5.6)
- **Created**: 2026-09-23 via `/vp-brainstorm` → `/vp-crystallize` → `/vp-evolve`
- **Target**: `1.3.0`; no versionCode change until a release candidate is approved
- **PM**: current task owner
- **Coder**: TERRA 5.6

| Task | Status | Control point |
|---|---|---|
| 9.1 — Launch/onboarding/Bluetooth stabilization | done — PM automated/emulator PASS | Physical-phone confirmation consolidated into 9.7 |
| 9.2 — Engine contracts and capability model | done — PM PASS | `87e9875`; 66 JVM tests; lint 0/136; explicit factories, session isolation, no runtime switch |
| 9.3 — ML Kit translation/model management | done — PM PASS | `384b114`; 99 JVM tests; 10 API 36 tests; lint 0/136; genuine post-relaunch offline translation proof |
| 9.4 — Android SpeechRecognizer engine | in_progress | Strict contract locked 2026-09-24; implementation assigned to TERRA 5.6; PM review required |
| 9.5 — Conversation/WalkieTalkie integration | planned | Requires 9.3 and 9.4 |
| 9.6 — Legacy Cloud opt-in and migration UX | planned | Requires 9.5 |
| 9.7 — Full QA and release-candidate gate | planned | Requires 9.1–9.6 |

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
