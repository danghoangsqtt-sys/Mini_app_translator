# Phase 8 — Release Readiness & Project Closure

**Mode**: Refactor / release hardening
**Target release**: Mini Conversation `1.2.0` (`versionCode 15`)
**Depends on**: Phase 7 delivery accepted; physical Android test devices/emulators; external signing material
**Future feature**: Wi-Fi Hotspot transport remains out of scope and is reserved for Phase 9 (`ENH-020`).

## Goal

Turn the current buildable `master` into an auditable, reproducible, signed and device-validated `1.2.0` release. Close the outstanding Phase 3 and Phase 5 gates, finish the deferred dependency and lint work, establish one repository source of truth, persist it to `origin`, and only then archive obsolete worktrees.

## Entry gates

- Phase 7 (`BUG-017` through `BUG-020`) has a reviewed delivery commit or a documented partial-delivery disposition.
- `master` is the only branch authorized for release integration; do not merge the stale `codex/phase3-android-modernization` worktree wholesale.
- The pre-refresh baseline passes `testDebugUnitTest`, `lintDebug`, and `assembleDebug`.
- A maintainer provides the human-only prerequisites: at least two physical Android phones for Bluetooth/SCO testing and an external release keystore/signing process. Secrets must not enter Git.

## Scope

### In scope

- Accept and independently verify the Phase 7 delivery.
- Decide and clean repository-only clutter (`.agents/skills/`, non-shared `.idea` files, stale worktrees) without deleting user data blindly.
- Capture a real-device baseline before dependency migration.
- Execute Phase 3 Task 3.3 dependency refresh incrementally (`ENH-012`) with rollback points.
- Finish Phase 3 Task 3.6 by making lint/release checks genuinely blocking.
- Complete Phase 3 Bluetooth/SCO and Phase 5 visual/accessibility/device QA.
- Produce, install and verify a signed release artifact.
- Synchronize final docs/state, create completion/release tags, and push the canonical branch/tags to `origin` after review.

### Out of scope

- Wi-Fi Hotspot, Wi-Fi Direct, transport abstraction, or any `1.3.0` feature.
- Java package/applicationId migration.
- Broad rewrites to Compose, Kotlin, database architecture, or networking behavior unrelated to dependency compatibility.
- Closing low-priority backlog items without an explicit release-blocking finding.

## Execution order

| Task | Description | Owner/gate |
|---|---|---|
| 8.1 | Accept Phase 7 delivery and freeze the canonical baseline | Automated + reviewer |
| 8.2 | Resolve repository source-of-truth and hygiene decisions | Maintainer decision required for deletion/untracking |
| 8.3 | Capture pre-refresh device and Bluetooth baseline | Human devices required |
| 8.4 | Refresh dependencies incrementally | Automated, one dependency family per checkpoint |
| 8.5 | Complete Phase 3/5 device, visual and accessibility validation | Human devices required |
| 8.6 | Enforce blocking quality gate and verify signed release | Signing material required |
| 8.7 | Close phases, publish `1.2.0`, and archive obsolete worktrees | Maintainer approval for push/archive |

## Phase acceptance criteria

- [x] Phase 7 changes are reviewed, tested, and committed without losing unrelated work.
- [ ] `master` is clean and is the documented source of truth; unrelated tracked content has an explicit keep/remove decision.
- [ ] Dependency upgrades pass automated checks and pre/post device regression without data loss or transport regressions.
- [ ] API 23/31/34/36 evidence exists, including a physical two-phone Bluetooth/SCO run and permission grant/deny/revoke/re-grant paths.
- [ ] TalkBack, 200% font scale, light/dark mode, adaptive icon/splash, background/foreground, screen lock and process recreation are recorded.
- [ ] Lint has zero errors and fails the build on future errors; no broad suppression is added.
- [ ] A signed `1.2.0` artifact installs, launches, imports a valid credential, and completes Conversation and WalkieTalkie smoke tests.
- [ ] Phase 3, Phase 5, Phase 7 and Phase 8 state is synchronized; required completion/release tags point to verified commits.
- [ ] Canonical commits and tags are present on `origin`; obsolete worktrees are removed only after clean-state and preservation checks.

## Release blockers

- No physical two-phone Bluetooth/SCO evidence.
- No external signing keystore/process.
- Any unresolved High/Critical correctness or security finding.
- Dependency upgrade that changes persisted data or transport behavior without migration/regression evidence.
- Dirty or unpushed canonical release commit.
