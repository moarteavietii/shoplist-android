package eu.pascariu.shoppinglist.data.model

import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemExpiryTest {

    private fun item(expiresAt: String?, isTemporary: Boolean = true) = Item(
        id = 1L,
        listId = 1L,
        name = "Milk",
        isChecked = true,
        orderMap = emptyMap(),
        quantity = 1,
        isTemporary = isTemporary,
        checkCount = 0,
        expiresAt = expiresAt,
        createdAt = "2026-10-22T10:00:00.000Z",
        visibleIn = listOf(1L),
    )

    @Test
    fun `item with expiry in the past is expired`() {
        val now = Instant.parse("2026-10-23T12:00:00Z")

        assertTrue(item("2026-10-23T11:00:00Z").isExpiredAt(now))
    }

    @Test
    fun `expiry exactly at now counts as expired`() {
        val now = Instant.parse("2026-10-23T12:00:00Z")

        assertTrue(item("2026-10-23T12:00:00Z").isExpiredAt(now))
    }

    @Test
    fun `item with expiry in the future is not expired`() {
        val now = Instant.parse("2026-10-23T12:00:00Z")

        assertFalse(item("2026-10-23T13:00:00Z").isExpiredAt(now))
    }

    @Test
    fun `absent expiry is not expired`() {
        val now = Instant.parse("2026-10-23T12:00:00Z")

        assertFalse(item(null).isExpiredAt(now))
    }

    @Test
    fun `malformed expiry is not expired`() {
        val now = Instant.parse("2026-10-23T12:00:00Z")

        assertFalse(item("not-a-timestamp").isExpiredAt(now))
    }

    @Test
    fun `permanent item with stale expiry is not expired`() {
        val now = Instant.parse("2026-10-23T12:00:00Z")

        assertFalse(item("2026-10-23T11:00:00Z", isTemporary = false).isExpiredAt(now))
    }
}
