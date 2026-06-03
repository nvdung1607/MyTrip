# MyTrip — Ứng Dụng Quản Lý Chuyến Du Lịch (Android)

## Tổng Quan

Ứng dụng Android native (Kotlin) giúp lên kế hoạch, theo dõi và tổng kết chuyến du lịch. Dữ liệu lưu hoàn toàn local bằng SQLite (Room). Ngôn ngữ giao diện: **Tiếng Việt**. Phân phối: file APK cài tay.

---

## Kiến Trúc Kỹ Thuật

- **Ngôn ngữ**: Kotlin
- **UI**: Jetpack Compose (Material 3)
- **Database**: Room (SQLite)
- **Navigation**: Jetpack Navigation Component
- **Ảnh**: CameraX + Coil
- **Excel**: Apache POI (đọc/ghi .xlsx)
- **PDF export**: iText / PdfDocument
- **Chia sẻ**: Android Share Sheet (Intent) → Zalo, Messenger, Facebook tự nhận
- **Thông báo**: AlarmManager + NotificationManager
- **Architecture**: MVVM + Repository pattern

---

## Cấu Trúc Màn Hình & Tính Năng

### 🏠 Màn hình chính
- Danh sách các chuyến đi (card đẹp, ảnh cover, số ngày, loại chuyến)
- Nút tạo chuyến mới (FAB)
- Trạng thái chuyến: **Sắp đi / Đang đi / Đã kết thúc**

---

### 📋 Module 1 — Lên Kế Hoạch

#### Tạo / Chỉnh sửa chuyến đi
- Tên chuyến, mô tả
- Loại hình: Ô tô tự lái, Phượt xe máy, Công cộng (máy bay/xe/tàu), Treking, Cắm trại, ...
- Ngày bắt đầu - kết thúc (tự tính số ngày)
- Ảnh cover chuyến đi
- Số người tham gia

#### Cấu trúc lịch trình (linh hoạt)
- **Chuyến ngắn** (≤ 3 ngày): hiển thị flat, không chia cụm
- **Chuyến dài** (> 3 ngày): tùy chọn chia cụm (mỗi cụm = vài ngày, VD: "Cụm 1: Hà Nội → Đà Nẵng")
- Mỗi **cụm** có thể thu gọn/mở rộng
- Mỗi **ngày** trong cụm có thể thu gọn/mở rộng

#### Thông tin mỗi ngày trong lịch trình
| Trường | Loại |
|--------|-------|
| Tên điểm đến / địa điểm | Text |
| Thời gian xuất phát → đến nơi | TimePicker |
| Khoảng cách (km) | Số |
| Tên khách sạn / nơi nghỉ | Text |
| Giá phòng (dự kiến) | Số tiền |
| Điểm check-in nổi tiếng cần ghé | Text (danh sách) |
| Link Google Maps | URL |
| Ghi chú thêm | Text |

#### Dự kiến chi phí (per chuyến đi)
- Tiền phòng
- Tiền ăn uống
- Di chuyển (xăng, cầu đường, vé xe/tàu/máy bay)
- Vé tham quan
- Quà cáp / Mua sắm
- Phát sinh
- **Chia đầu người**: tổng ÷ số người → hiển thị chi phí mỗi người

#### Import / Export
- **Import từ Excel (.xlsx)**: tải file mẫu trong app → điền vào → import vào app
- **Export ra Excel (.xlsx)**: xuất toàn bộ lịch trình ra file đẹp
- **Chia sẻ**: Share Sheet Android → Zalo / Messenger / Facebook / Email / lưu máy

---

### 🗺️ Module 2 — Trong Chuyến Đi

#### Theo dõi lịch trình
- Tab "Hôm nay" → lịch trình ngày hiện tại (highlight)
- Xem nhanh "Hôm qua" / "Ngày mai"
- Xem toàn bộ lịch trình (timeline dạng dọc)
- So sánh **Kế hoạch vs Thực tế** (2 cột song song)

#### Ghi nhận thực tế
- Đánh dấu từng mục trong lịch trình: ✅ Đã làm / ⏭️ Bỏ qua / 🔀 Thay đổi
- Nhập thực tế khác kế hoạch (ghi chú thay đổi)

