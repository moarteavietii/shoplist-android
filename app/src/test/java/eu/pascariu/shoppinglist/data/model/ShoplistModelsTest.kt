package eu.pascariu.shoppinglist.data.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoplistModelsTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun `item payload deserializes`() {
        val payload = """
            {
              "id": 42,
              "listId": 1,
              "name": "Milk",
              "isChecked": false,
              "orderMap": { "2": "a0", "3": "b1" },
              "quantity": 2,
              "isTemporary": false,
              "checkCount": 3,
              "expiresAt": null,
              "createdAt": "2026-10-22T10:00:00.000Z",
              "visibleIn": [1, 2]
            }
        """.trimIndent()

        val item = json.decodeFromString<Item>(payload)

        assertEquals(42L, item.id)
        assertEquals(1L, item.listId)
        assertEquals("Milk", item.name)
        assertEquals(false, item.isChecked)
        assertEquals(mapOf("2" to "a0", "3" to "b1"), item.orderMap)
        assertEquals(2, item.quantity)
        assertEquals(false, item.isTemporary)
        assertEquals(3, item.checkCount)
        assertNull(item.expiresAt)
        assertEquals("2026-10-22T10:00:00.000Z", item.createdAt)
        assertEquals(listOf(1L, 2L), item.visibleIn)
    }

    @Test
    fun `item payload with unknown fields deserializes`() {
        val payload = """
            {
              "id": 7,
              "listId": 1,
              "name": "Bread",
              "isChecked": true,
              "orderMap": {},
              "quantity": 1,
              "isTemporary": true,
              "checkCount": 0,
              "expiresAt": "2026-10-23T10:00:00.000Z",
              "createdAt": "2026-10-22T09:00:00.000Z",
              "visibleIn": [2],
              "someFutureField": "ignored"
            }
        """.trimIndent()

        val item = json.decodeFromString<Item>(payload)

        assertEquals("Bread", item.name)
        assertEquals(true, item.isChecked)
        assertEquals("2026-10-23T10:00:00.000Z", item.expiresAt)
        assertTrue(item.orderMap.isEmpty())
    }

    @Test
    fun `shop payload deserializes`() {
        val payload = """
            {
              "id": 2,
              "name": "Supermarket",
              "createdAt": "2026-10-22T10:00:00.000Z"
            }
        """.trimIndent()

        val shop = json.decodeFromString<Shop>(payload)

        assertEquals(2L, shop.id)
        assertEquals("Supermarket", shop.name)
        assertEquals("2026-10-22T10:00:00.000Z", shop.createdAt)
    }

    @Test
    fun `error payload deserializes`() {
        val payload = """{ "error": "name must be 1-200 characters" }"""

        val error = json.decodeFromString<ErrorPayload>(payload)

        assertEquals("name must be 1-200 characters", error.error)
    }

    @Test
    fun `create item request omits null fields`() {
        val request = CreateItemRequest(name = "Milk", quantity = 2)

        val encoded = json.encodeToString(CreateItemRequest.serializer(), request)

        assertEquals("""{"name":"Milk","quantity":2}""", encoded)
    }

    @Test
    fun `update item request omits null fields`() {
        val request = UpdateItemRequest(isChecked = true)

        val encoded = json.encodeToString(UpdateItemRequest.serializer(), request)

        assertEquals("""{"isChecked":true}""", encoded)
    }
}
