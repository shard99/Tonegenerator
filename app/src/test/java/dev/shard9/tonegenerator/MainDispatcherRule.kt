package dev.shard9.tonegenerator

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Replaces `Dispatchers.Main` with a test dispatcher so `viewModelScope` work is controllable.
 *
 * Pass [testDispatcher] to `runTest` (e.g. `runTest(rule.testDispatcher)`) so the test body and the
 * ViewModel share a single virtual clock; otherwise `advanceUntilIdle()` has nothing to drive.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
  val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
  override fun starting(description: Description) {
    Dispatchers.setMain(testDispatcher)
  }

  override fun finished(description: Description) {
    Dispatchers.resetMain()
  }
}
