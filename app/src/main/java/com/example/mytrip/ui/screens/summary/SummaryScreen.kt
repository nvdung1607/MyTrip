package com.example.mytrip.ui.screens.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.*
import com.example.mytrip.util.DateUtils
import com.example.mytrip.util.MoneyUtils
import java.io.File
import androidx.compose.ui.res.painterResource
import com.example.mytrip.R
import com.example.mytrip.ui.components.GlassmorphismCard
import com.example.mytrip.ui.components.MyTripPrimaryButton
import com.example.mytrip.ui.theme.TripThemeProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(navController: NavController, tripId: Long) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as MyTripApplication
    val vm: SummaryViewModel = viewModel(factory = SummaryViewModel.factory(app))

    LaunchedEffect(tripId) { vm.loadData(tripId) }

    val trip by vm.trip.collectAsStateWithLifecycle()
    val days by vm.days.collectAsStateWithLifecycle()
    val activitiesMap by vm.activitiesMap.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()
    val memberBalances by vm.memberBalances.collectAsStateWithLifecycle()
    val isExportingPhotos by vm.isExportingPhotos.collectAsStateWithLifecycle()

    val totalPlanned = expenses.sumOf { it.planned }
    val totalActual = records.sumOf { it.amount }
    val totalKm = activitiesMap.values.flatten().sumOf { it.distanceKm }
    val topNotes = notes.filter { it.rating >= 4 }.sortedByDescending { it.rating }.take(6)
    val avgRating = if (notes.isNotEmpty()) notes.map { it.rating }.average() else 0.0

    val memberNames = remember(trip) {
        trip?.memberNames?.trim('[', ']')
            ?.split(",")?.map { it.trim().trim('"') }?.filter { it.isNotBlank() }
            ?: listOf("Tôi")
    }

    TripThemeProvider(trip = trip) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = { Text("📊 Tổng kết chuyến đi") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Hero header ────────────────────────────────────────────
            item {
                trip?.let { t ->
                    val gradient = com.example.mytrip.ui.theme.TripThemeColors.getThemeGradient(t.themeColor)
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp)
                            .background(gradient)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(t.type.icon, fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(t.name, style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                "${DateUtils.formatDate(t.startDate)} → ${DateUtils.formatDate(t.endDate)}",
                                color = Color.White.copy(alpha = 0.85f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        // Status chip
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                            color = statusColor(t.status),
                            shape = RoundedCornerShape(20.dp),
                            tonalElevation = 4.dp
                        ) {
                            Text(t.status.label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Stats row ─────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("📅", "${days.size}", "Ngày")
                    StatItem("🛣️", "${String.format("%.0f", totalKm)}", "km")
                    StatItem("📷", "${notes.size}", "Ghi chú")
                    StatItem("⭐", String.format("%.1f", avgRating), "Đánh giá")
                    trip?.let { StatItem("👥", "${it.numPeople}", "Người") }
                }
            }

            // ── Timeline thực tế ──────────────────────────────────────
            item {
                SectionHeader("🗓️ Hành trình thực tế")
            }
            items(days.sortedBy { it.dayNumber }) { day ->
                val acts = activitiesMap[day.id] ?: emptyList()
                val done = acts.count { it.status == ActivityStatus.DONE }
                val skipped = acts.count { it.status == ActivityStatus.SKIPPED }
                val changed = acts.count { it.status == ActivityStatus.CHANGED }
                DayTimelineRow(day = day, done = done, skipped = skipped, changed = changed, total = acts.size)
            }

            // ── Chi phí ───────────────────────────────────────────────
            item { SectionHeader("💰 Tổng kết chi phí") }
            item {
                GlassmorphismCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Header row
                        Row(Modifier.fillMaxWidth()) {
                            Text("Hạng mục", Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Dự kiến", Modifier.width(80.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text("Thực tế", Modifier.width(80.dp), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }
                        HorizontalDivider()
                        expenses.forEach { exp ->
                            val actual = records.filter { it.category == exp.category }.sumOf { it.amount }
                            val over = actual > exp.planned && exp.planned > 0
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(exp.category.icon, fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(exp.category.label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(MoneyUtils.formatShort(exp.planned), Modifier.width(80.dp),
                                    textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                Text(MoneyUtils.formatShort(actual), Modifier.width(80.dp),
                                    textAlign = TextAlign.End, fontWeight = FontWeight.Bold,
                                    color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth()) {
                            Text("TỔNG", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            Text(MoneyUtils.formatShort(totalPlanned), Modifier.width(80.dp),
                                textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(MoneyUtils.formatShort(totalActual), Modifier.width(80.dp),
                                textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        val numPeople = trip?.numPeople ?: 1
                        if (numPeople > 0) {
                            Text("Mỗi người: ${MoneyUtils.formatVnd(totalActual / numPeople)}",
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Quyết toán ────────────────────────────────────────────
            if (memberBalances.isNotEmpty() && (trip?.numPeople ?: 1) > 1) {
                item { SectionHeader("⚖️ Quyết toán") }
                items(memberNames) { name ->
                    val balance = memberBalances[name] ?: 0L
                    GlassmorphismCard(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(name, fontWeight = FontWeight.SemiBold)
                            if (balance >= 0)
                                Text("✅ Được hoàn ${MoneyUtils.formatShort(balance)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            else
                                Text("❗ Cần trả ${MoneyUtils.formatShort(-balance)}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Ảnh nổi bật ───────────────────────────────────────────
            if (topNotes.isNotEmpty()) {
                item { SectionHeader("📷 Kỷ niệm nổi bật") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(topNotes) { note ->
                            NoteHighlightCard(note = note)
                        }
                    }
                }
            }

            // ── Export buttons ─────────────────────────────────────────
            item {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    MyTripPrimaryButton(
                        onClick = { vm.exportToExcel(ctx) },
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_excel),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Xuất file Excel", fontWeight = FontWeight.Bold)
                    }

                    MyTripPrimaryButton(
                        onClick = {
                            vm.exportPhotos(ctx) { count ->
                                android.widget.Toast.makeText(ctx, "Đã lưu $count ảnh vào thư viện", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = !isExportingPhotos
                    ) {
                        if (isExportingPhotos) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (isExportingPhotos) "Đang lưu ảnh..." else "Lưu toàn bộ ảnh về máy", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
}

@Composable
private fun StatItem(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 28.sp)
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun DayTimelineRow(day: Day, done: Int, skipped: Int, changed: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circle with day number
        Box(
            modifier = Modifier.size(40.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("N${day.dayNumber}", fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(DateUtils.formatDate(day.date), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            if (day.title.isNotEmpty()) Text(day.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (total > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (done > 0) StatusBadge("✅ $done", Color(0xFF4CAF50))
                if (skipped > 0) StatusBadge("⏭ $skipped", Color(0xFFF44336))
                if (changed > 0) StatusBadge("🔀 $changed", Color(0xFFFF9800))
            }
        }
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.15f)) {
        Text(text, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun NoteHighlightCard(note: Note) {
    GlassmorphismCard(
        modifier = Modifier.width(160.dp)
    ) {
        Column {
            if (note.photoPath != null) {
                AsyncImage(
                    model = File(note.photoPath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxWidth().height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentAlignment = Alignment.Center) {
                    Text(note.tag.icon, fontSize = 40.sp)
                }
            }
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row {
                    repeat(note.rating) { Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(14.dp)) }
                }
                if (note.name.isNotEmpty())
                    Text(note.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 2)
                Text(note.tag.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}



private fun statusColor(status: TripStatus): Color = when (status) {
    TripStatus.PLANNING -> Color(0xFF1976D2)
    TripStatus.ONGOING  -> Color(0xFF388E3C)
    TripStatus.DONE     -> Color(0xFF757575)
}
