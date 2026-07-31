package com.alchitry.labs2.ui.register_interface

import androidx.compose.animation.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.alchitry.labs2.painterResource
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.awt.Cursor
import kotlin.coroutines.resume

class RegisterRow(
    val address: Int,
    private val requestRemoval: (RegisterRow) -> Unit
) {

    @Composable
    fun Draw(dragHandleModifier: Modifier, connected: Boolean, onRequest: suspend (RegisterRequest) -> Unit) {
        val scope = rememberCoroutineScope()
        var running by remember { mutableStateOf(false) }
        var watchJob by remember { mutableStateOf<Job?>(null) }
        var valueState by remember {
            mutableStateOf(
                IntTextFieldState(
                    value = 0,
                    signed = false,
                    radix = Radix.Decimal,
                    valid = true
                )
            )
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

                val inactiveColor = MaterialTheme.colorScheme.surfaceColorAtElevation(100.dp)
                val activeColor = MaterialTheme.colorScheme.primary
                val watchColorAnimation = remember { Animatable(inactiveColor) }

                val watchEnabled = (!running || watchJob != null) && connected
                Icon(
                    painterResource("icons/glasses.svg"),
                    contentDescription = "Watch",
                    modifier = Modifier
                        .alpha(if (watchEnabled) 1f else 0.5f)
                        .shadow(10.dp, RoundedCornerShape(10.dp))
                        .background(
                            watchColorAnimation.value
                        )
                        .clickable(enabled = watchEnabled) {
                            if (watchJob == null) {
                                running = true
                                watchJob = scope.launch {
                                    var value: Int? = suspendCancellableCoroutine { cont ->
                                        launch {
                                            onRequest(RegisterRequest.BasicRead(address) { cont.resume(it) })
                                        }
                                    }
                                    while (value != null) {
                                        valueState = valueState.withNewValue(value)
                                        value = suspendCancellableCoroutine { cont ->
                                            launch {
                                                onRequest(RegisterRequest.BasicRead(address) { cont.resume(it) })
                                            }
                                        }
                                    }
                                    watchJob = null
                                    running = false
                                }
                            } else {
                                watchJob?.cancel()
                                watchJob = null
                                running = false
                            }
                            scope.launch {
                                watchColorAnimation.animateTo(if (watchJob != null) activeColor else inactiveColor)
                            }
                        }
                        .size(width = 70.dp, height = 45.dp)

                )

                Icon(
                    painterResource("icons/chart.svg"),
                    contentDescription = "Show graph",
                    modifier = Modifier
                        .alpha(if (watchJob != null) 1f else 0.5f)
                        .shadow(10.dp, RoundedCornerShape(10.dp))
                        .background(
                            watchColorAnimation.value
                        )
                        .clickable(enabled = watchJob != null) {

                        }
                        .size(45.dp)
                )
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
    }
}