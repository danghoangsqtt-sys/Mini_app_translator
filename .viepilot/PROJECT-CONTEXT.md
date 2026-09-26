<!-- crystallize_version: 0.8.0 -->

# Project Context — Mini Conversation

## Product scope

Mini Conversation is a native Android/Java speech-translation app with two modes:

- **Conversation** exchanges translated text between paired phones over Bluetooth.
- **Walkie-Talkie** lets two people share one phone and take bounded turns in two selected languages.

The on-device default is Android `SpeechRecognizer` → Google ML Kit Translation → Android Text-to-Speech. A normal user does not need a Google Cloud project, billing account, API key, or service-account JSON.

## Current delivery state

- Phase 9 is active and targets `1.3.0`; the `1.2.0` candidate is a release NO-GO.
- Tasks 9.1–9.5 implemented keyless onboarding, engine contracts, ML Kit model management/offline translation, lifecycle-safe speech recognition, and default Conversation/Walkie integration.
- Task 9.6 removes remaining Cloud-first UX/documentation and fixes audit blockers.
- Task 9.7 owns API 23/31/34/36, physical two-phone Bluetooth/SCO, accessibility, signing, and release-candidate evidence.
- Phase 3 dependency modernization and Phase 5 physical UI gates remain visible; emulator evidence does not close them.

## Domain and data flow

- Speech recognition capability is device-dependent. API 31+ on-device recognition is preferred when available; the Android system recognizer may require network access and may ignore an offline preference.
- ML Kit language models are explicitly downloaded/deleted in Settings and can translate offline after download.
- Android TTS output depends on the engine installed by the user/device.
- Conversation sends profile data and translated text to the selected peer over the existing Bluetooth transport. There is no app-operated server.
- Local state includes preferences, optional profile image, downloaded models, recent peers, and legacy usage history in Room.

## Credential model

Credential-free operation is the default. Existing service-account import/delete remains only as **Legacy Cloud (advanced / optional)** migration tooling:

- importing a credential does not select a Cloud runtime or enable fallback;
- credentials are validated, encrypted with an Android Keystore-backed key, stored privately, and excluded from Auto Backup;
- no shared credential is bundled in the APK;
- a functional Cloud mode would require a separately approved architecture because the retained legacy speech adapter consumes PCM while current controllers own microphone capture.

## Constraints and anti-goals

- Preserve minSdk 23, package/application ID, Bluetooth payload compatibility, and upstream Apache-2.0 attribution.
- Do not scrape `translate.google.com`, copy AGPL code from `vp-pdf`, add hidden Cloud fallback, or fabricate speech-language support from the ML Kit catalog.
- Do not close physical-device, accessibility, legal/privacy-owner, or signed-release gates using emulator/static evidence.
- Wi-Fi Hotspot transport is Phase 10 and must not be mixed into Phase 9.

## Known risk register

Canonical issue state is in `.viepilot/TRACKER.md` and `.viepilot/requests/`. Current release-sensitive items include dependency debt (`ENH-012`), Bluetooth application-layer confidentiality (`ENH-001`), final privacy-controller/legal review, and physical-device QA.
