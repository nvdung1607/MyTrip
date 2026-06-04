package com.example.mytrip.widget.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
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
import com.example.mytrip.widget.actions.AddNoteAction
import com.example.mytrip.widget.actions.tripIdKey

// ── Clean Light Color Palette (Tone màu trắng tương tự màu app) ──────────────────
private val BgColor       = ColorProvider(Color(0xFFFFFFFF)) // Base background
private val CardColor     = ColorProvider(Color(0xFFF1F5F9)) // Warm light card background
private val DividerColor  = ColorProvider(Color(0xFFE2E8F0)) // Soft grey borders
private val HeaderBg      = ColorProvider(Color(0xFFF8FAFC)) // Subtle header card background

private val TealColor     = ColorProvider(Color(0xFF26D0CE)) // Logo Gradient Teal
private val NavyColor     = ColorProvider(Color(0xFF1A2980)) // Logo Gradient Navy
private val BlueAccent    = ColorProvider(Color(0xFF2563EB)) // Blue interactive

private val TextPrimary   = ColorProvider(Color(0xFF0F172A)) // Slate 900
private val TextSecondary = ColorProvider(Color(0xFF475569)) // Slate 600
private val TextMuted     = ColorProvider(Color(0xFF94A3B8)) // Slate 400
private val TextOnDark    = ColorProvider(Color(0xFFFFFFFF))

private val OngoingBg     = ColorProvider(Color(0xFF22C55E)) // Green
private val PlanningBg    = ColorProvider(Color(0xFFF59E0B)) // Amber
private val DoneBg        = ColorProvider(Color(0xFF64748B)) // Slate 500

private val PlusColor     = ColorProvider(Color(0xFF1A2980)) // Deep navy for "+" FAB
private val RedAlert      = ColorProvider(Color(0xFFEF4444)) // Red 500

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatVnd(amount: Long): String = when {
    amount >= 1_000_000_000L -> String.format("%.1ftỷ", amount / 1_000_000_000.0)
    amount >= 1_000_000L    -> {
        val m = amount / 1_000_000.0
        if (m == kotlin.math.floor(m)) "${m.toInt()}tr" else String.format("%.1ftr", m)
    }
    amount >= 1_000L        -> "${amount / 1_000}k"
    else                    -> "${amount}đ"
}

private fun openAppIntent(tripId: Long, route: String = "trip_detail/$tripId") =
    Intent(Intent.ACTION_VIEW, Uri.parse("mytrip://$route"))
        .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }

// ── Reusable components ───────────────────────────────────────────────────────

