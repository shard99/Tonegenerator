package dev.shard9.tonegenerator.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shard9.tonegenerator.AppLanguage
import dev.shard9.tonegenerator.R
import dev.shard9.tonegenerator.ThemeMode
import dev.shard9.tonegenerator.ui.theme.LFTonegenTheme
import dev.shard9.tonegenerator.viewmodel.AppViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: AppViewModel) {
  SettingsContent(
    themeMode = viewModel.themeMode,
    language = viewModel.language,
    allowDualChannel = viewModel.allowDualChannel,
    minFreq = viewModel.minFreq,
    maxFreq = viewModel.maxFreq,
    graphDuration = viewModel.graphDuration,
    graphSmoothing = viewModel.graphSmoothing,
    maxRemoteVolume = viewModel.maxRemoteVolume,
    positionCount = viewModel.positionCount,
    positionNames = viewModel.positionNames,
    onUpdateTheme = { viewModel.updateTheme(it) },
    onUpdateLanguage = { viewModel.updateLanguage(it) },
    onUpdateAllowDualChannel = { viewModel.updateAllowDualChannel(it) },
    onUpdateFreqRange = { min, max -> viewModel.updateFreqRange(min, max) },
    onUpdateGraphDuration = { viewModel.updateGraphDuration(it) },
    onUpdateGraphSmoothing = { viewModel.updateGraphSmoothing(it) },
    onUpdateMaxRemoteVolume = { viewModel.updateMaxRemoteVolume(it) },
    onUpdatePositionCount = { viewModel.updatePositionCount(it) },
    onUpdatePositionName = { index, name -> viewModel.updatePositionName(index, name) },
    onResetToDefaults = { viewModel.resetToDefaults() },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
  themeMode: ThemeMode,
  language: AppLanguage,
  allowDualChannel: Boolean,
  minFreq: Int,
  maxFreq: Int,
  graphDuration: Int,
  graphSmoothing: Int,
  maxRemoteVolume: Int,
  positionCount: Int,
  positionNames: List<String>,
  onUpdateTheme: (ThemeMode) -> Unit,
  onUpdateLanguage: (AppLanguage) -> Unit,
  onUpdateAllowDualChannel: (Boolean) -> Unit,
  onUpdateFreqRange: (Int, Int) -> Unit,
  onUpdateGraphDuration: (Int) -> Unit,
  onUpdateGraphSmoothing: (Int) -> Unit,
  onUpdateMaxRemoteVolume: (Int) -> Unit,
  onUpdatePositionCount: (Int) -> Unit,
  onUpdatePositionName: (Int, String) -> Unit,
  onResetToDefaults: () -> Unit,
) {
  val focusManager = LocalFocusManager.current

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .padding(16.dp)
        .padding(bottom = 32.dp)
        .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(stringResource(R.string.settings), fontSize = 24.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))

    Text(stringResource(R.string.theme_mode), fontWeight = FontWeight.SemiBold)
    val themeModes = listOf(ThemeMode.LIGHT, ThemeMode.AUTO, ThemeMode.DARK)
    val themeLabels =
      listOf(
        stringResource(R.string.theme_light),
        stringResource(R.string.theme_auto),
        stringResource(R.string.theme_dark),
      )
    SingleChoiceSegmentedButtonRow(
      modifier = Modifier.fillMaxWidth(),
    ) {
      themeModes.forEachIndexed { index, mode ->
        SegmentedButton(
          shape = SegmentedButtonDefaults.itemShape(index = index, count = themeModes.size),
          onClick = { onUpdateTheme(mode) },
          selected = themeMode == mode,
        ) {
          Text(themeLabels[index], fontSize = 12.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Text(stringResource(R.string.language), fontWeight = FontWeight.SemiBold)
    val languages = listOf(AppLanguage.SYSTEM, AppLanguage.ENGLISH, AppLanguage.NORWEGIAN)
    val languageLabels =
      listOf(
        stringResource(R.string.lang_system),
        stringResource(R.string.lang_english),
        stringResource(R.string.lang_norwegian),
      )
    SingleChoiceSegmentedButtonRow(
      modifier = Modifier.fillMaxWidth(),
    ) {
      languages.forEachIndexed { index, lang ->
        SegmentedButton(
          shape = SegmentedButtonDefaults.itemShape(index = index, count = languages.size),
          onClick = { onUpdateLanguage(lang) },
          selected = language == lang,
        ) {
          Text(languageLabels[index], fontSize = 12.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
    Text(stringResource(R.string.allow_dual_channel), fontWeight = FontWeight.SemiBold)
    Text(
      stringResource(R.string.allow_dual_channel_desc),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp),
    )
    Spacer(modifier = Modifier.height(8.dp))
    Switch(
      checked = allowDualChannel,
      onCheckedChange = { onUpdateAllowDualChannel(it) },
    )

    Spacer(modifier = Modifier.height(24.dp))
    Text(stringResource(R.string.freq_range_hz), fontWeight = FontWeight.SemiBold)

    // Local state for free editing
    var minText by remember { mutableStateOf(minFreq.toString()) }
    var maxText by remember { mutableStateOf(maxFreq.toString()) }

    // Sync local state when ViewModel updates (e.g. from reset)
    LaunchedEffect(minFreq, maxFreq) {
      if (!minText.all { it.isDigit() } || minText.toIntOrNull() != minFreq) {
        minText = minFreq.toString()
      }
      if (!maxText.all { it.isDigit() } || maxText.toIntOrNull() != maxFreq) {
        maxText = maxFreq.toString()
      }
    }

    fun finalizeFrequencyChanges() {
      val minVal = minText.toIntOrNull() ?: minFreq
      val maxVal = maxText.toIntOrNull() ?: maxFreq

      // Clamp and ensure min < max
      var finalMin = minVal.coerceIn(10, 30000)
      var finalMax = maxVal.coerceIn(10, 30000)

      if (finalMin >= finalMax) {
        if (finalMin < 30000) {
          finalMax = finalMin + 1
        } else {
          finalMin = finalMax - 1
        }
      }

      onUpdateFreqRange(finalMin, finalMax)
      minText = finalMin.toString()
      maxText = finalMax.toString()
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      OutlinedTextField(
        value = minText,
        onValueChange = { input ->
          minText = input.filter { it.isDigit() }
        },
        label = { Text(stringResource(R.string.min)) },
        keyboardOptions =
          KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
          ),
        keyboardActions =
          KeyboardActions(
            onDone = {
              finalizeFrequencyChanges()
              focusManager.clearFocus()
            },
          ),
        modifier =
          Modifier
            .weight(1f)
            .onFocusChanged { if (!it.isFocused) finalizeFrequencyChanges() },
      )
      OutlinedTextField(
        value = maxText,
        onValueChange = { input ->
          maxText = input.filter { it.isDigit() }
        },
        label = { Text(stringResource(R.string.max)) },
        keyboardOptions =
          KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
          ),
        keyboardActions =
          KeyboardActions(
            onDone = {
              finalizeFrequencyChanges()
              focusManager.clearFocus()
            },
          ),
        modifier =
          Modifier
            .weight(1f)
            .onFocusChanged { if (!it.isFocused) finalizeFrequencyChanges() },
      )
    }

    Spacer(modifier = Modifier.height(24.dp))
    Text(stringResource(R.string.max_remote_volume, maxRemoteVolume), fontWeight = FontWeight.SemiBold)
    Text(
      stringResource(R.string.max_remote_volume_desc),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 8.dp),
    )
    Slider(
      value = maxRemoteVolume.toFloat(),
      onValueChange = { onUpdateMaxRemoteVolume(it.toInt()) },
      valueRange = 10f..100f,
      steps = 17, // (100 - 10) / 5 = 18 intervals? No, (100-10)/5 = 18, so 17 steps for 5% increments
      modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(24.dp))
    Text(stringResource(R.string.graph_duration, graphDuration), fontWeight = FontWeight.SemiBold)
    Slider(
      value = graphDuration.toFloat(),
      onValueChange = { onUpdateGraphDuration(it.toInt()) },
      valueRange = 2f..10f,
      steps = 7,
      modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(24.dp))
    Text(stringResource(R.string.graph_smoothing, graphSmoothing), fontWeight = FontWeight.SemiBold)
    Slider(
      value = graphSmoothing.toFloat(),
      onValueChange = { onUpdateGraphSmoothing(it.toInt()) },
      valueRange = 1f..10f,
      steps = 8,
      modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(24.dp))
    Text(stringResource(R.string.num_positions, positionCount), fontWeight = FontWeight.SemiBold)
    Slider(
      value = positionCount.toFloat(),
      onValueChange = { onUpdatePositionCount(it.toInt()) },
      valueRange = 1f..6f,
      steps = 4,
      modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(24.dp))
    Text(stringResource(R.string.position_names), fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(8.dp))

    for (i in 0 until positionCount) {
      OutlinedTextField(
        value = positionNames[i],
        onValueChange = { onUpdatePositionName(i, it) },
        label = { Text(stringResource(R.string.position_name_label, i + 1)) },
        maxLines = 1,
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
      )
    }

    Spacer(modifier = Modifier.height(32.dp))
    Button(
      onClick = { onResetToDefaults() },
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
    ) {
      Text(stringResource(R.string.reset_defaults))
    }
  }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
  LFTonegenTheme {
    SettingsContent(
      themeMode = ThemeMode.AUTO,
      language = AppLanguage.SYSTEM,
      allowDualChannel = false,
      minFreq = 20,
      maxFreq = 400,
      graphDuration = 3,
      graphSmoothing = 3,
      maxRemoteVolume = 50,
      positionCount = 3,
      positionNames = List(6) { "Position ${it + 1}" },
      onUpdateTheme = {},
      onUpdateLanguage = {},
      onUpdateAllowDualChannel = {},
      onUpdateFreqRange = { _, _ -> },
      onUpdateGraphDuration = {},
      onUpdateGraphSmoothing = {},
      onUpdateMaxRemoteVolume = {},
      onUpdatePositionCount = {},
      onUpdatePositionName = { _, _ -> },
      onResetToDefaults = {},
    )
  }
}
