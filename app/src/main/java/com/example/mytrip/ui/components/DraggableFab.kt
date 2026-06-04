package com.example.mytrip.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Uniformly styled, draggable FloatingActionButton.
 * Automatically snaps to the nearest vertical edge (left or right) on drag release.
 * Persistent coordinates on screen orientation or active screens.
 */
@Composable
fun DraggableFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
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

        val scope = rememberCoroutineScope()

        // Persistent coordinates across screen updates
        var savedX by rememberSaveable { mutableFloatStateOf(-1f) }
        var savedY by rememberSaveable { mutableFloatStateOf(-1f) }

        val animX = remember { Animatable(0f) }
        val animY = remember { Animatable(0f) }

        // Adjust constraints dynamically
        LaunchedEffect(maxWidthPx, maxHeightPx) {
            val defaultX = maxWidthPx - fabSizePx - marginPx
            val defaultY = maxHeightPx - fabSizePx - marginPx

            val targetX = if (savedX < 0f) defaultX else savedX.coerceIn(0f, maxWidthPx - fabSizePx)
            val targetY = if (savedY < 0f) defaultY else savedY.coerceIn(0f, maxHeightPx - fabSizePx)

            animX.snapTo(targetX)
            animY.snapTo(targetY)
            savedX = targetX
            savedY = targetY
        }

        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = onClick,
                modifier = Modifier
                    .offset { IntOffset(animX.value.roundToInt(), animY.value.roundToInt()) }
                    .size(fabSizeDp)
                    .pointerInput(maxWidthPx, maxHeightPx, fabSizePx, marginPx) {
                        detectDragGestures(
                            onDragEnd = {
                                scope.launch {
                                    val leftBound = marginPx
                                    val rightBound = maxWidthPx - fabSizePx - marginPx
                                    val targetX = if (animX.value < (maxWidthPx - fabSizePx) / 2) leftBound else rightBound
                                    animX.animateTo(targetX)
                                    savedX = targetX
                                }
                            },
                            onDragCancel = {
                                scope.launch {
                                    val leftBound = marginPx
                                    val rightBound = maxWidthPx - fabSizePx - marginPx
                                    val targetX = if (animX.value < (maxWidthPx - fabSizePx) / 2) leftBound else rightBound
                                    animX.animateTo(targetX)
                                    savedX = targetX
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                scope.launch {
                                    val newX = (animX.value + dragAmount.x).coerceIn(0f, maxWidthPx - fabSizePx)
                                    val newY = (animY.value + dragAmount.y).coerceIn(0f, maxHeightPx - fabSizePx)
                                    animX.snapTo(newX)
                                    animY.snapTo(newY)
                                    savedX = newX
                                    savedY = newY
                                }
                            }
                        )
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
