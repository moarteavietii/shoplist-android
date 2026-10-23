package eu.pascariu.shoppinglist.ui.list

import eu.pascariu.shoppinglist.data.model.Shop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ItemModalUiStateTest {

    private val shops = listOf(Shop(id = 1L, name = "Global", createdAt = "2026-10-22T09:00:00Z"))

    @Test
    fun `add modal defaults to quantity one and temporary off`() {
        val modal = AddItemUiState(
            title = "Milk",
            availableViews = shops,
            selectedViewIds = setOf(1L),
        )

        assertEquals(1, modal.quantity)
        assertFalse(modal.isTemporary)
        assertTrue(modal.canSave)
    }

    @Test
    fun `add modal can save with blank title when quantity changed`() {
        val modal = AddItemUiState(
            title = "",
            availableViews = shops,
            selectedViewIds = setOf(1L),
            quantity = 3,
        )

        assertFalse(modal.canSave)
    }

    @Test
    fun `edit modal with only quantity changed is not unchanged`() {
        val modal = EditItemUiState(
            itemId = 42L,
            originalName = "Milk",
            name = "Milk",
            availableViews = shops,
            selectedViewIds = setOf(1L),
            originalVisibleIn = setOf(1L),
            originalQuantity = 1,
            quantity = 3,
        )

        assertFalse(modal.isUnchanged)
        assertTrue(modal.canSave)
    }

    @Test
    fun `edit modal with only temporary changed is not unchanged`() {
        val modal = EditItemUiState(
            itemId = 42L,
            originalName = "Milk",
            name = "Milk",
            availableViews = shops,
            selectedViewIds = setOf(1L),
            originalVisibleIn = setOf(1L),
            originalIsTemporary = false,
            isTemporary = true,
        )

        assertFalse(modal.isUnchanged)
        assertTrue(modal.canSave)
    }

    @Test
    fun `edit modal with everything unchanged cannot save`() {
        val modal = EditItemUiState(
            itemId = 42L,
            originalName = "Milk",
            name = "Milk",
            availableViews = shops,
            selectedViewIds = setOf(1L),
            originalVisibleIn = setOf(1L),
            originalQuantity = 1,
            quantity = 1,
            originalIsTemporary = false,
            isTemporary = false,
        )

        assertTrue(modal.isUnchanged)
        assertFalse(modal.canSave)
    }

    @Test
    fun `edit modal with blank name cannot save even when quantity changed`() {
        val modal = EditItemUiState(
            itemId = 42L,
            originalName = "Milk",
            name = "  ",
            availableViews = shops,
            selectedViewIds = setOf(1L),
            originalVisibleIn = setOf(1L),
            originalQuantity = 1,
            quantity = 3,
        )

        assertFalse(modal.canSave)
    }

    @Test
    fun `edit modal secondary label reads Close when unchanged and Discard when changed`() {
        val base = EditItemUiState(
            itemId = 42L,
            originalName = "Milk",
            name = "Milk",
            availableViews = shops,
            selectedViewIds = setOf(1L),
            originalVisibleIn = setOf(1L),
        )

        assertEquals("Close", base.secondaryLabel)
        assertEquals("Discard", base.copy(selectedViewIds = emptySet()).secondaryLabel)
        assertEquals("Discard", base.copy(name = "Almond milk").secondaryLabel)
        assertEquals("Discard", base.copy(quantity = 3, originalQuantity = 1).secondaryLabel)
        assertEquals("Discard", base.copy(isTemporary = true).secondaryLabel)
    }

    @Test
    fun `edit modal pre-selects shops from visibleIn`() {
        val modal = EditItemUiState(
            itemId = 42L,
            originalName = "Milk",
            name = "Milk",
            availableViews = listOf(
                Shop(id = 1L, name = "Global", createdAt = "2026-10-22T09:00:00Z"),
                Shop(id = 2L, name = "Auchan", createdAt = "2026-10-22T09:00:00Z"),
                Shop(id = 3L, name = "Carrefour", createdAt = "2026-10-22T09:00:00Z"),
            ),
            selectedViewIds = setOf(1L, 3L),
            originalVisibleIn = setOf(1L, 3L),
        )

        assertTrue(1L in modal.selectedViewIds)
        assertTrue(3L in modal.selectedViewIds)
        assertFalse(2L in modal.selectedViewIds)
        assertFalse(modal.allViewsSelected)
    }

    @Test
    fun `edit modal selection change enables save and unchanged selection keeps it disabled`() {
        val base = EditItemUiState(
            itemId = 42L,
            originalName = "Milk",
            name = "Milk",
            availableViews = shops,
            selectedViewIds = setOf(1L),
            originalVisibleIn = setOf(1L),
        )

        assertFalse(base.canSave)
        assertTrue(base.copy(selectedViewIds = emptySet()).canSave)
        assertFalse(base.copy(selectedViewIds = setOf(1L), originalVisibleIn = setOf(1L)).canSave)
    }
}
