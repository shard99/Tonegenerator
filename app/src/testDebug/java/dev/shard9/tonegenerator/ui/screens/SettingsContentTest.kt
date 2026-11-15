package dev.shard9.tonegenerator.ui.screens

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import dev.shard9.tonegenerator.AppLanguage
import dev.shard9.tonegenerator.ThemeMode
import dev.shard9.tonegenerator.ui.theme.LFTonegenTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Behaviour tests for the companion ceiling control added in `Config max-volume`.
 *
 * Runs on the JVM via Robolectric, so no emulator is needed. SDK 35 is pinned because it is the
 * highest platform Robolectric ships an `android-all` jar for; the screen has no
 * API-level-conditional behaviour.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsContentTest {
  @get:Rule
  val composeTestRule = createComposeRule()

  /** The companion ceiling is the only slider on this screen with a 10..100 range. */
  private fun maxRemoteVolumeSlider() =
    SemanticsMatcher("slider with range 10..100") { node ->
      val config = node.config
      config.contains(SemanticsProperties.ProgressBarRangeInfo) &&
        config[SemanticsProperties.ProgressBarRangeInfo].range == 10f..100f
    }

  private fun setSettingsContent(
    maxRemoteVolume: MutableIntState,
    onUpdateMaxRemoteVolume: (Int) -> Unit = { maxRemoteVolume.intValue = it },
  ) {
    composeTestRule.setContent {
      LFTonegenTheme(darkTheme = false, dynamicColor = false) {
        SettingsContent(
          themeMode = ThemeMode.AUTO,
          language = AppLanguage.SYSTEM,
          allowDualChannel = false,
          minFreq = 20,
          maxFreq = 400,
          graphDuration = 3,
          graphSmoothing = 3,
          maxRemoteVolume = maxRemoteVolume.intValue,
          positionCount = 3,
          positionNames = List(6) { "Position ${it + 1}" },
          onUpdateTheme = {},
          onUpdateLanguage = {},
          onUpdateAllowDualChannel = {},
          onUpdateFreqRange = { _, _ -> },
          onUpdateGraphDuration = {},
          onUpdateGraphSmoothing = {},
          onUpdateMaxRemoteVolume = onUpdateMaxRemoteVolume,
          onUpdatePositionCount = {},
          onUpdatePositionName = { _, _ -> },
          onResetToDefaults = {},
        )
      }
    }
  }

  @Test
  fun `label renders the current ceiling`() {
    setSettingsContent(mutableIntStateOf(50))

    composeTestRule.onNodeWithText("Max Companion Volume: 50%").assertExists()
  }

  @Test
  fun `label reflects a non-default ceiling`() {
    setSettingsContent(mutableIntStateOf(85))

    composeTestRule.onNodeWithText("Max Companion Volume: 85%").assertExists()
  }

  @Test
  fun `moving the slider reports the new value`() {
    val updates = mutableListOf<Int>()
    setSettingsContent(mutableIntStateOf(50)) { updates += it }

    composeTestRule
      .onNode(maxRemoteVolumeSlider())
      .performSemanticsAction(SemanticsActions.SetProgress) { it(80f) }

    assertEquals(listOf(80), updates)
  }

  @Test
  fun `slider and label stay in sync`() {
    setSettingsContent(mutableIntStateOf(50))
    composeTestRule.onNodeWithText("Max Companion Volume: 50%").assertExists()

    composeTestRule
      .onNode(maxRemoteVolumeSlider())
      .performSemanticsAction(SemanticsActions.SetProgress) { it(90f) }

    composeTestRule.onNodeWithText("Max Companion Volume: 90%").assertExists()
  }
}
