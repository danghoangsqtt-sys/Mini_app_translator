# Task 9.12 — Bounded recent-peer persistence

**Status**: done — automated/API 36/release PASS
**Request**: `BUG-030`
**Depends on**: Task 9.11

## Objective

Retain Task 9.8 ordering guarantees while replacing the unbounded executor queue with fixed-capacity, keyed/coalescing serial work and deterministic persistence failure reporting.

## Locked paths

- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/communication/recent_peer/RecentPeersDataManager.java`
- New bounded executor/repository helper in the same package
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/communication/ConversationBluetoothCommunicator.java` only for failure/rejection propagation
- Focused JVM tests under the matching `app/src/test/` package

## Plan

1. Implement a one-worker bounded queue with stable keys: preserve identity-before-image ordering, coalesce superseded updates for one peer, and reject distinct overflow deterministically.
2. Catch Room/runtime write failures at the repository boundary and dispatch a sanitized failure callback on the expected thread.
3. Ensure callback snapshots and byte arrays remain defensive; shutdown clears pending work without accepting new writes.
4. Test burst/coalescing, overflow, per-peer ordering, database exceptions, shutdown, and post-failure progress.

## Acceptance criteria

- [x] Pending work has a small documented upper bound independent of peer traffic volume.
- [x] Latest same-peer state is retained without violating identity/image ordering.
- [x] Overflow and Room failures are observable, sanitized, and do not kill the worker.
- [x] Existing deterministic ordering test and full quality gates pass.

## Evidence

- Planning baseline / implementation: `mini-app-translator-vp-p9-t12` / `484e67f`.
- The internal queue holds at most 32 pending tasks and one running task; the single backing worker has an `ArrayBlockingQueue` capacity of one. Same-key updates replace pending work in place.
- Tests cover 100-image coalescing, distinct-key overflow, identity-before-image ordering, defensive bytes/lists, Room exception recovery, observer isolation, shutdown, and post-close rejection.
- Final gate: 184 JVM tests, 24 Pixel 7a API 36 instrumentation tests, lint 0 errors/122 warnings, debug + unsigned release assembly, R8 DEX privacy PASS, and clean `git diff --check`.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
git diff --check
```
