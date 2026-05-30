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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import dev.shard9.tonegenerator.R
import dev.shard9.tonegenerator.audio.BleManager
import dev.shard9.tonegenerator.audio.ToneGenerator
import dev.shard9.tonegenerator.ui.components.FrequencyWheel
import dev.shard9.tonegenerator.ui.components.MeasurementGraph
import dev.shard9.tonegenerator.ui.theme.GreenX
import dev.shard9.tonegenerator.ui.theme.LFTonegenTheme
import dev.shard9.tonegenerator.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(
  toneGenerator: ToneGenerator,
  viewModel: AppViewModel,
  modifier: Modifier = Modifier,
) {
  var showPositionPicker by remember { mutableStateOf(false) }
  var showBleInfoDialog by remember { mutableStateOf(false) }
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
    ) { granted ->
      if (granted) {
        toneGenerator.start(scope, context, playbackEnabled = !viewModel.useRemoteGenerator)
        viewModel.updatePlayingState(true)
      }
    }

  val blePermissionLauncher =
    rememberLauncherForActivityResult(
      ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
      if (permissions.all { it.value }) {
        viewModel.updateUseRemoteGenerator(true)
      }
    }

  LaunchedEffect(audioManager) {
    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    while (true) {
      val systemVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
      if (!viewModel.useRemoteGenerator) {
        val percent = if (max > 0) ((systemVol.toFloat() / max) * 100).toInt() else 0
        if (percent != viewModel.volume) {
          viewModel.updateVolume(percent)
        }
      }
      delay(500)
    }
  }

  GeneratorContent(
    measuredLevel = toneGenerator.measuredLevel,
    selectedFrequency = viewModel.selectedFrequency,
    volume = viewModel.volume,
    isPlaying = viewModel.isPlaying,
    useRemoteGenerator = viewModel.useRemoteGenerator,
    showLogs = viewModel.showLogs,
    bleLogs = viewModel.bleLogs,
    graphData = viewModel.graphData,
    sessionStartTime = viewModel.sessionStartTime,
    graphDuration = viewModel.graphDuration,
    bleStatus = viewModel.bleStatus,
    channelSelection = viewModel.channelSelection,
    positionCount = viewModel.positionCount,
    positionNames = viewModel.positionNames,
    currentSessionMeasurements = viewModel.currentSessionMeasurements,
    confirmationVisible = confirmationVisible,
    confirmationText = confirmationText,
    showPositionPicker = showPositionPicker,
    showBleInfoDialog = showBleInfoDialog,
    onTogglePositionPicker = { showPositionPicker = it },
    onToggleBleInfoDialog = { showBleInfoDialog = it },
    onSaveMeasurement = { index ->
      val value = toneGenerator.measuredLevel * 10.0
      viewModel.saveMeasurement(index, value)
      confirmationText = "${viewModel.positionNames[index]}: ${String.format(Locale.US, "%.1f", value)}"
      showPositionPicker = false
    },
    onTogglePlaying = {
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
          toneGenerator.start(scope, context, playbackEnabled = !viewModel.useRemoteGenerator)
          viewModel.updatePlayingState(true)
        }
      }
    },
    onToggleUseRemote = { useRemote ->
      if (viewModel.isPlaying) {
        toneGenerator.stop()
      }
      if (useRemote) {
        val permissions =
          if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
              android.Manifest.permission.BLUETOOTH_SCAN,
              android.Manifest.permission.BLUETOOTH_CONNECT,
            )
          } else {
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION)
          }

        val missing =
          permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
          }

        if (missing.isEmpty()) {
          viewModel.updateUseRemoteGenerator(true)
        } else {
          blePermissionLauncher.launch(missing.toTypedArray())
        }
      } else {
        viewModel.updateUseRemoteGenerator(false)
      }
    },
    onToggleShowLogs = { viewModel.toggleShowLogs() },
    onFrequencyChange = {
      viewModel.updateSelectedFrequency(it)
      toneGenerator.setFrequency(it.toDouble())
    },
    onVolumeChange = { newVol ->
      viewModel.updateVolume(newVol)
      if (!viewModel.useRemoteGenerator) {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(
          AudioManager.STREAM_MUSIC,
          ((newVol / 100f) * max).toInt(),
          0,
        )
      }
    },
    onChannelChange = { index ->
      viewModel.updateChannelSelection(index)
      toneGenerator.channelSelection = index
    },
    minFreq = viewModel.minFreq,
    maxFreq = viewModel.maxFreq,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorContent(
  measuredLevel: Double,
  selectedFrequency: Float,
  volume: Int,
  isPlaying: Boolean,
  useRemoteGenerator: Boolean,
  showLogs: Boolean,
  bleLogs: List<String>,
  graphData: List<AppViewModel.DataPoint>,
  sessionStartTime: Long,
  graphDuration: Int,
  bleStatus: BleManager.Status,
  channelSelection: Int,
  positionCount: Int,
  positionNames: List<String>,
  currentSessionMeasurements: Map<Int, Double>,
  confirmationVisible: Boolean,
  confirmationText: String,
  showPositionPicker: Boolean,
  showBleInfoDialog: Boolean,
  onTogglePositionPicker: (Boolean) -> Unit,
  onToggleBleInfoDialog: (Boolean) -> Unit,
  onSaveMeasurement: (Int) -> Unit,
  onTogglePlaying: () -> Unit,
  onToggleUseRemote: (Boolean) -> Unit,
  onToggleShowLogs: () -> Unit,
  onFrequencyChange: (Float) -> Unit,
  onVolumeChange: (Int) -> Unit,
  onChannelChange: (Int) -> Unit,
  minFreq: Int,
  maxFreq: Int,
  modifier: Modifier = Modifier,
) {
  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceEvenly,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
      ) {
        Text(stringResource(R.string.mic_level), fontSize = 18.sp, color = Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
          progress = { measuredLevel.toFloat() },
          modifier =
            Modifier
              .weight(1f)
              .height(12.dp),
          color = GreenX,
          trackColor = Color.LightGray,
        )
        Spacer(modifier = Modifier.width(8.dp))
        val measuredValue = measuredLevel * 10.0
        Text(
          text = String.format(Locale.US, "%.1f", measuredValue),
          fontSize = 21.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.width(45.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
          onClick = { onTogglePositionPicker(true) },
          modifier = Modifier.size(48.dp),
          enabled = isPlaying,
        ) {
          Icon(
            imageVector = Icons.Default.Star,
            contentDescription = stringResource(R.string.save_to_position),
            tint = if (isPlaying) GreenX else Color.LightGray,
            modifier = Modifier.size(36.dp),
          )
        }
      }

      Box(
        modifier =
          Modifier
            .fillMaxWidth(0.9f)
            .height(120.dp),
      ) {
        if (showLogs && useRemoteGenerator) {
          val listState = rememberLazyListState()
          LaunchedEffect(bleLogs.size) {
            if (bleLogs.isNotEmpty()) {
              listState.animateScrollToItem(bleLogs.size - 1)
            }
          }
          LazyColumn(
            state = listState,
            modifier =
              Modifier
                .fillMaxSize()
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.05f), MaterialTheme.shapes.small),
          ) {
            items(bleLogs) { logMsg ->
              Text(
                text = logMsg,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                lineHeight = 12.sp,
                modifier = Modifier.padding(vertical = 1.dp),
              )
            }
          }
        } else {
          MeasurementGraph(
            dataPoints = graphData,
            startTime = sessionStartTime,
            durationSeconds = graphDuration,
            modifier = Modifier.fillMaxSize(),
          )
        }

        if (useRemoteGenerator) {
          Row(
            modifier =
              Modifier
                .align(Alignment.TopEnd)
                .padding(top = 1.dp, end = 0.dp)
                .zIndex(1f),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = if (showLogs) Icons.AutoMirrored.Filled.List else Icons.Default.BarChart,
              contentDescription = stringResource(R.string.toggle_view),
              modifier =
                Modifier
                  .size(32.dp)
                  .padding(top = 0.dp),
              tint = Color.Gray,
            )
            Spacer(modifier = Modifier.width(2.dp))
            Switch(
              checked = showLogs,
              onCheckedChange = { onToggleShowLogs() },
              modifier =
                Modifier
                  .padding(top = 1.dp, end = 10.dp),
              thumbContent = {
                Box(
                  modifier =
                    Modifier
                      .size(SwitchDefaults.IconSize)
                      .background(Color.Transparent),
                )
              },
            )
          }
        }
      }

      if (showPositionPicker) {
        ModalBottomSheet(onDismissRequest = { onTogglePositionPicker(false) }) {
          Column(
            modifier =
              Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
          ) {
            Text(stringResource(R.string.select_position), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            for (i in 0 until positionCount) {
              ListItem(
                headlineContent = { Text(positionNames[i]) },
                supportingContent = {
                  currentSessionMeasurements[i]?.let {
                    Text(stringResource(R.string.stored_value, String.format(Locale.US, "%.1f", it)))
                  }
                },
                modifier =
                  Modifier.clickable {
                    onSaveMeasurement(i)
                  },
              )
            }
          }
        }
      }

      // Generator Toggle
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          stringResource(R.string.phone),
          fontWeight = if (!useRemoteGenerator) FontWeight.Bold else FontWeight.Normal,
        )
        Switch(
          checked = useRemoteGenerator,
          onCheckedChange = { onToggleUseRemote(it) },
          modifier = Modifier.padding(horizontal = 8.dp),
        )
        Text(
          stringResource(R.string.remote),
          fontWeight = if (useRemoteGenerator) FontWeight.Bold else FontWeight.Normal,
        )

        if (useRemoteGenerator) {
          Spacer(modifier = Modifier.width(16.dp))
          val statusColor =
            when (bleStatus) {
              BleManager.Status.DISCONNECTED -> Color.Gray
              BleManager.Status.CONNECTING -> Color.Yellow
              BleManager.Status.CONNECTED -> Color.Blue
              BleManager.Status.SYNCED -> GreenX
              BleManager.Status.ERROR -> Color.Red
            }
          Box(
            contentAlignment = Alignment.Center,
            modifier =
              Modifier
                .size(18.dp)
                .background(statusColor, CircleShape)
                .clickable { onToggleBleInfoDialog(true) },
          ) {
            Text(
              text = "i",
              color = if (statusColor == Color.Yellow) Color.Black else Color.White,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      if (showBleInfoDialog) {
        AlertDialog(
          onDismissRequest = { onToggleBleInfoDialog(false) },
          title = { Text(stringResource(R.string.connection_status)) },
          text = {
            Column {
              StatusInfoRow(
                Color.Gray,
                stringResource(R.string.disconnected),
                stringResource(R.string.disconnected_desc),
              )
              StatusInfoRow(Color.Yellow, stringResource(R.string.connecting), stringResource(R.string.connecting_desc))
              StatusInfoRow(Color.Blue, stringResource(R.string.connected), stringResource(R.string.connected_desc))
              StatusInfoRow(GreenX, stringResource(R.string.synced), stringResource(R.string.synced_desc))
              StatusInfoRow(Color.Red, stringResource(R.string.error), stringResource(R.string.error_desc))
            }
          },
          confirmButton = {
            TextButton(onClick = { onToggleBleInfoDialog(false) }) {
              Text(stringResource(R.string.ok))
            }
          },
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 16.dp),
      ) {
        FrequencyWheel(
          value = selectedFrequency,
          volume = volume,
          onValueChange = { onFrequencyChange(it) },
          range = minFreq.toFloat()..maxFreq.toFloat(),
          size = 240.dp,
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Vertical Volume Slider
        Box(
          modifier =
            Modifier
              .height(240.dp)
              .width(36.dp),
          contentAlignment = Alignment.Center,
        ) {
          var sliderVolume by remember(volume) {
            mutableFloatStateOf(volume.toFloat())
          }

          Slider(
            value = sliderVolume,
            onValueChange = { newVol ->
              sliderVolume = newVol
              onVolumeChange(newVol.toInt())
            },
            onValueChangeFinished = {
              onVolumeChange(sliderVolume.toInt())
            },
            valueRange = 0f..100f,
            modifier =
              Modifier
                .graphicsLayer {
                  rotationZ = -90f
                  transformOrigin = TransformOrigin.Center
                }.layout { measurable, constraints ->
                  val placeable =
                    measurable.measure(
                      Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth,
                      ),
                    )
                  layout(placeable.height, placeable.width) {
                    placeable.place(
                      (placeable.height - placeable.width) / 2,
                      (placeable.width - placeable.height) / 2,
                    )
                  }
                }.width(240.dp),
          )
        }
      }

      val channels =
        listOf(
          stringResource(R.string.left),
          stringResource(R.string.both),
          stringResource(R.string.right),
        )
      SingleChoiceSegmentedButtonRow(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
      ) {
        channels.forEachIndexed { index, label ->
          SegmentedButton(
            shape = SegmentedButtonDefaults.itemShape(index = index, count = channels.size),
            onClick = { onChannelChange(index) },
            selected = channelSelection == index,
          ) {
            Text(label, fontSize = 12.sp)
          }
        }
      }

      Button(
        onClick = { onTogglePlaying() },
        modifier = Modifier.size(110.dp),
      ) {
        Text(if (isPlaying) stringResource(R.string.stop) else stringResource(R.string.play))
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

@Composable
fun StatusInfoRow(
  color: Color,
  label: String,
  description: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.padding(vertical = 4.dp),
  ) {
    Box(
      modifier =
        Modifier
          .size(12.dp)
          .background(color, CircleShape),
    )
    Spacer(modifier = Modifier.width(12.dp))
    Column {
      Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
      Text(description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
  }
}

@Preview(showBackground = true, name = "Generator Screen - Remote Mode")
@Composable
fun GeneratorScreenRemotePreview() {
  val currentTime = System.currentTimeMillis()
  LFTonegenTheme {
    GeneratorContent(
      measuredLevel = 0.5,
      selectedFrequency = 100f,
      volume = 50,
      isPlaying = true,
      useRemoteGenerator = true,
      showLogs = false,
      bleLogs = emptyList(),
      graphData =
        listOf(
          AppViewModel.DataPoint(currentTime - 2000, 10.0),
          AppViewModel.DataPoint(currentTime - 1000, 30.0),
          AppViewModel.DataPoint(currentTime, 20.0),
        ),
      sessionStartTime = currentTime - 2000,
      graphDuration = 10,
      bleStatus = BleManager.Status.SYNCED,
      channelSelection = 1,
      positionCount = 3,
      positionNames = listOf("Pos 1", "Pos 2", "Pos 3"),
      currentSessionMeasurements = emptyMap(),
      confirmationVisible = false,
      confirmationText = "",
      showPositionPicker = false,
      showBleInfoDialog = false,
      onTogglePositionPicker = {},
      onToggleBleInfoDialog = {},
      onSaveMeasurement = {},
      onTogglePlaying = {},
      onToggleUseRemote = {},
      onToggleShowLogs = {},
      onFrequencyChange = {},
      onVolumeChange = {},
      onChannelChange = {},
      minFreq = 10,
      maxFreq = 30000,
    )
  }
}

@Preview(showBackground = true, name = "Generator Screen - Log View")
@Composable
fun GeneratorScreenLogPreview() {
  val currentTime = System.currentTimeMillis()
  LFTonegenTheme {
    GeneratorContent(
      measuredLevel = 0.8,
      selectedFrequency = 1000f,
      volume = 75,
      isPlaying = true,
      useRemoteGenerator = true,
      showLogs = true,
      bleLogs = listOf("Connected to ESP32", "Frequency set to 1000Hz", "Measuring level..."),
      graphData = emptyList(),
      sessionStartTime = currentTime,
      graphDuration = 10,
      bleStatus = BleManager.Status.CONNECTED,
      channelSelection = 1,
      positionCount = 3,
      positionNames = listOf("Pos 1", "Pos 2", "Pos 3"),
      currentSessionMeasurements = emptyMap(),
      confirmationVisible = false,
      confirmationText = "",
      showPositionPicker = false,
      showBleInfoDialog = false,
      onTogglePositionPicker = {},
      onToggleBleInfoDialog = {},
      onSaveMeasurement = {},
      onTogglePlaying = {},
      onToggleUseRemote = {},
      onToggleShowLogs = {},
      onFrequencyChange = {},
      onVolumeChange = {},
      onChannelChange = {},
      minFreq = 10,
      maxFreq = 30000,
    )
  }
}
