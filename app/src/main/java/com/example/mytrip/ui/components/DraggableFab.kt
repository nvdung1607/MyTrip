package com.example.mytrip.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Freely draggable FloatingActionButton.
 * Default position: bottom-right corner.
 * Position persists across recompositions via rememberSaveable.
 */
@Composable
fun DraggableFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary
) {
    val fabSizeDp = 56.dp
    val marginDp = 16.dp

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val fabSizePx = with(density) { fabSizeDp.toPx() }
        val marginPx = with(density) { marginDp.toPx() }
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        // Initial position: bottom-right
        var offsetX by rememberSaveable { mutableFloatStateOf(maxWidthPx - fabSizePx - marginPx) }
        var offsetY by rememberSaveable { mutableFloatStateOf(maxHeightPx - fabSizePx - marginPx) }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            FloatingActionButton(
                onClick = onClick,
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(fabSizeDp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x)
                                .coerceIn(0f, maxWidthPx - fabSizePx)
                            offsetY = (offsetY + dragAmount.y)
                                .coerceIn(0f, maxHeightPx - fabSizePx)
                        }
                    },
                containerColor = containerColor,
                contentColor = contentColor,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Thêm"
                )
            }
        }
    }
}
