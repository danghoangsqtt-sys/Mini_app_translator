# QA Log — SOL (Phase 3 Android Modernization)

## [2026-09-21 14:26:14 +07:00] PRECHECK — Phase 3 baseline
Commit: `249edc509dc5f732af0c75d35289ca218586fc75`

Lệnh đã chạy + kết quả tóm tắt:
- Đã đọc toàn bộ tài liệu bắt buộc ở Bước 0: `TRACKER.md`, `ROADMAP.md`, `SYSTEM-RULES.md`, `REMEDIATION-PLAN.md`, Phase 3 `SPEC.md`, `PHASE-STATE.md`, và task contracts `3.1`–`3.6`.
- `git status --short --branch`: nhánh `master` đang theo dõi `origin/master`; working tree **không sạch**.
- `EXEC-LOG-TERRA.md`: chưa tồn tại tại thời điểm tiền kiểm; chưa có cụm nào đủ điều kiện QA.

Baseline pending ngoài Phase 3, phải giữ nguyên và không gộp vào commit Phase 3:
- Phase 5/UI/assets: `.viepilot/phases/phase-5-mini-conversation-ui/SPEC.md`, thư mục task Phase 5 chưa track, các thay đổi/xóa trong `images/`, `images/icon.ico`, `Mo_May_Ao.bat`.
- IDE/local: `.idea/gradle.xml`, `.idea/misc.xml`, các file `.idea/*.xml` chưa track.
- Ngoài phạm vi khác: `.agents/`, `docs/brainstorm/session-2026-09-20.md`.

Baseline có thể ảnh hưởng trực tiếp môi trường build nhưng đã tồn tại trước Terra/Phase 3:
- `gradle.properties` đã sửa: thêm `org.gradle.java.home=C:\\Program Files\\Microsoft\\jdk-11.0.32.101-hotspot` và comment cấu hình debug signing cũ. Đường dẫn JDK cá nhân này trái ràng buộc Task 3.1/SPEC nếu bị đưa vào commit; Terra phải phân biệt đây là thay đổi pending có sẵn, không được gộp vào Cụm A.

Đối chiếu gate khởi động:
- PASS — Đã xác lập HEAD và inventory working tree ban đầu.
- PASS — Đã nhận diện rõ thay đổi Phase 5/IDE/asset là ngoài phạm vi; không xóa, không sửa, không commit gộp.
- BLOCKED — Chưa thể verify Cụm A/B/C vì `EXEC-LOG-TERRA.md` chưa có marker `STATUS: DONE — awaiting QA` và commit hash.

VERDICT: BLOCKED — chờ Terra tạo/cập nhật `EXEC-LOG-TERRA.md`; tuyệt đối chưa kiểm tra code dang dở.

## [2026-09-21 15:08:04 +07:00] Cụm A — Task(s) 3.1, 3.2, 3.4, 3.5
Commit: `0dcfe8ef2feaa20dd3152091504a5acb93f67009`

Lệnh đã chạy + kết quả tóm tắt:
- Xác minh commit: PASS — object tồn tại, là HEAD lúc bắt đầu QA, parent đúng baseline `249edc509dc5f732af0c75d35289ca218586fc75`, commit gộp đủ Task 3.1/3.2/3.4/3.5.
- Working tree bắt đầu có thay đổi mới ở `build.gradle` trong lúc QA, nhưng EXEC log chưa công bố Cụm B. SOL không kiểm tra code dang dở; toàn bộ gate quyết định được chạy trong worktree detached sạch tại đúng commit `0dcfe8e`.
- `gradlew.bat -Dorg.gradle.java.home=C:\\Program Files\\Microsoft\\jdk-17.0.20.101-hotspot --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease`: lệnh hoàn tất với exit 0 trên snapshot commit; 42 tests, 0 failures/errors/skipped; debug APK và unsigned release APK được tạo; R8/minify release hoàn tất.
- Toolchain thực tế trong commit: AGP `8.3.2`, Gradle `8.13`, JDK `17.0.20.1`, protobuf plugin `0.10.0`, `compileSdk 36`, `targetSdk 36`, `minSdk 23`. Gradle cảnh báo AGP 8.3.2 chỉ được kiểm thử tới compileSdk 34 và khuyến nghị plugin mới hơn. EXEC log ghi AGP `8.13.2`, không khớp commit.
- Lint snapshot chính xác: **20 errors / 165 warnings**, không phải 23/169 như EXEC log. So với baseline PM 5 errors/121 warnings, cả 5 lỗi baseline cũ (3 `ResourceType`, 2 `InvalidPackage`) đã biến mất nhưng có **20 lỗi mới**: `MissingPermission` 5, `MissingSuperCall` 2, `WrongThread` 1, `CoarseFineLocation` 1, `RestrictedApi` 2, `UseAppTint` 9. Exit 0 không phải bằng chứng lint sạch vì `abortOnError=false`/`checkReleaseBuilds=false` vẫn còn cho tới Cụm C.
- Merged manifest debug và release: PASS — `LoadingActivity` là component ứng dụng duy nhất exported; components nội bộ explicit non-exported; `GeneralService` không còn khai báo; chỉ `ConversationService` và `WalkieTalkieService` có `microphone|connectedDevice`; các permission FGS tương ứng có mặt.
- Regression test mới: 3 test manifest/source pass, nhưng chỉ xác minh khai báo tĩnh; không thay thế bằng chứng permission/service/launcher trên thiết bị.

Đối chiếu acceptance criteria:

