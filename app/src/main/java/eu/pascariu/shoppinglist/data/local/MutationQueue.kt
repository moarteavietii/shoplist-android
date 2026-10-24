package eu.pascariu.shoppinglist.data.local

import eu.pascariu.shoppinglist.data.connectivity.BackendConnectivity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

@Singleton
class MutationQueue @Inject constructor(
    private val dao: PendingMutationDao,
    private val connectivity: @JvmSuppressWildcards StateFlow<BackendConnectivity>,
    private val json: Json,
) {

    val isOffline: Boolean
        get() = connectivity.value == BackendConnectivity.UNREACHABLE

    suspend fun enqueue(payload: MutationPayload) {
        dao.enqueue(
            PendingMutation(
                kind = payload.kind,
                payloadJson = payload.toJson(json),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun enqueueCheckCoalesced(payload: MutationPayload.ItemCheck) {
        val newest = dao.peekNewest()
        if (newest != null && newest.kind == MutationKind.ITEM_CHECK) {
            val existing = json.decodeFromString<MutationPayload.ItemCheck>(newest.payloadJson)
            if (existing.itemId == payload.itemId) {
                dao.updatePayload(newest.id, payload.toJson(json))
                return
            }
        }
        enqueue(payload)
    }

    suspend fun peekOldest(): PendingMutation? = dao.peekOldest()

    suspend fun listAll(): List<PendingMutation> = dao.listAll()

    suspend fun deleteById(mutationId: Long) = dao.deleteById(mutationId)

    suspend fun updatePayload(mutationId: Long, payload: MutationPayload) {
        dao.updatePayload(mutationId, payload.toJson(json))
    }
}
