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
import com.example.mytrip.ui.components.ActivityEditSheet
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
            title = { Text("XÃ³a hoáº¡t Ä‘á»™ng?", fontWeight = FontWeight.Bold) },
            text = { Text("Báº¡n cÃ³ cháº¯c cháº¯n muá»‘n xÃ³a hoáº¡t Ä‘á»™ng \"${act.name}\" khá»i lá»‹ch trÃ¬nh ngÃ y nÃ y?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteActivity(act)
                        activityToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("XÃ³a") }
            },
            dismissButton = {
                TextButton(onClick = { activityToDelete = null }) { Text("Há»§y") }
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
            title = { Text("TÃ¹y chá»n nháº­t kÃ½", fontWeight = FontWeight.Bold) },
            text = { Text("Báº¡n muá»‘n lÃ m gÃ¬ vá»›i nháº­t kÃ½ nÃ y?") },
            confirmButton = {
                Button(onClick = {
                    val nId = note.id
                    noteOptionsTarget = null
                    navController.navigate(Screen.AddNote.createRoute(tripId, todayDay?.id, nId))
                }) { Text("Sá»­a") }
            },
            dismissButton = {
                Button(
                    onClick = {
                        noteToDelete = note
                        noteOptionsTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("XÃ³a") }
            }
        )
    }

    if (noteToDelete != null) {
        val note = noteToDelete!!
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("XÃ³a nháº­t kÃ½?", fontWeight = FontWeight.Bold) },
            text = { Text("Báº¡n cÃ³ cháº¯c cháº¯n muá»‘n xÃ³a nháº­t kÃ½ nÃ y khÃ´ng?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteNote(note)
                        noteToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("XÃ³a") }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) { Text("Há»§y") }
            }
        )
    }

    TripThemeProvider(trip = trip) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = trip?.name ?: "HÃ´m nay",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay láº¡i")
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
                        Icon(Icons.Filled.CalendarMonth, contentDescription = "Chá»n ngÃ y", tint = MaterialTheme.colorScheme.primary)
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
            // â”€â”€ Day navigator â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                DayNavigatorRow(
                    selectedIndex = selectedIndex,
                    onSelectDay = { viewModel.changeDay(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            // â”€â”€ Date header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                DateHeaderCard(
                    day = todayDay,
                    selectedIndex = selectedIndex,
                    onEditDay = { showEditDayDialog = true },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // â”€â”€ Empty state â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            if (todayDay == null) {
                item {
                    EmptyDayState(selectedIndex = selectedIndex)
                }
            } else {
                // â”€â”€ Activities timeline â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                if (activities.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "ðŸ“‹ Lá»‹ch trÃ¬nh",
                            action = {
                                TextButton(
                                    onClick = { showAddSheet = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("ThÃªm", style = MaterialTheme.typography.labelLarge)
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
                                            contentDescription = "ThÃªm hoáº¡t Ä‘á»™ng",
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
                                Text("+ ThÃªm hoáº¡t Ä‘á»™ng", style = MaterialTheme.typography.labelMedium)
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

                // â”€â”€ Notes section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
                item {
                    SectionHeader(
                        title = "ðŸ“ Ghi chÃº ngÃ y nÃ y",
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

// â”€â”€ Day navigator â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun DayNavigatorRow(
    selectedIndex: Int,
    onSelectDay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf("â—€ HÃ´m qua", "HÃ´m nay", "NgÃ y mai â–¶")

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

// â”€â”€ Date header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
                    text = "ðŸ“ HÃ´m nay",
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
                            contentDescription = "Sá»­a",
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
                            text = "(Cháº¡m Ä‘á»ƒ thÃªm lá»™ trÃ¬nh/Ä‘á»‹a Ä‘iá»ƒm)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Sá»­a",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "NgÃ y ${it.dayNumber}",
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

// â”€â”€ Note card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
                        text = "ðŸ’° ${MoneyUtils.formatShort(note.cost)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// â”€â”€ Section header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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

// â”€â”€ Empty states â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun EmptyDayState(selectedIndex: Int) {
    val message = when (selectedIndex) {
        0 -> "KhÃ´ng cÃ³ dá»¯ liá»‡u cho ngÃ y hÃ´m qua."
        2 -> "ChÆ°a cÃ³ káº¿ hoáº¡ch cho ngÃ y mai."
        else -> "HÃ´m nay khÃ´ng cÃ³ trong lá»‹ch trÃ¬nh chuyáº¿n Ä‘i."
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ðŸ“…", fontSize = 48.sp, textAlign = TextAlign.Center)
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
            Text("ðŸ—“ï¸", fontSize = 32.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "KhÃ´ng cÃ³ hoáº¡t Ä‘á»™ng nÃ o Ä‘Æ°á»£c lÃªn káº¿ hoáº¡ch.",
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
                Text("ThÃªm hoáº¡t Ä‘á»™ng")
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
            Text("ðŸ“", fontSize = 28.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "ChÆ°a cÃ³ ghi chÃº nÃ o.\nNháº¥n + Ä‘á»ƒ thÃªm note Ä‘áº§u tiÃªn!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// â”€â”€â”€ Edit Day Title Dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun EditDayTitleDialog(
    day: Day,
    onSave: (Day) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(day.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sá»­a thÃ´ng tin ngÃ y", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ThÃ´ng tin hÃ nh trÃ¬nh / Ä‘á»‹a Ä‘iá»ƒm trong ngÃ y:", style = MaterialTheme.typography.labelMedium)
                MyTripTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "VD: PhÃº Thá» â†’ Nghá»‡ An | 380km",
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(day.copy(title = title.trim()))
            }) { Text("LÆ°u") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Há»§y") }
        }
    )
}

