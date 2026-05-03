package dev.shard9.tonegenerator.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.shard9.tonegenerator.audio.ToneGenerator
import dev.shard9.tonegenerator.ui.components.FrequencyWheel
import dev.shard9.tonegenerator.ui.components.MeasurementGraph
import dev.shard9.tonegenerator.ui.theme.GreenX
import dev.shard9.tonegenerator.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
  toneGenerator: ToneGenerator,
  viewModel: AppViewModel,
  modifier: Modifier = Modifier,
) {
  var channelIndex by remember { mutableIntStateOf(1) }
  var showPositionPicker by remember { mutableStateOf(false) }
  var confirmationText by remember { mutableStateOf("") }
  var confirmationVisible by remember { mutableStateOf(false) }

  LaunchedEffect(viewModel.selectedFrequency) {
    toneGenerator.setFrequency(viewModel.selectedFrequency.toDouble())
  }

  LaunchedEffect(viewModel.isPlaying) {
    if (viewModel.isPlaying) {
      while (viewModel.isPlaying) {
        viewModel.addGraphPoint(toneGenerator.measuredLevel * 10.0)
        delay(33) // ~30 FPS
      }
    }
  }

  LaunchedEffect(confirmationText) {
    if (confirmationText.isNotEmpty()) {
      confirmationVisible = true
      delay(1000)
      confirmationVisible = false
      delay(500)
      confirmationText = ""
    }
  }

  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
  val clipboard = LocalClipboard.current

  val permissionLauncher =
    rememberLauncherForActivityResult(
      ActivityResultContracts.RequestPermission(),
    ) { _ ->
      toneGenerator.start(scope, context)
      viewModel.updatePlayingState(true)
    }

  if (showPositionPicker) {
    ModalBottomSheet(onDismissRequest = { showPositionPicker = false }) {
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
      ) {
        Text("Select Position to Store Value", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        for (i in 0 until viewModel.positionCount) {
          ListItem(
            headlineContent = { Text(viewModel.positionNames[i]) },
            supportingContent = {
              viewModel.currentSessionMeasurements[i]?.let {
                Text("Stored: ${String.format(Locale.US, "%.1f", it)}")
              }
            },
            modifier =
              Modifier.clickable {
                val value = toneGenerator.measuredLevel * 10.0
                viewModel.saveMeasurement(i, value)
                confirmationText =
                  "${viewModel.positionNames[i]}: ${String.format(Locale.US, "%.1f", value)}"
                showPositionPicker = false
              },
          )
        }
      }
    }
  }

  var currentVolumeInt by remember {
    mutableIntStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC))
  }

  val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
  val volumePercentage by remember {
    derivedStateOf {
      if (maxVolume > 0) ((currentVolumeInt.toFloat() / maxVolume) * 100).roundToInt() else 0
    }
  }

  LaunchedEffect(audioManager) {
    while (true) {
      currentVolumeInt = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
      delay(500)
    }
  }

  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceEvenly,
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FrequencyWheel(
          value = viewModel.selectedFrequency,
          volume = volumePercentage,
          onValueChange = {
            viewModel.updateSelectedFrequency(it)
            toneGenerator.setFrequency(it.toDouble())
          },
          range = viewModel.minFreq.toFloat()..viewModel.maxFreq.toFloat(),
          size = 240.dp,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = 24.dp),
        ) {
          Text("Mic Level:", fontSize = 18.sp, color = Color.Gray)
          Spacer(modifier = Modifier.width(8.dp))
          LinearProgressIndicator(
            progress = { toneGenerator.measuredLevel.toFloat() },
            modifier =
              Modifier
                .weight(1f)
                .height(12.dp),
            color = GreenX,
            trackColor = Color.LightGray,
          )
          Spacer(modifier = Modifier.width(8.dp))
          val measuredValue = toneGenerator.measuredLevel * 10.0
          Text(
            text = String.format(Locale.US, "%.1f", measuredValue),
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(45.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          IconButton(
            onClick = { showPositionPicker = true },
            modifier = Modifier.size(48.dp),
            enabled = viewModel.isPlaying,
          ) {
            Icon(
              imageVector = Icons.Default.Star,
              contentDescription = "Save to position",
              tint = if (viewModel.isPlaying) GreenX else Color.LightGray,
              modifier = Modifier.size(36.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        MeasurementGraph(
          dataPoints = viewModel.graphData,
          startTime = viewModel.sessionStartTime,
          durationSeconds = viewModel.graphDuration,
          modifier =
            Modifier
              .fillMaxWidth(0.8f)
              .height(100.dp),
        )
      }

      val channels = listOf("Left", "Both", "Right")
      SingleChoiceSegmentedButtonRow(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
      ) {
        channels.forEachIndexed { index, label ->
          SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = index, count = channels.size),
            onClick = {
              channelIndex = index
              toneGenerator.channelSelection = index
            },
            selected = channelIndex == index,
          ) {
            Text(label, fontSize = 12.sp)
          }
        }
      }

      Button(
        onClick = {
          if (viewModel.isPlaying) {
            toneGenerator.stop()
            viewModel.finishSession(viewModel.selectedFrequency.toDouble()) { result ->
              scope.launch {
                val csvData = "${viewModel.getCSVHeader()}\n$result"
                clipboard.setClipEntry(ClipData.newPlainText("Tone Results", csvData).toClipEntry())
              }
            }
            viewModel.updatePlayingState(false)
          } else {
            if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO,
              ) != PackageManager.PERMISSION_GRANTED
            ) {
              permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            } else {
              toneGenerator.setFrequency(viewModel.selectedFrequency.toDouble())
              toneGenerator.start(scope, context)
              viewModel.updatePlayingState(true)
            }
          }
        },
        modifier = Modifier.size(110.dp),
      ) {
        Text(if (viewModel.isPlaying) "STOP" else "PLAY")
      }
    }

    AnimatedVisibility(
      visible = confirmationVisible,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.align(Alignment.Center),
    ) {
      Card(
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
        shape = MaterialTheme.shapes.medium,
      ) {
        Text(
          text = confirmationText,
          color = Color.White,
          modifier = Modifier.padding(16.dp),
          fontWeight = FontWeight.Bold,
        )
      }
    }
  }
}
