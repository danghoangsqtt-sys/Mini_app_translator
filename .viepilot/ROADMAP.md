# Roadmap — Mini Conversation (runtime app_name is now Mini Conversation as of Phase 5; applicationId/package remain `nie.translator.rtranslatordevedition` from upstream RTranslator)

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

## Phase 2 — Correctness & Robustness (complete / PASS)

**Execution spec**: `.viepilot/phases/phase-2-correctness-robustness/SPEC.md`
**Task contracts**: `.viepilot/phases/phase-2-correctness-robustness/tasks/`; execution is gated on Phase 1's exit criteria.
**Status**: complete / PASS. All 6 tasks passed the technical and PM gate; remote persistence was verified at `origin/master = d7c591f95b3fde595a0ad8aac722ac033baf0bfa`.

| Task | Request | Priority |
|---|---|---|
| 2.1 | Reuse a single Room database instance | `BUG-007` |
| 2.2 | Verify AES/CTR helper reachability; remove dead helpers or migrate persisted consumers to versioned AEAD | `BUG-008` |
| 2.3 | Synchronize API token refresh/publication | `BUG-009` |
| 2.4 | Make WalkieTalkie service teardown binding-safe | `BUG-010` |
| 2.5 | Cancel delayed Conversation callbacks during teardown | `BUG-011` |
| 2.6 | Verify second-scale reachability, then implement or eliminate the GraphView crash branch | `BUG-012` |

**Final gate**: at `d7c591f`, 39/39 JVM tests, `assembleDebug`, and 7/7 API 36 instrumentation tests passed. The approved limited lint waiver covers only three `ResourceType` errors in `GridLabelRenderer` and two `InvalidPackage` errors from `grpc-core 1.11.0` (5 errors, 121 warnings); no new lint error was present. JDK 11 is required by Gradle 5.6.4; JBR 25 is incompatible. Task 3.6 remains incomplete and owns dependency-refresh-aware lint remediation and warning triage.

## Phase 3 — Android Modernization (in_progress; Cluster A static PASS, device evidence BLOCKED)

**Evolved via `/vp-evolve BUG-013 ENH-012` (2026-09-19), expanded by `/vp-audit` + `/vp-evolve` (2026-09-20)** — see `.viepilot/phases/phase-3-android-modernization/SPEC.md` for full task breakdown. The platform upgrade is sequenced after Phases 1 and 2:

| Task | Request | Priority |
|---|---|---|
| 3.1 | Upgrade AGP / Gradle wrapper / compileSdk / targetSdk to a supported API 36 toolchain | `ENH-011` |
| 3.2 | Add Android 12+ Bluetooth runtime permissions (`BLUETOOTH_SCAN`/`CONNECT`/`ADVERTISE`) — **must land in the same PR as 3.1**, or Conversation/WalkieTalkie mode crashes on first Bluetooth call once targetSdk hits 31+ | `BUG-013` |
| 3.3 | Refresh Room (2.1.0 → current), `play-services-nearby` (17.0.0 → current), gRPC, `google-auth-library-oauth2-http`, `nimbus-jose-jwt` | `ENH-012` |
| 3.4 | Add explicit exported declarations for Android 12+ | `BUG-014` |
| 3.5 | Declare and enforce microphone/connected-device foreground service types | `BUG-015` |
| 3.6 | Resolve lint errors and make quality/release checks blocking | 2026-09-20 `vp-audit` |

**Status**: `in_progress`. Tasks 3.1, 3.2, 3.4, and 3.5 have landed in code on `master` (Cluster A static PASS); the two-phone Bluetooth/SCO device evidence remains **PENDING HUMAN**, so the phase is not yet closed. Task 3.3 (dependency refresh) is intentionally deferred to a dedicated `/vp-evolve` pass to avoid an API-migration/backward-compatibility break.

**Acceptance criteria (phase-level)**:
- [ ] All 6 tasks above have a merged fix + regression test/manual verification
- [ ] `/vp-audit --tier3` re-run shows these findings resolved
- [ ] Tasks 3.1, 3.2, 3.4 and 3.5 land together; lint has no unexplained errors and blocks new errors

**Verification command**: `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease` + manual on-device regression (Conversation + WalkieTalkie mode) on API 23/31/34/36, including a physical two-phone Bluetooth pair.

## Phase 4 — Remaining Hygiene Backlog (proposed, not yet started)

Backlog of `ENH-*` requests not yet evolved into a phase: `ENH-013` (test coverage buildout), `ENH-014` (unrestricted deserialization), `ENH-015` (selective comparison/porting from upstream `v3.00`), `ENH-016` (`.idea/*.xml` committed), `ENH-017` (unrelated `.agents/skills/` content at repo root). Run `/vp-evolve` again against these when ready to sequence them.

## Phase 5 — Mini Conversation Rebrand & UI (in_progress — code complete, device/release QA pending)

