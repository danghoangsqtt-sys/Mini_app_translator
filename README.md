# Mini Conversation

Ứng dụng dịch giọng nói thời gian thực dành cho Android, hỗ trợ hội thoại giữa hai thiết bị và chế độ bộ đàm trên một thiết bị.

> [!IMPORTANT]
> Tên repository là **Mini App Translator**, nhưng ứng dụng đã được đổi tên hiển thị thành **Mini Conversation** (Phase 5); mã nguồn là ứng dụng Android native viết bằng Java, không phải mini app chạy trong trình duyệt. Dự án là một fork đang được hiện đại hóa từ [RTranslator](https://github.com/niedev/RTranslator).

## Tính năng

- **Conversation mode**: kết nối hai hoặc nhiều thiết bị qua Bluetooth để trao đổi hội thoại đã dịch.
- **Walkie-Talkie mode**: hai người dùng chung một điện thoại và nói luân phiên bằng hai ngôn ngữ.
- Nhận dạng giọng nói bằng Android `SpeechRecognizer`, ưu tiên recognizer on-device khi thiết bị hỗ trợ.
- Dịch văn bản bằng Google ML Kit Translation với model được tải và quản lý trên thiết bị.
- Phát lại nội dung đã dịch bằng Text-to-Speech của Android.
- Hỗ trợ tai nghe Bluetooth trong Conversation mode.
- Lưu thiết bị gần đây bằng Room; màn hình thống kê Cloud cũ chỉ còn là công cụ migration nâng cao.
- Có thể tiếp tục dịch khi ứng dụng chạy nền trong các chế độ hội thoại.

> [!NOTE]
> Ảnh chụp màn hình Conversation mode và WalkieTalkie mode trước đây đã được gỡ bỏ (`BUG-016`) vì không còn phản ánh giao diện Mini Conversation. Ảnh mới sẽ được thêm lại từ bộ screenshot baseline light/dark của task 5.6 sau khi có bằng chứng chạy trên thiết bị thật.

## Trạng thái dự án

| Thành phần | Giá trị hiện tại |
|---|---|
| Phiên bản ứng dụng | `1.2.0` (`versionCode 15`) |
| Ngôn ngữ | Java |
| Nền tảng | Android native |
| Min SDK | API 23 (Android 6.0) |
| Compile/Target SDK | API 36 (Android 16) |
| Android Gradle Plugin | 8.13.2 |
| Gradle Wrapper | 8.13 |
| Giấy phép | Apache License 2.0 |

Luồng on-device mặc định đã được tích hợp và có unit/instrumentation coverage. Dự án vẫn chưa phải release candidate: kiểm thử hai điện thoại thật, ma trận API 23/31/34/36, accessibility, signing và dependency refresh vẫn là các gate bắt buộc.

## Kiến trúc tổng quan

```text
Microphone
    │
    ▼
Android SpeechRecognizer
    │ transcript
    ▼
ML Kit Translation (downloaded model)
    │
    ├──► Android Text-to-Speech
    │
    └──► Bluetooth peer (Conversation mode)
```

Các package chính:

- `voice_translation/engines`: contracts và các implementation on-device/legacy được cô lập.
- `voice_translation/cloud_apis`: implementation Cloud cũ, không thuộc default runtime.
- `voice_translation/_conversation_mode`: ghép nối và hội thoại qua Bluetooth.
- `voice_translation/_walkie_talkie_mode`: nhận dạng luân phiên hai ngôn ngữ.
- `api_management`: quản lý credential Cloud legacy tùy chọn và thống kê lịch sử.
- `database`: Room database cho lịch sử thiết bị và mức sử dụng.
- `tools`: tiện ích âm thanh, Bluetooth, mã hóa và giao diện.

## Yêu cầu

- Android Studio có thể mở dự án dùng Android Gradle Plugin 8.13.2.
- JDK 17 được yêu cầu cho toolchain Gradle 8.13 hiện tại.
- Android SDK Platform 36.
- Thiết bị Android 6.0 trở lên có microphone.
- Bluetooth Low Energy cho Conversation mode.
- Android speech-recognition service có sẵn trên thiết bị; mức hỗ trợ on-device tùy thiết bị/ngôn ngữ.
- Dung lượng trống và kết nối mạng để tải model ML Kit trước khi dịch offline.

Thiết lập JDK 17 theo môi trường của mỗi máy, không commit đường dẫn JDK vào `gradle.properties`. Trên Windows, đặt `JAVA_HOME` tới JDK 17 của bạn và đưa `$env:JAVA_HOME\bin` vào `Path` cho phiên PowerShell; trong Android Studio, chọn cùng JDK 17 ở Gradle JDK. Xác minh bằng `java -version` và `./gradlew --version` trước khi build.

## Build từ mã nguồn

Clone repository:

```bash
git clone https://github.com/danghoangsqtt-sys/Mini_app_translator.git
cd Mini_app_translator
```

Trên Windows:

```powershell
.\gradlew.bat assembleDebug
```

Trên macOS/Linux:

```bash
./gradlew assembleDebug
```

APK debug được tạo tại:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Bạn cũng có thể mở thư mục gốc bằng Android Studio, chờ Gradle sync rồi chạy module `app` trên thiết bị thật. Các tính năng Bluetooth và microphone nên được kiểm thử trên thiết bị thật thay vì chỉ dùng emulator.

## Hoạt động on-device và Cloud legacy

Conversation và Walkie-Talkie không yêu cầu Google Cloud project, billing hoặc service-account key. Trước khi dịch offline, tải model ngôn ngữ trong **Settings → Translation models**. Model được ML Kit lưu trên thiết bị và có thể xóa trong cùng màn hình.

Android `SpeechRecognizer` ưu tiên recognizer on-device trên API/thiết bị hỗ trợ. Nếu chỉ có system recognizer, hệ thống có thể dùng mạng và có thể bỏ qua cờ ưu tiên offline; ứng dụng không tự chuyển sang Google Cloud khi xảy ra lỗi.

Màn hình **Legacy Cloud (advanced)** chỉ giữ khả năng import/xóa credential của implementation cũ phục vụ migration. Import credential không thay đổi runtime mặc định và không kích hoạt Cloud fallback. Credential được validate, mã hóa bằng Android Keystore, loại khỏi Auto Backup và có thể xóa trong ứng dụng. Không commit service-account JSON, API key hoặc thông tin thanh toán vào repository.

## Quyền Android chính

Ứng dụng dùng microphone cho nhận dạng giọng nói, quyền Bluetooth/Nearby Devices cho kết nối peer, và mạng để tải/cập nhật model hoặc khi system recognizer cần mạng. Credential legacy tùy chọn được chọn qua Storage Access Framework nên không cần quyền quét bộ nhớ rộng. Android cũ có thể yêu cầu location theo mô hình permission Bluetooth của nền tảng; ứng dụng không đọc hoặc lưu tọa độ GPS.

## Ngôn ngữ hỗ trợ

Mã nguồn hiện liệt kê các ngôn ngữ chính sau (không tính biến thể vùng): Bengali, Czech, Chinese, Korean, Danish, Finnish, French, Japanese, Greek, Hindi, Indonesian, English, Italian, Khmer, Nepali, Dutch, Polish, Portuguese, Romanian, Russian, Sinhala, Slovak, Spanish, Sundanese, Swedish, German, Thai, Turkish, Ukrainian, Hungarian và Vietnamese.

Khả dụng thực tế phụ thuộc vào recognizer, ngôn ngữ speech/TTS và model ML Kit trên từng thiết bị. Danh sách ngôn ngữ dịch không được dùng để khẳng định sai rằng mọi ngôn ngữ đều được speech recognizer hỗ trợ.

## Hạn chế đã biết

- Một số dependency legacy (Room, gRPC, Google auth/HTTP) vẫn cần được nâng cấp theo từng bước.
- Automated tests bao phủ các engine và lifecycle chính, nhưng chưa thay thế kiểm thử giọng nói/Bluetooth trên hai điện thoại thật.
- Một số thiết bị có thể gặp lỗi tìm kiếm hoặc kết nối Bluetooth LE.
- System speech recognizer có thể cần mạng; `EXTRA_PREFER_OFFLINE` chỉ là preference của nền tảng.
- Text-to-Speech không hoạt động đồng đều với mọi ngôn ngữ/engine.
- Conversation mode gửi payload qua lớp giao tiếp Bluetooth legacy; cần đánh giá và gia cố mã hóa trước khi dùng cho dữ liệu nhạy cảm.
- Credential Cloud legacy phía client không được dùng làm default runtime; một Cloud mode mới cần kiến trúc backend hoặc thiết kế riêng.

## Hướng phát triển đề xuất

- Hoàn tất physical-device matrix, Bluetooth/SCO, TalkBack và signed-release gate cho `1.3.0`.
- Cập nhật gRPC/Room/Google auth và các dependency legacy theo từng commit có regression gate.
- Mở rộng coverage và bổ sung CI sau khi release gate cục bộ ổn định.
- Tách rõ domain dịch, tầng dữ liệu và Android service để dễ kiểm thử.
- Đổi package/application ID và branding trong mã nguồn nếu fork được phát hành độc lập.

## Cấu trúc repository

```text
.
├── app/                         # Module Android chính
│   ├── src/main/java/           # Mã nguồn Java
│   ├── src/main/res/            # Layout, chuỗi, icon và tài nguyên
│   └── build.gradle             # Cấu hình module/version ứng dụng
├── gradle/wrapper/              # Gradle Wrapper
├── images/                      # Ảnh dùng trong tài liệu
├── privacy/                     # Chính sách quyền riêng tư gốc
├── build.gradle                 # Cấu hình build cấp project
└── settings.gradle
```

## Đóng góp

Issue và pull request được chào đón tại [danghoangsqtt-sys/Mini_app_translator](https://github.com/danghoangsqtt-sys/Mini_app_translator).

Khi đóng góp, vui lòng:

1. Không đưa credential hoặc dữ liệu cá nhân vào commit.
2. Mô tả thiết bị và phiên bản Android khi báo lỗi.
3. Kiểm thử cả Conversation mode và Walkie-Talkie mode nếu thay đổi luồng âm thanh/dịch.
4. Giữ nguyên các thông báo bản quyền và giấy phép của mã nguồn upstream.

## Nguồn gốc và giấy phép

Repository này được phát triển từ dự án mã nguồn mở [niedev/RTranslator](https://github.com/niedev/RTranslator) của Luca Martino. Tên repository và tài liệu đã được điều chỉnh cho fork này; package Java giữ nguyên để bảo toàn tương thích nâng cấp và nguồn gốc upstream.

Mã nguồn được phân phối theo [Apache License 2.0](LICENSE.txt). Xem thêm [NOTICE.txt](NOTICE.txt), [chính sách quyền riêng tư tiếng Anh](privacy/Privacy_Policy_en.md) và [chính sách quyền riêng tư tiếng Ý](privacy/Privacy_Policy_it.md).
