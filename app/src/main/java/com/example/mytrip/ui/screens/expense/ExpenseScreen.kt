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
import com.example.mytrip.ui.components.MyTripChip
import com.example.mytrip.ui.components.MyTripTextField
import com.example.mytrip.ui.components.MyTripPrimaryButton
import com.example.mytrip.ui.components.MyTripSecondaryButton
import com.example.mytrip.ui.components.GlassmorphismCard
import com.example.mytrip.ui.theme.TripThemeProvider
import com.example.mytrip.ui.theme.spacing

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

    TripThemeProvider(trip = trip) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = { Text("💰 Chi phí") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                },
                actions = {}
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
                    text = { Text("Thêm chi tiêu") }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("📊 Ngân sách") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("📝 Thực tế") })
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
            title = { Text("${exp.category.icon} ${exp.category.label}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Toggle Detailed vs Quick mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MyTripChip(
                            text = "Nhập nhanh",
                            selected = !isDetailedMode,
                            onClick = { isDetailedMode = false }
                        )
                        MyTripChip(
                            text = "🧮 Tính chi tiết",
                            selected = isDetailedMode,
                            onClick = { isDetailedMode = true }
                        )
                    }

                    if (!isDetailedMode) {
                        MyTripTextField(
                            value = input,
                            onValueChange = { input = it.filter { c -> c.isDigit() } },
                            label = "Dự kiến",
                            suffix = "k",
                            placeholder = "VD: 500 = 500.000₫",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Detailed inputs
                        when (exp.category) {
                            ExpenseCategory.HOTEL -> {
                                MyTripTextField(
                                    value = hotelPrice,
                                    onValueChange = { hotelPrice = it.filter { c -> c.isDigit() } },
                                    label = "Giá phòng / đêm (k)",
                                    suffix = "k",
                                    placeholder = "VD: 500 = 500.000₫",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                MyTripTextField(
                                    value = hotelNights,
                                    onValueChange = { hotelNights = it.filter { c -> c.isDigit() } },
                                    label = "Số đêm",
                                    placeholder = "VD: 3",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            ExpenseCategory.FOOD -> {
                                MyTripTextField(
                                    value = foodCostPerPersonPerDay,
                                    onValueChange = { foodCostPerPersonPerDay = it.filter { c -> c.isDigit() } },
                                    label = "Tiền ăn / người / ngày (k)",
                                    suffix = "k",
                                    placeholder = "VD: 150 = 150.000₫",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                MyTripTextField(
                                    value = foodDays,
                                    onValueChange = { foodDays = it.filter { c -> c.isDigit() } },
                                    label = "Số ngày",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                MyTripTextField(
                                    value = foodPeople,
                                    onValueChange = { foodPeople = it.filter { c -> c.isDigit() } },
                                    label = "Số người ăn",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            ExpenseCategory.TICKET -> {
                                MyTripTextField(
                                    value = ticketPrice,
                                    onValueChange = { ticketPrice = it.filter { c -> c.isDigit() } },
                                    label = "Giá vé / người (k)",
                                    suffix = "k",
                                    placeholder = "VD: 100 = 100.000₫",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                MyTripTextField(
                                    value = ticketCount,
                                    onValueChange = { ticketCount = it.filter { c -> c.isDigit() } },
                                    label = "Số vé",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            else -> {
                                MyTripTextField(
                                    value = genericPrice,
                                    onValueChange = { genericPrice = it.filter { c -> c.isDigit() } },
                                    label = "Đơn giá / khoản chi (k)",
                                    suffix = "k",
                                    placeholder = "VD: 200 = 200.000₫",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                MyTripTextField(
                                    value = genericQuantity,
                                    onValueChange = { genericQuantity = it.filter { c -> c.isDigit() } },
                                    label = "Số lượng / Số chặng",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                MyTripPrimaryButton(onClick = {
                    vm.updatePlanned(exp.copy(planned = MoneyUtils.inputToVnd(MoneyUtils.parseInput(input))))
                    editExpense = null
                }) { Text("Lưu") }
            },
            dismissButton = { MyTripSecondaryButton(onClick = { editExpense = null }) { Text("Hủy") } }
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

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Summary card
            GlassmorphismCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Dự kiến", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                            Text(MoneyUtils.formatShort(totalPlanned), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Thực tế", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f))
                            Text(MoneyUtils.formatShort(totalActual),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (overBudget) MaterialTheme.colorScheme.error else Color(0xFF2E7D32))
                        }
                    }
                    LinearProgressIndicator(
                        progress = { ratio },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(8.dp)),
                        color = if (overBudget) MaterialTheme.colorScheme.error else Color(0xFF4CAF50),
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.2f)
                    )
                    if (overBudget)
                        Text("⚠️ Vượt ngân sách ${MoneyUtils.formatShort(totalActual - totalPlanned)}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelMedium)
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
            GlassmorphismCard(
                modifier = Modifier.fillMaxWidth().clickable(enabled = !isDone) {
                    onCategoryClick(exp.category)
                }
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(exp.category.icon, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(exp.category.label, fontWeight = FontWeight.Medium)
                        Text(subtitle, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(MoneyUtils.formatShort(exp.planned),
                            color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Text(MoneyUtils.formatShort(actual), fontWeight = FontWeight.Bold,
                            color = if (actual > exp.planned && exp.planned > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        item {
            // Per person
            GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Dự kiến/người", style = MaterialTheme.typography.labelSmall)
                        Text(MoneyUtils.formatShort(if (numPeople > 0) totalPlanned / numPeople else 0), fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Thực tế/người", style = MaterialTheme.typography.labelSmall)
                        Text(MoneyUtils.formatShort(if (numPeople > 0) totalActual / numPeople else 0), fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
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
            item { Text(date, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }
            items(dayRecords, key = { it.id }) { rec ->
                GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(rec.category.icon, fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(rec.description.ifEmpty { rec.category.label }, fontWeight = FontWeight.Medium)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(top = 4.dp)
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
                            Text(MoneyUtils.formatShort(rec.amount), fontWeight = FontWeight.Bold,
                                fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            if (isEditable) {
                                IconButton(onClick = { deleteTarget = rec }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Balance summary
        if (memberBalances.isNotEmpty()) {
            item { Spacer(Modifier.height(8.dp)) }
            item {
                Text("⚖️ Quyết toán", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(memberNames) { name ->
                val balance = memberBalances[name] ?: 0L
                GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(name, fontWeight = FontWeight.Medium)
                        if (balance >= 0) {
                            Text("✅ Được hoàn ${MoneyUtils.formatShort(balance)}", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                        } else {
                            Text("❗ Cần trả thêm ${MoneyUtils.formatShort(-balance)}", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
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
            confirmButton = { Button(onClick = { onDeleteRecord(rec); deleteTarget = null },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Xóa") } },
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

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Thêm chi tiêu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Text("Hạng mục", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ExpenseCategory.values()) { cat ->
                    MyTripChip(
                        text = "${cat.icon} ${cat.label}",
                        selected = category == cat,
                        onClick = { category = cat }
                    )
                }
            }

            MyTripTextField(
                value = amountInput,
                onValueChange = { amountInput = it.filter { c -> c.isDigit() } },
                modifier = Modifier.fillMaxWidth(),
                label = "Số tiền",
                suffix = "k",
                placeholder = "VD: 200 = 200.000₫",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            if (amountInput.isNotEmpty())
                Text("= ${MoneyUtils.formatVnd(MoneyUtils.inputToVnd(MoneyUtils.parseInput(amountInput)))}",
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MoneyUtils.SHORTCUTS) { a ->
                    SuggestionChip(onClick = { amountInput = a.toString() },
                        label = { Text(if (a >= 1000) "${a/1000}M" else "${a}k") })
                }
            }

            Text("Ai trả", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(memberNames) { m ->
                    MyTripChip(
                        text = m,
                        selected = paidBy == m,
                        onClick = { paidBy = m }
                    )
                }
            }

            MyTripTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = "Mô tả (tùy chọn)",
                singleLine = true
            )

            MyTripPrimaryButton(
                onClick = {
                    onSave(ExpenseRecord(
                        tripId = 0L, category = category,
                        amount = MoneyUtils.inputToVnd(MoneyUtils.parseInput(amountInput)),
                        paidBy = paidBy, description = description,
                        timestamp = System.currentTimeMillis()
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = amountInput.isNotEmpty()
            ) { Text("Lưu chi tiêu", fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(8.dp))
        }
    }
}
