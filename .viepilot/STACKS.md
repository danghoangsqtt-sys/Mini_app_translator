# Stack Index — RTranslator

No `~/.viepilot/stacks/{stack}/SUMMARY.md` external research cache was generated for this brownfield import (see `.viepilot/BROWNFIELD-TRACE.md` scope-reduction note) — the stack's concrete Do/Don't guardrails for this codebase are already captured as individual findings in `.viepilot/requests/`, which is more precise than generic external research for a legacy codebase with known, specific defects.

## Detected stacks

| Stack | Version | Guardrails source |
|---|---|---|
| Android SDK / Java | compileSdk 29, AGP 3.6.1 | `.viepilot/requests/ENH-011.md` |
| Room (SQLite ORM) | 2.1.0 | `.viepilot/requests/BUG-007.md` |
| gRPC / Protobuf | 1.11.0 | `.viepilot/requests/BUG-005.md`, `ENH-008.md` |
| google-auth-library-oauth2-http | 0.8.0 | `.viepilot/requests/BUG-001.md`, `BUG-009.md` |
| BluetoothCommunicator (third-party) | 1.0.6 | `.viepilot/requests/ENH-001.md` |

If deep external best-practice research is wanted later, re-run `/vp-crystallize --brownfield` and request Step 1B explicitly.
