# Phase 2 — Correctness & Robustness (SPEC)

## Goal

Remove the confirmed medium-severity correctness defects and make lifecycle behavior deterministic before the Android/UI upgrades.

## Paths

```text
app/src/main/java/nie/translator/rtranslatordevedition/Global.java
app/src/main/java/nie/translator/rtranslatordevedition/database/AppDatabase.java
app/src/main/java/nie/translator/rtranslatordevedition/api_management/ConsumptionsDataManager.java
app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/communication/recent_peer/RecentPeersDataManager.java
app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationService.java
app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieService.java
app/src/main/java/nie/translator/rtranslatordevedition/tools/gui/graph/
app/src/test/
app/src/androidTest/
```

## Tasks

1. **2.1 / BUG-007** — Expose one process-wide Room database and inject/reuse its DAO in both managers.
2. **2.2 / BUG-008** — Replace unauthenticated AES/CTR storage with an AEAD construction and versioned migration.
3. **2.3 / BUG-009** — Serialize token refresh/state publication and test concurrent readers.
4. **2.4 / BUG-010** — Track binding state and only unbind active recognizer connections.
5. **2.5 / BUG-011** — Remove pending callbacks/messages during service teardown and reject work after destruction.
6. **2.6 / BUG-012** — Verify reachability, then implement or structurally prevent the GraphView second-scale crash branch.

## Acceptance

- Lifecycle tests exercise create/start/stop/destroy repeatedly.
- Database and crypto migrations preserve valid user data or fail with a clear recovery path.
- No task changes UI styling or target SDK, keeping regression scope reviewable.
