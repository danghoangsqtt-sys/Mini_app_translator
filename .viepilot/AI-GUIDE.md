# AI Navigation Guide — Mini Conversation

## Quick lookup

| I need to... | Read this first |
|---|---|
| Understand overall system state | `.viepilot/TRACKER.md` |
| Understand product/domain | `.viepilot/PROJECT-CONTEXT.md` |
| Understand system design | `.viepilot/ARCHITECTURE.md` |
| Understand coding conventions | `.viepilot/SYSTEM-RULES.md` |
| See phases/tasks | `.viepilot/ROADMAP.md` |
| See a specific tracked issue | `.viepilot/requests/{BUG,ENH}-*.md` |
| See raw audit evidence | conversation history of `/vp-audit` run, 2026-09-19 |
| Machine-readable state | `.viepilot/HANDOFF.json` |

## Context loading strategy

1. Always load `TRACKER.md` first — it says what phase/state the project is in.
2. For any task touching `voice_translation/`, `api_management/`, or `access/`, read the matching `.viepilot/requests/*.md` files first — most known defects in this codebase live in those three packages (Bluetooth transport, Google credential handling, speech recognition streaming).
3. Use `app/build.gradle` and `TRACKER.md` as the current version source of truth: app 1.2.0/versionCode 15. Keep Phase 3 and Phase 5 pending human QA explicit.

## File relationships

```
app/src/main/java/nie/translator/rtranslatordevedition/
├── access/            onboarding (name/photo/privacy) — low risk
├── api_management/    optional legacy Cloud credential handling + OAuth internals — HIGH RISK (BUG-001, BUG-002, BUG-006, BUG-009)
├── database/          Room DB (AppDatabase, MyDao, entities) — BUG-007
├── settings/          settings UI
├── tools/             utilities incl. crypto (Tools.java — BUG-003, BUG-008) and vendored GraphView lib
└── voice_translation/ on-device engines, Bluetooth conversation + walkie modes, isolated legacy gRPC code — BUG-004, BUG-005, BUG-010, BUG-011, ENH-001
```

## Known constraints (do not re-derive, already established by audit)

- No CI/CD is configured. The implemented toolchain is AGP 8.13.2 / Gradle 8.13 / compileSdk 36 / targetSdk 36 and requires JDK 17 to run Gradle; device evidence is still required before release.
- Automated coverage remains limited (`ENH-013`), even though focused regression tests exist. Any new fix should add focused coverage where feasible without claiming broad end-to-end coverage.
- `.agents/` is excluded from the sanitized release history and ignored because it contained unrelated workstation/plugin payloads — do not treat it as application source or read it for Mini Conversation context.
