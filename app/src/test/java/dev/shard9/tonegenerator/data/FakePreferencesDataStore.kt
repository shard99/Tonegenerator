package dev.shard9.tonegenerator.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory [DataStore] for JVM tests.
 *
 * Preferred over a real file-backed DataStore because it is deterministic and avoids the
 * filesystem entirely (`PreferenceDataStoreFactory` fails on its second atomic rename under
 * Windows). `Preferences.edit { }` works unchanged on top of this, so the migration logic in
 * [SettingsRepository] is still exercised for real.
 */
class FakePreferencesDataStore(
  initial: Preferences = emptyPreferences(),
) : DataStore<Preferences> {
  private val state = MutableStateFlow(initial)

  override val data: Flow<Preferences> = state

  override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
    val updated = transform(state.value)
    state.value = updated
    return updated
  }
}
