package io.github.miuzarte.scrcpyforandroid.gamepad

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun VirtualJoystick(
    def: VirtualJoystickDef,
    onAxis: (JoystickAxis, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val sizeDp = def.sizeDp.dp
    val knobSizeDp = def.sizeDp * 0.45f
    val scope = rememberCoroutineScope()

    val animX = remember { Animatable(0f) }
    val animY = remember { Animatable(0f) }

    fun emit(xPx: Float, yPx: Float, maxR: Float) {
        val nx = (xPx / maxR).coerceIn(-1f, 1f)
        val ny = (yPx / maxR).coerceIn(-1f, 1f)
        onAxis(def.axisX, ((nx + 1f) / 2f * 65535f).toInt())
        onAxis(def.axisY, ((ny + 1f) / 2f * 65535f).toInt())
    }

    fun reset() {
        scope.launch {
            launch { animX.animateTo(0f, spring(0.6f, 800f)) }
            launch { animY.animateTo(0f, spring(0.6f, 800f)) }
        }
        onAxis(def.axisX, 32768)
        onAxis(def.axisY, 32768)
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .pointerInput(def.id) {
                detectDragGestures(
                    onDragStart = { down ->
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val maxR = size.width / 2f - with(density) { knobSizeDp.dp.toPx() } / 2f
                        val dx = down.x - cx
                        val dy = down.y - cy
                        val len = sqrt(dx * dx + dy * dy)
                        val (ox, oy) = if (len > maxR) dx / len * maxR to dy / len * maxR else dx to dy
                        scope.launch { animX.snapTo(ox) }
                        scope.launch { animY.snapTo(oy) }
                        emit(ox, oy, maxR)
                    },
                    onDragEnd = { reset() },
                    onDragCancel = { reset() },
                ) { change, _ ->
                    change.consume()
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val maxR = size.width / 2f - with(density) { knobSizeDp.dp.toPx() } / 2f
                    val dx = change.position.x - cx
                    val dy = change.position.y - cy
                    val len = sqrt(dx * dx + dy * dy)
                    val (ox, oy) = if (len > maxR) dx / len * maxR to dy / len * maxR else dx to dy
                    scope.launch { animX.snapTo(ox) }
                    scope.launch { animY.snapTo(oy) }
                    emit(ox, oy, maxR)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(animX.value.toInt(), animY.value.toInt()) }
                .size(knobSizeDp.dp)
                .clip(CircleShape)
                .background(Color(0xCCFFFFFF)),
        )
    }
}

