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


@Composable
fun BudgetTab(
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
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
fun ActualTab(
    records: List<ExpenseRecord>,
    memberBalances: Map<String, Long>,
    memberNames: List<String>,
    isEditable: Boolean,
    onDeleteRecord: (ExpenseRecord) -> Unit,
    onEditRecord: (ExpenseRecord) -> Unit = {}
) {
    var deleteTarget by remember { mutableStateOf<ExpenseRecord?>(null) }
    // Group by actual day date if dayId is set, otherwise fall back to timestamp date
    val grouped = records.groupBy { rec ->
        DateUtils.formatDate(rec.timestamp)
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
                                Row {
                                    IconButton(onClick = { onEditRecord(rec) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { deleteTarget = rec }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
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
fun AddExpenseRecordSheet(
    initialCategory: ExpenseCategory = ExpenseCategory.FOOD,
    initialAmount: String = "",
    initialPaidBy: String = "",
    initialDescription: String = "",
    memberNames: List<String>,
    onDismiss: () -> Unit,
    onSave: (ExpenseRecord) -> Unit
) {
    var category by remember { mutableStateOf(initialCategory) }
    var amountInput by remember { mutableStateOf(initialAmount) }
    var paidBy by remember { mutableStateOf(initialPaidBy.ifBlank { memberNames.firstOrNull() ?: "Tôi" }) }
    var description by remember { mutableStateOf(initialDescription) }

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
            Text(
                text = if (initialAmount.isNotEmpty()) "Sửa chi tiêu" else "Thêm chi tiêu",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

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
            ) { Text(if (initialAmount.isNotEmpty()) "Cập nhật" else "Lưu chi tiêu", fontWeight = FontWeight.Bold) }

            Spacer(Modifier.height(8.dp))
        }
    }
}
