package dev.shard9.tonegenerator.data

import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import dev.shard9.tonegenerator.AppLanguage
import dev.shard9.tonegenerator.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pin-down tests for every persisted setting, including the legacy-key migrations that are easy
 * to break without noticing.
 */
class SettingsRepositoryTest {
  @Test
  fun `defaults are returned when nothing has been stored`() =
    runTest {
      val repository = SettingsRepository(FakePreferencesDataStore())

      val settings = repository.settingsFlow.first()

      assertEquals(3, settings.positionCount)
      assertEquals(20, settings.minFreq)
      assertEquals(400, settings.maxFreq)
      assertEquals(ThemeMode.AUTO, settings.themeMode)
      assertEquals(AppLanguage.SYSTEM, settings.language)
      assertEquals(3, settings.graphDuration)
      assertEquals(3, settings.graphSmoothing)
      assertEquals(50, settings.volume)
      assertEquals(50, settings.maxRemoteVolume)
      assertFalse(settings.allowDualChannel)
      assertEquals(List(6) { "Position ${it + 1}" }, settings.positionNames)
    }

  @Test
  fun `volume falls back to the legacy local_volume key`() =
    runTest {
      val store = FakePreferencesDataStore(mutablePreferencesOf(intPreferencesKey("local_volume") to 77))

      val settings = SettingsRepository(store).settingsFlow.first()

      assertEquals(77, settings.volume)
    }

  @Test
  fun `volume prefers the current key over the legacy key`() =
    runTest {
      val store =
        FakePreferencesDataStore(
          mutablePreferencesOf(
            intPreferencesKey("local_volume") to 77,
            intPreferencesKey("volume") to 12,
          ),
        )

      val settings = SettingsRepository(store).settingsFlow.first()

      assertEquals(12, settings.volume)
    }

  @Test
  fun `frequency range migrates from floats stored under the same keys`() =
    runTest {
      val store =
        FakePreferencesDataStore(
          mutablePreferencesOf(
            floatPreferencesKey("min_freq") to 30.5f,
            floatPreferencesKey("max_freq") to 1234.9f,
          ),
        )

      val settings = SettingsRepository(store).settingsFlow.first()

      assertEquals(30, settings.minFreq)
      assertEquals(1234, settings.maxFreq)
    }

  @Test
  fun `max remote volume round trips`() =
    runTest {
      val repository = SettingsRepository(FakePreferencesDataStore())
      assertEquals(50, repository.settingsFlow.first().maxRemoteVolume)

      repository.updateMaxRemoteVolume(85)

      assertEquals(85, repository.settingsFlow.first().maxRemoteVolume)
    }

  @Test
  fun `updates are persisted individually`() =
    runTest {
      val repository = SettingsRepository(FakePreferencesDataStore())

      repository.updatePositionCount(5)
      repository.updateFreqRange(50, 5000)
      repository.updateTheme(ThemeMode.DARK)
      repository.updateLanguage(AppLanguage.NORWEGIAN)
      repository.updateGraphDuration(7)
      repository.updateGraphSmoothing(9)
      repository.updateVolume(33)
      repository.updateMaxRemoteVolume(64)
      repository.updateAllowDualChannel(true)
      repository.updatePositionName(0, "Baseline")

      val settings = repository.settingsFlow.first()
      assertEquals(5, settings.positionCount)
      assertEquals(50, settings.minFreq)
      assertEquals(5000, settings.maxFreq)
      assertEquals(ThemeMode.DARK, settings.themeMode)
      assertEquals(AppLanguage.NORWEGIAN, settings.language)
      assertEquals(7, settings.graphDuration)
      assertEquals(9, settings.graphSmoothing)
      assertEquals(33, settings.volume)
      assertEquals(64, settings.maxRemoteVolume)
      assertTrue(settings.allowDualChannel)
      assertEquals("Baseline", settings.positionNames[0])
      assertEquals("Position 2", settings.positionNames[1])
    }

  @Test
  fun `reset to defaults clears every stored preference`() =
    runTest {
      val repository = SettingsRepository(FakePreferencesDataStore())
      repository.updateVolume(99)
      repository.updateMaxRemoteVolume(99)
      repository.updatePositionCount(6)

      repository.resetToDefaults()

      val settings = repository.settingsFlow.first()
      assertEquals(50, settings.volume)
      assertEquals(50, settings.maxRemoteVolume)
      assertEquals(3, settings.positionCount)
    }
}
