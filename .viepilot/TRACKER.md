# Tracker — Mini Conversation modernization (current runtime: RTranslator)

## Current state

- **Mode**: Brownfield (ViePilot initialized 2026-09-19 on an existing project, no prior brainstorm)
- **Current phase**: Multi-phase active — Phase 3 — Android Modernization (`in_progress`; static PASS, device evidence BLOCKED) and Phase 6 — Bug Fix Sprint (`complete`, 10/10 tasks)
- **Current task**: none
- **Branch**: `master` tracking this fork's `origin/master`; upstream RTranslator's default is `upstream/v3.00` (lineage review remains `ENH-015`)
- **Target product name**: Mini Conversation (planned in Phase 5; runtime still reports RTranslator)
- **Remediation plan**: `.viepilot/REMEDIATION-PLAN.md` (Phases 1–2 complete; Phase 3 is `in_progress` with Cluster A static PASS and device QA blocked; Phase 6 complete)

## Brownfield Import

- **Import source**: `vp-crystallize --brownfield`
- **Scan Report**: `docs/brainstorm/session-brownfield-import.md`
- **Trace**: `.viepilot/BROWNFIELD-TRACE.md`

## Progress overview

| Phase | Status | Progress |
|---|---|---|
| 1 — Security & Stability Hardening | complete | 6/6 tasks |
| 2 — Correctness & Robustness | complete / PASS | 6/6 tasks |
| 3 — Android Modernization | in_progress | 4/6 static (device evidence BLOCKED) |
| 4 — Remaining Hygiene Backlog | proposed | 0/5 tasks |
| 5 — Mini Conversation Rebrand & UI | planned | 0/6 tasks |
| 6 — Bug Fix Sprint | complete | 10/10 tasks |

**Phase 2 final gate**: PM accepted the limited waiver at `d7c591f`: 39/39 JVM tests,
`assembleDebug`, and 7/7 API 36 instrumentation tests passed; lint had only the three
`ResourceType` errors in `GridLabelRenderer` and two `InvalidPackage` errors from `grpc-core
1.11.0` (5 errors, 121 warnings). Remote persistence was verified at `origin/master = d7c591f`.
JDK 11 is required for Gradle 5.6.4; JBR 25 is incompatible. Task 3.6 remains incomplete and
owns final lint remediation after Task 3.3 dependency refresh; warnings are not mass-suppressed.

## Backlog — Pending Requests

Sourced from `/vp-audit` passes on 2026-09-19 and 2026-09-20. Planning status does not mean application code has been implemented.

