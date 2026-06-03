package com.example.mytrip.data.seed

import com.example.mytrip.data.db.entities.*

/**
 * Dữ liệu mẫu: Hành trình Xuyên Việt 30 ngày
 * Thanh Sơn (Phú Thọ) → TP.HCM → Thanh Sơn (Phú Thọ)
 * Xe: Mitsubishi Xforce | Dự kiến: 5000km
 */
object TripSeedData {

    /** Ngày bắt đầu placeholder (người dùng chỉnh lại trong app) */
    private val START_DATE_MS = 1_753_920_000_000L // 01/08/2026 00:00 UTC+7

    private fun dayMs(dayOffset: Int) = START_DATE_MS + dayOffset * 86_400_000L

    // ─── Trip ──────────────────────────────────────────────────────────────────
    val trip = Trip(
        id = 0,
        name = "Xuyên Việt 2026 – Mitsubishi Xforce",
        description = "Hành trình tự lái 30 ngày từ Thanh Sơn (Phú Thọ) vào TP.HCM, vòng qua miền Tây, Tây Nguyên và trở về. Quãng đường dự kiến 5.000 km.",
        type = TripType.CAR,
        startDate = dayMs(0),
        endDate = dayMs(29),
        numPeople = 3,
        memberNames = "[\"Mày\",\"Bạn A\",\"Bạn B\"]",
        status = TripStatus.PLANNING,
        createdAt = System.currentTimeMillis()
    )

    // ─── Days ──────────────────────────────────────────────────────────────────
    data class DaySeed(
        val dayNumber: Int,
        val title: String,
        val notes: String = "",
        val activities: List<ActivitySeed>
    )

    data class ActivitySeed(
        val name: String,
        val departure: String = "",
        val arrival: String = "",
        val distanceKm: Double = 0.0,
        val hotelName: String = "",
        val hotelPriceK: Long = 0,
        val checkInSpots: String = "",
        val mapsLink: String = "",
        val actNotes: String = ""
    )

