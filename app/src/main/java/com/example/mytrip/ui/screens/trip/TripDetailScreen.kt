package com.example.mytrip.ui.screens.trip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
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
                    if (trip != null) {
                        IconButton(
                            onClick = {
                                navController.navigate(Screen.EditTrip.createRoute(tripId))
                            }
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Chỉnh sửa",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Xóa chuyến đi",
                                tint = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ─── Hero header ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = tripTypeGradient(trip.type),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
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
                    .padding(top = topPadding + 8.dp, start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Status chip
                StatusChip(status = trip.status)
                Spacer(Modifier.height(8.dp))

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
                    Spacer(Modifier.weight(1f))
                }
            }

            // Row 2: Add Note + Chi phí
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionCard(
                    modifier = Modifier.weight(1f),
                    emoji = "📷",
                    title = "Thêm Note",
                    subtitle = "Ghi chép & ảnh",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { navController.navigate(Screen.AddNote.createRoute(trip.id)) }
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

        Spacer(Modifier.height(20.dp))

        // ─── Status change button ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Trạng thái chuyến đi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when (trip.status) {
                TripStatus.PLANNING -> {
                    Button(
                        onClick = { onStatusChange(TripStatus.ONGOING) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusOngoing
                        )
                    ) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Đặt trạng thái: Đang đi",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                TripStatus.ONGOING -> {
                    Button(
                        onClick = { onStatusChange(TripStatus.DONE) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusDone
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Đặt trạng thái: Hoàn thành",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                TripStatus.DONE -> {
                    OutlinedButton(
                        onClick = { onStatusChange(TripStatus.PLANNING) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Đặt lại thành Sắp đi")
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
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
    Card(
        onClick = onClick,
        modifier = modifier.then(if (fullWidth) Modifier.height(68.dp) else Modifier.height(100.dp)),
        shape = RoundedCornerShape(16.dp),
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
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
                Column {
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
                Spacer(Modifier.weight(1f))
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
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
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

// ─── Trip type gradient helper ────────────────────────────────────────────────

private fun tripTypeGradient(type: TripType): List<Color> = when (type) {
    TripType.CAR       -> listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
    TripType.MOTORBIKE -> listOf(Color(0xFFBF360C), Color(0xFFFF7043))
    TripType.PUBLIC    -> listOf(Color(0xFF1B5E20), Color(0xFF66BB6A))
    TripType.TREKKING  -> listOf(Color(0xFF4A148C), Color(0xFFAB47BC))
    TripType.CAMPING   -> listOf(Color(0xFF004D40), Color(0xFF26A69A))
    TripType.OTHER     -> listOf(Color(0xFF37474F), Color(0xFF78909C))
}
