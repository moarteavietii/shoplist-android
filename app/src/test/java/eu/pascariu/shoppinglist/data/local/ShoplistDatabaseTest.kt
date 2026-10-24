package eu.pascariu.shoppinglist.data.local

import android.content.Context
import androidx.room.Room
import eu.pascariu.shoppinglist.data.model.Item
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ShoplistDatabaseTest {

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var database: ShoplistDatabase

    private fun newDatabase(): ShoplistDatabase = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication() as Context,
        ShoplistDatabase::class.java,
    ).allowMainThreadQueries().build()

    @Before
    fun setUp() {
        database = newDatabase()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun itemEntity(id: Long, name: String = "Milk") = Item(
        id = id,
        listId = 1,
        name = name,
        isChecked = false,
        orderMap = mapOf("2" to "a0"),
        quantity = 1,
        isTemporary = false,
        checkCount = 0,
        expiresAt = null,
        createdAt = "2026-10-24T10:00:00.000Z",
        visibleIn = listOf(1L),
    ).toEntity()

    private fun mutation(kind: MutationKind, payloadJson: String = "{}") = PendingMutation(
        kind = kind,
        payloadJson = payloadJson,
        createdAt = 1000L,
    )

    @Test
    fun `replaceAll swaps the full item set in one transaction`() = runTest {
        val dao = database.itemDao()
        dao.upsertAll(listOf(itemEntity(1), itemEntity(2)))

        dao.replaceAll(listOf(itemEntity(2, "Oat milk"), itemEntity(3, "Bread")))

        assertEquals(listOf(2L, 3L), dao.observeAll().first().map { it.id })
    }

    @Test
    fun `replaceAll with an empty list clears the table`() = runTest {
        val dao = database.itemDao()
        dao.upsertAll(listOf(itemEntity(1)))

        dao.replaceAll(emptyList())

        assertEquals(emptyList<ItemEntity>(), dao.observeAll().first())
    }

    @Test
    fun `peekOldest returns the first enqueued mutation and peekNewest the last`() = runTest {
        val dao = database.pendingMutationDao()
        dao.enqueue(mutation(MutationKind.ITEM_CHECK, """{"itemId":1,"isChecked":true}"""))
        dao.enqueue(mutation(MutationKind.ITEM_UPDATE, """{"itemId":1,"name":"Oat milk"}"""))
        dao.enqueue(mutation(MutationKind.ITEM_DELETE, """{"itemId":2}"""))

        assertEquals(MutationKind.ITEM_CHECK, dao.peekOldest()?.kind)
        assertEquals(MutationKind.ITEM_DELETE, dao.peekNewest()?.kind)
    }

    @Test
    fun `peekOldest on an empty queue returns null`() = runTest {
        assertNull(database.pendingMutationDao().peekOldest())
    }

    @Test
    fun `mutation kind survives the database round-trip`() = runTest {
        val dao = database.pendingMutationDao()
        MutationKind.entries.forEach { kind ->
            dao.enqueue(mutation(kind))
        }

        val stored = dao.listAll()
        assertEquals(MutationKind.entries, stored.map { it.kind })
    }

    @Test
    fun `queue survives closing and reopening the database`() = runTest {
        val context = RuntimeEnvironment.getApplication() as Context
        val dbFile = context.getDatabasePath("persistence-test.db")
        dbFile.parentFile?.mkdirs()
        val fileDatabase = Room.databaseBuilder(context, ShoplistDatabase::class.java, "persistence-test.db")
            .allowMainThreadQueries().build()
        val firstId = fileDatabase.pendingMutationDao()
            .enqueue(mutation(MutationKind.ITEM_CHECK, """{"itemId":1,"isChecked":true}"""))
        val secondId = fileDatabase.pendingMutationDao()
            .enqueue(mutation(MutationKind.SHOP_CREATE, """{"request":{"name":"Lidl"}}"""))
        fileDatabase.close()

        val reopened = Room.databaseBuilder(context, ShoplistDatabase::class.java, "persistence-test.db")
            .allowMainThreadQueries().build()
        try {
            val restored = reopened.pendingMutationDao().listAll()
            assertEquals(2, restored.size)
            assertEquals(firstId, restored[0].id)
            assertEquals(secondId, restored[1].id)
            assertEquals(MutationKind.ITEM_CHECK, restored[0].kind)
            assertEquals("""{"request":{"name":"Lidl"}}""", restored[1].payloadJson)
        } finally {
            reopened.close()
            dbFile.delete()
        }
    }

    @Test
    fun `deleteById removes only the targeted mutation`() = runTest {
        val dao = database.pendingMutationDao()
        val firstId = dao.enqueue(mutation(MutationKind.ITEM_CHECK))
        val secondId = dao.enqueue(mutation(MutationKind.ITEM_UPDATE))

        dao.deleteById(firstId)

        val remaining = dao.listAll()
        assertEquals(listOf(secondId), remaining.map { it.id })
    }

    @Test
    fun `updatePayload rewrites the stored payload`() = runTest {
        val dao = database.pendingMutationDao()
        val id = dao.enqueue(mutation(MutationKind.ITEM_CHECK, """{"isChecked":false}"""))

        dao.updatePayload(id, """{"isChecked":true}""")

        assertEquals("""{"isChecked":true}""", dao.peekOldest()?.payloadJson)
    }
}
