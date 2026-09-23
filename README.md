# Mini Conversation

Ứng dụng dịch giọng nói thời gian thực dành cho Android, hỗ trợ hội thoại giữa hai thiết bị và chế độ bộ đàm trên một thiết bị.

> [!IMPORTANT]
> Tên repository là **Mini App Translator**, nhưng ứng dụng đã được đổi tên hiển thị thành **Mini Conversation** (Phase 5); mã nguồn là ứng dụng Android native viết bằng Java, không phải mini app chạy trong trình duyệt. Dự án là một fork đang được hiện đại hóa từ [RTranslator](https://github.com/niedev/RTranslator).

## Tính năng

- **Conversation mode**: kết nối hai hoặc nhiều thiết bị qua Bluetooth để trao đổi hội thoại đã dịch.
- **Walkie-Talkie mode**: hai người dùng chung một điện thoại và nói luân phiên bằng hai ngôn ngữ.
- Nhận dạng giọng nói bằng Google Cloud Speech-to-Text.
- Dịch văn bản bằng Google Cloud Translation API v2.
- Phát lại nội dung đã dịch bằng Text-to-Speech của Android.
- Hỗ trợ tai nghe Bluetooth trong Conversation mode.
- Lưu thiết bị gần đây và thống kê mức sử dụng API bằng Room.
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

Mã nguồn đang sử dụng toolchain và thư viện legacy. Bản hiện tại phù hợp cho mục đích nghiên cứu, bảo trì hoặc làm nền cho quá trình nâng cấp; chưa nên phát hành như một ứng dụng production mới nếu chưa xử lý các hạng mục bảo mật và tương thích Android hiện đại.

## Kiến trúc tổng quan

```text
Microphone
    │
    ▼
Google Cloud Speech-to-Text
    │
    ▼
Google Cloud Translation API
    │
    ├──► Android Text-to-Speech
    │
    └──► Bluetooth peer (Conversation mode)
```

Các package chính:

- `voice_translation/cloud_apis`: nhận dạng giọng nói và dịch.
- `voice_translation/_conversation_mode`: ghép nối và hội thoại qua Bluetooth.
- `voice_translation/_walkie_talkie_mode`: nhận dạng luân phiên hai ngôn ngữ.
- `api_management`: quản lý khóa Google Cloud và thống kê chi phí.
- `database`: Room database cho lịch sử thiết bị và mức sử dụng.
- `tools`: tiện ích âm thanh, Bluetooth, mã hóa và giao diện.

## Yêu cầu

- Android Studio có thể mở dự án dùng Android Gradle Plugin 8.13.2.
- JDK 17 được yêu cầu cho toolchain Gradle 8.13 hiện tại.
- Android SDK Platform 36.
- Thiết bị Android 6.0 trở lên có microphone.
- Bluetooth Low Energy cho Conversation mode.
- Một Google Cloud project đã bật:
  - Cloud Speech-to-Text API
  - Cloud Translation API

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

## Cấu hình Google Cloud

1. Tạo một project trong [Google Cloud Console](https://console.cloud.google.com/).
2. Bật Cloud Speech-to-Text API và Cloud Translation API.
3. Tạo service account, chỉ cấp các quyền tối thiểu cần thiết.
4. Tạo khóa JSON cho service account và tải tệp về thiết bị thử nghiệm.
5. Trong ứng dụng, mở menu **APIs Management** và chọn tệp JSON.
6. Theo dõi quota và chi phí trong Google Cloud Console. Xem bảng giá hiện hành của [Speech-to-Text](https://cloud.google.com/speech-to-text/pricing) và [Cloud Translation](https://cloud.google.com/translate/pricing).

> [!CAUTION]
> Không commit tệp JSON, API key hoặc thông tin thanh toán vào repository. Phiên bản legacy hiện lưu thông tin xác thực trên thiết bị theo cơ chế chưa phù hợp với yêu cầu bảo mật production hiện đại. Chỉ dùng project thử nghiệm có quota/budget giới hạn cho đến khi phần lưu trữ khóa và backup được gia cố.

## Quyền Android chính

Ứng dụng yêu cầu quyền truy cập mạng, microphone, Bluetooth, vị trí và bộ nhớ để hỗ trợ nhận dạng giọng nói, tìm thiết bị lân cận và chọn tệp khóa. Một số quyền/cơ chế lưu trữ trong manifest thuộc mô hình Android cũ và cần được chuyển sang API hiện hành trước khi phát hành lên Google Play.

## Ngôn ngữ hỗ trợ

Mã nguồn hiện liệt kê các ngôn ngữ chính sau (không tính biến thể vùng): Bengali, Czech, Chinese, Korean, Danish, Finnish, French, Japanese, Greek, Hindi, Indonesian, English, Italian, Khmer, Nepali, Dutch, Polish, Portuguese, Romanian, Russian, Sinhala, Slovak, Spanish, Sundanese, Swedish, German, Thai, Turkish, Ukrainian, Hungarian và Vietnamese.

Khả dụng thực tế còn phụ thuộc vào Google Cloud Speech-to-Text, Cloud Translation và engine Text-to-Speech trên thiết bị.

## Hạn chế đã biết

- Toolchain, SDK đích và nhiều dependency đã cũ.
- Test hiện chỉ gồm các test mẫu mặc định; chưa có test bao phủ luồng dịch và Bluetooth.
- Một số thiết bị có thể gặp lỗi tìm kiếm hoặc kết nối Bluetooth LE.
- Text-to-Speech không hoạt động đồng đều với mọi ngôn ngữ/engine.
- Conversation mode gửi payload qua lớp giao tiếp Bluetooth legacy; cần đánh giá và gia cố mã hóa trước khi dùng cho dữ liệu nhạy cảm.
- Mô hình service-account key phía client cần được thiết kế lại trước khi phát hành production.

## Hướng phát triển đề xuất

- Nâng cấp Gradle, Android Gradle Plugin, compile SDK và target SDK.
- Chuyển quyền Bluetooth/bộ nhớ sang mô hình permission và scoped storage mới.
- Bảo vệ credential bằng Android Keystore và loại dữ liệu nhạy cảm khỏi Auto Backup.
- Cập nhật Google Cloud/gRPC/Room và các dependency cũ.
- Bổ sung unit test, instrumentation test và CI.
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

Repository này được phát triển từ dự án mã nguồn mở [niedev/RTranslator](https://github.com/niedev/RTranslator) của Luca Martino. Tên repository và tài liệu đã được điều chỉnh cho fork này; package Java và một số branding trong ứng dụng vẫn giữ nguyên từ upstream.

Mã nguồn được phân phối theo [Apache License 2.0](LICENSE.txt). Xem thêm [NOTICE.txt](NOTICE.txt), [chính sách quyền riêng tư tiếng Anh](privacy/Privacy_Policy_en.md) và [chính sách quyền riêng tư tiếng Ý](privacy/Privacy_Policy_it.md).
