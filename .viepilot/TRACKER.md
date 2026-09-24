# Tracker — Mini Conversation modernization (runtime app_name is now Mini Conversation as of Phase 5; applicationId/package remain `nie.translator.rtranslatordevedition` from upstream RTranslator)

## Current state

- **Mode**: Brownfield (ViePilot initialized 2026-09-19 on an existing project, no prior brainstorm)
- **Current phase**: Phase 9 — No-Key On-Device Translation (`in_progress`). Phase 8's unsigned `1.2.0` candidate is a release NO-GO; Tasks 8.1–8.2 remain valid repository history while its remaining gates are consolidated into Phase 9. Phase 3 and Phase 5 retain open human validation gates.
- **Current task**: 9.4 — Implement lifecycle-safe Android SpeechRecognizer engine (`in_progress`; strict contract locked and assigned to TERRA 5.6; PM review pending).
- **Branch**: sanitized `master` is canonical and synchronized with `origin/master`; pre-sanitization history is retained locally at `codex/backup-master-pre-sanitize-20260923`. Upstream RTranslator's default is `upstream/v3.00` (lineage review remains `ENH-015`).
- **Target product name**: Mini Conversation — shipped in code (Phase 5, `app_name` and all first-party docs); `applicationId`/package remain `nie.translator.rtranslatordevedition` intentionally
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
| 5 — Mini Conversation Rebrand & UI | in_progress | 5/6 tasks (6th partially — code done, device/release QA PENDING HUMAN) |
| 6 — Bug Fix Sprint | complete | 10/10 tasks |
| 7 — Operational State & Build Portability | complete | 4/4 tasks |
| 8 — Release Readiness & Project Closure | blocked / `1.2.0` NO-GO | 2/7 historical tasks; remainder superseded |
| 9 — No-Key On-Device Translation | in_progress | 3/7 tasks; 9.4 next |
| 10 — Wi-Fi Hotspot Connection | proposed | 0/6 tasks |

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
| BUG-017 | 🐛 | HANDOFF state/version drift after Phase 5 | Medium | resolved (Phase 7, task 7.1; canonical `688f395`) |
| BUG-018 | 🐛 | Missing Phase 6 completion tag and closeout trace | Medium | resolved (Phase 7, task 7.2; local tag `mini-app-translator-vp-p6-complete`) |
| BUG-019 | 🐛 | Stale project/architecture docs after Phases 3, 5, and 6 | Low | resolved (Phase 7, task 7.3; current/historical docs separated) |
| BUG-020 | 🐛 | Machine-specific JDK path committed in `gradle.properties` | High | resolved (Phase 7, task 7.4; portable JDK configuration verified) |
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
| ENH-016 | 🔧 | `.idea/*.xml` committed to git | Low | resolved on canonical sanitized `master` |
| ENH-017 | 🔧 | Unrelated `.agents/skills/` content found at repo root | Low | resolved on canonical sanitized `master` |
| BUG-021 | 🐛 | Onboarding states an obsolete Google Cloud `$300`/one-year offer | Medium | resolved (Phase 9, task 9.1; `8a0a97a`) |
| BUG-022 | 🐛 | Onboarding still displays the legacy RTranslator logo after rebrand | Low | resolved (Phase 9, task 9.1; `8a0a97a`) |
| BUG-023 | 🐛 | False BLE capability gate blocks valid physical phones | High | code/emulator resolved (`8a0a97a`); physical confirmation due 9.7 |
| ENH-018 | 🔧 | Rename/rebrand application as Mini Conversation and replace launcher icons | Medium | planned (Phase 5, tasks 5.1–5.2) |
| ENH-019 | 🔧 | Modernize Views UI, dark theme, responsive layout, and accessibility | Medium | planned (Phase 5, tasks 5.3–5.6) |
| ENH-020 | 🔧 | Add Wi-Fi Hotspot transport alongside Bluetooth | Medium | proposed (Phase 10) |
| ENH-021 | 🔧 | No-key, on-device-first speech and translation architecture | Critical | in_progress (Phase 9) |

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
- **2026-09-23**: `/vp-evolve` (Add Feature) enriched Phase 5's SPEC with concrete, execution-ready detail per a user-supplied prescriptive brief: exact `values/colors.xml` and new `values-night/colors.xml` hex tokens, `Theme.Speech` → `Theme.MaterialComponents.DayNight.NoActionBar` wiring, adaptive-icon XML/asset breakdown, a per-screen task list for 5.4 (onboarding/pairing/Conversation/WalkieTalkie/API/settings), and the exact 5.6 version bump (`versionCode 15`, `versionName '1.2.0'`) plus build-verification commands. No new phase created — Phase 5 (0/6 tasks, `planned`) already covered this scope from the 2026-09-20 evolve pass; this pass only sharpened task contracts. Still sequenced after Phase 3, whose static work (3.1/3.2/3.4/3.5) is on `master` but device evidence remains PENDING HUMAN — Phase 5 code changes do not require that device evidence to start, but Phase 3 should be formally closed before Phase 5's own device/release QA (task 5.6) is treated as final. No application code or version changed by this planning pass.
- **2026-09-23**: `/vp-auto --from phase-5` executed Phase 5 tasks 5.1–5.6 (code portion). 5.1: renamed product to Mini Conversation in `strings.xml`/`values-it/strings.xml`/README/privacy docs/CHANGELOG, preserving `applicationId`/package and upstream credit. 5.2: generated a clean circular legacy/round launcher icon set from `images/icon.png` via Pillow (removing the noisy glow halo) plus a redrawn flat adaptive-icon foreground/monochrome pair and a gradient background drawable; removed the superseded per-density foreground PNGs and flat-color resource. 5.3: migrated `Theme.Speech` to `Theme.MaterialComponents.DayNight.NoActionBar` with new semantic color tokens in `values/colors.xml` and a new `values-night/colors.xml`, plus a `materialVersion` ext bumping Material to 1.9.0 without touching the shared `supportLibraryVersion` used by cardview/recyclerview. 5.4: refreshed all 6 named screens (MaterialButton/MaterialCheckBox on onboarding; MaterialCardView device rows + icon empty state on pairing; MaterialCardView message bubbles with new `colorPrimaryContainer`/`colorOnPrimaryContainer` tokens + 56dp mic on conversation; larger language labels on WalkieTalkie; MaterialCardView groups on API management; consolidated Language+Audio `PreferenceCategory` on settings) and swept remaining hardcoded hex colors across all 32 layout files. 5.5: added real `contentDescription` strings (EN+IT) for 28 icon controls (7 had none, 21 reused the `app_name` placeholder) and enlarged 15 sub-48dp touch targets to 48×48dp. 5.6: bumped `app/build.gradle` to `versionCode 15`/`versionName '1.2.0'` and cut a `## [1.2.0]` CHANGELOG section; ran `./gradlew clean testDebugUnitTest lintDebug assembleDebug` — BUILD SUCCESSFUL, 47/47 unit tests passing, 0 lint errors (139 warnings, up from 136 at the Phase 6 baseline). Device matrix, `connectedDebugAndroidTest`, `assembleRelease` signing, screenshot baselines, and TalkBack/two-phone Bluetooth checks remain PENDING HUMAN — no Android device/emulator or signing keystore is available in this environment, mirroring Phase 3's existing device-evidence gap. All work is committed on `master` with per-task `mini-app-translator-vp-p5-t{N}`/`-done` tags; `master` has unpushed local commits (already the case before this pass) — pushing to `origin/master` was not performed and needs explicit confirmation before it happens.
- **2026-09-23**: `/vp-audit` auto-logged `BUG-017` (HANDOFF drift), `BUG-018` (missing Phase 6 completion tag), `BUG-019` (stale project/architecture docs), and `BUG-020` (committed machine-specific JDK path). It also re-detected `ENH-012`, `ENH-016`, and `ENH-017` with current evidence.
- **2026-09-23**: `/vp-evolve BUG-017 BUG-018 BUG-019 BUG-020` created Phase 7 — Operational State & Build Portability (four planned tasks). This planning pass did not change application code or `versionName`/`versionCode`; Phase 3 and Phase 5 human QA gates remain open.
- **2026-09-23**: `/vp-evolve` created Phase 8 — Release Readiness & Project Closure. It consumes (but does not duplicate) the Phase 7 delivery, then sequences repository hygiene, a pre-refresh device baseline, incremental Phase 3.3 dependency upgrades, Phase 3/5 device and accessibility QA, a blocking signed-release gate, and final tags/push/worktree archival. The Wi-Fi Hotspot feature moved from the reserved Phase 7 label to proposed Phase 9 so it cannot start before `1.2.0` is verified and published. No application code or version changed during planning.
- **2026-09-23**: Phase 8 Task 8.1 accepted the Phase 7 baseline, now mapped canonically to `dffa145` with annotated tag `mini-app-translator-vp-p7-complete`. The portable build evidence remains 47/47 unit tests, lint 0 errors, and a debug APK; Phase 3/5 human QA gates remain open.
- **2026-09-23**: `/vp-audit` deep scan ran `clean testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest`: 47 JVM tests and 7 Pixel 7a API 36 instrumentation tests passed; cold launch completed without crash/ANR. It auto-logged `BUG-021` (obsolete Google Cloud trial/pricing claim) and `BUG-022` (legacy RTranslator onboarding logo). Existing release-gate, dependency, repo-hygiene, and legacy-code findings were deduplicated against Phase 8/`ENH-*` scope.
- **2026-09-23**: Task 8.2 prepared `codex/phase8-sanitized-history` from `origin/master` without changing or pushing `master`. The candidate replays 44 source commits as 43 clean commits, contains no `.agents` objects, retains only shared `.idea/codeStyles/Project.xml`, preserves an identical `app/` tree, and records tag relocation. Adoption/push and worktree deletion remain explicit approval gates.
- **2026-09-23**: With explicit maintainer approval, Task 8.2 advanced canonical `master`/`origin/master` to the sanitized history, pushed the mapped Phase 5–8.1 tags, and retained the former history in local backup branch `codex/backup-master-pre-sanitize-20260923`. No worktree was deleted. Task 8.2 is complete; Task 8.3 is next.
- **2026-09-23**: Product owner approved the no-key architecture after reviewing `vp-pdf`. `/vp-crystallize` + `/vp-evolve` created Phase 9 with seven gated tasks: launch/Bluetooth stabilization, engine contracts, ML Kit translation, Android SpeechRecognizer, mode integration, optional legacy Cloud migration, and full device/release QA. The current `1.2.0` candidate is a release NO-GO; target is `1.3.0`, while Wi-Fi Hotspot moved to Phase 10. `translate.google.com/m` scraping and copying AGPL code from `vp-pdf` are forbidden.
- **2026-09-23**: Phase 9 Task 9.1 passed PM code/emulator review at `8a0a97a`: 54 JVM tests, 7 Pixel 7a API 36 instrumentation tests, lint 0 errors/136 warnings, keyless fresh onboarding, Nearby permission deny/grant/relaunch, and pairing search all passed without crash/ANR. Physical-phone/two-phone proof remains explicitly deferred to 9.7.
- **2026-09-24**: Phase 9 Task 9.2 passed PM review at `87e9875` after correction of recognition-session callback isolation and cancellation/close races. The new runtime-neutral contracts, explicit factory registry, and composition adapters do not switch runtime behavior or touch credentials/consumers. Independent gate: 66 JVM tests, lint 0 errors/136 warnings, debug APK 11,183,277 bytes; commit persisted to `origin/master`.
- **2026-09-24**: Phase 9 Task 9.3 passed PM review at `384b114` after callback-under-lock, cancel-before-SDK-start, attribution/privacy, and metered-network confirmation corrections. Independent gate: 99 JVM tests, 10 Pixel 7a API 36 instrumentation tests, lint 0 errors/136 warnings, and debug APK SHA-256 `E3CDB5263CC0D84914F11D644D81300D8F3384788C6FE45FD758FBC11002DEA7`. PM installed the APK pair once, prepared Italian online, force-stopped both packages, removed all emulator routes, and passed the separate translate-without-download test offline before restoring validated network connectivity. Runtime integration remains deferred to 9.5.

## Version info

- App version at import: `1.1.2` (versionCode 13) — on the stale `master` branch only.
- Current app version in source: `1.2.0` (versionCode 15), bumped 2026-09-23 for Phase 5 (`app/build.gradle`). This candidate is a release NO-GO and must not be tagged or published.
- Phase 9 targets `1.3.0`; assign the next versionCode only at the reviewed release-candidate gate in Task 9.7.
- If a bug-fix-only release is cut before the planned feature release, propose a PATCH version (for example `1.1.3`) and increment `versionCode` at release time; this plan does not bump either value.
