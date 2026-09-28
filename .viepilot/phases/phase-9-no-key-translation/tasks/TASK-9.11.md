# Task 9.11 — Cancellable asynchronous service binding

**Status**: done — automated/API 36 PASS
**Request**: `BUG-029`
**Depends on**: Task 9.10

## Objective

Give Conversation and Walkie fragments a synchronous, cancellable ownership handle at connect initiation so stopping a fragment wins races against language lookup, service start, framework bind, and `onServiceConnected()` delivery.

## Locked paths

- `app/src/main/java/nie/translator/rtranslatordevedition/tools/CustomServiceConnection.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationActivity.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/main/ConversationMainFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieFragment.java`
- New narrowly scoped connection-handle class in the same production packages
- Focused JVM/instrumentation tests under `app/src/test/` and `app/src/androidTest/`

## Plan

1. Return an idempotent handle before any asynchronous language/service work begins.
2. Check cancellation/generation before start, before bind, and before callback delivery; attach the real connection atomically to the handle.
3. On cancel/failure, unbind only registered work, remove list ownership, stop a just-started service when no valid client remains, and suppress Fragment/UI callbacks.
4. Add deterministic stop-before-language, stop-before-connect, late-connect, false-bind, and normal lifecycle tests for both modes.

## Acceptance criteria

- [x] `onStop()` cancels the exact pending/active request without placeholder communicator IDs.
- [x] No stopped Fragment is retained or receives restore/error callbacks.
- [x] Failure/cancellation cannot leave an orphan started service or illegal unbind.
- [x] Normal reconnect and configuration flow remain functional and idempotent.

## Evidence

- Planning baseline / implementation: `mini-app-translator-vp-p9-t11` / `066002c`.
- An immediate `ServiceConnectionHandle` owns callbacks and cancellation from initiation; `CustomServiceConnection` clears retained callbacks on release and rejects late delivery.
- Binding plus registration is synchronized against cancellation, and Activity pending/active lists are finalized atomically before callbacks can outlive their Fragment.
- Verification: 174 JVM tests, 24 Pixel 7a API 36 instrumentation tests, lint 0 errors/122 warnings, debug assembly PASS, and clean `git diff --check`.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
git diff --check
```
