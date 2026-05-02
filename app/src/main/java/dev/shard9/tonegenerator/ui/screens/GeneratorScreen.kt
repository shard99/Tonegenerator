package dev.shard9.tonegenerator.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.shard9.tonegenerator.audio.ToneGenerator
import dev.shard9.tonegenerator.ui.components.FrequencyWheel
import dev.shard9.tonegenerator.viewmodel.AppViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(toneGenerator: ToneGenerator, viewModel: AppViewModel, modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    var frequency by remember { mutableFloatStateOf(100f) }
    var channelIndex by remember { mutableIntStateOf(1) }
    var showPositionPicker by remember { mutableStateOf(false) }
    var confirmationText by remember { mutableStateOf("") }
    var confirmationVisible by remember { mutableStateOf(false) }

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
    val clipboardManager = LocalClipboardManager.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { _ ->
        toneGenerator.start(scope, context)
        isPlaying = true
    }

    if (showPositionPicker) {
        ModalBottomSheet(onDismissRequest = { showPositionPicker = false }) {
            Column(
                modifier = Modifier
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
                        modifier = Modifier.clickable {
                            val value = toneGenerator.measuredLevel * 10.0
                            viewModel.saveMeasurement(i, value)
                            confirmationText = "${viewModel.positionNames[i]}: ${String.format(Locale.US, "%.1f", value)}"
                            showPositionPicker = false
                        }
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
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FrequencyWheel(
                    value = frequency,
                    volume = volumePercentage,
                    onValueChange = {
                        frequency = it
                        toneGenerator.setFrequency(it.toDouble())
                    },
                    range = viewModel.minFreq..viewModel.maxFreq,
                    size = 240.dp,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                ) {
                    Text("Mic Level:", fontSize = 18.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { toneGenerator.measuredLevel.toFloat() },
                        modifier = Modifier.weight(1f).height(6.dp),
                        color = Color.Blue,
                        trackColor = Color.LightGray,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val measuredValue = toneGenerator.measuredLevel * 10.0
                    Text(
                        text = String.format(Locale.US, "%.1f", measuredValue),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(45.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showPositionPicker = true },
                        modifier = Modifier.size(48.dp),
                        enabled = isPlaying
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Save to position",
                            tint = if (isPlaying) Color.Blue else Color.LightGray,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            val channels = listOf("Left", "Both", "Right")
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
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
                    if (isPlaying) {
                        toneGenerator.stop()
                        viewModel.finishSession(frequency.toDouble(), clipboardManager)
                        isPlaying = false
                    } else {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        } else {
                            toneGenerator.start(scope, context)
                            isPlaying = true
                        }
                    }
                },
                modifier = Modifier.size(110.dp),
            ) {
                Text(if (isPlaying) "STOP" else "PLAY")
            }
        }

        AnimatedVisibility(
            visible = confirmationVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = confirmationText,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
