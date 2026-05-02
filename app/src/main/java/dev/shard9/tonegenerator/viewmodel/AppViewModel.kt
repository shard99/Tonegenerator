package dev.shard9.tonegenerator.viewmodel

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.shard9.tonegenerator.ThemeMode
import dev.shard9.tonegenerator.data.SettingsRepository
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.*

class AppViewModel(private val repository: SettingsRepository) : ViewModel() {
    var positionCount by mutableIntStateOf(3)
        private set
    var positionNames by mutableStateOf(List(6) { "Position ${it + 1}" })
        private set
    var minFreq by mutableIntStateOf(20)
        private set
    var maxFreq by mutableIntStateOf(400)
        private set
    var themeMode by mutableStateOf(ThemeMode.AUTO)
        private set
    var selectedFrequency by mutableFloatStateOf(100f)
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

                // Ensure selected frequency is within valid range
                if (selectedFrequency < minFreq || selectedFrequency > maxFreq) {
                    resetSelectedFrequency(minFreq, maxFreq)
                }
            }
        }
    }

    private fun resetSelectedFrequency(min: Int, max: Int) {
        // Set to 10% into the linear range
        selectedFrequency = min + (max - min) * 0.1f
    }

    fun updateSelectedFrequency(freq: Float) {
        selectedFrequency = freq
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

    fun updateFreqRange(min: Int, max: Int) {
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

    fun finishSession(frequency: Double, onResult: (String) -> Unit) {
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

        onResult(resultString)
        currentSessionMeasurements.clear()
    }
}
