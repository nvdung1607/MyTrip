package com.example.mytrip.ui.screens.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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

    LaunchedEffect(tripId) {
        viewModel.loadTrip(tripId)
    }

    // ─── Delete confirmation dialog ───────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(14.dp)
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
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Tùy chọn",
                                    tint = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null,
                                            modifier = Modifier.size(18.dp))
                                    },
                                    text = { Text("Chỉnh sửa chuyến đi") },
                                    onClick = {
                                        showMenu = false
                                        navController.navigate(Screen.EditTrip.createRoute(tripId))
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.Delete, contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.error)
                                    },
                                    text = {
                                        Text(
                                            "Xóa chuyến đi",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        showDeleteDialog = true
                                    }
                                )
                                HorizontalDivider()
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.Schedule, contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary)
                                    },
                                    text = { Text("Đặt trạng thái: Sắp đi") },
                                    enabled = t.status != TripStatus.PLANNING,
                                    onClick = {
                                        showMenu = false
                                        viewModel.updateStatus(tripId, TripStatus.PLANNING)
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.DirectionsRun, contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = Color(0xFF137333))
                                    },
                                    text = { Text("Đặt trạng thái: Đang đi") },
                                    enabled = t.status != TripStatus.ONGOING,
                                    onClick = {
                                        showMenu = false
                                        viewModel.updateStatus(tripId, TripStatus.ONGOING)
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.outline)
                                    },
                                    text = { Text("Đặt trạng thái: Hoàn thành") },
                                    enabled = t.status != TripStatus.DONE,
                                    onClick = {
                                        showMenu = false
                                        viewModel.updateStatus(tripId, TripStatus.DONE)
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
                        onStatusChange = { newStatus ->
                            viewModel.updateStatus(tripId, newStatus)
                        }
                    )
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
                .height(220.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = tripTypeGradient(trip.type),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            // Scrim for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.20f))
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = topPadding, start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Trip name
                Text(
                    text = trip.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 36.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))

                // Type + dates row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.20f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = tripTypeIcon(trip.type),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = trip.type.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${DateUtils.formatDate(trip.startDate)} – ${DateUtils.formatDate(trip.endDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                    }
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
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    icon = Icons.Default.DateRange,
                    value = "$numDays",
                    label = "ngày"
                )
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatItem(
                    icon = Icons.Default.Group,
                    value = "${trip.numPeople}",
                    label = "người"
                )
                VerticalDivider(modifier = Modifier.height(40.dp))
                StatItem(
                    icon = Icons.Default.AccountBalanceWallet,
                    value = if (totalPlanned > 0) MoneyUtils.formatShort(totalPlanned) else "–",
                    label = "dự kiến"
                )
            }
        }

        Spacer(Modifier.height(20.dp))

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

            // Row 1: Lịch trình + Hôm nay / Trạng thái
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DateRange,
                    title = "Lịch trình",
                    subtitle = "$numDays ngày",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onClick = { navController.navigate(Screen.Itinerary.createRoute(trip.id)) }
                )
                if (trip.status == TripStatus.ONGOING) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.WbSunny,
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
                        icon = Icons.Default.Sync,
                        title = "Trạng thái",
                        subtitle = statusLabel,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = { showStatusDialog = true }
                    )
                }
            }

            // Row 2: Nhật ký + Chi phí
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    title = "Nhật ký & Ảnh",
                    subtitle = "Tất cả ghi chép",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { navController.navigate(Screen.AllNotes.createRoute(trip.id)) }
                )
                ActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.AccountBalanceWallet,
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
                icon = Icons.Default.BarChart,
                title = "Tổng kết chuyến đi",
                subtitle = "Chi phí & hành trình",
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = { navController.navigate(Screen.Summary.createRoute(trip.id)) },
                fullWidth = true
            )
        }

        Spacer(Modifier.height(88.dp)) // space for FAB
    }

    // ─── Status change dialog ─────────────────────────────────────────────────
    if (showStatusDialog) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            icon = {
                Icon(Icons.Default.Sync, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary)
            },
            title = { Text("Chuyển trạng thái", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StatusOptionButton(
                        icon = Icons.Default.Schedule,
                        label = "Sắp đi",
                        isSelected = trip.status == TripStatus.PLANNING,
                        selectedColor = MaterialTheme.colorScheme.primary,
                        onClick = { onStatusChange(TripStatus.PLANNING); showStatusDialog = false }
                    )
                    StatusOptionButton(
                        icon = Icons.Default.DirectionsRun,
                        label = "Đang đi",
                        isSelected = trip.status == TripStatus.ONGOING,
                        selectedColor = Color(0xFF137333),
                        onClick = { onStatusChange(TripStatus.ONGOING); showStatusDialog = false }
                    )
                    StatusOptionButton(
                        icon = Icons.Default.CheckCircle,
                        label = "Hoàn thành",
                        isSelected = trip.status == TripStatus.DONE,
                        selectedColor = MaterialTheme.colorScheme.outline,
                        onClick = { onStatusChange(TripStatus.DONE); showStatusDialog = false }
                    )
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

// ─── Helper composables ────────────────────────────────────────────────────────

@Composable
private fun StatusOptionButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) selectedColor else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun StatusChip(status: TripStatus) {
    val (bgColor, label, icon) = when (status) {
        TripStatus.PLANNING -> Triple(StatusPlanning, "Sắp đi", Icons.Default.Schedule)
        TripStatus.ONGOING  -> Triple(StatusOngoing,  "Đang đi", Icons.Default.DirectionsRun)
        TripStatus.DONE     -> Triple(StatusDone,     "Hoàn thành", Icons.Default.CheckCircle)
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
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
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    fullWidth: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = modifier.then(if (fullWidth) Modifier.height(68.dp) else Modifier.height(104.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        if (fullWidth) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(26.dp)
                )
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
                        color = contentColor.copy(alpha = 0.75f)
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
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = contentColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
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
                        color = contentColor.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─── Trip type gradient helper ────────────────────────────────────────────────

private fun tripTypeGradient(type: TripType): List<Color> = when (type) {
    TripType.CAR       -> listOf(Color(0xFF1A73E8), Color(0xFF4DABF7))
    TripType.MOTORBIKE -> listOf(Color(0xFFE8710A), Color(0xFFFFB347))
    TripType.PUBLIC    -> listOf(Color(0xFF137333), Color(0xFF34A853))
    TripType.TREKKING  -> listOf(Color(0xFF7B1FA2), Color(0xFFAB47BC))
    TripType.CAMPING   -> listOf(Color(0xFF00695C), Color(0xFF26A69A))
    TripType.OTHER     -> listOf(Color(0xFF5F6368), Color(0xFF9AA0A6))
}

// ─── Trip type icon helper ────────────────────────────────────────────────────

private fun tripTypeIcon(type: TripType): ImageVector = when (type) {
    TripType.CAR       -> Icons.Default.DirectionsCar
    TripType.MOTORBIKE -> Icons.Default.TwoWheeler
    TripType.PUBLIC    -> Icons.Default.DirectionsBus
    TripType.TREKKING  -> Icons.Default.Hiking
    TripType.CAMPING   -> Icons.Default.Forest
    TripType.OTHER     -> Icons.Default.Explore
}
