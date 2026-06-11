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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessTime
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.mytrip.ui.components.NoteDetailDialog
import com.example.mytrip.ui.components.ScheduleTimelineList
import com.example.mytrip.ui.components.MyTripChip
import com.example.mytrip.ui.components.MyTripTextField
import com.example.mytrip.ui.components.MyTripPrimaryButton
import com.example.mytrip.ui.components.MyTripSecondaryButton
import com.example.mytrip.ui.components.GlassmorphismCard
import com.example.mytrip.ui.theme.TripThemeProvider
import com.example.mytrip.ui.theme.spacing
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.InputChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import android.net.Uri
import android.content.Intent
import org.json.JSONArray
import com.example.mytrip.data.db.entities.ActivityType
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.mytrip.data.db.entities.Day
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.launch

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

    var selectedNoteForDetail by remember { mutableStateOf<Note?>(null) }
    var noteOptionsTarget by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showEditDayDialog by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<Activity?>(null) }
    var activityToDelete by remember { mutableStateOf<Activity?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()
    var dragActivities by remember(activities) { mutableStateOf(activities) }

    // val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to -> ... }
    // val draggingKey = reorderableState.draggingItemKey
    // LaunchedEffect(draggingKey) { ... }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    if (showEditDayDialog && todayDay != null) {
        EditDayTitleDialog(
            day = todayDay!!,
            onSave = { updatedDay ->
                viewModel.updateDay(updatedDay)
                showEditDayDialog = false
            },
            onDismiss = { showEditDayDialog = false }
        )
    }

    if (activityToEdit != null || showAddSheet) {
        ModalBottomSheet(
            onDismissRequest = { 
                scope.launch { sheetState.hide() }
                activityToEdit = null
                showAddSheet = false
            },
            sheetState = sheetState,
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ActivityEditSheet(
                dayId = todayDay?.id ?: 0L,
                existingActivity = activityToEdit,
                insertAfterIndex = if (activityToEdit == null) activities.size - 1 else 0,
                onSave = { activity ->
                    if (activityToEdit == null) {
                        viewModel.insertActivityAfter(activity, activities.size - 1)
                    } else {
                        viewModel.updateActivity(activity)
                    }
                    scope.launch { sheetState.hide() }
                    activityToEdit = null
                    showAddSheet = false
                },
                onDismiss = { 
                    scope.launch { sheetState.hide() }
                    activityToEdit = null
                    showAddSheet = false
                }
            )
        }
    }

    if (activityToDelete != null) {
        val act = activityToDelete!!
        AlertDialog(
            onDismissRequest = { activityToDelete = null },
            title = { Text("Xóa hoạt động?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa hoạt động \"${act.name}\" khỏi lịch trình ngày này?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteActivity(act)
                        activityToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { activityToDelete = null }) { Text("Hủy") }
            }
        )
    }

    selectedNoteForDetail?.let { note ->
        NoteDetailDialog(
            note = note,
            dayNumber = todayDay?.dayNumber,
            onDismiss = { selectedNoteForDetail = null }
        )
    }

    if (noteOptionsTarget != null) {
        val note = noteOptionsTarget!!
        AlertDialog(
            onDismissRequest = { noteOptionsTarget = null },
            title = { Text("Tùy chọn nhật ký", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn muốn làm gì với nhật ký này?") },
            confirmButton = {
                Button(onClick = {
                    val nId = note.id
                    noteOptionsTarget = null
                    navController.navigate(Screen.AddNote.createRoute(tripId, todayDay?.id, nId))
                }) { Text("Sửa") }
            },
            dismissButton = {
                Button(
                    onClick = {
                        noteToDelete = note
                        noteOptionsTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            }
        )
    }

    if (noteToDelete != null) {
        val note = noteToDelete!!
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Xóa nhật ký?", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn xóa nhật ký này không?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(note)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Hủy") }
            }
        )
    }

    TripThemeProvider(trip = trip) {
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
                actions = {
                    val ctx = LocalContext.current
                    IconButton(onClick = {
                        val calendar = java.util.Calendar.getInstance()
                        android.app.DatePickerDialog(
                            ctx,
                            { _, year, month, dayOfMonth ->
                                val cal = java.util.Calendar.getInstance()
                                cal.set(year, month, dayOfMonth, 0, 0, 0)
                                cal.set(java.util.Calendar.MILLISECOND, 0)
                                viewModel.jumpToDate(cal.timeInMillis)
                            },
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH),
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    }) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Chọn ngày", tint = MaterialTheme.colorScheme.primary)
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
                state = lazyListState,
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
                    onEditDay = { showEditDayDialog = true },
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
                            action = {
                                TextButton(
                                    onClick = { showAddSheet = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Thêm", style = MaterialTheme.typography.labelLarge)
                                }
                            },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    item {
                        ScheduleTimelineList(
                            activities = activities,
                            onReorder = { viewModel.reorderActivities(it) },
                            onEdit = { activityToEdit = it },
                            onDelete = { activityToDelete = it },
                            onStatusChange = { act, next -> viewModel.updateActivityStatus(act.id, next) }
                        )
                    }
                    item {
                        // Draw bottom connector to add button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(32.dp)
                            ) {
                                Surface(
                                    onClick = { showAddSheet = true },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Thêm hoạt động",
                                            modifier = Modifier.size(11.dp),
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(
                                onClick = { showAddSheet = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text("+ Thêm hoạt động", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                } else {
                    item {
                        EmptyActivitiesState(
                            onAddClick = { showAddSheet = true },
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
                                    onClick = { selectedNoteForDetail = note },
                                    onLongClick = { noteOptionsTarget = note },
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
    onEditDay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetMillis = when (selectedIndex) {
        0 -> DateUtils.todayMillis() - 86_400_000L
        2 -> DateUtils.todayMillis() + 86_400_000L
        else -> DateUtils.todayMillis()
    }
    val displayMillis = day?.date ?: targetMillis

    GlassmorphismCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onEditDay() }
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = it.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Sửa",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "(Chạm để thêm lộ trình/địa điểm)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Sửa",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                    }
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = if (isActive) activeColor else MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GlassmorphismCard(
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Column {
            // Photo
            val displayPhoto = note.photoPaths.firstOrNull() ?: note.photoPath
            if (displayPhoto != null) {
                AsyncImage(
                    model = displayPhoto,
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
                    shape = RoundedCornerShape(50)
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
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        action?.invoke(this)
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
private fun EmptyActivitiesState(
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassmorphismCard(
        modifier = modifier.fillMaxWidth()
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
            Spacer(Modifier.height(10.dp))
            MyTripSecondaryButton(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Thêm hoạt động")
            }
        }
    }
}

@Composable
private fun EmptyNotesState(modifier: Modifier = Modifier) {
    GlassmorphismCard(
        modifier = modifier.fillMaxWidth()
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

// ─── Edit Day Title Dialog ──────────────────────────────────────────────────
@Composable
private fun EditDayTitleDialog(
    day: Day,
    onSave: (Day) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(day.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sửa thông tin ngày", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Thông tin hành trình / địa điểm trong ngày:", style = MaterialTheme.typography.labelMedium)
                MyTripTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "VD: Phú Thọ → Nghệ An | 380km",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(day.copy(title = title.trim()))
            }) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

// ─── Helpers copied from ItineraryScreen ──────────────────────────────────────
private fun parseSpots(json: String): List<String> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) { emptyList() }
}

private fun spotsToJson(spots: List<String>): String {
    val arr = JSONArray()
    spots.forEach { arr.put(it) }
    return arr.toString()
}

private data class MoneyShortcut(val label: String, val valueK: Long)
private val hotelShortcuts = listOf(
    MoneyShortcut("300k", 300L),
    MoneyShortcut("500k", 500L),
    MoneyShortcut("800k", 800L),
    MoneyShortcut("1M", 1_000L),
    MoneyShortcut("1.5M", 1_500L),
    MoneyShortcut("2M", 2_000L)
)

private val transitSuggestions = listOf("Đi xe máy", "Đi ô tô", "Bắt xe khách", "Đi tàu hỏa", "Bay", "Đi thuyền", "Thuê xe tuk-tuk")
private val sightseeingSuggestions = listOf("Bãi biển", "Hồ", "Núi", "Chùa", "Đền", "Phố cổ", "Công viên", "Bảo tàng", "Thác nước", "Hang động")
private val mealSuggestions = listOf("Ăn sáng", "Ăn trưa", "Ăn tối", "Quán hải sản", "Quán bún bò", "Quán phở", "Nhà hàng địa phương", "Coffee break")
private val accommodationSuggestions = listOf("Check-in khách sạn", "Check-out khách sạn", "Nhà nghỉ", "Homestay", "Resort")
private val activitySuggestions = listOf("Tắm biển", "Leo núi", "Chèo thuyền kayak", "Cắm trại", "Mua sắm", "Thăm bạn bè", "Thuê xe đạp")

private fun suggestionsFor(type: ActivityType): List<String> = when (type) {
    ActivityType.TRANSIT -> transitSuggestions
    ActivityType.SIGHTSEEING -> sightseeingSuggestions
    ActivityType.MEAL -> mealSuggestions
    ActivityType.ACCOMMODATION -> accommodationSuggestions
    ActivityType.ACTIVITY -> activitySuggestions
}

// ─── ActivityEditSheet (Copied from ItineraryScreen) ─────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivityEditSheet(
    dayId: Long,
    existingActivity: Activity?,
    insertAfterIndex: Int,
    onSave: (Activity) -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Activity type selection
    var selectedType by rememberSaveable { mutableStateOf(existingActivity?.activityType ?: ActivityType.TRANSIT) }

    // Common fields
    var name by rememberSaveable { mutableStateOf(existingActivity?.name ?: "") }
    var notes by rememberSaveable { mutableStateOf(existingActivity?.notes ?: "") }
    var nameError by rememberSaveable { mutableStateOf(false) }

    // Time fields (TRANSIT, SIGHTSEEING, ACCOMMODATION, ACTIVITY)
    var departureTime by rememberSaveable { mutableStateOf(existingActivity?.departureTime ?: "") }
    var arrivalTime by rememberSaveable { mutableStateOf(existingActivity?.arrivalTime ?: "") }
    var departureTimeError by rememberSaveable { mutableStateOf(false) }
    var arrivalTimeError by rememberSaveable { mutableStateOf(false) }

    val departureTimeValue = remember(departureTime) {
        TextFieldValue(departureTime, TextRange(departureTime.length))
    }
    val arrivalTimeValue = remember(arrivalTime) {
        TextFieldValue(arrivalTime, TextRange(arrivalTime.length))
    }

    // Expandable detail view toggle
    var showMoreDetails by rememberSaveable { mutableStateOf(false) }

    // TRANSIT fields
    var distanceText by rememberSaveable {
        mutableStateOf(if ((existingActivity?.distanceKm ?: 0.0) > 0) "%.1f".format(existingActivity?.distanceKm) else "")
    }
    var mapsLink by rememberSaveable { mutableStateOf(existingActivity?.mapsLink ?: "") }

    // SIGHTSEEING fields
    var checkInSpots by rememberSaveable { mutableStateOf(parseSpots(existingActivity?.checkInSpots ?: "")) }
    var spotInput by rememberSaveable { mutableStateOf("") }

    // ACCOMMODATION fields
    var hotelName by rememberSaveable { mutableStateOf(existingActivity?.hotelName ?: "") }
    var hotelPriceText by rememberSaveable {
        mutableStateOf(if ((existingActivity?.hotelPricePlanned ?: 0L) > 0L) existingActivity!!.hotelPricePlanned.toString() else "")
    }

    // Helper functions for time validation & formatting
    fun isValidTime(time: String): Boolean {
        val clean = time.trim()
        if (clean.isEmpty()) return true
        val regex = Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")
        return regex.matches(clean)
    }

    fun formatDigitsToTime(digits: String): String {
        return when (digits.length) {
            0 -> ""
            1 -> digits
            2 -> digits
            3 -> "${digits.substring(0, 2)}:${digits.substring(2)}"
            else -> "${digits.substring(0, 2)}:${digits.substring(2, 4)}"
        }
    }

    fun onTimeValueChange(newVal: String): String {
        val digits = newVal.filter { it.isDigit() }.take(4)
        return formatDigitsToTime(digits)
    }

    fun addTimeToFormatted(time: String, minutesToAdd: Int): String {
        val parts = time.split(":")
        if (parts.size != 2) return ""
        val hour = parts[0].toIntOrNull() ?: return ""
        val minute = parts[1].toIntOrNull() ?: return ""
        
        val totalMinutes = hour * 60 + minute + minutesToAdd
        val newHour = (totalMinutes / 60) % 24
        val newMinute = totalMinutes % 60
        return String.format(Locale.US, "%02d:%02d", newHour, newMinute)
    }

    fun showTimePicker(currentTime: String, onTimeSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        val initHour = currentTime.split(":").firstOrNull()?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY)
        val initMinute = currentTime.split(":").lastOrNull()?.toIntOrNull() ?: cal.get(Calendar.MINUTE)
        
        android.app.TimePickerDialog(
            context,
            { _, hour, minute ->
                val formatted = String.format(Locale.US, "%02d:%02d", hour, minute)
                onTimeSelected(formatted)
            },
            initHour,
            initMinute,
            true // 24 hours format
        ).show()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Sheet handle
        Box(modifier = Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outlineVariant))

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (existingActivity == null) "Tạo hoạt động mới" else "Chỉnh sửa hoạt động",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Loại hoạt động ────────────────────────────────────────────
        Text("Loại hoạt động", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            ActivityType.entries.forEach { type ->
                val isSelected = selectedType == type
                MyTripChip(
                    text = "${type.icon} ${type.label}",
                    selected = isSelected,
                    onClick = {
                        selectedType = type
                        name = "" // reset name suggestions on type change
                    }
                )
            }
        }

        // ── Tên hoạt động ─────────────────────────────────────────────
        MyTripTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = "${selectedType.icon} Tên ${selectedType.label} *",
            isError = nameError,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Gợi ý tên
        val suggestions = suggestionsFor(selectedType)
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            suggestions.forEach { s ->
                SuggestionChip(onClick = { name = s }, label = { Text(s, style = MaterialTheme.typography.labelSmall) })
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Giờ (Basic - Always Visible except MEAL) ──────────────────
        if (selectedType != ActivityType.MEAL) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MyTripTextField(
                    value = departureTimeValue,
                    onValueChange = { 
                        departureTime = onTimeValueChange(it.text)
                        departureTimeError = false 
                    },
                    label = if (selectedType == ActivityType.ACCOMMODATION) "Check-in" else "Giờ đi",
                    placeholder = "HH:mm",
                    isError = departureTimeError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = {
                        IconButton(onClick = {
                            showTimePicker(departureTime) { departureTime = it; departureTimeError = false }
                        }) {
                            Icon(Icons.Filled.AccessTime, contentDescription = "Chọn giờ")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
                MyTripTextField(
                    value = arrivalTimeValue,
                    onValueChange = { 
                        arrivalTime = onTimeValueChange(it.text)
                        arrivalTimeError = false 
                    },
                    label = if (selectedType == ActivityType.ACCOMMODATION) "Check-out" else "Giờ đến",
                    placeholder = "HH:mm",
                    isError = arrivalTimeError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = {
                        IconButton(onClick = {
                            showTimePicker(arrivalTime) { arrivalTime = it; arrivalTimeError = false }
                        }) {
                            Icon(Icons.Filled.AccessTime, contentDescription = "Chọn giờ")
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Smart time offset suggestion chips
            if (isValidTime(departureTime) && departureTime.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(listOf(
                        "+30p" to 30,
                        "+1h" to 60,
                        "+2h" to 120,
                        "+3h" to 180,
                        "+4h" to 240
                    )) { (label, mins) ->
                        SuggestionChip(
                            onClick = {
                                val suggested = addTimeToFormatted(departureTime, mins)
                                if (suggested.isNotEmpty()) {
                                    arrivalTime = suggested
                                    arrivalTimeError = false
                                }
                            },
                            label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ACCOMMODATION: Khách sạn (Tên là Basic - Always Visible)
        if (selectedType == ActivityType.ACCOMMODATION) {
            MyTripTextField(
                value = hotelName,
                onValueChange = { hotelName = it },
                label = "Tên khách sạn",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // ── Nút mở rộng thông tin chi tiết ────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { showMoreDetails = !showMoreDetails }) {
                Icon(
                    imageVector = if (showMoreDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (showMoreDetails) "Ẩn bớt thông tin chi tiết" else "Thêm thông tin chi tiết")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // ── Phân hệ thông tin chi tiết ──────────────────────────────────
        androidx.compose.animation.AnimatedVisibility(
            visible = showMoreDetails,
            enter = androidx.compose.animation.expandVertically(),
            exit = androidx.compose.animation.shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ACCOMMODATION: Giá phòng (Detailed)
                if (selectedType == ActivityType.ACCOMMODATION) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MyTripTextField(
                            value = hotelPriceText,
                            onValueChange = { hotelPriceText = it },
                            label = "Giá phòng dự kiến (nghìn ₫)",
                            placeholder = "VD: 500 = 500.000 ₫",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            hotelShortcuts.forEach { sc ->
                                SuggestionChip(onClick = { hotelPriceText = sc.valueK.toString() }, label = { Text(sc.label, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                }

                // TRANSIT: Khoảng cách (Detailed)
                if (selectedType == ActivityType.TRANSIT) {
                    MyTripTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it },
                        label = "Khoảng cách (km)",
                        placeholder = "0.0",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // SIGHTSEEING: Check-in spots (Detailed)
                if (selectedType == ActivityType.SIGHTSEEING) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Điểm check-in cần ghé", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (checkInSpots.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                checkInSpots.forEach { spot ->
                                    InputChip(
                                        selected = false,
                                        onClick = { checkInSpots = checkInSpots.filter { it != spot } },
                                        label = { Text(spot, style = MaterialTheme.typography.labelSmall) },
                                        trailingIcon = {
                                            Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp))
                                        }
                                    )
                                }
                            }
                        }
                        MyTripTextField(
                            value = spotInput,
                            onValueChange = { spotInput = it },
                            label = "Thêm điểm check-in",
                            placeholder = "Nhập tên rồi nhấn Enter",
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val t = spotInput.trim()
                                if (t.isNotBlank() && !checkInSpots.contains(t)) checkInSpots = checkInSpots + t
                                spotInput = ""
                            }),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Link Google Maps (Detailed, shown for Transit, Sightseeing, Accommodation)
                if (selectedType == ActivityType.TRANSIT || selectedType == ActivityType.SIGHTSEEING || selectedType == ActivityType.ACCOMMODATION) {
                    MyTripTextField(
                        value = mapsLink,
                        onValueChange = { mapsLink = it },
                        label = "Link Google Maps",
                        placeholder = "https://maps.google.com/...",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        trailingIcon = {
                            if (mapsLink.isNotBlank()) {
                                IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsLink))) }) {
                                    Icon(Icons.Filled.OpenInNew, "Mở bản đồ", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Ghi chú thêm (Detailed)
                MyTripTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Ghi chú thêm",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MyTripSecondaryButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Huỷ") }
            MyTripPrimaryButton(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@MyTripPrimaryButton }
                    
                    val isDepValid = isValidTime(departureTime)
                    val isArrValid = isValidTime(arrivalTime)
                    if (!isDepValid) { departureTimeError = true }
                    if (!isArrValid) { arrivalTimeError = true }
                    if (!isDepValid || !isArrValid) return@MyTripPrimaryButton

                    focusManager.clearFocus()
                    val activity = Activity(
                        id = existingActivity?.id ?: 0L,
                        dayId = dayId,
                        orderIndex = existingActivity?.orderIndex ?: 0,
                        activityType = selectedType,
                        name = name.trim(),
                        departureTime = departureTime.trim(),
                        arrivalTime = arrivalTime.trim(),
                        distanceKm = distanceText.toDoubleOrNull() ?: 0.0,
                        hotelName = hotelName.trim(),
                        hotelPricePlanned = hotelPriceText.toLongOrNull() ?: 0L,
                        checkInSpots = spotsToJson(checkInSpots),
                        mapsLink = mapsLink.trim(),
                        notes = notes.trim(),
                        status = existingActivity?.status ?: ActivityStatus.PENDING,
                        actualDepartureTime = existingActivity?.actualDepartureTime ?: "",
                        actualArrivalTime = existingActivity?.actualArrivalTime ?: "",
                        actualNotes = existingActivity?.actualNotes ?: ""
                    )
                    onSave(activity)
                },
                modifier = Modifier.weight(1f)
            ) { Text("Lưu") }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
