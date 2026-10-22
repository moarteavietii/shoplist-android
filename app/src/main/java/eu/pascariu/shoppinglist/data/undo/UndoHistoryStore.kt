package eu.pascariu.shoppinglist.data.undo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.pascariu.shoppinglist.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Singleton
class UndoHistoryStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    @ApplicationScope storeScope: CoroutineScope,
) {

    private val history = MutableStateFlow<List<UndoEntry>>(emptyList())
    val entries: StateFlow<List<UndoEntry>> = history.asStateFlow()

    private var restoreJob: Job? = null

    init {
        restoreJob = storeScope.launch { restore() }
    }

    suspend fun record(entry: UndoEntry) {
        awaitRestore()
        persist(listOf(entry) + pruneExpired(history.value).take(UndoConfig.MAX_ENTRIES - 1))
    }

    suspend fun prune() {
        awaitRestore()
        val pruned = pruneExpired(history.value)
        if (pruned != history.value) persist(pruned)
    }

    suspend fun removeTop(): UndoEntry? {
        awaitRestore()
        val current = pruneExpired(history.value)
        val top = current.firstOrNull() ?: return null
        persist(current.drop(1))
        return top
    }

    suspend fun restoreTop(entry: UndoEntry) {
        awaitRestore()
        persist(listOf(entry) + pruneExpired(history.value))
    }

    private suspend fun awaitRestore() {
        restoreJob?.join()
    }

    private fun pruneExpired(entries: List<UndoEntry>): List<UndoEntry> {
        val cutoff = System.currentTimeMillis() - UndoConfig.TTL.toMillis()
        return entries.filter { it.at > cutoff }
    }

    private suspend fun persist(updated: List<UndoEntry>) {
        history.value = updated
        dataStore.edit { prefs ->
            prefs[KEY_HISTORY] = json.encodeToString(ListSerializer(UndoEntry.serializer()), updated)
        }
    }

    private suspend fun restore() {
        val raw = try {
            dataStore.data.first()[KEY_HISTORY]
        } catch (_: Exception) {
            null
        }
        val restored = raw?.let {
            try {
                json.decodeFromString(ListSerializer(UndoEntry.serializer()), it)
            } catch (_: Exception) {
                null
            }
        }.orEmpty()
        history.value = pruneExpired(restored)
    }

    private companion object {
        val KEY_HISTORY = stringPreferencesKey("undo_history")
    }
}
