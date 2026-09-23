# Phase 5 — Mini Conversation Rebrand & UI (SPEC)

## Goal

Ship a coherent product identity named **Mini Conversation**, use the supplied icon correctly on Android launchers, and modernize the existing Views UI without changing conversation behavior.

## Design direction

- Derive the primary palette from the supplied icon: deep blue for primary actions, cyan for active/listening states, mint/teal for translated output, and neutral surfaces for long conversations.
- Keep the conversation itself visually dominant. Connection state, source/target language, microphone state, and recovery actions must be understandable at a glance.
- Use Material Components/Material 3 for Views after Phase 3. A Compose rewrite is outside this phase because it would combine architecture migration with a visual redesign.
- Support light/dark themes and 200% font scale. Use semantic theme attributes instead of screen-level color literals.

## Paths

```text
images/icon.png
images/icon.ico
app/src/main/AndroidManifest.xml
app/src/main/res/mipmap-*/
app/src/main/res/mipmap-anydpi-v26/
app/src/main/res/drawable/
app/src/main/res/values/colors.xml
app/src/main/res/values/styles.xml
app/src/main/res/values/strings.xml
app/src/main/res/values-it/strings.xml
app/src/main/res/values-night/
app/src/main/res/layout/
privacy/
README.md
```

## Tasks

### 5.1 — Product identity and documentation

- Change user-visible `RTranslator` and `Mini App Translator` branding to `Mini Conversation` in strings, notifications, onboarding, README, privacy documents, and screenshots.
- Preserve `applicationId` and Java package names for upgrade compatibility. Record any future package rename as a separate migration.
- Replace the two missing README images with new, privacy-safe screenshots.
- Keep upstream RTranslator credit and license notices.
- Correct README's now-stale statements about only boilerplate tests and plaintext credential storage; distinguish the implemented Keystore protection from the still-open public-distribution authentication decision.

### 5.2 — Launcher/adaptive icon set

- Use `images/icon.png` as the master bitmap; keep `icon.ico` out of Android resources.
- Generate density-correct legacy `ic_launcher`/`ic_launcher_round` assets.
- Create adaptive background and foreground layers for API 26+, centered inside the 66×66 safe zone of a 108×108 canvas.
- Add `android:roundIcon`; add a dedicated monochrome layer if a clean one-color mark can be derived without losing meaning.
- Verify circle, squircle, rounded-square, themed icon, Settings, recents, and Android 12+ splash masks.

### 5.3 — Material theme and design tokens

- Move from the current light-only `Theme.AppCompat.Light.DarkActionBar` setup to a supported Material DayNight theme.
- Define semantic colors (`primary`, `onPrimary`, `surface`, `onSurface`, `error`, containers), typography, shapes, elevation, and spacing.
- Remove hardcoded white/gray/green surfaces from core layouts where theme roles are appropriate.

### 5.4 — Refresh core screens

- Onboarding: concise value statement, progressive permission explanation, clear privacy/age actions.
- Pairing: visible scan/discover state, device cards, clear empty/error/retry states.
- Conversation: strong language/status header, readable message bubbles, persistent microphone/input action, clear disconnect state.
- WalkieTalkie: distinct speaker/language regions, obvious listening/processing/result states.
- API/settings: separate credential setup, usage, audio, language, privacy, and about sections.
- Retain existing view IDs where possible to reduce Java regression risk.

### 5.5 — Accessibility and responsive layout

- Fix all 14 currently detected interactive icon controls without `contentDescription`.
- Ensure at least 48×48dp focus/touch targets, including the 24dp pairing search/cancel control.
- Validate contrast, TalkBack order/roles, error announcements, keyboard focus, RTL behavior, compact screens, landscape, and 200% font scale.

### 5.6 — QA and release evidence

- Build screenshot baselines for onboarding, pairing, Conversation, WalkieTalkie, API management, and settings in light/dark modes.
- Run accessibility checks plus TalkBack smoke tests.
- Device matrix: API 23, 31, 34, and 36; at least one compact and one large screen.
- Run `testDebugUnitTest`, `connectedDebugAndroidTest`, `lintDebug`, and `assembleDebug`; manually verify Bluetooth, microphone, translation, TTS, background notification, and process recreation.
- Run `assembleRelease`, inspect the release artifact, and verify a signed installable artifact without committing a keystore or secret. Record permission grant/deny/revoke and credential import failures; use a physical two-phone Bluetooth pair.
- Treat any missing device result, unresolved High/Critical issue, or unapproved service-account-on-device risk decision as a release blocker, not as a passed test.

Detailed execution contracts for audit-driven documentation and release QA work are in `tasks/5.1-identity-docs.md` and `tasks/5.6-device-release-qa.md`.

## Acceptance

- Installed app and all first-party documents say Mini Conversation.
- Icon renders without clipping or unintended transparent/black edges on supported masks.
- Core tasks remain behaviorally equivalent and pass the device matrix.
- No production credential or personal data appears in screenshots, tests, or logs.
