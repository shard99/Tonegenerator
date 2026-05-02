package dev.shard9.tonegenerator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
fun FrequencyWheel(
    value: Float,
    volume: Int,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    size: Dp,
    enabled: Boolean = true,
) {
    val minFreq = range.start
    val maxFreq = range.endInclusive

    var angle by remember {
        mutableFloatStateOf(
            (ln(value / minFreq) / ln(maxFreq / minFreq) * 360f).coerceIn(0f, 360f),
        )
    }

    LaunchedEffect(value, minFreq, maxFreq) {
        val targetAngle = (ln(value / minFreq) / ln(maxFreq / minFreq) * 360f).coerceIn(0f, 360f)
        if (abs(targetAngle - angle) > 0.5f) {
            angle = targetAngle
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .pointerInput(enabled, range) {
                    if (!enabled) return@pointerInput
                    val center =
                        Offset(this.size.width.toFloat() / 2, this.size.height.toFloat() / 2)
                    var previousTouchAngle: Float? = null

                    detectDragGestures(
                        onDragStart = { offset ->
                            val touchVector = offset - center
                            previousTouchAngle =
                                atan2(touchVector.y, touchVector.x) * (180f / PI.toFloat())
                        },
                        onDragEnd = { previousTouchAngle = null },
                        onDragCancel = { previousTouchAngle = null },
                        onDrag = { change, _ ->
                            change.consume()
                            val touchVector = change.position - center
                            val currentTouchAngle =
                                atan2(touchVector.y, touchVector.x) * (180f / PI.toFloat())

                            previousTouchAngle?.let { prev ->
                                var delta = currentTouchAngle - prev
                                if (delta > 180f) delta -= 360f
                                if (delta < -180f) delta += 360f

                                angle = (angle + delta).coerceIn(0f, 360f)

                                val t = angle / 360f
                                val newValue =
                                    minFreq * (maxFreq / minFreq).toDouble().pow(t.toDouble())
                                        .toFloat()
                                onValueChange(newValue)
                            }
                            previousTouchAngle = currentTouchAngle
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.toPx() / 2, size.toPx() / 2)
                val radius = size.toPx() / 2 - 10f

                drawCircle(
                    color = if (enabled) Color.DarkGray else Color.LightGray,
                    radius = radius,
                    center = center,
                    style = Stroke(width = 4f),
                )

                drawArc(
                    color = if (enabled) Color.Blue else Color.LightGray,
                    startAngle = -90f,
                    sweepAngle = angle,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = 12f),
                )

                val rad = (angle - 90f) * (PI / 180f).toFloat()
                val pointPos = Offset(
                    center.x + cos(rad) * radius,
                    center.y + sin(rad) * radius,
                )
                drawCircle(
                    color = if (enabled) Color.Blue else Color.LightGray,
                    radius = 16f,
                    center = pointPos,
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${value.roundToInt()} Hz",
                    fontSize = (size.value / 6).sp,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "Vol: $volume%",
                    fontSize = (size.value / 15).sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
