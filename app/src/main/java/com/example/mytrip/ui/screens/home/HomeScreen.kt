package com.example.mytrip.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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

// ─── Gradient per trip type ───────────────────────────────────────────────────

private fun tripGradient(type: TripType): Brush = when (type) {
    TripType.CAR       -> Brush.linearGradient(listOf(Color(0xFF1A73E8), Color(0xFF4DABF7)))
    TripType.MOTORBIKE -> Brush.linearGradient(listOf(Color(0xFFE8710A), Color(0xFFFFB347)))
    TripType.PUBLIC    -> Brush.linearGradient(listOf(Color(0xFF137333), Color(0xFF34A853)))
    TripType.TREKKING  -> Brush.linearGradient(listOf(Color(0xFF7B1FA2), Color(0xFFAB47BC)))
    TripType.CAMPING   -> Brush.linearGradient(listOf(Color(0xFF00695C), Color(0xFF26A69A)))
    TripType.OTHER     -> Brush.linearGradient(listOf(Color(0xFF5F6368), Color(0xFF9AA0A6)))
}

private fun statusInfo(status: TripStatus): Triple<String, Color, Color> = when (status) {
    TripStatus.PLANNING -> Triple("Sắp đi",    Color(0xFF1A73E8), Color(0xFFD2E3FC))
    TripStatus.ONGOING  -> Triple("Đang đi",   Color(0xFF137333), Color(0xFFB7F1C5))
    TripStatus.DONE     -> Triple("Hoàn thành",Color(0xFF5F6368), Color(0xFFE8EAED))
}

// ─── HomeScreen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
) {
    val trips by viewModel.allTrips.collectAsState()
    val activeFilter by viewModel.filter.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = "MyTrip",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.importSampleTrip() },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Tải mẫu", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor      = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor   = MaterialTheme.colorScheme.onSurface
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateTrip.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Tạo chuyến đi", modifier = Modifier.size(24.dp))
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── Filter chips ──────────────────────────────────────────────
            FilterChipsRow(
                activeFilter     = activeFilter,
                onFilterSelected = { viewModel.filterByStatus(it) }
            )

            // ── Trip list or empty state ───────────────────────────────────
            if (trips.isEmpty()) {
                EmptyState(
                    onImportSampleClick = { viewModel.importSampleTrip() },
                    onCreateClick       = { navController.navigate(Screen.CreateTrip.route) },
                    modifier            = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start  = 16.dp,
                        end    = 16.dp,
                        top    = 8.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(trips, key = { it.id }) { trip ->
                        AnimatedVisibility(
                            visible = true,
                            enter   = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 },
                            exit    = fadeOut(tween(300))
                        ) {
                            TripCard(
                                trip            = trip,
                                onCardClick     = { navController.navigate(Screen.TripDetail.createRoute(trip.id)) },
                                onItineraryClick= { navController.navigate(Screen.Itinerary.createRoute(trip.id)) },
                                onTodayClick    = { navController.navigate(Screen.Today.createRoute(trip.id)) },
                                onExpenseClick  = { navController.navigate(Screen.Expense.createRoute(trip.id)) },
                                onDeleteConfirmed = { viewModel.deleteTrip(trip) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Filter chips row ─────────────────────────────────────────────────────────

@Composable
private fun FilterChipsRow(
    activeFilter: TripStatus?,
    onFilterSelected: (TripStatus?) -> Unit
) {
    data class FilterOption(val label: String, val status: TripStatus?)

    val options = listOf(
        FilterOption("Tất cả",     null),
        FilterOption("Sắp đi",     TripStatus.PLANNING),
        FilterOption("Đang đi",    TripStatus.ONGOING),
        FilterOption("Hoàn thành", TripStatus.DONE)
    )

    LazyRow(
        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { option ->
            val selected = activeFilter == option.status
            FilterChip(
                selected  = selected,
                onClick   = { onFilterSelected(option.status) },
                label     = { Text(option.label, style = MaterialTheme.typography.labelLarge) },
                colors    = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor     = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled          = true,
                    selected         = selected,
                    selectedBorderColor = MaterialTheme.colorScheme.primary,
                    selectedBorderWidth = 1.dp
                )
            )
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    onImportSampleClick: () -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier          = modifier.padding(24.dp),
        contentAlignment  = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "🗺️", fontSize = 72.sp)

            Text(
                text  = "Chưa có chuyến đi nào",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text      = "Tạo chuyến đi mới hoặc tải lịch trình mẫu Xuyên Việt 30 ngày để khám phá đầy đủ tính năng.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick  = onCreateClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tạo chuyến đi mới", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick  = onImportSampleClick,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Text("Tải lịch trình Xuyên Việt mẫu", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── TripCard ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TripCard(
    trip: Trip,
    onCardClick: () -> Unit,
    onItineraryClick: () -> Unit,
    onTodayClick: () -> Unit,
    onExpenseClick: () -> Unit,
    onDeleteConfirmed: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            tripName  = trip.name,
            onConfirm = { showDeleteDialog = false; onDeleteConfirmed() },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick    = onCardClick,
                onLongClick = { showDeleteDialog = true }
            ),
        shape     = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            // ── Gradient header ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(
                        brush = tripGradient(trip.type),
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text(
                        text     = trip.name,
                        style    = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color      = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text  = "${trip.type.icon} ${trip.type.label}",
                        style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f))
                    )
                }

                // Status chip — top right
                val (statusLabel, statusFg, statusBg) = statusInfo(trip.status)
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape    = RoundedCornerShape(50),
                    color    = statusBg
                ) {
                    Text(
                        text     = statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style    = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color      = statusFg
                        )
                    )
                }
            }

            // ── Info section ──────────────────────────────────────────────
            Row(
                modifier             = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment    = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: dates
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        modifier    = Modifier.size(15.dp),
                        tint        = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text  = "${DateUtils.formatDate(trip.startDate)} – ${DateUtils.formatDate(trip.endDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Right: duration + people
                Row(
                    verticalAlignment    = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val numDays = DateUtils.countDays(trip.startDate, trip.endDate)
                    InfoChip("$numDays ngày")

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Group,
                            contentDescription = null,
                            modifier    = Modifier.size(14.dp),
                            tint        = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text  = "${trip.numPeople}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── Divider ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            )

            // ── Action buttons ────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TripActionButton(
                    icon  = Icons.Filled.CalendarMonth,
                    label = "Lịch trình",
                    tint  = MaterialTheme.colorScheme.primary,
                    onClick = onItineraryClick
                )
                TripActionButton(
                    icon  = Icons.Filled.WbSunny,
                    label = "Hôm nay",
                    tint  = MaterialTheme.colorScheme.tertiary,
                    onClick = onTodayClick
                )
                TripActionButton(
                    icon  = Icons.Filled.MonetizationOn,
                    label = "Chi phí",
                    tint  = Color(0xFF137333),
                    onClick = onExpenseClick
                )
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Text(
            text     = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style    = MaterialTheme.typography.labelSmall.copy(
                color      = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun TripActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        colors  = ButtonDefaults.textButtonColors(contentColor = tint),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier    = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

// ─── Delete confirm dialog ────────────────────────────────────────────────────

@Composable
private fun DeleteConfirmDialog(
    tripName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text  = "Xóa chuyến đi",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text  = {
            Text(
                text  = "Bạn có chắc muốn xóa \"$tripName\"? Hành động này không thể hoàn tác.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor   = MaterialTheme.colorScheme.onError
                )
            ) { Text("Xóa") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
