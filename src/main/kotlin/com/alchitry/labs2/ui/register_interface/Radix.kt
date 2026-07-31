package com.alchitry.labs2.ui.register_interface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alchitry.labs2.ui.components.MultiSlider

sealed class Radix(val radix: Int, val prefix: String, val name: String) {
    data object Decimal : Radix(10, "d", "Decimal")
    data object Hex : Radix(16, "h", "Hex")
    data object Binary : Radix(2, "b", "Binary")

    companion object {
        val All by lazy { listOf(Decimal, Hex, Binary) }
    }
}

@Composable
fun RadixSliderSelector(
    radix: Radix,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onRadixChanged: (Radix) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Radix", style = MaterialTheme.typography.labelMedium)
        MultiSlider(Radix.All, radix, { Text(it.prefix) }, modifier, enabled, onRadixChanged)
    }
}