| ID | Type | Title | Priority | Status |
|----|------|-------|----------|--------|
| BUG-001 | 🐛 | GCP service-account key stored unencrypted at rest | Critical | resolved (Phase 1, task 1.1) |
| BUG-002 | 🐛 | Service-account key exposed via unencrypted Auto Backup | Critical | resolved (Phase 1, task 1.2) |
| BUG-003 | 🐛 | `Tools.merge()` byte-array corruption | High | resolved (Phase 1, task 1.3) |
| BUG-004 | 🐛 | `Recorder` stop/release race with recording thread | High | resolved (Phase 1, task 1.4) |
| BUG-005 | 🐛 | gRPC streaming `onError` silently swallowed | High | resolved (Phase 1, task 1.5) |
| BUG-006 | 🐛 | Unrestricted Downloads `.json` scan + legacy storage | High | resolved (Phase 1, task 1.6) |
| BUG-007 | 🐛 | Two independent Room DB instances on one SQLite file | Medium | resolved (Phase 2, task 2.1; migration deferred to schema change) |
| BUG-008 | 🐛 | AES/CTR encryption without MAC (malleable ciphertext) | Medium | resolved (Phase 2, task 2.2) |
| BUG-009 | 🐛 | Data race on `Global.apiToken` | Medium | resolved (Phase 2, task 2.3) |
| BUG-010 | 🐛 | `WalkieTalkieService.onDestroy` unconditional `unbindService` crash | Medium | resolved (Phase 2, task 2.4) |
| BUG-011 | 🐛 | Stale delayed `Handler` runnable after `ConversationService` teardown | Medium | resolved (Phase 2, task 2.5) |
| BUG-012 | 🐛 | GraphView vendored lib unimplemented branch throws | Low | closed/reclassified future capability; no production fix (Phase 2, task 2.6) |
| BUG-013 | 🐛 | Missing Android 12+ Bluetooth runtime permissions (blocks safe targetSdk 31+ upgrade) | High | in_progress (Phase 3, task 3.2 — code committed, device QA pending) |
| BUG-014 | 🐛 | Launcher activity missing explicit `android:exported` | High | in_progress (Phase 3, task 3.4 — code committed, device QA pending) |
| BUG-015 | 🐛 | Foreground voice services missing service types/type permissions | High | in_progress (Phase 3, task 3.5 — code committed, device QA pending) |
| BUG-016 | 🐛 | README references two deleted screenshots | Low | planned (Phase 5, task 5.1) |
| ENH-001 | 🔧 | No app-layer encryption for Bluetooth conversation payloads | Medium | new |
| ENH-002 | 🔧 | Contact photo stored as unencrypted BLOB | Medium | new |
| ENH-003 | 🔧 | Dead code with leak-prone patterns (`FileManager`, `EncryptionKey`) | Low | new |
| ENH-004 | 🔧 | `FileLog` hardcoded legacy path + unclosed resource | Low | new |
| ENH-005 | 🔧 | Deprecated `AsyncTask` / no-Looper `Handler()` usage | Low | new |
| ENH-006 | 🔧 | Swallowed exceptions via `printStackTrace()` only | Low | new |
| ENH-007 | 🔧 | Debug/error logs without `BuildConfig.DEBUG` guard | Low | new |
| ENH-008 | 🔧 | gRPC `channel.shutdown()` without `awaitTermination` | Low | new |
| ENH-009 | 🔧 | Unsynchronized field race in `RecognizerService.languageCode` | Low | new |
| ENH-010 | 🔧 | Dead fragment-action constants (removed account/password flow) | Low | new |
| ENH-011 | 🔧 | Toolchain 6+ years outdated (AGP 3.6.1 / Gradle 5.6.4 / compileSdk 29) | High | planned (Phase 3, task 3.1) |
| ENH-012 | 🔧 | Core dependencies frozen at 2018–2020 versions (incl. Room 2.1.0, play-services-nearby 17.0.0) | Medium | planned (Phase 3, task 3.3) |
| ENH-013 | 🔧 | Near-zero automated test coverage | Medium | new |
| ENH-014 | 🔧 | Unrestricted Java deserialization in `Tools.objToByte/byteToObj` | Low | new |
| ENH-015 | 🔧 | Review fork `master` against upstream `v3.00` before porting large changes | Medium | new (branch ambiguity resolved) |
| ENH-016 | 🔧 | `.idea/*.xml` committed to git | Low | new |
| ENH-017 | 🔧 | Unrelated `.agents/skills/` content found at repo root | Low | new |
| ENH-018 | 🔧 | Rename/rebrand application as Mini Conversation and replace launcher icons | Medium | planned (Phase 5, tasks 5.1–5.2) |
| ENH-019 | 🔧 | Modernize Views UI, dark theme, responsive layout, and accessibility | Medium | planned (Phase 5, tasks 5.3–5.6) |

## Decision log

