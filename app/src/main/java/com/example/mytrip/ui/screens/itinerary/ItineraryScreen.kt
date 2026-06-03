package com.example.mytrip.ui.screens.itinerary

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mytrip.data.db.entities.Activity
import com.example.mytrip.data.db.entities.ActivityStatus
import com.example.mytrip.data.db.entities.Day
import com.example.mytrip.navigation.Screen
import com.example.mytrip.util.DateUtils
import com.example.mytrip.util.MoneyUtils
import kotlinx.coroutines.launch
import org.json.JSONArray

// ─── Palette for day number circles ───────────────────────────────────────────
private val dayColors = listOf(
    Color(0xFF6750A4), Color(0xFF0288D1), Color(0xFF2E7D32),
    Color(0xFFC62828), Color(0xFFE65100), Color(0xFF6A1B9A),
    Color(0xFF00695C), Color(0xFF1565C0), Color(0xFFAD1457)
)

private fun dayColor(dayNumber: Int): Color = dayColors[(dayNumber - 1) % dayColors.size]

private fun statusColor(status: ActivityStatus): Color = when (status) {
    ActivityStatus.PENDING -> Color(0xFF9E9E9E)
    ActivityStatus.DONE    -> Color(0xFF4CAF50)
    ActivityStatus.SKIPPED -> Color(0xFFF44336)
    ActivityStatus.CHANGED -> Color(0xFFFF9800)
}

// ─── Helper: parse checkInSpots JSON ──────────────────────────────────────────
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

// ─── Money shortcuts for hotel price ─────────────────────────────────────────
private data class MoneyShortcut(val label: String, val valueK: Long)
private val hotelShortcuts = listOf(
    MoneyShortcut("300k", 300L),
    MoneyShortcut("500k", 500L),
    MoneyShortcut("800k", 800L),
    MoneyShortcut("1M", 1_000L),
    MoneyShortcut("1.5M", 1_500L),
    MoneyShortcut("2M", 2_000L)
)

// ─── Main screen ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun ItineraryScreen(
    navController: NavController,
    tripId: Long,
    viewModel: ItineraryViewModel = viewModel(factory = ItineraryViewModel.Factory)
) {
    LaunchedEffect(tripId) { viewModel.loadData(tripId) }

    val trip by viewModel.trip.collectAsState()
    val days by viewModel.days.collectAsState()
    val activitiesMap by viewModel.activitiesMap.collectAsState()
    val expandedDays by viewModel.expandedDays.collectAsState()

    // Bottom sheet state
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<Activity?>(null) }
    var sheetDayId by rememberSaveable { mutableStateOf(0L) }

    // Delete dialog state
    var deleteTarget by remember { mutableStateOf<Activity?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = trip?.name ?: "Lịch trình",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${days.size} ngày",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.expandAll() }) {
                        Icon(
                            Icons.Filled.UnfoldMore,
                            contentDescription = "Mở rộng tất cả",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { viewModel.collapseAll() }) {
                        Icon(
                            Icons.Filled.UnfoldLess,
                            contentDescription = "Thu gọn tất cả",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        if (days.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Chưa có ngày nào trong lịch trình",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                val sortedDays = days.sortedBy { it.dayNumber }
                items(sortedDays, key = { it.id }) { day ->
                    val activities = activitiesMap[day.id] ?: emptyList()
                    val isExpanded = expandedDays.contains(day.id)

                    DaySection(
                        day = day,
                        activities = activities,
                        isExpanded = isExpanded,
                        onToggleExpand = { viewModel.toggleDayExpanded(day.id) },
                        onAddActivity = {
                            sheetDayId = day.id
                            editingActivity = null
                            showSheet = true
                        },
                        onEditActivity = { act ->
                            sheetDayId = day.id
                            editingActivity = act
                            showSheet = true
                        },
                        onDeleteActivity = { act -> deleteTarget = act },
                        onStatusChange = { act, status ->
                            viewModel.updateActivityStatus(act.id, status)
                        }
                    )
                }
            }
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────────
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xoá hoạt động?") },
            text = {
                Text("Bạn có chắc muốn xoá \"${deleteTarget?.name}\" không? Hành động này không thể hoàn tác.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteTarget?.let { viewModel.deleteActivity(it) }
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Xoá") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Huỷ") }
            }
        )
    }

    // ── Add / Edit bottom sheet ───────────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                scope.launch { sheetState.hide() }
                showSheet = false
            },
            sheetState = sheetState,
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ActivityEditSheet(
                dayId = sheetDayId,
                existingActivity = editingActivity,
                onSave = { activity ->
                    if (editingActivity == null) {
                        viewModel.addActivity(activity)
                    } else {
                        viewModel.updateActivity(activity)
                    }
                    scope.launch { sheetState.hide() }
                    showSheet = false
                },
                onDismiss = {
                    scope.launch { sheetState.hide() }
                    showSheet = false
                }
            )
        }
    }
}

