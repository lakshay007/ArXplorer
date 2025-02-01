package com.lakshay.arxplorer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import com.lakshay.arxplorer.ui.theme.LocalAppColors

@Composable
fun ThemeToggleButton(
    isDarkTheme: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    var targetRotation by remember { mutableStateOf(0f) }
    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = tween(
            durationMillis = 500,
            easing = FastOutSlowInEasing
        )
    )

    LaunchedEffect(isDarkTheme) {
        targetRotation += 180f
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardBackground.copy(alpha = 0.9f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                onToggle()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
            contentDescription = if (isDarkTheme) "Switch to light theme" else "Switch to dark theme",
            tint = colors.primary,
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation)
        )
    }
} 