**Execution spec**: `.viepilot/phases/phase-5-mini-conversation-ui/SPEC.md`  
**Sequenced after**: Phase 3 Android Modernization  
**Target release**: 1.2.0 — shipped in code as `versionCode 15`/`versionName '1.2.0'` on 2026-09-23; not yet cut as a signed release artifact

| Task | Request | Priority | Status |
|---|---|---|---|
| 5.1 | Rename user-visible product/docs to Mini Conversation and replace broken README screenshots | `ENH-018`, `BUG-016` | done |
| 5.2 | Generate legacy, round, adaptive, and optional monochrome launcher icons from `images/icon.png` | `ENH-018` | done |
| 5.3 | Introduce Material DayNight design tokens based on the new icon palette | `ENH-019` | done |
| 5.4 | Refresh onboarding, pairing, Conversation, WalkieTalkie, API, and settings screens | `ENH-019` | done |
| 5.5 | Fix touch targets, accessibility labels, contrast, font scaling, and responsive layouts | `ENH-019` | done |
| 5.6 | Run visual/accessibility/device QA and verify a signed release on API 23/31/34/36 | `ENH-019`, 2026-09-20 `vp-audit` | in_progress — version bump + `clean testDebugUnitTest lintDebug assembleDebug` done (BUILD SUCCESSFUL, 47/47 tests, 0 lint errors); device matrix/`connectedDebugAndroidTest`/`assembleRelease` signing/screenshot baselines/TalkBack/two-phone Bluetooth PENDING HUMAN |

**Acceptance criteria (phase-level)**:
- [x] Installed app and first-party documentation use Mini Conversation consistently
- [x] Application ID/package remain stable and upstream attribution is preserved
- [ ] Launcher icon passes adaptive mask and splash-screen checks (needs device/emulator verification)
- [ ] Core conversation behavior passes automated and manual regression gates (unit tests pass; manual/device gates pending)
- [ ] No unresolved High/Critical defect, missing device evidence, or unsigned artifact is represented as release-ready (device/release QA still open — do not represent this phase as release-ready yet)

## Phase 6 — Bug Fix Sprint (complete)

**Source**: `/vp-audit` 2026-09-22 lint baseline (174 warnings, 0 errors)
**Execution spec**: `.viepilot/phases/phase-6-bug-fix-sprint/SPEC.md`
**Status**: complete (10/10 tasks); app bumped to `1.1.3` (`versionCode 14`)

Sửa toàn bộ lint warning không đòi hỏi nâng dependency, đồng thời khắc phục lỗi biên dịch phát sinh trong quá trình sửa (`InflateParams`).

| Task | Request | Priority |
|---|---|---|
| 6.1 | Handler Looper fix | done (pre-phase) |
| 6.2 | UnknownIdInLayout analysis (confirmed false positives) | Low |
| 6.3 | contentDescription cho ImageViews | Medium |
| 6.4 | Overdraw trong các layout | Low |
| 6.5 | InflateParams trong các adapter | Low |
| 6.6 | CanvasSize trong GraphView + RoundedCornerLayout | Low |
| 6.7 | FileLog hardcoded path | done (already Logcat) |
| 6.8 | DefaultLocale trong Translator.java | Low |
| 6.9 | DataExtractionRules cho Android 12+ | Medium |
| 6.10 | Build + Install + Verify | — |

**Final gate (2026-09-23)**: `testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL; `lintDebug` → **0 errors, 136 warnings** (below the `<160` target). During the sprint a build-breaking regression in the `InflateParams` fix (undefined `parent` symbol in three list adapters) was found and corrected to use the `viewGroup` parameter.

## Progress Summary

| Phase | Status | Tasks Done | Tasks Total |
|---|---|---|---|
| 1 — Security & Stability Hardening | complete | 6 | 6 |
| 2 — Correctness & Robustness | complete / PASS | 6 | 6 |
| 3 — Android Modernization | in_progress (device evidence BLOCKED) | 4 (static) | 6 |
| 4 — Remaining Hygiene Backlog | proposed | 0 | 5 |
| 5 — Mini Conversation Rebrand & UI | in_progress (device/release QA PENDING HUMAN) | 5 (+1 partial) | 6 |
| 6 — Bug Fix Sprint | complete | 10 | 10 |

Phase 2 is complete. Phase 3 is `in_progress`: Cluster A (toolchain, Bluetooth permissions, exported declarations, foreground service types) is a static PASS, but two-phone Bluetooth/SCO device evidence is still PENDING HUMAN; Task 3.3 is deferred to a dedicated `/vp-evolve`. Phase 6 is complete at `1.1.3` (0 lint errors / 136 warnings). The audit-driven execution order and release gates are in `.viepilot/REMEDIATION-PLAN.md`. Phase 5 is `in_progress`: tasks 5.1–5.5 are done and 5.6's code/build-verification portion is done (app bumped to `1.2.0`/`versionCode 15`; 0 lint errors / 139 warnings, 47/47 unit tests); its device matrix, `assembleRelease` signing, and manual QA remain PENDING HUMAN. Phase 4 remains an independently schedulable hygiene backlog.
