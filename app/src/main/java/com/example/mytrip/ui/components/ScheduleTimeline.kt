package com.example.mytrip.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mytrip.data.db.entities.Activity
import com.example.mytrip.data.db.entities.ActivityStatus
import com.example.mytrip.data.db.entities.ActivityType
import com.example.mytrip.util.MoneyUtils
import org.json.JSONArray
import sh.calvin.reorderable.ReorderableColumn

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScheduleTimelineList(
    activities: List<Activity>,
    onReorder: (List<Activity>) -> Unit,
    onEdit: (Activity) -> Unit,
    onDelete: (Activity) -> Unit,
    onStatusChange: (Activity, ActivityStatus) -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep local list state during active dragging
    var dragActivities by remember(activities) { mutableStateOf(activities) }

    // ReorderableColumn handles vertical list items
    ReorderableColumn(
        list = dragActivities,
        onSettle = { from, to ->
            val updated = dragActivities.toMutableList().apply {
                add(to, removeAt(from))
            }
            dragActivities = updated
            onReorder(updated)
        },
        modifier = modifier.fillMaxWidth()
    ) { index, activity, isDragging ->
        val isLast = index == dragActivities.size - 1

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .longPressDraggableHandle() // drag handle triggers on holding the item
        ) {
            TimelineItem(
                activity = activity,
                isLast = isLast,
                isDragging = isDragging,
                onEdit = onEdit,
                onDelete = onDelete,
                onStatusChange = onStatusChange
            )
        }
    }
}

@Composable
private fun TimelineItem(
    activity: Activity,
    isLast: Boolean,
    isDragging: Boolean,
    onEdit: (Activity) -> Unit,
    onDelete: (Activity) -> Unit,
    onStatusChange: (Activity, ActivityStatus) -> Unit
) {
    val statusColor = when (activity.status) {
        ActivityStatus.DONE -> Color(0xFF2E7D32)
        ActivityStatus.SKIPPED -> Color(0xFFC62828)
        ActivityStatus.CHANGED -> Color(0xFFE65100)
        ActivityStatus.PENDING -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(horizontal = 16.dp)
    ) {
        // Timeline line + dot column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(32.dp)
                .fillMaxHeight()
        ) {
            Spacer(Modifier.height(8.dp))
            // Clickable status dot to quickly toggle/cycle status
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        val next = when (activity.status) {
                            ActivityStatus.PENDING -> ActivityStatus.DONE
                            ActivityStatus.DONE -> ActivityStatus.SKIPPED
                            ActivityStatus.SKIPPED -> ActivityStatus.CHANGED
                            ActivityStatus.CHANGED -> ActivityStatus.PENDING
                        }
                        onStatusChange(activity, next)
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(
                            Brush.verticalGradient(
                                listOf(statusColor.copy(alpha = 0.6f), Color.Transparent)
                            )
                        )
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Activity card
        ActivityTimelineCard(
            activity = activity,
            statusColor = statusColor,
            isDragging = isDragging,
            onEdit = onEdit,
            onDelete = onDelete,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 14.dp)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActivityTimelineCard(
    activity: Activity,
    statusColor: Color,
    isDragging: Boolean,
    onEdit: (Activity) -> Unit,
    onDelete: (Activity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val elevation = if (isDragging) 8.dp else 3.dp
    val containerColor = if (isDragging) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = modifier
            .border(
                width = 1.5.dp,
                color = statusColor.copy(alpha = if (isDragging) 0.8f else 0.4f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Time range & type icon & status badge & action icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(activity.activityType.icon, fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))

                    val timeText = buildString {
                        if (activity.departureTime.isNotBlank()) {
                            append(activity.departureTime)
                        }
                        if (activity.arrivalTime.isNotBlank()) {
                            if (isNotEmpty()) append(" - ")
                            append(activity.arrivalTime)
                        }
                    }
                    if (timeText.isNotEmpty()) {
                        Text(
                            text = "🕐 $timeText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { onEdit(activity) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Sửa",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { onDelete(activity) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Xóa",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp)) // reduced from 8.dp

            // Activity Name
            Text(
                text = activity.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Hotel info if available
            if (activity.activityType == ActivityType.ACCOMMODATION && activity.hotelName.isNotBlank()) {
                Spacer(Modifier.height(3.dp)) // reduced from 6.dp
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🏨 ${activity.hotelName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (activity.hotelPricePlanned > 0) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "• ${MoneyUtils.formatShort(MoneyUtils.inputToVnd(activity.hotelPricePlanned))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Distance if transit & distance > 0
            if (activity.distanceKm > 0) {
                Spacer(Modifier.height(2.dp)) // reduced from 4.dp
                Text(
                    text = "📍 ${"%.1f".format(activity.distanceKm)} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Check-in spots (List of chips)
            val spots = remember(activity.checkInSpots) { parseSpots(activity.checkInSpots) }
            if (spots.isNotEmpty()) {
                Spacer(Modifier.height(3.dp)) // reduced from 6.dp
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    spots.forEach { spot ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "📷 $spot",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // General Notes
            if (activity.notes.isNotBlank()) {
                Spacer(Modifier.height(3.dp)) // reduced from 6.dp
                Text(
                    text = activity.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Actual Notes for CHANGED status
            if (activity.status == ActivityStatus.CHANGED && activity.actualNotes.isNotBlank()) {
                Spacer(Modifier.height(3.dp)) // reduced from 6.dp
                Surface(
                    color = Color(0xFFFFF3E0),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🔀 ${activity.actualNotes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Google Maps Link
            if (activity.mapsLink.isNotBlank()) {
                Spacer(Modifier.height(3.dp)) // reduced from 6.dp
                TextButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(activity.mapsLink)))
                        } catch (_: Exception) {
                            android.widget.Toast.makeText(context, "Không thể mở liên kết bản đồ", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp)
                ) {
                    Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Xem bản đồ", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
