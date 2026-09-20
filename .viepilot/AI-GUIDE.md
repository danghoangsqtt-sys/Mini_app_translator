# AI Navigation Guide — RTranslator

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
3. Do not assume `README.md` describes the current branch's version — see the branch-mismatch note in `PROJECT-META.md` before quoting a version number to the user.

## File relationships

```
app/src/main/java/nie/translator/rtranslatordevedition/
├── access/            onboarding (name/photo/privacy) — low risk
├── api_management/    Google Cloud key handling + OAuth token refresh — HIGH RISK (BUG-001, BUG-002, BUG-006, BUG-009)
├── database/          Room DB (AppDatabase, MyDao, entities) — BUG-007
├── settings/          settings UI
├── tools/             utilities incl. crypto (Tools.java — BUG-003, BUG-008) and vendored GraphView lib
└── voice_translation/ Bluetooth conversation + walkie-talkie modes, gRPC speech recognition — BUG-004, BUG-005, BUG-010, BUG-011, ENH-001
```

## Known constraints (do not re-derive, already established by audit)

- No CI/CD for this app; toolchain is 2020-era (AGP 3.6.1 / Gradle 5.6.4 / compileSdk 29) — see `ENH-011`.
- Test coverage is effectively 0% — see `ENH-013`. Any fix task should add a regression test where feasible, but do not block a fix on achieving broad coverage first.
- `.agents/skills/` at repo root is untracked and appears to be unrelated content (a different project's skill files) — do not treat it as part of this app's source; do not read it for context on RTranslator itself.
