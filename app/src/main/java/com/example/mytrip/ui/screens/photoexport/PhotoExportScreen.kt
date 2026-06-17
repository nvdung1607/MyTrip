package com.example.mytrip.ui.screens.photoexport

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.mytrip.MyTripApplication
import com.example.mytrip.ui.components.MyTripPrimaryButton
import com.example.mytrip.ui.theme.TripThemeProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoExportScreen(navController: NavController, tripId: Long) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as MyTripApplication
    val vm: PhotoExportViewModel = viewModel(factory = PhotoExportViewModel.factory(app))

    LaunchedEffect(tripId) { vm.loadData(tripId) }

    val trip by vm.trip.collectAsStateWithLifecycle()
    val photoGroups by vm.photoGroups.collectAsStateWithLifecycle()
    val selectedPhotos by vm.selectedPhotos.collectAsStateWithLifecycle()
    val includeWatermark by vm.includeWatermark.collectAsStateWithLifecycle()
    val isExporting by vm.isExporting.collectAsStateWithLifecycle()

    TripThemeProvider(trip = trip) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Tải ảnh (${selectedPhotos.size})") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null)
                        }
                    }
                )
            },
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MyTripPrimaryButton(
                            onClick = {
                                vm.exportPhotos(ctx) { count ->
                                    android.widget.Toast.makeText(ctx, "Đã lưu $count ảnh vào thư viện", android.widget.Toast.LENGTH_LONG).show()
                                    navController.popBackStack()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = !isExporting && selectedPhotos.isNotEmpty()
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Đang tải xuống...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Rounded.SaveAlt, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Lưu ${selectedPhotos.size} ảnh về máy", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                // Header Options
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Chèn thông tin chuyến đi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Thêm khung watermark vào ảnh tải về", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = includeWatermark,
                                onCheckedChange = { vm.toggleWatermark(it) }
                            )
                        }
                    }
                }

                if (photoGroups.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Chuyến đi này chưa có hình ảnh nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Groups
                items(photoGroups) { group ->
                    val allSelected = group.photos.all { selectedPhotos.contains(it) }
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Group header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { vm.toggleGroup(group) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (allSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (allSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Ngày ${group.day.dayNumber}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${group.note.tag.icon} ${group.note.name.ifBlank { group.note.comment.take(20) }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Photo grid (using FlowRow or simple Row if few)
                        // Using a simple Row for up to 3 photos, or just vertical list
                        // Let's use a standard horizontal scroll for photos in a group
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(group.photos) { photoPath ->
                                val isSelected = selectedPhotos.contains(photoPath)
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { vm.togglePhoto(photoPath) }
                                ) {
                                    AsyncImage(
                                        model = File(photoPath),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(if (isSelected) Color.Black.copy(alpha = 0.2f) else Color.Transparent)
                                    )
                                    
                                    Icon(
                                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
