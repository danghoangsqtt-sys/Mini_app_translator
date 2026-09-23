# Phase State — Phase 8: Release Readiness & Project Closure

- **Status**: in_progress
- **Tasks**: 1/7 complete
- **Current task**: 8.2 — Resolve source-of-truth and repository hygiene (awaiting PM decision)
- **Created**: 2026-09-23 via `/vp-evolve`
- **Target release**: `1.2.0` (`versionCode 15`); no planning-time version bump
- **Sequenced after**: Phase 7 — Operational State & Build Portability
- **Reserves next feature phase**: Phase 9 — Wi-Fi Hotspot Connection (`ENH-020`)

| Task | Status | Blocking condition |
|---|---|---|
| 8.1 — Accept Phase 7 delivery and freeze baseline | done | Accepted local baseline `76d30b5` / `mini-app-translator-vp-p7-complete` |
| 8.2 — Resolve source-of-truth and repo hygiene | not_started — awaiting PM decision | Maintainer decision for tracked unrelated content |
| 8.3 — Capture pre-refresh device baseline | planned | Two physical phones + emulator/device matrix |
| 8.4 — Refresh dependencies incrementally | planned | 8.3 baseline complete |
| 8.5 — Complete Phase 3/5 device and UI QA | planned | 8.4 complete; devices available |
| 8.6 — Enforce release gate and verify signed artifact | planned | External signing material |
| 8.7 — Close, publish and archive | planned | All earlier gates pass; maintainer approves push/archive |

## Notes

- The separate Phase 7 task was assigned the implementation of `BUG-017`–`BUG-020`; Phase 8 consumes the accepted local delivery rather than reimplementing those changes in parallel.
- Task 8.1 accepted `76d30b5a05d6457bf62b10f3e79aa4e2f44bba2b` (`docs(phase7): finalize audited closeout`) and its annotated tag `mini-app-translator-vp-p7-complete`. The Phase 7 baseline remains local; no push was performed.
- Task 8.2 is deliberately not started. Its `.agents`, `.idea`, branch, and worktree decisions require explicit PM direction.
- Wi-Fi Hotspot work must not begin until this release gate closes.
