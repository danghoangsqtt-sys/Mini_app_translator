# Task 9.8 — Audit stabilization hotfixes

**Status**: in_progress
**Requests**: `BUG-027`, `BUG-028`, `BUG-029`, `BUG-030`
**Depends on**: Task 9.7 Gate A evidence at `f166217`; Task 9.7 remains release NO-GO and resumes after this remediation
**Owner split**: PM owns ViePilot state and acceptance; implementation may change only the locked application/test paths below

## Objective

Remove the four release-blocking defects found by the 2026-09-28 deep audit without widening Phase 9: synchronize project state, prevent stale Conversation restoration from creating a crash loop, make service binding/unbinding exact and idempotent, and serialize recent-peer persistence so the peer image cannot race its identity row.

## Paths

- `.viepilot/ROADMAP.md`
- `.viepilot/TRACKER.md`
- `.viepilot/HANDOFF.json`
- `.viepilot/phases/phase-7-operational-state-build-portability/PHASE-STATE.md`
- `.viepilot/phases/phase-9-no-key-translation/SPEC.md`
- `.viepilot/phases/phase-9-no-key-translation/PHASE-STATE.md`
- `.viepilot/phases/phase-9-no-key-translation/tasks/TASK-9.8.md`
- `.viepilot/requests/BUG-026.md`
- `.viepilot/requests/BUG-027.md`
- `.viepilot/requests/BUG-028.md`
- `.viepilot/requests/BUG-029.md`
- `.viepilot/requests/BUG-030.md`
- `CHANGELOG.md`
- `app/src/main/java/nie/translator/rtranslatordevedition/tools/CustomServiceConnection.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationActivity.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/communication/ConversationBluetoothCommunicator.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/communication/recent_peer/RecentPeersDataManager.java`
- Focused JVM/instrumentation tests under `app/src/test/` and `app/src/androidTest/` matching the production packages above

## Subtasks

### 9.8.1 — Canonical state synchronization (`BUG-027`)

- Make ROADMAP, TRACKER, HANDOFF, Phase 7 state, and Phase 9 state agree.
- Restore the missing `BUG-026` request from verified debug evidence.
- Correct stale backlog statuses without rewriting historical decision logs.

### 9.8.2 — Safe persisted-mode restore (`BUG-028`)

- Treat the saved Conversation mode as a hint only.
- Restore Conversation only when the application can prove a currently connected peer/session; otherwise show Pairing.
- Downgrade the persisted mode when the final peer disconnects or a terminal startup path cannot continue.
- Preserve normal background/foreground continuity while a genuine Conversation session remains active.

### 9.8.3 — Binding lifecycle ownership (`BUG-029`)

- Retain only successful bind attempts and track their releasable state explicitly.
- Make disconnect/unbind idempotent and contain platform `Service not registered` races.
- Surface false bind, null binding, binding death, and pre-connect failure through the recoverable response path.

### 9.8.4 — Serialized recent-peer persistence (`BUG-030`)

- Replace raw Thread/AsyncTask mixing with one bounded serial executor.
- Query/update the addressed peer directly after the identity insert rather than scanning a race-prone snapshot.
- Remove the never-populated cache or give it coherent defensive ownership.
- Dispatch defensive result snapshots on the expected callback thread.

## Acceptance criteria

- [ ] `BUG-027`: all active ViePilot documents agree; the missing `BUG-026` artifact exists.
- [ ] `BUG-028`: a stale saved Conversation value cannot auto-enter the mode after process loss; active-session restore still works.
- [ ] `BUG-029`: false bind, failure before connect, repeated disconnect, null binding, and binding death cannot lead to an illegal unbind.
- [ ] `BUG-030`: a deterministic queued identity-then-image scenario persists the final peer image/row in order.
- [ ] Focused regression tests pass, followed by full JVM, lint, debug APK, and API 36 instrumentation gates.
- [ ] No new lint error, no credential/Cloud fallback, no dependency/version/signing change, and `.viepilot/debug/` remains untouched.
- [ ] Task 9.7 returns to `in_progress` after code verification; physical two-phone retest remains mandatory before release PASS.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
git diff --check
```

## Forbidden changes

- No dependency upgrade, database schema/version migration, Wi-Fi transport work, Cloud fallback, application ID/package change, version bump, release signing, release tag, force push, or publication.
- Do not close Task 9.7 or Phase 9 using emulator-only evidence.
- Do not delete, stage, or modify `.viepilot/debug/`.

## Execution notes

- Planning baseline is created by `/vp-evolve`; `/vp-auto` owns implementation and verification.
- Commit logical fixes separately where practical. Do not push until PM review and the task persistence gate explicitly permits it.
