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

    @Test
    fun `search control led follows search expanded state`() {
        assertEquals(
            SortModeLedState.FILLED,
            deriveControlLedState(
                control = QuickMenuControl.SEARCH,
                isSearchExpanded = true,
                isShowingCheckedItems = false,
                isSortModeEnabled = false,
                sortOrder = SortOrder.MANUAL,
            ),
        )
        assertEquals(
            SortModeLedState.HOLLOW,
            deriveControlLedState(
                control = QuickMenuControl.SEARCH,
                isSearchExpanded = false,
                isShowingCheckedItems = false,
                isSortModeEnabled = false,
                sortOrder = SortOrder.MANUAL,
            ),
        )
    }

    @Test
    fun `checked items control led follows checked items state`() {
        assertEquals(
            SortModeLedState.FILLED,
            deriveControlLedState(
                control = QuickMenuControl.CHECKED_ITEMS_TOGGLE,
                isSearchExpanded = false,
                isShowingCheckedItems = true,
                isSortModeEnabled = false,
                sortOrder = SortOrder.MANUAL,
            ),
        )
        assertEquals(
            SortModeLedState.HOLLOW,
            deriveControlLedState(
                control = QuickMenuControl.CHECKED_ITEMS_TOGGLE,
                isSearchExpanded = false,
                isShowingCheckedItems = false,
                isSortModeEnabled = false,
                sortOrder = SortOrder.MANUAL,
            ),
        )
    }

    @Test
    fun `sort mode control led delegates to sort mode derivation`() {
        assertEquals(
            SortModeLedState.FILLED,
            deriveControlLedState(
                control = QuickMenuControl.SORT_MODE,
                isSearchExpanded = false,
                isShowingCheckedItems = false,
                isSortModeEnabled = true,
                sortOrder = SortOrder.MANUAL,
            ),
        )
        assertEquals(
            SortModeLedState.NONE,
            deriveControlLedState(
                control = QuickMenuControl.SORT_MODE,
                isSearchExpanded = false,
                isShowingCheckedItems = false,
                isSortModeEnabled = true,
                sortOrder = SortOrder.ALPHABETICAL,
            ),
        )
    }

    @Test
    fun `controls without led mapping show no led`() {
        assertEquals(
            null,
            deriveControlLedState(
                control = QuickMenuControl.VIEW_SWITCHER,
                isSearchExpanded = false,
                isShowingCheckedItems = false,
                isSortModeEnabled = false,
                sortOrder = SortOrder.MANUAL,
            ),
        )
        assertEquals(
            null,
            deriveControlLedState(
                control = QuickMenuControl.SETTINGS,
                isSearchExpanded = false,
                isShowingCheckedItems = false,
                isSortModeEnabled = false,
                sortOrder = SortOrder.MANUAL,
            ),
        )
    }
}