/** Round blue "+" FAB */
@Composable
private fun AddNoteButton(tripId: Long, size: Int = 30) {
    Box(
        modifier = GlanceModifier
            .size(size.dp)
            .cornerRadius((size / 2).dp)
            .background(PlusColor)
            .clickable(actionRunCallback<AddNoteAction>(actionParametersOf(tripIdKey to tripId))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "+",
            style = TextStyle(
                color = TextOnDark,
                fontSize = (size * 0.55f).sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

/** Colored status pill */
@Composable
private fun StatusBadge(status: TripStatus, compact: Boolean = false) {
    val (label, bg) = when (status) {
        TripStatus.ONGOING  -> "● Đang đi" to OngoingBg
        TripStatus.PLANNING -> "⏳ Sắp đi" to PlanningBg
        TripStatus.DONE     -> "✓ Xong"    to DoneBg
    }
    Box(
        modifier = GlanceModifier
            .cornerRadius(12.dp)
            .background(bg)
            .padding(horizontal = if (compact) 6.dp else 8.dp, vertical = if (compact) 1.dp else 2.dp)
    ) {
        Text(
            label,
            style = TextStyle(
                color = TextOnDark,
                fontSize = if (compact) 9.sp else 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

/** Horizontal progress bar */
@Composable
private fun ProgressBar(fraction: Float, isAlert: Boolean = false) {
    val fillColor = if (isAlert) RedAlert else TealColor
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(5.dp)
            .cornerRadius(3.dp)
            .background(DividerColor)
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(end = ((1f - fraction.coerceIn(0f, 1f)) * 120).dp)
                .cornerRadius(3.dp)
                .background(fillColor)
        ) {}
    }
}

/** Budget summary with bar */
@Composable
private fun BudgetSection(actual: Long, planned: Long, compact: Boolean = false) {
    val fraction = if (planned > 0) actual.toFloat() / planned else 0f
    val overBudget = fraction > 0.9f
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.Vertical.CenterVertically) {
            Text(
                "💰 ${formatVnd(actual)}",
                style = TextStyle(
                    color = if (overBudget) RedAlert else TextSecondary,
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(GlanceModifier.defaultWeight())
            Text(
                "/ ${formatVnd(planned)}",
                style = TextStyle(color = TextMuted, fontSize = if (compact) 9.sp else 10.sp)
            )
        }
        Spacer(GlanceModifier.height(3.dp))
        ProgressBar(fraction, isAlert = overBudget)
    }
}

// ── Empty State ────────────────────────────────────────────────────────────────

@Composable
fun EmptyWidget() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(BgColor)
            .padding(12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text("🗺️", style = TextStyle(fontSize = 32.sp, textAlign = TextAlign.Center))
        Spacer(GlanceModifier.height(6.dp))
        Text(
            "Chưa có chuyến đi",
            style = TextStyle(
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
        Spacer(GlanceModifier.height(4.dp))
        Text(
            "Nhấn để tạo ngay",
            style = TextStyle(color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
        )
    }
}

// ── Small Widget (2×2) ────────────────────────────────────────────────────────

@Composable
fun SmallWidget(state: MyTripWidgetState) {
    val openApp = actionStartActivity(openAppIntent(state.tripId))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(BgColor)
            .padding(10.dp)
            .clickable(openApp)
    ) {
        // Top row
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(state.tripTypeIcon, style = TextStyle(fontSize = 18.sp))
            Spacer(GlanceModifier.width(5.dp))
            StatusBadge(state.tripStatus, compact = true)
            Spacer(GlanceModifier.defaultWeight())
            AddNoteButton(tripId = state.tripId, size = 24)
        }

        Spacer(GlanceModifier.height(8.dp))

        // Trip Name
        Text(
            state.tripName,
            style = TextStyle(
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )

        Spacer(GlanceModifier.defaultWeight())

        // Bottom info
        when {
            state.tripStatus == TripStatus.ONGOING && state.currentDay > 0 -> {
                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Text(
                        "Ngày ${state.currentDay} / ${state.totalDays}",
                        style = TextStyle(
                            color = NavyColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    ProgressBar(state.currentDay.toFloat() / state.totalDays)
                }
            }
            state.tripStatus == TripStatus.PLANNING -> {
                Column {
                    Text(
                        "Còn ${state.daysUntilTrip} ngày",
                        style = TextStyle(
                            color = NavyColor,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        "nữa khởi hành!",
                        style = TextStyle(color = TextSecondary, fontSize = 10.sp)
                    )
                }
            }
            else -> {
                Text(
                    "Đã kết thúc",
                    style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                )
            }
        }
    }
}

// ── Medium Widget (4×2) ───────────────────────────────────────────────────────

@Composable
fun MediumWidget(state: MyTripWidgetState) {
    val openApp = actionStartActivity(openAppIntent(state.tripId))

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(BgColor)
            .padding(12.dp)
            .clickable(openApp)
    ) {
        // Left Column (50% space): General trip status
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
        ) {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(state.tripTypeIcon, style = TextStyle(fontSize = 20.sp))
                Spacer(GlanceModifier.width(6.dp))
                StatusBadge(state.tripStatus, compact = true)
            }
            
            Spacer(GlanceModifier.height(8.dp))
            
            Text(
                state.tripName,
                style = TextStyle(
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1
            )
            
            Spacer(GlanceModifier.defaultWeight())
            
            if (state.tripStatus == TripStatus.ONGOING && state.currentDay > 0) {
                Column {
                    Text(
                        "Ngày ${state.currentDay} / ${state.totalDays}",
                        style = TextStyle(color = NavyColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    ProgressBar(state.currentDay.toFloat() / state.totalDays)
                }
            } else if (state.tripStatus == TripStatus.PLANNING) {
                Column {
                    Text(
                        "Còn ${state.daysUntilTrip} ngày nữa",
                        style = TextStyle(color = NavyColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        "lên đường!",
                        style = TextStyle(color = TextSecondary, fontSize = 10.sp)
                    )
                }
            } else {
                Text(
                    "Đã kết thúc",
                    style = TextStyle(color = TextMuted, fontSize = 11.sp)
                )
            }
        }

        Spacer(GlanceModifier.width(10.dp))
        
        // Vertical Divider
        Box(
            modifier = GlanceModifier
                .width(1.dp)
                .fillMaxHeight()
                .background(DividerColor)
        ) {}

        Spacer(GlanceModifier.width(10.dp))

        // Right Column (50% space): Actions and next activities / budget
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
        ) {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text(
                    "Kế hoạch tiếp theo",
                    style = TextStyle(color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.defaultWeight())
                AddNoteButton(tripId = state.tripId, size = 26)
            }
            
            Spacer(GlanceModifier.height(6.dp))

            when {
                state.hasNextActivity -> {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(8.dp)
                            .background(CardColor)
                            .padding(8.dp),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(state.nextActivityIcon, style = TextStyle(fontSize = 14.sp))
                        Spacer(GlanceModifier.width(6.dp))
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Text(
                                state.nextActivityName,
                                style = TextStyle(color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            if (state.nextActivityTime.isNotBlank()) {
                                Text(
                                    "🕐 ${state.nextActivityTime}",
                                    style = TextStyle(color = TextSecondary, fontSize = 9.sp)
                                )
                            }
                        }
                    }
                }
                state.tripStatus == TripStatus.PLANNING -> {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(8.dp)
                            .background(CardColor)
                            .padding(8.dp)
                    ) {
                        Text(
                            "Đang chuẩn bị khởi hành...",
                            style = TextStyle(color = TextSecondary, fontSize = 10.sp)
                        )
                    }
                }
                else -> {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(8.dp)
                            .background(CardColor)
                            .padding(8.dp)
                    ) {
                        Text(
                            "Không có hoạt động nào",
                            style = TextStyle(color = TextMuted, fontSize = 10.sp)
                        )
                    }
                }
            }

            Spacer(GlanceModifier.defaultWeight())

            if (state.totalPlanned > 0) {
                BudgetSection(state.totalActual, state.totalPlanned, compact = true)
            }
        }
    }
}

// ── Large Widget (4×4) ────────────────────────────────────────────────────────

@Composable
fun LargeWidget(state: MyTripWidgetState) {
    val openApp = actionStartActivity(openAppIntent(state.tripId))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(16.dp)
            .background(BgColor)
            .padding(14.dp)
            .clickable(openApp)
    ) {
        // ── Clean Header ──────────────────────────────────────────────
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Text(state.tripTypeIcon, style = TextStyle(fontSize = 20.sp))
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        state.tripName,
                        style = TextStyle(
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                }
                Spacer(GlanceModifier.height(4.dp))
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    StatusBadge(state.tripStatus)
                    Spacer(GlanceModifier.width(8.dp))
                    if (state.tripStatus == TripStatus.ONGOING && state.currentDay > 0) {
                        Text(
                            "Ngày ${state.currentDay} / ${state.totalDays}",
                            style = TextStyle(
                                color = NavyColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    } else if (state.tripStatus == TripStatus.PLANNING) {
                        Text(
                            "Còn ${state.daysUntilTrip} ngày xuất phát",
                            style = TextStyle(
                                color = NavyColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
            AddNoteButton(tripId = state.tripId, size = 32)
        }

        Spacer(GlanceModifier.height(10.dp))
        Box(modifier = GlanceModifier.fillMaxWidth().height(1.dp).background(DividerColor)) {}
        Spacer(GlanceModifier.height(10.dp))

        // ── Dashboard Split ───────────────────────────────────────────
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
        ) {
            // Left Column (50% width): Activities List
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
            ) {
                Text(
                    if (state.tripStatus == TripStatus.ONGOING) "📋 Hôm nay" else "📋 Dự kiến hành trình",
                    style = TextStyle(
                        color = NavyColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(GlanceModifier.height(6.dp))

                if (state.todayActivities.isNotEmpty()) {
                    Column {
                        state.todayActivities.take(4).forEach { item ->
                            LargeActivityRow(item)
                            Spacer(GlanceModifier.height(4.dp))
                        }
                    }
                } else {
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .defaultWeight()
                            .cornerRadius(8.dp)
                            .background(CardColor)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (state.tripStatus == TripStatus.PLANNING) "Hãy chuẩn bị hành trang cho chuyến đi mẫu sắp tới!" else "Không có hoạt động nào",
                            style = TextStyle(
                                color = TextMuted,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            Spacer(GlanceModifier.width(12.dp))

            // Right Column (50% width): Budget and Summary Cards
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
            ) {
                if (state.totalPlanned > 0) {
                    Text(
                        "💰 Chi phí",
                        style = TextStyle(
                            color = NavyColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(6.dp))
                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .cornerRadius(8.dp)
                            .background(CardColor)
                            .padding(8.dp)
                    ) {
                        Column {
                            val fraction = if (state.totalPlanned > 0) state.totalActual.toFloat() / state.totalPlanned else 0f
                            val overBudget = fraction > 0.9f
                            Text(
                                "Đã chi: ${formatVnd(state.totalActual)}",
                                style = TextStyle(
                                    color = if (overBudget) RedAlert else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(GlanceModifier.height(2.dp))
                            Text(
                                "Hạn mức: ${formatVnd(state.totalPlanned)}",
                                style = TextStyle(color = TextSecondary, fontSize = 9.sp)
                            )
                            Spacer(GlanceModifier.height(6.dp))
                            ProgressBar(fraction, isAlert = overBudget)
                        }
                    }
                }

                Spacer(GlanceModifier.defaultWeight())

                // App Branding Tag
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                    horizontalAlignment = Alignment.Horizontal.End
                ) {
                    Text("MyTrip App 🗺️", style = TextStyle(color = TextMuted, fontSize = 9.sp))
                }
            }
        }
    }
}

// ── Activity Row (Large widget only) ──────────────────────────────────────────

@Composable
private fun LargeActivityRow(item: ActivityItem) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(8.dp)
            .background(if (item.isDone) CardColor else ColorProvider(Color(0xFFFFFFFF)))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        // Status dot
        Box(
            modifier = GlanceModifier
                .size(6.dp)
                .cornerRadius(3.dp)
                .background(
                    if (item.isDone) OngoingBg
                    else BlueAccent
                )
        ) {}
        
        Spacer(GlanceModifier.width(6.dp))
        
        // Name
        Text(
            item.name,
            style = TextStyle(
                color = if (item.isDone) TextMuted else TextPrimary,
                fontSize = 11.sp,
                fontWeight = if (item.isDone) FontWeight.Normal else FontWeight.Medium
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
        
        Spacer(GlanceModifier.width(4.dp))
        
        // Time tag
        if (item.time.isNotBlank()) {
            Box(
                modifier = GlanceModifier
                    .cornerRadius(4.dp)
                    .background(CardColor)
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    item.time,
                    style = TextStyle(
                        color = NavyColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        
        Spacer(GlanceModifier.width(4.dp))
        
        Text(
            if (item.isDone) "✅" else item.icon,
            style = TextStyle(fontSize = 11.sp)
        )
    }
}
