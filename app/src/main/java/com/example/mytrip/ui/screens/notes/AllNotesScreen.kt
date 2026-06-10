package com.example.mytrip.ui.screens.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.mytrip.data.db.entities.Day
import com.example.mytrip.data.db.entities.Note
import com.example.mytrip.data.db.entities.NoteTag
import com.example.mytrip.navigation.Screen
import com.example.mytrip.util.DateUtils
import com.example.mytrip.util.MoneyUtils
import com.example.mytrip.ui.components.DraggableFab
import com.example.mytrip.ui.components.NoteDetailDialog
import com.example.mytrip.ui.components.GlassmorphismCard
import com.example.mytrip.ui.components.MyTripChip
import com.example.mytrip.ui.theme.TripThemeProvider


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllNotesScreen(
    navController: NavController,
    tripId: Long,
    viewModel: AllNotesViewModel = viewModel(factory = AllNotesViewModel.Factory)
) {
    LaunchedEffect(tripId) {
        viewModel.loadTrip(tripId)
    }

    val trip by viewModel.trip.collectAsState()
    val days by viewModel.days.collectAsState()
    val filteredNotes by viewModel.filteredNotes.collectAsState()
    val currentFilter by viewModel.currentFilter.collectAsState()

    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var expandedNote by remember { mutableStateOf<Note?>(null) }
    var isListView by remember { mutableStateOf(false) }

    // Group-by category selected helper
    var activeCategoryGroup by remember { mutableStateOf("ALL") } // "ALL", "DAY", "WEEK", "TAG"

    TripThemeProvider(trip = trip) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Nhật ký & Ảnh",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = trip?.name ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { isListView = !isListView }) {
                        Icon(
                            imageVector = if (isListView) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = "Chuyển chế độ xem"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
            // ─── Filter Categories Row ─────────────────────────────────────────
            ScrollableFilterCategories(
                activeGroup = activeCategoryGroup,
                onGroupSelected = { group ->
                    activeCategoryGroup = group
                    when (group) {
                        "ALL" -> viewModel.setFilter(NoteFilter.All)
                        "DAY" -> {
                            if (days.isNotEmpty()) {
                                viewModel.setFilter(NoteFilter.ByDay(days.first().id))
                            }
                        }
                        "WEEK" -> viewModel.setFilter(NoteFilter.ByWeek(1))
                        "TAG" -> viewModel.setFilter(NoteFilter.ByTag(NoteTag.OTHER))
                    }
                }
            )

            // ─── Sub-filters Row ───────────────────────────────────────────────
            AnimatedSubFilters(
                activeGroup = activeCategoryGroup,
                days = days,
                currentFilter = currentFilter,
                onFilterSelected = { viewModel.setFilter(it) }
            )

            // ─── Main Content Grid ──────────────────────────────────────────────
            if (filteredNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("📷", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Không tìm thấy ghi chú nào.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                androidx.compose.animation.AnimatedContent(
                    targetState = isListView,
                    label = "ViewModeAnimation",
                    modifier = Modifier.fillMaxSize().weight(1f)
                ) { listMode ->
                    if (listMode) {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                val noteDay = days.find { it.id == note.dayId }
                                AllNoteListRow(
                                    note = note,
                                    dayNumber = noteDay?.dayNumber,
                                    onClick = { expandedNote = note },
                                    onLongClick = { noteToDelete = note }
                                )
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                val noteDay = days.find { it.id == note.dayId }
                                AllNoteCard(
                                    note = note,
                                    dayNumber = noteDay?.dayNumber,
                                    onClick = { expandedNote = note },
                                    onLongClick = { noteToDelete = note }
                                )
                            }
                        }
                    }
                }
            }
        }
        DraggableFab(
            onClick = {
                navController.navigate(Screen.AddNote.createRoute(tripId, null))
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
}

    // ── Delete confirmation dialog ────────────────────────────────────
    if (noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Xoá ghi chú này?", fontWeight = FontWeight.Bold) },
            text = { Text("Mọi dữ liệu chi phí và hình ảnh liên kết với ghi chú này sẽ bị xoá vĩnh viễn.") },
            confirmButton = {
                Button(
                    onClick = {
                        noteToDelete?.let { viewModel.deleteNote(it) }
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xoá") }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Huỷ") }
            }
        )
    }

    // ── Expanded detail dialog ────────────────────────────────────────
    if (expandedNote != null) {
        val note = expandedNote!!
        val noteDay = days.find { it.id == note.dayId }
        NoteDetailDialog(
            note = note,
            dayNumber = noteDay?.dayNumber,
            onDismiss = { expandedNote = null }
        )
    }
}

