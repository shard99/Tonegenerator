package dev.shard9.tonegenerator

import android.content.Context
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.shard9.tonegenerator.ui.theme.ToneGeneratorTheme
import kotlinx.coroutines.*
import java.util.Locale
import kotlin.math.*

class AppViewModel : ViewModel() {
    var slotCount by mutableIntStateOf(3)
    var slotNames by mutableStateOf(List(6) { "Slot ${it + 1}" })
    var currentSessionMeasurements = mutableStateMapOf<Int, Double>()
    var history = mutableStateListOf<String>()

    fun saveMeasurement(slotIndex: Int, value: Double) {
        currentSessionMeasurements[slotIndex] = value
    }

    fun finishSession(frequency: Double, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
        if (currentSessionMeasurements.isEmpty()) return

        val result = StringBuilder("${frequency.roundToInt()}")
        for (i in 0 until slotCount) {
            currentSessionMeasurements[i]?.let { value ->
                val formattedValue = String.format(Locale.US, "%.1f", value)
                result.append(";${slotNames[i]};$formattedValue")
            }
        }

        val resultString = result.toString()
        history.add(0, resultString)
        if (history.size > 3) {
            history.removeAt(3)
        }

        clipboardManager.setText(AnnotatedString(resultString))
        currentSessionMeasurements.clear()
    }
}

