# Task 9.16 — Bounded session queues and Binder-safe history

**Status**: planned
**Request**: `BUG-035`
**Depends on**: Task 9.15

## Objective

Bound every release-reachable voice/Bluetooth session queue by count and retained bytes, define recoverable backpressure, and keep every Messenger/Binder snapshot far below the process-wide transaction limit without changing the Bluetooth wire protocol.

## Paths

- `app/src/main/AndroidManifest.xml` only to remove `android:largeHeap` if the bounded implementation passes without it
- `app/src/main/java/com/bluetooth/communicator/BluetoothCommunicator.java`
- `app/src/main/java/com/bluetooth/communicator/Message.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/ConversationOnDeviceController.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/communication/ConversationBluetoothCommunicator.java`
- New reusable bounded queue/byte-budget helper in the owning packages
- Focused stress tests under `app/src/test/` and `app/src/androidTest/`

## Locked initial budgets

- Incoming translation lane: at most 16 queued payloads and 256 KiB retained UTF-8 payload bytes.
- Pending Bluetooth text/control: at most 32 entries and 256 KiB retained payload bytes.
- Pending Bluetooth data: at most 4 entries and 1 MiB retained payload bytes; each item must still satisfy the existing 256 KiB channel limit.
- Service/UI history: at most 200 final messages and 256 KiB retained text; partial previews are never retained as history.
- One attributes reply: most-recent messages only, measured serialized `Parcel` size at or below 128 KiB.

## Plan

1. Centralize overflow-safe byte accounting with defensive copies; reject negative/overflowed sizes and release counters exactly once on success, failure, cancellation, disconnect, and teardown.
2. Preserve FIFO for accepted Conversation translation work. On overflow, reject newest peer work, continue the existing queue, and emit one coalesced sanitized busy/overflow signal without including payload or identity.
3. Bound vendored transport output queues without altering headers/fragments. Never silently drop disconnect/control semantics; apply explicit rejection/callback behavior before allocation where possible.
4. Retain only final bounded UI history. Build a measured recent snapshot for `GET_ATTRIBUTES`; if the byte ceiling is reached, stop before adding the next message and return truncation metadata.
5. Remove `android:largeHeap="true"` if the full stress/runtime gate passes; otherwise document the unrelated measured requirement instead of using it as queue protection.
6. Stress with at least 10,000 offered translations/messages, stalled callbacks, reconnect/teardown, near-limit UTF-8, counter overflow attempts, and repeated attribute restores.

## Acceptance criteria

- [ ] All four queues/histories remain within both count and byte budgets under stalled or hostile input.
- [ ] Overflow is recoverable, sanitized, observable, and cannot corrupt accepted FIFO work or Bluetooth framing.
- [ ] Every attributes reply is measured at or below 128 KiB and reports truncation without `TransactionTooLargeException`.
- [ ] Disconnect/close releases retained bytes and rejects new work; no late callback underflows counters or restarts a queue.
- [ ] Stress tests and normal Conversation/Walkie behavior pass without relying on a larger heap for queue safety.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug assembleRelease verifyReleasePrivacy --console=plain --no-daemon
.\gradlew.bat connectedDebugAndroidTest --console=plain --no-daemon
git diff --check
```

## Forbidden changes

- No Bluetooth wire-format/API break, silent control-frame loss, payload logging, unbounded retry queue, dependency upgrade, version bump, tag, push, or physical-device PASS.
- Do not use `largeHeap`, GC calls, or swallowed `OutOfMemoryError` as the bounding mechanism.
- Do not touch `.viepilot/debug/`.
