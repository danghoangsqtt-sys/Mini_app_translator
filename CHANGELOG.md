# Changelog

All notable changes to this fork will be documented in this file.

## [1.2.0] - 2026-09-23

### Changed

- Renamed product from Mini Translator to Mini Conversation across all user-facing strings, docs, and notifications (`ENH-018`).
- Replaced launcher icons with a clean circular legacy/round set and an adaptive icon (background/foreground/monochrome layers) derived from `images/icon.png` (`ENH-018`).
- Switched `Theme.Speech` from `Theme.AppCompat.Light.DarkActionBar` to `Theme.MaterialComponents.DayNight.NoActionBar` with semantic color tokens and a `values-night/colors.xml` dark palette (`ENH-019`).
- Refreshed onboarding, pairing, conversation, WalkieTalkie, API management, and settings screens with Material components (MaterialButton/MaterialCheckBox/MaterialCardView), theme-token colors, a clearer pairing empty state, larger WalkieTalkie language labels, and consolidated settings categories (`ENH-019`).

### Fixed

- Added real contentDescription strings for 28 interactive icon controls that were missing one or reusing the generic app-name placeholder (`ENH-019`).
- Enlarged 15 sub-48dp interactive icon controls to a 48x48dp minimum touch target (`ENH-019`).

## [Unreleased]

### Planned — 1.3.0

- Replace key-required Google Cloud onboarding with an on-device-first engine architecture (`ENH-021`).
- Correct the false Bluetooth Low Energy capability gate and make unsupported states recoverable (`BUG-023`).
- Remove obsolete `$300`/one-year Cloud messaging and legacy RTranslator active branding (`BUG-021`, `BUG-022`).
- Keep legacy Cloud configuration optional; do not use undocumented Google Translate web scraping.
- Do not publish or tag the current `1.2.0` candidate; it is a release NO-GO pending Phase 9.

### Fixed

- Made first-run onboarding reach the main UI without a Google Cloud credential and removed obsolete Cloud/RTranslator promotional assets (`BUG-021`, `BUG-022`).
- Reclassified legacy Bluetooth multiple-advertisement failures as a recoverable discovery limitation instead of falsely claiming the device has no BLE (`BUG-023`).

### Changed

- Added runtime-neutral speech-recognition, text-translation, and speech-output contracts with explicit engine selection and isolated legacy adapters; runtime behavior is not switched yet (`ENH-021`).

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
- Synchronize project handoff/closeout records, current-state documentation, and portable JDK 17 build setup (`BUG-017` through `BUG-020`); no application release version change was made by this metadata-only phase.
- Accept the sanitized canonical Phase 7 baseline at `dffa145` / `mini-app-translator-vp-p7-complete` for Phase 8 release readiness; this does not certify device QA or a signed release.
- Establish sanitized `master`/`origin/master` as the single source of truth, removing unrelated `.agents` payloads from history, limiting shared IDE configuration, and persisting mapped Phase 5–8.1 tags.

### Planned

- Complete the remaining `1.2.0` release-readiness gates: capture real-device baselines, refresh dependencies incrementally, finish Bluetooth/SCO and accessibility QA, enable blocking lint/release checks, and verify a signed artifact.
- Defer Wi-Fi Hotspot transport (`ENH-020`) to Phase 10 / `1.4.0`, after Phase 9 no-key migration.

- Security and stability hardening for credentials, storage, audio capture, and cloud streaming.
- Correctness fixes for Room instance ownership, token/service lifecycles, and any reachable unauthenticated crypto or GraphView path (reachability checks come first).
- Android toolchain modernization targeting API 36, including Bluetooth permissions, component exports, and foreground service types.
- Resolve lint errors, make quality checks blocking, and collect real-device plus signed-release evidence before public distribution.
