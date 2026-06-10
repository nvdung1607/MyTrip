package com.example.mytrip.ui.screens.trip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    navController: NavController,
    tripId: Long,
    viewModel: TripViewModel = viewModel()
) {
    val trip by viewModel.trip.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val totalPlanned by viewModel.totalPlanned.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingStatusChange by remember { mutableStateOf<TripStatus?>(null) }
    val datePickerState = rememberDatePickerState()

    LaunchedEffect(tripId) {
        viewModel.loadTrip(tripId)
    }

    val handleStatusChange: (TripStatus) -> Unit = { newStatus ->
        if (trip?.startDate == 0L) {
            pendingStatusChange = newStatus
            showDatePicker = true
        } else {
            viewModel.updateStatus(tripId, newStatus)
        }
    }

    // ─── Delete confirmation dialog ───────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
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
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    val t = trip
                    if (t != null) {
                        Box {
                            var showMenu by remember { mutableStateOf(false) }
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Cài đặt",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Chỉnh sửa thông tin") },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Screen.EditTrip.createRoute(tripId))
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Xóa chuyến đi", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("🔵 Đặt trạng thái: Sắp đi") },
                                    enabled = t.status != TripStatus.PLANNING,
                                    onClick = {
                                        showMenu = false
                                        handleStatusChange(TripStatus.PLANNING)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("🟢 Đặt trạng thái: Đang đi") },
                                    enabled = t.status != TripStatus.ONGOING,
                                    onClick = {
                                        showMenu = false
                                        handleStatusChange(TripStatus.ONGOING)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("⚫ Đặt trạng thái: Hoàn thành") },
                                    enabled = t.status != TripStatus.DONE,
                                    onClick = {
                                        showMenu = false
                                        handleStatusChange(TripStatus.DONE)
                                    }
                                )
                            }
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
                .defaultMinSize(minHeight = 280.dp)
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
                    Text(
                        text = "📅 ${DateUtils.formatDate(trip.startDate)} – ${DateUtils.formatDate(trip.endDate)}",
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
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Tính năng",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

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
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { navController.navigate(Screen.Itinerary.createRoute(trip.id)) }
                )
                if (trip.status == TripStatus.ONGOING) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🌄",
                        title = "Hôm nay",
                        subtitle = "Đang đi",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
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
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
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
                    title = "Nhật ký & Ảnh",
                    subtitle = "Tất cả ghi chép",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { navController.navigate(Screen.AllNotes.createRoute(trip.id)) }
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    emoji = "💰",
                    title = "Chi phí",
                    subtitle = if (totalPlanned > 0) MoneyUtils.formatShort(totalPlanned) else "Chưa có",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = { navController.navigate(Screen.Expense.createRoute(trip.id)) }
                )
            }

            // Row 3: Tổng kết (full width)
            ActionCard(
                modifier = Modifier.fillMaxWidth(),
                emoji = "📊",
                title = "Tổng kết chuyến đi",
                subtitle = "Chi phí & hành trình",
                containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.08f),
                contentColor = MaterialTheme.colorScheme.onSurface,
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
            .then(if (fullWidth) Modifier.defaultMinSize(minHeight = 72.dp) else Modifier.defaultMinSize(minHeight = 110.dp)),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        shadowElevation = 2.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (containerColor.alpha < 0.3f)
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            else
                Color.Transparent
        ),
        onClick = onClick
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
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.5f)
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
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


