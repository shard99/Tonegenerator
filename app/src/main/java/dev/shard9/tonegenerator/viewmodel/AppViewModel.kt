package dev.shard9.tonegenerator.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    var minFreq by mutableIntStateOf(20)
        private set
    var maxFreq by mutableIntStateOf(400)
        private set
    var themeMode by mutableStateOf(ThemeMode.AUTO)
        private set
    var graphDuration by mutableIntStateOf(3)
        private set
    var graphSmoothing by mutableIntStateOf(3)
        private set
    var selectedFrequency by mutableFloatStateOf(100f)
        private set
    var isPlaying by mutableStateOf(false)
        private set

    var currentSessionMeasurements = mutableStateMapOf<Int, Double>()
        private set
    var history = mutableStateListOf<String>()
        private set

    data class DataPoint(val timestamp: Long, val value: Double)

    var graphData = mutableStateListOf<DataPoint>()
        private set
    var sessionStartTime by mutableLongStateOf(0L)
        private set

    private val smoothingBuffer = mutableListOf<Double>()

    init {
        viewModelScope.launch {
            repository.settingsFlow.collect { settings ->
                positionCount = settings.positionCount
                positionNames = settings.positionNames
                minFreq = settings.minFreq
                maxFreq = settings.maxFreq
                themeMode = settings.themeMode
                graphDuration = settings.graphDuration
                graphSmoothing = settings.graphSmoothing

                // Prune graph data if duration changed
                val now = System.currentTimeMillis()
                while (graphData.isNotEmpty() && (now - graphData.first().timestamp) > (graphDuration * 1000L)) {
                    graphData.removeAt(0)
                }

                // Ensure history is capped
                while (history.size > 50) {
                    history.removeAt(history.size - 1)
                }

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

    fun updatePlayingState(playing: Boolean) {
        isPlaying = playing
        if (playing) {
            sessionStartTime = System.currentTimeMillis()
            graphData.clear()
            smoothingBuffer.clear()
        } else {
            graphData.clear()
            smoothingBuffer.clear()
        }
    }

    fun addGraphPoint(value: Double) {
        if (!isPlaying) return

        // Apply smoothing
        smoothingBuffer.add(value)
        while (smoothingBuffer.size > graphSmoothing) {
            smoothingBuffer.removeAt(0)
        }

        val smoothedValue = if (smoothingBuffer.isEmpty()) 0.0 else smoothingBuffer.average()

        val now = System.currentTimeMillis()
        graphData.add(DataPoint(now, smoothedValue))
        // Keep only requested duration
        while (graphData.isNotEmpty() && (now - graphData.first().timestamp) > (graphDuration * 1000L)) {
            graphData.removeAt(0)
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

    fun updateGraphDuration(duration: Int) {
        viewModelScope.launch {
            repository.updateGraphDuration(duration)
        }
    }

    fun updateGraphSmoothing(smoothing: Int) {
        viewModelScope.launch {
            repository.updateGraphSmoothing(smoothing)
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
        }
    }

    fun clearHistory() {
        history.clear()
    }

    fun saveMeasurement(positionIndex: Int, value: Double) {
        currentSessionMeasurements[positionIndex] = value
    }

    fun getCSVHeader(): String {
        val header = StringBuilder("Hz")
        for (i in 0 until positionCount) {
            header.append(";${positionNames[i]};Value for ${positionNames[i]}")
        }
        return header.toString()
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
        while (history.size > positionCount) {
            history.removeAt(history.size - 1)
        }

        onResult(resultString)
        currentSessionMeasurements.clear()
    }
}
