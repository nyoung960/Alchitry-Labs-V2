package com.alchitry.labs2.ui.tabs.register_interface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.onClick
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alchitry.labs2.Log
import com.alchitry.labs2.ui.components.AlchitryToolTip
import com.alchitry.labs2.ui.drag_and_drop.DragAnchor
import com.alchitry.labs2.ui.drag_and_drop.DragAndDropZone
import com.alchitry.labs2.ui.drag_and_drop.DragDirection
import com.alchitry.labs2.ui.tabs.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow

data class IntTextFieldState(
    val value: Int,
    val signed: Boolean,
    val isHex: Boolean,
    val valid: Boolean,
) {
    val radix = if (isHex) 16 else 10
    val valueString: String = formatValue(value, false)

    fun formatValue(value: Int, includePrefix: Boolean = true) = buildString {
        if (isHex && includePrefix) append("0x")
        if (signed)
            append(value.toString(radix).uppercase())
        else
            append(value.toUInt().toString(radix).uppercase())
    }
}

@Composable
fun IntTextField(
    state: IntTextFieldState,
    label: String,
    modifier: Modifier = Modifier.Companion,
    signSelector: Boolean = true,
    enabled: Boolean = true,
    onChange: (IntTextFieldState) -> Unit
) {
    var text by remember(state.valueString) { mutableStateOf(state.valueString) }
    Row(modifier.width(IntrinsicSize.Max), verticalAlignment = Alignment.CenterVertically) {
        TextField(
            value = text,
            onValueChange = {
                text = it.uppercase()
                val newValue = if (state.signed) {
                    it.toIntOrNull(state.radix)
                } else {
                    it.toUIntOrNull(state.radix)?.toInt()
                }
                when (newValue) {
                    null -> onChange(state.copy(valid = false))
                    else -> onChange(state.copy(value = newValue, valid = true))
                }
            },
            prefix = { if (state.isHex) Text("0x") },
            label = { Text(label) },
            enabled = enabled,
            isError = text != state.valueString,
            modifier = Modifier.weight(1f)
        )
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 35.dp) {
            Column(Modifier.padding(start = 20.dp).width(IntrinsicSize.Max)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Decimal:", Modifier.weight(1f).onClick { onChange(state.copy(isHex = false)) })
                    RadioButton(
                        selected = !state.isHex,
                        onClick = { onChange(state.copy(isHex = false)) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hex:", Modifier.weight(1f).onClick { onChange(state.copy(isHex = true)) })
                    RadioButton(
                        selected = state.isHex,
                        onClick = { onChange(state.copy(isHex = true)) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (signSelector) {
                        Text("Signed:")
                        Switch(checked = state.signed, onCheckedChange = { onChange(state.copy(signed = it)) })
                    }
                }
            }
        }
    }
}

class RegisterInterface(
    override var parent: TabPanel
) : Tab {
    val state = SerialState()
    private val requests = MutableSharedFlow<RegisterRequest>(extraBufferCapacity = 256)
    private val rows = mutableStateListOf<RegisterRow>()

    private var addressState by mutableStateOf(
        IntTextFieldState(
            value = 0,
            signed = false,
            isHex = false,
            valid = true
        )
    )

    private var valueState by mutableStateOf(
        IntTextFieldState(
            value = 0,
            signed = false,
            isHex = false,
            valid = true
        )
    )


    @Composable
    override fun label() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PulsingLink(state)
            Text("Register Interface")
        }
    }

    @Composable
    override fun content() {
        Surface {
            Box(Modifier.fillMaxSize())
            Column {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SerialTerminalToolbar(state) { device ->
                        device.setTimeouts(100, 100)
                        requests.collect { request ->
                            try {
                                request.process(device)
                            } catch (e: IllegalStateException) {
                                val log = when (request) {
                                    is RegisterRequest.BasicRead -> "Read failed: ${e.message}"
                                    is RegisterRequest.BasicWrite -> "Write failed: ${e.message}"
                                    is RegisterRequest.BulkRead -> TODO()
                                    is RegisterRequest.BulkWrite -> TODO()
                                }
                                Log.error(log)
                            }
                        }
                    }

                    IntTextField(
                        addressState,
                        "Address",
                        signSelector = false
                    ) { addressState = it }
                    val canAddAddress = rows.none { it.address == addressState.value }
                    AlchitryToolTip(
                        tooltip = { Text("Address \"${addressState.value}\" is already in use.") },
                        enabled = !canAddAddress
                    ) {
                        Button(onClick = {
                            rows.add(RegisterRow(addressState.value) { rows.remove(it) })
                        }, enabled = canAddAddress) { Text("Add Address") }
                    }
                }
                DragAndDropZone<RegisterRow>(
                    dragAnchor = DragAnchor.KeepOffset,
                    dragDirection = DragDirection.VerticalOnly
                ) {
                    HorizontalDivider()
                    rows.forEach {
                        it.Draw()
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    override fun onClose(save: Boolean): Boolean {
        state.scope.cancel("Tab closed.")
        return true
    }
}