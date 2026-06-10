package com.example.mytrip.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.mytrip.ui.components.MyTripChip
import com.example.mytrip.ui.components.GlassmorphismCard
import com.example.mytrip.ui.components.MyTripPrimaryButton
import com.example.mytrip.ui.theme.spacing
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mytrip.R
import androidx.navigation.NavController
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.db.entities.TripStatus
import com.example.mytrip.data.db.entities.TripType
import com.example.mytrip.navigation.Screen
import com.example.mytrip.util.DateUtils

// ─── Gradient colors per trip type ───────────────────────────────────────────

private fun tripGradient(type: TripType): Brush = when (type) {
    TripType.CAR -> Brush.linearGradient(
        listOf(Color(0xFF1565C0), Color(0xFF42A5F5))
    )
    TripType.MOTORBIKE -> Brush.linearGradient(
        listOf(Color(0xFFE65100), Color(0xFFFF9800))
    )
    TripType.PUBLIC -> Brush.linearGradient(
        listOf(Color(0xFF2E7D32), Color(0xFF66BB6A))
    )
    TripType.TREKKING -> Brush.linearGradient(
        listOf(Color(0xFF6A1B9A), Color(0xFFAB47BC))
    )
    TripType.CAMPING -> Brush.linearGradient(
        listOf(Color(0xFF00695C), Color(0xFF26A69A))
    )
    TripType.OTHER -> Brush.linearGradient(
        listOf(Color(0xFF37474F), Color(0xFF78909C))
    )
}

private fun statusChipColors(status: TripStatus): Pair<Color, Color> = when (status) {
    TripStatus.PLANNING -> Pair(Color(0xFF1565C0), Color(0xFFE3F2FD))
    TripStatus.ONGOING  -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
    TripStatus.DONE     -> Pair(Color(0xFF546E7A), Color(0xFFECEFF1))
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.logo),
                            contentDescription = "App Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "My Trip",
                            style = androidx.compose.ui.text.TextStyle(
                                brush = Brush.linearGradient(
                                    listOf(Color(0xFF00B4DB), Color(0xFF0083B0))
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Cursive,
                                fontSize = 34.sp,
                                letterSpacing = 1.sp
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate(Screen.CreateTrip.route) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(24.dp)) },
                text = { Text("Tạo chuyến đi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(24.dp),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 8.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            // ── Filter chips ──────────────────────────────────────────────
            FilterChipsRow(
                activeFilter = activeFilter,
                onFilterSelected = { viewModel.filterByStatus(it) }
            )

            // ── Trip list or empty state ───────────────────────────────────
            if (trips.isEmpty()) {
                EmptyState(
                    onImportSampleClick = { viewModel.importSampleTrip() },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = MaterialTheme.spacing.marginMobile,
                        end = MaterialTheme.spacing.marginMobile,
                        top = MaterialTheme.spacing.small,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.gutterSm),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(trips, key = { it.id }) { trip ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 },
                            exit = fadeOut(tween(300))
                        ) {
                            TripCard(
                                trip = trip,
                                onCardClick = {
                                    navController.navigate(Screen.TripDetail.createRoute(trip.id))
                                },
                                onItineraryClick = {
                                    navController.navigate(Screen.Itinerary.createRoute(trip.id))
                                },
                                onTodayClick = {
                                    navController.navigate(Screen.Today.createRoute(trip.id))
                                },
                                onExpenseClick = {
                                    navController.navigate(Screen.Expense.createRoute(trip.id))
                                },
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
        FilterOption("Tất cả", null),
        FilterOption("Sắp đi", TripStatus.PLANNING),
        FilterOption("Đang đi", TripStatus.ONGOING),
        FilterOption("Đã kết thúc", TripStatus.DONE)
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { option ->
            val selected = activeFilter == option.status
            MyTripChip(
                text = option.label,
                selected = selected,
                onClick = { onFilterSelected(option.status) }
            )
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(
    onImportSampleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        GlassmorphismCard(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp).fillMaxWidth()
            ) {
                Text(
                    text = "✈️",
                    fontSize = 80.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Chưa có chuyến đi nào",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Hãy tạo một hành trình mới để bắt đầu lên lịch trình, theo dõi hôm nay và quản lý chi phí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                MyTripPrimaryButton(
                    onClick = onImportSampleClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = "🚀 Tải chuyến đi mẫu"
                    )
                }
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
            tripName = trip.name,
            onConfirm = {
                showDeleteDialog = false
                onDeleteConfirmed()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    GlassmorphismCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = { showDeleteDialog = true }
            )
    ) {
        Column {
            // ── Gradient header ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .background(brush = tripGradient(trip.type), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .padding(16.dp)
            ) {
                // Trip name
                Column(modifier = Modifier.align(Alignment.CenterStart).padding(end = 90.dp)) {
                    Text(
                        text = trip.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${trip.type.icon} ${trip.type.label}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.White.copy(alpha = 0.9f))
                        )
                    }
                }

                // Status chip
                val (chipText, chipFg, chipBg) = run {
                    val (fg, bg) = statusChipColors(trip.status)
                    Triple(trip.status.label, fg, bg)
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd),
                    shape = RoundedCornerShape(50),
                    color = chipBg
                ) {
                    Text(
                        text = chipText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = chipFg
                        )
                    )
                }
            }

            // ── Info section ──────────────────────────────────────────────
            Column(modifier = Modifier.padding(16.dp)) {
                // Dates & duration row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${DateUtils.formatDate(trip.startDate)} → ${DateUtils.formatDate(trip.endDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Num days
                    val numDays = DateUtils.countDays(trip.startDate, trip.endDate)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "$numDays ngày",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Num people row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Group,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${trip.numPeople} người",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Divider ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // ── Action buttons ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Lịch trình
                TextButton(
                    onClick = onItineraryClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Lịch trình",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Hôm nay
                TextButton(
                    onClick = onTodayClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hôm nay",
                        style = MaterialTheme.typography.labelMedium
                    )
                }

                // Chi phí
                TextButton(
                    onClick = onExpenseClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF2E7D32)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Today,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Chi phí",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
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
        icon = {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Xóa chuyến đi",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Text(
                text = "Bạn có chắc muốn xóa \"$tripName\"? Hành động này không thể hoàn tác.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Xóa")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
