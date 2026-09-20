# Phase State — Phase 1: Security & Stability Hardening

- **Status**: in progress
- **Tasks**: 1/6 complete
- **Current task**: 1.2 — Exclude secrets from backup
- **Created**: 2026-09-20 via `/vp-evolve` after `/vp-audit`

| Task | Request | Status |
|---|---|---|
| 1.1 — Protect stored GCP key | `BUG-001` | done |
| 1.2 — Exclude secrets from backup | `BUG-002` | not started |
| 1.3 — Fix byte-array merge | `BUG-003` | not started |
| 1.4 — Fix recorder teardown race | `BUG-004` | not started |
| 1.5 — Handle gRPC stream errors | `BUG-005` | not started |
| 1.6 — Replace unrestricted key-file scan | `BUG-006` | not started |

## Entry gate

- Work on the current fork branch (`master` tracking `origin/master`).
- Install JDK/Android SDK or use Android Studio's bundled JBR before build verification.
- Preserve a clean copy of the user's uncommitted `images/` changes.

## Exit gate

- All six fixes have focused regression coverage.
- No service-account credential is included in backup, logs, screenshots, or test fixtures.
- `testDebugUnitTest`, `lintDebug`, and `assembleDebug` pass in the supported toolchain.
