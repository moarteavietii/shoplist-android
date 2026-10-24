package eu.pascariu.shoppinglist.data.local

import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.data.model.Shop
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappingTest {

    @Test
    fun `item round-trips through entity`() {
        val item = Item(
            id = 42,
            listId = 1,
            name = "Milk",
            isChecked = true,
            orderMap = mapOf("2" to "a0", "5" to "a1h"),
            quantity = 3,
            isTemporary = true,
            checkCount = 2,
            expiresAt = "2026-10-25T10:00:00.000Z",
            createdAt = "2026-10-22T10:00:00.000Z",
            visibleIn = listOf(1L, 5L),
        )

        assertEquals(item, item.toEntity().toModel())
    }

    @Test
    fun `item with empty collections round-trips`() {
        val item = Item(
            id = 7,
            listId = 1,
            name = "Bread",
            isChecked = false,
            orderMap = emptyMap(),
            quantity = 1,
            isTemporary = false,
            checkCount = 0,
            expiresAt = null,
            createdAt = "2026-10-22T10:00:00.000Z",
            visibleIn = emptyList(),
        )

        assertEquals(item, item.toEntity().toModel())
    }

    @Test
    fun `orderMap and visibleIn serialize to JSON text columns`() {
        val entity = sampleItem().toEntity()

        assertEquals("""{"2":"a0"}""", entity.orderMapJson)
        assertEquals("""[1]""", entity.visibleInJson)
    }

    @Test
    fun `shop round-trips through entity`() {
        val shop = Shop(id = 3, name = "Lidl", createdAt = "2026-10-22T10:00:00.000Z")

        assertEquals(shop, shop.toEntity().toModel())
    }
}

private fun sampleItem(): Item = Item(
    id = 42,
    listId = 1,
    name = "Milk",
    isChecked = false,
    orderMap = mapOf("2" to "a0"),
    quantity = 1,
    isTemporary = false,
    checkCount = 0,
    expiresAt = null,
    createdAt = "2026-10-22T10:00:00.000Z",
    visibleIn = listOf(1L),
)
