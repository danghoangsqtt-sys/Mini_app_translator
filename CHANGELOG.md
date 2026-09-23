# Changelog

All notable changes to this fork will be documented in this file.

## [Unreleased]

### Changed

- Renamed product from Mini Translator to Mini Conversation across all user-facing strings, docs, and notifications (`ENH-018`).
- Replaced launcher icons with a clean circular legacy/round set and an adaptive icon (background/foreground/monochrome layers) derived from `images/icon.png` (`ENH-018`).
- Switched `Theme.Speech` from `Theme.AppCompat.Light.DarkActionBar` to `Theme.MaterialComponents.DayNight.NoActionBar` with semantic color tokens and a `values-night/colors.xml` dark palette (`ENH-019`).
- Refreshed onboarding, pairing, conversation, WalkieTalkie, API management, and settings screens with Material components (MaterialButton/MaterialCheckBox/MaterialCardView), theme-token colors, a clearer pairing empty state, larger WalkieTalkie language labels, and consolidated settings categories (`ENH-019`).

### Fixed

- Encrypt the Google Cloud service-account credential at rest with an Android Keystore-backed AES-GCM key, including safe migration from the former plaintext file.
- Exclude encrypted and legacy credential files plus identifying preference metadata from Android Auto Backup.
- Fix `Tools.merge()` so byte arrays are concatenated in order instead of repeatedly overwriting the first output byte.
- Serialize recorder shutdown and make the capture thread release `AudioRecord` only after its blocking read has stopped.
- Propagate gRPC speech-stream failures through the service/UI error path with status mapping and stale-stream isolation.
- Replace the recursive Downloads credential scan with a Storage Access Framework JSON picker, validate service-account content before encrypted persistence, and remove legacy external-storage access.
- Share a single application-context Room database instance between consumption and recent-peer managers, preserving the existing database file and schema.
- Remove unreachable, unauthenticated AES/CTR helpers so no application path can use malleable ciphertext.
- Prevent a queued access-token callback from delivering a token for a replaced or deleted credential after reset.
- Track each WalkieTalkie recognizer bind attempt so teardown releases the matching registrations
  without unconditional unbinds, including false-return and partial-bind cases.
- Avoid duplicate active WalkieTalkie recognizer registrations, validated against API 36 binding
  lifecycle behavior.
- Prevent stale Conversation SCO reconnect callbacks from restarting Bluetooth after service
  teardown or from an old service instance.
- Add real contentDescription strings for 28 interactive icon controls that were missing one or reusing the generic app-name placeholder.
- Enlarge 15 sub-48dp interactive icon controls to a 48x48dp minimum touch target.

### Planned

- Security and stability hardening for credentials, storage, audio capture, and cloud streaming.
- Correctness fixes for Room instance ownership, token/service lifecycles, and any reachable unauthenticated crypto or GraphView path (reachability checks come first).
- Android toolchain modernization targeting API 36, including Bluetooth permissions, component exports, and foreground service types.
- Resolve lint errors, make quality checks blocking, and collect real-device plus signed-release evidence before public distribution.
- Product rename to **Mini Conversation** with launcher/adaptive icons sourced from `images/icon.png`.
- Material-based light/dark UI refresh with accessibility and responsive-layout improvements.
