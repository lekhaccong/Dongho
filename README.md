# Đồng hồ – Android

Bản Android dựng lại từ gói iOS `Lecong.ipa`, sử dụng Java và Android SDK thuần để dễ build, ít phụ thuộc.

## Chức năng

- Giờ quốc tế: Hà Nội, London, Tokyo, New York; cập nhật mỗi giây.
- Báo thức: thêm, bật/tắt, xóa, lưu trên máy và phát thông báo.
- Giờ đi ngủ: đặt giờ ngủ/thức và bật nhắc lịch hằng ngày.
- Bấm giờ: bắt đầu, dừng, đặt lại và lưu các vòng.
- Hẹn giờ: chọn giờ/phút/giây, bắt đầu, tạm dừng, đặt lại và thông báo khi hết giờ.

## Build

Mở thư mục bằng Android Studio (JDK 17), chờ đồng bộ rồi chọn **Build > Build APK(s)**.

Hoặc chạy:

```bash
./gradlew assembleDebug
```

APK nằm tại `app/build/outputs/apk/debug/app-debug.apk`.
