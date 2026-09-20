# Changelog

All notable changes to this fork will be documented in this file.

## [Unreleased]

### Fixed

- Encrypt the Google Cloud service-account credential at rest with an Android Keystore-backed AES-GCM key, including safe migration from the former plaintext file.
- Exclude encrypted and legacy credential files plus identifying preference metadata from Android Auto Backup.
- Fix `Tools.merge()` so byte arrays are concatenated in order instead of repeatedly overwriting the first output byte.
- Serialize recorder shutdown and make the capture thread release `AudioRecord` only after its blocking read has stopped.
- Propagate gRPC speech-stream failures through the service/UI error path with status mapping and stale-stream isolation.

### Planned

- Security and stability hardening for credentials, storage, audio capture, and cloud streaming.
- Android toolchain modernization targeting API 36, including Bluetooth permissions, component exports, and foreground service types.
- Product rename to **Mini Conversation** with launcher/adaptive icons sourced from `images/icon.png`.
- Material-based light/dark UI refresh with accessibility and responsive-layout improvements.
