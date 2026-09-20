# Changelog

All notable changes to this fork will be documented in this file.

## [Unreleased]

### Fixed

- Encrypt the Google Cloud service-account credential at rest with an Android Keystore-backed AES-GCM key, including safe migration from the former plaintext file.
- Exclude encrypted and legacy credential files plus identifying preference metadata from Android Auto Backup.
- Fix `Tools.merge()` so byte arrays are concatenated in order instead of repeatedly overwriting the first output byte.
- Serialize recorder shutdown and make the capture thread release `AudioRecord` only after its blocking read has stopped.
- Propagate gRPC speech-stream failures through the service/UI error path with status mapping and stale-stream isolation.
- Replace the recursive Downloads credential scan with a Storage Access Framework JSON picker, validate service-account content before encrypted persistence, and remove legacy external-storage access.
- Share a single application-context Room database instance between consumption and recent-peer managers, preserving the existing database file and schema.

### Planned

- Security and stability hardening for credentials, storage, audio capture, and cloud streaming.
- Correctness fixes for Room instance ownership, token/service lifecycles, and any reachable unauthenticated crypto or GraphView path (reachability checks come first).
- Android toolchain modernization targeting API 36, including Bluetooth permissions, component exports, and foreground service types.
- Resolve lint errors, make quality checks blocking, and collect real-device plus signed-release evidence before public distribution.
- Product rename to **Mini Conversation** with launcher/adaptive icons sourced from `images/icon.png`.
- Material-based light/dark UI refresh with accessibility and responsive-layout improvements.
