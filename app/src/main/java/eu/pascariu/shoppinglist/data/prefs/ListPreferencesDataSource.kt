package eu.pascariu.shoppinglist.data.prefs

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.pascariu.shoppinglist.di.ApplicationScope
import eu.pascariu.shoppinglist.di.IoDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ListPreferencesDataSource(
    private val dataStore: DataStore<Preferences>,
    private val storeScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
) {

    private val expanded = MutableStateFlow(false)
    val quickMenuExpanded: StateFlow<Boolean> = expanded.asStateFlow()

    private val activeShop = MutableStateFlow<Long?>(null)
    val activeShopId: StateFlow<Long?> = activeShop.asStateFlow()

    private val searchExpandedFlow = MutableStateFlow(false)
    val searchExpanded: StateFlow<Boolean> = searchExpandedFlow.asStateFlow()

    private val showCheckedFlow = MutableStateFlow(true)
    val showChecked: StateFlow<Boolean> = showCheckedFlow.asStateFlow()

    private val sortModeEnabledFlow = MutableStateFlow(true)
    val sortModeEnabled: StateFlow<Boolean> = sortModeEnabledFlow.asStateFlow()

    private val sortOrderFlow = MutableStateFlow(SortOrder.MANUAL)
    val sortOrder: StateFlow<SortOrder> = sortOrderFlow.asStateFlow()

    private val ready = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = ready.asStateFlow()

    init {
        storeScope.launch(ioDispatcher) {
            val prefs = dataStore.data
                .catch { failure ->
                    Log.w(TAG, "List preferences load failed; using defaults", failure)
                    emit(emptyPreferences())
                }
                .first()
            expanded.value = prefs[KEY_QUICK_MENU_EXPANDED] ?: false
            activeShop.value = prefs[KEY_ACTIVE_SHOP_ID]
            searchExpandedFlow.value = prefs[KEY_SEARCH_EXPANDED] ?: false
            showCheckedFlow.value = prefs[KEY_SHOW_CHECKED] ?: true
            sortModeEnabledFlow.value = prefs[KEY_SORT_MODE_ENABLED] ?: true
            sortOrderFlow.value = prefs[KEY_SORT_ORDER]?.let(SortOrder::fromName) ?: SortOrder.MANUAL
            ready.value = true
        }
    }

    suspend fun setQuickMenuExpanded(isExpanded: Boolean) {
        update(expanded, isExpanded) { prefs, value ->
            prefs[KEY_QUICK_MENU_EXPANDED] = value
        }
    }

    suspend fun setActiveShopId(shopId: Long?) {
        update(activeShop, shopId) { prefs, value ->
            if (value == null) {
                prefs.remove(KEY_ACTIVE_SHOP_ID)
            } else {
                prefs[KEY_ACTIVE_SHOP_ID] = value
            }
        }
    }

    suspend fun setSearchExpanded(isExpanded: Boolean) {
        update(searchExpandedFlow, isExpanded) { prefs, value ->
            prefs[KEY_SEARCH_EXPANDED] = value
        }
    }

    suspend fun setShowChecked(show: Boolean) {
        update(showCheckedFlow, show) { prefs, value ->
            prefs[KEY_SHOW_CHECKED] = value
        }
    }

    suspend fun setSortModeEnabled(enabled: Boolean) {
        update(sortModeEnabledFlow, enabled) { prefs, value ->
            prefs[KEY_SORT_MODE_ENABLED] = value
        }
    }

    suspend fun setSortOrder(order: SortOrder) {
        update(sortOrderFlow, order) { prefs, value ->
            prefs[KEY_SORT_ORDER] = value.name
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
            Log.w(TAG, "List preferences write failed", error)
        }
    }

    private companion object {
        const val TAG = "ListPreferences"
        val KEY_QUICK_MENU_EXPANDED = booleanPreferencesKey("quick_menu_expanded")
        val KEY_ACTIVE_SHOP_ID = longPreferencesKey("active_shop_id")
        val KEY_SEARCH_EXPANDED = booleanPreferencesKey("search_expanded")
        val KEY_SHOW_CHECKED = booleanPreferencesKey("show_checked")
        val KEY_SORT_MODE_ENABLED = booleanPreferencesKey("sort_mode_enabled")
        val KEY_SORT_ORDER = stringPreferencesKey("sort_order")
    }
}

enum class SortOrder {
    MANUAL,
    ALPHABETICAL,
    FREQUENCY;

    companion object {
        fun fromName(name: String): SortOrder? = entries.firstOrNull { it.name == name }
    }
}
