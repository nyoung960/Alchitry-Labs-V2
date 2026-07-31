package com.alchitry.labs2.ui.register_interface

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.alchitry.labs2.painterResource
import com.alchitry.labs2.ui.components.ToggleButton
import kotlinx.coroutines.*
import java.awt.Cursor
import kotlin.coroutines.resume
import kotlin.time.TimeSource

data class TimedValue(
    val value: Int,
    val timestamp: TimeSource.Monotonic.ValueTimeMark = TimeSource.Monotonic.markNow()
)

class RegisterRow(
    val address: Int,
    private val requestRemoval: (RegisterRow) -> Unit
) {
    val scope = CoroutineScope(Dispatchers.Main)
    var running by mutableStateOf(false)
    var watchJob by mutableStateOf<Job?>(null)
    var valueState by mutableStateOf(
        IntTextFieldState(
            value = 0,
            signed = false,
            radix = Radix.Decimal,
            valid = true
        )
    )
    var showGraph by mutableStateOf(false)
    var collectingValues by mutableStateOf(false)
    val graphValues = mutableStateListOf<TimedValue>()

    @Composable
    fun Draw(dragHandleModifier: Modifier, connected: Boolean, onRequest: suspend (RegisterRequest) -> Unit) {
        LaunchedEffect(connected) {
            if (!connected) {
                running = false
                watchJob?.cancel()
                watchJob = null
                collectingValues = false
            }
        }

        Row(
            Modifier.height(IntrinsicSize.Max).background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = dragHandleModifier
                    .pointerHoverIcon(PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
                    .alpha(0.7f)
                    .fillMaxHeight()
                    .padding(15.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource("icons/drag_indicator.svg"),
                    "Drag",
                    Modifier.size(25.dp)
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.padding(vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.defaultMinSize(minWidth = 275.dp)
                        ) {
                            Text("Address: ")
                            Text(address.toUInt().toString())
                            Text(" 0x${address.toUInt().toHexString(HexFormat.UpperCase)}", Modifier.alpha(0.5f))
                        }

                        IntTextField(valueState, "Value", signSelector = true, readOnly = running) { valueState = it }

                        Button({
                            running = true
                            scope.launch {
                                onRequest(RegisterRequest.BasicRead(address) { value ->
                                    if (value != null)
                                        valueState = valueState.withNewValue(value)
                                    running = false
                                })
                            }
                        }, enabled = !running && connected) {
                            Text("Read")
                        }
                        Button({}, enabled = !running && connected) {
                            Text("Write")
                        }

                        ToggleButton(
                            active = watchJob != null,
                            enabled = (!running || watchJob != null) && connected,
                            tooltip = { Text("Watch Register") },
                            onClick = {
                                if (watchJob == null) {
                                    running = true
                                    watchJob = scope.launch {
                                        while (isActive) {
                                            val value: Int = suspendCancellableCoroutine { cont ->
                                                launch {
                                                    onRequest(RegisterRequest.BasicRead(address) { cont.resume(it) })
                                                }
                                            } ?: break
                                            if (collectingValues) {
                                                graphValues.add(TimedValue(value))
                                            }
                                            valueState = valueState.withNewValue(value)
                                        }
                                        watchJob = null
                                        running = false
                                    }
                                } else {
                                    watchJob?.cancel()
                                    watchJob = null
                                    running = false
                                }
                            }
                        ) {
                            Icon(
                                painterResource("icons/glasses.svg"),
                                contentDescription = "Watch",
                                modifier = Modifier.size(width = 70.dp, height = 45.dp)
                            )
                        }


                        ToggleButton(
                            active = showGraph,
                            onClick = {
                                showGraph = it
                                if (it) collectingValues = true
                            },
                            tooltip = { Text("Show Graph") }
                        ) {
                            Icon(
                                painterResource("icons/chart.svg"),
                                contentDescription = "Show Graph",
                                modifier = Modifier.size(width = 70.dp, height = 45.dp)
                            )
                        }

                        AnimatedVisibility(
                            showGraph,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(15.dp)
                            ) {
                                ToggleButton(
                                    active = collectingValues,
                                    enabled = connected && watchJob != null,
                                    tooltip = { Text(if (collectingValues) "Pause" else "Start") },
                                    onClick = { collectingValues = it }
                                ) {
                                    Crossfade(collectingValues) { collecting ->
                                        if (collecting) {
                                            Icon(
                                                painterResource("icons/pause.svg"),
                                                contentDescription = "Pause",
                                                modifier = Modifier.size(45.dp).padding(5.dp)
                                            )
                                        } else {
                                            Icon(
                                                painterResource("icons/play.svg"),
                                                contentDescription = "Start",
                                                modifier = Modifier.size(45.dp).padding(5.dp)
                                            )
                                        }
                                    }
                                }

                                ToggleButton(
                                    active = false,
                                    enabled = connected && watchJob != null,
                                    tooltip = { Text("Reset") },
                                    onClick = { graphValues.clear() }
                                ) {
                                    Icon(
                                        painterResource("icons/replay.svg"),
                                        contentDescription = "Reset",
                                        modifier = Modifier.size(45.dp).padding(5.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f).pointerInput(Unit) {

                    })
                    Box(
                        modifier = Modifier
                            .padding(end = 15.dp)
                            .size(35.dp)
                            .padding(2.dp)
                            .clip(CircleShape)
                            .clickable(
                                onClick = { requestRemoval(this@RegisterRow) },
                                role = Role.Button,
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painterResource("icons/close.svg"),
                            "Close",
                            modifier = Modifier.matchParentSize().padding(4.dp)
                        )
                    }
                }
                AnimatedVisibility(showGraph) {
                    Box(Modifier.height(250.dp)) {
                        // Graph goes here
                    }
                }
            }
        }
    }
}