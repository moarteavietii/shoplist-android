package eu.pascariu.shoppinglist.ui.quickmenu

import eu.pascariu.shoppinglist.data.prefs.SortOrder
import org.junit.Assert.assertEquals
import org.junit.Test

class SortModeLedStateTest {

    @Test
    fun `manual order with sort mode on shows filled dot`() {
        assertEquals(
            SortModeLedState.FILLED,
            deriveSortModeLedState(sortModeEnabled = true, order = SortOrder.MANUAL),
        )
    }

    @Test
    fun `manual order with sort mode off shows hollow dot`() {
        assertEquals(
            SortModeLedState.HOLLOW,
            deriveSortModeLedState(sortModeEnabled = false, order = SortOrder.MANUAL),
        )
    }

    @Test
    fun `alphabetical order shows no dot`() {
        assertEquals(
            SortModeLedState.NONE,
            deriveSortModeLedState(sortModeEnabled = true, order = SortOrder.ALPHABETICAL),
        )
    }

    @Test
    fun `frequency order shows no dot`() {
        assertEquals(
            SortModeLedState.NONE,
            deriveSortModeLedState(sortModeEnabled = false, order = SortOrder.FREQUENCY),
        )
    }
}