// ─── Day section ──────────────────────────────────────────────────────────────
@Composable
private fun DaySection(
    day: Day,
    activities: List<Activity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddActivity: () -> Unit,
    onEditActivity: (Activity) -> Unit,
    onDeleteActivity: (Activity) -> Unit,
    onStatusChange: (Activity, ActivityStatus) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Day header card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { onToggleExpand() },
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored circle with day number
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(dayColor(day.dayNumber)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "N${day.dayNumber}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Date and title
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateUtils.formatFull(day.date),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (day.title.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = day.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${activities.size} hoạt động",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Expand / collapse chevron
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp
                    else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Expanded activities
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, end = 16.dp, bottom = 4.dp)
            ) {
                activities.forEach { activity ->
                    ActivityRow(
                        activity = activity,
                        onEdit = { onEditActivity(activity) },
                        onDelete = { onDeleteActivity(activity) },
                        onStatusChange = { status -> onStatusChange(activity, status) }
                    )
                }

                // Add activity button
                TextButton(
                    onClick = onAddActivity,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Thêm hoạt động")
                }
            }
        }
    }
}

// ─── Activity row ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ActivityRow(
    activity: Activity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStatusChange: (ActivityStatus) -> Unit
) {
    val context = LocalContext.current
    var showStatusMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = { onDelete() }
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Time column
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(52.dp)
            ) {
                if (activity.departureTime.isNotBlank()) {
                    Text(
                        text = activity.departureTime,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (activity.departureTime.isNotBlank() && activity.arrivalTime.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(10.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
                if (activity.arrivalTime.isNotBlank()) {
                    Text(
                        text = activity.arrivalTime,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main content
            Column(modifier = Modifier.weight(1f)) {
                // Activity name
                Text(
                    text = activity.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Hotel
                if (activity.hotelName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🏨 ${activity.hotelName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (activity.hotelPricePlanned > 0) {
                            Text(
                                text = "  •  ${MoneyUtils.formatShort(MoneyUtils.inputToVnd(activity.hotelPricePlanned))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // Distance
                if (activity.distanceKm > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "📍 ${"%.1f".format(activity.distanceKm)} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Check-in spots
                val spots = parseSpots(activity.checkInSpots)
                if (spots.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        spots.forEach { spot ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Text(
                                    text = "📷 $spot",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Notes
                if (activity.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = activity.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Maps link
                if (activity.mapsLink.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(activity.mapsLink))
                            context.startActivity(intent)
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.height(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Xem bản đồ",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Right side: status dot + edit button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Status dot (clickable to cycle)
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(statusColor(activity.status))
                        .clickable {
                            val next = when (activity.status) {
                                ActivityStatus.PENDING -> ActivityStatus.DONE
                                ActivityStatus.DONE    -> ActivityStatus.SKIPPED
                                ActivityStatus.SKIPPED -> ActivityStatus.CHANGED
                                ActivityStatus.CHANGED -> ActivityStatus.PENDING
                            }
                            onStatusChange(next)
                        }
                )
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalIconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = "Chỉnh sửa",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Status label at bottom
        if (activity.status != ActivityStatus.PENDING) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(statusColor(activity.status).copy(alpha = 0.08f))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.CenterVertically)
                        .clip(CircleShape)
                        .background(statusColor(activity.status))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = activity.status.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor(activity.status),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─── Add / Edit Activity bottom sheet ────────────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivityEditSheet(
    dayId: Long,
    existingActivity: Activity?,
    onSave: (Activity) -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    // Form state
    var name by rememberSaveable { mutableStateOf(existingActivity?.name ?: "") }
    var departureTime by rememberSaveable { mutableStateOf(existingActivity?.departureTime ?: "") }
    var arrivalTime by rememberSaveable { mutableStateOf(existingActivity?.arrivalTime ?: "") }
    var distanceText by rememberSaveable {
        mutableStateOf(
            if ((existingActivity?.distanceKm ?: 0.0) > 0) "%.1f".format(existingActivity?.distanceKm) else ""
        )
    }
    var hotelName by rememberSaveable { mutableStateOf(existingActivity?.hotelName ?: "") }
    var hotelPriceText by rememberSaveable {
        mutableStateOf(
            if ((existingActivity?.hotelPricePlanned ?: 0L) > 0L)
                existingActivity!!.hotelPricePlanned.toString()
            else ""
        )
    }
    var checkInSpots by rememberSaveable {
        mutableStateOf(parseSpots(existingActivity?.checkInSpots ?: ""))
    }
    var spotInput by rememberSaveable { mutableStateOf("") }
    var mapsLink by rememberSaveable { mutableStateOf(existingActivity?.mapsLink ?: "") }
    var notes by rememberSaveable { mutableStateOf(existingActivity?.notes ?: "") }

    var nameError by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Sheet handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(40.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (existingActivity == null) "Thêm hoạt động" else "Chỉnh sửa hoạt động",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Tên địa điểm ─────────────────────────────────────────────
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = { Text("Tên địa điểm *") },
            isError = nameError,
            supportingText = if (nameError) {
                { Text("Vui lòng nhập tên địa điểm") }
            } else null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Times ─────────────────────────────────────────────────────
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = departureTime,
                onValueChange = { departureTime = it },
                label = { Text("Giờ xuất phát") },
                placeholder = { Text("HH:mm") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = arrivalTime,
                onValueChange = { arrivalTime = it },
                label = { Text("Giờ đến nơi") },
                placeholder = { Text("HH:mm") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Distance ──────────────────────────────────────────────────
        OutlinedTextField(
            value = distanceText,
            onValueChange = { distanceText = it },
            label = { Text("Khoảng cách (km)") },
            placeholder = { Text("0.0") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Hotel name ────────────────────────────────────────────────
        OutlinedTextField(
            value = hotelName,
            onValueChange = { hotelName = it },
            label = { Text("Tên khách sạn / nơi nghỉ") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Hotel price ───────────────────────────────────────────────
        OutlinedTextField(
            value = hotelPriceText,
            onValueChange = { hotelPriceText = it },
            label = { Text("Giá phòng dự kiến (nghìn ₫)") },
            placeholder = { Text("VD: 500 = 500.000 ₫") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            supportingText = {
                val v = hotelPriceText.toLongOrNull() ?: 0L
                if (v > 0) Text(MoneyUtils.formatVnd(MoneyUtils.inputToVnd(v)))
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Hotel price shortcuts
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            hotelShortcuts.forEach { sc ->
                SuggestionChip(
                    onClick = { hotelPriceText = sc.valueK.toString() },
                    label = { Text(sc.label, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Check-in spots ────────────────────────────────────────────
        Text(
            text = "Điểm check-in cần ghé",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (checkInSpots.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                checkInSpots.forEach { spot ->
                    InputChip(
                        selected = false,
                        onClick = {},
                        label = { Text(spot, style = MaterialTheme.typography.labelSmall) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Xoá $spot",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        checkInSpots = checkInSpots.filter { it != spot }
                                    }
                            )
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = spotInput,
            onValueChange = { spotInput = it },
            label = { Text("Thêm điểm check-in") },
            placeholder = { Text("Nhập tên rồi nhấn Enter") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    val trimmed = spotInput.trim()
                    if (trimmed.isNotBlank() && !checkInSpots.contains(trimmed)) {
                        checkInSpots = checkInSpots + trimmed
                    }
                    spotInput = ""
                }
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Maps link ─────────────────────────────────────────────────
        OutlinedTextField(
            value = mapsLink,
            onValueChange = { mapsLink = it },
            label = { Text("Link Google Maps") },
            placeholder = { Text("https://maps.google.com/...") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            trailingIcon = {
                if (mapsLink.isNotBlank()) {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsLink))
                        context.startActivity(intent)
                    }) {
                        Icon(
                            Icons.Filled.OpenInNew,
                            contentDescription = "Mở bản đồ",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ── Notes ─────────────────────────────────────────────────────
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Ghi chú") },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── Action buttons ────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) { Text("Huỷ") }

            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    focusManager.clearFocus()
                    val activity = Activity(
                        id = existingActivity?.id ?: 0L,
                        dayId = dayId,
                        orderIndex = existingActivity?.orderIndex ?: 0,
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
            ) { Text(if (existingActivity == null) "Thêm" else "Lưu") }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
