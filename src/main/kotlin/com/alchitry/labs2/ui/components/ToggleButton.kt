package com.alchitry.labs2.ui.components

import androidx.compose.animation.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ToggleButton(
    active: Boolean,
    onClick: (Boolean) -> Unit,
    tooltip: @Composable () -> Unit,
    enabled: Boolean = true,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceColorAtElevation(100.dp),
    activeColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    AlchitryToolTip(tooltip) {
        val colorAnimation = remember { Animatable(if (active) activeColor else inactiveColor) }
        LaunchedEffect(active) {
            colorAnimation.animateTo(if (active) activeColor else inactiveColor)
        }
        Box(
            Modifier.alpha(if (enabled) 1f else 0.5f)
                .shadow(10.dp, RoundedCornerShape(10.dp))
                .background(
                    colorAnimation.value
                )
                .clickable(enabled = enabled) { onClick(!active) }) {
            content()
        }
    }
}
