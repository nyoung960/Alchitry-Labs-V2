package com.alchitry.labs2.ui.tabs.register_interface

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.alchitry.labs2.painterResource
import com.alchitry.labs2.ui.drag_and_drop.DragAndDropContext
import com.alchitry.labs2.ui.drag_and_drop.Draggable
import java.awt.Cursor

class RegisterRow(
    val address: Int,
    private val requestRemoval: (RegisterRow) -> Unit
) {

    @Composable
    context(dndContext: DragAndDropContext<RegisterRow>)
    fun Draw() {
        key(this) {
            dndContext.Draggable(this, onMoved = {}) {
                Row(
                    Modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
                        .height(IntrinsicSize.Max),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painterResource("icons/drag_indicator.svg"),
                        "Drag",
                        Modifier.pointerHoverIcon(
                            PointerIcon(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
                        ).alpha(0.7f).aspectRatio(1f).fillMaxHeight().padding(15.dp)
                    )
                    Text(address.toString())
                    Spacer(Modifier.weight(1f))
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
    }
}