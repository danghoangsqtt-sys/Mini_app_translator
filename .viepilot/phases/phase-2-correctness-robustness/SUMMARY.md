# Phase 2: Correctness & Robustness - Summary

## Overview

- **Started**: 2026-09-20
- **Completed**: 2026-09-21
- **Duration**: 2 calendar days
- **Status**: Complete / PASS
- **Final gate commit**: `d7c591f95b3fde595a0ad8aac722ac033baf0bfa`
- **Remote persistence at gate closure**: `origin/master = d7c591f`

## Completed Tasks

| # | Task | Commits | Notes |
|---|------|---------|-------|
| 2.1 | Single Room database instance | `ac83ecf` | One application-context Room builder; migration intentionally deferred until a schema change. |
| 2.2 | Crypto helper reachability | `4330c12`, `f30212f`, `dc9103c` | Removed unreachable AES/CTR API; credential persistence remains AES-GCM. |
| 2.3 | API token publication | `7d1a34f`, `8e38646` | Coordinator serializes token state and invalidates stale queued callbacks. |
| 2.4 | WalkieTalkie binding teardown | `00d73b7`, `2317406`, `bbf8d70` | One active registration per connection; API 36 probe covers duplicate binding semantics. |
| 2.5 | Conversation delayed reconnect | `56f3a7f` | Per-instance reconnect ownership prevents stale post-destroy work. |
| 2.6 | GraphView second-scale reachability | `9a8f850`, `d1c2718` | Closed as Low future capability; no production GraphView change. |

## Skipped Tasks

| # | Task | Reason |
|---|------|--------|
| None | — | All six planned Phase 2 tasks completed. |

## Key Decisions

| Decision | Rationale |
|----------|-----------|
| Defer Room migration | Task 2.1 changes instance ownership only; schema remains version 1. |
| Remove dead AES/CTR helpers | Live source and 141 ancestors showed no caller or persisted consumer. |
| Keep GraphView unchanged | The second-scale throw is not reachable by current application code and is a future capability risk. |
| Accept a limited lint waiver | Final lint contains only the approved five baseline errors; Task 3.6 retains final remediation. |

## Files Changed

> Every path changed by Phase 2, including phase-completion evidence, is listed individually.

### Created

| File | Task |
|------|------|
| `.viepilot/phases/phase-2-correctness-robustness/SUMMARY.md` | Phase completion |
| `.viepilot/phases/phase-2-correctness-robustness/VERIFICATION.md` | Phase completion |
| `.viepilot/phases/phase-2-correctness-robustness/tasks/2.1-single-room-instance.md` | 2.1 |
| `.viepilot/phases/phase-2-correctness-robustness/tasks/2.2-crypto-helper-reachability.md` | 2.2 |
| `.viepilot/phases/phase-2-correctness-robustness/tasks/2.3-token-publication.md` | 2.3 |
| `.viepilot/phases/phase-2-correctness-robustness/tasks/2.4-safe-walkie-teardown.md` | 2.4 |
| `.viepilot/phases/phase-2-correctness-robustness/tasks/2.5-cancel-conversation-callbacks.md` | 2.5 |
| `.viepilot/phases/phase-2-correctness-robustness/tasks/2.6-graphview-branch.md` | 2.6 |
| `app/src/androidTest/AndroidManifest.xml` | 2.4 |
| `app/src/androidTest/java/nie/translator/rtranslatordevedition/BindingProbeService.java` | 2.4 |
| `app/src/androidTest/java/nie/translator/rtranslatordevedition/ServiceConnectionBindingContractInstrumentedTest.java` | 2.4 |
| `app/src/androidTest/java/nie/translator/rtranslatordevedition/database/AppDatabaseInstrumentationTest.java` | 2.1 |
| `app/src/main/java/nie/translator/rtranslatordevedition/ApiTokenCallbackDispatcher.java` | 2.3 |
| `app/src/main/java/nie/translator/rtranslatordevedition/ApiTokenCoordinator.java` | 2.3 |
| `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ScoReconnectCoordinator.java` | 2.5 |
| `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/BindingAttemptTracker.java` | 2.4 |
| `app/src/test/java/nie/translator/rtranslatordevedition/ApiTokenCallbackDispatcherTest.java` | 2.3 |
| `app/src/test/java/nie/translator/rtranslatordevedition/ApiTokenCoordinatorTest.java` | 2.3 |
| `app/src/test/java/nie/translator/rtranslatordevedition/database/AppDatabaseSingletonSourceTest.java` | 2.1 |
| `app/src/test/java/nie/translator/rtranslatordevedition/tools/UnauthenticatedCryptoHelperSourceTest.java` | 2.2 |
| `app/src/test/java/nie/translator/rtranslatordevedition/tools/gui/graph/SecondScaleReachabilityProofTest.java` | 2.6 |
| `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationServiceReconnectSourceTest.java` | 2.5 |
| `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ScoReconnectCoordinatorTest.java` | 2.5 |
| `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/BindingAttemptTrackerTest.java` | 2.4 |
| `app/src/test/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieServiceBindingSourceTest.java` | 2.4 |