Task 3.1:
- FAIL — Compatibility spike chưa chứng minh một toolchain **được hỗ trợ** cho API 36: commit dùng AGP 8.3.2 và chính Gradle báo plugin này chỉ được kiểm thử tới API 34. EXEC log còn ghi sai thành 8.13.2.
- FAIL — `testDebugUnitTest`, `assembleDebug`, `assembleRelease` và R8 chạy được bằng JDK 17, nhưng tiêu chí “supported clean build” chưa đạt vì lint có 20 lỗi mới và build chỉ xanh do lint không blocking.
- FAIL — API 36, namespace, Maven Central, Gradle/JDK/protobuf và bỏ explicit build tools đã có; phần tài liệu toolchain không chính xác vì phiên bản AGP trong EXEC log khác commit.
- PENDING-HUMAN — Chưa có regression/device evidence tích hợp từ 3.2/3.4/3.5 trên API 23/31/34/36.

Task 3.2:
- PASS — Manifest có `BLUETOOTH_SCAN` (`neverForLocation`), `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `NEARBY_WIFI_DEVICES`; runtime arrays được SDK-gate; advertising/discovery/connect/accept/reject/disconnect có guard ở các entry point đã sửa.
- FAIL — Legacy API 23–30 runtime array yêu cầu `ACCESS_COARSE_LOCATION`, nhưng source/merged manifest chỉ khai báo `ACCESS_FINE_LOCATION` với `maxSdkVersion=30`; lint xác nhận `CoarseFineLocation`. Path legacy vì vậy không có bằng chứng có thể grant đầy đủ.
- FAIL — Lint có 5 lỗi `MissingPermission` tại `BluetoothHeadsetUtils.java` (các thao tác headset/SCO). Dù code có helper kiểm tra `BLUETOOTH_CONNECT`, gate hiện không chứng minh tĩnh hoặc bằng thiết bị rằng mọi nhánh đều an toàn khi quyền bị revoke.
- PENDING-HUMAN — Chưa có bằng chứng grant/deny/revoke và `neverForLocation` trên API 31/34/36; chưa chứng minh advertising, discovery, connect, accept, disconnect không ném `SecurityException`.
- PENDING-HUMAN — Chưa có two-phone Conversation smoke test và one-phone WalkieTalkie/headset smoke test.
- PASS — Task 3.2 nằm cùng commit với 3.1/3.4/3.5.

Task 3.4:
- PASS — Manifest merger debug/release thành công; launcher exported true; toàn bộ activity/service/provider nội bộ quan sát được non-exported; `GeneralService` stale đã loại.
- PENDING-HUMAN — Chưa có bằng chứng cài đặt và launch launcher trên API 31/34/36.
- PASS — Integrated host build với 3.1 thành công.

Task 3.5:
- PASS — Source/merged manifests có đúng hai foreground voice services với `microphone|connectedDevice`; bound-only recognizer services không có FGS type; normal permissions tương ứng đã khai báo.
- PASS — `VoiceTranslationService` kiểm tra `RECORD_AUDIO` và `BLUETOOTH_CONNECT` trước foreground promotion và truyền type flags trên API 29+.
- PENDING-HUMAN — Chưa chứng minh không có missing-type/permission exception trong Conversation/WalkieTalkie trên API 34/36.
- PENDING-HUMAN — Chưa có bằng chứng background restriction, screen lock, stop/restart, process recreation; thời điểm promotion từ `onUnbind()` vẫn cần thiết bị xác nhận.
- PENDING-HUMAN — Chưa có đầy đủ ma trận API 23/31/34/36 và physical two-phone/SCO evidence; đây là release blocker.

Vấn đề Terra cần sửa trước khi yêu cầu QA lại Cụm A:
1. Dùng và ghi đúng một phiên bản AGP hỗ trợ compile/target API 36; EXEC log phải khớp commit và build không còn cảnh báo unsupported compileSdk.
2. Sửa mismatch `ACCESS_COARSE_LOCATION` giữa runtime request API 23–30 và manifest.
3. Xử lý/giải thích bằng chứng cụ thể cho 5 `MissingPermission` trong `BluetoothHeadsetUtils`; không mass-suppress.
4. Không coi lint exit 0 là pass; liệt kê và chuyển giao chính xác 20 errors/165 warnings cho Cụm C, đồng thời không để Cụm A được mô tả là “clean build”.
5. Cung cấp human/device evidence bắt buộc nêu trên; nếu chưa có, giữ nguyên `PENDING HUMAN EVIDENCE` và release blocker.

Không tạo request BUG/ENH mới: các vấn đề trên nằm trực tiếp trong phạm vi Task 3.1, 3.2 và lint gate 3.6.

VERDICT: FAIL — toolchain API 36 chưa được hỗ trợ, tài liệu phiên bản sai, permission legacy không nhất quán, và lint có 20 lỗi mới; đồng thời toàn bộ device matrix/physical Bluetooth evidence vẫn PENDING HUMAN.

## [2026-09-21 15:52:01 +07:00] Cụm A re-QA — Task(s) 3.1, 3.2, 3.4, 3.5
Commit: `3b6626085e41fb5bd65db6efe354dd0db8db41a3` (remediation code: `4121be7dead0ba27ca3543bf2aed1c68436ea07e`)

Lệnh đã chạy + kết quả tóm tắt:
- Xác minh snapshot: PASS — cả hai commit tồn tại; `4121be7` là ancestor trực tiếp của corrective commit `3b66260`; hash này chưa có verdict trước đó. Do working tree chính còn thay đổi Phase 5/IDE/local ngoài phạm vi, SOL dùng worktree detached sạch `D:\DataAdmin\qa-a-3b66` tại đúng `3b66260`.
- `gradlew.bat ... --max-workers=1 --no-daemon --console=plain testDebugUnitTest lintDebug assembleDebug assembleRelease` bằng Microsoft OpenJDK `17.0.20.1`, AGP `8.13.2`, Gradle `8.13`: lần quyết định cuối **BUILD SUCCESSFUL** trong 43 giây, 97 actionable tasks (15 executed, 82 up-to-date). Trước đó host có hai JVM native-memory crash và một R8 heap OOM; không lần nào được tính PASS. Sau khi commit memory của Windows hồi phục, lần cuối dùng heap 1024 MiB và hoàn tất cả bốn gate.
- Unit tests: PASS — 17 suites, 42 tests, 0 failures, 0 errors, 0 skipped.
- Artifacts/R8: PASS — `app-debug.apk` và `app-release-unsigned.apk` được tạo; `minifyReleaseWithR8` hoàn tất. Release APK vẫn unsigned đúng trạng thái hiện tại, không phải distributable release.
- Toolchain/static config: PASS — `compileSdk`/`targetSdk` 36, `minSdk` 23, namespace, Maven Central, Java source/target 8, protobuf plugin 0.10.0; không còn cảnh báo AGP không hỗ trợ compileSdk 36. Không có đường dẫn JDK/signing cá nhân trong snapshot commit.
- Lint snapshot chính xác từ `lint-results-debug.xml`: **18 errors / 168 warnings**. Baseline PM là 5 errors/121 warnings; 3 `ResourceType` và 2 `InvalidPackage` baseline không còn xuất hiện, nhưng có **18 lỗi mới**: `UseAppTint` 9, `GestureBackNavigation` 3, `MissingSuperCall` 2, `RestrictedApi` 2, `NewApi` 1, `WrongThread` 1. Năm `MissingPermission` và một `CoarseFineLocation` của verdict trước đã được loại. Exit 0 chỉ do `abortOnError=false`/`checkReleaseBuilds=false`, không phải lint sạch.
- Merged manifest debug/release: PASS — `LoadingActivity` exported true; toàn bộ activity/service/provider còn lại quan sát được exported false; `GeneralService` count 0; chỉ `ConversationService` và `WalkieTalkieService` có `microphone|connectedDevice`; hai recognizer service không có FGS type.
- Manifest/runtime permission static evidence: PASS — legacy `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` đều `maxSdkVersion=30`; `BLUETOOTH_SCAN` có `neverForLocation`; `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `NEARBY_WIFI_DEVICES` và hai FGS normal permissions có mặt. Runtime arrays được gate theo API 23–30/31–32/33+, và năm headset/SCO call site không còn `MissingPermission` lint error.
- Không phát hiện defect mới ngoài sáu task Phase 3; 18 lint errors thuộc trực tiếp phạm vi Task 3.6 nên không tạo BUG/ENH request mới. Không sửa code app, Phase 4/5, icon, `Mo_May_Ao.bat` hoặc `.idea`.

