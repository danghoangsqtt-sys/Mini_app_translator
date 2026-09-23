# Phase 6 — Bug Fix Sprint

**Source**: VP-Audit 2026-09-22 (174 warnings, 0 errors)  
**Mode**: Refactor + Bug Fix  
**Target version**: 1.1.3 (PATCH release)  
**Constraint**: No dependency upgrades

## Goal

Sửa tất cả lint warnings có thể fix mà không cần nâng dependencies. Task 6.1 (Handler Looper) đã hoàn thành trước phase này.

## Tasks

| Task | Description | Priority |
|------|-------------|----------|
| 6.1 | Handler Looper fix | ✅ Done |
| 6.2 | UnknownIdInLayout analysis | Low |
| 6.3 | contentDescription cho 13 ImageViews | Medium |
| 6.4 | Overdraw trong 5 layouts | Low |
| 6.5 | InflateParams trong 3 Dialog inflate calls | Low |
| 6.6 | CanvasSize trong GraphView + RoundedCornerLayout | Low |
| 6.7 | FileLog hardcoded path | ✅ Done (already Logcat) |
| 6.8 | DefaultLocale trong Translator.java | Low |
| 6.9 | DataExtractionRules cho Android 12+ | Medium |
| 6.10 | Build + Install + Verify | — |

## Acceptance Criteria

- [ ] assembleDebug passes
- [ ] testDebugUnitTest passes (regression-free)
- [ ] Lint warning count reduced (target: <160 from 174)
- [ ] No new lint errors introduced
- [ ] Version bumped to 1.1.3 in build.gradle