#### Hệ thống Note / Nhật ký chuyến đi
Quy trình tạo note (theo mô tả của mày):
1. Nhấn "+" → **Tự mở camera** → chụp ảnh (hoặc chọn từ gallery)
2. Sau khi chụp → form hiện ra với các **trường bắt buộc**:
   - ⭐ Đánh giá (1-5 sao)
   - 🏷️ Tag loại (Khách sạn / Quán ăn / Điểm tham quan / Cửa hàng / Khác)
   - 💰 Chi phí thực tế (tiền đã trả)
3. Trường **mở rộng** (bấm "Thêm" để hiện):
   - 📝 Tên địa điểm / tên món ăn
   - 💬 Nhận xét chi tiết
   - 📍 GPS location (tự động gắn)
   - 🕐 Timestamp (tự động gắn)
4. Lưu → Note gắn vào ngày đang xem

#### Nhắc nhở thông báo
- Mỗi mục lịch trình có thời gian → cài nhắc nhở
- Tùy chọn: nhắc trước **15 / 30 / 60 phút**
- Thông báo hiện ra: "15 phút nữa: Xuất phát đến Đà Nẵng (5h50)"
- Hoạt động offline, dùng AlarmManager

---

### 📊 Module 3 — Sau Chuyến Đi

#### Tổng kết hành trình
- Timeline đầy đủ với ảnh và note thực tế
- Bản đồ hành trình (list các địa điểm đã ghé theo thứ tự)
- Tổng số km đã đi
- Đánh giá tổng thể chuyến đi (sao)

#### Tổng kết chi phí
- Bảng so sánh: **Dự kiến vs Thực tế** cho từng mục
- Tổng chi phí theo đầu người
- Biểu đồ tròn tỉ lệ từng hạng mục
- Export báo cáo chi phí ra Excel

#### Tạo album / tổng kết (nice-to-have)
- Tập hợp ảnh nổi bật đã note trong chuyến
- Tạo PDF tổng kết có ảnh + lộ trình
- Chia sẻ lên Zalo/Messenger

---

## Quyết Định Đã Confirmed

> [!NOTE]
> **Chi phí nhóm**: Track riêng từng người trả gì → tổng hợp → chia đầu người. Ví dụ: Anh A trả tiền xăng 200k, anh B trả tiền phòng 500k → tổng 700k ÷ 2 người = 350k/người, anh A được hoàn 150k, anh B được hoàn 150k.

> [!NOTE]
> **Tiền tệ**: Chỉ VNĐ. Hỗ trợ nhập nhanh: loại bỏ 3 số 0 cuối. Ví dụ nhập "500" = 500.000 VND, hiển thị "500k". Có nút shortcuts: 50k / 100k / 200k / 500k.

---

## Lộ Trình Thực Hiện

### Phase 1 — Core Foundation (Ưu tiên cao nhất)
- [x] Setup project Kotlin + Jetpack Compose + Room
- [ ] Database schema (Trip, Cluster, Day, Activity, Note, Expense)
- [ ] Màn hình danh sách chuyến đi
- [ ] Tạo / sửa / xóa chuyến đi
- [ ] Lịch trình: cụm + ngày + hoạt động (CRUD)
- [ ] Dự kiến chi phí

### Phase 2 — Tính năng trong chuyến đi
- [ ] Màn hình "Hôm nay" / Timeline
- [ ] Note với camera, rating, tag, chi phí
- [ ] Ghi nhận thực tế vs kế hoạch
- [ ] Nhắc nhở AlarmManager

### Phase 3 — Tổng kết & Chia sẻ
- [ ] Màn hình tổng kết chi phí
- [ ] Import/Export Excel (Apache POI)
- [ ] Export PDF
- [ ] Chia sẻ qua Share Sheet

### Phase 4 — Nice-to-have
- [ ] Tạo album ảnh tổng kết
- [ ] Biểu đồ chi phí

---

## Cấu Trúc Database (Room)

```
Trip: id, name, description, type, startDate, endDate, coverImage, numPeople, status
Cluster: id, tripId, name, orderIndex
Day: id, clusterId (nullable), tripId, dayNumber, date, notes
Activity: id, dayId, name, departureTime, arrivalTime, distanceKm, hotelName, hotelPrice, checkInSpots (JSON), mapsLink, notes, actualDepartureTime, status
Note: id, tripId, dayId (nullable), photoPath, rating, tag, cost, name, comment, gpsLat, gpsLng, timestamp
Expense: id, tripId, category, planned, actual
```
