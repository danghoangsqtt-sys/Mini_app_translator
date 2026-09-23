# Execution Log — TERRA (Phase 3 Android Modernization)

## [2026-09-21 14:52:58 +07:00] Cụm A — Task 3.1, 3.2, 3.4, 3.5 hoàn tất

Commit: `0dcfe8ef2feaa20dd3152091504a5acb93f67009`

File đã đổi:

- `build.gradle`
- `gradle/wrapper/gradle-wrapper.properties`
- `app/build.gradle`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/nie/translator/rtranslatordevedition/tools/BluetoothHeadsetUtils.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationActivity.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationService.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/PairingFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/_conversation_mode/_conversation/connection_info/PeersInfoFragment.java`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/cloud_apis/voice/Recorder.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/AndroidModernizationManifestTest.java`

Lệnh đã chạy + kết quả tóm tắt:

- Dùng Microsoft OpenJDK `17.0.20.101` qua environment/CLI override; không sửa hay commit `gradle.properties` có sẵn.
- `gradlew.bat -Dorg.gradle.java.home=... :app:processDebugMainManifest :app:generateDebugProto :app:compileDebugJavaWithJavac`: PASS — manifest merger, protobuf generation và Java compilation hoạt động với AGP 8.13.2/Gradle 8.13/API 36.
- `gradlew.bat -Dorg.gradle.java.home=... testDebugUnitTest lintDebug assembleDebug assembleRelease`: PASS — 42 JVM tests, debug APK và unsigned release APK được tạo; debug/release merged manifests đã kiểm tra.
- Lint vẫn `abortOnError=false` theo entry contract trước Cụm C: 23 errors/169 warnings. Không suppress lint; Cụm C xử lý quality gate riêng.
- Merged debug/release manifests: `LoadingActivity` là launcher exported; activities/services nội bộ explicit non-exported; `GeneralService` abstract đã bị loại; chỉ `ConversationService` và `WalkieTalkieService` có `microphone|connectedDevice`.

STATUS: DONE — awaiting QA

## [2026-09-22 07:18:41 +07:00] AUDIT FOLLOW-UP — Cluster A / Tasks 3.1 and 3.2 remediation

Commit: `b3f76d05aa278affbf64763b50c505629b919289` — `fix(android): correct Nearby permissions and sync Gradle wrapper`

Changed files (commit scope only):

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationActivity.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/AndroidModernizationManifestTest.java`
- `gradle/wrapper/gradle-wrapper.jar`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradlew`
- `gradlew.bat`

Applied Nearby Connections matrix:

- API 23–28: legacy `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_COARSE_LOCATION`.
- API 29–30: legacy Bluetooth plus coarse/fine location. Coarse is the Android-required companion request for fine location; this resolves the real `CoarseFineLocation` contract without suppression.
- API 31: `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION`, `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`.
- API 32+: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `NEARBY_WIFI_DEVICES`; no location request after API 31.
- Manifest gates: legacy Bluetooth max 30; Wi-Fi state/change max 31; fine min 29/max 31; coarse max 31 (fine companion); modern Bluetooth min 31; Nearby Wi-Fi min 32; scan retains `neverForLocation`.
- `getRequiredNearbyPermissionsForSdk()` regression tests API 23/28/29/30/31/33/36. Existing permission guards stop advertising/discovery/connect/accept/reject/disconnect before communicator calls. Empty/partial/revoked results stay on the missing-permission path.

Wrapper remediation:

