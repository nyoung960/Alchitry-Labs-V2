package com.alchitry.labs2.ui.register_interface

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class IntTextFieldState(
    val value: Int,
    val signed: Boolean,
    val radix: Radix,
    val text: String = formatValue(value, signed, radix, false),
    val valid: Boolean = true,
) {
    val valueString: String = formatValue(value, signed, radix, false)

    fun withNewValue(value: Int): IntTextFieldState =
        copy(value = value, text = formatValue(value, signed, radix, false))


    companion object {
        fun formatValue(value: Int, signed: Boolean, radix: Radix, includePrefix: Boolean = true) = buildString {
            if (includePrefix) {
                append(radix.prefix)
            }
            if (signed)
                append(value.toString(radix.radix).uppercase())
            else
                append(value.toUInt().toString(radix.radix).uppercase())
        }
    }
}

@Composable
fun IntTextField(
    state: IntTextFieldState,
    label: String,
    modifier: Modifier = Modifier.Companion,
    signSelector: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    onChange: (IntTextFieldState) -> Unit
) {
    fun setNewValues(text: String = state.text, signed: Boolean = state.signed, radix: Radix = state.radix) {
        val newValue = if (signed) {
            text.toIntOrNull(radix.radix)
        } else {
            text.toUIntOrNull(radix.radix)?.toInt()
        }
        when (newValue) {
            null ->
                onChange(state.copy(valid = false, text = text, signed = signed, radix = radix))

            else ->
                onChange(state.copy(value = newValue, valid = true, text = text, signed = signed, radix = radix))
        }
    }

    Row(
        modifier.width(IntrinsicSize.Max),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 35.dp) {

            RadixSliderSelector(state.radix, modifier = Modifier.width(125.dp)) {
                val text = if (state.valid) {
                    IntTextFieldState.formatValue(state.value, state.signed, it, false)
                } else state.text
                setNewValues(radix = it, text = text)
            }

            if (signSelector) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Signed: ")
                    Switch(checked = state.signed, onCheckedChange = {
                        setNewValues(signed = it)
                    })
                }
            }
        }
        TextField(
            value = state.text,
            onValueChange = {
                setNewValues(text = it.uppercase())
            },
            prefix = { if (state.radix !is Radix.Decimal) Text(state.radix.prefix) },
            label = { Text(label) },
            enabled = enabled,
            isError = state.text != state.valueString,
            readOnly = readOnly,
            modifier = Modifier.weight(1f),
            singleLine = true
        )
    }
}
