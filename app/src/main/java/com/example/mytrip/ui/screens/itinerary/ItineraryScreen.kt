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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mytrip.data.db.entities.Activity
import com.example.mytrip.data.db.entities.ActivityStatus
import com.example.mytrip.data.db.entities.ActivityType
import com.example.mytrip.data.db.entities.Cluster
import com.example.mytrip.data.db.entities.Day
import com.example.mytrip.navigation.Screen
import com.example.mytrip.util.DateUtils
import com.example.mytrip.util.MoneyUtils
import kotlinx.coroutines.launch
import org.json.JSONArray
import com.example.mytrip.ui.components.ScheduleTimelineList
import com.example.mytrip.ui.components.MyTripChip
import com.example.mytrip.ui.components.MyTripTextField
import com.example.mytrip.ui.components.MyTripPrimaryButton
import com.example.mytrip.ui.components.MyTripSecondaryButton
import com.example.mytrip.ui.components.GlassmorphismCard
import com.example.mytrip.ui.theme.TripThemeProvider
import com.example.mytrip.ui.theme.spacing

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

// ─── Suggestion lists per type ────────────────────────────────────────────────
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
    val clusters by viewModel.clusters.collectAsState()
    val expandedDays by viewModel.expandedDays.collectAsState()
    val expandedClusters by viewModel.expandedClusters.collectAsState()
    val activitiesMap by viewModel.activitiesMap.collectAsState()

    // Bottom sheet state
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<Activity?>(null) }
    var sheetDayId by rememberSaveable { mutableStateOf(0L) }
    var insertAfterIndex by rememberSaveable { mutableStateOf(-1) } // -1 = append

    // Delete dialog state
    var deleteTarget by remember { mutableStateOf<Activity?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Observe snackbar events from ViewModel
    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    TripThemeProvider(trip = trip) {
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
                        Icon(Icons.Filled.UnfoldMore, contentDescription = "Mở rộng tất cả", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.collapseAll() }) {
                        Icon(Icons.Filled.UnfoldLess, contentDescription = "Thu gọn tất cả", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (days.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(text = "Chưa có ngày nào trong lịch trình", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                if (clusters.isEmpty()) {
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
                                insertAfterIndex = -1
                                showSheet = true
                            },
                            onEditActivity = { act ->
                                sheetDayId = day.id
                                editingActivity = act
                                insertAfterIndex = -1
                                showSheet = true
                            },
                            onDeleteActivity = { act -> deleteTarget = act },
                            onStatusChange = { act, status -> viewModel.updateActivityStatus(act.id, status) },
                            onReorder = { activities -> viewModel.reorderActivities(day.id, activities) }
                        )
                    }
                } else {
                    val sortedClusters = clusters.sortedBy { it.orderIndex }
                    sortedClusters.forEach { cluster ->
                        val clusterDays = days.filter { it.clusterId == cluster.id }.sortedBy { it.dayNumber }
                        if (clusterDays.isNotEmpty()) {
                            val isClusterExpanded = expandedClusters.contains(cluster.id)

                            item(key = "cluster_${cluster.id}") {
                                ClusterHeader(
                                    cluster = cluster,
                                    daysCount = clusterDays.size,
                                    isExpanded = isClusterExpanded,
                                    onToggle = { viewModel.toggleClusterExpanded(cluster.id) }
                                )
                            }

                            if (isClusterExpanded) {
                                items(clusterDays, key = { it.id }) { day ->
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
                                            insertAfterIndex = -1
                                            showSheet = true
                                        },
                                        onEditActivity = { act ->
                                            sheetDayId = day.id
                                            editingActivity = act
                                            insertAfterIndex = -1
                                            showSheet = true
                                        },
                                        onDeleteActivity = { act -> deleteTarget = act },
                                        onStatusChange = { act, status -> viewModel.updateActivityStatus(act.id, status) },
                                        onReorder = { activities -> viewModel.reorderActivities(day.id, activities) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────────
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Xoá hoạt động?") },
            text = { Text("Bạn có chắc muốn xoá \"${deleteTarget?.name}\" không?") },
            confirmButton = {
                Button(
                    onClick = { deleteTarget?.let { viewModel.deleteActivity(it) }; deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xoá") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Huỷ") } }
        )
    }

    // ── Add / Edit bottom sheet ───────────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { scope.launch { sheetState.hide() }; showSheet = false },
            sheetState = sheetState,
            dragHandle = null,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            ActivityEditSheet(
                dayId = sheetDayId,
                existingActivity = editingActivity,
                insertAfterIndex = insertAfterIndex,
                onSave = { activity ->
                    if (editingActivity == null) {
                        if (insertAfterIndex >= 0) {
                            viewModel.insertActivityAfter(activity, insertAfterIndex)
                        } else {
                            viewModel.addActivity(activity)
                        }
                    } else {
                        viewModel.updateActivity(activity)
                    }
                    scope.launch { sheetState.hide() }
                    showSheet = false
                },
                onDismiss = { scope.launch { sheetState.hide() }; showSheet = false }
            )
        }
        }
    }
}

// ─── Day section ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DaySection(
    day: Day,
    activities: List<Activity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddActivity: () -> Unit,
    onEditActivity: (Activity) -> Unit,
    onDeleteActivity: (Activity) -> Unit,
    onStatusChange: (Activity, ActivityStatus) -> Unit,
    onReorder: (List<Activity>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Day header card
        GlassmorphismCard(
            modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.marginMobile, vertical = 6.dp).clickable { onToggleExpand() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(dayColor(day.dayNumber)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("N${day.dayNumber}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ngày ${day.dayNumber} — ${DateUtils.formatFull(day.date)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (day.title.isNotBlank()) {
                        Text(day.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("${activities.size} hoạt động", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Expanded activities list
        AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
            val sortedActivities = activities.sortedBy { it.orderIndex }

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                if (sortedActivities.isNotEmpty()) {
                    ScheduleTimelineList(
                        activities = sortedActivities,
                        onReorder = onReorder,
                        onEdit = onEditActivity,
                        onDelete = onDeleteActivity,
                        onStatusChange = onStatusChange,
                        modifier = Modifier.padding(start = 12.dp, end = 0.dp)
                    )
                }

                // Add activity button at bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(32.dp)
                    ) {
                        Surface(
                            onClick = onAddActivity,
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
                        onClick = onAddActivity,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+ Thêm hoạt động", style = MaterialTheme.typography.labelMedium)
                    }
                }
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
            ActivityType.values().forEach { type ->
                MyTripChip(
                    text = "${type.icon} ${type.label}",
                    selected = selectedType == type,
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
        AnimatedVisibility(
            visible = showMoreDetails,
            enter = expandVertically(),
            exit = shrinkVertically()
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
                                    Icon(Icons.Filled.OpenInNew, null, tint = MaterialTheme.colorScheme.primary)
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
            ) { Text(if (existingActivity == null) "Thêm" else "Lưu") }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// ─── Cluster header ──────────────────────────────────────────────────────────
@Composable
private fun ClusterHeader(cluster: Cluster, daysCount: Int, isExpanded: Boolean, onToggle: () -> Unit) {
    GlassmorphismCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.marginMobile, vertical = 8.dp).clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📦", fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(cluster.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("Gồm $daysCount ngày • ${if (isExpanded) "Chạm để ẩn" else "Chạm để xem chi tiết"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
            }
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
