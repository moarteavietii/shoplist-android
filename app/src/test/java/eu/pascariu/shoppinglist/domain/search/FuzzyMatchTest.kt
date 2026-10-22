package eu.pascariu.shoppinglist.domain.search

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzyMatchTest {

    @Test
    fun `empty query matches everything`() {
        assertTrue(fuzzyMatch("", "Milk"))
        assertTrue(fuzzyMatch("", ""))
    }

    @Test
    fun `exact match matches`() {
        assertTrue(fuzzyMatch("Milk", "Milk"))
    }

    @Test
    fun `case-insensitive in-order subsequence matches`() {
        assertTrue(fuzzyMatch("mlk", "Milk"))
        assertTrue(fuzzyMatch("MK", "Milk"))
        assertTrue(fuzzyMatch("bd", "Bread"))
    }

    @Test
    fun `out-of-order characters do not match`() {
        assertFalse(fuzzyMatch("mb", "Milk"))
        assertFalse(fuzzyMatch("klm", "Milk"))
    }

    @Test
    fun `query longer than name does not match`() {
        assertFalse(fuzzyMatch("Milks", "Milk"))
    }

    @Test
    fun `contiguous substring matches`() {
        assertTrue(fuzzyMatch("bre", "Bread"))
    }

    @Test
    fun `whitespace in query is ignored`() {
        assertTrue(fuzzyMatch("m lk", "Milk"))
    }

    @Test
    fun `repeated characters must appear in name`() {
        assertTrue(fuzzyMatch("oo", "Noodles"))
        assertFalse(fuzzyMatch("oo", "Milk"))
    }
}
