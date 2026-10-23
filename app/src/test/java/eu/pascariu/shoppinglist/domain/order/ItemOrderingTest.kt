package eu.pascariu.shoppinglist.domain.order

import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.prefs.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemOrderingTest {

    private fun item(
        id: Long,
        name: String,
        checkCount: Int = 0,
        orderKey: String? = null,
    ) = Item(
        id = id,
        listId = 1,
        name = name,
        isChecked = false,
        orderMap = orderKey?.let { mapOf("1" to it) } ?: emptyMap(),
        quantity = 1,
        isTemporary = false,
        checkCount = checkCount,
        expiresAt = null,
        createdAt = "2026-10-22T09:00:00Z",
        visibleIn = listOf(1),
    )

    @Test
    fun `manual order sorts by ascending fractional key`() {
        val items = listOf(
            item(id = 3, name = "Third", orderKey = "a2"),
            item(id = 1, name = "First", orderKey = "a0"),
            item(id = 2, name = "Second", orderKey = "a1"),
        )

        val ordered = orderItems(items, viewId = 1L, order = SortOrder.MANUAL)

        assertEquals(listOf("First", "Second", "Third"), ordered.map { it.name })
    }

    @Test
    fun `manual order breaks missing-key ties by item id`() {
        val items = listOf(
            item(id = 2, name = "Second"),
            item(id = 1, name = "First"),
        )

        val ordered = orderItems(items, viewId = 1L, order = SortOrder.MANUAL)

        assertEquals(listOf("First", "Second"), ordered.map { it.name })
    }

    @Test
    fun `alphabetical order is case-insensitive`() {
        val items = listOf(
            item(id = 1, name = "banana"),
            item(id = 2, name = "Apple"),
            item(id = 3, name = "cherry"),
        )

        val ordered = orderItems(items, viewId = 1L, order = SortOrder.ALPHABETICAL)

        assertEquals(listOf("Apple", "banana", "cherry"), ordered.map { it.name })
    }

    @Test
    fun `frequency order puts higher check count first`() {
        val items = listOf(
            item(id = 1, name = "Bread", checkCount = 2),
            item(id = 2, name = "Milk", checkCount = 5),
        )

        val ordered = orderItems(items, viewId = 1L, order = SortOrder.FREQUENCY)

        assertEquals(listOf("Milk", "Bread"), ordered.map { it.name })
    }

    @Test
    fun `frequency tie-break is case-insensitive alphabetical`() {
        val items = listOf(
            item(id = 1, name = "Banana", checkCount = 3),
            item(id = 2, name = "apple", checkCount = 3),
        )

        val ordered = orderItems(items, viewId = 1L, order = SortOrder.FREQUENCY)

        assertEquals(listOf("apple", "Banana"), ordered.map { it.name })
    }

    @Test
    fun `switching back to manual restores the exact stored order`() {
        val manualItems = listOf(
            item(id = 1, name = "Milk", checkCount = 5, orderKey = "a0"),
            item(id = 2, name = "Bread", checkCount = 2, orderKey = "a1"),
            item(id = 3, name = "Eggs", checkCount = 9, orderKey = "a2"),
        )

        val viaFrequency = orderItems(manualItems, viewId = 1L, order = SortOrder.FREQUENCY)
        val viaAlphabetical = orderItems(manualItems, viewId = 1L, order = SortOrder.ALPHABETICAL)
        val restored = orderItems(manualItems, viewId = 1L, order = SortOrder.MANUAL)

        assertEquals(listOf("Eggs", "Milk", "Bread"), viaFrequency.map { it.name })
        assertEquals(listOf("Bread", "Eggs", "Milk"), viaAlphabetical.map { it.name })
        assertEquals(manualItems, restored)
    }

    @Test
    fun `ordering never mutates the input list`() {
        val items = listOf(
            item(id = 2, name = "B", orderKey = "a1"),
            item(id = 1, name = "A", orderKey = "a0"),
        )

        orderItems(items, viewId = 1L, order = SortOrder.ALPHABETICAL)

        assertEquals(listOf("B", "A"), items.map { it.name })
    }
}
