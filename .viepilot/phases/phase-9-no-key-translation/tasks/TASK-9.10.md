# Task 9.10 — Bluetooth permissions and foreground-service lifecycle

**Status**: planned
**Requests**: `BUG-013`, `BUG-015`
**Depends on**: Task 9.9

## Objective

Request only permissions used by the active Bluetooth code on each Android generation and start both voice services through the documented foreground-service path with prompt typed promotion and recoverable failure behavior.

## Locked paths

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationActivity.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_walkie_talkie_mode/_walkie_talkie/WalkieTalkieService.java`
- Focused permission/service lifecycle tests under `app/src/test/` and `app/src/androidTest/`

## Plan

1. Remove unused Wi-Fi permissions and stop requesting location on API 31+ when `BLUETOOTH_SCAN` is declared `neverForLocation`; preserve legacy location requirements only through API 30.
2. Start service work with `ContextCompat.startForegroundService()` from the visible Activity after runtime prerequisites pass.
3. Promote promptly in `onStartCommand()` with the minimum active service types on supported APIs; keep null-intent/restart and bind/unbind behavior safe.
4. Cover API 23/30/31/32/33/36 permission arrays, deny-location/grant-Bluetooth behavior, notification promotion, and start failures.

## Acceptance criteria

- [ ] API 31+ requests exactly Bluetooth scan/connect/advertise and does not gate on location or Nearby Wi-Fi.
- [ ] API 23–30 retains the minimum legacy permission set.
- [ ] Both services satisfy the foreground start deadline and typed-service contract without duplicate teardown/promotion.
- [ ] Automated tests pass; API 31/34/36 physical confirmation remains Task 9.7 evidence.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug --console=plain --no-daemon
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
git diff --check
```