Đối chiếu acceptance criteria:

Task 3.1:
- PASS — Compatibility spike/build chứng minh AGP 8.13.2 + Gradle 8.13 + JDK 17 + protobuf generation hoạt động với API 36; manifest merger, Java compile, unit tests, debug build và release/R8 đều hoàn tất.
- FAIL — Tiêu chí “supported clean build” chưa đạt theo quality gate: lint có 18 lỗi mới và 168 warnings so với baseline 5/121; Gradle xanh vì lint chưa blocking.
- PASS — API 36 target, namespace, repository/tool versions và compatibility decisions khớp commit/EXEC log; thay đổi vẫn giới hạn ở cụm tích hợp Phase 3.
- PENDING-HUMAN — Chưa có regression/device evidence tích hợp trên API 23/31/34/36.

Task 3.2:
- PASS — Manifest declarations/flags và runtime request arrays đúng nhánh API; mismatch `ACCESS_COARSE_LOCATION` đã sửa.
- PASS — Static lint không còn `CoarseFineLocation` hoặc `MissingPermission`; advertising/discovery/connect/accept/reject/disconnect và headset/SCO có guard tại các call site đã sửa.
- PENDING-HUMAN — Chưa có bằng chứng grant/deny/revoke và đánh giá `neverForLocation` trên API 31/34/36; chưa chứng minh các flow không ném `SecurityException` trên thiết bị.
- PENDING-HUMAN — Chưa có two-phone Conversation và one-phone WalkieTalkie/headset/SCO smoke tests.
- PASS — Task 3.2 vẫn nằm cùng snapshot tích hợp với 3.1/3.4/3.5.

Task 3.4:
- PASS — Merged debug/release manifests xác nhận launcher exported true, components nội bộ/dependency quan sát được non-exported, `GeneralService` stale đã loại.
- PENDING-HUMAN — Chưa có install/launch evidence trên API 31/34/36.
- PASS — Manifest merger và integrated host build với 3.1 hoàn tất.

Task 3.5:
- PASS — Hai foreground voice services có đúng `microphone|connectedDevice` và normal permissions; bound-only recognizer services không bị gán type.
- PASS — Foreground promotion kiểm tra `RECORD_AUDIO` và `BLUETOOTH_CONNECT` trước khi gọi `startForeground`, kèm type flags trên API 29+.
- PENDING-HUMAN — Chưa có bằng chứng không có missing-type/permission exception trong Conversation/WalkieTalkie trên API 34/36.
- PENDING-HUMAN — Chưa có bằng chứng permission ordering, background start restrictions, screen lock, stop/restart, process recreation trên API 34/36 và tương thích API 23/31.
- PENDING-HUMAN — Chưa có đầy đủ ma trận API 23/31/34/36 và physical two-phone Bluetooth/SCO evidence; đây là release blocker.

