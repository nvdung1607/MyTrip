package com.example.mytrip.ui.screens.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mytrip.data.db.entities.Activity
import com.example.mytrip.data.db.entities.ActivityStatus
import com.example.mytrip.data.db.entities.Note
import com.example.mytrip.navigation.Screen
import com.example.mytrip.util.DateUtils
import com.example.mytrip.util.MoneyUtils
import com.example.mytrip.ui.components.DraggableFab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    navController: NavController,
    tripId: Long,
    viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory)
) {
    LaunchedEffect(tripId) { viewModel.loadData(tripId) }

    val trip by viewModel.trip.collectAsState()
    val todayDay by viewModel.todayDay.collectAsState()
    val activities by viewModel.todayActivities.collectAsState()
    val notes by viewModel.todayNotes.collectAsState()
    val selectedIndex by viewModel.selectedDayIndex.collectAsState()

    // Dialog state for CHANGED notes
    var changedActivityDialog by remember { mutableStateOf<Activity?>(null) }
    var changedNotesText by remember { mutableStateOf("") }

    changedActivityDialog?.let { activity ->
        AlertDialog(
            onDismissRequest = { changedActivityDialog = null },
            title = { Text("Ghi chú thay đổi", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Hoạt động: ${activity.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = changedNotesText,
                        onValueChange = { changedNotesText = it },
                        label = { Text("Điều gì đã thay đổi?") },
                        placeholder = { Text("VD: Đổi khách sạn, thay đổi lộ trình...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateActivityActualNotes(activity, changedNotesText)
                    changedActivityDialog = null
                    changedNotesText = ""
                }) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { changedActivityDialog = null }) { Text("Huỷ") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = trip?.name ?: "Hôm nay",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
            // ── Day navigator ────────────────────────────────────────────
            item {
                DayNavigatorRow(
                    selectedIndex = selectedIndex,
                    onSelectDay = { viewModel.changeDay(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // ── Date header ──────────────────────────────────────────────
            item {
                DateHeaderCard(
                    day = todayDay,
                    selectedIndex = selectedIndex,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // ── Empty state ──────────────────────────────────────────────
            if (todayDay == null) {
                item {
                    EmptyDayState(selectedIndex = selectedIndex)
                }
            } else {
                // ── Activities timeline ──────────────────────────────────
                if (activities.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "📋 Lịch trình",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        TimelineView(
                            activities = activities,
                            onMarkDone = { viewModel.markActivityDone(it) },
                            onMarkSkipped = { viewModel.markActivitySkipped(it) },
                            onMarkChanged = { activity ->
                                changedNotesText = activity.actualNotes
                                changedActivityDialog = activity
                                viewModel.markActivityChanged(activity.id)
                            },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    item {
                        EmptyActivitiesState(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                // ── Notes section ────────────────────────────────────────
                item {
                    SectionHeader(
                        title = "📝 Ghi chú ngày này",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                if (notes.isEmpty()) {
                    item {
                        EmptyNotesState(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    items(notes.chunked(2)) { pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { note ->
                                NoteCard(
                                    note = note,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
        }
        }
        DraggableFab(
            onClick = {
                navController.navigate(
                    Screen.AddNote.createRoute(tripId, todayDay?.id)
                )
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
}

// ── Day navigator ────────────────────────────────────────────────────────────

@Composable
private fun DayNavigatorRow(
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("◀ Hôm qua", "Hôm nay", "Ngày mai ▶")

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.Transparent,
                    animationSpec = tween(200), label = "tabBg"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200), label = "tabText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(bgColor)
                        .clickable { onSelectDay(index) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ── Date header ──────────────────────────────────────────────────────────────

@Composable
private fun DateHeaderCard(
    day: com.example.mytrip.data.db.entities.Day?,
    selectedIndex: Int,
    modifier: Modifier = Modifier
) {
    val targetMillis = when (selectedIndex) {
        0 -> DateUtils.todayMillis() - 86_400_000L
        2 -> DateUtils.todayMillis() + 86_400_000L
        else -> DateUtils.todayMillis()
    }
    val displayMillis = day?.date ?: targetMillis

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (selectedIndex) {
                1 -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedIndex == 1) {
                Text(
                    text = "📍 Hôm nay",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = DateUtils.formatFull(displayMillis),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            day?.let {
                if (it.title.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = it.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Ngày ${it.dayNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ── Timeline ─────────────────────────────────────────────────────────────────

@Composable
private fun TimelineView(
    activities: List<Activity>,
    onMarkDone: (Long) -> Unit,
    onMarkSkipped: (Long) -> Unit,
    onMarkChanged: (Activity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        activities.forEachIndexed { idx, activity ->
            TimelineItem(
                activity = activity,
                isLast = idx == activities.lastIndex,
                onMarkDone = onMarkDone,
                onMarkSkipped = onMarkSkipped,
                onMarkChanged = onMarkChanged
            )
        }
    }
}

@Composable
private fun TimelineItem(
    activity: Activity,
    isLast: Boolean,
    onMarkDone: (Long) -> Unit,
    onMarkSkipped: (Long) -> Unit,
    onMarkChanged: (Activity) -> Unit
) {
    val statusColor = when (activity.status) {
        ActivityStatus.DONE -> Color(0xFF2E7D32)
        ActivityStatus.SKIPPED -> Color(0xFFC62828)
        ActivityStatus.CHANGED -> Color(0xFFE65100)
        ActivityStatus.PENDING -> MaterialTheme.colorScheme.primary
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline line + dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(160.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(statusColor.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Activity card
        ActivityTimelineCard(
            activity = activity,
            statusColor = statusColor,
            onMarkDone = onMarkDone,
            onMarkSkipped = onMarkSkipped,
            onMarkChanged = onMarkChanged,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
        )
    }
}

@Composable
private fun ActivityTimelineCard(
    activity: Activity,
    statusColor: Color,
    onMarkDone: (Long) -> Unit,
    onMarkSkipped: (Long) -> Unit,
    onMarkChanged: (Activity) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(
                width = 1.5.dp,
                color = statusColor.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Time & status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (activity.departureTime.isNotBlank()) {
                    Text(
                        text = "🕐 ${activity.departureTime}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                StatusBadge(status = activity.status, color = statusColor)
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = activity.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (activity.notes.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = activity.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Actual notes for CHANGED
            if (activity.status == ActivityStatus.CHANGED && activity.actualNotes.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "🔀 ${activity.actualNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusActionButton(
                    label = "✅ Xong",
                    isActive = activity.status == ActivityStatus.DONE,
                    activeColor = Color(0xFF2E7D32),
                    onClick = { onMarkDone(activity.id) },
                    modifier = Modifier.weight(1f)
                )
                StatusActionButton(
                    label = "⏭️ Bỏ qua",
                    isActive = activity.status == ActivityStatus.SKIPPED,
                    activeColor = Color(0xFFC62828),
                    onClick = { onMarkSkipped(activity.id) },
                    modifier = Modifier.weight(1f)
                )
                StatusActionButton(
                    label = "🔀 Đổi",
                    isActive = activity.status == ActivityStatus.CHANGED,
                    activeColor = Color(0xFFE65100),
                    onClick = { onMarkChanged(activity) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ActivityStatus, color: Color) {
    val (emoji, label) = when (status) {
        ActivityStatus.PENDING -> Pair("⏳", "Chờ")
        ActivityStatus.DONE -> Pair("✅", "Xong")
        ActivityStatus.SKIPPED -> Pair("⏭️", "Bỏ qua")
        ActivityStatus.CHANGED -> Pair("🔀", "Đổi")
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "$emoji $label",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun StatusActionButton(
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        color = if (isActive) activeColor else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp, horizontal = 2.dp)
        )
    }
}

// ── Note card ─────────────────────────────────────────────────────────────────

@Composable
private fun NoteCard(
    note: Note,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column {
            // Photo
            if (note.photoPath != null) {
                AsyncImage(
                    model = note.photoPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(note.tag.icon, fontSize = 24.sp)
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                // Stars
                Row {
                    repeat(5) { i ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i < note.rating) Color(0xFFFFC107)
                            else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Tag chip
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${note.tag.icon} ${note.tag.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (note.name.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = note.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (note.cost > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "💰 ${MoneyUtils.formatShort(note.cost)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ── Empty states ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyDayState(selectedIndex: Int) {
    val message = when (selectedIndex) {
        0 -> "Không có dữ liệu cho ngày hôm qua."
        2 -> "Chưa có kế hoạch cho ngày mai."
        else -> "Hôm nay không có trong lịch trình chuyến đi."
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📅", fontSize = 48.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyActivitiesState(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("🗓️", fontSize = 32.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Không có hoạt động nào được lên kế hoạch.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptyNotesState(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("📝", fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Chưa có ghi chú nào.\nNhấn + để thêm note đầu tiên!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
