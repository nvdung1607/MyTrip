package com.example.mytrip.widget.ui

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.mytrip.data.db.entities.TripStatus
import com.example.mytrip.widget.ActivityItem
import com.example.mytrip.widget.MyTripWidgetState

// ── Static palette (fallback / neutral colors) ────────────────────────────────

private val BgColor      = ColorProvider(Color(0xFFFFFFFF))
private val CardColor    = ColorProvider(Color(0xFFF1F5F9))
private val DividerColor = ColorProvider(Color(0xFFE2E8F0))
private val TrackColor   = ColorProvider(Color(0xFFE2E8F0))

private val TextPrimary   = ColorProvider(Color(0xFF0F172A))
private val TextSecondary = ColorProvider(Color(0xFF475569))
private val TextMuted     = ColorProvider(Color(0xFF94A3B8))
private val TextOnDark    = ColorProvider(Color(0xFFFFFFFF))

private val BlueAccent  = ColorProvider(Color(0xFF2563EB))
private val BlueLight   = ColorProvider(Color(0xFFDBEAFE))

private val OngoingBg  = ColorProvider(Color(0xFF22C55E))
private val PlanningBg = ColorProvider(Color(0xFFF59E0B))
private val DoneBg     = ColorProvider(Color(0xFF64748B))
private val DoneDot    = ColorProvider(Color(0xFF22C55E))
private val RedAlert   = ColorProvider(Color(0xFFEF4444))

private val DefaultAccent = ColorProvider(Color(0xFF1A2980))

// ── Theme color helper ─────────────────────────────────────────────────────────

/**
 * Parse the trip's hex themeColor into a ColorProvider.
 * Falls back to navy if blank / unparseable.
 */
private fun themeColorProvider(hex: String): ColorProvider = try {
    if (hex.isBlank()) DefaultAccent
    else ColorProvider(Color(AndroidColor.parseColor(hex)))
} catch (_: Exception) {
    DefaultAccent
}