Vấn đề Terra cần xử lý trước lần QA kế tiếp của Cụm A:
1. Không mô tả lint là sạch hoặc coi exit 0 là pass. Phải xử lý/disposition toàn bộ **18 lint errors mới theo ID/nguồn** để Cụm A không làm xấu baseline 5/121; không mass-suppress. Task 3.6 vẫn chịu trách nhiệm bật blocking gate sau Cụm B, nhưng thứ tự gate yêu cầu Cụm A PASS trước khi bắt đầu Cụm B.
2. Warning count hiện là **168**, tăng 47 so với baseline 121; chuyển giao danh sách/triage cụ thể cho Task 3.6, không dùng broad suppression.
3. Cung cấp human/device evidence bắt buộc nêu trên. Nếu host/static findings đã sạch nhưng evidence vẫn thiếu, verdict kế tiếp tối đa là BLOCKED/PENDING-HUMAN, không được tự pass.

VERDICT: FAIL — remediation đã sửa đúng AGP, legacy permission và năm headset/SCO `MissingPermission`, đồng thời full host build/R8 chạy thành công; tuy nhiên lint vẫn lệch baseline với 18 lỗi mới/168 warnings. Ma trận API 23/31/34/36 và physical two-phone Bluetooth/SCO evidence tiếp tục PENDING HUMAN EVIDENCE và là release blocker.

## [2026-09-21 21:21:02 +07:00] Cụm A re-QA #2 — Task(s) 3.1, 3.2, 3.4, 3.5
Commit: `0b50beff501e04bf37a3ae7ba38a44a681d30cd5`

Lệnh đã chạy + kết quả tóm tắt:
- Xác minh snapshot: PASS — commit tồn tại, là hậu duệ của corrective snapshot `3b6626085e41fb5bd65db6efe354dd0db8db41a3`, chưa từng được QA. Working tree chính còn thay đổi ngoài phạm vi nên toàn bộ kiểm tra chạy trong worktree detached sạch `D:\DataAdmin\qa-a-0b50` tại đúng hash trên.
- `gradlew.bat ... --max-workers=1 --no-daemon --console=plain testDebugUnitTest lintDebug assembleDebug assembleRelease` bằng Microsoft OpenJDK `17.0.20.1`, AGP `8.13.2`, Gradle `8.13`: **BUILD SUCCESSFUL** trong 1 phút 52 giây; 97/97 actionable tasks executed.
- Unit tests: PASS — 17 suites, 42 tests, 0 failures, 0 errors, 0 skipped.
- Artifacts/R8: PASS — debug APK và unsigned release APK được tạo; `minifyReleaseWithR8` hoàn tất.
- Lint XML: PASS về error gate — **0 errors / 168 warnings**. So với baseline PM 5 errors/121 warnings: ba `ResourceType` = 0, hai `InvalidPackage` = 0; toàn bộ 18 error IDs của verdict trước (`UseAppTint`, `GestureBackNavigation`, `MissingSuperCall`, `RestrictedApi`, `NewApi`, `WrongThread`) đều = 0. EXEC log ghi 169 warnings nhưng report QA độc lập ghi **168**; dùng số từ XML làm bằng chứng quyết định.
- Warning inventory hiện tại: `Typos` 36, `UnusedResources` 29, `HardcodedText` 15, `ContentDescription` 13, `GradleDependency` 10, `NewerVersionAvailable` 7, bốn nhóm 6 (`CanvasSize`, `ObsoleteSdkInt`, `SetTextI18n`, `InflateParams`), `Overdraw` 5, hai nhóm 4 (`UseCompatLoadingForDrawables`, `IconLocation`), `UnknownIdInLayout` 3, bốn nhóm 2 và mười nhóm 1; tổng 168, tăng 47 so với baseline warning count. Đây là inventory chuyển giao cho Cụm C, không được mass-suppress.
- Kiểm tra suppression: PASS — commit không thêm `@SuppressLint`, lint baseline, broad `disable`, `warningsAsErrors` override hay `tools:ignore`; chỉ còn `GoogleAppIndexingWarning` ignore có sẵn. `abortOnError=false`/`checkReleaseBuilds=false` vẫn tồn tại theo sequencing và phải được xử lý ở Task 3.6.
- Merged debug/release manifests: PASS — `appComponentFactory` vẫn được AndroidX merger cung cấp; `LoadingActivity` exported true; mọi activity/service/provider còn lại quan sát được exported false; `GeneralService` count 0; chỉ `ConversationService` và `WalkieTalkieService` có `microphone|connectedDevice`.
- Permission contract: PASS tĩnh — legacy Bluetooth/location permissions đều `maxSdkVersion=30`; `BLUETOOTH_SCAN` có `neverForLocation`; `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE`, `NEARBY_WIFI_DEVICES` và hai FGS normal permissions có mặt.
- Review code lint fixes: không thấy defect tĩnh mới. Việc đổi ba back handlers sang `OnBackPressedDispatcher`, bỏ custom restricted adapter/layout wiring của `SettingsFragment`, và giữ snapshot sharing sau khi bỏ buffer compression thừa vẫn cần on-device/UI regression evidence; unit/lint/build không thay thế bằng chứng này.
- Không tạo BUG/ENH request mới: chưa phát hiện defect mới ngoài sáu task Phase 3. Không sửa code app hoặc file Phase 4/5/IDE/assets trong quá trình QA.

