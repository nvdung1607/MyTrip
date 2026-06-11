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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.*
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
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_COARSE_LOCATION)

    TripThemeProvider(trip = trip) {
    // State
    var photoPaths by remember { mutableStateOf<List<String>>(emptyList()) }
    var showCamera by remember { mutableStateOf(false) } // Bắt đầu ở màn hình Form trực tiếp
    var rating by remember { mutableIntStateOf(0) }
    var selectedTag by remember { mutableStateOf(NoteTag.OTHER) }
    var costInput by remember { mutableStateOf("0") }
    var paidBy by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var showOptional by remember { mutableStateOf(false) }
    var gpsLat by remember { mutableStateOf<Double?>(null) }
    var gpsLng by remember { mutableStateOf<Double?>(null) }

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
                name = note.name
                comment = note.comment
                gpsLat = note.gpsLat
                gpsLng = note.gpsLng
                if (comment.isNotBlank() || note.photoPaths.size > 1 || (note.gpsLat != null)) showOptional = true
                isInitialized = true
            }
        }
    }

    // Auto-get GPS
    LaunchedEffect(Unit) {
        if (locationPermission.status.isGranted) {
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                loc?.let { gpsLat = it.latitude; gpsLng = it.longitude }
            } catch (_: Exception) {}
        } else {
            locationPermission.launchPermissionRequest()
        }
    }

    val memberNames = remember(trip) {
        trip?.memberNames?.trim('[', ']')
            ?.split(",")?.map { it.trim().trim('"') }?.filter { it.isNotBlank() }
            ?: listOf("Tôi")
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
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
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
                                name = name,
                                comment = comment,
                                gpsLat = gpsLat,
                                gpsLng = gpsLng,
                                timestamp = noteToEdit?.timestamp ?: System.currentTimeMillis()
                            ))
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = rating > 0
                    ) {
                        Icon(Icons.Default.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Lưu ghi chú", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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
                                            imageVector = Icons.Default.Close,
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
                                        Icon(Icons.Default.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary)
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
                                        Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary)
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
                                    Icon(Icons.Default.AddAPhoto, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Chụp ảnh")
                                }
                                MyTripSecondaryButton(
                                    onClick = { galleryLauncher.launch("image/*") },
                                    modifier = Modifier.weight(1f).height(56.dp)
                                ) {
                                    Icon(Icons.Default.Image, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Chọn ảnh")
                                }
                            }
                        }
                    }
                }

                // 📝 Tên ghi chú
                GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("📝 Tên ghi chú / địa điểm", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        MyTripTextField(
                            value = name,
                            onValueChange = { name = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "VD: Ăn trưa hải sản, Vé cáp treo...",
                            leadingIcon = { Icon(Icons.Default.Place, null) },
                            singleLine = true
                        )
                    }
                }

                // ⭐ Rating
                GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("⭐ Đánh giá *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            (1..5).forEach { star ->
                                IconButton(onClick = { rating = star }) {
                                    Icon(
                                        imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                        contentDescription = null,
                                        tint = if (star <= rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                        if (rating == 0) {
                            Text(
                                text = "Bắt buộc chọn đánh giá",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                // 🏷️ Tag
                GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🏷️ Loại *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                }

                // 💰 Chi phí
                GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("💰 Chi phí *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 👤 Ai trả
                GlassmorphismCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("👤 Ai trả *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
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
                }

                // Optional fields
                MyTripSecondaryButton(
                    onClick = { showOptional = !showOptional },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(if (showOptional) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
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
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Comment, null) }
                        )
                        if (gpsLat != null && gpsLng != null) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.GpsFixed, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                    } catch (_: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Skip button top-right
        MyTripSecondaryButton(
            onClick = onSkip,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Text("Bỏ qua ảnh", color = Color.White, fontWeight = FontWeight.Bold)
        }

        // Shutter button
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)) {
            IconButton(
                onClick = {
                    val ic = imageCapture ?: return@IconButton
                    val dir = context.getExternalFilesDir("Pictures")
                    val file = File(dir, "NOTE_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")
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
