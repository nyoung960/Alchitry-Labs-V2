package com.alchitry.labs2.ui.register_interface

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.alchitry.labs2.Log
import com.alchitry.labs2.ui.components.AlchitryToolTip
import com.alchitry.labs2.ui.drag_and_drop.*
import com.alchitry.labs2.ui.tabs.*
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow

class RegisterInterface(
    override var parent: TabPanel
) : Tab {
    val state = SerialState()
    private val requests = MutableSharedFlow<RegisterRequest>(extraBufferCapacity = 256)
    private val rows = mutableStateListOf<RegisterRow>()

    private var addressState by mutableStateOf(
        IntTextFieldState(
            text = "0",
            value = 0,
            signed = false,
            radix = Radix.Decimal,
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
                    ) {
                        addressState = it
                    }
                    val canAddAddress = rows.none { it.address == addressState.value }
                    AlchitryToolTip(
                        tooltip = { Text("Address \"${addressState.value}\" is already in use.") },
                        enabled = !canAddAddress
                    ) {
                        Button(onClick = {
                            rows.add(RegisterRow(addressState.value) { rows.remove(it) })
                        }, enabled = canAddAddress && addressState.valid) { Text("Add Address") }
                    }
                }
                DragAndDropZone(
                    dragAnchor = DragAnchor.KeepOffset,
                    dragDirection = DragDirection.VerticalOnly
                ) {
                    DropZone(
                        minimumSize = DpSize(1.dp, 1.dp),
                        content = { HorizontalDivider() },
                        activeModifier = Modifier.fillMaxWidth(),
                        inactiveModifier = Modifier.fillMaxWidth(),
                        hoverModifier = defaultHoverModifier.then(Modifier.fillMaxWidth())
                    ) {
                        rows.add(0, it)
                    }
                    rows.forEachIndexed { index, row ->
                        key(row) {
                            Draggable(row, onMoved = { rows.remove(row) }, waitForSlop = false) { dragHandleModifier ->
                                Column {
                                    row.Draw(dragHandleModifier, state.connected) { requests.emit(it) }
                                    DropZone(
                                        minimumSize = DpSize(1.dp, 1.dp),
                                        content = { HorizontalDivider() },
                                        activeModifier = Modifier.fillMaxWidth(),
                                        inactiveModifier = Modifier.fillMaxWidth(),
                                        hoverModifier = defaultHoverModifier.then(Modifier.fillMaxWidth())
                                    ) {
                                        rows.add(rows.indexOf(row) + 1, it)
                                    }
                                }
                            }
                        }
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