package eu.pascariu.shoppinglist.data.undo

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import eu.pascariu.shoppinglist.di.ApplicationScope
import eu.pascariu.shoppinglist.di.IoDispatcher
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Duration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Singleton
class UndoHistoryStore(
    private val dataStore: DataStore<Preferences>,
    private val json: Json,
    storeScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    private val pruneInterval: Duration,
    private val clock: () -> Long,
) {

    @Inject
    constructor(
        dataStore: DataStore<Preferences>,
        json: Json,
        @ApplicationScope storeScope: CoroutineScope,
        @IoDispatcher ioDispatcher: CoroutineDispatcher,
    ) : this(dataStore, json, storeScope, ioDispatcher, UndoConfig.PRUNE_INTERVAL, System::currentTimeMillis)

    private val history = MutableStateFlow<List<UndoEntry>>(emptyList())
    val entries: StateFlow<List<UndoEntry>> = history.asStateFlow()

    private val mutationMutex = Mutex()
    @Volatile
    private var restoreJob: Job? = null

    init {
        restoreJob = storeScope.launch { restore() }
        storeScope.launch(ioDispatcher) {
            while (true) {
                delay(pruneInterval.toMillis())
                prune()
            }
        }
    }

    suspend fun record(entry: UndoEntry) = mutationMutex.withLock {
        awaitRestore()
        persist(listOf(entry) + pruneExpired(history.value).take(UndoConfig.MAX_ENTRIES - 1))
    }

    suspend fun prune() = mutationMutex.withLock {
        awaitRestore()
        val pruned = pruneExpired(history.value)
        if (pruned != history.value) persist(pruned)
    }

    suspend fun removeTop(): UndoEntry? = mutationMutex.withLock {
        awaitRestore()
        val current = pruneExpired(history.value)
        val top = current.firstOrNull() ?: return null
        persist(current.drop(1))
        top
    }

    suspend fun restoreTop(entry: UndoEntry) = mutationMutex.withLock {
        awaitRestore()
        persist(listOf(entry) + pruneExpired(history.value))
    }

    private suspend fun awaitRestore() {
        restoreJob?.join()
    }

    private fun pruneExpired(entries: List<UndoEntry>): List<UndoEntry> {
        val cutoff = clock() - UndoConfig.TTL.toMillis()
        return entries.filter { it.at > cutoff }
    }

    private suspend fun persist(updated: List<UndoEntry>) {
        dataStore.edit { prefs ->
            prefs[KEY_HISTORY] = json.encodeToString(ListSerializer(UndoEntry.serializer()), updated)
        }
        history.value = updated
    }

    private suspend fun restore() {
        val raw = try {
            dataStore.data.first()[KEY_HISTORY]
        } catch (e: CorruptionException) {
            null
        } catch (e: IOException) {
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
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
