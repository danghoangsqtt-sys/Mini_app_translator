# Phase State — Phase 3: Android Modernization

- **Status**: in_progress (Cluster A static PASS; device evidence BLOCKED)
- **Sequenced after**: Phase 2 — Correctness & Robustness
- **Created**: 2026-09-19 via `/vp-evolve BUG-013 ENH-012` (user chose to bundle `ENH-011` in, since `BUG-013` is meaningless without it)
- **Tasks**: 4/6 static PASS (device evidence pending); 1/6 not started (3.3); 3.6 partially addressed

| Task | Request | Status |
|---|---|---|
| 3.1 — Toolchain / targetSdk upgrade | `ENH-011` | in_progress — compileSdk/targetSdk 36, AGP 8.13.2, Gradle 8.13 (static PASS; device BLOCKED) |
| 3.2 — Bluetooth runtime permissions (must ship with 3.1) | `BUG-013` | in_progress — `BLUETOOTH_SCAN`/`CONNECT`/`ADVERTISE` declared, runtime request flow added (static PASS; device BLOCKED) |
| 3.3 — Dependency refresh (Room, Nearby, gRPC, OAuth2, JWT) | `ENH-012` | not started — deferred to a dedicated `/vp-evolve` (API migration / backward-compat risk) |
| 3.4 — Android 12+ exported component declarations | `BUG-014` | in_progress — launcher `android:exported="true"` (static PASS; device BLOCKED) |
| 3.5 — Foreground service types and permissions | `BUG-015` | in_progress — `microphone|connectedDevice` types + permissions (static PASS; device BLOCKED) |
| 3.6 — Blocking lint and release quality gate | 2026-09-20 `vp-audit` | partially addressed — 0 lint errors / 136 warnings after Phase 6; device/release gate pending |

## Notes

- No app version bump applied at planning time — `SYSTEM-RULES.md` versioning convention bumps `versionCode`/`versionName` on release builds, not on phase planning. Bump when task 3.1–3.3 actually land.
- The 2026-09-20 audit set the platform target to API 36 because that is the Google Play submission requirement from 31 August 2026 for ordinary Android apps.
- Tasks 3.1, 3.2, 3.4, and 3.5 must ship together; separating them creates install/runtime failures on current Android versions.
- Run `/vp-audit --tier3` after these land to confirm the findings are resolved.
- **Cluster A is a static PASS only.** The two-phone Bluetooth/SCO device evidence remains **PENDING HUMAN**; the phase is not closed until real-device QA runs (API 23/31/34/36 + physical two-phone Bluetooth pair).
- Task 3.3 (dependency refresh) is intentionally out of scope for the Cluster A work to avoid an unmanaged API migration; handle it in a dedicated `/vp-evolve` after device evidence.
- `testDebugUnitTest`, `lintDebug`, `assembleDebug`, and `assembleRelease` passed on the legacy toolchain on 2026-09-20, but lint had 5 errors/119 warnings with `abortOnError=false`; there was no connected device and the release APK was unsigned. These are not phase-exit evidence.
- The PM-confirmed entry lint baseline is 5 errors/121 warnings. The 5 errors are three `ResourceType` findings in `GridLabelRenderer` and two `InvalidPackage` findings from `grpc-core:1.11.0`; Task 3.6 must compare lint by ID, source, and count rather than Gradle exit code. The 119-warning figure is historical 2026-09-20 provenance only. No lint error or warning may be mass-suppressed.
- Phase 3 is now `in_progress`: Cluster A (3.1, 3.2, 3.4, 3.5) is committed as a static PASS. The phase is not closed and Task 3.3 (dependency refresh) is explicitly deferred to a dedicated `/vp-evolve`; do not start it without a new authorization. The remaining exit gate is real two-phone Bluetooth/SCO device evidence.
