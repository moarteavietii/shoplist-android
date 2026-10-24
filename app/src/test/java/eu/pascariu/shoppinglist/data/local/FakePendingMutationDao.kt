package eu.pascariu.shoppinglist.data.local

import kotlinx.serialization.json.Json

open class FakePendingMutationDao : PendingMutationDao {

    private var nextId = 1L
    private val rows = mutableListOf<PendingMutation>()

    override suspend fun enqueue(mutation: PendingMutation): Long {
        val withId = if (mutation.id == 0L) mutation.copy(id = nextId++) else mutation
        rows.add(withId)
        return withId.id
    }

    override suspend fun peekOldest(): PendingMutation? = rows.minByOrNull { it.id }

    override suspend fun peekNewest(): PendingMutation? = rows.maxByOrNull { it.id }

    override suspend fun listAll(): List<PendingMutation> = rows.sortedBy { it.id }

    open override suspend fun deleteById(mutationId: Long) {
        rows.removeAll { it.id == mutationId }
    }

    override suspend fun updatePayload(mutationId: Long, payloadJson: String) {
        val index = rows.indexOfFirst { it.id == mutationId }
        if (index >= 0) rows[index] = rows[index].copy(payloadJson = payloadJson)
    }

    override suspend fun clear() {
        rows.clear()
    }
}