### Modified

| File | Task |
|------|------|
| `.viepilot/HANDOFF.json` | Phase state / completion |
| `.viepilot/ROADMAP.md` | Phase state / completion |
| `.viepilot/TRACKER.md` | Phase state / completion |
| `.viepilot/phases/phase-2-correctness-robustness/PHASE-STATE.md` | Phase state / completion |
| `.viepilot/phases/phase-2-correctness-robustness/SPEC.md` | Phase 2 contract |
| `.viepilot/requests/BUG-007.md` | 2.1 |
| `.viepilot/requests/BUG-008.md` | 2.2 |
| `.viepilot/requests/BUG-009.md` | 2.3 |
| `.viepilot/requests/BUG-010.md` | 2.4 |
| `.viepilot/requests/BUG-011.md` | 2.5 |
| `.viepilot/requests/BUG-012.md` | 2.6 |
| `CHANGELOG.md` | 2.1–2.5 |
| `app/build.gradle` | 2.1 |
| `app/src/androidTest/java/nie/translator/rtranslatordevedition/api_management/CredentialStoreInstrumentedTest.java` | 2.1 |
| `app/src/main/java/nie/translator/rtranslatordevedition/Global.java` | 2.3 |
| `app/src/main/java/nie/translator/rtranslatordevedition/api_management/ConsumptionsDataManager.java` | 2.1 |
| `app/src/main/java/nie/translator/rtranslatordevedition/database/AppDatabase.java` | 2.1 |
| `app/src/main/java/nie/translator/rtranslatordevedition/database/dao/MyDao.java` | 2.1 |
| `app/src/main/java/nie/translator/rtranslatordevedition/tools/Tools.java` | 2.2 |
| `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationService.java` | 2.5 |
| `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/communication/recent_peer/RecentPeersDataManager.java` | 2.1 |
| `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieService.java` | 2.4 |

### Deleted

| File | Task |
|------|------|
| None | — |

## Metrics

| Metric | Value |
|--------|-------|
| Tasks completed | 6 |
| Tasks skipped | 0 |
| Commits | 15 (14 through final gate plus this completion-state commit) |
| Lines added | 2,696 through final gate |
| Lines removed | 292 through final gate |
| Test coverage | Final gate: 39 JVM tests and 7 API 36 instrumentation tests passed |

## Lessons Learned

- Lifecycle correctness required controlled dispatch and Android binding tests, not source scans alone.
- A reachability audit can correctly close a future-risk item without unnecessary production churn.
- Gradle 5.6.4 must be run with JDK 11; JBR 25 is incompatible.

## Notes

No physical SCO Bluetooth verification was performed. The final lint waiver does not suppress the
121 warnings or transfer Task 3.6 into Phase 2.

---

Git Tag: not created; tag creation requires separate authorization.
