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

1. **2.1 / BUG-007** — Expose one process-wide Room database and inject/reuse its DAO in both managers. No schema/data migration is performed here; migration design is deferred until the first schema version change.
2. **2.2 / BUG-008** — First verify `Tools.encript/decript` reachability and persisted-data use; remove dead helpers if unused, otherwise replace unauthenticated encryption with versioned AEAD migration. `CredentialStore` already uses AES-GCM and is not the target of this task.
3. **2.3 / BUG-009** — Serialize token refresh/state publication and test concurrent readers.
4. **2.4 / BUG-010** — Track binding state and only unbind active recognizer connections.
5. **2.5 / BUG-011** — Give each service instance ownership of its delayed reconnect runnable, cancel only that runnable during teardown, and reject work after destruction.
6. **2.6 / BUG-012** — Verify reachability of the GraphView second-scale branch. Current evidence reclassifies it as a Low future capability; do not change production GraphView solely to create a diff.

Individual execution contracts are in `tasks/2.1-*` through `tasks/2.6-*`. Start only after the Phase 1 exit gate; preserve the existing database and credential formats unless a documented migration is required.

## Acceptance

- Lifecycle tests exercise create/start/stop/destroy repeatedly.
- Database and crypto migrations preserve valid user data or fail with a clear recovery path.
- No task changes UI styling or target SDK, keeping regression scope reviewable.
- `BUG-008` and `BUG-012` are reclassified if reachability checks show no production caller; do not claim an active crash or data migration without evidence.
- PM accepted all six implementations locally. This is distinct from phase completion: 0/6 satisfy the Git-persistence/quality gate until authorized remote persistence, state synchronization, and the shared lint decision are complete.
- The shared lint baseline is five errors and 121 warnings. It is not waived here; Phase 3 task 3.6 remains its owner and is not part of Phase 2.
