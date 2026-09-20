<!-- crystallize_version: 0.8.0 -->

# Project Context — RTranslator

## Product scope

RTranslator lets two people who don't share a language have a real-time spoken conversation: each phone captures speech, sends it to Google Cloud for recognition + translation, and speaks the translated result — either over a live Bluetooth link between two phones (Conversation mode) or on a single phone used in turns (WalkieTalkie mode). Users bring their own Google Cloud billing account (via a service-account JSON key) rather than the app operator paying for API usage.

## Phase overview

No greenfield phases were brainstormed for this import — this project was crystallized in **brownfield mode** purely to make an existing codebase audit's findings trackable. There is currently one working phase:

- **Phase 1 — Security & Stability Hardening**: address the Critical/High findings from the 2026-09-19 `/vp-audit` pass (credential storage, data corruption bug, race conditions) before any new feature work. See `.viepilot/ROADMAP.md`.

Further phases (new features, toolchain upgrade, branch consolidation) are expected to be defined later via `/vp-evolve` once Phase 1 is triaged.

## Anti-goals

- This ViePilot setup is **not** a rewrite or fork announcement — it does not change the app's public identity (README, LICENSE, package name, repo).
- Not scoped to decide whether to move development to the `v3.00` branch — that's flagged as an open question (`ENH-015`), not decided here.

## Domain knowledge

- **Credential model**: Each user supplies their own GCP service-account JSON key with `cloud-platform` scope. This is the most sensitive data the app handles — see `.viepilot/requests/BUG-001.md` and `BUG-002.md`.
- **Two independent conversation UX modes** (Conversation vs WalkieTalkie) share the same underlying Google Cloud Speech/Translation calls but differ in transport (persistent Bluetooth session vs local dual-language listening).
- **No server component** — this is a pure client app; all state is local (Room DB) or in the user's own GCP project.

## Constraints

- Upstream project identity files (README.md, LICENSE.txt, NOTICE.txt) are out of scope for ViePilot-driven edits unless the user explicitly asks.
- Any fix work should target the correct branch — confirm `master` vs `v3.00` before starting non-trivial changes (see `ENH-015`).

## Known issues register

All current known issues are tracked as individual requests under `.viepilot/requests/` (12 `BUG-*`, 17 `ENH-*`), sourced from the `/vp-audit` pass on 2026-09-19. Summary table in `.viepilot/TRACKER.md`.
