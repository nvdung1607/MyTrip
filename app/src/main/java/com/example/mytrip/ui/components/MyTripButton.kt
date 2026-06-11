package com.example.mytrip.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mytrip.ui.components.bounceClick

@Composable
fun MyTripPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.bounceClick(),
        enabled = enabled,
        shape = MaterialTheme.shapes.small, // 8dp
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer, // #D1E9E6
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer, // #546a67 (close to #425454)
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = contentPadding
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)) {
            content()
        }
    }
}

@Composable
fun MyTripSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.bounceClick(),
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        contentPadding = contentPadding
    ) {
        ProvideTextStyle(value = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)) {
            content()
        }
    }
}
