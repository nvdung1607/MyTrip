package com.example.mytrip.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun GlassmorphismCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // Note: True blur requires RenderEffect which is API 31+.
    // For standard cards, we use the specified elevated surface properties.
    Surface(
        modifier = modifier.shadow(
            elevation = 20.dp,
            shape = MaterialTheme.shapes.large,
            ambientColor = Color(0xFF425454).copy(alpha = 0.05f),
            spotColor = Color(0xFF425454).copy(alpha = 0.05f)
        ),
        shape = MaterialTheme.shapes.large,
        color = Color.White, // Pure white containers
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Box {
            content()
        }
    }
}

@Composable
fun BlurOverlay(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    // A placeholder for blur overlay components like nav bars
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.85f),
    ) {
        Box {
            content()
        }
    }
}