- **2026-09-19**: Initialized ViePilot in brownfield mode. Deliberately skipped README/LICENSE/CONTRIBUTING rewrite and external stack research to stay scoped to "make audit findings trackable" (see `.viepilot/BROWNFIELD-TRACE.md` scope-reduction note).
- **2026-09-19**: `/vp-audit` Tier 3 re-pass (user-prompted, cross-checking an external AI's Android-modernization review against the actual codebase). Confirmed as accurate: `targetSdkVersion 29` (`ENH-011`), legacy `requestLegacyExternalStorage` + broad storage perms (`BUG-006`), Room 2.1.0 and `play-services-nearby:17.0.0` (added to `ENH-012`). Added one new tracked item, `BUG-013`, for the missing Android 12+ Bluetooth runtime permissions — verified by grep that `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`/`BLUETOOTH_ADVERTISE` appear nowhere in the project. Corrected the external review's framing: this does **not** crash today (targetSdk 29 keeps the app on the legacy Bluetooth permission model) — it will crash on first Bluetooth use the moment `ENH-011`'s targetSdk bump lands, unless `BUG-013` ships in the same change.
- **2026-09-19**: `/vp-evolve BUG-013 ENH-012` — user chose to also bundle `ENH-011` in, since `BUG-013` has no executable meaning without the targetSdk bump `ENH-011` performs. Created Phase 3 ("Android Modernization", planned, 3 tasks) with `.viepilot/phases/phase-3-android-modernization/{SPEC.md,PHASE-STATE.md}`. Renumbered the old undifferentiated Phase 3 backlog remainder (`ENH-013`–`ENH-017`) into Phase 4 ("Remaining Hygiene Backlog", still proposed). No app version bump — planning only, no code changed yet; version bumps on actual merge per `SYSTEM-RULES.md`.
- **2026-09-20**: `/vp-audit` confirmed the existing defect backlog and added `BUG-014` (exported launcher), `BUG-015` (foreground service types), `BUG-016` (missing README images), `ENH-018` (Mini Conversation identity/icon), and `ENH-019` (UI/accessibility). Build execution is currently blocked by the absence of Java and Android SDK on this machine.
- **2026-09-20**: `/vp-evolve` made Phases 1 and 2 executable, expanded Phase 3 from 3 to 5 tasks for target API 36, and created Phase 5 for the Mini Conversation rebrand/UI. Application code and runtime version remain unchanged.
- **2026-09-20**: `/vp-evolve` audit follow-up created a remediation sequence and task contracts for Phases 2–3 plus Phase 5 documentation/release QA. Added Phase 3 task 3.6 for blocking lint/release checks (5 lint errors and 119 warnings were observed despite a successful legacy build); host builds and 9 unit tests passed, but no Android device was connected and the release APK was unsigned. Phase 1 task 1.6 remains in progress; no app code or runtime version was changed by this planning pass.
- **2026-09-20**: `/vp-auto` completed Phase 1 task 1.6. Credential import now uses SAF, validates the selected JSON before encrypted persistence, and removes legacy storage access. Build checks passed with 14 unit tests; API 36 emulator validation covered picker open/cancel, invalid rejection, valid import persistence, and the gallery provider entry point without a storage-permission prompt.
- **2026-09-22**: Created Phase 6 — Bug Fix Sprint (`.viepilot/phases/phase-6-bug-fix-sprint/{SPEC.md,PHASE-STATE.md}`) from the `/vp-audit` lint baseline (174 warnings, 0 errors). Scope: fix all non-dependency lint warnings; no dependency upgrades.
- **2026-09-23**: Completed Phase 6 (10/10 tasks) and resolved a build-breaking regression where Task 6.5 referred to an undefined `parent` symbol in three list adapters (`LanguageListAdapter`, `FileListAdapter`, `PeerListAdapter`). Bumped the app to `1.1.3` (`versionCode 14`). Verification: `testDebugUnitTest assembleDebug` BUILD SUCCESSFUL; `lintDebug` 0 errors / 136 warnings. Phase 3 remains `in_progress` (Cluster A static PASS; two-phone Bluetooth/SCO device evidence still PENDING HUMAN).

## Version info

- App version at import: `1.1.2` (versionCode 13) — on the stale `master` branch only.
- Planned feature release after Phase 5: `1.2.0`; select the next `versionCode` during implementation/release, not during planning.
- If a bug-fix-only release is cut before the planned feature release, propose a PATCH version (for example `1.1.3`) and increment `versionCode` at release time; this plan does not bump either value.
