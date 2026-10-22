package eu.pascariu.shoppinglist.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import eu.pascariu.shoppinglist.di.IoDispatcher
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ListPreferencesDataSource(
    private val dataStore: DataStore<Preferences>,
    ioDispatcher: CoroutineDispatcher,
) {

    private val scope = kotlinx.coroutines.CoroutineScope(ioDispatcher + kotlinx.coroutines.SupervisorJob())

    private val expanded = MutableStateFlow(false)
    val widgetStackExpanded: StateFlow<Boolean> = expanded.asStateFlow()

    private val activeShop = MutableStateFlow<Long?>(null)
    val activeShopId: StateFlow<Long?> = activeShop.asStateFlow()

    private val ready = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = ready.asStateFlow()

    init {
        dataStore.data
            .catch { failure ->
                if (failure is IOException) emit(emptyPreferences()) else throw failure
            }
            .onEach { prefs ->
                expanded.value = prefs[KEY_WIDGET_STACK_EXPANDED] ?: false
                activeShop.value = prefs[KEY_ACTIVE_SHOP_ID]
                ready.value = true
            }
            .launchIn(scope)
    }

    suspend fun setWidgetStackExpanded(isExpanded: Boolean) {
        expanded.value = isExpanded
        dataStore.edit { prefs ->
            prefs[KEY_WIDGET_STACK_EXPANDED] = isExpanded
        }
    }

    suspend fun setActiveShopId(shopId: Long?) {
        activeShop.value = shopId
        dataStore.edit { prefs ->
            if (shopId == null) {
                prefs.remove(KEY_ACTIVE_SHOP_ID)
            } else {
                prefs[KEY_ACTIVE_SHOP_ID] = shopId
            }
        }
    }

    private companion object {
        val KEY_WIDGET_STACK_EXPANDED = booleanPreferencesKey("widget_stack_expanded")
        val KEY_ACTIVE_SHOP_ID = longPreferencesKey("active_shop_id")
    }
}
