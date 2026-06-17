package com.example.mytrip.ui.screens.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.navigation.NavController
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.db.entities.TripStatus
import com.example.mytrip.data.db.entities.TripType
import com.example.mytrip.navigation.Screen
import com.example.mytrip.util.DateUtils
import com.example.mytrip.util.MoneyUtils
import com.example.mytrip.ui.components.DraggableFab
import com.example.mytrip.ui.components.MyTripPrimaryButton
import com.example.mytrip.ui.theme.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.example.mytrip.util.BackupUtils
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    navController: NavController,
    tripId: Long,
    viewModel: TripViewModel = viewModel()
) {
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val totalPlanned by viewModel.totalPlanned.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pendingStatusChange by remember { mutableStateOf<TripStatus?>(null) }
    val datePickerState = rememberDatePickerState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(tripId) {
        viewModel.loadTrip(tripId)
    }

    val handleStatusChange: (TripStatus) -> Unit = { newStatus ->
        if (trip?.startDate == 0L) {
            pendingStatusChange = newStatus
            showDatePicker = true
        } else {
            if (newStatus == TripStatus.ONGOING && trip != null) {
                val now = System.currentTimeMillis()
                val cal = java.util.Calendar.getInstance().apply { timeInMillis = now }
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                cal.set(java.util.Calendar.MILLISECOND, 0)
                val startOfToday = cal.timeInMillis

                if (trip!!.startDate > startOfToday) {
                    android.widget.Toast.makeText(
                        context,
                        "Chưa đến ngày xuất phát! Nếu bạn xuất phát sớm, hãy cập nhật lại ngày bắt đầu.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else {
                    viewModel.updateStatus(tripId, newStatus)
                }
            } else {
                viewModel.updateStatus(tripId, newStatus)
            }
        }
    }

    // ─── Delete confirmation dialog ───────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Xóa chuyến đi?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Hành động này sẽ xóa vĩnh viễn \"${trip?.name}\" cùng toàn bộ dữ liệu liên quan.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        trip?.let {
                            viewModel.deleteTrip(it)
                            navController.popBackStack()
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        trip?.let { viewModel.updateStartDate(it, dateMillis, pendingStatusChange) }
                    }
                    showDatePicker = false
                    pendingStatusChange = null
                }) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    // ─── Settings Modal Bottom Sheet ──────────────────────────────────────────
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Tùy chọn chuyến đi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                // Hành động
                ListItem(
                    headlineContent = { Text("Chỉnh sửa thông tin", fontWeight = FontWeight.Medium) },
                    leadingContent = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        navController.navigate(Screen.EditTrip.createRoute(tripId))
                    }.padding(horizontal = 8.dp)
                )
                
                ListItem(
                    headlineContent = { Text("Xuất backup (JSON)", fontWeight = FontWeight.Medium) },
                    leadingContent = { Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        trip?.let { t ->
                            scope.launch {
                                try {
                                    val json = viewModel.exportBackupJson(tripId)
                                    val tripName = t.name
                                    val uri = BackupUtils.saveToCache(context, json, tripName)
                                    if (uri != null) BackupUtils.shareBackup(context, uri, tripName)
                                    BackupUtils.saveToDownloads(context, json, tripName)
                                } catch (_: Exception) {}
                            }
                        }
                    }.padding(horizontal = 8.dp)
                )

                ListItem(
                    headlineContent = { Text("Xóa chuyến đi", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                    leadingContent = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable {
                        showSettingsSheet = false
                        showDeleteDialog = true
                    }.padding(horizontal = 8.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Trạng thái
                Text(
                    text = "Trạng thái",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ListItem(
                    headlineContent = { Text("Sắp đi", fontWeight = FontWeight.Medium) },
                    leadingContent = { Icon(Icons.Rounded.Schedule, contentDescription = null, tint = if (trip?.status == TripStatus.PLANNING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = { if (trip?.status == TripStatus.PLANNING) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        if (trip?.status != TripStatus.PLANNING) {
                            showSettingsSheet = false
                            handleStatusChange(TripStatus.PLANNING)
                        }
                    }.padding(horizontal = 8.dp)
                )

                ListItem(
                    headlineContent = { Text("Đang đi", fontWeight = FontWeight.Medium) },
                    leadingContent = { Icon(Icons.Rounded.DirectionsCar, contentDescription = null, tint = if (trip?.status == TripStatus.ONGOING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = { if (trip?.status == TripStatus.ONGOING) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        if (trip?.status != TripStatus.ONGOING) {
                            showSettingsSheet = false
                            handleStatusChange(TripStatus.ONGOING)
                        }
                    }.padding(horizontal = 8.dp)
                )

                ListItem(
                    headlineContent = { Text("Hoàn thành", fontWeight = FontWeight.Medium) },
                    leadingContent = { Icon(Icons.Rounded.TaskAlt, contentDescription = null, tint = if (trip?.status == TripStatus.DONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                    trailingContent = { if (trip?.status == TripStatus.DONE) Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable {
                        if (trip?.status != TripStatus.DONE) {
                            showSettingsSheet = false
                            handleStatusChange(TripStatus.DONE)
                        }
                    }.padding(horizontal = 8.dp)
                )
            }
        }
    }

    TripThemeProvider(trip = trip) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = {
                    val t = trip
                    if (t != null) {
                        StatusChip(status = t.status)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    val t = trip
                    if (t != null) {
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = "Cài đặt",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->

        Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState is TripUiState.Loading && trip == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState is TripUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (uiState as TripUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            trip != null -> {
                TripDetailContent(
                    trip = trip!!,
                    totalPlanned = totalPlanned,
                    topPadding = innerPadding.calculateTopPadding(),
                    navController = navController,
                    onStatusChange = handleStatusChange
                )
            }
            
            uiState is TripUiState.Success && trip == null -> {
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(0)
                    }
                }
            }
        }

        // Draggable FAB overlay – thêm nhật ký
        if (trip != null) {
            DraggableFab(
                onClick = {
                    navController.navigate(Screen.AddNote.createRoute(tripId, null))
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
        } // end Box
    }
    }
}

@Composable
private fun TripDetailContent(
    trip: Trip,
    totalPlanned: Long,
    topPadding: androidx.compose.ui.unit.Dp,
    navController: NavController,
    onStatusChange: (TripStatus) -> Unit
) {
    val numDays = DateUtils.countDays(trip.startDate, trip.endDate)
    var showStatusDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ─── Hero header ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 180.dp)
                .background(com.example.mytrip.ui.theme.TripThemeColors.getThemeGradient(trip.themeColor))
        ) {
            // Overlay scrim for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.18f))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = topPadding + MaterialTheme.spacing.small, 
                        start = MaterialTheme.spacing.marginMobile, 
                        end = MaterialTheme.spacing.marginMobile, 
                        bottom = MaterialTheme.spacing.marginMobile
                    ),
                verticalArrangement = Arrangement.Bottom
            ) {

                // Trip name
                Text(
                    text = trip.name,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 38.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))

                // Type + dates row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.22f)
                    ) {
                        Text(
                            text = "${trip.type.icon} ${trip.type.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    val dateText = if (trip.startDate == 0L) {
                        "Chưa xác định ngày"
                    } else {
                        "📅 ${DateUtils.formatDate(trip.startDate)} – ${DateUtils.formatDate(trip.endDate)}"
                    }
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
        // ─── Quick stats row ──────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = "🗓️",
                    value = "$numDays",
                    label = "ngày"
                )
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatItem(
                    icon = "👥",
                    value = "${trip.numPeople}",
                    label = "người"
                )
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatItem(
                    icon = "💰",
                    value = if (totalPlanned > 0) MoneyUtils.formatShort(totalPlanned) else "–",
                    label = "dự kiến"
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ─── Action grid ──────────────────────────────────────────────────────
        // Tính màu pastel từ themeColor chuyến đi
        val themeHex = trip.themeColor
        val pastelColors = remember(themeHex) { buildPastelColors(themeHex) }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Removed "Tính năng" text as requested

            // Row 1: Lịch trình + Hôm nay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    emoji = "🗓️",
                    title = "Lịch trình",
                    subtitle = "$numDays ngày",
                    containerColor = pastelColors[0],
                    contentColor = pastelColors[4],
                    onClick = { navController.navigate(Screen.Itinerary.createRoute(trip.id)) }
                )
                if (trip.status == TripStatus.ONGOING) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🌄",
                        title = "Hôm nay",
                        subtitle = "Đang đi",
                        containerColor = pastelColors[1],
                        contentColor = pastelColors[4],
                        onClick = { navController.navigate(Screen.Today.createRoute(trip.id)) }
                    )
                } else {
                    val statusLabel = when (trip.status) {
                        TripStatus.PLANNING -> "Sắp đi"
                        TripStatus.DONE -> "Hoàn thành"
                        else -> ""
                    }
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🔄",
                        title = "Trạng thái",
                        subtitle = statusLabel,
                        containerColor = pastelColors[1],
                        contentColor = pastelColors[4],
                        onClick = { showStatusDialog = true }
                    )
                }
            }

            // Row 2: Add Note + Chi phí
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    emoji = "📖",
                    title = "Nhật ký",
                    subtitle = "Tất cả ghi chép",
                    containerColor = pastelColors[2],
                    contentColor = pastelColors[4],
                    onClick = { navController.navigate(Screen.AllNotes.createRoute(trip.id)) }
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    emoji = "💰",
                    title = "Chi phí",
                    subtitle = if (totalPlanned > 0) MoneyUtils.formatShort(totalPlanned) else "Chưa có",
                    containerColor = pastelColors[3],
                    contentColor = pastelColors[4],
                    onClick = { navController.navigate(Screen.Expense.createRoute(trip.id)) }
                )
            }

            // Row 3: Tổng kết (full width)
            ActionCard(
                modifier = Modifier.fillMaxWidth(),
                emoji = "📊",
                title = "Tổng kết chuyến đi",
                subtitle = "Chi phí & hành trình",
                containerColor = pastelColors[5],
                contentColor = pastelColors[4],
                onClick = { navController.navigate(Screen.Summary.createRoute(trip.id)) },
                fullWidth = true
            )
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Chuyển trạng thái chuyến đi", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Chọn trạng thái mới cho chuyến đi của bạn:")
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Planning option
                    Button(
                        onClick = {
                            onStatusChange(TripStatus.PLANNING)
                            showStatusDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (trip.status == TripStatus.PLANNING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (trip.status == TripStatus.PLANNING) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("🔵 Sắp đi")
                    }
                    
                    // Ongoing option
                    Button(
                        onClick = {
                            onStatusChange(TripStatus.ONGOING)
                            showStatusDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (trip.status == TripStatus.ONGOING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (trip.status == TripStatus.ONGOING) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("🟢 Đang đi")
                    }
                    
                    // Done option
                    Button(
                        onClick = {
                            onStatusChange(TripStatus.DONE)
                            showStatusDialog = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (trip.status == TripStatus.DONE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (trip.status == TripStatus.DONE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("⚫ Hoàn thành")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStatusDialog = false }) {
                    Text("Đóng")
                }
            }
        )
    }
}


// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
private fun StatusChip(status: TripStatus) {
    val (bgColor, label) = when (status) {
        TripStatus.PLANNING -> StatusPlanning to "🔵 Sắp đi"
        TripStatus.ONGOING  -> StatusOngoing  to "🟢 Đang đi"
        TripStatus.DONE     -> StatusDone     to "⚫ Hoàn thành"
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bgColor.copy(alpha = 0.85f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun StatItem(
    icon: String,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(text = icon, style = MaterialTheme.typography.titleMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    emoji: String,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    fullWidth: Boolean = false
) {
    Surface(
        modifier = modifier
            .then(if (fullWidth) Modifier.defaultMinSize(minHeight = 72.dp) else Modifier.defaultMinSize(minHeight = 110.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp
    ) {
        if (fullWidth) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.5f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Tạo ra 5 màu từ themeColor của chuyến đi:
 * [0..3] = 4 màu pastel cho 4 ActionCard (hòa 12–20% màu chủ đề với nền trắng)
 * [4]    = màu chữ (phiên bản đậm hơn của themeColor để đọc được trên nền pastel)
 */
private fun buildPastelColors(themeHex: String): List<Color> {
    return try {
        if (themeHex.isBlank()) {
            // Fallback sang màu xám nhạt
            listOf(
                Color(0xFFE8EAF6), Color(0xFFE3F2FD),
                Color(0xFFE8F5E9), Color(0xFFFFF3E0),
                Color(0xFF37474F)
            )
        } else {
            val base = android.graphics.Color.parseColor(themeHex)
            val r = android.graphics.Color.red(base)
            val g = android.graphics.Color.green(base)
            val b = android.graphics.Color.blue(base)

            // Pastel variants: pha tỉ lệ khác nhau với nền trắng
            fun pastel(ratio: Float): Color {
                val pr = ((r * ratio) + (255 * (1f - ratio))).roundToInt().coerceIn(0, 255)
                val pg = ((g * ratio) + (255 * (1f - ratio))).roundToInt().coerceIn(0, 255)
                val pb = ((b * ratio) + (255 * (1f - ratio))).roundToInt().coerceIn(0, 255)
                return Color(android.graphics.Color.rgb(pr, pg, pb))
            }

            // Màu chữ: dùng themeColor gốc với alpha cao để đủ contrast trên nền pastel
            val textColor = Color(
                r / 255f * 0.65f,
                g / 255f * 0.65f,
                b / 255f * 0.65f,
                1f
            )

            listOf(
                pastel(0.18f),  // card 1 – Lịch trình
                pastel(0.14f),  // card 2 – Hôm nay / Trạng thái
                pastel(0.16f),  // card 3 – Nhật ký
                pastel(0.12f),  // card 4 – Chi phí
                textColor,      // màu chữ chung
                pastel(0.10f)   // card 5 – Tổng kết (nhạt nhất)
            )
        }
    } catch (_: Exception) {
        listOf(
            Color(0xFFE8EAF6), Color(0xFFE3F2FD),
            Color(0xFFE8F5E9), Color(0xFFFFF3E0),
            Color(0xFF37474F),
            Color(0xFFF3F4F6)  // fallback Tổng kết
        )
    }
}
