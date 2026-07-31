package com.alchitry.labs2.ui.register_interface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed class Radix(val radix: Int, val prefix: String, val name: String) {
    data object Decimal : Radix(10, "d", "Decimal")
    data object Hex : Radix(16, "h", "Hex")
    data object Binary : Radix(2, "b", "Binary")

    companion object {
        val All = listOf(Decimal, Hex, Binary)
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