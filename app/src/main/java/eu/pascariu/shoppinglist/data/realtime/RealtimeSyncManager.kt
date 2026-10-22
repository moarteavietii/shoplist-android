package eu.pascariu.shoppinglist.data.realtime

import android.util.Log
import eu.pascariu.shoppinglist.data.model.ItemEvent
import eu.pascariu.shoppinglist.data.model.ShopEvent
import eu.pascariu.shoppinglist.data.repo.ItemsRepository
import eu.pascariu.shoppinglist.data.repo.ShopsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

interface RealtimeClientContract {
    val connectSignals: kotlinx.coroutines.flow.SharedFlow<RealtimeClient.Stream>
    val itemEvents: kotlinx.coroutines.flow.SharedFlow<ItemEvent>
    val shopEvents: kotlinx.coroutines.flow.SharedFlow<ShopEvent>
    fun start()
    fun stop()
}

@Singleton
class RealtimeSyncManager @Inject constructor(
    private val realtimeClient: RealtimeClientContract,
    private val itemsRepository: ItemsRepository,
    private val shopsRepository: ShopsRepository,
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            launch { realtimeClient.connectSignals.collect { refetchAll() } }
            launch { realtimeClient.itemEvents.collect { mergeItemEvent(it) } }
            launch { realtimeClient.shopEvents.collect { mergeShopEvent(it) } }
            realtimeClient.start()
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        realtimeClient.stop()
    }

    private suspend fun refetchAll() {
        try {
            itemsRepository.refetch(listId = DEFAULT_LIST_ID)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Items refetch after reconnect failed", error)
        }
        try {
            shopsRepository.refetch()
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.w(TAG, "Shops refetch after reconnect failed", error)
        }
    }

    private suspend fun mergeItemEvent(event: ItemEvent) {
        when (event.type) {
            "item:created" -> event.item?.let { itemsRepository.applyCreated(it) }
            "item:updated" -> event.item?.let { itemsRepository.applyUpdated(it) }
            "item:deleted" -> event.itemId?.let { itemsRepository.applyDeleted(it) }
        }
    }

    private suspend fun mergeShopEvent(event: ShopEvent) {
        when (event.type) {
            "shop:created" -> event.shop?.let { shopsRepository.applyCreated(it) }
            "shop:updated" -> event.shop?.let { shopsRepository.applyUpdated(it) }
            "shop:deleted" -> event.shopId?.let { shopsRepository.applyDeleted(it) }
        }
    }

    private companion object {
        const val TAG = "RealtimeSyncManager"
        const val DEFAULT_LIST_ID = 1L
    }
}
