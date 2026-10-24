package eu.pascariu.shoppinglist.data.local

import eu.pascariu.shoppinglist.data.connectivity.BackendConnectivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MutationQueueTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun queue(
        dao: FakePendingMutationDao = FakePendingMutationDao(),
        connectivity: BackendConnectivity = BackendConnectivity.REACHABLE,
    ): MutationQueue = MutationQueue(
        dao = dao,
        connectivity = MutableStateFlow(connectivity),
        json = json,
    )

    @Test
    fun `enqueue preserves order across separate mutations`() = runTest {
        val dao = FakePendingMutationDao()
        val queue = queue(dao)

        queue.enqueue(MutationPayload.ItemDelete(itemId = 1))
        queue.enqueue(MutationPayload.ItemCheck(itemId = 2, isChecked = true))
        queue.enqueue(MutationPayload.ShopDelete(shopId = 3))

        val all = queue.listAll()
        assertEquals(3, all.size)
        assertEquals(MutationKind.ITEM_DELETE, all[0].kind)
        assertEquals(MutationKind.ITEM_CHECK, all[1].kind)
        assertEquals(MutationKind.SHOP_DELETE, all[2].kind)
    }

    @Test
    fun `repeated checks on the same item coalesce to the latest value`() = runTest {
        val queue = queue()

        queue.enqueueCheckCoalesced(MutationPayload.ItemCheck(itemId = 42, isChecked = true))
        queue.enqueueCheckCoalesced(MutationPayload.ItemCheck(itemId = 42, isChecked = false))

        val all = queue.listAll()
        assertEquals(1, all.size)
        val payload = json.decodeFromString<MutationPayload.ItemCheck>(all.single().payloadJson)
        assertEquals(false, payload.isChecked)
    }

    @Test
    fun `checks on different items do not coalesce`() = runTest {
        val queue = queue()

        queue.enqueueCheckCoalesced(MutationPayload.ItemCheck(itemId = 1, isChecked = true))
        queue.enqueueCheckCoalesced(MutationPayload.ItemCheck(itemId = 2, isChecked = true))

        assertEquals(2, queue.listAll().size)
    }

    @Test
    fun `check coalescing only applies to the newest entry`() = runTest {
        val queue = queue()

        queue.enqueue(MutationPayload.ItemCheck(itemId = 1, isChecked = true))
        queue.enqueue(MutationPayload.ItemDelete(itemId = 9))
        queue.enqueueCheckCoalesced(MutationPayload.ItemCheck(itemId = 1, isChecked = false))

        val all = queue.listAll()
        assertEquals(3, all.size)
    }

    @Test
    fun `payload round-trips through json`() = runTest {
        val queue = queue()
        val payload = MutationPayload.ItemCreate(
            listId = 1,
            placeholderId = -1,
            request = eu.pascariu.shoppinglist.data.model.CreateItemRequest(name = "Milk"),
        )

        queue.enqueue(payload)

        val stored = queue.peekOldest()
        val decoded = json.decodeFromString<MutationPayload.ItemCreate>(requireNotNull(stored).payloadJson)
        assertEquals(payload, decoded)
    }

    @Test
    fun `empty queue peeks nothing`() = runTest {
        val queue = queue()

        assertNull(queue.peekOldest())
    }
}
