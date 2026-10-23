package eu.pascariu.shoppinglist.data.realtime

import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.ItemEvent
import eu.pascariu.shoppinglist.data.model.Shop
import org.junit.Assert.assertEquals
import org.junit.Test

class ChangeToastDerivationTest {

    @Test
    fun `checked event produces checked label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:updated", listId = 1, item = sampleItem().copy(isChecked = true)),
            previousItem = sampleItem(),
            shops = sampleShops(),
        )

        assertEquals(listOf("checked: Milk"), labels.map { it.text })
    }

    @Test
    fun `unchecked event produces unchecked label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:updated", listId = 1, item = sampleItem(isChecked = true).copy(isChecked = false)),
            previousItem = sampleItem(isChecked = true),
            shops = sampleShops(),
        )

        assertEquals(listOf("unchecked: Milk"), labels.map { it.text })
    }

    @Test
    fun `created event produces new item label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:created", listId = 1, item = sampleItem()),
            previousItem = null,
            shops = sampleShops(),
        )

        assertEquals(listOf("new item: Milk"), labels.map { it.text })
    }

    @Test
    fun `deleted event uses the pre-delete name from the cache`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:deleted", listId = 1, itemId = 42),
            previousItem = sampleItem(),
            shops = sampleShops(),
        )

        assertEquals(listOf("deleted: Milk"), labels.map { it.text })
    }

    @Test
    fun `deleted event with no cached item produces no label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:deleted", listId = 1, itemId = 42),
            previousItem = null,
            shops = sampleShops(),
        )

        assertEquals(emptyList<String>(), labels.map { it.text })
    }

    @Test
    fun `updated event without item payload produces no label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:updated", listId = 1, item = null),
            previousItem = sampleItem(),
            shops = sampleShops(),
        )

        assertEquals(emptyList<String>(), labels.map { it.text })
    }

    @Test
    fun `updated event for uncached item produces no label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:updated", listId = 1, item = sampleItem()),
            previousItem = null,
            shops = sampleShops(),
        )

        assertEquals(emptyList<String>(), labels.map { it.text })
    }

    @Test
    fun `renamed event produces transition label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:updated", listId = 1, item = sampleItem().copy(name = "Bread rolls")),
            previousItem = sampleItem(),
            shops = sampleShops(),
        )

        assertEquals(listOf("Milk → Bread rolls"), labels.map { it.text })
    }

    @Test
    fun `quantity change produces quantity label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:updated", listId = 1, item = sampleItem().copy(quantity = 3)),
            previousItem = sampleItem(quantity = 2),
            shops = sampleShops(),
        )

        assertEquals(listOf("quantity: 2 → 3 Milk"), labels.map { it.text })
    }

    @Test
    fun `visibility removal produces removed-from-shop label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:updated", listId = 1, item = sampleItem().copy(visibleIn = listOf(1L))),
            previousItem = sampleItem().copy(visibleIn = listOf(1L, 3L)),
            shops = sampleShops(),
        )

        assertEquals(listOf("Milk removed from Carrefour"), labels.map { it.text })
    }

    @Test
    fun `visibility addition produces added-to label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:updated", listId = 1, item = sampleItem().copy(visibleIn = listOf(1L, 2L))),
            previousItem = sampleItem(),
            shops = sampleShops(),
        )

        assertEquals(listOf("Milk added to Auchan"), labels.map { it.text })
    }

    @Test
    fun `visibility change to unknown shop produces no label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:updated", listId = 1, item = sampleItem().copy(visibleIn = listOf(1L, 99L))),
            previousItem = sampleItem(),
            shops = sampleShops(),
        )

        assertEquals(emptyList<String>(), labels.map { it.text })
    }

    @Test
    fun `reorder-only update produces no label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:updated", listId = 1, item = sampleItem().copy(orderMap = mapOf("2" to "z9"))),
            previousItem = sampleItem(),
            shops = sampleShops(),
        )

        assertEquals(emptyList<String>(), labels.map { it.text })
    }

    @Test
    fun `no-op update produces no label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:updated", listId = 1, item = sampleItem()),
            previousItem = sampleItem(),
            shops = sampleShops(),
        )

        assertEquals(emptyList<String>(), labels.map { it.text })
    }

    @Test
    fun `multi-field update derives labels in field order`() {
        val updated = sampleItem()
            .copy(isChecked = true, name = "Bread rolls", quantity = 3, visibleIn = listOf(1L, 2L))
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:updated", listId = 1, item = updated),
            previousItem = sampleItem(),
            shops = sampleShops(),
        )

        assertEquals(
            listOf(
                "checked: Bread rolls",
                "Milk → Bread rolls",
                "quantity: 1 → 3 Bread rolls",
                "Bread rolls added to Auchan",
            ),
            labels.map { it.text },
        )
    }

    @Test
    fun `unknown event type produces no label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:something-else", listId = 1, item = sampleItem()),
            previousItem = sampleItem(),
            shops = sampleShops(),
        )

        assertEquals(emptyList<String>(), labels.map { it.text })
    }

    @Test
    fun `duplicate created event produces no label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:created", listId = 1, item = sampleItem()),
            previousItem = sampleItem(),
            shops = sampleShops(),
        )

        assertEquals(emptyList<String>(), labels.map { it.text })
    }

    @Test
    fun `created event without item payload produces no label`() {
        val labels = deriveChangeToasts(
            event = ItemEvent(type = "item:created", listId = 1),
            previousItem = null,
            shops = sampleShops(),
        )

        assertEquals(emptyList<String>(), labels.map { it.text })
    }
}

private fun sampleItem(
    name: String = "Milk",
    isChecked: Boolean = false,
    quantity: Int = 1,
) = Item(
    id = 42,
    listId = 1,
    name = name,
    isChecked = isChecked,
    orderMap = mapOf("2" to "a0"),
    quantity = quantity,
    isTemporary = false,
    checkCount = 0,
    expiresAt = null,
    createdAt = "2026-10-22T10:00:00.000Z",
    visibleIn = listOf(1L),
)

private fun sampleShops() = listOf(
    Shop(id = 1, name = "Global", createdAt = "2026-10-22T10:00:00.000Z"),
    Shop(id = 2, name = "Auchan", createdAt = "2026-10-22T10:00:00.000Z"),
    Shop(id = 3, name = "Carrefour", createdAt = "2026-10-22T10:00:00.000Z"),
)
