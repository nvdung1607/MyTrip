package com.example.mytrip.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytrip.data.db.entities.Activity
import com.example.mytrip.data.db.entities.ActivityStatus
import com.example.mytrip.data.db.entities.ActivityType
import org.json.JSONArray
import java.util.Calendar
import java.util.Locale

data class MoneyShortcut(val label: String, val valueK: Long)

// ─── Helper: parse checkInSpots JSON ──────────────────────────────────────────
fun parseSpots(json: String): List<String> {
    if (json.isBlank()) return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { arr.getString(it) }
    } catch (_: Exception) { emptyList() }
}

fun spotsToJson(spots: List<String>): String {
    val arr = JSONArray()
    spots.forEach { arr.put(it) }
    return arr.toString()
}

// ─── Money shortcuts for hotel price ─────────────────────────────────────────

val hotelShortcuts = listOf(
    MoneyShortcut("300k", 300L),
    MoneyShortcut("500k", 500L),
    MoneyShortcut("800k", 800L),
    MoneyShortcut("1M", 1_000L),
    MoneyShortcut("1.5M", 1_500L),
    MoneyShortcut("2M", 2_000L)
)

// ─── Suggestion lists per type ────────────────────────────────────────────────
private val transitSuggestions = listOf("Đi xe máy", "Đi ô tô", "Bắt xe khách", "Đi tàu hỏa", "Bay", "Đi thuyền", "Thuê xe tuk-tuk")
private val sightseeingSuggestions = listOf("Bãi biển", "Hồ", "Núi", "Chùa", "Đền", "Phố cổ", "Công viên", "Bảo tàng", "Thác nước", "Hang động")
private val mealSuggestions = listOf("Ăn sáng", "Ăn trưa", "Ăn tối", "Quán hải sản", "Quán bún bò", "Quán phở", "Nhà hàng địa phương", "Coffee break")
private val accommodationSuggestions = listOf("Check-in khách sạn", "Check-out khách sạn", "Nhà nghỉ", "Homestay", "Resort")
private val activitySuggestions = listOf("Tắm biển", "Leo núi", "Chèo thuyền kayak", "Cắm trại", "Mua sắm", "Thăm bạn bè", "Thuê xe đạp")