/** Light tint of the theme color for card backgrounds (alpha blend over white). */
private fun themeCardColorProvider(hex: String): ColorProvider = try {
    if (hex.isBlank()) CardColor
    else {
        val c = AndroidColor.parseColor(hex)
        val r = ((AndroidColor.red(c) * 0.15f) + (255 * 0.85f)).toInt()
        val g = ((AndroidColor.green(c) * 0.15f) + (255 * 0.85f)).toInt()
        val b = ((AndroidColor.blue(c) * 0.15f) + (255 * 0.85f)).toInt()
        ColorProvider(Color(android.graphics.Color.rgb(r, g, b)))
    }
} catch (_: Exception) {
    CardColor
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatVnd(amount: Long): String = when {
    amount >= 1_000_000_000L -> String.format("%.1ftỷ", amount / 1_000_000_000.0)
    amount >= 1_000_000L -> {
        val m = amount / 1_000_000.0
        if (m == kotlin.math.floor(m)) "${m.toInt()}tr" else String.format("%.1ftr", m)
    }
    amount >= 1_000L -> "${amount / 1_000}k"
    else -> "${amount}đ"
}

private fun openAppIntent(context: android.content.Context, route: String) =
    Intent(Intent.ACTION_VIEW, android.net.Uri.parse("mytrip://$route")).apply {
        component = android.content.ComponentName(context, com.example.mytrip.MainActivity::class.java)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

private fun tripIntent(context: android.content.Context, tripId: Long) =
    openAppIntent(context, "trip_detail/$tripId")

private fun todayIntent(context: android.content.Context, tripId: Long) =
    openAppIntent(context, "today/$tripId")

// ── Reusable composables ──────────────────────────────────────────────────────

/** Round (+) FAB with theme colour */
@Composable
private fun AddNoteButton(tripId: Long, accentColor: ColorProvider, size: Int = 30) {
    Box(
        modifier = GlanceModifier
            .size(size.dp)
            .cornerRadius((size / 2).dp)
            .background(accentColor)
            .clickable(actionStartActivity(openAppIntent(LocalContext.current, "add_note/$tripId?dayId=-1"))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "+",
            style = TextStyle(
                color      = TextOnDark,
                fontSize   = (size * 0.55f).sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center
            )
        )
    }
}

/** Pill badge with theme colour background */
@Composable
private fun DayBadge(
    label: String,
    accentColor: ColorProvider,
    compact: Boolean = false
) {
    Box(
        modifier = GlanceModifier
            .cornerRadius(20.dp)
            .background(accentColor)
            .padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 4.dp else 6.dp)
    ) {
        Text(label, style = TextStyle(color = TextOnDark, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Bold))
    }
}

/** Thin horizontal progress bar coloured with theme */
@Composable
private fun DayProgressBar(currentDay: Int, totalDays: Int, accentColor: ColorProvider) {
    if (totalDays <= 0) return
    val fraction = (currentDay.toFloat() / totalDays).coerceIn(0f, 1f)
    val maxTrackDp = 100
    val fillEnd = ((1f - fraction) * maxTrackDp).dp
    Box(
        modifier = GlanceModifier.fillMaxWidth().height(4.dp).cornerRadius(2.dp).background(TrackColor)
    ) {
        Box(
            modifier = GlanceModifier.fillMaxSize().padding(end = fillEnd).cornerRadius(2.dp).background(accentColor)
        ) {}
    }
}

/** Status pill */
@Composable
private fun StatusBadge(status: TripStatus) {
    val (label, bg) = when (status) {
        TripStatus.ONGOING  -> "● Đang đi"      to OngoingBg
        TripStatus.PLANNING -> "⏳ Sắp đi"      to PlanningBg
        TripStatus.DONE     -> "✓ Hoàn thành"   to DoneBg
    }
    Box(modifier = GlanceModifier.cornerRadius(12.dp).background(bg).padding(horizontal = 7.dp, vertical = 2.dp)) {
        Text(label, style = TextStyle(color = TextOnDark, fontSize = 9.sp, fontWeight = FontWeight.Bold))
    }
}

/** Single activity row — NO individual clickable so LazyColumn scroll is not blocked.
 *  The parent widget container already handles tap-to-open-Today.
 */
@Composable
private fun ActivityRow(
    item: ActivityItem,
    accentColor: ColorProvider,
    timeCardColor: ColorProvider,
    compact: Boolean = false
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(8.dp)
            .background(if (item.isDone) CardColor else BgColor)
            .padding(horizontal = 8.dp, vertical = if (compact) 2.dp else 3.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier.size(6.dp).cornerRadius(3.dp)
                .background(if (item.isDone) DoneDot else accentColor)
        ) {}
        Spacer(GlanceModifier.width(6.dp))
        Text(
            item.name,
            style = TextStyle(
                color      = if (item.isDone) TextMuted else TextPrimary,
                fontSize   = if (compact) 11.sp else 13.sp,
                fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.Medium
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
        Spacer(GlanceModifier.width(4.dp))
        if (item.time.isNotBlank()) {
            Box(modifier = GlanceModifier.cornerRadius(4.dp).background(timeCardColor).padding(horizontal = 5.dp, vertical = 2.dp)) {
                Text(item.time, style = TextStyle(color = accentColor, fontSize = 10.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(GlanceModifier.width(3.dp))
        }
        Text(if (item.isDone) "✅" else item.icon, style = TextStyle(fontSize = if (compact) 11.sp else 13.sp))
    }
}

/** Budget section nâng cao cho Large widget:
 *  - Tổng thực tế / dự kiến + progress bar
 *  - Chi tiêu hôm nay
 *  - Chi phí còn lại (hoặc vượt ngân sách)
 *  - Chi phí / người (thực tế & dự kiến)
 */
@Composable
private fun BudgetSection(
    actual: Long,
    planned: Long,
    todayActual: Long,
    numPeople: Int,
    accentColor: ColorProvider,
    accentCardColor: ColorProvider
) {
    val overBudget  = planned > 0 && actual > planned
    val fraction    = if (planned > 0) (actual.toFloat() / planned).coerceIn(0f, 1f) else 0f
    val fillEnd     = ((1f - fraction) * 100).dp
    val remaining   = planned - actual            // âm = vượt ngân sách
    val perActual   = if (numPeople > 1) actual / numPeople else actual
    val perPlanned  = if (numPeople > 1) planned / numPeople else planned
    val percentUsed = if (planned > 0) (fraction * 100).toInt() else 0

    Column(modifier = GlanceModifier.fillMaxWidth()) {

        // ── Header label ──────────────────────────────────────────────────────
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                "💰 Chi phí chuyến đi",
                style = TextStyle(color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            // % đã sử dụng
            Box(
                modifier = GlanceModifier
                    .cornerRadius(6.dp)
                    .background(if (overBudget) RedAlert else accentCardColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    "$percentUsed%",
                    style = TextStyle(
                        color      = if (overBudget) TextOnDark else accentColor,
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(GlanceModifier.height(5.dp))

        // ── Row 1: Thực tế / Dự kiến ─────────────────────────────────────────
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    "Thực tế",
                    style = TextStyle(color = TextMuted, fontSize = 9.sp)
                )
                Text(
                    formatVnd(actual),
                    style = TextStyle(
                        color      = if (overBudget) RedAlert else accentColor,
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            // Divider
            Box(modifier = GlanceModifier.width(1.dp).height(28.dp).background(DividerColor)) {}
            Spacer(GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    "Dự kiến",
                    style = TextStyle(color = TextMuted, fontSize = 9.sp)
                )
                Text(
                    if (planned > 0) formatVnd(planned) else "–",
                    style = TextStyle(color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(GlanceModifier.height(5.dp))

        // ── Progress bar ──────────────────────────────────────────────────────
        Box(modifier = GlanceModifier.fillMaxWidth().height(5.dp).cornerRadius(3.dp).background(TrackColor)) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(end = fillEnd)
                    .cornerRadius(3.dp)
                    .background(if (overBudget) RedAlert else accentColor)
            ) {}
        }

        Spacer(GlanceModifier.height(6.dp))

        // ── Row 2: Hôm nay + Còn lại (2 thẻ nhỏ) ────────────────────────────
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.Top
        ) {
            // Thẻ: Chi tiêu hôm nay
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .cornerRadius(8.dp)
                    .background(accentCardColor)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Column {
                    Text(
                        "📅 Hôm nay",
                        style = TextStyle(color = TextMuted, fontSize = 9.sp)
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        if (todayActual > 0) formatVnd(todayActual) else "–",
                        style = TextStyle(
                            color      = accentColor,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Spacer(GlanceModifier.width(6.dp))

            // Thẻ: Còn lại / Vượt ngân sách
            Box(
                modifier = GlanceModifier
                    .defaultWeight()
                    .cornerRadius(8.dp)
                    .background(if (overBudget) ColorProvider(Color(0xFFFFEBEE)) else accentCardColor)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Column {
                    Text(
                        if (overBudget) "⚠️ Vượt NS" else "✅ Còn lại",
                        style = TextStyle(color = TextMuted, fontSize = 9.sp)
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        if (planned > 0) formatVnd(if (overBudget) actual - planned else remaining) else "–",
                        style = TextStyle(
                            color      = if (overBudget) RedAlert else accentColor,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // ── Row 3: Chi phí / người ────────────────────────────────────────────
        if (numPeople > 1) {
            Spacer(GlanceModifier.height(5.dp))
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .cornerRadius(8.dp)
                    .background(CardColor)
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text("👤", style = TextStyle(fontSize = 11.sp))
                    Spacer(GlanceModifier.width(5.dp))
                    Text(
                        "Mỗi người:",
                        style = TextStyle(color = TextMuted, fontSize = 10.sp)
                    )
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        formatVnd(perActual),
                        style = TextStyle(
                            color      = if (overBudget) RedAlert else accentColor,
                            fontSize   = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    if (planned > 0) {
                        Text(
                            " / ${formatVnd(perPlanned)}",
                            style = TextStyle(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                    Spacer(GlanceModifier.defaultWeight())
                    Text(
                        "$numPeople người",
                        style = TextStyle(color = TextMuted, fontSize = 9.sp)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── EMPTY STATE ───────────────────────────────────────────────────────────────
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun EmptyWidget() {
    val openApp = actionStartActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse("mytrip://home")).apply {
            component = android.content.ComponentName(LocalContext.current, com.example.mytrip.MainActivity::class.java)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    )
    Column(
        modifier = GlanceModifier.fillMaxSize().cornerRadius(16.dp).background(BgColor)
            .padding(12.dp).clickable(openApp),
        verticalAlignment   = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text("🗺️", style = TextStyle(fontSize = 32.sp, textAlign = TextAlign.Center))
        Spacer(GlanceModifier.height(6.dp))
        Text(
            "Chưa có chuyến đi",
            style = TextStyle(color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        )
        Spacer(GlanceModifier.height(4.dp))
        Text("Nhấn để tạo ngay", style = TextStyle(color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center))
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── SMALL WIDGET (2×2) ────────────────────────────────────────────────────────
//  TOP:    tên chuyến + (+) button
//  MIDDLE: badge Ngày X/Y + tên ngày (route)
//  BOTTOM: danh sách lịch hôm nay (LazyColumn cuộn)
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SmallWidget(state: MyTripWidgetState) {
    val ctx        = LocalContext.current
    val accent     = themeColorProvider(state.tripThemeColor)
    val accentCard = themeCardColorProvider(state.tripThemeColor)
    val openApp    = actionStartActivity(tripIntent(ctx, state.tripId))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(BgColor)
            .padding(10.dp)
            .clickable(openApp)
    ) {
        // ── Row 1: Tên chuyến đi + nút (+) ───────────────────────────────
        Row(
            modifier          = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                state.tripName,
                style    = TextStyle(color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            Spacer(GlanceModifier.width(4.dp))
            AddNoteButton(tripId = state.tripId, accentColor = accent, size = 24)
        }

        Spacer(GlanceModifier.height(5.dp))

        // ── Row 2: Badge ngày + tên ngày ─────────────────────────────────
        when {
            state.tripStatus == TripStatus.ONGOING && state.currentDay > 0 -> {
                val label = if (state.totalDays > 0)
                    "Ngày ${state.currentDay}/${state.totalDays}"
                else "Ngày ${state.currentDay}"
                Row(
                    modifier          = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    DayBadge(label, accent, compact = true)
                    if (state.todayTitle.isNotBlank()) {
                        Spacer(GlanceModifier.width(6.dp))
                        Text(
                            state.todayTitle,
                            style    = TextStyle(color = TextSecondary, fontSize = 9.sp),
                            maxLines = 1,
                            modifier = GlanceModifier.defaultWeight()
                        )
                    }
                }
                if (state.totalDays > 0) {
                    Spacer(GlanceModifier.height(4.dp))
                    DayProgressBar(state.currentDay, state.totalDays, accent)
                }
            }
            state.tripStatus == TripStatus.PLANNING ->
                DayBadge("Còn ${state.daysUntilTrip} ngày", PlanningBg, compact = true)
            else ->
                DayBadge("Đã xong", DoneBg, compact = true)
        }

        Spacer(GlanceModifier.height(4.dp))

        // ── Row 3: Label "Lịch hôm nay" ──────────────────────────────────
        Text(
            "Lịch hôm nay",
            style = TextStyle(color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(3.dp))

        // ── Row 4: Danh sách lịch (cuộn) ─────────────────────────────────
        if (state.todayActivities.isNotEmpty()) {
            LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                items(state.todayActivities) { item ->
                    ActivityRow(item, accent, accentCard, compact = true)
                    Spacer(GlanceModifier.height(3.dp))
                }
            }
        } else if (state.hasNextActivity) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().cornerRadius(8.dp)
                    .background(accentCard).padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(state.nextActivityIcon, style = TextStyle(fontSize = 10.sp))
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    state.nextActivityName,
                    style    = TextStyle(color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight()
                )
                if (state.nextActivityTime.isNotBlank()) {
                    Spacer(GlanceModifier.width(4.dp))
                    Text(
                        state.nextActivityTime,
                        style = TextStyle(color = accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        } else {
            Box(
                modifier         = GlanceModifier.fillMaxWidth().defaultWeight()
                    .cornerRadius(8.dp).background(CardColor).padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Không có lịch", style = TextStyle(color = TextMuted, fontSize = 9.sp, textAlign = TextAlign.Center))
            }
        }
    }
}



@Composable
fun MediumWidget(state: MyTripWidgetState) {
    val ctx        = LocalContext.current
    val accent     = themeColorProvider(state.tripThemeColor)
    val accentCard = themeCardColorProvider(state.tripThemeColor)
    val openApp    = actionStartActivity(tripIntent(ctx, state.tripId))

    // ❌ Không đặt .clickable() ở đây — nếu đặt sẽ chặn scroll của LazyColumn bên trong.
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(BgColor)
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.Top
    ) {
        // ── LEFT: trip info (✔ clickable — không chứa LazyColumn) ───────
        Column(
            modifier = GlanceModifier
                .width(115.dp)
                .fillMaxHeight()
                .clickable(openApp),     // ← chỉ đặt clickable ở phần tĩnh
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Spacer(GlanceModifier.defaultWeight())
            Text(
                state.tripName,
                style    = TextStyle(color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                maxLines = 2
            )
            Spacer(GlanceModifier.height(8.dp))
            when {
                state.tripStatus == TripStatus.ONGOING && state.currentDay > 0 -> {
                    val label = if (state.totalDays > 0) "Ngày ${state.currentDay}/${state.totalDays}" else "Ngày ${state.currentDay}"
                    DayBadge(label, accent, compact = false)
                    if (state.todayTitle.isNotBlank()) {
                        Spacer(GlanceModifier.height(6.dp))
                        Text(state.todayTitle, style = TextStyle(color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center), maxLines = 2)
                    }
                    if (state.totalDays > 0) {
                        Spacer(GlanceModifier.height(8.dp))
                        DayProgressBar(state.currentDay, state.totalDays, accent)
                    }
                }
                state.tripStatus == TripStatus.PLANNING ->
                    DayBadge("Còn ${state.daysUntilTrip} ngày", PlanningBg, compact = false)
                else ->
                    DayBadge("Đã xong", DoneBg, compact = false)
            }
            Spacer(GlanceModifier.defaultWeight())
        }

        Spacer(GlanceModifier.width(12.dp))

        // ── RIGHT: schedule (✖ không clickable — chứa LazyColumn) ──────────
        Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight()) {
            // Header row: label + add button.
            // Click vào "Lịch hôm nay" → mở Today screen nếu đang đi, nếu không mở trip.
            val todayAction = if (state.tripStatus == TripStatus.ONGOING && state.currentDay > 0)
                actionStartActivity(todayIntent(ctx, state.tripId))
            else
                openApp

            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(todayAction),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    "Lịch hôm nay",
                    style    = TextStyle(color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    modifier = GlanceModifier.defaultWeight()
                )
                AddNoteButton(tripId = state.tripId, accentColor = accent, size = 28)
            }
            Spacer(GlanceModifier.height(6.dp))

            // LazyColumn: không có clickable trên container → scroll tự do.
            if (state.todayActivities.isNotEmpty()) {
                LazyColumn(modifier = GlanceModifier.defaultWeight().fillMaxWidth()) {
                    items(state.todayActivities) { item ->
                        ActivityRow(item, accent, accentCard, compact = false)
                        Spacer(GlanceModifier.height(1.dp))
                    }
                }
            } else if (state.hasNextActivity) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().cornerRadius(10.dp)
                        .background(accentCard).padding(10.dp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Text(state.nextActivityIcon, style = TextStyle(fontSize = 14.sp))
                    Spacer(GlanceModifier.width(6.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            state.nextActivityName,
                            style    = TextStyle(color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        if (state.nextActivityTime.isNotBlank()) {
                            Spacer(GlanceModifier.height(2.dp))
                            Text("🕐 ${state.nextActivityTime}", style = TextStyle(color = TextSecondary, fontSize = 10.sp))
                        }
                    }
                }
            } else {
                Box(
                    modifier         = GlanceModifier.fillMaxWidth().defaultWeight().cornerRadius(10.dp)
                        .background(CardColor).padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Không có lịch hôm nay", style = TextStyle(color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center))
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ── LARGE WIDGET (4×4) ────────────────────────────────────────────────────────
//  NOTE: Không dùng .clickable() trên container cha của LazyColumn.
//        Clickable đặt riêng trên từng section cố định.
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun LargeWidget(state: MyTripWidgetState) {
    val ctx        = LocalContext.current
    val accent     = themeColorProvider(state.tripThemeColor)
    val accentCard = themeCardColorProvider(state.tripThemeColor)
    val openApp    = actionStartActivity(tripIntent(ctx, state.tripId))
    val hasBudget  = state.totalPlanned > 0L

    // ❌ Không đặt .clickable() trên Column này.
    // Clickable được gắn trực tiếp vào từng section cố định — không gắn vào LazyColumn.
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(BgColor)
            .padding(16.dp)
    ) {
        // ── Header (✔ clickable) ───────────────────────────────────────────
        Row(
            modifier          = GlanceModifier.fillMaxWidth().clickable(openApp),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    state.tripName,
                    style    = TextStyle(color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(GlanceModifier.height(3.dp))
                StatusBadge(state.tripStatus)
            }
            AddNoteButton(tripId = state.tripId, accentColor = accent, size = 34)
        }

        Spacer(GlanceModifier.height(10.dp))

        // ── Day counter + route title + progress (✔ clickable) ────────────────
        when {
            state.tripStatus == TripStatus.ONGOING && state.currentDay > 0 -> {
                Row(
                    modifier          = GlanceModifier.fillMaxWidth().clickable(openApp),
                    verticalAlignment = Alignment.Vertical.CenterVertically
                ) {
                    Box(
                        modifier = GlanceModifier.cornerRadius(20.dp).background(accent)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "Ngày ${state.currentDay}",
                            style = TextStyle(color = TextOnDark, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(GlanceModifier.width(10.dp))
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        if (state.totalDays > 0) {
                            Text("/ ${state.totalDays} ngày", style = TextStyle(color = TextMuted, fontSize = 12.sp))
                            Spacer(GlanceModifier.height(4.dp))
                            DayProgressBar(state.currentDay, state.totalDays, accent)
                        }
                        if (state.todayTitle.isNotBlank()) {
                            Spacer(GlanceModifier.height(3.dp))
                            Text(state.todayTitle, style = TextStyle(color = TextSecondary, fontSize = 11.sp), maxLines = 1)
                        }
                    }
                }
            }
            state.tripStatus == TripStatus.PLANNING ->
                Box(modifier = GlanceModifier.clickable(openApp)) {
                    DayBadge("Còn ${state.daysUntilTrip} ngày nữa", PlanningBg)
                }
            else ->
                Text("Chuyến đi đã kết thúc", style = TextStyle(color = TextMuted, fontSize = 13.sp))
        }

        Spacer(GlanceModifier.height(10.dp))
        Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(DividerColor)) {}
        Spacer(GlanceModifier.height(8.dp))

        // ── Schedule header (✔ clickable → Today screen) ─────────────────────
        val todayAction = if (state.tripStatus == TripStatus.ONGOING && state.currentDay > 0)
            actionStartActivity(todayIntent(ctx, state.tripId))
        else
            openApp

        Row(
            modifier          = GlanceModifier.fillMaxWidth().clickable(todayAction),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                "Lịch trình hôm nay",
                style    = TextStyle(color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            if (state.tripStatus == TripStatus.ONGOING && state.currentDay > 0 && state.totalDays > 0) {
                Box(
                    modifier = GlanceModifier.cornerRadius(8.dp).background(accentCard)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        "${state.currentDay}/${state.totalDays}",
                        style = TextStyle(color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        Spacer(GlanceModifier.height(6.dp))

        // ── Schedule list ─────────────────────────────────────────────────
        // Dùng fixed height khi có budget để budget luôn hiển thị ở cuối.
        // Khi không có budget, defaultWeight() để chiếm hết không gian còn lại.
        if (state.todayActivities.isNotEmpty()) {
            val listModifier = if (hasBudget)
                GlanceModifier.fillMaxWidth().height(160.dp)
            else
                GlanceModifier.fillMaxWidth().defaultWeight()
            LazyColumn(modifier = listModifier) {
                items(state.todayActivities) { item ->
                    ActivityRow(item, accent, accentCard, compact = false)
                    Spacer(GlanceModifier.height(5.dp))
                }
            }
        } else {
            val emptyModifier = if (hasBudget)
                GlanceModifier.fillMaxWidth().height(80.dp)
            else
                GlanceModifier.fillMaxWidth().defaultWeight()
            Box(
                modifier         = emptyModifier.cornerRadius(12.dp).background(CardColor).padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Không có lịch trình hôm nay", style = TextStyle(color = TextMuted, fontSize = 13.sp, textAlign = TextAlign.Center))
            }
        }

        // ── Budget section (luôn hiển thị cuối widget nếu có dữ liệu) ──────
        if (hasBudget) {
            Spacer(GlanceModifier.height(8.dp))
            Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(DividerColor)) {}
            Spacer(GlanceModifier.height(6.dp))
            Column(modifier = GlanceModifier.fillMaxWidth().clickable(openApp)) {
                BudgetSection(
                    actual          = state.totalActual,
                    planned         = state.totalPlanned,
                    todayActual     = state.todayActual,
                    numPeople       = state.numPeople,
                    accentColor     = accent,
                    accentCardColor = accentCard
                )
            }
        }
    }
}
