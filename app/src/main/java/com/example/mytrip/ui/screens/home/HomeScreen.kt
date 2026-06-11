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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.mytrip.ui.components.bounceClick
import com.example.mytrip.ui.theme.spacing
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.mytrip.R
import androidx.navigation.NavController
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.db.entities.TripStatus
import com.example.mytrip.data.db.entities.TripType
import com.example.mytrip.navigation.Screen
import com.example.mytrip.ui.components.MyTripSecondaryButton
import com.example.mytrip.util.DateUtils

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
    val trips by viewModel.allTrips.collectAsStateWithLifecycle()
    val activeFilter by viewModel.filter.collectAsStateWithLifecycle()

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
                icon = { Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(24.dp)) },
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

            // ── Search bar ────────────────────────────────────────────────
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                placeholder = { Text("🔍 Tìm chuyến đi...") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Xóa")
                        }
                    }
                }
            )

            if (trips.isEmpty()) {
                EmptyState(
                    modifier = Modifier.fillMaxSize(),
                    onCreateSampleTrip = { viewModel.importSampleTrip() }
                )
            } else {
                var visibleIds by remember { mutableStateOf(emptySet<Long>()) }
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
                        LaunchedEffect(trip.id) {
                            visibleIds = visibleIds + trip.id
                        }
                        AnimatedVisibility(
                            visible = trip.id in visibleIds,
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
                                onNotesClick = {
                                    navController.navigate(Screen.AllNotes.createRoute(trip.id))
                                },
                                onSummaryClick = if (trip.status == TripStatus.DONE) {
                                    { navController.navigate(Screen.Summary.createRoute(trip.id)) }
                                } else null,
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
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
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
    modifier: Modifier = Modifier,
    onCreateSampleTrip: () -> Unit
) {
    Box(
        modifier = modifier.padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
        contentAlignment = Alignment.TopCenter
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
                
                MyTripSecondaryButton(onClick = onCreateSampleTrip) {
                    Text("Tạo chuyến đi mẫu", fontWeight = FontWeight.Bold)
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
    onNotesClick: () -> Unit,
    onSummaryClick: (() -> Unit)? = null,
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
            .bounceClick()
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = { showDeleteDialog = true }
            )
    ) {
        Column {
            // ── Top header (Colored) ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(brush = com.example.mytrip.ui.theme.TripThemeColors.getThemeGradient(trip.themeColor), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
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
                            imageVector = Icons.Rounded.CalendarMonth,
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
                        imageVector = Icons.Rounded.Group,
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

                // ── Progress indicator for ONGOING trips ──────────────────
                if (trip.status == TripStatus.ONGOING) {
                    val today = DateUtils.todayMillis()
                    val totalDays = DateUtils.countDays(trip.startDate, trip.endDate)
                    val daysPassed = DateUtils.countDays(trip.startDate, today).coerceIn(0, totalDays)
                    val progress = if (totalDays > 0) daysPassed.toFloat() / totalDays else 0f

                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tiến độ: Ngày $daysPassed/$totalDays",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
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

            // ── Action buttons — 4 nút cùng 1 hàng ──────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Lịch trình
                TextButton(
                    onClick = onItineraryClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Lịch trình",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Hôm nay
                TextButton(
                    onClick = onTodayClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WbSunny,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Hôm nay",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Chi phí
                TextButton(
                    onClick = onExpenseClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF2E7D32)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Chi phí",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Nhật ký
                TextButton(
                    onClick = onNotesClick,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF7B1FA2)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Nhật ký",
                        style = MaterialTheme.typography.labelSmall
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
                imageVector = Icons.Rounded.Delete,
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
