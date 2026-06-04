package com.example.mytrip.ui.screens.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.*
import com.example.mytrip.util.DateUtils
import com.example.mytrip.util.MoneyUtils

// Category accent colors (kept as constants, not hardcoded as theme colors)
private fun categoryColor(category: ExpenseCategory): Color = when (category) {
    ExpenseCategory.FOOD         -> Color(0xFF2E7D32)
    ExpenseCategory.HOTEL        -> Color(0xFF1565C0)
    ExpenseCategory.TRANSPORT    -> Color(0xFF6A1B9A)
    ExpenseCategory.TICKET       -> Color(0xFFE65100)
    ExpenseCategory.SHOPPING     -> Color(0xFFAD1457)
    ExpenseCategory.OTHER        -> Color(0xFF00695C)
    else                         -> Color(0xFF455A64)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(navController: NavController, tripId: Long) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as MyTripApplication
    val vm: ExpenseViewModel = viewModel(factory = ExpenseViewModel.factory(app))

    LaunchedEffect(tripId) { vm.loadData(tripId) }

    val trip by vm.trip.collectAsState()
    val expenses by vm.expenses.collectAsState()
    val records by vm.records.collectAsState()
    val totalPlanned by vm.totalPlanned.collectAsState()
    val totalActual by vm.totalActual.collectAsState()
    val memberBalances by vm.memberBalances.collectAsState()

    val memberNames = remember(trip) {
        trip?.memberNames?.trim('[', ']')
            ?.split(",")?.map { it.trim().trim('"') }?.filter { it.isNotBlank() }
            ?: listOf("Tôi")
    }

    var selectedTab by remember { mutableStateOf(0) }
    var showAddRecord by remember { mutableStateOf(false) }
    var editExpense by remember { mutableStateOf<Expense?>(null) }
    var selectedCategoryForNewRecord by remember { mutableStateOf(ExpenseCategory.FOOD) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chi phí",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            if (trip?.status == TripStatus.ONGOING) {
                ExtendedFloatingActionButton(
                    onClick = {
                        selectedCategoryForNewRecord = ExpenseCategory.FOOD
                        showAddRecord = true
                    },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Thêm chi tiêu", fontWeight = FontWeight.Medium) }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "Ngân sách",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Thực tế",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> BudgetTab(
                    trip = trip,
                    expenses = expenses,
                    records = records,
                    totalPlanned = totalPlanned,
                    totalActual = totalActual,
                    onCategoryClick = { category ->
                        if (trip?.status == TripStatus.ONGOING) {
                            selectedCategoryForNewRecord = category
                            showAddRecord = true
                        } else if (trip?.status == TripStatus.PLANNING) {
                            editExpense = expenses.find { it.category == category }
                        }
                    }
                )
                1 -> ActualTab(
                    records = records,
                    memberBalances = memberBalances,
                    memberNames = memberNames,
                    isEditable = trip?.status == TripStatus.ONGOING,
                    onDeleteRecord = { vm.deleteRecord(it) }
                )
            }
        }
    }

    // Edit planned budget dialog
    editExpense?.let { exp ->
        var isDetailedMode by remember(exp) { mutableStateOf(false) }

        val tripNumDays = trip?.let { DateUtils.countDays(it.startDate, it.endDate) }?.coerceAtLeast(1) ?: 1
        val tripNumPeople = trip?.numPeople ?: 1

        // States for detailed mode
        var hotelPrice by remember(exp) { mutableStateOf("") }
        var hotelNights by remember(exp) { mutableStateOf((tripNumDays - 1).coerceAtLeast(1).toString()) }

        var foodCostPerPersonPerDay by remember(exp) { mutableStateOf("") }
        var foodDays by remember(exp) { mutableStateOf(tripNumDays.toString()) }
        var foodPeople by remember(exp) { mutableStateOf(tripNumPeople.toString()) }

        var ticketPrice by remember(exp) { mutableStateOf("") }
        var ticketCount by remember(exp) { mutableStateOf(tripNumPeople.toString()) }

        var genericPrice by remember(exp) { mutableStateOf("") }
        var genericQuantity by remember(exp) { mutableStateOf("1") }

        var input by remember(exp) { mutableStateOf(MoneyUtils.vndToInput(exp.planned).toString().takeIf { it != "0" } ?: "") }

        // Automatically update the main input when in detailed mode
        LaunchedEffect(
            isDetailedMode, exp.category,
            hotelPrice, hotelNights,
            foodCostPerPersonPerDay, foodDays, foodPeople,
            ticketPrice, ticketCount,
            genericPrice, genericQuantity
        ) {
            if (isDetailedMode) {
                val calculatedValue = when (exp.category) {
                    ExpenseCategory.HOTEL -> {
                        val p = hotelPrice.toLongOrNull() ?: 0L
                        val n = hotelNights.toLongOrNull() ?: 0L
                        p * n
                    }
                    ExpenseCategory.FOOD -> {
                        val c = foodCostPerPersonPerDay.toLongOrNull() ?: 0L
                        val d = foodDays.toLongOrNull() ?: 0L
                        val p = foodPeople.toLongOrNull() ?: 0L
                        c * d * p
                    }
                    ExpenseCategory.TICKET -> {
                        val p = ticketPrice.toLongOrNull() ?: 0L
                        val c = ticketCount.toLongOrNull() ?: 0L
                        p * c
                    }
                    else -> {
                        val p = genericPrice.toLongOrNull() ?: 0L
                        val q = genericQuantity.toLongOrNull() ?: 0L
                        p * q
                    }
                }
                input = if (calculatedValue > 0) calculatedValue.toString() else ""
            }
        }

        AlertDialog(
            onDismissRequest = { editExpense = null },
            title = { Text("${exp.category.icon} ${exp.category.label}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Toggle Detailed vs Quick mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !isDetailedMode,
                            onClick = { isDetailedMode = false },
                            label = { Text("Nhập nhanh") }
                        )
                        FilterChip(
                            selected = isDetailedMode,
                            onClick = { isDetailedMode = true },
                            label = { Text("Tính chi tiết") }
                        )
                    }

                    if (!isDetailedMode) {
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it.filter { c -> c.isDigit() } },
                            label = { Text("Dự kiến") },
                            suffix = { Text("k") },
                            placeholder = { Text("VD: 500 = 500.000₫") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Detailed inputs
                        when (exp.category) {
                            ExpenseCategory.HOTEL -> {
                                OutlinedTextField(
                                    value = hotelPrice,
                                    onValueChange = { hotelPrice = it.filter { c -> c.isDigit() } },
                                    label = { Text("Giá phòng / đêm (k)") },
                                    suffix = { Text("k") },
                                    placeholder = { Text("VD: 500 = 500.000₫") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = hotelNights,
                                    onValueChange = { hotelNights = it.filter { c -> c.isDigit() } },
                                    label = { Text("Số đêm") },
                                    placeholder = { Text("VD: 3") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            ExpenseCategory.FOOD -> {
                                OutlinedTextField(
                                    value = foodCostPerPersonPerDay,
                                    onValueChange = { foodCostPerPersonPerDay = it.filter { c -> c.isDigit() } },
                                    label = { Text("Tiền ăn / người / ngày (k)") },
                                    suffix = { Text("k") },
                                    placeholder = { Text("VD: 150 = 150.000₫") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = foodDays,
                                    onValueChange = { foodDays = it.filter { c -> c.isDigit() } },
                                    label = { Text("Số ngày") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = foodPeople,
                                    onValueChange = { foodPeople = it.filter { c -> c.isDigit() } },
                                    label = { Text("Số người ăn") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            ExpenseCategory.TICKET -> {
                                OutlinedTextField(
                                    value = ticketPrice,
                                    onValueChange = { ticketPrice = it.filter { c -> c.isDigit() } },
                                    label = { Text("Giá vé / người (k)") },
                                    suffix = { Text("k") },
                                    placeholder = { Text("VD: 100 = 100.000₫") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = ticketCount,
                                    onValueChange = { ticketCount = it.filter { c -> c.isDigit() } },
                                    label = { Text("Số vé") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            else -> {
                                OutlinedTextField(
                                    value = genericPrice,
                                    onValueChange = { genericPrice = it.filter { c -> c.isDigit() } },
                                    label = { Text("Đơn giá / khoản chi (k)") },
                                    suffix = { Text("k") },
                                    placeholder = { Text("VD: 200 = 200.000₫") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = genericQuantity,
                                    onValueChange = { genericQuantity = it.filter { c -> c.isDigit() } },
                                    label = { Text("Số lượng / Số chặng") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (input.isNotEmpty()) {
                        Text(
                            text = "= ${MoneyUtils.formatVnd(MoneyUtils.inputToVnd(MoneyUtils.parseInput(input)))}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isDetailedMode) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(MoneyUtils.SHORTCUTS) { a ->
                                SuggestionChip(onClick = { input = a.toString() },
                                    label = { Text(if (a >= 1000) "${a/1000}M" else "${a}k") })
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    vm.updatePlanned(exp.copy(planned = MoneyUtils.inputToVnd(MoneyUtils.parseInput(input))))
                    editExpense = null
                }) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { editExpense = null }) { Text("Hủy") } }
        )
    }

    // Add record bottom sheet
    if (showAddRecord) {
        AddExpenseRecordSheet(
            initialCategory = selectedCategoryForNewRecord,
            memberNames = memberNames,
            onDismiss = { showAddRecord = false },
            onSave = { record ->
                vm.addRecord(record.copy(tripId = tripId))
                showAddRecord = false
            }
        )
    }
}

@Composable
private fun BudgetTab(
    trip: Trip?,
    expenses: List<Expense>,
    records: List<ExpenseRecord>,
    totalPlanned: Long,
    totalActual: Long,
    onCategoryClick: (ExpenseCategory) -> Unit
) {
    val numPeople = trip?.numPeople ?: 1
    val ratio = if (totalPlanned > 0) (totalActual.toFloat() / totalPlanned).coerceIn(0f, 1f) else 0f
    val overBudget = totalActual > totalPlanned
    val progressColor = if (overBudget) MaterialTheme.colorScheme.error else Color(0xFF1A73E8)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Budget summary card — large, prominent
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "Dự kiến",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                            )
                            Text(
                                MoneyUtils.formatShort(totalPlanned),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Thực tế",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f)
                            )
                            Text(
                                MoneyUtils.formatShort(totalActual),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (overBudget) MaterialTheme.colorScheme.error
                                        else Color(0xFF137333)
                            )
                        }
                    }

                    // Progress bar 10dp, rounded
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50)),
                        color = progressColor,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                    )

                    if (overBudget) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Vượt ngân sách ${MoneyUtils.formatShort(totalActual - totalPlanned)}",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        items(expenses, key = { it.id }) { exp ->
            val actual = records.filter { it.category == exp.category }.sumOf { it.amount }
            val isOngoing = trip?.status == TripStatus.ONGOING
            val isDone = trip?.status == TripStatus.DONE
            val subtitle = when {
                isOngoing -> "Nhấn để thêm chi tiêu thực tế"
                isDone -> "Chuyến đi đã kết thúc"
                else -> "Nhấn để chỉnh dự kiến"
            }
            val catColor = categoryColor(exp.category)
            val catActualOver = actual > exp.planned && exp.planned > 0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isDone) { onCategoryClick(exp.category) },
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left accent bar
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(64.dp)
                            .background(catColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    )
                    // Icon circle
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(catColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(exp.category.icon, fontSize = 20.sp)
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp)
                    ) {
                        Text(exp.category.label, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.padding(end = 16.dp, top = 12.dp, bottom = 12.dp)
                    ) {
                        Text(
                            MoneyUtils.formatShort(exp.planned),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            MoneyUtils.formatShort(actual),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (catActualOver) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            // Per person summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Dự kiến / người",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            MoneyUtils.formatShort(if (numPeople > 0) totalPlanned / numPeople else 0),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Thực tế / người",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            MoneyUtils.formatShort(if (numPeople > 0) totalActual / numPeople else 0),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActualTab(
    records: List<ExpenseRecord>,
    memberBalances: Map<String, Long>,
    memberNames: List<String>,
    isEditable: Boolean,
    onDeleteRecord: (ExpenseRecord) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<ExpenseRecord?>(null) }
    val grouped = records.groupBy {
        DateUtils.formatDate(it.timestamp)
    }.toSortedMap(compareByDescending { it })

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (date, dayRecords) ->
            item {
                Text(
                    date,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }
            items(dayRecords, key = { it.id }) { rec ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category icon pill
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(categoryColor(rec.category).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(rec.category.icon, fontSize = 20.sp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                rec.description.ifEmpty { rec.category.label },
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(3.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = rec.paidBy,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                MoneyUtils.formatShort(rec.amount),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (isEditable) {
                                IconButton(
                                    onClick = { deleteTarget = rec },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Balance summary
        if (memberBalances.isNotEmpty()) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                Text(
                    "Quyết toán",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            items(memberNames) { name ->
                val balance = memberBalances[name] ?: 0L
                val isPositive = balance >= 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPositive)
                            MaterialTheme.colorScheme.tertiaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            name,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isPositive) MaterialTheme.colorScheme.onTertiaryContainer
                                    else MaterialTheme.colorScheme.onErrorContainer
                        )
                        if (isPositive) {
                            Text(
                                "Được hoàn ${MoneyUtils.formatShort(balance)}",
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text(
                                "Cần trả ${MoneyUtils.formatShort(-balance)}",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    deleteTarget?.let { rec ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Xóa chi tiêu?") },
            text = { Text("Bạn có chắc muốn xóa khoản ${MoneyUtils.formatShort(rec.amount)} (${rec.category.label})?") },
            confirmButton = {
                Button(
                    onClick = { onDeleteRecord(rec); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Hủy") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseRecordSheet(
    initialCategory: ExpenseCategory = ExpenseCategory.FOOD,
    memberNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (ExpenseRecord) -> Unit
) {
    var category by remember { mutableStateOf(initialCategory) }
    var amountInput by remember { mutableStateOf("") }
    var paidBy by remember { mutableStateOf(memberNames.firstOrNull() ?: "Tôi") }
    var description by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Thêm chi tiêu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Category section
            Text("Hạng mục", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExpenseCategory.values()) { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text("${cat.icon} ${cat.label}") }
                    )
                }
            }

            // Amount section
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it.filter { c -> c.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Số tiền") },
                suffix = { Text("k") },
                placeholder = { Text("VD: 200 = 200.000₫") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp)
            )
            if (amountInput.isNotEmpty()) {
                Text(
                    "= ${MoneyUtils.formatVnd(MoneyUtils.inputToVnd(MoneyUtils.parseInput(amountInput)))}",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Amount shortcuts
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MoneyUtils.SHORTCUTS) { a ->
                    SuggestionChip(
                        onClick = { amountInput = a.toString() },
                        label = { Text(if (a >= 1000) "${a/1000}M" else "${a}k") }
                    )
                }
            }

            // Payer section
            Text("Ai trả", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(memberNames) { m ->
                    FilterChip(selected = paidBy == m, onClick = { paidBy = m }, label = { Text(m) })
                }
            }

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mô tả (tùy chọn)") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            // Save button
            Button(
                onClick = {
                    onSave(
                        ExpenseRecord(
                            tripId = 0L, category = category,
                            amount = MoneyUtils.inputToVnd(MoneyUtils.parseInput(amountInput)),
                            paidBy = paidBy, description = description,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = amountInput.isNotEmpty()
            ) {
                Text("Lưu chi tiêu", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}
