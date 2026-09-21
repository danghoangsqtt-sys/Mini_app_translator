# Phase State — Phase 3: Android Modernization

- **Status**: planned (not started)
- **Sequenced after**: Phase 2 — Correctness & Robustness
- **Created**: 2026-09-19 via `/vp-evolve BUG-013 ENH-012` (user chose to bundle `ENH-011` in, since `BUG-013` is meaningless without it)
- **Tasks**: 0/6 complete

| Task | Request | Status |
|---|---|---|
| 3.1 — Toolchain / targetSdk upgrade | `ENH-011` | not started |
| 3.2 — Bluetooth runtime permissions (must ship with 3.1) | `BUG-013` | not started |
| 3.3 — Dependency refresh (Room, Nearby, gRPC, OAuth2, JWT) | `ENH-012` | not started |
| 3.4 — Android 12+ exported component declarations | `BUG-014` | not started |
| 3.5 — Foreground service types and permissions | `BUG-015` | not started |
| 3.6 — Blocking lint and release quality gate | 2026-09-20 `vp-audit` | not started |

## Notes

- No app version bump applied at planning time — `SYSTEM-RULES.md` versioning convention bumps `versionCode`/`versionName` on release builds, not on phase planning. Bump when task 3.1–3.3 actually land.
- The 2026-09-20 audit set the platform target to API 36 because that is the Google Play submission requirement from 31 August 2026 for ordinary Android apps.
- Tasks 3.1, 3.2, 3.4, and 3.5 must ship together; separating them creates install/runtime failures on current Android versions.
- Run `/vp-audit --tier3` after these land to confirm the findings are resolved.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `assembleRelease` passed on the legacy toolchain on 2026-09-20, but lint had 5 errors/119 warnings with `abortOnError=false`; there was no connected device and the release APK was unsigned. These are not phase-exit evidence.
- The PM-confirmed entry lint baseline is 5 errors/121 warnings. The 5 errors are three `ResourceType` findings in `GridLabelRenderer` and two `InvalidPackage` findings from `grpc-core:1.11.0`; Task 3.6 must compare lint by ID, source, and count rather than Gradle exit code. The 119-warning figure is historical 2026-09-20 provenance only. No lint error or warning may be mass-suppressed.
- Phase 3 remains planned with no active task. The entry contract is documentation only; it authorizes neither source/build changes nor Task 3.1 implementation.
