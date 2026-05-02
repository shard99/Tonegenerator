package dev.shard9.tonegenerator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.shard9.tonegenerator.ui.theme.GreenX
import dev.shard9.tonegenerator.viewmodel.AppViewModel

@Composable
fun MeasurementGraph(
    dataPoints: List<AppViewModel.DataPoint>,
    startTime: Long,
    durationSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val currentTime = System.currentTimeMillis()
    val maxVal = dataPoints.maxOfOrNull { it.value } ?: 0.0
    val yMax = if (maxVal > 0) maxVal / 0.9 else 1.0

    // Window duration from settings
    val windowDuration = durationSeconds * 1000L

    // Determine time range to show
    val displayEndTime = if (currentTime - startTime < windowDuration) {
        startTime + windowDuration
    } else {
        currentTime
    }
    val displayStartTime = displayEndTime - windowDuration

    Canvas(
        modifier = modifier
            .background(Color.Gray.copy(alpha = 0.05f))
            .border(1.dp, Color.LightGray.copy(alpha = 0.5f)),
    ) {
        if (dataPoints.size < 2) return@Canvas

        val path = Path()
        var started = false

        dataPoints.forEach { point ->
            if (point.timestamp < displayStartTime) return@forEach

            // X coordinate: normalized time in window
            val xFraction = (point.timestamp - displayStartTime).toFloat() / windowDuration
            val xPos = xFraction * size.width

            // Y coordinate: inverted because (0,0) is top-left
            val yFraction = (point.value / yMax).toFloat()
            val yPos = size.height - (yFraction * size.height)

            if (!started) {
                path.moveTo(xPos, yPos)
                started = true
            } else {
                path.lineTo(xPos, yPos)
            }
        }

        drawPath(
            path = path,
            color = GreenX,
            style = Stroke(width = 2.dp.toPx()),
        )
    }
}
