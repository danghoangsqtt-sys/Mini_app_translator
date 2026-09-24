# Phase State — Phase 9: No-Key On-Device Translation

- **Status**: in_progress
- **Tasks**: 1/7 complete
- **Current task**: 9.2 — Engine contracts and capability model (`in_progress`; contract locked for Terra)
- **Created**: 2026-09-23 via `/vp-brainstorm` → `/vp-crystallize` → `/vp-evolve`
- **Target**: `1.3.0`; no versionCode change until a release candidate is approved
- **PM**: current task owner
- **Coder**: TERRA 5.6

| Task | Status | Control point |
|---|---|---|
| 9.1 — Launch/onboarding/Bluetooth stabilization | done — PM automated/emulator PASS | Physical-phone confirmation consolidated into 9.7 |
| 9.2 — Engine contracts and capability model | in_progress | PM contract locked; implementation restricted to new engine-boundary files |
| 9.3 — ML Kit translation/model management | planned | Requires 9.2 |
| 9.4 — Android SpeechRecognizer engine | planned | Requires 9.2; may proceed after 9.3 review |
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
