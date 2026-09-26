# Architecture — Mini Conversation

> Current app version: `1.2.0` / versionCode 15. Phase 9 targets `1.3.0`; no release is approved yet. The Android `applicationId` and Java package remain unchanged for upgrade compatibility and upstream lineage.

## System overview — on-device default

Mini Conversation is a single-module native Android/Java application. The default runtime is:

```text
Microphone
  → Android SpeechRecognizer (on-device preferred; system recognizer disclosed)
  → mode controller / generation gate
  → ML Kit Translation (downloaded model)
  → translated text
      ├─ Android TextToSpeech
      └─ Bluetooth peer in Conversation mode
```

UI and foreground services compose `SpeechRecognitionEngine`, `TextTranslationEngine`, and `SpeechOutputEngine` implementations through explicit factories. One bounded recognition turn owns one engine session; generation checks suppress stale/late callbacks, and engine resources are closed exactly once.

## Modes

- **Conversation**: each phone recognizes its local speaker, translates outgoing/incoming text on device, and exchanges framed translated text over `ConversationBluetoothCommunicator`. Incoming translation has a FIFO lane isolated from microphone recognition.
- **Walkie-Talkie**: one phone uses an explicit persisted source direction and one recognizer. Each tap starts one bounded turn; changing direction or language invalidates the active turn.

## Engine boundaries

| Contract | Default implementation | Important behavior |
|---|---|---|
| `SpeechRecognitionEngine` | `AndroidSpeechRecognitionEngine` | API 31+ on-device preference; guarded system fallback before listening; main-thread framework lifecycle; 30-second watchdog |
| `TextTranslationEngine` | `MlKitTextTranslationEngine` | Explicit model management; English built in; downloaded models support offline use; no hidden Cloud fallback |
| `SpeechOutputEngine` | `AndroidSpeechOutputEngine` | Wraps the single service-owned `TTS`; shared output ownership remains with the service |

The Android system recognizer can use network services and may ignore `EXTRA_PREFER_OFFLINE`. ML Kit may use the network to download/update models and for SDK operational behavior, but translation content is processed using the device model.

## Legacy Cloud boundary

Legacy gRPC/REST, token, and credential code is retained for compatibility and migration analysis, not selected by the default modes. The **Legacy Cloud (advanced)** screen can validate, encrypt, store, and delete a user-supplied credential. Importing one does not activate a Cloud engine.

A functional Cloud speech selector is deliberately outside Phase 9: `LegacyCloudSpeechRecognitionEngine` accepts PCM streaming while current mode controllers use engine-owned microphone capture. Reintroducing Cloud requires a separate design rather than a factory-name switch. No `translate.google.com` scraping or bundled shared credential is allowed.

## Package layout

| Package | Responsibility |
|---|---|
| `access/` | Keyless first-run profile/privacy onboarding |
| `api_management/` | Optional legacy credential import/delete, OAuth/token internals, historical usage UI |
| `database/` | Shared Room database for recent peers and legacy consumption records |
| `settings/` | Language, audio, TTS, and ML Kit model-management settings |
| `tools/` | Shared Android, crypto, message, Bluetooth, and UI helpers |
| `voice_translation/engines/` | Runtime-neutral contracts plus on-device and isolated legacy adapters |
| `voice_translation/_conversation_mode/` | Bluetooth Conversation UI/service/controller/codec |
| `voice_translation/_walkie_talkie_mode/` | Single-device Walkie UI/service/controller |
| `voice_translation/cloud_apis/` | Retained legacy Cloud implementations; not the default runtime |

## Data and privacy boundaries

- Profile name/photo, settings, models, recent peers, and credential ciphertext remain in app/device storage.
- Optional credentials use an Android Keystore-backed AES-GCM key and are excluded from Auto Backup.
- Conversation profile/text payloads traverse the existing Bluetooth library; application-layer encryption remains tracked separately by `ENH-001`.
- There is no Mini Conversation-operated backend. Third-party behavior can still come from the selected Android speech/TTS services, Google ML Kit model infrastructure, Google Play, and the peer device.

## Lifecycle and restart policy

Voice services require launch extras that cannot be reconstructed after process death. They therefore reject a null restart intent, stop the restarted instance, and return `START_NOT_STICKY`. Normal starts capture their notification/languages before entering the bounded on-device controller lifecycle.

## UI architecture

The existing Java + XML Views architecture uses Material DayNight resources, stable view IDs, localized EN/IT strings, and minimum touch-target/content-description improvements. Package/application ID migration is not part of the UI rebrand.

## Diagram applicability matrix

| Diagram type | Status | Rationale |
|---|---|---|
| `system-overview` | required | Cross-device and on-device boundaries are core; see `.viepilot/architecture/system-overview.mermaid` |
| `data-flow` | optional | Covered by the system diagram and sections above |
| `event-flows` | N/A | No event bus; callbacks and Bluetooth messages are direct |
| `module-dependencies` | optional | Single Gradle module; package table is sufficient |
| `deployment` | N/A | Native APK with no app-operated server |
| `user-use-case` | optional | Two bounded modes are documented above and in README |

## Technology and validation boundary

- Android compile/target 36, minSdk 23, AGP 8.13.2, Gradle 8.13, JDK 17 runtime, Java 8 source compatibility.
- Room 2.1.0, legacy gRPC/auth dependencies, and their lint/security warnings remain under `ENH-012`.
- Automated JVM/instrumentation/lint gates cover recent engine work, but physical API/device, two-phone Bluetooth/SCO, TalkBack, legal-controller, signing, and release-install evidence remain mandatory before release.
