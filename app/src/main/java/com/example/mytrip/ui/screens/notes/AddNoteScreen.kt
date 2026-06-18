package com.example.mytrip.ui.screens.notes

import android.Manifest
import android.content.Context
import android.location.LocationManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mytrip.MyTripApplication
import com.example.mytrip.data.db.entities.Note
import com.example.mytrip.data.db.entities.NoteTag
import com.example.mytrip.ui.screens.trip.TripViewModel
import com.example.mytrip.util.MoneyUtils
import com.example.mytrip.ui.components.MyTripChip
import com.example.mytrip.ui.components.MyTripTextField
import com.example.mytrip.ui.components.MyTripPrimaryButton
import com.example.mytrip.ui.components.MyTripSecondaryButton
import com.example.mytrip.ui.components.GlassmorphismCard
import com.example.mytrip.ui.theme.TripThemeProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddNoteScreen(
    navController: NavController,
    tripId: Long,
    dayId: Long?,
    noteId: Long? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as MyTripApplication
    val noteVm: NoteViewModel = viewModel(factory = NoteViewModel.factory(app))
    val tripVm: TripViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>) = TripViewModel(app) as T
    })

    val trip by tripVm.trip.collectAsStateWithLifecycle()
    
    LaunchedEffect(tripId) {
        tripVm.loadTrip(tripId)
    }
    
    // Watch for saved ID
    val savedId by noteVm.savedNoteId.collectAsStateWithLifecycle()
    LaunchedEffect(savedId) {
        if (savedId != null) {
            // Kiểm tra màn hình trước: nếu không phải TripDetail (ví dụ mở từ widget),
            // navigate đến TripDetail thay vì popBackStack về Home.
            val prevRoute = navController.previousBackStackEntry?.destination?.route
            if (prevRoute != null && prevRoute.startsWith("trip_detail")) {
                navController.popBackStack()
            } else {
                navController.navigate(com.example.mytrip.navigation.Screen.TripDetail.createRoute(tripId)) {
                    popUpTo(com.example.mytrip.navigation.Screen.Home.route)
                }
            }
        }
    }

    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    TripThemeProvider(trip = trip) {
    // State
    var photoPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCamera by remember { mutableStateOf(false) } // Bắt đầu ở màn hình Form trực tiếp
    var rating by remember { mutableIntStateOf(0) }
    var selectedTag by remember { mutableStateOf(NoteTag.OTHER) }
    var costInput by remember { mutableStateOf("0") }
    var paidBy by remember { mutableStateOf("") }
    var advancedTo by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var showOptional by remember { mutableStateOf(false) }
    var gpsLat by remember { mutableStateOf<Double?>(null) }
    var gpsLng by remember { mutableStateOf<Double?>(null) }
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Launcher chọn nhiều ảnh từ thư viện máy
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val copiedPaths = uris.mapNotNull { uri ->
            copyUriToExternalFilesDir(context, uri)
        }
        if (copiedPaths.isNotEmpty()) {
            photoPaths = photoPaths + copiedPaths
        }
    }

    // Tải note nếu noteId != null
    val noteToEdit by noteVm.noteToEdit.collectAsStateWithLifecycle()
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(noteId) {
        if (noteId != null) {
            noteVm.loadNote(noteId)
        } else {
            isInitialized = true
        }
    }

    LaunchedEffect(noteToEdit) {
        noteToEdit?.let { note ->
            if (!isInitialized) {
                photoPaths = if (note.photoPaths.isNotEmpty()) note.photoPaths else if (note.photoPath != null) listOf(note.photoPath!!) else emptyList()
                rating = note.rating
                selectedTag = note.tag
                costInput = note.cost.toString()
                paidBy = note.paidBy
                advancedTo = note.advancedTo ?: ""
                name = note.name
                comment = note.comment
                gpsLat = note.gpsLat
                gpsLng = note.gpsLng
                timestamp = note.timestamp
                if (comment.isNotBlank() || note.photoPaths.size > 1 || (note.gpsLat != null)) showOptional = true
                isInitialized = true
            }
        }
    }

    // Auto-get GPS
    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            try {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                val cancellationTokenSource = CancellationTokenSource()
                
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
                    .addOnSuccessListener { loc ->
                        if (loc != null) {
                            gpsLat = loc.latitude
                            gpsLng = loc.longitude
                        } else {
                            // Fallback to LocationManager if FusedLocationProvider returns null
                            try {
                                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                                val fallbackLoc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                fallbackLoc?.let { 
                                    gpsLat = it.latitude
                                    gpsLng = it.longitude 
                                }
                            } catch (_: Exception) {}
                        }
                    }
            } catch (_: SecurityException) {}
        } else {
            locationPermission.launchPermissionRequest()
        }
    }

    val memberNames = remember(trip) {
        val names = trip?.memberNames?.trim('[', ']')
            ?.split(",")?.map { it.trim().trim('"') }?.filter { it.isNotBlank() }
            ?.toMutableList() ?: mutableListOf()
        
        val count = trip?.numPeople ?: 1
        if (names.isEmpty()) {
            if (count <= 1) {
                names.add("Tôi")
            } else {
                for (i in 1..count) {
                    names.add("Người $i")
                }
            }
        } else if (names.size < count) {
             for (i in (names.size + 1)..count) {
                 names.add("Người $i")
             }
        }
        names.toList()
    }
    LaunchedEffect(memberNames) { if (paidBy.isEmpty() && memberNames.isNotEmpty()) paidBy = memberNames[0] }

    if (showCamera && cameraPermission.status.isGranted) {
        CameraScreen(
            onPhotoCaptured = { path ->
                photoPaths = photoPaths + path
                showCamera = false
            },
            onSkip = { showCamera = false }
        )
    } else if (showCamera && !cameraPermission.status.isGranted) {
        LaunchedEffect(Unit) { cameraPermission.launchPermissionRequest() }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Rounded.CameraAlt, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                Text("Cần quyền camera", style = MaterialTheme.typography.titleMedium)
                MyTripPrimaryButton(onClick = { cameraPermission.launchPermissionRequest() }) { Text("Cấp quyền") }
                MyTripSecondaryButton(onClick = { showCamera = false }) { Text("Bỏ qua ảnh") }
            }
        }
    } else {
        // Form
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Thêm nhật ký") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                        }
                    }
                )
            },
            bottomBar = {
                Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                    val cost = MoneyUtils.inputToVnd(MoneyUtils.parseInput(costInput))
                    MyTripPrimaryButton(
                        onClick = {
                            noteVm.saveNote(Note(
                                id = noteToEdit?.id ?: 0L,
                                tripId = tripId,
                                dayId = dayId,
                                photoPath = photoPaths.firstOrNull(),
                                photoPaths = photoPaths,
                                rating = rating,
                                tag = selectedTag,
                                cost = cost,
                                paidBy = paidBy,
                                advancedTo = if (selectedTag == NoteTag.ADVANCE) advancedTo else null,
                                name = name,
                                comment = comment,
                                gpsLat = gpsLat,
                                gpsLng = gpsLng,
                                timestamp = timestamp
                            ))
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = rating > 0
                    ) {
                        Icon(Icons.Rounded.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Lưu nhật ký", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Photo layout (horizontal list if not empty, otherwise action card)
                if (photoPaths.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Hình ảnh (${photoPaths.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(photoPaths) { path ->
                                Box(Modifier.size(120.dp).clip(RoundedCornerShape(12.dp))) {
                                    AsyncImage(
                                        model = File(path),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    IconButton(
                                        onClick = { photoPaths = photoPaths.filter { it != path } },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Xóa",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            
                            // Nút thêm ảnh từ Camera trong hàng
                            item {
                                GlassmorphismCard(
                                    modifier = Modifier.size(120.dp).clickable { if (cameraPermission.status.isGranted) showCamera = true else cameraPermission.launchPermissionRequest() }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Rounded.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Chụp ảnh", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            
                            // Nút thêm ảnh từ máy trong hàng
                            item {
                                GlassmorphismCard(
                                    modifier = Modifier.size(120.dp).clickable { galleryLauncher.launch("image/*") }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Chọn ảnh", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    GlassmorphismCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MyTripSecondaryButton(
                                    onClick = { if (cameraPermission.status.isGranted) showCamera = true else cameraPermission.launchPermissionRequest() },
                                    modifier = Modifier.weight(1f).height(56.dp)
                                ) {
                                    Icon(Icons.Rounded.AddAPhoto, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Chụp ảnh")
                                }
                                MyTripSecondaryButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f).height(56.dp)
                                ) {
                                    Icon(Icons.Rounded.Image, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Chọn ảnh")
                                }
                            }
                        }
                    }
                }

                // 📝 Tên ghi chú
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tên ghi chú / địa điểm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    MyTripTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "VD: Ăn trưa hải sản, Vé cáp treo...",
                        leadingIcon = { Icon(Icons.Rounded.Place, null) },
                        singleLine = true
                    )
                }

                // 🕒 Thời gian
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Thời gian *", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val dateStr = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("vi", "VN")).format(java.util.Date(timestamp))
                        val timeStr = java.text.SimpleDateFormat("HH:mm", java.util.Locale("vi", "VN")).format(java.util.Date(timestamp))

                        // Nút chọn ngày
                        OutlinedButton(
                            onClick = {
                                val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        cal.set(year, month, dayOfMonth)
                                        timestamp = cal.timeInMillis
                                    },
                                    cal.get(java.util.Calendar.YEAR),
                                    cal.get(java.util.Calendar.MONTH),
                                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarToday, null)
                            Spacer(Modifier.width(8.dp))
                            Text(dateStr, color = MaterialTheme.colorScheme.onSurface)
                        }

                        // Nút chọn giờ
                        OutlinedButton(
                            onClick = {
                                val cal = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        cal.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
                                        cal.set(java.util.Calendar.MINUTE, minute)
                                        timestamp = cal.timeInMillis
                                    },
                                    cal.get(java.util.Calendar.HOUR_OF_DAY),
                                    cal.get(java.util.Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.AccessTime, null)
                            Spacer(Modifier.width(8.dp))
                            Text(timeStr, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }

                // ⭐ Rating
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Đánh giá *", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
                    ) {
                        (1..5).forEach { star ->
                            IconButton(onClick = { rating = star }) {
                                Icon(
                                    imageVector = if (star <= rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = null,
                                    tint = if (star <= rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                    if (rating == 0) {
                        Text(
                            text = "Bắt buộc chọn đánh giá",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }

                // 🏷️ Tag
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Loại *", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NoteTag.entries.forEach { tag ->
                            val isSelected = selectedTag == tag
                            MyTripChip(
                                text = "${tag.icon} ${tag.label}",
                                selected = isSelected,
                                onClick = { selectedTag = tag }
                            )
                        }
                    }
                }

                // 💰 Chi phí
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Chi phí *", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    MyTripTextField(
                        value = costInput,
                        onValueChange = { costInput = it.filter { c -> c.isDigit() } },
                        modifier = Modifier.fillMaxWidth(),
                        label = "Số tiền",
                        placeholder = "VD: 150 = 150.000₫",
                        suffix = "k",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    // Shortcuts
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MoneyUtils.SHORTCUTS) { amount ->
                            MyTripChip(
                                text = if (amount >= 1000) "${amount/1000}M" else "${amount}k",
                                selected = false,
                                onClick = { costInput = amount.toString() }
                            )
                        }
                    }
                    if (costInput.isNotEmpty()) {
                        Text(
                            "= ${MoneyUtils.formatVnd(MoneyUtils.inputToVnd(MoneyUtils.parseInput(costInput)))}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                // 👤 Ai trả
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Ai trả *", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        memberNames.forEach { member ->
                            val isSelected = paidBy == member
                            MyTripChip(
                                text = member,
                                selected = isSelected,
                                onClick = { paidBy = member }
                            )
                        }
                    }
                }

                // 👤 Người nhận ứng (nếu là ứng tiền)
                AnimatedVisibility(visible = selectedTag == NoteTag.ADVANCE) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Người nhận ứng *", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            memberNames.forEach { member ->
                                val isSelected = advancedTo == member
                                MyTripChip(
                                    text = member,
                                    selected = isSelected,
                                    onClick = { advancedTo = member }
                                )
                            }
                        }
                    }
                }

                // Optional fields
                MyTripSecondaryButton(
                    onClick = { showOptional = !showOptional },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (showOptional) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (showOptional) "Ẩn bớt thông tin" else "Thêm thông tin")
                }

                AnimatedVisibility(visible = showOptional, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        MyTripTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Nhận xét chi tiết",
                            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Comment, null) }
                        )
                        if (gpsLat != null && gpsLng != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.GpsFixed, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Text(String.format(Locale.US, "GPS: %.5f, %.5f", gpsLat, gpsLng),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
    }
}

@Composable
private fun CameraScreen(
    onPhotoCaptured: (String) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                previewView
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture
                    
                    val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                    
                    try {
                        provider.unbindAll()
                        camera = provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture)
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(context))
            },
            modifier = Modifier.fillMaxSize()
        )

        // Top Controls: Skip & Switch Camera
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) 
                        CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Rounded.FlipCameraIos, contentDescription = "Đổi camera", tint = Color.White)
            }

            MyTripSecondaryButton(onClick = onSkip) {
                Text("Bỏ qua ảnh", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Zoom Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val zoomOptions = listOf(0.5f, 1f, 2f)
            zoomOptions.forEach { ratio ->
                TextButton(
                    onClick = {
                        val zoomState = camera?.cameraInfo?.zoomState?.value
                        if (zoomState != null) {
                            val clampedRatio = ratio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
                            camera?.cameraControl?.setZoomRatio(clampedRatio)
                        }
                    }
                ) {
                    val label = if (ratio == 0.5f) "0.5x" else "${ratio.toInt()}x"
                    Text(label, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Shutter button
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)) {
            IconButton(
                onClick = {
                    val ic = imageCapture ?: return@IconButton
                    val dir = context.getExternalFilesDir("Pictures")
                    val file = File(dir, "NOTE_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())}.jpg")
                    val opts = ImageCapture.OutputFileOptions.Builder(file).build()
                    ic.takePicture(opts, executor, object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(results: ImageCapture.OutputFileResults) { onPhotoCaptured(file.absolutePath) }
                        override fun onError(e: ImageCaptureException) { onSkip() }
                    })
                },
                modifier = Modifier.size(80.dp)
                    .background(Color.White, CircleShape)
                    .border(4.dp, Color.Gray, CircleShape)
            ) {
                Box(Modifier.size(60.dp).background(Color.White, CircleShape))
            }
        }
    }
}

private fun copyUriToExternalFilesDir(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val dir = context.getExternalFilesDir("Pictures")
        val file = File(dir, "NOTE_${UUID.randomUUID()}.jpg")
        val outputStream = java.io.FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
