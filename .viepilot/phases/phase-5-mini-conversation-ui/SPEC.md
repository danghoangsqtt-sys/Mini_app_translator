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

- Change user-visible `RTranslator` and `Mini App Translator`/`Mini Translator` branding to `Mini Conversation` in strings, notifications, onboarding, README, privacy documents, and screenshots.
- `values/strings.xml`: rename `app_name`, `toast_working_background`, `error_internet_lack_loading`, `description_discovery`, `description_conversation`, `description_walkie_talkie`, `description_notice`.
- `values-it/strings.xml`: sync the same renames for any Italian string containing "Mini Translator"/"RTranslator".
- Preserve `applicationId` (`nie.translator.rtranslatordevedition`) and Java package names for upgrade compatibility. Record any future package rename as a separate migration.
- Replace the two missing README images with new, privacy-safe screenshots; update README's project-status table to the released version/versionCode once 5.6 lands.
- Keep upstream RTranslator credit and license notices, including the "Nguồn gốc và giấy phép" section.
- Correct README's now-stale statements about only boilerplate tests and plaintext credential storage; distinguish the implemented Keystore protection from the still-open public-distribution authentication decision.
- `privacy/Privacy_Policy_en.md` and `privacy/Privacy_Policy_it.md`: replace `RTranslator`/`Mini Translator` with `Mini Conversation`.
- `CHANGELOG.md`: keep the rename entry in `[Unreleased]` until release, per `SYSTEM-RULES.md` (no version bump during planning or mid-implementation); move it into a dated `## [1.2.0]` section only at the actual release commit alongside the `app/build.gradle` version bump in task 5.6.

### 5.2 — Launcher/adaptive icon set

- Use `images/icon.png` as the master bitmap; keep `icon.ico` out of Android resources.
- Generate density-correct legacy assets from `images/icon.png`: `mipmap-mdpi` (48px), `-hdpi` (72px), `-xhdpi` (96px), `-xxhdpi` (144px), `-xxxhdpi` (192px), for both `ic_launcher.png` and a circle-cropped `ic_launcher_round.png`.
- `mipmap-anydpi-v26/ic_launcher.xml` (adaptive icon, API 26+):
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
      <background android:drawable="@drawable/ic_launcher_background"/>
      <foreground android:drawable="@drawable/ic_launcher_foreground"/>
      <monochrome android:drawable="@drawable/ic_launcher_monochrome"/>
  </adaptive-icon>
  ```
- `drawable/ic_launcher_background.xml`: linear/radial gradient `#0277BD` → `#00BCD4`.
- `drawable/ic_launcher_foreground.png`: the two chat-bubble marks (A/文) on a transparent ground, centered inside the 66×66 safe zone of a 108×108 canvas.
- `drawable/ic_launcher_monochrome.xml`: white silhouette of the two chat bubbles, for themed icons.
- Set `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"` on `<application>` in `AndroidManifest.xml`.
- Verify circle, squircle, rounded-square, themed icon, Settings, recents, and Android 12+ splash masks.

### 5.3 — Material theme and design tokens

- Move from the current light-only `Theme.AppCompat.Light.DarkActionBar` setup to `Theme.MaterialComponents.DayNight.NoActionBar`.
- `app/build.gradle`: confirm/raise `com.google.android.material:material` to at least `1.9.0`.
- `values/colors.xml`: keep all existing colors; add semantic roles — `colorPrimary #0277BD`, `colorPrimaryVariant #01579B`, `colorOnPrimary #FFFFFF`, `colorSecondary #00BCD4`, `colorSecondaryVariant #006064`, `colorOnSecondary #FFFFFF`, `colorSurface #FAFAFA`, `colorOnSurface #212121`, `colorError #D50000`, `colorOnError #FFFFFF`, `colorBackground #FFFFFF`, `colorOnBackground #212121`.
- New `values-night/colors.xml` with the dark counterparts — `colorPrimary #4FC3F7`, `colorPrimaryVariant #0288D1`, `colorOnPrimary #003258`, `colorSecondary #4DD0E1`, `colorSecondaryVariant #0097A7`, `colorOnSecondary #00363D`, `colorSurface #1E1E1E`, `colorOnSurface #E0E0E0`, `colorError #FF5252`, `colorOnError #690000`, `colorBackground #121212`, `colorOnBackground #E0E0E0` — plus dark overrides of the existing `primary`/`primary_dark`/`accent`/`white`/`black`/`very_very_light_gray`/`very_light_gray`/`light_gray` tokens so legacy layout references stay correct without individually converting every literal.
- `values/styles.xml`: rewire `Theme.Speech` to the semantic attrs above (`colorPrimary`, `colorPrimaryVariant`, `colorOnPrimary`, `colorSecondary`, `colorSecondaryVariant`, `colorOnSecondary`, `colorSurface`, `colorOnSurface`, `colorError`, `colorOnError`, `android:colorBackground`, `colorOnBackground`), `android:statusBarColor = colorPrimaryVariant`, `android:navigationBarColor = colorSurface`, `preferenceTheme = @style/PreferenceThemeOverlay.v14.Material`; update `Theme.Toolbar`/`Theme.TabLayout` parents to MaterialComponents equivalents.
- Remove hardcoded white/gray/green surfaces from core layouts where theme roles are appropriate (`?attr/colorSurface`, `?attr/colorOnSurface`, `?attr/colorBackground`, `?android:attr/textColorPrimary`), including the general sweep across all `layout/` files, not only the six screens named in 5.4.

