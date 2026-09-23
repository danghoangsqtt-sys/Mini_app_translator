# Phase State — Phase 9: No-Key On-Device Translation

- **Status**: in_progress
- **Tasks**: 0/7 complete
- **Current task**: 9.1 — Stabilize launch/onboarding and Bluetooth capability handling
- **Created**: 2026-09-23 via `/vp-brainstorm` → `/vp-crystallize` → `/vp-evolve`
- **Target**: `1.3.0`; no versionCode change until a release candidate is approved
- **PM**: current task owner
- **Coder**: TERRA 5.6

| Task | Status | Control point |
|---|---|---|
| 9.1 — Launch/onboarding/Bluetooth stabilization | in_progress | Stop after local implementation commit and evidence report |
| 9.2 — Engine contracts and capability model | planned | Requires PM acceptance of 9.1 |
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