Đối chiếu acceptance criteria:

Task 3.1:
- PASS — AGP 8.13.2 + Gradle 8.13 + JDK 17 + protobuf generation hoạt động với compile/target API 36; manifest merge, Java compile, 42 unit tests, lint, debug build và release/R8 hoàn tất.
- PASS — API 36, namespace, Maven Central, Java 8 source/target và toolchain decisions khớp snapshot; không có warning unsupported compileSdk.
- PENDING-HUMAN — Chưa có regression evidence cho chức năng hiện hữu trên API 23/31/34/36, gồm launcher, Settings/API-key flow và back gesture/navigation vừa sửa.

Task 3.2:
- PASS — Manifest/runtime permission arrays và guards đúng theo kiểm tra tĩnh; không còn `MissingPermission`/`CoarseFineLocation` lint error.
- PENDING-HUMAN — Chưa có grant/deny/revoke evidence, đánh giá `neverForLocation`, hoặc chứng minh advertising/discovery/connect/accept/disconnect không ném `SecurityException` trên API 31/34/36.
- PENDING-HUMAN — Chưa có legacy API 23 permission/device evidence, two-phone Conversation test và one-phone WalkieTalkie/headset/SCO test.
- PASS — 3.2 vẫn nằm trong cùng chuỗi commit tích hợp với 3.1/3.4/3.5.

Task 3.4:
- PASS — Debug/release manifest merger và export-surface review đạt: launcher true, internal/dependency components false, stale `GeneralService` đã loại.
- PENDING-HUMAN — Chưa có install/launch evidence trên API 31/34/36.

Task 3.5:
- PASS — Hai voice foreground services có đúng `microphone|connectedDevice`, matching normal permissions, runtime permission checks trước promotion; recognizer services không bị gán type.
- PENDING-HUMAN — Chưa chứng minh không có missing-type/permission exception trong Conversation/WalkieTalkie trên API 34/36.
- PENDING-HUMAN — Chưa có permission ordering, background restriction, screen-lock, stop/restart và process-recreation evidence trên API 34/36, cùng compatibility behavior trên API 23/31.
- PENDING-HUMAN — Toàn bộ ma trận API 23/31/34/36 và physical two-phone Bluetooth/SCO evidence vẫn thiếu; đây là release blocker.

Điều kiện để gỡ BLOCKED:
1. Cung cấp log/video/checklist thiết bị định danh rõ API/device cho toàn bộ matrix và Bluetooth/SCO flows nêu trên.
2. Xác nhận Settings rendering, toolbar/system back, predictive/back gesture, API-key file picker, Conversation và WalkieTalkie không regress sau lint remediation.
3. Cụm B chưa được phép bắt đầu cho tới khi Cụm A có verdict PASS theo thứ tự gate đã chốt.

VERDICT: BLOCKED — toàn bộ host/static criteria của Cụm A hiện PASS, lint có 0 errors và không mass-suppress; nhưng acceptance criteria bắt buộc về API 23/31/34/36 và physical two-phone Bluetooth/SCO chưa có bằng chứng. Cụm A chưa PASS và chưa mở gate cho Cụm B.

## [2026-09-21 22:06:04 +07:00] AUDIT FOLLOW-UP — Cụm A / Task 3.1, 3.2
Commit được rà soát: `0b50beff501e04bf37a3ae7ba38a44a681d30cd5` (code snapshot hiện tại; không chạy lại full Gradle gate)

Bằng chứng mới từ audit mã nguồn và tài liệu chính thức:
- FAIL — Ma trận quyền Nearby Connections chưa đúng cho API 29–31. Tài liệu Google yêu cầu `ACCESS_COARSE_LOCATION` đến API 28 và `ACCESS_FINE_LOCATION` từ API 29 đến 31, đồng thời các quyền nguy hiểm phải được request runtime trước advertising/discovery. Manifest hiện giới hạn `ACCESS_FINE_LOCATION` ở API 30; `VoiceTranslationActivity.getRequiredNearbyPermissions()` chỉ request `ACCESS_COARSE_LOCATION` trên API 23–30 và không request location trên API 31. Vì vậy static acceptance của Task 3.2 chưa đạt, dù lint không phát hiện và device evidence chưa có. Nguồn: https://developers.google.com/nearby/connections/android/get-started
- GAP — `gradle-wrapper.properties` trỏ tới Gradle 8.13 nhưng `gradle-wrapper.jar`, `gradlew`, `gradlew.bat` trên `master` vẫn không đổi từ commit ban đầu `63128ec`; `distributionSha256Sum` cũng chưa có. Gradle xác nhận wrapper cũ thường vẫn chạy được, nhưng cần chạy wrapper task lần hai để đồng bộ toàn bộ wrapper artifacts; checksum là guardrail tái lập/supply-chain nên cần disposition trong Task 3.1/3.6. Nguồn: https://docs.gradle.org/current/userguide/gradle_wrapper.html
- LOCAL-ENV — Android Studio đã cài nhưng `.idea/gradle.xml` local đang chọn `jbr-25`; Gradle 8.13 không hỗ trợ chạy bằng Java 25 (Java 25 cần Gradle 9.1+). CLI JDK 17 hiện hoạt động. Đây là cấu hình local ngoài commit app; chọn Microsoft JDK 17 làm Gradle JDK và không commit đường dẫn máy cá nhân. Nguồn: https://docs.gradle.org/current/userguide/compatibility.html

