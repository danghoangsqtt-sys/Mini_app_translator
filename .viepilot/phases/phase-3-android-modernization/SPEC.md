# Phase 3 — Android Modernization (SPEC)

## Scope

Evolved via `/vp-evolve BUG-013 ENH-012` (2026-09-19), off the back of a `/vp-audit` Tier 3 re-pass that cross-checked an external review of the Android stack against the actual codebase. `ENH-011` was pulled into this same phase on the user's explicit choice, because `BUG-013` has no meaning without it: the new Bluetooth runtime permissions only become mandatory once `targetSdkVersion` crosses 31, which is exactly what `ENH-011` does.

This phase is **one dedicated, highest-blast-radius mechanical upgrade** — sequenced after Phases 1 and 2 so security/correctness fixes don't get entangled with a large toolchain bump. The 2026-09-20 audit expanded it for the current Google Play target API 36 requirement and the Android behavior changes that this target activates.

## Tasks

| Task | Request | Priority | Depends on |
|---|---|---|---|
| 3.1 | Upgrade AGP / Gradle wrapper / `compileSdkVersion` / `targetSdkVersion` to a supported API 36 toolchain | `ENH-011` | Phases 1–2 complete |
| 3.2 | Add Android 12+ Bluetooth runtime permissions (`BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`/`BLUETOOTH_ADVERTISE`) — manifest + runtime request array, SDK-gated | `BUG-013` | 3.1 (must land in the **same PR**, not after) |
| 3.3 | Refresh Room (`2.1.0` → current stable), `play-services-nearby` (`17.0.0` → current stable), gRPC, `google-auth-library-oauth2-http`, `nimbus-jose-jwt` | `ENH-012` | 3.1 (newer AGP/Gradle may require newer dep versions anyway) |
| 3.4 | Add explicit `android:exported` declarations and review merged component exposure | `BUG-014` | 3.1 (same PR) |
| 3.5 | Add microphone/connected-device foreground service types, permissions, and runtime ordering | `BUG-015` | 3.1 + 3.2 (same PR) |

## Task 3.1 — Toolchain / targetSdk upgrade (`ENH-011`)

**Current state**: AGP 3.6.1, Gradle wrapper 5.6.4, `compileSdkVersion 29`, `buildToolsVersion 28.0.3`, `targetSdkVersion 29` (`app/build.gradle:28,32`, root `build.gradle:12`).

**Acceptance criteria**:
- [ ] App builds cleanly on a current Android Studio / AGP / Gradle version
- [ ] `compileSdk` and `targetSdk` are API 36 and the chosen AGP/Gradle/JDK combination is documented
- [ ] Existing functionality regression-tested after the upgrade — this is the highest blast-radius change in the whole backlog; do not fold in unrelated changes

## Task 3.2 — Bluetooth runtime permissions (`BUG-013`)

**Why it's here and not standalone**: with `targetSdkVersion` still at 29, the app runs under Android's legacy Bluetooth permission model even on API 31+ devices — it does not crash today. The moment task 3.1 lands, the OS stops honoring the legacy permissions for Bluetooth scan/connect, and every `BluetoothCommunicator`/`BluetoothAdapter` call in Conversation mode throws `SecurityException` unless this task ships in the same change.

**Acceptance criteria**:
- [ ] `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT` (and `BLUETOOTH_ADVERTISE` if the app becomes discoverable) declared in the manifest with correct `usesPermissionFlags`
- [ ] Runtime permission-request array(s) (`VoiceTranslationActivity.java:81-83` and any other call site) updated for API 31+, gated by `Build.VERSION.SDK_INT`
- [ ] Verified on an Android 12+ device/emulator with the task-3.1 targetSdk in place — Conversation and WalkieTalkie mode both connect without a `SecurityException`
- [ ] Landed in the **same PR** as task 3.1, per `BUG-013`'s acceptance criteria

## Task 3.3 — Dependency refresh (`ENH-012`)

**Current state**: gRPC 1.11.0, `google-auth-library-oauth2-http` 0.8.0, `nimbus-jose-jwt` 5.1, JUnit 4.12, Espresso 3.2.0, Room 2.1.0 (`app/build.gradle:130-138`), `play-services-nearby:17.0.0` (`app/build.gradle:104`).

**Acceptance criteria**:
- [ ] Each dependency's current stable version reviewed for breaking changes and (where findable) security advisories — confirm exact current versions from Maven Central at implementation time, don't trust a pre-stated number
- [ ] Dependencies bumped incrementally with build/test verification at each step
- [ ] Room bump coordinated with `BUG-007`'s fix (shared `AppDatabase` singleton) since both touch the same file
- [ ] `play-services-nearby` bump checked for behavioral changes affecting P2P discovery on Android 13/14 devices

## Task 3.4 — Exported component declarations (`BUG-014`)

- Set `android:exported="true"` on the launcher activity and explicitly keep internal app components non-exported.
- Inspect the merged manifest after dependency upgrades for any third-party component exposure.
- Verify installation and launcher behavior on Android 12+.

## Task 3.5 — Foreground service types (`BUG-015`)

- Map each declared service to its real work; use the minimum necessary `microphone` and/or `connectedDevice` types.
- Add the matching type-specific normal permissions for API 34+.
- Ensure microphone and Bluetooth runtime permissions are granted before foreground promotion.
- Verify background/foreground transitions, screen lock, process recreation, and stop/restart on Android 14–16.

## Verification command

`./gradlew testDebugUnitTest lintDebug assembleDebug` + manual on-device regression pass (Conversation mode, WalkieTalkie mode, API key file picker) on API 23, 31, 34, and 36 — existing automated coverage is minimal (`ENH-013`), so manual regression is load-bearing here.

## Out of scope (remains in Phase 4 backlog)

`ENH-013` (test coverage), `ENH-014` (deserialization), `ENH-015` (branch mismatch), `ENH-016` (`.idea` files committed), `ENH-017` (unrelated `.agents/skills/` content) — not touched by this evolve pass, still `status: proposed`.
