package com.example.mytrip.ui.screens.trip

import android.app.DatePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.navigation.NavController
import com.example.mytrip.data.db.entities.Trip
import com.example.mytrip.data.db.entities.TripType
import com.example.mytrip.navigation.Screen
import com.example.mytrip.util.DateUtils
import com.example.mytrip.ui.components.*
import com.example.mytrip.ui.theme.spacing
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditTripScreen(
    navController: NavController,
    tripId: Long?,
    viewModel: TripViewModel = viewModel()
) {
    val isEditMode = tripId != null
    val trip by viewModel.trip.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val importedTripId by viewModel.importedTripId.collectAsStateWithLifecycle()
    val importError by viewModel.importError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Import mode (only in create mode)
    var selectedImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var useFileImport by remember { mutableStateOf(false) }
    var importFileName by remember { mutableStateOf<String?>(null) }
    var importSuccess by remember { mutableStateOf(false) }

    // File picker launcher for Excel
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            importFileName = uri.lastPathSegment ?: "file.xlsx"
            selectedImportUri = uri
        }
    }

    // Navigate to TripDetail after import success
    LaunchedEffect(importedTripId) {
        val newId = importedTripId
        if (newId != null) {
            importSuccess = true
            snackbarHostState.showSnackbar("✅ Nhập lịch trình thành công!")
            navController.navigate(Screen.TripDetail.createRoute(newId)) {
                popUpTo(Screen.Home.route)
            }
        }
    }

    // Show import error
    LaunchedEffect(importError) {
        val err = importError
        if (err != null) {
            snackbarHostState.showSnackbar("❌ $err")
            viewModel.clearImportError()
        }
    }

    // ─── Form state ───────────────────────────────────────────────────────────
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TripType.CAR) }
    var startDate by remember { mutableStateOf(DateUtils.todayMillis()) }
    var endDate by remember { mutableStateOf(DateUtils.todayMillis() + 86_400_000L) }
    var numPeople by remember { mutableStateOf(1) }
    var memberNames by remember { mutableStateOf(listOf("")) }
    var useClusters by remember { mutableStateOf(false) }
    var themeColor by remember { mutableStateOf(com.example.mytrip.ui.theme.TripThemeColors.vibrantColors[0]) }
    var nameError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    // Load existing trip data in edit mode
    LaunchedEffect(tripId) {
        if (tripId != null) {
            viewModel.loadTrip(tripId)
        } else {
            viewModel.resetUiState()
        }
    }

    // Populate form when trip data arrives (edit mode)
    LaunchedEffect(trip) {
        val t = trip ?: return@LaunchedEffect
        if (isEditMode) {
            name = t.name
            description = t.description
            selectedType = t.type
            startDate = t.startDate
            endDate = t.endDate
            numPeople = t.numPeople
            useClusters = t.useClusters
            if (t.themeColor.isNotEmpty()) {
                themeColor = t.themeColor
            }
            // Parse member names from JSON
            val parsed = mutableListOf<String>()
            try {
                val arr = JSONArray(t.memberNames)
                for (i in 0 until arr.length()) parsed.add(arr.getString(i))
            } catch (_: Exception) {}
            memberNames = if (parsed.isEmpty()) List(t.numPeople) { "" } else parsed
        }
    }

    // Sync member names list size with numPeople
    LaunchedEffect(numPeople) {
        memberNames = List(numPeople) { i -> memberNames.getOrElse(i) { "" } }
    }

    val numDays = DateUtils.countDays(startDate, endDate).coerceAtLeast(1)

    // ─── Date pickers ─────────────────────────────────────────────────────────
    fun showStartDatePicker() {
        val cal = DateUtils.getCalendarFromMillis(startDate)
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val newStart = DateUtils.millisFromDateParts(y, m, d)
                startDate = newStart
                if (endDate < newStart) endDate = newStart
                dateError = false
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showEndDatePicker() {
        val cal = DateUtils.getCalendarFromMillis(endDate)
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val newEnd = DateUtils.millisFromDateParts(y, m, d)
                if (newEnd < startDate) {
                    dateError = true
                } else {
                    endDate = newEnd
                    dateError = false
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // ─── Trip type chips ──────────────────────────────────────────────────────
    val tripTypes = TripType.entries

    // ─── Save logic ───────────────────────────────────────────────────────────
    fun validateAndSave() {
        nameError = name.isBlank()
        dateError = endDate < startDate
        if (nameError || dateError) return

        coroutineScope.launch {
            isSaving = true
            try {
                val namesJson = JSONArray(memberNames).toString()
                val tripToSave = if (isEditMode && trip != null) {
                    trip!!.copy(
                        name = name.trim(),
                        description = description.trim(),
                        type = selectedType,
                        startDate = startDate,
                        endDate = endDate,
                        numPeople = numPeople,
                        memberNames = namesJson,
                        useClusters = useClusters,
                        themeColor = themeColor
                    )
                } else {
                    Trip(
                        name = name.trim(),
                        description = description.trim(),
                        type = selectedType,
                        startDate = startDate,
                        endDate = endDate,
                        numPeople = numPeople,
                        memberNames = namesJson,
                        useClusters = useClusters,
                        themeColor = themeColor
                    )
                }
                viewModel.saveTrip(tripToSave)
                navController.popBackStack()
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Lưu thất bại: ${e.message}")
            } finally {
                isSaving = false
            }
        }
    }

    // ─── UI ───────────────────────────────────────────────────────────────────
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditMode) "Chỉnh sửa chuyến đi" else "Tạo chuyến đi",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                ) {
                    MyTripPrimaryButton(
                        onClick = { validateAndSave() },
                        enabled = !isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Rounded.Check, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isEditMode) "Lưu thay đổi" else "Tạo chuyến đi",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── MODE SELECTOR (only in Create mode) ──────────────────────────
            if (!isEditMode) {
                // Top Action Buttons
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MyTripSecondaryButton(
                        onClick = { useFileImport = !useFileImport },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (useFileImport) Icons.Rounded.Remove else Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Nhập từ Excel", fontWeight = FontWeight.Bold)
                    }
                    
                    MyTripSecondaryButton(
                        onClick = { viewModel.importSampleTemplateTrip() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Lịch trình mẫu", fontWeight = FontWeight.Bold)
                    }
                }

                // ── FILE IMPORT PANEL ─────────────────────────────────────────
                AnimatedVisibility(visible = useFileImport, enter = expandVertically(), exit = shrinkVertically()) {
                    GlassmorphismCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("📋 Hướng dẫn nhập file", fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall)

                            listOf(
                                "1️⃣ Tải file mẫu Excel về máy",
                                "2️⃣ Mở file và điền thông tin chuyến đi",
                                "3️⃣ Tải file lên và nhấn Nhập"
                            ).forEach { step ->
                                Text(step, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            HorizontalDivider()

                            // Download template button
                            MyTripSecondaryButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val savedName = viewModel.downloadTemplateExcel(context)
                                        if (savedName != null) {
                                            snackbarHostState.showSnackbar("✅ Đã tải file mẫu: $savedName")
                                        } else {
                                            snackbarHostState.showSnackbar("❌ Không tải được file mẫu")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Tải file mẫu (.xlsx)", fontWeight = FontWeight.Medium)
                            }

                            // Pick file button
                            MyTripSecondaryButton(
                                onClick = { filePickerLauncher.launch("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.FolderOpen, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (importFileName != null) "📂 ${if (importFileName!!.length > 25) importFileName!!.substring(0, 25) + "..." else importFileName}"
                                           else "Tải file lên",
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Import button
                            MyTripPrimaryButton(
                                onClick = {
                                    selectedImportUri?.let { viewModel.importFromExcelUri(context, it) }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                enabled = selectedImportUri != null && uiState !is TripUiState.Loading
                            ) {
                                if (uiState is TripUiState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                                        color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Đang nhập...")
                                } else {
                                    Icon(Icons.Rounded.UploadFile, null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Nhập", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Divider between mode choices and manual form
                if (!useFileImport) HorizontalDivider()
            }

            // ── Show manual form only when not in file import mode ────────────
            if (!useFileImport || isEditMode) {

            // ── 1. Trip Name ──────────────────────────────────────────────────
            FormSection(title = "🔢 Tên chuyến đi") {
                MyTripTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "VD: Đà Nẵng - Hội An 2026",
                    isError = nameError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
            }

            // ── 2. Description ────────────────────────────────────────────────
            FormSection(title = "🗒️ Mô tả (tùy chọn)") {
                MyTripTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp),
                    placeholder = "Ghi chú ngắn về chuyến đi...",
                    maxLines = 4,
                    singleLine = false,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    )
                )
            }

            // ── 3. Trip Type ──────────────────────────────────────────────────
            FormSection(title = "🚗 Loại hình di chuyển") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tripTypes.forEach { type ->
                        val isSelected = selectedType == type
                        MyTripChip(
                            text = "${type.icon} ${type.label}",
                            selected = isSelected,
                            onClick = { selectedType = type }
                        )
                    }
                }
            }

            // ── 4. Date range ─────────────────────────────────────────────────
            FormSection(title = "📅 Ngày đi") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Start date
                    DateButton(
                        modifier = Modifier.weight(1f),
                        label = "Ngày bắt đầu",
                        dateText = DateUtils.formatDate(startDate),
                        isError = dateError,
                        onClick = { showStartDatePicker() }
                    )
                    // End date
                    DateButton(
                        modifier = Modifier.weight(1f),
                        label = "Ngày kết thúc",
                        dateText = DateUtils.formatDate(endDate),
                        isError = dateError,
                        onClick = { showEndDatePicker() }
                    )
                }
                if (dateError) {
                    Text(
                        text = "Ngày kết thúc phải sau ngày bắt đầu",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // ── 5. Number of people ───────────────────────────────────────────
            FormSection(title = "👥 Số người tham gia") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    FilledIconButton(
                        onClick = { if (numPeople > 1) numPeople-- },
                        enabled = numPeople > 1,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Remove,
                            contentDescription = "Giảm",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .defaultMinSize(minWidth = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$numPeople",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    FilledIconButton(
                        onClick = { if (numPeople < 20) numPeople++ },
                        enabled = numPeople < 20,
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Tăng",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "người",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── 6. Member names ───────────────────────────────────────────────
            AnimatedVisibility(visible = numPeople > 0) {
                FormSection(title = "👤 Tên thành viên") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        memberNames.forEachIndexed { index, memberName ->
                            MyTripTextField(
                                value = memberName,
                                onValueChange = { newVal ->
                                    memberNames = memberNames.toMutableList().also { it[index] = newVal }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = "Thành viên ${index + 1}",
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = if (index < memberNames.lastIndex) ImeAction.Next else ImeAction.Done
                                )
                            )
                        }
                    }
                }
            }

            // ── 6.5 Theme Color ───────────────────────────────────────────────
            FormSection(title = "🎨 Màu chủ đề") {
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(com.example.mytrip.ui.theme.TripThemeColors.vibrantColors) { colorHex ->
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        val isSelected = themeColor == colorHex
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { themeColor = colorHex }
                                .then(
                                    if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            // ── 7. Auto-calculated days ───────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "ℹ️", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Số ngày tự tính:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "$numDays ngày",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // ── 8. Cluster toggle (only if days > 3) ──────────────────────────
            AnimatedVisibility(
                visible = numDays > 3,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "📌", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Chia cụm địa điểm",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Gộp các ngày thành từng cụm (VD: Hà Nội, Đà Nẵng, Huế...)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp, start = 28.dp)
                            )
                        }
                        Switch(
                            checked = useClusters,
                            onCheckedChange = { useClusters = it }
                        )
                    }
                }
            }

            // Bottom spacing for FAB / button
            } // end if (!useFileImport || isEditMode)
            Spacer(Modifier.height(8.dp))
        }
    }
}


// ─── Helper composables ───────────────────────────────────────────────────────


@Composable
private fun DateButton(
    modifier: Modifier = Modifier,
    label: String,
    dateText: String,
    isError: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.outline
        ),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(6.dp))
        Column(horizontalAlignment = Alignment.Start) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
