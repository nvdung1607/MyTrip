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
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ArrowForward
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
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
import com.example.mytrip.util.ShareUtils
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
    val context = LocalContext.current
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
                    // Share PDF
                    IconButton(onClick = {
                        scope.launch {
                            val t = trip
                            val d = days
                            val am = activitiesMap
                            if (t != null) {
                                val uri = ShareUtils.buildItineraryPdf(context, t, d, am)
                                if (uri != null) ShareUtils.sharePdf(context, uri, t.name)
                                else snackbarHostState.showSnackbar("Không thể tạo PDF")
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Chia sẻ lịch trình", tint = MaterialTheme.colorScheme.primary)
                    }
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

    var selectedType by rememberSaveable { mutableStateOf(existingActivity?.activityType ?: ActivityType.TRANSIT) }
    var name by rememberSaveable { mutableStateOf(existingActivity?.name ?: "") }
    var notes by rememberSaveable { mutableStateOf(existingActivity?.notes ?: "") }
    var nameError by rememberSaveable { mutableStateOf(false) }
    var departureTime by rememberSaveable { mutableStateOf(existingActivity?.departureTime ?: "") }
    var arrivalTime by rememberSaveable { mutableStateOf(existingActivity?.arrivalTime ?: "") }
    var departureTimeError by rememberSaveable { mutableStateOf(false) }
    var arrivalTimeError by rememberSaveable { mutableStateOf(false) }
    var showMoreDetails by rememberSaveable { mutableStateOf(false) }
    var distanceText by rememberSaveable {
        mutableStateOf(if ((existingActivity?.distanceKm ?: 0.0) > 0) "%.1f".format(existingActivity?.distanceKm) else "")
    }
    var mapsLink by rememberSaveable { mutableStateOf(existingActivity?.mapsLink ?: "") }
    var checkInSpots by rememberSaveable { mutableStateOf(parseSpots(existingActivity?.checkInSpots ?: "")) }
    var spotInput by rememberSaveable { mutableStateOf("") }
    var hotelName by rememberSaveable { mutableStateOf(existingActivity?.hotelName ?: "") }
    var hotelPriceText by rememberSaveable {
        mutableStateOf(if ((existingActivity?.hotelPricePlanned ?: 0L) > 0L) existingActivity!!.hotelPricePlanned.toString() else "")
    }

    val departureTimeValue = remember(departureTime) { TextFieldValue(departureTime, TextRange(departureTime.length)) }
    val arrivalTimeValue = remember(arrivalTime) { TextFieldValue(arrivalTime, TextRange(arrivalTime.length)) }

    fun isValidTime(time: String): Boolean {
        val clean = time.trim()
        if (clean.isEmpty()) return true
        return Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$").matches(clean)
    }
    fun formatDigitsToTime(digits: String): String = when (digits.length) {
        0 -> ""; 1, 2 -> digits
        3 -> "${digits.substring(0, 2)}:${digits.substring(2)}"
        else -> "${digits.substring(0, 2)}:${digits.substring(2, 4)}"
    }
    fun onTimeValueChange(newVal: String): String = formatDigitsToTime(newVal.filter { it.isDigit() }.take(4))
    fun addTimeToFormatted(time: String, minutesToAdd: Int): String {
        val parts = time.split(":")
        if (parts.size != 2) return ""
        val hour = parts[0].toIntOrNull() ?: return ""
        val minute = parts[1].toIntOrNull() ?: return ""
        val totalMinutes = hour * 60 + minute + minutesToAdd
        return String.format(Locale.US, "%02d:%02d", (totalMinutes / 60) % 24, totalMinutes % 60)
    }
    fun showTimePicker(currentTime: String, onTimeSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        android.app.TimePickerDialog(
            context,
            { _, hour, minute -> onTimeSelected(String.format(Locale.US, "%02d:%02d", hour, minute)) },
            currentTime.split(":").firstOrNull()?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY),
            currentTime.split(":").lastOrNull()?.toIntOrNull() ?: cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    // ── Màu accent theo loại hoạt động ───────────────────────────────────────
    val typeColor = when (selectedType) {
        ActivityType.TRANSIT      -> Color(0xFF1565C0)
        ActivityType.SIGHTSEEING  -> Color(0xFF2E7D32)
        ActivityType.MEAL         -> Color(0xFFE65100)
        ActivityType.ACCOMMODATION-> Color(0xFF6A1B9A)
        ActivityType.ACTIVITY     -> Color(0xFF00695C)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Drag handle ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // ── Header với màu gradient theo loại ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(typeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(selectedType.icon, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (existingActivity == null) "Tạo hoạt động mới" else "Chỉnh sửa hoạt động",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedType.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = typeColor
                    )
                }
            }
        }

        // ── Loại hoạt động — Icon card selector ───────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActivityType.values().forEach { type ->
                val isSelected = selectedType == type
                val tColor = when (type) {
                    ActivityType.TRANSIT      -> Color(0xFF1565C0)
                    ActivityType.SIGHTSEEING  -> Color(0xFF2E7D32)
                    ActivityType.MEAL         -> Color(0xFFE65100)
                    ActivityType.ACCOMMODATION-> Color(0xFF6A1B9A)
                    ActivityType.ACTIVITY     -> Color(0xFF00695C)
                }
                Surface(
                    onClick = { selectedType = type; name = "" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) tColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, tColor) else null,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                    ) {
                        Text(type.icon, fontSize = 18.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = type.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) tColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Tên hoạt động ─────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                "Tên hoạt động *",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                placeholder = { Text("Nhập tên ${selectedType.label.lowercase()}...") },
                isError = nameError,
                supportingText = if (nameError) { { Text("Vui lòng nhập tên", color = MaterialTheme.colorScheme.error) } } else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeColor,
                    focusedLabelColor = typeColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Gợi ý tên
            val suggestions = suggestionsFor(selectedType)
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suggestions) { s ->
                        SuggestionChip(
                            onClick = { name = s },
                            label = { Text(s, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Thời gian ─────────────────────────────────────────────────────────
        if (selectedType != ActivityType.MEAL) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    if (selectedType == ActivityType.ACCOMMODATION) "Check-in / Check-out" else "Thời gian",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Giờ đi / Check-in
                    Surface(
                        onClick = { showTimePicker(departureTime) { departureTime = it; departureTimeError = false } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (departureTimeError) MaterialTheme.colorScheme.error
                            else if (departureTime.isNotBlank()) typeColor
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        color = if (departureTime.isNotBlank()) typeColor.copy(alpha = 0.07f)
                                else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = if (departureTime.isNotBlank()) typeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (departureTime.isNotBlank()) departureTime else "--:--",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (departureTime.isNotBlank()) typeColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (selectedType == ActivityType.ACCOMMODATION) "Check-in" else "Giờ đi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mũi tên ở giữa
                    Box(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Giờ đến / Check-out
                    Surface(
                        onClick = { showTimePicker(arrivalTime) { arrivalTime = it; arrivalTimeError = false } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (arrivalTimeError) MaterialTheme.colorScheme.error
                            else if (arrivalTime.isNotBlank()) typeColor
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        color = if (arrivalTime.isNotBlank()) typeColor.copy(alpha = 0.07f)
                                else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = if (arrivalTime.isNotBlank()) typeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (arrivalTime.isNotBlank()) arrivalTime else "--:--",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (arrivalTime.isNotBlank()) typeColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (selectedType == ActivityType.ACCOMMODATION) "Check-out" else "Giờ đến",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Smart time offset chips
                if (isValidTime(departureTime) && departureTime.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Thêm:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("+30p" to 30, "+1h" to 60, "+2h" to 120, "+3h" to 180, "+4h" to 240)) { (label, mins) ->
                                SuggestionChip(
                                    onClick = {
                                        val suggested = addTimeToFormatted(departureTime, mins)
                                        if (suggested.isNotEmpty()) { arrivalTime = suggested; arrivalTimeError = false }
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // ── ACCOMMODATION: Tên khách sạn ──────────────────────────────────────
        if (selectedType == ActivityType.ACCOMMODATION) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Khách sạn", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(
                    value = hotelName,
                    onValueChange = { hotelName = it },
                    placeholder = { Text("Tên khách sạn / homestay...") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = typeColor, focusedLabelColor = typeColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Divider + Nút mở thêm chi tiết ───────────────────────────────────
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Surface(
            onClick = { showMoreDetails = !showMoreDetails },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (showMoreDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (showMoreDetails) "Ẩn thông tin chi tiết" else "Thêm thông tin chi tiết",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ── Chi tiết mở rộng ──────────────────────────────────────────────────
        AnimatedVisibility(visible = showMoreDetails, enter = expandVertically(), exit = shrinkVertically()) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ACCOMMODATION: Giá phòng
                if (selectedType == ActivityType.ACCOMMODATION) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Giá phòng (nghìn ₫)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = hotelPriceText,
                            onValueChange = { hotelPriceText = it },
                            placeholder = { Text("VD: 500 = 500.000 ₫") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = typeColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(hotelShortcuts) { sc ->
                                SuggestionChip(onClick = { hotelPriceText = sc.valueK.toString() }, label = { Text(sc.label, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                }

                // TRANSIT: Khoảng cách
                if (selectedType == ActivityType.TRANSIT) {
                    Text("Khoảng cách", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it },
                        placeholder = { Text("km") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = typeColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // SIGHTSEEING: Check-in spots
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
                                        trailingIcon = { Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp)) }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = spotInput,
                            onValueChange = { spotInput = it },
                            placeholder = { Text("Nhập tên điểm rồi nhấn Enter") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val t = spotInput.trim()
                                if (t.isNotBlank() && !checkInSpots.contains(t)) checkInSpots = checkInSpots + t
                                spotInput = ""
                            }),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = typeColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Link Google Maps
                if (selectedType == ActivityType.TRANSIT || selectedType == ActivityType.SIGHTSEEING || selectedType == ActivityType.ACCOMMODATION) {
                    Text("Link Google Maps", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = mapsLink,
                        onValueChange = { mapsLink = it },
                        placeholder = { Text("https://maps.google.com/...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = typeColor),
                        trailingIcon = {
                            if (mapsLink.isNotBlank()) {
                                IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsLink))) }) {
                                    Icon(Icons.Filled.OpenInNew, null, tint = typeColor)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Ghi chú
                Text("Ghi chú", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Thêm ghi chú cho hoạt động...") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = typeColor),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))
            }
        }

        // ── Action buttons ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Huỷ")
            }
            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    val isDepValid = isValidTime(departureTime)
                    val isArrValid = isValidTime(arrivalTime)
                    if (!isDepValid) departureTimeError = true
                    if (!isArrValid) arrivalTimeError = true
                    if (!isDepValid || !isArrValid) return@Button
                    focusManager.clearFocus()
                    onSave(Activity(
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
                    ))
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = typeColor)
            ) {
                Text(if (existingActivity == null) "✓  Thêm" else "✓  Lưu")
            }
        }
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
