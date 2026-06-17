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
import androidx.compose.material.icons.rounded.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.navigation.NavController
import com.example.mytrip.MyTripApplication
import org.json.JSONArray
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

    val trip by vm.trip.collectAsStateWithLifecycle()
    val expenses by vm.expenses.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()
    val totalPlanned by vm.totalPlanned.collectAsStateWithLifecycle()
    val totalActual by vm.totalActual.collectAsStateWithLifecycle()
    val memberBalances by vm.memberBalances.collectAsStateWithLifecycle()
    val transfers by vm.transfers.collectAsStateWithLifecycle()

    val memberNames = remember(trip) {
        try {
            val raw = trip?.memberNames ?: return@remember listOf("Tôi")
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }.filter { it.isNotBlank() }
                .ifEmpty { listOf("Tôi") }
        } catch (_: Exception) {
            listOf("Tôi")
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    var showAddRecord by remember { mutableStateOf(false) }
    var editExpense by remember { mutableStateOf<Expense?>(null) }
    var editRecord by remember { mutableStateOf<ExpenseRecord?>(null) }
    var selectedCategoryForNewRecord by remember { mutableStateOf(ExpenseCategory.FOOD) }

    TripThemeProvider(trip = trip) {
        Scaffold(
            topBar = {
            TopAppBar(
                title = { Text("💰 Chi phí") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Rounded.ArrowBack, null)
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
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text("Thêm chi tiêu") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
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
                    transfers = transfers,
                    memberNames = memberNames,
                    isEditable = trip?.status == TripStatus.ONGOING,
                    onDeleteRecord = { vm.deleteRecord(it) },
                    onEditRecord = { editRecord = it }
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

    // Edit record bottom sheet
    editRecord?.let { rec ->
        AddExpenseRecordSheet(
            initialCategory = rec.category,
            initialAmount = (rec.amount / 1000L).toString(),
            initialPaidBy = rec.paidBy,
            initialAdvancedTo = rec.advancedTo ?: "",
            initialDescription = rec.description,
            memberNames = memberNames,
            onDismiss = { editRecord = null },
            onSave = { updated ->
                vm.updateRecord(updated.copy(id = rec.id, tripId = tripId, timestamp = rec.timestamp, noteId = rec.noteId))
                editRecord = null
            }
        )
    }
}
}

