package eu.pascariu.shoppinglist.domain.search

import eu.pascariu.shoppinglist.data.model.Shop
import org.junit.Assert.assertEquals
import org.junit.Test

class ViewOrderingTest {

    private fun shop(id: Long, name: String) = Shop(id = id, name = name, createdAt = "2026-10-22T09:00:00Z")

    @Test
    fun `global comes first then alphabetical`() {
        val ordered = orderViewsGlobalFirst(
            listOf(
                shop(1, "Carrefour"),
                shop(2, "Global"),
                shop(3, "Auchan"),
            ),
        )

        assertEquals(listOf("Global", "Auchan", "Carrefour"), ordered.map { it.name })
    }

    @Test
    fun `alphabetical order is case-insensitive`() {
        val ordered = orderViewsGlobalFirst(
            listOf(
                shop(1, "banana"),
                shop(2, "Apple"),
                shop(3, "cherry"),
            ),
        )

        assertEquals(listOf("Apple", "banana", "cherry"), ordered.map { it.name })
    }

    @Test
    fun `empty list stays empty`() {
        assertEquals(emptyList<Shop>(), orderViewsGlobalFirst(emptyList()))
    }

    @Test
    fun `already ordered list is stable`() {
        val shops = listOf(shop(1, "Global"), shop(2, "Auchan"))
        assertEquals(shops, orderViewsGlobalFirst(shops))
    }
}
