# Task 9.15 — Generation-safe voice UI snapshot and callbacks

**Status**: planned
**Request**: `BUG-034`
**Depends on**: Task 9.14

## Objective

Remove message-before-attributes and callback-after-view races by giving each bound UI instance one generation-scoped snapshot/event gate with explicit listener cancellation and deterministic message ordering.

## Paths

- `app/src/main/java/nie/translator/rtranslatordevedition/tools/CustomServiceConnection.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/tools/services_communication/ServiceCommunicator.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/main/ConversationMainFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieFragment.java`
- New narrow UI event/snapshot helper under `voice_translation/`
- Focused tests under `app/src/test/` and `app/src/androidTest/`

## Plan

1. Create the empty `MessagesAdapter` synchronously in `onViewCreated()` so rendering never depends on a later Binder reply.
2. Add a monotonically increasing service message revision. `GET_ATTRIBUTES` returns a bounded snapshot plus its revision; live message callbacks carry their revision.
3. Buffer live events until the snapshot is installed, then apply only events newer than the snapshot revision in order. Suppress duplicates, old-generation events, and late partial/final callbacks.
4. Return cancellable attribute requests. Disconnect and `onDestroyView()` clear attribute listeners, close the view generation, remove queued callbacks where possible, detach adapters/listeners, and null view references.
5. Render only when the matching view lifecycle is at least STARTED. Recovery paths may update state, but never a detached RecyclerView, Activity, dialog, or control.
6. Add deterministic tests for message-before-attributes, final/partial ordering, duplicate revision, attributes-after-stop, old generation after recreate, rapid enter/leave, and normal reconnect in both modes.

## Acceptance criteria

- [ ] `mAdapter`, RecyclerView, scroller, and controls cannot be dereferenced before initialization or after their view generation closes.
- [ ] Snapshot plus live callbacks are lossless and duplicate-free for one generation.
- [ ] Disconnect/onDestroyView cancels attribute listeners and suppresses queued callbacks from the old connection.
- [ ] Recreate/reconnect cannot render an old mode/service generation into the new Fragment.
- [ ] Focused JVM and API 36 instrumentation tests pass for Conversation and WalkieTalkie.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
git diff --check
```

## Forbidden changes

- No protocol framing change, message-content logging, UI redesign, dependency upgrade, version bump, tag, push, or physical-device PASS.
- Do not retain Fragment/View/Activity references beyond the matching view generation.
- Do not touch `.viepilot/debug/`.
