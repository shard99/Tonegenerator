package dev.shard9.tonegenerator.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.shard9.tonegenerator.AppLanguage
import dev.shard9.tonegenerator.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(
  private val dataStore: DataStore<Preferences>,
) {
  private val positionCountKey = intPreferencesKey("position_count")
  private val minFreqKey = intPreferencesKey("min_freq")
  private val maxFreqKey = intPreferencesKey("max_freq")
  private val themeModeKey = stringPreferencesKey("theme_mode")
  private val languageKey = stringPreferencesKey("app_language")
  private val graphDurationKey = intPreferencesKey("graph_duration")
  private val graphSmoothingKey = intPreferencesKey("graph_smoothing")
  private val volumeKey = intPreferencesKey("volume")
  private val allowDualChannelKey = booleanPreferencesKey("allow_dual_channel")
  private val positionNamesPrefix = "position_name_"

  data class AppSettings(
    val positionCount: Int,
    val minFreq: Int,
    val maxFreq: Int,
    val themeMode: ThemeMode,
    val language: AppLanguage,
    val graphDuration: Int,
    val graphSmoothing: Int,
    val volume: Int,
    val allowDualChannel: Boolean,
    val positionNames: List<String>,
  )

  val settingsFlow: Flow<AppSettings> =
    dataStore.data.map { prefs ->
      val positionCount = prefs[positionCountKey] ?: 3

      // Handle migration from Float to Int to prevent ClassCastException
      val minFreq =
        try {
          prefs[minFreqKey] ?: 20
        } catch (_: ClassCastException) {
          // If it was stored as Float, we'll get an error. Convert it.
          (prefs[floatPreferencesKey("min_freq")] ?: 20f).toInt()
        }

      val maxFreq =
        try {
          prefs[maxFreqKey] ?: 400
        } catch (_: ClassCastException) {
          (prefs[floatPreferencesKey("max_freq")] ?: 400f).toInt()
        }

      val themeMode = ThemeMode.valueOf(prefs[themeModeKey] ?: ThemeMode.AUTO.name)
      val language = AppLanguage.valueOf(prefs[languageKey] ?: AppLanguage.SYSTEM.name)
      val graphDuration = prefs[graphDurationKey] ?: 3
      val graphSmoothing = prefs[graphSmoothingKey] ?: 3

      // Fallback for migration from localVolumeKey if volumeKey is missing
      val volume = prefs[volumeKey] ?: prefs[intPreferencesKey("local_volume")] ?: 50

      val allowDualChannel = prefs[allowDualChannelKey] ?: false

      val positionNames =
        List(6) { i ->
          prefs[stringPreferencesKey(positionNamesPrefix + i)] ?: "Position ${i + 1}"
        }

      AppSettings(
        positionCount = positionCount,
        minFreq = minFreq,
        maxFreq = maxFreq,
        themeMode = themeMode,
        language = language,
        graphDuration = graphDuration,
        graphSmoothing = graphSmoothing,
        volume = volume,
        allowDualChannel = allowDualChannel,
        positionNames = positionNames,
      )
    }

  suspend fun updatePositionCount(count: Int) {
    dataStore.edit { it[positionCountKey] = count }
  }

  suspend fun updatePositionName(
    index: Int,
    name: String,
  ) {
    dataStore.edit { it[stringPreferencesKey(positionNamesPrefix + index)] = name }
  }

  suspend fun updateFreqRange(
    min: Int,
    max: Int,
  ) {
    dataStore.edit {
      it[minFreqKey] = min
      it[maxFreqKey] = max
    }
  }

  suspend fun updateTheme(mode: ThemeMode) {
    dataStore.edit { it[themeModeKey] = mode.name }
  }

  suspend fun updateLanguage(language: AppLanguage) {
    dataStore.edit { it[languageKey] = language.name }
  }

  suspend fun updateGraphDuration(duration: Int) {
    dataStore.edit { it[graphDurationKey] = duration }
  }

  suspend fun updateGraphSmoothing(smoothing: Int) {
    dataStore.edit { it[graphSmoothingKey] = smoothing }
  }

  suspend fun updateVolume(volume: Int) {
    dataStore.edit { it[volumeKey] = volume }
  }

  suspend fun updateAllowDualChannel(allow: Boolean) {
    dataStore.edit { it[allowDualChannelKey] = allow }
  }

  suspend fun resetToDefaults() {
    dataStore.edit { it.clear() }
  }
}
