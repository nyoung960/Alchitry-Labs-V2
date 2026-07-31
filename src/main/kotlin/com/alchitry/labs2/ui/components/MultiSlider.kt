package com.alchitry.labs2.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@Composable
fun <T> MultiSlider(
    options: List<T>,
    selected: T,
    labeler: @Composable (T) -> Unit = { Text(it.toString()) },
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onChanged: (T) -> Unit
) {
    val selectedIndex = options.indexOf(selected).coerceAtLeast(0)
    val skewDp = 8.dp

    val leftSkewOffset by animateDpAsState(
        targetValue = if (selectedIndex == 0) -skewDp / 2 else 0.dp,
        animationSpec = tween(200)
    )

    val rightSkewOffset by animateDpAsState(
        targetValue = if (selectedIndex == options.size - 1) skewDp / 2 else 0.dp,
        animationSpec = tween(200)
    )

    val baseSkewShape = SkewShape(0.dp, 0.dp, skewDp)
    val selectionShape = SkewShape(leftSkewOffset, rightSkewOffset, skewDp)

    var sectionWidthPx by remember { mutableIntStateOf(0) }
    val animatedOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 200)
    )

    Box(
        modifier = modifier
            .height(IntrinsicSize.Max)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Sliding background indicator
        if (sectionWidthPx > 0) {
            Box(
                modifier = Modifier
                    .offset { IntOffset((animatedOffset * sectionWidthPx).roundToInt(), 0) }
                    .width(with(LocalDensity.current) { sectionWidthPx.toDp() })
                    .fillMaxHeight()
                    .clip(selectionShape)
                    .background(MaterialTheme.colorScheme.primary, selectionShape)
            )
        }

        // Sections
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .onSizeChanged { sectionWidthPx = it.width }
                        .zIndex(if (isSelected) 1f else 0f)
                        .clip(
                            when (index) {
                                0 -> SkewShape(-skewDp / 2, 0.dp, skewDp)
                                options.size - 1 -> SkewShape(0.dp, skewDp / 2, skewDp)
                                else -> baseSkewShape
                            }
                        )
                        .clickable(
                            enabled = enabled,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(
                                bounded = false,
                                radius = LocalDensity.current.run { sectionWidthPx.toDp() })
                        ) { onChanged(option) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CompositionLocalProvider(
                        LocalContentColor provides if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        LocalTextStyle provides MaterialTheme.typography.labelMedium
                    ) {
                        labeler(option)
                    }
                }
            }
        }
    }
}

private class SkewShape(
    private val leftOffset: Dp = 0.dp,
    private val rightOffset: Dp = 0.dp,
    private val skewDp: Dp = 8.dp
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val skew = with(density) { skewDp.toPx() }
        val leftOffsetPx = with(density) { leftOffset.toPx() }
        val rightOffsetPx = with(density) { rightOffset.toPx() }
        val path = Path().apply {
            // Top-Left
            moveTo((skew / 2f) + leftOffsetPx, 0f)
            // Top-Right
            lineTo(size.width + (skew / 2f) + rightOffsetPx, 0f)
            // Bottom-Right
            lineTo(size.width - (skew / 2f) + rightOffsetPx, size.height)
            // Bottom-Left
            lineTo(-(skew / 2f) + leftOffsetPx, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}