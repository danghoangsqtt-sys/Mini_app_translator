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