// ─── Filter Category Buttons Row ───────────────────────────────────────────
@Composable
private fun ScrollableFilterCategories(
    activeGroup: String,
    onGroupSelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            val isSelected = activeGroup == "ALL"
            MyTripChip(
                text = "Tất cả",
                selected = isSelected,
                onClick = { onGroupSelected("ALL") }
            )
        }
        item {
            val isSelected = activeGroup == "DAY"
            MyTripChip(
                text = "Lọc theo ngày",
                selected = isSelected,
                onClick = { onGroupSelected("DAY") }
            )
        }
        item {
            val isSelected = activeGroup == "WEEK"
            MyTripChip(
                text = "Lọc theo tuần",
                selected = isSelected,
                onClick = { onGroupSelected("WEEK") }
            )
        }
        item {
            val isSelected = activeGroup == "TAG"
            MyTripChip(
                text = "Lọc theo loại",
                selected = isSelected,
                onClick = { onGroupSelected("TAG") }
            )
        }
    }
}

// ─── Sub-filters display ───────────────────────────────────────────────────
@Composable
private fun AnimatedSubFilters(
    activeGroup: String,
    days: List<Day>,
    currentFilter: NoteFilter,
    onFilterSelected: (NoteFilter) -> Unit
) {
    AnimatedVisibility(visible = activeGroup != "ALL") {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            when (activeGroup) {
                "DAY" -> {
                    val sortedDays = days.sortedBy { it.dayNumber }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedDays, key = { it.id }) { day ->
                            val selected = currentFilter is NoteFilter.ByDay && currentFilter.dayId == day.id
                            MyTripChip(
                                text = "Ngày ${day.dayNumber} (${DateUtils.formatDate(day.date)})",
                                selected = selected,
                                onClick = { onFilterSelected(NoteFilter.ByDay(day.id)) }
                            )
                        }
                    }
                }
                "WEEK" -> {
                    // Maximum of 5 weeks for 30-day road trip
                    val totalWeeks = if (days.isEmpty()) 1 else ((days.size - 1) / 7) + 1
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items((1..totalWeeks).toList()) { week ->
                            val selected = currentFilter is NoteFilter.ByWeek && currentFilter.weekNumber == week
                            val weekDays = days.filter { it.dayNumber in ((week - 1) * 7 + 1)..(week * 7) }
                            val labelText = if (weekDays.isNotEmpty()) {
                                val startDay = weekDays.minBy { it.dayNumber }
                                val endDay = weekDays.maxBy { it.dayNumber }
                                val startStr = DateUtils.formatDate(startDay.date).substring(0, 5) // dd/MM
                                val endStr = DateUtils.formatDate(endDay.date).substring(0, 5) // dd/MM
                                "Tuần $week ($startStr - $endStr)"
                            } else {
                                "Tuần $week"
                            }
                            MyTripChip(
                                text = labelText,
                                selected = selected,
                                onClick = { onFilterSelected(NoteFilter.ByWeek(week)) }
                            )
                        }
                    }
                }
                "TAG" -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(NoteTag.entries) { tag ->
                            val selected = currentFilter is NoteFilter.ByTag && currentFilter.tag == tag
                            MyTripChip(
                                text = "${tag.icon} ${tag.label}",
                                selected = selected,
                                onClick = { onFilterSelected(NoteFilter.ByTag(tag)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Compact Note Card for Grid ────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AllNoteCard(
    note: Note,
    dayNumber: Int?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    GlassmorphismCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column {
            // Photo Thumbnail
            if (note.photoPath != null) {
                AsyncImage(
                    model = note.photoPath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(70.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(note.tag.icon, fontSize = 28.sp)
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                // Row for Tag Icon & Day Number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${note.tag.icon} ${note.tag.label}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (dayNumber != null) {
                        Text(
                            text = "N$dayNumber",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // Title
                Text(
                    text = if (note.name.isNotBlank()) note.name else note.comment,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(32.dp)
                )

                Spacer(Modifier.height(6.dp))

                // Rating & Cost
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        repeat(5) { i ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i < note.rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    if (note.cost > 0) {
                        Text(
                            text = MoneyUtils.formatShort(note.cost),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }
    }
}

// ─── List Row View ────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllNoteListRow(
    note: Note,
    dayNumber: Int?,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    GlassmorphismCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Note image or icon
            val firstImage = try {
                val arr = org.json.JSONArray(note.photoPaths)
                if (arr.length() > 0) arr.getString(0) else null
            } catch (_: Exception) { null }

            if (firstImage != null) {
                coil.compose.AsyncImage(
                    model = firstImage,
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(note.tag.icon, fontSize = 24.sp)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = note.tag.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    if (note.cost > 0.0) {
                        Text(
                            text = MoneyUtils.formatShort(note.cost),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (note.name.isNotBlank()) {
                    Text(
                        text = note.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else if (note.comment.isNotBlank()) {
                    Text(
                        text = note.comment,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(note.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (dayNumber != null) {
                        Text(
                            text = "Ngày $dayNumber",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