- Microsoft OpenJDK `17.0.20.1` used only through environment/CLI; no machine path or local `gradle.properties` setting was committed.
- Official `https://services.gradle.org/distributions/gradle-8.13-all.zip.sha256`: `fba8464465835e74f7270bbf43d6d8a8d7709ab0a43ce1aa3323f73e9aa0c612`.
- Ran twice: `gradlew.bat wrapper --gradle-version 8.13 --distribution-type all --gradle-distribution-sha256-sum fba8464465835e74f7270bbf43d6d8a8d7709ab0a43ce1aa3323f73e9aa0c612 --no-daemon --max-workers=1 --console=plain` — first PASS; second `:wrapper UP-TO-DATE` PASS.
- Artifact SHA-256: jar `81A82AAEA5ABCC8FF68B3DFCB58B3C3C429378EFD98E7433460610FECD7AE45F`; properties `AD03EE10E05D83BDEA5CBE7A23EB70BDC136C7FB02AA0315A8FE7B8B9AA08664`; `gradlew` `734B3879D3501DCE471CF0522D3BCBAFE76873D9FC5129345B67FB43BD15E933`; `gradlew.bat` `57931B17DD228E5C24DAC90E815D0BF82477E831A4618DFAB4136F5446B42A9F`.
- Fresh `GRADLE_USER_HOME`: downloaded Gradle 8.13 all distribution, wrapper verified its checksum, and `gradlew.bat --version --no-daemon --console=plain` reported Gradle 8.13 / Launcher JVM 17.0.20.1.

Commands and results:

- `gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon --max-workers=1 --console=plain` with JDK 17 and `GRADLE_OPTS=-Xmx2048m -Dfile.encoding=UTF-8`: PASS in 1m37s; 97 actionable tasks (21 executed, 76 up-to-date); debug APK PASS; `minifyReleaseWithR8` and unsigned release APK PASS.
- Unit XML: 17 suites, 43 tests, 0 failures, 0 errors, 0 skipped.
- Merged debug/release manifests inspected under `app/build/intermediates/merged_manifests/{debug,release}`: matrix declarations and ranges present.
- `git diff --check`: PASS (exit 0).

Lint XML `app/build/reports/lint-results-debug.xml`: **0 errors, 169 warnings**. No `@SuppressLint`, `tools:ignore`, lint baseline, or broad lint configuration was added. Inventory by ID/source:

- `AndroidGradlePluginVersion` 1 — wrapper properties; `GradleDependency` 10 and `NewerVersionAvailable` 7 — `app/build.gradle`.
- `Autofill` 1, `SmallSp` 2, `SpUsage` 1, `ContentDescription` 13, `HardcodedText` 15, `MergeRootFrame` 1, `Overdraw` 5, `UnknownIdInLayout` 3 — layout resources.
- `CanvasSize` 6, `ClickableViewAccessibility` 2, `ObsoleteSdkInt` 6, `UseRequiresApi` 1 — GraphView/GUI classes; `InflateParams` 6 — list adapters; `UseCompatLoadingForDrawables` 4 — Button classes.
- `DataExtractionRules` 1, `ScopedStorage` 1, `UnusedAttribute` 1 — `AndroidManifest.xml`; `DefaultLocale` 1 — `Translator.java`; `HardwareIds` 1 — `Global.java`; `SetTextI18n` 6 — API/security UI classes; `NotifyDataSetChanged` 2 — `MessagesAdapter.java`.
- `IconLocation` 4 and `MonochromeLauncherIcon` 1 — drawable/mipmap assets; `UnusedResources` 29 — drawable/layout/mipmap/raw/values resources; `Typos` 36 — `values/font_certs.xml`, `values-it/strings.xml`.
- `TrustAllX509TrustManager` 2 — transitive `google-http-client-1.19.0.jar`.

PENDING HUMAN EVIDENCE — API 23/31/34/36 and physical two-phone Bluetooth/SCO.

STATUS: DONE — awaiting QA

## [2026-09-21 15:30:43 +07:00] Cụm A — QA remediation hoàn tất, awaiting re-QA

Commit sửa Cụm A: `4121be7dead0ba27ca3543bf2aed1c68436ea07e`

Commit corrective (loại IDE metadata đã stage sẵn trong shared index, giữ nguyên file local): `3b6626085e41fb5bd65db6efe354dd0db8db41a3`

