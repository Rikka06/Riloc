package com.riloc.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Classic virtual-location joystick: a draggable thumb inside a fixed base circle.
 * Reports a normalized deflection vector (-1..1 on both axes; +x = east, -y = north)
 * while dragged and resets to zero on release, matching the LocationJoystick UX.
 */
@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    onVectorChange: (x: Float, y: Float) -> Unit,
) {
    val baseRadius = 64.dp
    val thumbRadius = 32.dp
    val density = LocalDensity.current
    val basePx = with(density) { baseRadius.toPx() }
    val thumbPx = with(density) { thumbRadius.toPx() }
    val maxTravel = basePx - thumbPx

    var offset by remember { mutableStateOf(Offset.Zero) }
    val primary = MaterialTheme.colorScheme.primary

    fun emitVector() {
        onVectorChange(offset.x / maxTravel, offset.y / maxTravel)
    }

    Box(
        modifier = modifier
            .size(baseRadius * 2)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        // base ring
        Box(
            Modifier
                .size(baseRadius * 2)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.18f)),
        )
        // center dot (visual reference)
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.6f)),
        )
        // draggable thumb
        Box(
            Modifier
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
                .size(thumbRadius * 2)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.85f))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offset = (offset + dragAmount)
                                .let { o -> Offset(o.x.coerceIn(-maxTravel, maxTravel), o.y.coerceIn(-maxTravel, maxTravel)) }
                            emitVector()
                        },
                        onDragEnd = {
                            offset = Offset.Zero
                            emitVector()
                        },
                        onDragCancel = {
                            offset = Offset.Zero
                            emitVector()
                        },
                    )
                },
        )
    }
}