### 5.4 — Refresh core screens

Retain existing view IDs in every screen below to reduce Java regression risk.

- **Onboarding** (`fragment_notice.xml`, `fragment_user_data.xml`): concise value statement, progressive permission explanation, clear privacy/age actions; buttons as `MaterialButton`, checkboxes as `MaterialCheckBox`, 16dp side padding, `?attr/colorPrimary` instead of hardcoded color.
- **Pairing** (`fragment_pairing.xml`): visible scan/discover state, device rows as `MaterialCardView` (2dp elevation), clear empty state (icon + "searching for devices" text instead of a blank list), clear error/retry states; search/cancel controls at least 48×48dp.
- **Conversation** (`fragment_conversation.xml`): strong source→target language header, message bubbles with 16dp corner radius (`?attr/colorPrimaryContainer` for sent, `?attr/colorSurface` for received), persistent 56dp FAB microphone action in `?attr/colorSecondary`, clear disconnect state.
- **WalkieTalkie** (`fragment_walkie_talkie.xml`): two distinct speaker/language regions, obvious listening/processing/result states with a light pulse animation while listening, larger translation-result text for readability.
- **API/settings** (`fragment_credit.xml`, `activity_credit.xml`, `activity_settings.xml`): separate credential setup, usage stats, pricing/about sections as `MaterialCardView` groups (keep the existing `GraphView` as-is); settings grouped into `PreferenceCategory` headers (Language, Audio, Privacy, About).

### 5.5 — Accessibility and responsive layout

- Fix all 14 currently detected interactive icon controls (`ImageView`/`ImageButton` without `android:contentDescription`) across `layout/`, sourced from a string resource rather than hardcoded text.
- Ensure at least 48×48dp focus/touch targets (`android:minWidth`/`android:minHeight`) on every interactive element below that size, including the 24dp pairing search/cancel control.
- Validate contrast (text on a `colorPrimary`/primary-variant background uses `colorOnPrimary`; text on `colorSurface` uses `colorOnSurface`), TalkBack order/roles, error announcements, keyboard focus, RTL behavior, compact screens, landscape, and 200% font scale.

### 5.6 — Version bump, QA, and release evidence

- `app/build.gradle`: bump `versionCode` to `15` and `versionName` to `'1.2.0'` as the last step, once 5.1–5.5 are complete and verified — not during planning.
- After each of 5.1–5.5, run `./gradlew testDebugUnitTest assembleDebug` to confirm no build regression before moving on.
- Final verification: `./gradlew clean testDebugUnitTest lintDebug assembleDebug` — require 0 lint errors, `BUILD SUCCESSFUL`, and all unit tests passing.
- Build screenshot baselines for onboarding, pairing, Conversation, WalkieTalkie, API management, and settings in light/dark modes.
- Run accessibility checks plus TalkBack smoke tests.
- Device matrix: API 23, 31, 34, and 36; at least one compact and one large screen.
- Run `connectedDebugAndroidTest`; manually verify Bluetooth, microphone, translation, TTS, background notification, and process recreation.
- Run `assembleRelease`, inspect the release artifact, and verify a signed installable artifact without committing a keystore or secret. Record permission grant/deny/revoke and credential import failures; use a physical two-phone Bluetooth pair.
- Treat any missing device result, unresolved High/Critical issue, or unapproved service-account-on-device risk decision as a release blocker, not as a passed test.
- Move the `CHANGELOG.md` rename/UI entries from `[Unreleased]` into a dated `## [1.2.0]` section at the release commit.

Detailed execution contracts for audit-driven documentation and release QA work are in `tasks/5.1-identity-docs.md` and `tasks/5.6-device-release-qa.md`.

## Acceptance

- Installed app and all first-party documents say Mini Conversation.
- Icon renders without clipping or unintended transparent/black edges on supported masks.
- Core tasks remain behaviorally equivalent and pass the device matrix.
- No production credential or personal data appears in screenshots, tests, or logs.
