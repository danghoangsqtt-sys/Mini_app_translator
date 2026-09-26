# Mini Conversation — Technical Privacy Notice

Last technical update: 26 September 2026.

This document describes the data flows implemented by the current Mini Conversation source code. It is not a substitute for legal review. Before public distribution, the publisher must add the current data-controller identity, contact details, applicable legal bases, store disclosures, and any jurisdiction-specific wording.

## Default on-device operation

Mini Conversation does not require a Google Cloud account, billing project, or service-account credential for its default Conversation and Walkie-Talkie modes.

- Microphone audio is supplied to Android's `SpeechRecognizer`. When the device provides an on-device recognizer, recognition can occur on the device. Otherwise Android may use a system recognition service that requires a network connection or processes audio under that service provider's terms. Mini Conversation does not silently fall back to the legacy Google Cloud implementation.
- Final and partial transcripts are passed to Google ML Kit Translation on the device. Translation models are downloaded and stored on the device. ML Kit may contact Google to download or update models, receive SDK fixes, and send performance or utilization metrics as described by Google's terms.
- Android Text-to-Speech speaks translated text. The installed TTS engine is selected and governed by the device/user; an engine may have its own network and privacy behavior.
- In Conversation mode, profile data and translated text are exchanged with the selected peer through the existing Bluetooth transport. The application operator does not run an intermediary conversation server.

## Data stored on the device

- The chosen display name, optional profile image, settings, downloaded translation models, recent-peer information, and limited historical usage data are stored locally.
- Clearing application data or uninstalling the app removes application-owned local data, subject to Android and third-party component behavior.
- Bluetooth conversation payloads use the existing transport. Do not use the current build for highly sensitive conversations until the separate application-layer encryption review is complete.

## Optional legacy Cloud credential

The **Legacy Cloud (advanced)** screen retains credential import/delete only for migration and compatibility work. Importing a service-account JSON file does not change the default runtime and does not enable an automatic Cloud fallback.

The selected credential is validated, encrypted using an Android Keystore-backed key, stored in application-private storage, and excluded from Android Auto Backup. It can be deleted from the same screen. No shared credential is bundled in the APK. Service-account credentials are sensitive and should not be used on a personal or production Cloud project without an independently reviewed security design.

## Permissions and device capabilities

- Microphone permission is used for speech recognition.
- Bluetooth/Nearby Devices permissions are used for peer discovery and communication. On Android versions whose Bluetooth discovery permission model is tied to location, the operating system may request location permission; Mini Conversation does not read or store GPS coordinates.
- Internet access may be used for ML Kit model downloads/updates, a network-backed system speech recognizer, third-party TTS, repository/privacy links, or explicitly invoked legacy tooling.
- Notification/foreground-service permissions are used to keep active voice sessions visible to the user.
- Profile images and optional legacy credential files are selected through Android pickers; the app does not scan shared storage for credentials.

## Third-party components

Relevant providers may include the Android speech-recognition service selected on the device, Google ML Kit, the installed Android TTS engine, Google Play, and the peer device selected by the user. Their own terms and privacy notices apply to processing they perform.

## User controls

Users can deny or revoke permissions in Android Settings, delete downloaded translation models in Mini Conversation settings, delete an optional legacy credential, clear application data, disconnect peers, or uninstall the app. Some features will not operate without the corresponding permission, model, or device capability.

## Release-review requirement

This notice records the implemented technical behavior. A public release remains blocked until the publisher confirms the final controller/contact information and obtains any legal review required for its distribution territory and app store.
