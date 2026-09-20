# Roadmap — Mini Conversation (current runtime: RTranslator)

## Phase 1 — Security & Stability Hardening

**Status**: complete (6/6 tasks complete)
**Source**: `/vp-audit` full-codebase pass, 2026-09-19 (see `.viepilot/requests/`)
**Goal**: close the Critical/High findings before any new feature work proceeds.
**Execution spec**: `.viepilot/phases/phase-1-security-stability/SPEC.md`

| Task | Request | Priority |
|---|---|---|
| 1.1 | Encrypt/protect the stored GCP service-account key at rest | `BUG-001` |
| 1.2 | Exclude credential storage from Android Auto Backup | `BUG-002` |
| 1.3 | Fix `Tools.merge()` byte-array corruption | `BUG-003` |
| 1.4 | Fix `Recorder` stop/release race with the recording thread | `BUG-004` |
| 1.5 | Implement the stubbed gRPC `onError` callback in `Recognizer` | `BUG-005` |
| 1.6 | Replace unrestricted Downloads `.json` scan / legacy storage flag with scoped storage picker | `BUG-006` |

**Acceptance criteria (phase-level)**:
- [x] All 6 tasks above have a fix + regression test (see `SYSTEM-RULES.md` quality gate)
- [ ] `/vp-audit --tier3` re-run shows these findings resolved

**Verification command**: `./gradlew testDebugUnitTest lintDebug assembleDebug` plus credential import and backup checks on a device. Focused unit tests now exist for all Phase 1 fixes; task 1.6 passed manual SAF import validation on the API 36 emulator.

## Phase 2 — Correctness & Robustness (planned, not started)

**Execution spec**: `.viepilot/phases/phase-2-correctness-robustness/SPEC.md`
**Task contracts**: `.viepilot/phases/phase-2-correctness-robustness/tasks/`; execution is gated on Phase 1's exit criteria.

| Task | Request | Priority |
|---|---|---|
| 2.1 | Reuse a single Room database instance | `BUG-007` |
| 2.2 | Verify AES/CTR helper reachability; remove dead helpers or migrate persisted consumers to versioned AEAD | `BUG-008` |
| 2.3 | Synchronize API token refresh/publication | `BUG-009` |
| 2.4 | Make WalkieTalkie service teardown binding-safe | `BUG-010` |
| 2.5 | Cancel delayed Conversation callbacks during teardown | `BUG-011` |
| 2.6 | Verify second-scale reachability, then implement or eliminate the GraphView crash branch | `BUG-012` |

## Phase 3 — Android Modernization (planned, not started)

**Evolved via `/vp-evolve BUG-013 ENH-012` (2026-09-19), expanded by `/vp-audit` + `/vp-evolve` (2026-09-20)** — see `.viepilot/phases/phase-3-android-modernization/SPEC.md` for full task breakdown. The platform upgrade is sequenced after Phases 1 and 2:

| Task | Request | Priority |
|---|---|---|
| 3.1 | Upgrade AGP / Gradle wrapper / compileSdk / targetSdk to a supported API 36 toolchain | `ENH-011` |
| 3.2 | Add Android 12+ Bluetooth runtime permissions (`BLUETOOTH_SCAN`/`CONNECT`/`ADVERTISE`) — **must land in the same PR as 3.1**, or Conversation/WalkieTalkie mode crashes on first Bluetooth call once targetSdk hits 31+ | `BUG-013` |
| 3.3 | Refresh Room (2.1.0 → current), `play-services-nearby` (17.0.0 → current), gRPC, `google-auth-library-oauth2-http`, `nimbus-jose-jwt` | `ENH-012` |
| 3.4 | Add explicit exported declarations for Android 12+ | `BUG-014` |
| 3.5 | Declare and enforce microphone/connected-device foreground service types | `BUG-015` |
| 3.6 | Resolve lint errors and make quality/release checks blocking | 2026-09-20 `vp-audit` |

**Acceptance criteria (phase-level)**:
- [ ] All 6 tasks above have a merged fix + regression test/manual verification
- [ ] `/vp-audit --tier3` re-run shows these findings resolved
- [ ] Tasks 3.1, 3.2, 3.4 and 3.5 land together; lint has no unexplained errors and blocks new errors

**Verification command**: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease` + manual on-device regression (Conversation + WalkieTalkie mode) on API 23/31/34/36, including a physical two-phone Bluetooth pair.

## Phase 4 — Remaining Hygiene Backlog (proposed, not yet started)

Backlog of `ENH-*` requests not yet evolved into a phase: `ENH-013` (test coverage buildout), `ENH-014` (unrestricted deserialization), `ENH-015` (selective comparison/porting from upstream `v3.00`), `ENH-016` (`.idea/*.xml` committed), `ENH-017` (unrelated `.agents/skills/` content at repo root). Run `/vp-evolve` again against these when ready to sequence them.

## Phase 5 — Mini Conversation Rebrand & UI (planned, not started)

**Execution spec**: `.viepilot/phases/phase-5-mini-conversation-ui/SPEC.md`  
**Sequenced after**: Phase 3 Android Modernization  
**Target release**: 1.2.0 (runtime version remains 1.1.2 until implementation/release)

| Task | Request | Priority |
|---|---|---|
| 5.1 | Rename user-visible product/docs to Mini Conversation and replace broken README screenshots | `ENH-018`, `BUG-016` |
| 5.2 | Generate legacy, round, adaptive, and optional monochrome launcher icons from `images/icon.png` | `ENH-018` |
| 5.3 | Introduce Material DayNight design tokens based on the new icon palette | `ENH-019` |
| 5.4 | Refresh onboarding, pairing, Conversation, WalkieTalkie, API, and settings screens | `ENH-019` |
| 5.5 | Fix touch targets, accessibility labels, contrast, font scaling, and responsive layouts | `ENH-019` |
| 5.6 | Run visual/accessibility/device QA and verify a signed release on API 23/31/34/36 | `ENH-019`, 2026-09-20 `vp-audit` |

**Acceptance criteria (phase-level)**:
- [ ] Installed app and first-party documentation use Mini Conversation consistently
- [ ] Application ID/package remain stable and upstream attribution is preserved
- [ ] Launcher icon passes adaptive mask and splash-screen checks
- [ ] Core conversation behavior passes automated and manual regression gates
- [ ] No unresolved High/Critical defect, missing device evidence, or unsigned artifact is represented as release-ready

## Progress Summary

| Phase | Status | Tasks Done | Tasks Total |
|---|---|---|---|
| 1 — Security & Stability Hardening | complete | 6 | 6 |
| 2 — Correctness & Robustness | planned | 0 | 6 |
| 3 — Android Modernization | planned | 0 | 6 |
| 4 — Remaining Hygiene Backlog | proposed | 0 | 5 |
| 5 — Mini Conversation Rebrand & UI | planned | 0 | 6 |

Run `/vp-auto --from 2` to begin Phase 2 task 2.1. The audit-driven execution order and release gates are in `.viepilot/REMEDIATION-PLAN.md`. Phase 3 performs the integrated platform upgrade; Phase 5 applies the rebrand/UI and records device/release evidence. Phase 4 remains an independently schedulable hygiene backlog.
