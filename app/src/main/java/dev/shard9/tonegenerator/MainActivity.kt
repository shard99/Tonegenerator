package dev.shard9.tonegenerator

import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.core.os.LocaleListCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.shard9.tonegenerator.audio.BleManager
import dev.shard9.tonegenerator.audio.ToneGenerator
import dev.shard9.tonegenerator.data.SettingsRepository
import dev.shard9.tonegenerator.ui.AppNavigation
import dev.shard9.tonegenerator.ui.theme.LFTonegenTheme
import dev.shard9.tonegenerator.viewmodel.AppViewModel

val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeMode { LIGHT, DARK, AUTO }

class MainActivity : AppCompatActivity() {
  private var toneGenerator: ToneGenerator? = null
  private var bleManager: BleManager? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    toneGenerator = ToneGenerator()
    bleManager = BleManager(this)
    val repository = SettingsRepository(dataStore)

    setContent {
      val viewModel: AppViewModel =
        viewModel(
          factory =
            object : androidx.lifecycle.ViewModelProvider.Factory {
              override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return AppViewModel(repository, bleManager!!) as T
              }
            },
        )

      LaunchedEffect(viewModel.language) {
        val languageTag =
          when (viewModel.language) {
            AppLanguage.SYSTEM -> ""
            AppLanguage.ENGLISH -> "en"
            AppLanguage.NORWEGIAN -> "nb"
          }
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageTag)
        if (AppCompatDelegate.getApplicationLocales() != appLocale) {
          AppCompatDelegate.setApplicationLocales(appLocale)
        }
      }

      val darkTheme =
        when (viewModel.themeMode) {
          ThemeMode.LIGHT -> false
          ThemeMode.DARK -> true
          ThemeMode.AUTO -> isSystemInDarkTheme()
        }

      LFTonegenTheme(darkTheme = darkTheme) {
        toneGenerator?.let { generator ->
          AppNavigation(toneGenerator = generator, viewModel = viewModel)
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    toneGenerator?.release()
    bleManager?.disconnect()
  }
}
