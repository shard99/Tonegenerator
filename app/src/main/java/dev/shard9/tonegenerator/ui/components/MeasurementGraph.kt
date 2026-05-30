package dev.shard9.tonegenerator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shard9.tonegenerator.ui.theme.GreenX
import dev.shard9.tonegenerator.ui.theme.LFTonegenTheme
import dev.shard9.tonegenerator.viewmodel.AppViewModel
import java.util.Locale

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
  val displayEndTime =
    if (currentTime - startTime < windowDuration) {
      startTime + windowDuration
    } else {
      currentTime
    }
  val displayStartTime = displayEndTime - windowDuration

  Box(modifier = modifier) {
    Canvas(
      modifier =
        Modifier
          .fillMaxSize()
          .background(Color.Gray.copy(alpha = 0.05f), MaterialTheme.shapes.small)
          .border(1.dp, Color.LightGray.copy(alpha = 0.5f), MaterialTheme.shapes.small),
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

    // Y-Axis Labels (Inside, Transparent)
    Text(
      text = String.format(Locale.US, "%.1f", yMax),
      fontSize = 10.sp,
      color = Color.Black.copy(alpha = 0.5f),
      modifier =
        Modifier
          .align(Alignment.TopStart)
          .padding(start = 6.dp, top = 4.dp),
    )

    Text(
      text = "0",
      fontSize = 10.sp,
      color = Color.Black.copy(alpha = 0.5f),
      modifier =
        Modifier
          .align(Alignment.BottomStart)
          .padding(start = 6.dp, bottom = 4.dp),
    )

    // X-Axis Label (End)
    Text(
      text = "${durationSeconds}s",
      fontSize = 10.sp,
      color = Color.Black.copy(alpha = 0.5f),
      modifier =
        Modifier
          .align(Alignment.BottomEnd)
          .padding(end = 6.dp, bottom = 4.dp),
    )
  }
}

@Preview(showBackground = true)
@Composable
fun MeasurementGraphPreview() {
  val currentTime = System.currentTimeMillis()
  val dataPoints =
    listOf(
      AppViewModel.DataPoint(currentTime - 5000, 10.0),
      AppViewModel.DataPoint(currentTime - 4000, 25.0),
      AppViewModel.DataPoint(currentTime - 3000, 15.0),
      AppViewModel.DataPoint(currentTime - 2000, 40.0),
      AppViewModel.DataPoint(currentTime - 1000, 30.0),
      AppViewModel.DataPoint(currentTime, 50.0),
    )
  LFTonegenTheme {
    MeasurementGraph(
      dataPoints = dataPoints,
      startTime = currentTime - 5000,
      durationSeconds = 10,
      modifier =
        Modifier
          .fillMaxWidth()
          .height(200.dp)
          .padding(16.dp),
    )
  }
}