fun suggestionsFor(type: ActivityType): List<String> = when (type) {
    ActivityType.TRANSIT -> transitSuggestions
    ActivityType.SIGHTSEEING -> sightseeingSuggestions
    ActivityType.MEAL -> mealSuggestions
    ActivityType.ACCOMMODATION -> accommodationSuggestions
    ActivityType.ACTIVITY -> activitySuggestions
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivityEditSheet(
    dayId: Long,
    existingActivity: Activity?,
    insertAfterIndex: Int,
    onSave: (Activity) -> Unit,
    onDismiss: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    var selectedType by rememberSaveable { mutableStateOf(existingActivity?.activityType ?: ActivityType.TRANSIT) }
    var name by rememberSaveable { mutableStateOf(existingActivity?.name ?: "") }
    var notes by rememberSaveable { mutableStateOf(existingActivity?.notes ?: "") }
    var nameError by rememberSaveable { mutableStateOf(false) }
    var departureTime by rememberSaveable { mutableStateOf(existingActivity?.departureTime ?: "") }
    var arrivalTime by rememberSaveable { mutableStateOf(existingActivity?.arrivalTime ?: "") }
    var departureTimeError by rememberSaveable { mutableStateOf(false) }
    var arrivalTimeError by rememberSaveable { mutableStateOf(false) }
    var showMoreDetails by rememberSaveable { mutableStateOf(false) }
    var distanceText by rememberSaveable {
        mutableStateOf(if ((existingActivity?.distanceKm ?: 0.0) > 0) "%.1f".format(existingActivity?.distanceKm) else "")
    }
    var mapsLink by rememberSaveable { mutableStateOf(existingActivity?.mapsLink ?: "") }
    var checkInSpots by rememberSaveable { mutableStateOf(parseSpots(existingActivity?.checkInSpots ?: "")) }
    var spotInput by rememberSaveable { mutableStateOf("") }
    var hotelName by rememberSaveable { mutableStateOf(existingActivity?.hotelName ?: "") }
    var hotelPriceText by rememberSaveable {
        mutableStateOf(if ((existingActivity?.hotelPricePlanned ?: 0L) > 0L) existingActivity!!.hotelPricePlanned.toString() else "")
    }

    val departureTimeValue = remember(departureTime) { TextFieldValue(departureTime, TextRange(departureTime.length)) }
    val arrivalTimeValue = remember(arrivalTime) { TextFieldValue(arrivalTime, TextRange(arrivalTime.length)) }

    fun isValidTime(time: String): Boolean {
        val clean = time.trim()
        if (clean.isEmpty()) return true
        return Regex("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$").matches(clean)
    }
    fun formatDigitsToTime(digits: String): String = when (digits.length) {
        0 -> ""; 1, 2 -> digits
        3 -> "${digits.substring(0, 2)}:${digits.substring(2)}"
        else -> "${digits.substring(0, 2)}:${digits.substring(2, 4)}"
    }
    fun onTimeValueChange(newVal: String): String = formatDigitsToTime(newVal.filter { it.isDigit() }.take(4))
    fun addTimeToFormatted(time: String, minutesToAdd: Int): String {
        val parts = time.split(":")
        if (parts.size != 2) return ""
        val hour = parts[0].toIntOrNull() ?: return ""
        val minute = parts[1].toIntOrNull() ?: return ""
        val totalMinutes = hour * 60 + minute + minutesToAdd
        return String.format(Locale.US, "%02d:%02d", (totalMinutes / 60) % 24, totalMinutes % 60)
    }
    fun showTimePicker(currentTime: String, onTimeSelected: (String) -> Unit) {
        val cal = Calendar.getInstance()
        android.app.TimePickerDialog(
            context,
            { _, hour, minute -> onTimeSelected(String.format(Locale.US, "%02d:%02d", hour, minute)) },
            currentTime.split(":").firstOrNull()?.toIntOrNull() ?: cal.get(Calendar.HOUR_OF_DAY),
            currentTime.split(":").lastOrNull()?.toIntOrNull() ?: cal.get(Calendar.MINUTE),
            true
        ).show()
    }

    // ── Màu accent theo loại hoạt động ───────────────────────────────────────
    val typeColor = when (selectedType) {
        ActivityType.TRANSIT      -> Color(0xFF1565C0)
        ActivityType.SIGHTSEEING  -> Color(0xFF2E7D32)
        ActivityType.MEAL         -> Color(0xFFE65100)
        ActivityType.ACCOMMODATION-> Color(0xFF6A1B9A)
        ActivityType.ACTIVITY     -> Color(0xFF00695C)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Drag handle ───────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 12.dp)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        // ── Header với màu gradient theo loại ────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(typeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(selectedType.icon, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (existingActivity == null) "Tạo hoạt động mới" else "Chỉnh sửa hoạt động",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = selectedType.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = typeColor
                    )
                }
            }
        }

        // ── Loại hoạt động — Icon card selector ───────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ActivityType.values().forEach { type ->
                val isSelected = selectedType == type
                val tColor = when (type) {
                    ActivityType.TRANSIT      -> Color(0xFF1565C0)
                    ActivityType.SIGHTSEEING  -> Color(0xFF2E7D32)
                    ActivityType.MEAL         -> Color(0xFFE65100)
                    ActivityType.ACCOMMODATION-> Color(0xFF6A1B9A)
                    ActivityType.ACTIVITY     -> Color(0xFF00695C)
                }
                Surface(
                    onClick = { selectedType = type; name = "" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) tColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, tColor) else null,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                    ) {
                        Text(type.icon, fontSize = 18.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = type.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) tColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Tên hoạt động ─────────────────────────────────────────────────────
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                "Tên hoạt động *",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                placeholder = { Text("Nhập tên ${selectedType.label.lowercase()}...") },
                isError = nameError,
                supportingText = if (nameError) { { Text("Vui lòng nhập tên", color = MaterialTheme.colorScheme.error) } } else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = typeColor,
                    focusedLabelColor = typeColor
                ),
                modifier = Modifier.fillMaxWidth()
            )

            // Gợi ý tên
            val suggestions = suggestionsFor(selectedType)
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suggestions) { s ->
                        SuggestionChip(
                            onClick = { name = s },
                            label = { Text(s, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Thời gian ─────────────────────────────────────────────────────────
        if (selectedType != ActivityType.MEAL) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    if (selectedType == ActivityType.ACCOMMODATION) "Check-in / Check-out" else "Thời gian",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Giờ đi / Check-in
                    Surface(
                        onClick = { showTimePicker(departureTime) { departureTime = it; departureTimeError = false } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (departureTimeError) MaterialTheme.colorScheme.error
                            else if (departureTime.isNotBlank()) typeColor
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        color = if (departureTime.isNotBlank()) typeColor.copy(alpha = 0.07f)
                                else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = if (departureTime.isNotBlank()) typeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (departureTime.isNotBlank()) departureTime else "--:--",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (departureTime.isNotBlank()) typeColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (selectedType == ActivityType.ACCOMMODATION) "Check-in" else "Giờ đi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Mũi tên ở giữa
                    Box(
                        modifier = Modifier.align(Alignment.CenterVertically),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Giờ đến / Check-out
                    Surface(
                        onClick = { showTimePicker(arrivalTime) { arrivalTime = it; arrivalTimeError = false } },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (arrivalTimeError) MaterialTheme.colorScheme.error
                            else if (arrivalTime.isNotBlank()) typeColor
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        color = if (arrivalTime.isNotBlank()) typeColor.copy(alpha = 0.07f)
                                else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp)
                        ) {
                            Icon(
                                Icons.Filled.AccessTime,
                                contentDescription = null,
                                tint = if (arrivalTime.isNotBlank()) typeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (arrivalTime.isNotBlank()) arrivalTime else "--:--",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (arrivalTime.isNotBlank()) typeColor else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (selectedType == ActivityType.ACCOMMODATION) "Check-out" else "Giờ đến",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Smart time offset chips
                if (isValidTime(departureTime) && departureTime.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Thêm:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("+30p" to 30, "+1h" to 60, "+2h" to 120, "+3h" to 180, "+4h" to 240)) { (label, mins) ->
                                SuggestionChip(
                                    onClick = {
                                        val suggested = addTimeToFormatted(departureTime, mins)
                                        if (suggested.isNotEmpty()) { arrivalTime = suggested; arrivalTimeError = false }
                                    },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // ── ACCOMMODATION: Tên khách sạn ──────────────────────────────────────
        if (selectedType == ActivityType.ACCOMMODATION) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text("Khách sạn", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
                OutlinedTextField(
                    value = hotelName,
                    onValueChange = { hotelName = it },
                    placeholder = { Text("Tên khách sạn / homestay...") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = typeColor, focusedLabelColor = typeColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── Divider + Nút mở thêm chi tiết ───────────────────────────────────
        HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Surface(
            onClick = { showMoreDetails = !showMoreDetails },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (showMoreDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (showMoreDetails) "Ẩn thông tin chi tiết" else "Thêm thông tin chi tiết",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // ── Chi tiết mở rộng ──────────────────────────────────────────────────
        AnimatedVisibility(visible = showMoreDetails, enter = expandVertically(), exit = shrinkVertically()) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ACCOMMODATION: Giá phòng
                if (selectedType == ActivityType.ACCOMMODATION) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Giá phòng (nghìn ₫)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = hotelPriceText,
                            onValueChange = { hotelPriceText = it },
                            placeholder = { Text("VD: 500 = 500.000 ₫") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = typeColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(hotelShortcuts) { sc ->
                                SuggestionChip(onClick = { hotelPriceText = sc.valueK.toString() }, label = { Text(sc.label, style = MaterialTheme.typography.labelSmall) })
                            }
                        }
                    }
                }

                // TRANSIT: Khoảng cách
                if (selectedType == ActivityType.TRANSIT) {
                    Text("Khoảng cách", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it },
                        placeholder = { Text("km") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = typeColor),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // SIGHTSEEING: Check-in spots
                if (selectedType == ActivityType.SIGHTSEEING) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Điểm check-in cần ghé", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (checkInSpots.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                checkInSpots.forEach { spot ->
                                    InputChip(
                                        selected = false,
                                        onClick = { checkInSpots = checkInSpots.filter { it != spot } },
                                        label = { Text(spot, style = MaterialTheme.typography.labelSmall) },
                                        trailingIcon = { Icon(Icons.Filled.Close, null, modifier = Modifier.size(14.dp)) }
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = spotInput,
                            onValueChange = { spotInput = it },
                            placeholder = { Text("Nhập tên điểm rồi nhấn Enter") },
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                val t = spotInput.trim()
                                if (t.isNotBlank() && !checkInSpots.contains(t)) checkInSpots = checkInSpots + t
                                spotInput = ""
                            }),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = typeColor),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Link Google Maps
                if (selectedType == ActivityType.TRANSIT || selectedType == ActivityType.SIGHTSEEING || selectedType == ActivityType.ACCOMMODATION) {
                    Text("Link Google Maps", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = mapsLink,
                        onValueChange = { mapsLink = it },
                        placeholder = { Text("https://maps.google.com/...") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = typeColor),
                        trailingIcon = {
                            if (mapsLink.isNotBlank()) {
                                IconButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mapsLink))) }) {
                                    Icon(Icons.Filled.OpenInNew, null, tint = typeColor)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Ghi chú
                Text("Ghi chú", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Thêm ghi chú cho hoạt động...") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(focusedBorderColor = typeColor),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))
            }
        }

        // ── Action buttons ────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Huỷ")
            }
            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    val isDepValid = isValidTime(departureTime)
                    val isArrValid = isValidTime(arrivalTime)
                    if (!isDepValid) departureTimeError = true
                    if (!isArrValid) arrivalTimeError = true
                    if (!isDepValid || !isArrValid) return@Button
                    focusManager.clearFocus()
                    onSave(Activity(
                        id = existingActivity?.id ?: 0L,
                        dayId = dayId,
                        orderIndex = existingActivity?.orderIndex ?: 0,
                        activityType = selectedType,
                        name = name.trim(),
                        departureTime = departureTime.trim(),
                        arrivalTime = arrivalTime.trim(),
                        distanceKm = distanceText.toDoubleOrNull() ?: 0.0,
                        hotelName = hotelName.trim(),
                        hotelPricePlanned = hotelPriceText.toLongOrNull() ?: 0L,
                        checkInSpots = spotsToJson(checkInSpots),
                        mapsLink = mapsLink.trim(),
                        notes = notes.trim(),
                        status = existingActivity?.status ?: ActivityStatus.PENDING,
                        actualDepartureTime = existingActivity?.actualDepartureTime ?: "",
                        actualArrivalTime = existingActivity?.actualArrivalTime ?: "",
                        actualNotes = existingActivity?.actualNotes ?: ""
                    ))
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = typeColor)
            ) {
                Text(if (existingActivity == null) "✓  Thêm" else "✓  Lưu")
            }
        }
    }
}

