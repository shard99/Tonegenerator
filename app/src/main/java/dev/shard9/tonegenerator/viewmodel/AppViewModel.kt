package dev.shard9.tonegenerator.viewmodel

import androidx.compose.runtime.*
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shard9.tonegenerator.ThemeMode
import dev.shard9.tonegenerator.data.SettingsRepository
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

class AppViewModel(private val repository: SettingsRepository) : ViewModel() {
    var positionCount by mutableIntStateOf(3)
        private set
    var positionNames by mutableStateOf(List(6) { "Position ${it + 1}" })
        private set
    var minFreq by mutableFloatStateOf(20f)
        private set
    var maxFreq by mutableFloatStateOf(400f)
        private set
    var themeMode by mutableStateOf(ThemeMode.AUTO)
        private set

    var currentSessionMeasurements = mutableStateMapOf<Int, Double>()
        private set
    var history = mutableStateListOf<String>()
        private set

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                positionCount = settings.positionCount
                positionNames = settings.positionNames
                minFreq = settings.minFreq
                maxFreq = settings.maxFreq
                themeMode = settings.themeMode
            }
        }
    }

    fun updatePositionCount(count: Int) {
        viewModelScope.launch {
            repository.updatePositionCount(count)
        }
    }

    fun updatePositionName(index: Int, name: String) {
        val sanitized = name.take(30).replace("\n", "")
        viewModelScope.launch {
            repository.updatePositionName(index, sanitized)
        }
    }

    fun updateFreqRange(min: Float, max: Float) {
        viewModelScope.launch {
            repository.updateFreqRange(min, max)
        }
    }

    fun updateTheme(mode: ThemeMode) {
        viewModelScope.launch {
            repository.updateTheme(mode)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }

    fun saveMeasurement(positionIndex: Int, value: Double) {
        currentSessionMeasurements[positionIndex] = value
    }

    fun finishSession(frequency: Double, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
        if (currentSessionMeasurements.isEmpty()) return

        val result = StringBuilder("${frequency.roundToInt()}")
        for (i in 0 until positionCount) {
            currentSessionMeasurements[i]?.let { value ->
                val formattedValue = String.format(Locale.US, "%.1f", value)
                result.append(";${positionNames[i]};$formattedValue")
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