File đã đổi trong snapshot Cụm A sau remediation:

- `build.gradle`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/nie/translator/rtranslatordevedition/tools/BluetoothHeadsetUtils.java`
- `app/src/test/java/nie/translator/rtranslatordevedition/AndroidModernizationManifestTest.java`

Sửa theo QA verdict:

- Chốt AGP `8.13.2` (Gradle `8.13`, JDK `17`), là tổ hợp hỗ trợ API 36; loại AGP `8.3.2` và warning unsupported compileSdk do nó gây ra.
- Khai báo `ACCESS_COARSE_LOCATION` với `maxSdkVersion=30`, khớp runtime request của nhánh API 23–30; test manifest được mở rộng cho contract này.
- Tại năm thao tác headset/SCO được QA chỉ ra, dùng `ContextCompat.checkSelfPermission(..., BLUETOOTH_CONNECT)` trực tiếp trong nhánh gọi API để lint có thể phân tích guard; không thêm suppress lint.
- Bốn file `.idea` từng được stage sẵn ngoài phạm vi đã bị untrack bởi commit corrective; chúng vẫn tồn tại local và không xuất hiện trong snapshot HEAD của Cụm A.

Lệnh đã chạy + kết quả tóm tắt:

- `gradlew.bat ... --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease`: bắt đầu với AGP 8.13.2/JDK 17 nhưng host không đủ commit memory để tạo daemon với cấu hình local `-Xmx1536m`; retry `-Xmx768m` compile debug chạy nhưng process quality gate bị ngắt trước release/lint completion.
- `gradlew.bat ... --no-daemon lintDebug` với `-Xmx512m -XX:+UseSerialGC`: Gradle configuration và lint task khởi động thành công; lần rerun sau đó bị một tiến trình khác gửi `stop` đến shared daemon trước khi lint report hoàn tất.
- Do đó không ghi nhận false PASS cho host lint/release gate. Device matrix/physical Bluetooth evidence vẫn `PENDING HUMAN EVIDENCE`.

STATUS: DONE — awaiting QA re-check; host quality gate và device evidence remain pending

## [2026-09-21 16:11:40 +07:00] Cụm A — remediation 18 lint errors hoàn tất

Snapshot commit cho SOL re-QA: `0b50beff501e04bf37a3ae7ba38a44a681d30cd5` (`fix(android): resolve cluster A lint errors`)

Sửa cụ thể, không mass-suppress:

- `UseAppTint` (9): chuyển đúng chín `android:tint` bị lint chỉ ra sang `app:tint`; ba tint khác không thuộc lỗi vẫn giữ nguyên.
- `GestureBackNavigation` (3): thay ba override `onBackPressed()` bằng `OnBackPressedCallback` gắn lifecycle; khi cần fallback, callback tạm disable và delegate qua dispatcher để giữ hành vi back gốc.
- `MissingSuperCall` (2): bỏ cú pháp `super.` sai khi gán hai protected service fields; `super.onCreate(...)` vẫn được gọi.
- `RestrictedApi` (2): loại custom adapter wiring dùng `PreferenceGroupAdapter`/`onCreateAdapter` restricted; `PreferenceFragmentCompat` nay tự tạo, bind và quản lý RecyclerView qua public lifecycle.
- `NewApi` (1): bỏ `android:appComponentFactory` khai báo trực tiếp vốn đòi API 28; AndroidX/manifest merger tự xử lý component factory phù hợp.
- `WrongThread` (1): bỏ PNG compression và buffer không được tiêu thụ trong GraphView; snapshot vẫn được đưa trực tiếp vào `MediaStore` như trước.

Lệnh đã chạy trên Microsoft OpenJDK `17.0.20.101`, Gradle `8.13`, AGP `8.13.2`:

- `gradlew.bat testDebugUnitTest`: PASS — 42 tests, 0 failures, 0 errors.
- `gradlew.bat assembleDebug`: PASS — debug APK tạo thành công.
- `gradlew.bat assembleRelease`: PASS — `minifyReleaseWithR8` và unsigned release APK tạo thành công.
- `gradlew.bat lintDebug`: PASS — 0 errors, 169 warnings; không còn 18 error IDs được SOL nêu.

Handoff cho SOL:

- Re-QA đúng commit `0b50beff501e04bf37a3ae7ba38a44a681d30cd5` bằng đủ bốn Gradle gate.
- Human evidence vẫn thiếu: smoke matrix API 23/31/34/36 và kiểm thử hai điện thoại Bluetooth/SCO. Vì vậy Cụm A vẫn `BLOCKED/PENDING-HUMAN` dù code/lint sạch; không chuyển sang Cụm B (Task 3.3).

STATUS: DONE — awaiting QA

## [2026-09-22 07:18:41 +07:00] AUDIT FOLLOW-UP — Cluster A authoritative handoff

Commit: `b3f76d05aa278affbf64763b50c505629b919289` (`fix(android): correct Nearby permissions and sync Gradle wrapper`). Commit scope exactly: `AndroidManifest.xml`, `VoiceTranslationActivity.java`, `AndroidModernizationManifestTest.java`, `gradle-wrapper.jar`, `gradle-wrapper.properties`, `gradlew`, `gradlew.bat`.

Permission matrix: API 23–28 legacy Bluetooth + coarse; API 29–30 legacy Bluetooth + coarse/fine; API 31 coarse/fine + scan/connect/advertise; API 32+ scan/connect/advertise + Nearby Wi-Fi. Manifest: legacy Bluetooth max 30, Wi-Fi state/change max 31, fine min 29/max 31, coarse max 31 as required companion to fine, modern Bluetooth min 31, Nearby Wi-Fi min 32, scan `neverForLocation`. Regression test covers API 23/28/29/30/31/33/36; all Nearby flows retain pre-call permission guards and empty/partial/revoked results deny flow safely.

Wrapper: JDK 17 only by environment/CLI; official Gradle 8.13 all SHA-256 `fba8464465835e74f7270bbf43d6d8a8d7709ab0a43ce1aa3323f73e9aa0c612`; wrapper command ran twice (PASS, then UP-TO-DATE). Fresh `GRADLE_USER_HOME` downloaded and verified Gradle 8.13; `--version` reported Launcher JVM 17.0.20.1. Artifact hashes: jar `81A82AAEA5ABCC8FF68B3DFCB58B3C3C429378EFD98E7433460610FECD7AE45F`; properties `AD03EE10E05D83BDEA5CBE7A23EB70BDC136C7FB02AA0315A8FE7B8B9AA08664`; `gradlew` `734B3879D3501DCE471CF0522D3BCBAFE76873D9FC5129345B67FB43BD15E933`; bat `57931B17DD228E5C24DAC90E815D0BF82477E831A4618DFAB4136F5446B42A9F`.

Verification command: `gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon --max-workers=1 --console=plain` with JDK 17 and `GRADLE_OPTS=-Xmx2048m -Dfile.encoding=UTF-8`: PASS in 1m37s, 97 tasks; unit XML 17 suites/43 tests/0 failures/0 errors/0 skipped; debug APK PASS; release/R8 PASS. Merged debug/release manifests inspected. `git diff --check`: PASS.

Lint XML: **0 errors / 169 warnings**; no suppression added. ID/count/source inventory: `AndroidGradlePluginVersion` 1 (wrapper properties); `GradleDependency` 10 and `NewerVersionAvailable` 7 (app Gradle); `Autofill` 1, `ContentDescription` 13, `HardcodedText` 15, `MergeRootFrame` 1, `Overdraw` 5, `SmallSp` 2, `SpUsage` 1, `UnknownIdInLayout` 3 (layouts); `CanvasSize` 6, `ClickableViewAccessibility` 2, `ObsoleteSdkInt` 6, `UseRequiresApi` 1 (GraphView GUI); `InflateParams` 6 (adapters); `UseCompatLoadingForDrawables` 4 (buttons); `DataExtractionRules` 1, `ScopedStorage` 1, `UnusedAttribute` 1 (manifest); `DefaultLocale` 1 (Translator); `HardwareIds` 1 (Global); `SetTextI18n` 6 (API/security UI); `NotifyDataSetChanged` 2 (MessagesAdapter); `IconLocation` 4 and `MonochromeLauncherIcon` 1 (assets); `UnusedResources` 29 (resources); `TrustAllX509TrustManager` 2 (transitive google-http-client); `Typos` 36 (values XML).

PENDING HUMAN EVIDENCE — API 23/31/34/36 and physical two-phone Bluetooth/SCO.

STATUS: DONE — awaiting QA

## [2026-09-22 08:35:00 +07:00] Cluster A API 32 corrective handoff

Commit: `9ae921fd21748db97194c8949b7ea34ae4b42463` — `fix(android): correct API 32 Nearby permissions`

Changed files only: `app/src/main/AndroidManifest.xml`, `app/src/main/java/nie/translator/rtranslatordevedition/voice_translation/VoiceTranslationActivity.java`, `app/src/test/java/nie/translator/rtranslatordevedition/AndroidModernizationManifestTest.java`.

Corrective matrix: API 31–32 requests coarse/fine location with Bluetooth scan/connect/advertise; API 33+ requests Bluetooth scan/connect/advertise plus Nearby Wi-Fi. Manifest now has fine/coarse through max API 32 and Nearby Wi-Fi min API 33. Regression assertions now explicitly cover API 32 and confirm it equals the API 31 permission array; API 33 and 36 retain the Nearby Wi-Fi array.

Actual commands/results: JDK 17 `gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --no-daemon --max-workers=1 --console=plain` PASS in 1m25s (97 tasks); unit XML 17 suites/43 tests/0 failures/0 errors; debug/release/R8 PASS; merged debug/release manifests inspected; `git diff --check` PASS.

Lint XML actual for this host run: 0 errors / 169 warnings. The one warning difference from SOL's clean-worktree 168 is `AndroidGradlePluginVersion` sourced at `gradle/wrapper/gradle-wrapper.properties` (online version-catalog dependent); no suppression or lint configuration change was made. All remaining warning IDs/sources are listed in the immediately preceding authoritative handoff.

PENDING HUMAN EVIDENCE — API 23/31/34/36 and physical two-phone Bluetooth/SCO.

STATUS: DONE — awaiting QA

## [2026-09-22 13:52:30 +07:00] Device-evidence availability check

Snapshot lineage check: current HEAD `3ac6a6a4be8709b9974af04d3eb3f90968764169` contains required Cluster A snapshot `9ae921fd21748db97194c8949b7ea34ae4b42463`.

ADB check performed:

- Executable: `C:\Users\Admin\AppData\Local\Android\Sdk\platform-tools\adb.exe`
- Version: Android Debug Bridge 1.0.41 / 37.0.1-15733141.
- Command: `adb devices -l`
- Result: PASS for ADB availability; **no attached emulator or physical device listed**.

No APK was installed and no launch, Settings/API-key/file-picker, permission lifecycle, Conversation, WalkieTalkie, foreground-service, screen-lock, restart, process-recreation, Bluetooth, or SCO action was performed. There is no two-phone physical device evidence.

PENDING HUMAN EVIDENCE — no available API 23/31/34/36 device/emulator; two-phone Conversation Bluetooth/SCO; WalkieTalkie/headset; grant/deny/revoke; foreground-service lifecycle; UI/back-navigation.

STATUS: BLOCKED — awaiting device evidence and QA
