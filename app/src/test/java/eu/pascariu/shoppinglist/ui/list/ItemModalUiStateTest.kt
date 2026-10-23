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
}
