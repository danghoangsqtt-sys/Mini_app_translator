# Task 9.14 — Promotion-first voice services and recreation-safe permissions

**Status**: planned
**Requests**: `BUG-015`, `BUG-032`
**Depends on**: Task 9.13

## Objective

Make both voice-service boot paths promotion-first and deterministic, move permission-result ownership out of transient Fragment booleans, and guarantee that failure, denial, recreation, cancellation, and teardown return to a safe state without an orphan service, wake-lock reacquisition, or persisted crash route.

## Paths

- `app/src/main/AndroidManifest.xml` only if the existing declarations need a verified correction
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationActivity.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/main/ConversationMainFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieFragment.java`
- New narrowly scoped launch/permission state-machine classes under `voice_translation/`
- Focused tests under `app/src/test/` and `app/src/androidTest/`

## Plan

1. Make base/subclass `Service.onCreate()` minimal: no TTS, engine, model, wake-lock, Bluetooth helper, controller, or listener initialization before the start intent is validated and foreground promotion succeeds.
2. Introduce one explicit service bootstrap state machine (`NEW → PROMOTED → INITIALIZING → READY → STOPPING → CLOSED`). Promote once with `ServiceCompat.startForeground()` and the minimum declared types before expensive work; rollback partial initialization in reverse order.
3. Move permission requests to an Activity-owned Activity Result flow with a saved pending-mode/token. A recreated Activity restores enough state to interpret grant/deny exactly once; Fragments never own a transient `waitingForServicePermission` flag.
4. Re-check prerequisites at caller and service boundaries, normalize launch/promotion exceptions to sanitized diagnostic categories, clear the persisted mode on terminal failure, and show one actionable recovery result even before service binding completes.
5. Make wake-lock/timer ownership generation-guarded. `onDestroy()` closes the generation before cancelling callbacks, and no late timer/helper callback may reacquire resources.
6. Add deterministic delay/failure seams and runtime tests for denied, granted, permanently denied, Activity recreation during the prompt, launch cancellation, slow initialization beyond five seconds, promotion failure, null restart, and repeated start/stop.

## Acceptance criteria

- [ ] Both services promote before any TTS/engine/wake-lock/Bluetooth/controller initialization.
- [ ] Grant resumes one intended mode exactly once after Activity/Fragment recreation; denial/permanent denial returns to Pairing and clears the persisted voice mode exactly once.
- [ ] No UI action can call a null/stale communicator while permission or service connection is pending.
- [ ] Slow/failing initialization cannot trigger `ForegroundServiceDidNotStartInTimeException`, ANR, orphan notification/service, or crash-loop persistence.
- [ ] Start, promotion, partial initialization, cancellation, and teardown are idempotent; late wake-lock/helper callbacks are suppressed.
- [ ] API 36 runtime tests wait beyond the foreground deadline and assert process survival, correct service type, and no fatal/ANR.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
git diff --check
```

## Forbidden changes

- No weaker/false foreground-service type, background-start exemption claim, automatic permission grant, hidden Cloud fallback, dependency upgrade, version bump, tag, push, or physical-device PASS.
- Do not treat `stopSelf()` after failed promotion as sufficient without a runtime test proving the process stays alive beyond the platform deadline.
- Do not touch `.viepilot/debug/`.