    val days: List<DaySeed> = listOf(

        // ── Ngày 1 ─────────────────────────────────────────────────────────────
        DaySeed(1, "Phú Thọ → Nghệ An (Vinh) | 380km",
            "Đường HCM vắng, chú ý sương mù sáng sớm và súc vật qua đường. Kiểm tra số dư VETC/ePass trước khi lên cao tốc.",
            listOf(
                ActivitySeed("Xuất phát từ Thanh Sơn", "05:30", "", 0.0, actNotes = "Khởi hành sớm tránh tắc đường"),
                ActivitySeed("Dừng ăn sáng – Xuân Mai / Miếu Môn", "07:45", "08:15", 90.0, actNotes = "30 phút nghỉ, đường HCM"),
                ActivitySeed("Dừng vệ sinh – Tam Điệp (Ninh Bình) + Nhập Cao tốc", "10:30", "10:45", 90.0),
                ActivitySeed("Dừng ăn trưa – Trạm dừng Thanh Hóa", "12:15", "13:15", 100.0, actNotes = "60 phút nghỉ trên cao tốc"),
                ActivitySeed("Tới TP. Vinh – Nhận phòng", "14:45", "", 100.0,
                    checkInSpots = "[\"Khu di tích Kim Liên (Quê Bác)\",\"Biển Cửa Lò\",\"Đảo Lan Châu\",\"Đền Cuông (Diễn Châu)\"]")
            )
        ),

        // ── Ngày 2 ─────────────────────────────────────────────────────────────
        DaySeed(2, "Vinh (Nghệ An) → Đà Nẵng | 470km",
            "Cao tốc Cam Lộ - La Sơn hẹp 2 làn, tuyệt đối tuân thủ tốc độ. Ưu tiên đi hầm Hải Vân.",
            listOf(
                ActivitySeed("Xuất phát từ TP. Vinh", "05:30"),
                ActivitySeed("Dừng ăn sáng – Kỳ Anh (Hà Tĩnh)", "07:45", "08:15", 90.0),
                ActivitySeed("Dừng nghỉ – Đồng Hới (Quảng Bình)", "10:30", "10:45", 100.0),
                ActivitySeed("Dừng ăn trưa – TP. Đông Hà (Quảng Trị) + Nhập Cao tốc Cam Lộ", "13:00", "14:00", 100.0),
                ActivitySeed("Dừng nghỉ – Trạm La Sơn - Túy Loan", "15:30", "15:45", 100.0),
                ActivitySeed("Tới TP. Đà Nẵng – Nhận phòng", "17:00", "", 80.0,
                    checkInSpots = "[\"Bãi biển Mỹ Khê\",\"Bán đảo Sơn Trà & Chùa Linh Ứng\",\"Cầu Rồng & Cầu Tình Yêu\",\"Ngũ Hành Sơn\"]")
            )
        ),

        // ── Ngày 3 ─────────────────────────────────────────────────────────────
        DaySeed(3, "Đà Nẵng – NGHỈ NGƠI THAM QUAN",
            "Nắng gắt và gió Lào: Bôi kem chống nắng, mặc áo khoác dày, mang theo nước.",
            listOf(
                ActivitySeed("Dậy sớm – Tắm biển, đi dạo", "06:00", "08:00", actNotes = "Tự do dọc bãi biển Mỹ Khê"),
                ActivitySeed("Ăn sáng, uống cà phê", "08:00", "09:30"),
                ActivitySeed("Bà Nà Hills", "10:00", "16:00", 30.0, actNotes = "Vé khoảng 900k/người. Đặt trước để tiết kiệm."),
                ActivitySeed("Ăn tối hải sản – Dạo phố đêm", "18:00", "21:00",
                    checkInSpots = "[\"Chợ Cồn / Chợ Hàn\",\"Công viên Châu Á (Asia Park)\",\"Phố cổ Hội An\"]")
            )
        ),

        // ── Ngày 4 ─────────────────────────────────────────────────────────────
        DaySeed(4, "Đà Nẵng → Quảng Ngãi | 130km",
            "Chặng ngắn, khởi hành muộn, ăn sáng thong thả. Ưu tiên cao tốc Đà Nẵng - Quảng Ngãi.",
            listOf(
                ActivitySeed("Xuất phát từ TP. Đà Nẵng – Nhập Cao tốc Đà Nẵng - QNgãi", "08:00"),
                ActivitySeed("Dừng vệ sinh – Trạm trên cao tốc", "09:30", "09:45", 90.0),
                ActivitySeed("Tới TP. Quảng Ngãi – Nhận phòng", "10:30", "", 40.0,
                    checkInSpots = "[\"Tượng đài Mẹ Thứ (Quảng Nam)\",\"Làng bích họa Tam Thanh\",\"Khu chứng tích Sơn Mỹ\",\"Biển Mỹ Khê (Quảng Ngãi)\"]")
            )
        ),

        // ── Ngày 5 ─────────────────────────────────────────────────────────────
        DaySeed(5, "Quảng Ngãi → Quy Nhơn (Bình Định) | 175km",
            "Chủ yếu đi QL1A, chú ý camera phạt nguội và biển báo tốc độ khu dân cư.",
            listOf(
                ActivitySeed("Xuất phát từ TP. Quảng Ngãi", "07:30"),
                ActivitySeed("Đồng muối Sa Huỳnh – Tham quan", "09:00", "10:00", 60.0, actNotes = "60 phút, miễn phí"),
                ActivitySeed("Dừng ăn trưa – TP. Quy Nhơn", "12:30", "13:30", 115.0),
                ActivitySeed("Nhận phòng khách sạn – Nghỉ ngơi", "13:30"),
                ActivitySeed("Tháp Đôi (Quy Nhơn)", "15:00", "15:45", actNotes = "45 phút, vé 20k"),
                ActivitySeed("Khu du lịch Ghềnh Ráng – Tiên Sa", "16:00", "17:15", actNotes = "60 phút, miễn phí. Mộ Hàn Mặc Tử 10k"),
                ActivitySeed("Tắm biển – Dạo phố – Ăn tối", "17:15")
            )
        ),

        // ── Ngày 6 ─────────────────────────────────────────────────────────────
        DaySeed(6, "Quy Nhơn – NGHỈ HOÀN TOÀN",
            "Ngày nghỉ ngơi tự do. Đường ra Eo Gió/Kỳ Co rộng, Xforce gầm cao chạy rất thoải mái.",
            listOf(
                ActivitySeed("Chạy bộ dọc bãi biển – Đón bình minh", "06:00", "08:00"),
                ActivitySeed("Ăn sáng đặc sản – Bún rạm/bún sứa", "08:00", "09:00"),
                ActivitySeed("Eo Gió – Hiking mỏm đá", "09:00", "11:00", actNotes = "Vé 25k, địa hình dốc tốn thể lực"),
                ActivitySeed("Kỳ Co – Tắm biển", "11:00", "12:30", actNotes = "Vé 150k đường bộ hoặc 250k cano"),
                ActivitySeed("Ăn trưa hải sản – Làng chài Nhơn Lý", "12:30", "14:00"),
                ActivitySeed("Surf Bar – Cà phê dọc bãi biển", "16:30", "18:30", actNotes = "Chi phí đồ uống tự túc")
            )
        ),

        // ── Ngày 7 ─────────────────────────────────────────────────────────────
        DaySeed(7, "Quy Nhơn → Nha Trang (Khánh Hòa) | 210km",
            "Đèo Cả độ dốc lớn, nhiều khúc cua, giữ khoảng cách an toàn với xe tải. Cung đường ven vịnh Vũng Rô rất đẹp.",
            listOf(
                ActivitySeed("Xuất phát từ TP. Quy Nhơn", "07:30"),
                ActivitySeed("Gành Đá Đĩa (Phú Yên)", "09:30", "10:30", 80.0, actNotes = "60 phút, vé 20k"),
                ActivitySeed("Dừng ăn trưa – Đầm Ô Loan / Tuy Hòa", "11:30", "12:30", 40.0),
                ActivitySeed("Đèo Cả – Đi qua cẩn thận (số thấp)", "12:30", "14:00", 60.0, actNotes = "Đoạn dốc dài, nhiều cua"),
                ActivitySeed("Tới TP. Nha Trang – Nhận phòng", "14:30"),
                ActivitySeed("Tháp Bà Ponagar", "16:00", "17:00", actNotes = "60 phút, vé 30k"),
                ActivitySeed("Tắm biển Trần Phú – Dạo phố – Ăn tối", "17:15",
                    checkInSpots = "[\"Gành Đá Đĩa\",\"Bãi Xép - Ghềnh Ông\",\"Hải đăng Đại Lãnh / Mũi Điện\"]")
            )
        ),

        // ── Ngày 8 ─────────────────────────────────────────────────────────────
        DaySeed(8, "Nha Trang – NGHỈ HOÀN TOÀN",
            "Ngày nghỉ, vui chơi tự do. Đường Trần Phú đông đúc giờ chiều tối.",
            listOf(
                ActivitySeed("Chạy bộ dọc biển Trần Phú – Đón bình minh", "05:30", "07:30"),
                ActivitySeed("Ăn sáng bún cá sứa đặc sản", "08:00", "09:00"),
                ActivitySeed("Viện Hải dương học hoặc Hòn Chồng", "09:00", "10:30", actNotes = "Vé 40k"),
                ActivitySeed("Ăn trưa nem nướng Nha Trang – Nghỉ trưa", "12:00", "15:00"),
                ActivitySeed("Tắm bùn khoáng (I-Resort hoặc Tháp Bà)", "15:00", "17:00", actNotes = "250k - 350k, thư giãn phục hồi cơ bắp"),
                ActivitySeed("Ăn tối hải sản – Dạo chợ đêm Nha Trang", "18:00")
            )
        ),

        // ── Ngày 9 ─────────────────────────────────────────────────────────────
        DaySeed(9, "Nha Trang → Mũi Né (Bình Thuận) | 220km",
            "Cung đường ven biển Bàu Trắng (ĐT716) rất ấn tượng – đồi cát trắng và biển xanh.",
            listOf(
                ActivitySeed("Xuất phát từ TP. Nha Trang", "07:30"),
                ActivitySeed("Tháp Po Klong Garai – TP. Phan Rang", "10:00", "11:00", 100.0, actNotes = "60 phút, vé 20k"),
                ActivitySeed("Dừng ăn trưa – Khu vực biển Cà Ná", "11:45", "12:45", 40.0),
                ActivitySeed("Cung đường ven biển Bàu Trắng (ĐT716) – Dừng chụp ảnh", "12:45", "14:00", 50.0),
                ActivitySeed("Tới Mũi Né – Nhận phòng", "14:15"),
                ActivitySeed("Đồi Cát Hồng (Đồi Cát Bay) – Trượt cát & Hoàng hôn", "16:00", "18:00", actNotes = "Miễn phí, thuê máng trượt ~20k"),
                ActivitySeed("Tắm biển – Ăn tối hải sản", "18:00")
            )
        ),

        // ── Ngày 10 ────────────────────────────────────────────────────────────
        DaySeed(10, "Mũi Né → Vũng Tàu | 160km",
            "Nên đến Suối Tiên sớm. Tượng Chúa Kitô gần 1.000 bậc thang, đi chiều mát.",
            listOf(
                ActivitySeed("Suối Tiên (Mũi Né) – Tham quan", "07:00", "08:00", actNotes = "60 phút, vé 15k"),
                ActivitySeed("Xuất phát từ Mũi Né", "08:00"),
                ActivitySeed("Hải đăng Kê Gà (Bình Thuận)", "09:30", "10:30", 45.0, actNotes = "60 phút, ~50k cano"),
                ActivitySeed("Tới TP. Vũng Tàu – Dừng ăn trưa", "12:45", "13:45", 115.0),
                ActivitySeed("Nhận phòng – Nghỉ ngơi", "14:00"),
                ActivitySeed("Tượng Chúa Kitô (Tượng Chúa Dang Tay)", "15:30", "16:30", actNotes = "Miễn phí, gần 1.000 bậc thang"),
                ActivitySeed("Tắm biển Bãi Sau – Dạo phố – Ăn tối", "17:00")
            )
        ),

        // ── Ngày 11 ────────────────────────────────────────────────────────────
        DaySeed(11, "Vũng Tàu → TP. Hồ Chí Minh | 100km",
            "QL51 đông xe tải, giữ khoảng cách. Trung tâm TP.HCM kẹt giờ tan tầm 16h30-18h30.",
            listOf(
                ActivitySeed("Xuất phát từ TP. Vũng Tàu", "07:30"),
                ActivitySeed("Dừng vệ sinh – Trên QL51", "08:45", "09:00", 50.0),
                ActivitySeed("Tới TP.HCM – Gửi đồ nghỉ ngơi", "10:15", "", 50.0),
                ActivitySeed("Dừng ăn trưa", "11:30", "12:30"),
                ActivitySeed("Nhận phòng khách sạn", "13:30"),
                ActivitySeed("Dinh Độc Lập", "15:00", "16:30", actNotes = "90 phút, vé 65k"),
                ActivitySeed("Nhà thờ Đức Bà & Bưu điện TP", "16:30", "17:15", actNotes = "Miễn phí"),
                ActivitySeed("Phố đi bộ Nguyễn Huệ – Ăn tối", "18:00")
            )
        ),

        // ── Ngày 12 ────────────────────────────────────────────────────────────
        DaySeed(12, "TP. Hồ Chí Minh – NGHỈ NGƠI",
            "Bảo dưỡng xe bắt buộc. Dùng xe ôm công nghệ di chuyển trong ngày.",
            listOf(
                ActivitySeed("Bảo dưỡng xe tổng thể – Thay nhớt, kiểm tra phanh/lốp", "08:00", "10:30", actNotes = "Nhiệm vụ bắt buộc trước hành trình miền Tây & Tây Nguyên"),
                ActivitySeed("Uống cà phê vợt", "10:30", "12:00"),
                ActivitySeed("Ăn trưa cơm tấm", "12:00", "13:30"),
                ActivitySeed("Tham quan Chợ Bến Thành", "15:00", "16:00", actNotes = "Miễn phí"),
                ActivitySeed("Tự do dạo Quận 1 – Ăn tối", "18:00")
            )
        ),

        // ── Ngày 13 ────────────────────────────────────────────────────────────
        DaySeed(13, "TP.HCM → Củ Chi → Tây Ninh | 100km",
            "QL22 đông xe máy, giữ tốc độ chậm.",
            listOf(
                ActivitySeed("Xuất phát từ TP.HCM", "07:30"),
                ActivitySeed("Địa đạo Củ Chi – Tham quan", "08:30", "10:30", 40.0, actNotes = "120 phút, vé ~35k"),
                ActivitySeed("Tiếp tục đi Tây Ninh", "10:30"),
                ActivitySeed("Dừng ăn trưa – Tây Ninh", "12:00", "13:30", 60.0),
                ActivitySeed("Nhận phòng khách sạn", "13:30"),
                ActivitySeed("KDL Núi Bà Đen – Lên cáp treo", "15:00", "18:00", actNotes = "400k tuyến đỉnh hoặc 600k combo đỉnh + chùa")
            )
        ),

        // ── Ngày 14 ────────────────────────────────────────────────────────────
        DaySeed(14, "Tây Ninh → Cần Thơ | 210km",
            "Sử dụng trọn chuỗi cao tốc để nhàn tay lái.",
            listOf(
                ActivitySeed("Xuất phát từ Tây Ninh", "07:30"),
                ActivitySeed("Nghỉ ngơi – Nhập Cao tốc TP.HCM - Trung Lương", "09:30", "09:45", 80.0),
                ActivitySeed("Dừng ăn trưa – Trạm dừng Tiền Giang", "11:15", "12:15", 60.0),
                ActivitySeed("Tới TP. Cần Thơ – Nhận phòng", "14:00", "", 70.0),
                ActivitySeed("Nhà cổ Bình Thủy", "16:00", "16:45", actNotes = "45 phút, vé 15k"),
                ActivitySeed("Dạo Bến Ninh Kiều – Ăn tối", "18:00")
            )
        ),

        // ── Ngày 15 ────────────────────────────────────────────────────────────
        DaySeed(15, "Cần Thơ – NGHỈ HOÀN TOÀN",
            "Ngày xả cơ, không di chuyển xa.",
            listOf(
                ActivitySeed("Thuê thuyền đi Chợ nổi Cái Răng", "06:00", "09:00", actNotes = "~150k-300k thuê tàu/thuyền"),
                ActivitySeed("Ăn sáng – Uống cà phê", "09:00", "11:30"),
                ActivitySeed("Ăn trưa lẩu mắm đặc sản", "11:30", "13:00"),
                ActivitySeed("Nghỉ ngơi tại phòng – Phục hồi thể lực", "13:00", "17:00"),
                ActivitySeed("Dạo Bến Ninh Kiều – Ăn tối", "18:00")
            )
        ),

        // ── Ngày 16 ────────────────────────────────────────────────────────────
        DaySeed(16, "Cần Thơ → TP. Cà Mau | 150km",
            "QL1A đoạn này đông đúc, giữ tốc độ đều.",
            listOf(
                ActivitySeed("Xuất phát từ Cần Thơ", "08:30"),
                ActivitySeed("Dừng nghỉ ngơi – Sóc Trăng", "10:30", "10:45", 75.0),
                ActivitySeed("Tới TP. Cà Mau – Dừng ăn trưa", "12:30", "13:30", 75.0),
                ActivitySeed("Nhận phòng – Nghỉ ngơi", "13:30"),
                ActivitySeed("Tham quan Quảng trường trung tâm Cà Mau", "16:00", "17:00", actNotes = "Miễn phí"),
                ActivitySeed("Ăn tối", "18:00")
            )
        ),

        // ── Ngày 17 ────────────────────────────────────────────────────────────
        DaySeed(17, "TP. Cà Mau → Đất Mũi → Cà Mau | 220km (khứ hồi)",
            "Đường về Đất Mũi nhiều mố cầu, rà phanh đi chậm tránh kịch gầm.",
            listOf(
                ActivitySeed("Xuất phát từ Cà Mau", "07:30"),
                ActivitySeed("Vườn QG Mũi Cà Mau & Cột mốc GPS 0001", "10:15", "12:15", 110.0, actNotes = "~30k, điểm cực Nam Tổ quốc"),
                ActivitySeed("Ăn trưa hải sản tại Đất Mũi", "12:00", "13:30"),
                ActivitySeed("Lên xe quay về Cà Mau", "13:30"),
                ActivitySeed("Về tới khách sạn – Nghỉ ngơi", "16:15", "", 110.0)
            )
        ),

        // ── Ngày 18 ────────────────────────────────────────────────────────────
        DaySeed(18, "Cà Mau → Bạc Liêu → Sóc Trăng | 120km",
            "Chặng ngắn, nhàn nhã.",
            listOf(
                ActivitySeed("Xuất phát từ Cà Mau", "08:00"),
                ActivitySeed("Nhà Công tử Bạc Liêu – Tham quan", "09:45", "10:45", 70.0, actNotes = "60 phút, vé 30k"),
                ActivitySeed("Dừng ăn trưa tại Bạc Liêu", "11:30", "12:30"),
                ActivitySeed("Tới TP. Sóc Trăng – Nhận phòng", "13:45", "", 50.0),
                ActivitySeed("Chùa Dơi / Chùa Som Rong", "15:30", "16:30", actNotes = "60 phút, miễn phí"),
                ActivitySeed("Ăn tối bún nước lèo đặc sản", "18:00")
            )
        ),

        // ── Ngày 19 ────────────────────────────────────────────────────────────
        DaySeed(19, "Sóc Trăng → Bình Dương (Thủ Dầu Một) | 240km",
            "Đi hoàn toàn bằng cao tốc để thoát nhanh miền Tây và khu vực TP.HCM.",
            listOf(
                ActivitySeed("Xuất phát từ Sóc Trăng – Hướng về Cần Thơ nhập cao tốc CT01", "07:30"),
                ActivitySeed("Dừng nghỉ – Trạm dừng Tiền Giang", "10:30", "10:45", 60.0),
                ActivitySeed("Tiếp tục trên chuỗi cao tốc xuyên TP.HCM → Bình Dương", "10:45"),
                ActivitySeed("Tới Bình Dương – Nhận phòng gần KDL Đại Nam", "14:30", "", 180.0),
                ActivitySeed("Ăn tối đặc sản Bình Dương – Bánh bèo Mỹ Liên / Gỏi gà măng cụt", "18:00")
            )
        ),

        // ── Ngày 20 ────────────────────────────────────────────────────────────
        DaySeed(20, "Bình Dương → KDL Đại Nam → Bảo Lộc | 150km",
            "Chia đôi ngày vừa chơi Đại Nam vừa di chuyển lên Bảo Lộc tránh kiệt sức.",
            listOf(
                ActivitySeed("Trả phòng – Vào KDL Đại Nam", "08:00"),
                ActivitySeed("Khu du lịch Đại Nam – Vui chơi", "08:00", "12:00", actNotes = "Vé combo ~400k/người (Vườn thú + Biển nhân tạo)"),
                ActivitySeed("Ăn trưa – Khu vực Đại Nam", "12:00", "13:00"),
                ActivitySeed("Xuất phát đi Bảo Lộc dọc QL1A + QL20", "13:00"),
                ActivitySeed("Tới TP. Bảo Lộc – Nhận phòng", "17:00", "", 150.0, actNotes = "Nghỉ ngơi chuẩn bị leo cao nguyên ngày mai")
            )
        ),

        // ── Ngày 21 ────────────────────────────────────────────────────────────
        DaySeed(21, "Bảo Lộc → Đà Lạt | 110km",
            "Chặng lái ngắn, nửa ngày nghỉ ngơi thích nghi khí hậu lạnh.",
            listOf(
                ActivitySeed("Xuất phát từ Bảo Lộc", "08:30"),
                ActivitySeed("Thác Pongour – Tham quan", "10:30", "11:30", 80.0, actNotes = "Thác nước đẹp trên QL20"),
                ActivitySeed("Tới Đà Lạt – Ăn trưa – Nhận phòng", "12:00", "14:00", 30.0),
                ActivitySeed("Nghỉ ngơi tự do tại phòng", "14:00", "17:00"),
                ActivitySeed("Dạo Hồ Xuân Hương – Ăn tối", "18:00")
            )
        ),

        // ── Ngày 22 ────────────────────────────────────────────────────────────
        DaySeed(22, "Đà Lạt – NGHỈ HOÀN TOÀN",
            "Ngày xả cơ trọn vẹn, nạp lại năng lượng trước chuỗi đèo Tây Nguyên từ ngày 23.",
            listOf(
                ActivitySeed("Dậy muộn – Ăn sáng bánh mì xíu mại", "08:30", "10:00"),
                ActivitySeed("Ga Đà Lạt – Tham quan", "10:00", "10:45", actNotes = "5k"),
                ActivitySeed("Dinh Bảo Đại – Tham quan", "11:00", "12:00", actNotes = "60 phút, vé 40k"),
                ActivitySeed("Ăn trưa và nghỉ ngơi", "12:00", "15:00"),
                ActivitySeed("Cà phê thung lũng", "15:00", "17:00", actNotes = "Ngắm view Đà Lạt tuyệt đẹp"),
                ActivitySeed("Lẩu bò Ba Toa – Dạo chợ đêm", "18:00")
            )
        ),

        // ── Ngày 23 ────────────────────────────────────────────────────────────
        DaySeed(23, "Đà Lạt → Buôn Ma Thuột | 210km",
            "QL27 hẹp, nhiều dốc quanh co. Chạy từ tốn, ưu tiên an toàn.",
            listOf(
                ActivitySeed("Xuất phát từ Đà Lạt dọc QL27", "07:30"),
                ActivitySeed("Dừng nghỉ chân đèo – Kiểm tra xe", "09:30", "09:45", 80.0),
                ActivitySeed("Hồ Lắk – Ăn trưa & Tham quan", "11:30", "13:00", 70.0, actNotes = "60 phút, miễn phí"),
                ActivitySeed("Tới Buôn Ma Thuột – Nhận phòng", "14:00", "", 60.0),
                ActivitySeed("Dạo trung tâm – Uống cà phê Ban Mê", "16:00", "19:00")
            )
        ),

        // ── Ngày 24 ────────────────────────────────────────────────────────────
        DaySeed(24, "Buôn Ma Thuột → Pleiku (Gia Lai) | 180km",
            "QL14 mặt đường đẹp nhưng cực kỳ nhiều camera phạt nguội.",
            listOf(
                ActivitySeed("Bảo tàng Thế giới Cà phê – Tham quan", "08:30", "10:00", actNotes = "90 phút, vé 150k"),
                ActivitySeed("Xuất phát đi Pleiku dọc QL14", "10:00"),
                ActivitySeed("Dừng ăn trưa dọc đường", "12:15", "13:15", 90.0),
                ActivitySeed("Tới Pleiku – Nhận phòng", "14:30", "", 90.0),
                ActivitySeed("Biển Hồ Tơ Nưng – Tham quan", "16:00", "17:00", actNotes = "60 phút, vé 10k"),
                ActivitySeed("Ăn tối phở khô Gia Lai đặc sản", "18:00")
            )
        ),

        // ── Ngày 25 ────────────────────────────────────────────────────────────
        DaySeed(25, "Pleiku → Kon Tum → Măng Đen | 110km",
            "Đèo Măng Đen dốc, chiều tối thường có sương mù dày.",
            listOf(
                ActivitySeed("Xuất phát từ Pleiku dọc QL14", "08:30"),
                ActivitySeed("Nhà thờ Gỗ Kon Tum – Tham quan", "09:45", "10:30", 50.0, actNotes = "45 phút, miễn phí"),
                ActivitySeed("Leo đèo QL24 lên Măng Đen – Ăn trưa", "10:30", "12:00"),
                ActivitySeed("Tới Măng Đen – Nhận phòng", "12:00", "", 60.0, actNotes = "Khí hậu mát mẻ ~18-22°C"),
                ActivitySeed("Hồ Đăk Ke & Thác Pa Sỹ – Tham quan", "15:00", "17:00", actNotes = "Thác 20k")
            )
        ),

        // ── Ngày 26 ────────────────────────────────────────────────────────────
        DaySeed(26, "Măng Đen – NGHỈ HOÀN TOÀN",
            "Nghỉ 100% để thư giãn trước khi đổ đèo Lò Xo siêu dài ngày mai.",
            listOf(
                ActivitySeed("Ăn sáng bún măng vịt đặc sản", "08:00", "09:30"),
                ActivitySeed("Cà phê – Tận hưởng không khí rừng thông", "09:30", "12:00"),
                ActivitySeed("Ăn trưa gà nướng cơm lam", "12:00", "13:30"),
                ActivitySeed("Tản bộ dọc rừng thông Măng Đen", "15:00", "17:00", actNotes = "Miễn phí, không khí trong lành"),
                ActivitySeed("Ăn tối nhẹ nhàng – Nghỉ sớm", "18:00", "20:00")
            )
        ),

        // ── Ngày 27 ────────────────────────────────────────────────────────────
        DaySeed(27, "Măng Đen → Đà Nẵng | 250km (Đèo Lò Xo)",
            "BẮT BUỘC gài số thấp (L/1/2) khi đổ đèo Lò Xo. TUYỆT ĐỐI không rà phanh liên tục tránh sôi dầu phanh.",
            listOf(
                ActivitySeed("Xuất phát – Bắt đầu đổ đèo Lò Xo", "07:30", actNotes = "Đèo dốc dài ~30km, gài số thấp hãm tốc bằng động cơ"),
                ActivitySeed("Dừng nghỉ chân – Kiểm tra lốp và phanh", "10:30", "10:45", 100.0, actNotes = "Quan trọng: Kiểm tra nhiệt độ dầu phanh"),
                ActivitySeed("Dừng ăn trưa – Nam Giang / Đại Lộc", "12:30", "13:30", 75.0),
                ActivitySeed("Tới Đà Nẵng – Nhận phòng", "14:30", "", 75.0),
                ActivitySeed("Tắm biển Mỹ Khê giải nhiệt", "16:30", "18:30")
            )
        ),

        // ── Ngày 28 ────────────────────────────────────────────────────────────
        DaySeed(28, "Đà Nẵng → Đồng Hới (Quảng Bình) | 270km",
            "Cao tốc Cam Lộ - La Sơn chỉ 2 làn, cấm vượt phần lớn tuyến. Tuân thủ tuyệt đối.",
            listOf(
                ActivitySeed("Xuất phát – Đi qua hầm Hải Vân nhập cao tốc La Sơn", "08:00"),
                ActivitySeed("Dừng nghỉ vươn vai", "10:30", "10:45", 100.0),
                ActivitySeed("Dừng ăn trưa – Đông Hà (Quảng Trị)", "12:30", "13:30", 80.0),
                ActivitySeed("Tới Đồng Hới – Nhận phòng", "15:00", "", 90.0),
                ActivitySeed("Dạo biển Nhật Lệ – Tượng đài Mẹ Suốt", "16:30", "18:00", actNotes = "Miễn phí")
            )
        ),

        // ── Ngày 29 ────────────────────────────────────────────────────────────
        DaySeed(29, "Đồng Hới → Vinh (Nghệ An) | 200km",
            "Đường đi nhàn nhã, chủ yếu QL1A bằng phẳng. Nghỉ sớm hồi sức chuẩn bị chặng cuối.",
            listOf(
                ActivitySeed("Xuất phát từ Đồng Hới", "08:00"),
                ActivitySeed("Dừng nghỉ ngơi – Kỳ Anh (Hà Tĩnh)", "10:30", "10:45", 100.0),
                ActivitySeed("Tới TP. Vinh – Ăn trưa súp lươn đặc sản", "13:00", "14:00", 100.0),
                ActivitySeed("Nhận phòng khách sạn", "14:00"),
                ActivitySeed("Nghỉ ngơi sớm hồi sức – Chuẩn bị chặng 380km ngày mai", "15:00")
            )
        ),

        // ── Ngày 30 ────────────────────────────────────────────────────────────
        DaySeed(30, "Vinh → Thanh Sơn (Phú Thọ) | 380km – VỀ NHÀ!",
            "Đoạn đường HCM vắng, chú ý trâu bò qua đường. KẾT THÚC HÀNH TRÌNH!",
            listOf(
                ActivitySeed("Xuất phát sớm từ TP. Vinh – Nhập cao tốc Bắc Nam", "06:00", actNotes = "Xuất phát sớm để về trước 16h"),
                ActivitySeed("Dừng ăn sáng & nghỉ ngơi – Trạm dừng Thanh Hóa", "08:30", "09:00", 100.0),
                ActivitySeed("Tiếp tục cao tốc – Thoát tại Miếu Môn/Xuân Mai", "09:00", "11:00", 100.0),
                ActivitySeed("Dừng ăn trưa dọc đường HCM", "13:15", "14:15", 90.0),
                ActivitySeed("🏠 VỀ TỚI THANH SƠN – KẾT THÚC HÀNH TRÌNH!", "15:30", "", 90.0,
                    actNotes = "🎉 30 ngày – 4.745km – Hoàn thành!")
            )
        )
    )
}
