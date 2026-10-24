package eu.pascariu.shoppinglist.data.local

import android.util.Log
import eu.pascariu.shoppinglist.data.api.ItemsApi
import eu.pascariu.shoppinglist.data.api.ShopsApi
import eu.pascariu.shoppinglist.data.model.CreateItemRequest
import eu.pascariu.shoppinglist.data.model.CreateShopRequest
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.RenameShopRequest
import eu.pascariu.shoppinglist.data.model.Shop
import eu.pascariu.shoppinglist.data.model.UpdateItemRequest
import eu.pascariu.shoppinglist.data.repo.ItemsRepository
import eu.pascariu.shoppinglist.data.repo.ShopsRepository
import eu.pascariu.shoppinglist.di.IoDispatcher
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import retrofit2.HttpException

@Singleton
class MutationDrainer @Inject constructor(
    private val queue: MutationQueue,
    private val itemsApi: ItemsApi,
    private val shopsApi: ShopsApi,
    private val itemsRepository: ItemsRepository,
    private val shopsRepository: ShopsRepository,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun drainAll() = withContext(ioDispatcher) {
        while (true) {
            val mutation = queue.peekOldest() ?: return@withContext
            val payload = decode(mutation.payloadJson)
            if (payload == null) {
                queue.deleteById(mutation.id)
                continue
            }
            val outcome = drainOne(payload)
            when (outcome) {
                ReplayOutcome.SUCCESS -> queue.deleteById(mutation.id)
                ReplayOutcome.DROPPED -> queue.deleteById(mutation.id)
                ReplayOutcome.NETWORK_ERROR -> return@withContext
            }
        }
    }

    private suspend fun drainOne(payload: MutationPayload): ReplayOutcome {
        return try {
            when (payload) {
                is MutationPayload.ItemCreate -> {
                    val created = itemsApi.createItem(payload.listId, payload.request)
                    itemsRepository.remapPlaceholder(payload.placeholderId, created)
                    remapFollowUps(payload.placeholderId, created.id)
                }
                is MutationPayload.ItemUpdate -> {
                    val updated = itemsApi.updateItem(payload.itemId, payload.request)
                    itemsRepository.applyUpdated(updated)
                }
                is MutationPayload.ItemDelete -> {
                    val response = itemsApi.deleteItem(payload.itemId)
                    when {
                        response.isSuccessful -> itemsRepository.applyDeleted(payload.itemId)
                        response.code() == 404 -> Unit
                        else -> return ReplayOutcome.NETWORK_ERROR
                    }
                }
                is MutationPayload.ItemCheck -> {
                    val updated = itemsApi.updateItem(
                        payload.itemId,
                        UpdateItemRequest(isChecked = payload.isChecked, isUndo = payload.isUndo),
                    )
                    itemsRepository.applyUpdated(updated)
                }
                is MutationPayload.ShopCreate -> {
                    val created = shopsApi.createShop(payload.request)
                    shopsRepository.remapPlaceholder(payload.placeholderId, created)
                }
                is MutationPayload.ShopRename -> {
                    val renamed = shopsApi.renameShop(payload.shopId, payload.request)
                    shopsRepository.applyUpdated(renamed)
                }
                is MutationPayload.ShopDelete -> {
                    val response = shopsApi.deleteShop(payload.shopId)
                    when {
                        response.isSuccessful || response.code() == 404 ->
                            shopsRepository.applyDeleted(payload.shopId)
                        else -> return ReplayOutcome.NETWORK_ERROR
                    }
                }
            }
            ReplayOutcome.SUCCESS
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            if (error is retrofit2.HttpException) {
                when (error.code()) {
                    404, 409 -> ReplayOutcome.DROPPED
                    else -> ReplayOutcome.NETWORK_ERROR
                }
            } else {
                ReplayOutcome.NETWORK_ERROR
            }
        }
    }

    private suspend fun remapFollowUps(oldItemId: Long, newItemId: Long) {
        queue.listAll().forEach { entry ->
            when (val payload = decode(entry.payloadJson)) {
                is MutationPayload.ItemUpdate -> if (payload.itemId == oldItemId) {
                    queue.updatePayload(entry.id, payload.copy(itemId = newItemId))
                }
                is MutationPayload.ItemCheck -> if (payload.itemId == oldItemId) {
                    queue.updatePayload(entry.id, payload.copy(itemId = newItemId))
                }
                is MutationPayload.ItemDelete -> if (payload.itemId == oldItemId) {
                    queue.updatePayload(entry.id, payload.copy(itemId = newItemId))
                }
                else -> Unit
            }
        }
    }

    private fun decode(payloadJson: String): MutationPayload? = try {
        json.decodeFromString(MutationPayload.serializer(), payloadJson)
    } catch (error: Exception) {
        Log.w(TAG, "Dropping corrupt queued mutation", error)
        null
    }

    private companion object {
        const val TAG = "MutationDrainer"
    }
}

private enum class ReplayOutcome {
    SUCCESS,
    DROPPED,
    NETWORK_ERROR,
}
