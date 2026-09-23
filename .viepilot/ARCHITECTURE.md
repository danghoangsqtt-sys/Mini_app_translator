# Architecture — Mini Conversation

> Current runtime identity: **Mini Conversation** (Phase 5 code complete at version 1.2.0/versionCode 15). The Android `applicationId` and Java package remain unchanged to preserve upgrade compatibility and upstream lineage.

## System overview

RTranslator is a native Android app (Java) that provides real-time speech translation between two phones over Bluetooth, using Google Cloud Speech-to-Text for recognition and Google Cloud Translation for translation, then Android TTS for output. It has two modes:

- **Conversation mode** — persistent Bluetooth pairing between two phones running the app; bidirectional streaming translation via a foreground `ConversationService`.
- **WalkieTalkie mode** — single-phone, turn-based, two-language listening via `WalkieTalkieService` + two recognizer services (one per language).

Credentials are user-supplied: the user provides their own Google Cloud service-account JSON key (loaded via `api_management/KeyFileContainer`), scoped for Speech-to-Text + Translation billing on their own GCP account.

> **Migration state (Phase 9)**: this describes the legacy implementation, not the target default. The approved target is on-device-first: Android SpeechRecognizer → ML Kit Translation → Android TextToSpeech. User-supplied Cloud credentials become optional legacy/advanced configuration; no shared credential may be bundled in the APK. Undocumented `translate.google.com/m` scraping is forbidden.

## Target engine boundaries (Phase 9)

- UI/services depend on `SpeechRecognitionEngine`, `TextTranslationEngine`, and `SpeechOutputEngine` contracts rather than constructing Cloud clients directly.
- Capability selection is explicit and recoverable: missing on-device recognizer, missing ML Kit model, permission denial, and network fallback are distinct states.
- API 31 on-device recognizer calls are guarded while API 23 remains supported.
- ML Kit model download/removal is user-visible; downloaded models support offline translation.
- Legacy Cloud adapters remain opt-in only during migration. A managed backend is a separate future decision.

## ViePilot organization context

None — no `~/.viepilot/profiles/` binding configured for this project.

## Package layout (module boundaries, inferred from directory structure — not enforced by build modules; this is a single Gradle module)

| Package | Responsibility |
|---|---|
| `access/` | First-run onboarding (name, photo, privacy consent) |
| `api_management/` | Google Cloud key selection, storage, OAuth2 token refresh, usage/quota UI |
| `database/` | Room persistence (recent peers, consumption/credit tracking) |
| `settings/` | App settings UI |
| `tools/` | Shared utilities: file I/O, crypto helpers, logging, a vendored GraphView charting library |
| `voice_translation/` | Core feature: Bluetooth conversation mode, walkie-talkie mode, gRPC-based speech recognizer services, cloud API wrappers |

## Data flow (conversation mode, happy path)

1. User speaks → `AudioRecord` (in `voice_translation/cloud_apis/voice/Recorder.java`) captures PCM audio.
2. Audio streamed via gRPC to Google Cloud Speech-to-Text (`Recognizer.java`) → partial/final transcript.
3. Transcript sent to Google Cloud Translation API.
4. Translated text sent to the peer phone over Bluetooth (`ConversationBluetoothCommunicator`, backed by the external `BluetoothCommunicator` library).
5. Peer phone converts translated text to speech via Android TTS and plays it.

## UI architecture (Phase 5 implementation)

Phase 5 kept the existing Java + XML Views architecture and business/service boundaries. It introduced a shared Material DayNight theme layer and refreshed layouts in place, preserving view IDs where practical so the rebrand did not become a behavior rewrite. Device, accessibility, signed-release, and two-phone Bluetooth evidence remain pending human validation.

| Layer | Implemented responsibility |
|---|---|
| Product identity | `app_name`, user-visible copy, README/privacy branding, notification labels, upstream attribution |
| Launcher identity | Legacy density icons plus adaptive foreground/background, round icon, and optional monochrome resource sourced from `images/icon.png` |
| Design tokens | Semantic light/dark colors, typography, shapes, elevation, and spacing in resource values |
| Screen layouts | Onboarding, pairing, Conversation, WalkieTalkie, API management, and settings XML layouts |
| Accessibility | Localized labels, 48dp touch/focus targets, TalkBack order/roles, font scaling, contrast, RTL checks |

The product-name change does not rename `applicationId` or the Java package. Package migration would affect upgrades, providers, persisted data, and distribution identity and therefore requires its own decision and migration plan.

## Diagram applicability matrix

| Diagram type | Status | Rationale |
|---|---|---|
| `system-overview` | required | Core value of the app is the cross-device data flow — see `.viepilot/architecture/system-overview.mermaid` |
| `data-flow` | optional | Covered inline above (Data flow section); a dedicated diagram would duplicate it for a single linear pipeline |
| `event-flows` | N/A | No event bus / pub-sub system in this app — Bluetooth messages and gRPC calls are direct point-to-point, not an event architecture |
| `module-dependencies` | optional | Single Gradle module; package layout table above already captures this at sufficient granularity for a ~24k LOC app |
| `deployment` | N/A | Native mobile app distributed as an APK via GitHub Releases — no server-side deployment topology to diagram |
| `user-use-case` | optional | Two modes (Conversation / WalkieTalkie) are already described in narrative form in README.md; no additional actors/use-cases beyond the two documented modes |

## Technology decisions (as found — not proposed by ViePilot)

- **Android SDK**: compileSdk 36 / targetSdk 36 / minSdk 23, AGP 8.13.2, Gradle 8.13, with JDK 17 used to run Gradle; Java source/target compatibility remains 8. See `.viepilot/requests/ENH-011.md` for the remaining device-validation gate.
- **Persistence**: Room 2.1.0 over SQLite, no migrations defined (`version = 1`).
- **Transport**: Bluetooth via third-party `com.github.niedev:BluetoothCommunicator:1.0.6` (not audited here — see `.viepilot/requests/ENH-001.md`).
- **Cloud APIs**: gRPC 1.11.0 + Protobuf for Speech-to-Text (v1/v1beta1/v1p1beta1 protos vendored under `app/src/main/proto/google/`); REST/OAuth2 for Translation and credential refresh.

## Known architectural risk areas (from audit, tracked individually)

See `.viepilot/requests/` — highest-risk areas are credential storage (`api_management/`) and the Bluetooth/gRPC streaming lifecycle (`voice_translation/`).

## Current validation boundary

Phase 3 Cluster A (toolchain, Bluetooth permissions, exported components, and foreground-service types) is implemented and static-pass verified, but its two-phone Bluetooth/SCO device evidence remains pending. Phase 5 code is implemented at app 1.2.0/versionCode 15, while its device matrix, TalkBack, screenshot-baseline, signing, and release QA remain pending. These are release blockers, not evidence of a shipped signed release.