Đối chiếu acceptance criteria:
- Task 3.1: PASS phần AGP 8.13.2/API 36/JDK 17 host build theo re-QA trước; GAP phần wrapper artifacts/checksum và IDE local JBR selection cần được xử lý hoặc disposition rõ.
- Task 3.2: FAIL phần permission matrix tĩnh API 29–31; cần corrective commit rồi re-QA trước human device matrix.
- PENDING-HUMAN vẫn giữ nguyên cho API 23/31/34/36, grant/deny/revoke, two-phone Conversation, WalkieTalkie, SCO/headset, background/screen-lock/restart/process-recreation và UI/back-navigation.

Không tạo request mới: hai code finding thuộc trực tiếp Task 3.1/3.2; cấu hình JBR thuộc môi trường local/IDE ngoài phạm vi app. Không sửa code app và không mở Cụm B.

VERDICT: FAIL/BLOCKED — Cụm A cần corrective commit cho permission matrix API 29–31 (và disposition wrapper) trước khi có thể quay lại trạng thái chỉ chờ human evidence.

## [2026-09-22 08:05:12 +07:00] Cụm A re-QA #3 — Task(s) 3.1, 3.2, 3.4, 3.5
Commit: `b3f76d05aa278affbf64763b50c505629b919289`

Lệnh đã chạy + kết quả tóm tắt:
- Xác minh snapshot: PASS — commit tồn tại, là hậu duệ của snapshot `0b50beff501e04bf37a3ae7ba38a44a681d30cd5`, chưa từng được QA. Working tree chính còn thay đổi ngoài phạm vi; toàn bộ kiểm tra quyết định chạy trong worktree detached sạch `D:\DataAdmin\qa-a-b3f7` đúng tại commit này.
- Commit scope: PASS — đúng 7 file được handoff: manifest, `VoiceTranslationActivity.java`, regression test, và bốn wrapper artifacts. Không có Phase 4/5, icon, `Mo_May_Ao.bat`, `.idea`, local Gradle config hoặc log trong commit.
- `gradlew.bat --no-daemon --max-workers=1 --console=plain testDebugUnitTest lintDebug assembleDebug assembleRelease` bằng Microsoft OpenJDK 17.0.20.1, AGP 8.13.2, Gradle 8.13: **BUILD SUCCESSFUL** trong 2 phút 13 giây; 97/97 actionable tasks executed. Hai lần chuẩn bị trước bị lỗi truyền tham số PowerShell và thiếu SDK location trong detached worktree, không phải kết quả code và không được tính verdict.
- Unit XML: PASS — 17 suites, 43 tests, 0 failures, 0 errors, 0 skipped.
- Artifacts/R8: PASS — debug APK (7,905,823 bytes) và unsigned release APK (2,040,108 bytes) được tạo; `minifyReleaseWithR8` hoàn tất.
- Lint XML độc lập: **0 errors / 168 warnings**, không phải 169 warnings như EXEC handoff. Inventory giữ nguyên so với re-QA trước, ngoại trừ không có `AndroidGradlePluginVersion`: `Typos` 36, `UnusedResources` 29, `HardcodedText` 15, `ContentDescription` 13, `GradleDependency` 10, `NewerVersionAvailable` 7, `CanvasSize`/`ObsoleteSdkInt`/`SetTextI18n`/`InflateParams` mỗi ID 6, `Overdraw` 5, `UseCompatLoadingForDrawables`/`IconLocation` mỗi ID 4, `UnknownIdInLayout` 3, và các ID còn lại tổng 15. Không phát hiện mass-suppress mới; `abortOnError=false`/`checkReleaseBuilds=false` có sẵn vẫn phải được Cụm C xử lý.
- Merged debug/release manifests: PASS — launcher `LoadingActivity` exported true; các component nội bộ quan sát được exported false; `GeneralService` không còn khai báo; chỉ `ConversationService` và `WalkieTalkieService` có `microphone|connectedDevice`; normal permissions tương ứng hiện diện.
- Wrapper: PASS về khả năng chạy và tái lập distribution — wrapper tải/xác minh Gradle 8.13 với official distribution SHA-256 trong properties. SHA-256 của JAR và BAT khớp handoff. Hash của `gradlew` và properties sau checkout không khớp các hash text trong EXEC vì repository có `core.autocrlf=true` và không có attribute cố định EOL; đây là sai khác biểu diễn line-ending, không phải thay đổi snapshot. Handoff sau nên ghi rõ raw blob hay checkout/EOL context nếu dùng hash text làm bằng chứng.
- `git diff --check`: PASS theo handoff; detached worktree sạch sau gate.

Đối chiếu acceptance criteria:

Task 3.1:
- PASS — AGP 8.13.2 + Gradle 8.13 + JDK 17 + protobuf generation hoạt động với compile/target API 36; manifest merge, Java compile, 43 unit tests, lint, debug và release/R8 hoàn tất.
- PASS — Wrapper artifacts/checksum đã được đồng bộ; namespace, repository/tool versions và compatibility decisions có bằng chứng host.
- FAIL — Tiêu chí integrated permission checks chưa đạt vì nhánh API 32 của Task 3.2 sai platform contract như nêu dưới đây.
- PENDING-HUMAN — Chưa có regression/device evidence tích hợp trên API 23/31/34/36.

