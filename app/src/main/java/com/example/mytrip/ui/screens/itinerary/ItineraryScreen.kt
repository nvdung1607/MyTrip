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

import com.example.mytrip.ui.components.ActivityEditSheet
// â”€â”€â”€ Palette for day number circles â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

// â”€â”€â”€ Main screen â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                            text = trip?.name ?: "Lá»‹ch trÃ¬nh",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${days.size} ngÃ y",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay láº¡i")
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
                                else snackbarHostState.showSnackbar("KhÃ´ng thá»ƒ táº¡o PDF")
                            }
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Chia sáº» lá»‹ch trÃ¬nh", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.expandAll() }) {
                        Icon(Icons.Filled.UnfoldMore, contentDescription = "Má»Ÿ rá»™ng táº¥t cáº£", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.collapseAll() }) {
                        Icon(Icons.Filled.UnfoldLess, contentDescription = "Thu gá»n táº¥t cáº£", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (days.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(text = "ChÆ°a cÃ³ ngÃ y nÃ o trong lá»‹ch trÃ¬nh", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

    // â”€â”€ Delete confirmation dialog â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("XoÃ¡ hoáº¡t Ä‘á»™ng?") },
            text = { Text("Báº¡n cÃ³ cháº¯c muá»‘n xoÃ¡ \"${deleteTarget?.name}\" khÃ´ng?") },
            confirmButton = {
                Button(
                    onClick = { deleteTarget?.let { viewModel.deleteActivity(it) }; deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("XoÃ¡") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Huá»·") } }
        )
    }

    // â”€â”€ Add / Edit bottom sheet â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

// â”€â”€â”€ Day section â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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
                    Text("NgÃ y ${day.dayNumber} â€” ${DateUtils.formatFull(day.date)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (day.title.isNotBlank()) {
                        Text(day.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("${activities.size} hoáº¡t Ä‘á»™ng", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                    contentDescription = "ThÃªm hoáº¡t Ä‘á»™ng",
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
                        Text("+ ThÃªm hoáº¡t Ä‘á»™ng", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// â”€â”€â”€ Cluster header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
@Composable
private fun ClusterHeader(cluster: Cluster, daysCount: Int, isExpanded: Boolean, onToggle: () -> Unit) {
    GlassmorphismCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = MaterialTheme.spacing.marginMobile, vertical = 8.dp).clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ðŸ“¦", fontSize = 20.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(cluster.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text("Gá»“m $daysCount ngÃ y â€¢ ${if (isExpanded) "Cháº¡m Ä‘á»ƒ áº©n" else "Cháº¡m Ä‘á»ƒ xem chi tiáº¿t"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
            }
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}
