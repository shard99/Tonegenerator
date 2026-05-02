package dev.shard9.tonegenerator.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import dev.shard9.tonegenerator.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    private val positionCountKey = intPreferencesKey("position_count")
    private val minFreqKey = intPreferencesKey("min_freq")
    private val maxFreqKey = intPreferencesKey("max_freq")
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val graphDurationKey = intPreferencesKey("graph_duration")
    private val graphSmoothingKey = intPreferencesKey("graph_smoothing")
    private val positionNamesPrefix = "position_name_"

    data class AppSettings(
        val positionCount: Int,
        val minFreq: Int,
        val maxFreq: Int,
        val themeMode: ThemeMode,
        val graphDuration: Int,
        val graphSmoothing: Int,
        val positionNames: List<String>
    )

    val settingsFlow: Flow<AppSettings> = dataStore.data.map { prefs ->
        val positionCount = prefs[positionCountKey] ?: 3

        // Handle migration from Float to Int to prevent ClassCastException
        val minFreq = try {
            prefs[minFreqKey] ?: 20
        } catch (_: ClassCastException) {
            // If it was stored as Float, we'll get an error. Convert it.
            (prefs[floatPreferencesKey("min_freq")] ?: 20f).toInt()
        }

        val maxFreq = try {
            prefs[maxFreqKey] ?: 400
        } catch (_: ClassCastException) {
            (prefs[floatPreferencesKey("max_freq")] ?: 400f).toInt()
        }

        val themeMode = ThemeMode.valueOf(prefs[themeModeKey] ?: ThemeMode.AUTO.name)
        val graphDuration = prefs[graphDurationKey] ?: 3
        val graphSmoothing = prefs[graphSmoothingKey] ?: 3

        val positionNames = List(6) { i ->
            prefs[stringPreferencesKey(positionNamesPrefix + i)] ?: "Position ${i + 1}"
        }

        AppSettings(positionCount, minFreq, maxFreq, themeMode, graphDuration, graphSmoothing, positionNames)
    }

    suspend fun updatePositionCount(count: Int) {
        dataStore.edit { it[positionCountKey] = count }
    }

    suspend fun updatePositionName(index: Int, name: String) {
        dataStore.edit { it[stringPreferencesKey(positionNamesPrefix + index)] = name }
    }

    suspend fun updateFreqRange(min: Int, max: Int) {
        dataStore.edit {
            it[minFreqKey] = min
            it[maxFreqKey] = max
        }
    }

    suspend fun updateTheme(mode: ThemeMode) {
        dataStore.edit { it[themeModeKey] = mode.name }
    }

    suspend fun updateGraphDuration(duration: Int) {
        dataStore.edit { it[graphDurationKey] = duration }
    }

    suspend fun updateGraphSmoothing(smoothing: Int) {
        dataStore.edit { it[graphSmoothingKey] = smoothing }
    }

    suspend fun resetToDefaults() {
        dataStore.edit { it.clear() }
    }
}