Task 3.2:
- PASS — Nhánh API 23–30 và API 31 đã được sửa theo Nearby Connections; regression unit test hiện có API 23/28/29/30/31/33/36 và các permission guards tĩnh vẫn hiện diện.
- **FAIL — API 32 bị regression.** Source dùng `sdk >= Build.VERSION_CODES.S_V2` để bỏ `ACCESS_FINE_LOCATION` và yêu cầu `android.permission.NEARBY_WIFI_DEVICES`; manifest cũng khai báo `NEARBY_WIFI_DEVICES` từ API 32 và giới hạn fine location tới API 31. Nhưng Android platform định nghĩa `NEARBY_WIFI_DEVICES` từ API 33; Android 12L/API 32 vẫn cần `ACCESS_FINE_LOCATION` cho Wi-Fi/Nearby APIs. Vì vậy trên API 32 app có thể yêu cầu một permission chưa tồn tại và không thể thỏa permission guard, làm chặn advertising/discovery. Unit matrix bỏ đúng API 32 nên không bắt được lỗi. Nguồn platform: https://developer.android.com/develop/connectivity/wifi/wifi-permissions và https://developer.android.com/reference/android/Manifest.permission
- FAIL — Cần đổi ranh giới sang API 33+: API 32 giữ fine location (cùng Bluetooth runtime permissions phù hợp), manifest range tương ứng phải bao phủ API 32, và thêm regression test riêng cho API 32. Không được chỉ sửa test để hợp thức hóa hành vi hiện tại.
- PENDING-HUMAN — Chưa có grant/deny/revoke, `neverForLocation`, advertising/discovery/connect/accept/disconnect evidence trên API 31/34/36; chưa có API 23 legacy device evidence.
- PENDING-HUMAN — Chưa có two-phone Conversation và one-phone WalkieTalkie/headset/SCO smoke tests.

Task 3.4:
- PASS — Debug/release manifest merger và export-surface review đạt trên host.
- PENDING-HUMAN — Chưa có install/launcher/UI/back-navigation evidence trên API 31/34/36.

Task 3.5:
- PASS — Static manifest/service type và pre-promotion permission checks đạt; host build/R8 hoàn tất.
- PENDING-HUMAN — Chưa có bằng chứng không có missing-type/permission exception trong Conversation/WalkieTalkie trên API 34/36.
- PENDING-HUMAN — Chưa có permission ordering, background restriction, screen lock, stop/restart, process recreation trên API 34/36 và compatibility API 23/31.
- PENDING-HUMAN — Toàn bộ ma trận API 23/31/34/36 và physical two-phone Bluetooth/SCO vẫn là release blocker.

Vấn đề Terra cần sửa trước lần re-QA tiếp theo:
1. Sửa permission branch/manifest cho API 32 như trên và thêm API 32 regression test; giữ thay đổi trong Cụm A.
2. Re-run đủ bốn Gradle gate, báo lint theo XML/ID/count thực tế (hiện QA là 0/168), rồi append commit mới với `STATUS: DONE — awaiting QA`.
3. Không bắt đầu Cụm B/C trước khi Cụm A PASS. Human evidence vẫn bắt buộc ngay cả sau khi static fix qua gate.

Không tạo BUG/ENH request mới: finding API 32 thuộc trực tiếp Task 3.2. Không sửa code app hoặc file Phase 4/5 trong quá trình QA.

VERDICT: FAIL — host build, tests, lint, artifacts, wrapper và static component/service checks đều đạt, nhưng permission matrix làm hỏng API 32. Ma trận API 23/31/34/36 và physical two-phone Bluetooth/SCO tiếp tục PENDING HUMAN EVIDENCE và là release blocker.

## [2026-09-22 08:43:02 +07:00] Cụm A re-QA #4 — Task(s) 3.1, 3.2, 3.4, 3.5
Commit: `9ae921fd21748db97194c8949b7ea34ae4b42463`

Lệnh đã chạy + kết quả tóm tắt:
- Xác minh snapshot: PASS — commit tồn tại, parent trực tiếp là SOL QA-doc commit `75be7974e8afd76bd74685fb46f35cddcf063eee`, chưa từng được QA và EXEC có marker `STATUS: DONE — awaiting QA`. Commit chỉ đổi manifest, `VoiceTranslationActivity.java` và regression test; không chứa Phase 4/5, IDE, ảnh hoặc local config.
- Working tree chính còn thay đổi ngoài phạm vi; toàn bộ kiểm tra quyết định chạy trong detached worktree sạch `D:\DataAdmin\qa-a-9ae9` đúng tại commit này.
- `gradlew.bat --no-daemon --max-workers=1 --console=plain testDebugUnitTest lintDebug assembleDebug assembleRelease` bằng Microsoft OpenJDK 17.0.20.1, AGP 8.13.2, Gradle 8.13: **BUILD SUCCESSFUL** trong 2 phút 14 giây; 97/97 actionable tasks executed.
- Unit XML: PASS — 17 suites, 43 tests, 0 failures, 0 errors, 0 skipped.
- Artifacts/R8: PASS — debug APK 7,905,835 bytes và unsigned release APK 2,040,112 bytes được tạo; `minifyReleaseWithR8` hoàn tất.
- Lint XML độc lập: **0 errors / 168 warnings**. Chênh 1 warning so với EXEC `0/169` là `AndroidGradlePluginVersion` phụ thuộc online version lookup; không có thay đổi suppression/config trong corrective commit. Inventory QA: `Typos` 36, `UnusedResources` 29, `HardcodedText` 15, `ContentDescription` 13, `GradleDependency` 10, `NewerVersionAvailable` 7, bốn ID có 6 warnings, `Overdraw` 5, hai ID có 4, `UnknownIdInLayout` 3, bốn ID có 2 và chín ID có 1; tổng 168.
- API 32 correction: PASS tĩnh — runtime boundary đổi sang `Build.VERSION_CODES.TIRAMISU`; API 31–32 yêu cầu coarse/fine location cùng Bluetooth scan/connect/advertise; API 33+ dùng Bluetooth trio cùng `NEARBY_WIFI_DEVICES`. Manifest fine/coarse max API 32, Nearby Wi-Fi min API 33. Unit regression thêm assertion API 32 bằng permission array API 31.
- Merged debug/release manifests: PASS — permission ranges sau merge hiện diện; launcher exported true; components nội bộ quan sát được false; chỉ `ConversationService` và `WalkieTalkieService` có `microphone|connectedDevice`.
- `git diff --check`: PASS; detached worktree sạch sau gate. Không phát hiện mass-suppress hoặc defect mới ngoài sáu task Phase 3.

