package dev.shard9.tonegenerator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.shard9.tonegenerator.ui.theme.GreenX
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin

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

  // Snap State
  var isSnapDisabledForTouch by remember { mutableStateOf(false) }
  var snapStartTime by remember { mutableLongStateOf(0L) }
  var lastSnappedValue by remember { mutableFloatStateOf(-1f) }

  LaunchedEffect(value, minFreq, maxFreq) {
    val targetAngle = (ln(value / minFreq) / ln(maxFreq / minFreq) * 360f).coerceIn(0f, 360f)
    if (abs(targetAngle - angle) > 0.5f) {
      angle = targetAngle
    }
  }

  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Box(
      modifier =
        Modifier
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

                // Reset snap state for new touch
                isSnapDisabledForTouch = false
                snapStartTime = 0L
                lastSnappedValue = -1f
              },
              onDragEnd = {
                previousTouchAngle = null
                snapStartTime = 0L
              },
              onDragCancel = {
                previousTouchAngle = null
                snapStartTime = 0L
              },
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
                  val rawValue =
                    minFreq *
                      (maxFreq / minFreq)
                        .toDouble()
                        .pow(t.toDouble())
                        .toFloat()

                  var finalValue = rawValue

                  if (!isSnapDisabledForTouch) {
                    // Calculate major step based on magnitude
                    val magnitude = 10.0.pow(floor(log10(rawValue.toDouble()))).toFloat()
                    val step = if (rawValue < 100) 10f else magnitude

                    val snapTarget = (round(rawValue / step) * step)
                    val snapThreshold = step * 0.3f // +/- 20% of step

                    if (abs(rawValue - snapTarget) <= snapThreshold) {
                      // We are in a snap zone
                      finalValue = snapTarget

                      if (lastSnappedValue != snapTarget) {
                        // Just entered this specific snap target
                        snapStartTime = System.currentTimeMillis()
                        lastSnappedValue = snapTarget
                      } else {
                        // Holding on same snap target
                        val elapsed = System.currentTimeMillis() - snapStartTime
                        if (snapStartTime > 0 && elapsed > 2000) {
                          isSnapDisabledForTouch = true
                        }
                      }
                    } else {
                      // Outside snap zone
                      snapStartTime = 0L
                      lastSnappedValue = -1f
                    }
                  }

                  onValueChange(finalValue)
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

        // Draw background circle
        drawCircle(
          color = if (enabled) Color.DarkGray else Color.LightGray,
          radius = radius,
          center = center,
          style = Stroke(width = 4f),
        )

        // Draw major step markings
        val markerLength = radius / 10f
        val logMin = ln(minFreq.toDouble())
        val logMax = ln(maxFreq.toDouble())
        val logRange = logMax - logMin

        if (logRange > 0) {
          var currentBase = 10.0
          while (currentBase <= maxFreq) {
            for (i in 1..10) {
              val freq = currentBase * i
              if (freq < minFreq) continue
              if (freq > maxFreq) break

              val t = (ln(freq) - logMin) / logRange
              val markerAngleRad = (t * 360.0 - 90.0) * (PI / 180.0)

              val start =
                Offset(
                  (center.x + cos(markerAngleRad) * radius).toFloat(),
                  (center.y + sin(markerAngleRad) * radius).toFloat(),
                )
              val end =
                Offset(
                  (center.x + cos(markerAngleRad) * (radius - markerLength)).toFloat(),
                  (center.y + sin(markerAngleRad) * (radius - markerLength)).toFloat(),
                )

              drawLine(
                color = Color.Gray.copy(alpha = 0.6f),
                start = start,
                end = end,
                strokeWidth = 4f,
              )
            }
            currentBase *= 10.0
          }
        }

        // Draw active sweep arc
        drawArc(
          color = if (enabled) GreenX else Color.LightGray,
          startAngle = -90f,
          sweepAngle = angle,
          useCenter = false,
          topLeft = Offset(center.x - radius, center.y - radius),
          size = Size(radius * 2, radius * 2),
          style = Stroke(width = 12f),
        )

        val rad = (angle - 90f) * (PI / 180f).toFloat()
        val pointPos =
          Offset(
            center.x + cos(rad) * radius,
            center.y + sin(rad) * radius,
          )
        drawCircle(
          color = if (enabled) GreenX else Color.LightGray,
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
