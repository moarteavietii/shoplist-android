package eu.pascariu.shoppinglist.domain.order

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FractionalIndexingTest {

    @Test
    fun `initial key for empty view is a0`() {
        assertEquals("a0", generateKeyBetween(null, null))
    }

    @Test
    fun `key between two neighbors sorts strictly between them`() {
        val key = generateKeyBetween("a0", "a1")

        assertTrue("a0" < key)
        assertTrue(key < "a1")
    }

    @Test
    fun `append key after highest key sorts after it`() {
        val key = generateKeyBetween("a0z", null)

        assertTrue("a0z" < key)
    }

    @Test
    fun `prepend key before lowest key sorts before it`() {
        val key = generateKeyBetween(null, "a0")

        assertTrue(key < "a0")
    }

    @Test
    fun `byte-wise comparison matches server ordering`() {
        val samples = listOf(
            "a0" to "a1",
            "a0" to "aZ",
            "a0" to "b00",
            "Zz" to "a0",
            "a0" to "a0a",
            "a1" to "a1a",
        )
        for ((a, b) in samples) {
            val key = generateKeyBetween(a, b)
            assertTrue("expected $a < $key < $b", a < key && key < b)
        }
    }

    @Test
    fun `neighbors passed in either order produce the same key`() {
        assertEquals(generateKeyBetween("a0", "a1"), generateKeyBetween("a1", "a0"))
    }

    @Test
    fun `keys between consecutive integer parts stay short`() {
        assertEquals("a1", generateKeyBetween("a0", "b00"))
    }

    @Test
    fun `append after a0 gives a1`() {
        assertEquals("a1", generateKeyBetween("a0", null))
    }

    @Test
    fun `insert before a0 gives a minus key`() {
        assertEquals("Zz", generateKeyBetween(null, "a0"))
    }

    @Test
    fun `midpoint of dense fractional keys generates longer keys`() {
        val key = generateKeyBetween("a0z", "a1")

        assertTrue("a0z" < key)
        assertTrue(key < "a1")
    }

    @Test
    fun `equal neighbors throw`() {
        var thrown = false
        try {
            generateKeyBetween("a0", "a0")
        } catch (error: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
    }

    @Test
    fun `trailing zero keys are rejected`() {
        var thrown = false
        try {
            generateKeyBetween("a0", "aZ0")
        } catch (error: IllegalArgumentException) {
            thrown = true
        }
        assertTrue(thrown)
    }
}
