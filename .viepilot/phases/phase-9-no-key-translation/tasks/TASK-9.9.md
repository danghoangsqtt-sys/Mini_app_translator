# Task 9.9 — Production privacy and bounded Bluetooth ingress

**Status**: done — automated/API 36/release-artifact PASS; physical legacy-peer interoperability remains Task 9.7
**Requests**: `ENH-007`, `BUG-031`
**Depends on**: Task 9.8 persisted baseline `5c9c70e`; Task 9.7 remains blocked

## Objective

Remove release-reachable Conversation payload/peer-address logs and replace the opaque Bluetooth transport binary with reviewed Apache-2.0 source that preserves protocol compatibility while bounding incomplete, malformed, and oversized inbound frames before whole-message allocation. Validate and decode peer images away from the main thread under explicit encoded-byte, dimension, and pixel budgets.

## Locked paths

- `settings.gradle`
- `app/build.gradle`
- `app/proguard-rules.pro`
- `app/src/main/java/com/bluetooth/communicator/**`
- `third_party/BluetoothCommunicator-1.0.6-NOTICE.md`
- `app/src/main/java/nie/translator/rtranslatordevedition/tools/Tools.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/tools/gui/peers/GuiPeer.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/communication/ConversationBluetoothCommunicator.java`
- First-party files containing sensitive `Log` calls identified by `rg`, limited to payload/name/token/consumption/exception sanitization
- Focused JVM/instrumentation/source-contract tests under `app/src/test/` and `app/src/androidTest/`

## Plan

1. Vendor the exact 1.0.6 source with provenance, remove the remote binary dependency, and preserve public API/wire framing.
2. Add per-channel caps for text/data bytes, fragment count, concurrent incomplete IDs, exact sequence progression, and incomplete-message timeout; drop malformed/unknown work recoverably and clear it on teardown.
3. Remove payload/address logging from the transport and sensitive first-party logging. Strip remaining `android.util.Log` calls from release bytecode while retaining sanitized debug diagnostics.
4. Reject unknown app headers; validate encoded image size and bounds, sample to a fixed pixel budget off-main, and only then persist/update UI. Keep normal 160px JPEG interoperability.
5. Add adversarial assembly/image tests and a release-artifact verification task that rejects known sensitive tags/markers in DEX.

## Acceptance criteria

- [x] Normal 1.0.6 public API/framing is preserved in the reviewed vendored source; physical old/new APK interoperability remains a Task 9.7 gate.
- [x] Oversized, out-of-order, excessive, timed-out, malformed, and unknown-header input cannot grow retained state without bounds or reach UI/Room.
- [x] Image decode does not run on the Bluetooth/main callback and respects encoded-byte/dimension/pixel caps.
- [x] Debug diagnostics contain event metadata only; release DEX contains no sensitive transport/recognizer tags.
- [x] Focused tests plus full JVM/lint/debug/release gates pass.

## Evidence

- Planning baseline: `311a887`; implementation: `6ff92b8`.
- Exact upstream 1.0.6 sources are vendored with SHA-256 provenance; the binary dependency is absent from `debugRuntimeClasspath`.
- Reassembly limits: 64 KiB text, 256 KiB data, 1,024 fragments, four incomplete IDs per stream/channel, 10-second timeout, bounded recent-ID dedupe.
- Verification: 163 JVM tests and 23 Pixel 7a API 36 instrumentation tests passed; lint 0 errors/119 warnings; debug and unsigned R8 release APKs assembled; `verifyReleasePrivacy` passed.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --console=plain --no-daemon
.\gradlew.bat clean testDebugUnitTest lintDebug assembleDebug assembleRelease verifyReleasePrivacy --console=plain --no-daemon
git diff --check
```

## Forbidden changes

- No protocol header/framing break, application ID/version bump, transport encryption claim, release tag, or physical-device PASS.
- Do not touch `.viepilot/debug/`.