Đối chiếu acceptance criteria:

Task 3.1:
- PASS — Compatibility spike/toolchain API 36, protobuf generation, manifest merge, test, lint, debug, release và R8 đều đạt bằng JDK 17.
- PASS — Wrapper/checksum, namespace, repository/tool versions và compatibility decisions đã có bằng chứng host.
- PASS — Integrated static/build checks của 3.2/3.4/3.5 đạt trong cùng chuỗi Cụm A.
- PENDING-HUMAN — Chưa có integrated regression/device matrix API 23/31/34/36.

Task 3.2:
- PASS — Corrective API 32 sửa đúng ranh giới runtime/manifest và có regression unit test; static permission declarations/guards đạt, lint không có `MissingPermission` hoặc `CoarseFineLocation`.
- PENDING-HUMAN — Chưa có bằng chứng grant/deny/revoke, `neverForLocation`, hoặc advertising/discovery/connect/accept/disconnect không ném `SecurityException` trên API 31/34/36.
- PENDING-HUMAN — Chưa có API 23 legacy location/Wi-Fi/Bluetooth evidence.
- PENDING-HUMAN — Chưa có two-phone Conversation và one-phone WalkieTalkie/headset/SCO smoke tests.

Task 3.4:
- PASS — Debug/release manifest merger và external component surface review đạt trên host.
- PENDING-HUMAN — Chưa có install/launcher evidence trên API 31/34/36.
- PENDING-HUMAN — Chưa có Settings/API-key/file-picker, toolbar/system/predictive-back regression evidence sau các sửa lint trước đó.

Task 3.5:
- PASS — Hai foreground voice services có đúng type/normal permissions và static pre-promotion permission checks; recognizer services vẫn bound-only.
- PENDING-HUMAN — Chưa có bằng chứng không có missing-type/permission exception trong Conversation/WalkieTalkie trên API 34/36.
- PENDING-HUMAN — Chưa có permission ordering, background start restriction, screen lock, stop/restart, process recreation trên API 34/36 và compatibility API 23/31.
- PENDING-HUMAN — Ma trận API 23/31/34/36 và physical two-phone Bluetooth/SCO là release blocker bắt buộc.

Điều kiện để gỡ BLOCKED:
1. Cung cấp log/video/checklist định danh rõ thiết bị và API cho toàn bộ device matrix và flow Bluetooth/SCO nêu trên.
2. Cung cấp UI/back-navigation regression evidence cho các flow đã bị tác động trong Cụm A.
3. Cụm B/C chưa được bắt đầu cho tới khi Cụm A có verdict PASS.

Không tạo BUG/ENH request mới: corrective commit đã xử lý finding API 32 và không phát hiện defect mới ngoài sáu task. Không sửa code app hoặc file ngoài phạm vi trong quá trình QA.

VERDICT: BLOCKED — toàn bộ host/static criteria của Cụm A hiện PASS, lint 0 errors và không mass-suppress; nhưng acceptance criteria bắt buộc về API 23/31/34/36, physical two-phone Bluetooth/SCO và UI/device regression vẫn PENDING HUMAN EVIDENCE. Cụm A chưa PASS; Cụm B/C tiếp tục khóa.

## [2026-09-22 10:13:13 +07:00] SOL HANDOFF — TERRA NEXT ACTION

Áp dụng cho snapshot Cụm A `9ae921fd21748db97194c8949b7ea34ae4b42463` và verdict QA commit `44b3d4e7a2b57152c8e4aca90168eed6d69710a2`.

TERRA — NEXT ACTION:
1. Đọc đầy đủ verdict `Cụm A re-QA #4` ngay phía trên; coi đây là chỉ thị hiện hành thay cho các entry cũ.
2. Không sửa thêm code Cụm A khi chưa có finding mới; không bắt đầu Cụm B/C.
3. Giữ trạng thái `BLOCKED — PENDING HUMAN EVIDENCE` cho tới khi SOL nhận đủ bằng chứng thiết bị API 23/31/34/36, two-phone Conversation Bluetooth/SCO, WalkieTalkie/headset, grant/deny/revoke, foreground-service lifecycle và UI/back-navigation.
4. Khi có bằng chứng mới, chỉ append kết quả/bằng chứng có thể truy vết vào `EXEC-LOG-TERRA.md`; không tự đánh dấu PASS. SOL sẽ đọc marker/handoff mới và quyết định gate tiếp theo.

Đây là handoff tự động qua kênh file đã phê duyệt; người dùng không cần sao chép prompt này sang phiên TERRA nếu TERRA đang poll `QA-LOG-SOL.md` đúng quy ước.
