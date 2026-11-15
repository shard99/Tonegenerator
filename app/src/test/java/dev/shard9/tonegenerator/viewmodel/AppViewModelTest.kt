package dev.shard9.tonegenerator.viewmodel

import androidx.compose.runtime.snapshots.Snapshot
import dev.shard9.tonegenerator.MainDispatcherRule
import dev.shard9.tonegenerator.audio.FakeRemoteGenerator
import dev.shard9.tonegenerator.data.FakePreferencesDataStore
import dev.shard9.tonegenerator.data.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Lock-down tests for the app's central state machine. Everything runs on the JVM: settings come
 * from an in-memory DataStore, BLE is replaced by [FakeRemoteGenerator].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  private val remote = FakeRemoteGenerator()

  private fun createRepository() = SettingsRepository(FakePreferencesDataStore())

  @Test
  fun `defaults are applied on init`() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = AppViewModel(createRepository(), remote)
      advanceUntilIdle()

      assertEquals(3, viewModel.positionCount)
      assertEquals(20, viewModel.minFreq)
      assertEquals(400, viewModel.maxFreq)
      assertEquals(50, viewModel.volume)
      assertEquals(50, viewModel.maxRemoteVolume)
      assertEquals(100f, viewModel.selectedFrequency, 0.001f)
    }

  @Test
  fun `disabling dual channel forces the right channel`() =
    runTest(mainDispatcherRule.testDispatcher) {
      val repository = createRepository()
      repository.updateAllowDualChannel(true)
      val viewModel = AppViewModel(repository, remote)
      advanceUntilIdle()

      viewModel.updateChannelSelection(1)
      assertEquals(1, viewModel.channelSelection)

      repository.updateAllowDualChannel(false)
      advanceUntilIdle()

      assertEquals(2, viewModel.channelSelection)
    }

  @Test
  fun `selected frequency is clamped into the configured range`() =
    runTest(mainDispatcherRule.testDispatcher) {
      val repository = createRepository()
      repository.updateFreqRange(100, 200)
      val viewModel = AppViewModel(repository, remote)
      advanceUntilIdle()

      viewModel.updateSelectedFrequency(500f)
      // Any settings change re-runs the collector, which re-validates the selection.
      repository.updatePositionCount(4)
      advanceUntilIdle()

      // Reset to 10% into the linear range: 100 + (200 - 100) * 0.1
      assertEquals(110f, viewModel.selectedFrequency, 0.001f)
    }

  @Test
  fun `graph data older than the configured duration is pruned`() =
    runTest(mainDispatcherRule.testDispatcher) {
      val repository = createRepository()
      val viewModel = AppViewModel(repository, remote)
      advanceUntilIdle()

      val now = System.currentTimeMillis()
      val stale = AppViewModel.DataPoint(now - 10_000, 1.0)
      val fresh = AppViewModel.DataPoint(now, 2.0)
      viewModel.graphData.add(stale)
      viewModel.graphData.add(fresh)

      repository.updatePositionCount(4)
      advanceUntilIdle()

      assertEquals(listOf(fresh), viewModel.graphData.toList())
    }

  @Test
  fun `history is capped at fifty entries`() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = AppViewModel(createRepository(), remote)
      advanceUntilIdle()

      repeat(60) {
        viewModel.saveMeasurement(0, 1.0)
        viewModel.finishSession(100.0) {}
      }

      assertEquals(50, viewModel.history.size)
    }

  @Test
  fun `sync pushes the current state to the remote`() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = AppViewModel(createRepository(), remote)
      advanceUntilIdle()
      viewModel.updateUseRemoteGenerator(true)
      advanceUntilIdle()

      remote.completeSync()
      advanceUntilIdle()

      assertEquals(100f, remote.frequencyWrites.last(), 0.001f)
      assertEquals(2, remote.channelWrites.last())
      assertEquals(false, remote.playStateWrites.last())
      // Volume 50, scaled by a max remote volume of 50.
      assertEquals(25, remote.volumeWrites.last())
    }

  @Test
  fun `volume changes are scaled by the max remote volume`() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = AppViewModel(createRepository(), remote)
      advanceUntilIdle()
      viewModel.updateUseRemoteGenerator(true)
      advanceUntilIdle()
      remote.completeSync()
      advanceUntilIdle()
      remote.clearWrites()

      viewModel.updateVolume(80)
      Snapshot.sendApplyNotifications()
      advanceUntilIdle()

      assertEquals(listOf(40), remote.volumeWrites)
    }

  @Test
  fun `changing the max remote volume re-pushes the scaled volume`() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = AppViewModel(createRepository(), remote)
      advanceUntilIdle()
      viewModel.updateUseRemoteGenerator(true)
      advanceUntilIdle()
      remote.completeSync()
      advanceUntilIdle()
      remote.clearWrites()

      viewModel.updateMaxRemoteVolume(20)
      Snapshot.sendApplyNotifications()
      advanceUntilIdle()

      assertEquals(listOf(10), remote.volumeWrites)
    }

  @Test
  fun `volume changes are ignored while the remote generator is off`() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = AppViewModel(createRepository(), remote)
      advanceUntilIdle()

      viewModel.updateVolume(80)
      Snapshot.sendApplyNotifications()
      advanceUntilIdle()

      assertTrue(remote.volumeWrites.isEmpty())
    }

  @Test
  fun `turning the remote generator off stops playback and disconnects`() =
    runTest(mainDispatcherRule.testDispatcher) {
      val viewModel = AppViewModel(createRepository(), remote)
      advanceUntilIdle()
      viewModel.updateUseRemoteGenerator(true)
      advanceUntilIdle()
      remote.completeSync()
      advanceUntilIdle()
      remote.clearWrites()

      viewModel.updateUseRemoteGenerator(false)
      advanceUntilIdle()

      assertEquals(1, remote.disconnectCount)
      assertEquals(listOf(false), remote.playStateWrites)
    }
}