class MainActivity : ComponentActivity() {
    private var toneGenerator: ToneGenerator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        toneGenerator = ToneGenerator()
        setContent {
            ToneGeneratorTheme {
                val viewModel: AppViewModel = viewModel()
                toneGenerator?.let { generator ->
                    AppNavigation(toneGenerator = generator, viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
    }
}

class ToneGenerator {
    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var audioRecord: AudioRecord? = null

    @Volatile
    private var isPlaying = false

    @Volatile
    private var isStopping = false

    private var frequency = 100.0
    var overtones = 0
    var channelSelection = 1 // 0: Left, 1: Both, 2: Right
    private var job: Job? = null
    private var recordJob: Job? = null

    var measuredLevel by mutableDoubleStateOf(0.0)

    fun start(scope: CoroutineScope, context: Context) {
        // If we are already playing and not trying to stop, do nothing.
        if (isPlaying && !isStopping) return

        // If we were in the middle of a fade-out, cancel the stop and continue.
        if (isPlaying && isStopping) {
            isStopping = false
            return
        }

        // Otherwise, start a fresh job.
        isPlaying = true
        isStopping = false

        startPlayback(scope)
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startRecording(scope)
        }
    }

    private fun startPlayback(scope: CoroutineScope) {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_STEREO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )

        audioTrack = AudioTrack.Builder()
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .build(),
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        job = scope.launch(Dispatchers.Default) {
            var phase = 0.0

            val numSamples = (bufferSize / 4) and -2
            val buffer = FloatArray(numSamples)

            val fadeInSamples = sampleRate // 1 second
            val fadeOutLimit = (sampleRate * 0.1).toInt() // 20ms

            var samplesProcessed = 0
            var fadeOutSamples = 0

            try {
                while (isPlaying) {
                    val currentFreq = frequency
                    val currentOvertones = overtones
                    val currentChannel = channelSelection

                    val activeHarmonics = (0..currentOvertones).filter { k ->
                        (currentFreq * 2.0.pow(k)) <= 20000.0
                    }
                    val gain = if (activeHarmonics.isEmpty()) 0f else 1.0f / activeHarmonics.size

                    for (i in 0 until buffer.size step 2) {
                        val fadeInVolume = if (samplesProcessed < fadeInSamples) {
                            samplesProcessed.toFloat() / fadeInSamples
                        } else {
                            1.0f
                        }

                        val fadeOutVolume = if (isStopping) {
                            val v = 1.0f - (fadeOutSamples.toFloat() / fadeOutLimit)
                            fadeOutSamples++
                            v.coerceIn(0f, 1f)
                        } else {
                            fadeOutSamples = 0
                            1.0f
                        }

                        if (isStopping && fadeOutVolume <= 0f) {
                            isPlaying = false
                        }

                        val combinedVolume = fadeInVolume * fadeOutVolume

                        var sampleValue = 0.0
                        for (k in activeHarmonics) {
                            sampleValue += sin(phase * (2.0.pow(k)))
                        }

                        val finalSample = (sampleValue * gain * combinedVolume).toFloat()

                        when (currentChannel) {
                            0 -> {
                                buffer[i] = finalSample
                                buffer[i + 1] = 0f
                            }

                            2 -> {
                                buffer[i] = 0f
                                buffer[i + 1] = finalSample
                            }

                            else -> {
                                buffer[i] = finalSample
                                buffer[i + 1] = finalSample
                            }
                        }

                        phase += 2.0 * PI * currentFreq / sampleRate
                        if (phase > 2.0 * PI) phase -= 2.0 * PI
                        samplesProcessed++
                    }
                    audioTrack?.write(buffer, 0, buffer.size, AudioTrack.WRITE_BLOCKING)
                }
            } finally {
                audioTrack?.stop()
                audioTrack?.flush()
                audioTrack?.release()
                audioTrack = null
                isPlaying = false
                isStopping = false
            }
        }
    }

    private fun startRecording(scope: CoroutineScope) {
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        )

        try {
            audioRecord = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(bufferSize)
                .build()

            audioRecord?.startRecording()

            recordJob = scope.launch(Dispatchers.Default) {
                val buffer = FloatArray(bufferSize / 4)
                while (isPlaying) {
                    val read = audioRecord?.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING) ?: 0
                    if (read > 0) {
                        val magSq = calculateGoertzel(buffer, read, frequency)
                        val mag = sqrt(magSq)
                        // Heuristic normalization for UI
                        val normalized = (mag / (read / 2.0)).coerceIn(0.0, 1.0)
                        measuredLevel = normalized
                    }
                }
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
                measuredLevel = 0.0
            }
        } catch (_: SecurityException) {
            // Permission might have been revoked
            measuredLevel = 0.0
        }
    }

    private fun calculateGoertzel(samples: FloatArray, len: Int, targetFreq: Double): Double {
        val omega = 2.0 * PI * targetFreq / sampleRate
        val cosine = cos(omega)
        val coeff = 2.0 * cosine

        var q0: Double
        var q1 = 0.0
        var q2 = 0.0

        for (i in 0 until len) {
            q0 = coeff * q1 - q2 + samples[i]
            q2 = q1
            q1 = q0
        }
        return q1 * q1 + q2 * q2 - q1 * q2 * coeff
    }

    fun stop() {
        if (isPlaying) {
            isStopping = true
        }
    }

    fun release() {
        isPlaying = false
        isStopping = false
        job?.cancel()
        recordJob?.cancel()
        audioTrack?.release()
        audioTrack = null
        audioRecord?.release()
        audioRecord = null
    }

    fun setFrequency(freq: Double) {
        frequency = freq
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(toneGenerator: ToneGenerator, viewModel: AppViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerItems = listOf(
        DrawerItem("Generator", "generator", Icons.Default.Menu),
        DrawerItem("Results", "results", Icons.Default.History),
        DrawerItem("Settings", "settings", Icons.Default.Settings)
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(12.dp))
                drawerItems.forEach { item ->
                    NavigationDrawerItem(
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                if (item.route == "generator") {
                                    popUpTo("generator") { inclusive = true }
                                }
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Tone generator") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "generator",
                modifier = Modifier.padding(padding)
            ) {
                composable("generator") {
                    ToneGeneratorScreen(toneGenerator = toneGenerator, viewModel = viewModel)
                }
                composable("results") {
                    ResultsScreen(viewModel = viewModel)
                }
                composable("settings") {
                    SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}

data class DrawerItem(val label: String, val route: String, val icon: ImageVector)

@Composable
fun ResultsScreen(viewModel: AppViewModel) {
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Results History", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.history.isEmpty()) {
            Text("No results yet", color = Color.Gray)
        } else {
            viewModel.history.forEach { line ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = line,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Start
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val allText = viewModel.history.joinToString("\n")
                    clipboardManager.setText(AnnotatedString(allText))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Copy All to Clipboard")
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Text("Number of Slots: ${viewModel.slotCount}")
        Slider(
            value = viewModel.slotCount.toFloat(),
            onValueChange = { viewModel.slotCount = it.toInt() },
            valueRange = 1f..6f,
            steps = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text("Slot Names:", fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            for (i in 0 until viewModel.slotCount) {
                OutlinedTextField(
                    value = viewModel.slotNames[i],
                    onValueChange = { newName ->
                        val newList = viewModel.slotNames.toMutableList()
                        newList[i] = newName
                        viewModel.slotNames = newList
                    },
                    label = { Text("Slot ${i + 1} Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToneGeneratorScreen(toneGenerator: ToneGenerator, viewModel: AppViewModel, modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    var frequency by remember { mutableFloatStateOf(100f) }
    var overtoneCount by remember { mutableFloatStateOf(0f) }
    var channelIndex by remember { mutableIntStateOf(1) } // Default: Both
    var showSlotPicker by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val clipboardManager = LocalClipboardManager.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        if (isGranted) {
            toneGenerator.start(scope, context)
            isPlaying = true
        } else {
            toneGenerator.start(scope, context)
            isPlaying = true
        }
    }

    if (showSlotPicker) {
        ModalBottomSheet(onDismissRequest = { showSlotPicker = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Select Slot to Store Value", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                for (i in 0 until viewModel.slotCount) {
                    ListItem(
                        headlineContent = { Text(viewModel.slotNames[i]) },
                        modifier = Modifier.clickable {
                            viewModel.saveMeasurement(i, toneGenerator.measuredLevel * 10.0)
                            showSlotPicker = false
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
            if (maxVolume > 0) (currentVolumeInt.toFloat() / maxVolume * 100).roundToInt() else 0
        }
    }

    LaunchedEffect(audioManager) {
        while (true) {
            currentVolumeInt = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            delay(500)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        Text("Tone Generator", fontSize = 18.sp, color = Color.Gray)

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FrequencyWheel(
                value = frequency,
                volume = volumePercentage,
                onValueChange = {
                    frequency = it
                    toneGenerator.setFrequency(it.toDouble())
                },
                range = 20f..500f,
                size = 240.dp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Measured Level Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Text("Mic Level:", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { toneGenerator.measuredLevel.toFloat() },
                    modifier = Modifier.weight(1f).height(4.dp),
                    color = Color.Blue,
                    trackColor = Color.LightGray,
                )
                Spacer(modifier = Modifier.width(8.dp))
                val measuredValue = toneGenerator.measuredLevel * 10.0
                Text(
                    text = String.format(Locale.US, "%.1f", measuredValue),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(30.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showSlotPicker = true },
                    modifier = Modifier.size(32.dp),
                    enabled = isPlaying
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Save to slot",
                        tint = if (isPlaying) Color.Blue else Color.LightGray
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

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Overtones: ${overtoneCount.toInt()}")
            Slider(
                value = overtoneCount,
                onValueChange = {
                    overtoneCount = it
                    toneGenerator.overtones = it.toInt()
                },
                valueRange = 0f..5f,
                steps = 4,
                modifier = Modifier.width(280.dp),
            )
            Text("Adds multiples of frequency (2x, 4x, etc.)", fontSize = 12.sp, color = Color.Gray)
        }

        Button(
            onClick = {
                if (isPlaying) {
                    toneGenerator.stop()
                    viewModel.finishSession(frequency.toDouble(), clipboardManager)
                    overtoneCount = 0f
                    toneGenerator.overtones = 0
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
}

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
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
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
