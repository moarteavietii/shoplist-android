package eu.pascariu.shoppinglist.domain.order

import org.junit.Assert.assertEquals
import org.junit.Test

class DropKeyTest {

    @Test
    fun `drop between neighbors generates midpoint key`() {
        assertEquals("a1", dropKeyForIndex(orderedKeys = listOf("a0", "b00"), dropIndex = 1))
    }

    @Test
    fun `drop at start boundary generates key before first`() {
        assertEquals("Zz", dropKeyForIndex(orderedKeys = listOf("a0", "a1"), dropIndex = 0))
    }

    @Test
    fun `drop at end boundary generates key after last`() {
        assertEquals("a1", dropKeyForIndex(orderedKeys = listOf("a0", "a0z"), dropIndex = 2))
    }

    @Test
    fun `drop into empty list generates first key`() {
        assertEquals("a0", dropKeyForIndex(orderedKeys = emptyList(), dropIndex = 0))
    }
}
