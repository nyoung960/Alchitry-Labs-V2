package com.alchitry.labs2.ui.register_interface

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

sealed class Radix(val radix: Int, val prefix: String, val name: String) {
    data object Decimal : Radix(10, "d", "Decimal")
    data object Hex : Radix(16, "h", "Hex")
    data object Binary : Radix(2, "b", "Binary")

    companion object {
        val All by lazy { listOf(Decimal, Hex, Binary) }
    }
}

@Composable
fun RadixSelector(
    radix: Radix,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onRadixChanged: (Radix) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded, modifier = modifier, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            readOnly = true,
            value = radix.name,
            onValueChange = {},
            label = { Text("Radix") },
            enabled = enabled,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded,
            onDismissRequest = { expanded = false },
            Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(1000.dp))
        ) {
            listOf(Radix.Decimal, Radix.Hex, Radix.Binary).forEach {
                DropdownMenuItem(
                    text = { Text(it.name) },
                    onClick = {
                        onRadixChanged(it)
                        expanded = false
                    })
            }
        }

    }
}

@Composable
fun RadixSliderSelector(
    radix: Radix,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onRadixChanged: (Radix) -> Unit
) {
    val options = Radix.All
    val selectedIndex = options.indexOf(radix).coerceAtLeast(0)
    val skewDp = 8.dp
    val selectionShape = object : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
            val skew = with(density) { skewDp.toPx() }
            val path = Path().apply {
                moveTo(skew / 2f, 0f)
                lineTo(size.width + skew / 2f, 0f)
                lineTo(size.width - skew / 2f, size.height)
                lineTo(-skew / 2f, size.height)
                close()
            }
            return Outline.Generic(path)
        }
    }

    var sectionWidthPx by remember { mutableIntStateOf(0) }
    val animatedOffset by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = tween(durationMillis = 200)
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant, selectionShape)
            .height(IntrinsicSize.Max)
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
                        .clip(selectionShape)
                        .clickable(enabled = enabled) { onRadixChanged(option) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.prefix,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        color = if (isSelected)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}