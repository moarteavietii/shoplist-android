package eu.pascariu.shoppinglist.data.prefs

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.pascariu.shoppinglist.ui.theme.ThemeId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ThemeRepository(
    private val dataStore: DataStore<Preferences>,
    private val storeScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
) {

    private val themeFlow = MutableStateFlow(ThemeId.DEFAULT)
    val theme: StateFlow<ThemeId> = themeFlow.asStateFlow()

    private val changeToastsEnabledFlow = MutableStateFlow(true)
    val changeToastsEnabled: StateFlow<Boolean> = changeToastsEnabledFlow.asStateFlow()

    init {
        storeScope.launch(ioDispatcher) {
            val prefs = dataStore.data
                .catch { failure ->
                    Log.w(TAG, "Theme preferences load failed; using defaults", failure)
                    emit(emptyPreferences())
                }
                .first()
            themeFlow.value = prefs[KEY_THEME]?.let(ThemeId::fromName) ?: ThemeId.DEFAULT
            changeToastsEnabledFlow.value = prefs[KEY_CHANGE_TOASTS_ENABLED] ?: true
        }
    }

    suspend fun setTheme(themeId: ThemeId) {
        update(themeFlow, themeId) { prefs, value ->
            prefs[KEY_THEME] = value.name
        }
    }

    suspend fun setChangeToastsEnabled(enabled: Boolean) {
        update(changeToastsEnabledFlow, enabled) { prefs, value ->
            prefs[KEY_CHANGE_TOASTS_ENABLED] = value
        }
    }

    private suspend fun <T> update(
        flow: MutableStateFlow<T>,
        newValue: T,
        write: (MutablePreferences, T) -> Unit,
    ) {
        val previousValue = flow.value
        flow.value = newValue
        try {
            dataStore.edit { prefs -> write(prefs, newValue) }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            flow.value = previousValue
            Log.w(TAG, "Theme preference write failed", error)
        }
    }

    companion object {
        private const val TAG = "ThemeRepository"
        val KEY_THEME = stringPreferencesKey("theme_id")
        val KEY_CHANGE_TOASTS_ENABLED = booleanPreferencesKey("change_toasts_enabled")
    }
}
