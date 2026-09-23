# ViePilot Audit Report — Mini Conversation / RTranslator

**Date**: 2026-09-22 (re-audit; original: 2026-09-20)  
**Scope**: Cả 2 workspace — `Mini_app_Translator` (master) và `Mini_app_Translator-phase3` (compatibility spike); Android/Java source, resources, documentation, ViePilot state  
**Build verification**: blocked locally — Java/Android SDK không được cấu hình trên máy  
**UI Upgrade**: Ocean Blue theme áp dụng lên `Mini_app_Translator` (master) ✅

## Executive summary

- `Mini_app_Translator` (master) là phiên bản **khuyến nghị để tiếp tục** — targetSdk 36, BUG-013/014/015 đã sửa, Phase 1 & 2 hoàn thành.
- `Mini_app_Translator-phase3` là compatibility spike (Task 3.1) — vẫn ở targetSdk 29, thiếu exported declarations, không có Bluetooth 12+ permissions. **Không dùng để release**.
- UI nâng cấp sang **Ocean Blue palette** (#0277BD) đã hoàn thành trong master: `colors.xml`, `ic_launcher_background.xml`, `styles.xml`, icon foreground đã được cập nhật.
- Phase 3 (Android Modernization, 6 tasks) và Phase 5 (Rebrand & UI) vẫn đang planned.


## Tier 1 — ViePilot state consistency: warning, repaired in planning

| Check | Result |
|---|---|
| TRACKER ↔ phase state | Phase 1 was current/planned but had no `SPEC.md` or `PHASE-STATE.md`; Phase 2 was also only narrative. Both were created by this evolve pass. |
| ROADMAP ↔ phase state | Phase 3 was the only phase with state files. Phase 1, 2, 3, and 5 now have explicit state/spec artifacts. |
| HANDOFF ↔ TRACKER | Request counts/status and branch wording were stale; update is part of this evolve pass. |
| Git tags ↔ completed phases | No ViePilot phase is complete, so no completion tag is due. |
| Brownfield import | Valid: `docs/brainstorm/session-brownfield-import.md` contains a Scan Report YAML block. |

## Tier 2 — documentation drift: warning

- `README.md` version 1.1.2 matches `app/build.gradle`.
- `CHANGELOG.md` was absent and is introduced with an Unreleased planning section.
- README references two deleted screenshots: `images/conversation_image_github.png` and `images/WalkieTalkie_and_Costs_image_github.png` (`BUG-016`).
- Identity is inconsistent: README says Mini App Translator, runtime strings/privacy say RTranslator, and the requested target is Mini Conversation (`ENH-018`).
- Architecture matrix is internally valid; required `system-overview` has its Mermaid sidecar and N/A rows include rationales.
- No placeholder organization URLs were found in project documentation.

## Tier 3 — Android stack and code quality: issues found

### Critical

1. Raw Google service-account material is stored without adequate at-rest protection (`BUG-001`).
2. Credential storage can enter Android Auto Backup (`BUG-002`).

### High

1. `Tools.merge()` never increments `count`, repeatedly overwriting index zero and corrupting all multi-byte merges (`BUG-003`, `Tools.java:301-305`).
2. Recorder stop/release races with the capture thread (`BUG-004`).
3. gRPC streaming errors are swallowed (`BUG-005`).
4. Key discovery scans Downloads broadly and depends on legacy external storage (`BUG-006`).
5. Android toolchain/target API is obsolete. Starting 31 August 2026, ordinary Play submissions must target API 36; this project targets 29 (`ENH-011`).
6. Bluetooth runtime permissions are missing for a target 31+ migration (`BUG-013`).
7. Launcher activity lacks the explicit exported declaration required by Android 12+ (`BUG-014`).
8. Foreground voice services lack service types and type permissions required for modern targets (`BUG-015`).

### Medium

- Duplicate Room database instances, unauthenticated AES/CTR storage, token/thread races, unsafe service teardown, stale handler callbacks, and a reachable-risk GraphView throw remain tracked in `BUG-007` through `BUG-012`.
- Dependencies date from 2018–2020 and need staged upgrades after the toolchain move (`ENH-012`).
- Automated coverage is effectively absent: two generated example tests for 122 Java files (`ENH-013`).

### UI/accessibility

- Current theme is light-only AppCompat with hardcoded surfaces/colors and no `values-night` resources.
- All 14 detected actionable image controls lack accessible descriptions.
- Pairing search/cancel is visibly 24×24dp; Android guidance recommends at least a 48×48dp focus/touch area.
- Rebrand must generate adaptive/round launcher resources from `images/icon.png`; the finished composite needs safe-zone resizing rather than full-bleed placement.

## Tier 4 — framework integrity

Skipped: this is an application repository, not the ViePilot framework repository.

## Verification evidence and limitations

- Static source/resource checks completed with repository-wide search and targeted file inspection.
- `gradlew --version`, unit tests, lint, and assembly could not start: `JAVA_HOME` is unset, `java.exe` is absent, and no Android SDK was found under the standard user path.
- No runtime UI screenshot audit was possible without a buildable APK/emulator. Phase 5 includes explicit visual and device gates.

## Guardrails contract for execution

```yaml
stack: android-java
guardrails:
  - never commit, log, back up, or screenshot service-account credentials
  - preserve applicationId/package identity during the product-name-only rebrand
  - ship targetSdk, Bluetooth permissions, exported declarations, and foreground-service types together
  - request runtime permissions before starting microphone/connected-device foreground work
  - keep lifecycle teardown idempotent and cancel callbacks before releasing resources
  - use one Room database instance per process
  - add focused regression tests for every correctness fix
  - use Material theme roles, 48dp touch targets, localized accessibility labels, and dark-theme resources
  - verify API 23, 31, 34, and 36 before release
required_commands:
  - gradlew.bat testDebugUnitTest
  - gradlew.bat lintDebug
  - gradlew.bat assembleDebug
  - gradlew.bat connectedDebugAndroidTest
```

## Primary references

- Android target API requirements: https://developer.android.com/google/play/requirements/target-sdk
- Exported component requirement: https://developer.android.com/guide/components/intents-filters
- Foreground service types: https://developer.android.com/about/versions/14/changes/fgs-types-required
- Adaptive icons: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive
- Views accessibility: https://developer.android.com/guide/topics/ui/accessibility/views/apps-views
