package eu.pascariu.shoppinglist.ui.quickmenu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import eu.pascariu.shoppinglist.data.prefs.SortOrder
import eu.pascariu.shoppinglist.ui.theme.ShoplistTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class QuickMenuUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var lastTappedControl: QuickMenuControl? = null
    private var expandedChanges = 0
    private var lastExpandedValue: Boolean? = null
    private var lastSelectedOrder: SortOrder? = null

    private fun setContent(
        isDrawerOpen: Boolean = false,
        isExpanded: Boolean = true,
        isSearchExpanded: Boolean = false,
        isShowingCheckedItems: Boolean = false,
        isSortModeEnabled: Boolean = false,
        sortOrder: SortOrder = SortOrder.MANUAL,
        globalUncheckedCount: Int = 0,
        canUndo: Boolean = false,
        isUndoInFlight: Boolean = false,
    ) {
        lastTappedControl = null
        expandedChanges = 0
        lastExpandedValue = null
        lastSelectedOrder = null
        composeRule.setContent {
            ShoplistTheme {
                QuickMenu(
                    isDrawerOpen = isDrawerOpen,
                    isExpanded = isExpanded,
                    isSearchExpanded = isSearchExpanded,
                    isShowingCheckedItems = isShowingCheckedItems,
                    isSortModeEnabled = isSortModeEnabled,
                    sortOrder = sortOrder,
                    globalUncheckedCount = globalUncheckedCount,
                    canUndo = canUndo,
                    isUndoInFlight = isUndoInFlight,
                    onControlTapped = { control -> lastTappedControl = control },
                    onSortOrderSelected = { order -> lastSelectedOrder = order },
                    onExpandedChanged = { value ->
                        expandedChanges++
                        lastExpandedValue = value
                    },
                )
            }
        }
    }

    @Test
    fun `chevron content description reflects expanded state`() {
        setContent(isExpanded = true)

        composeRule.onNodeWithContentDescription("Collapse quick menu")
            .assertIsDisplayed()
    }

    @Test
    fun `chevron content description says expand when collapsed`() {
        setContent(isExpanded = false)

        composeRule.onNodeWithContentDescription("Expand quick menu")
            .assertIsDisplayed()
    }

    @Test
    fun `unchecked badge appears on view switcher when count is positive`() {
        setContent(globalUncheckedCount = 3)

        composeRule.onNodeWithContentDescription("3 unchecked items")
            .assertIsDisplayed()
    }

    @Test
    fun `no badge when unchecked count is zero`() {
        setContent(globalUncheckedCount = 0)

        composeRule.onNodeWithContentDescription("0 unchecked items")
            .assertDoesNotExist()
    }

    @Test
    fun `control buttons are rendered when expanded`() {
        setContent(isExpanded = true)

        composeRule.onNodeWithContentDescription("View switcher").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Settings").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sort mode").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Checked items toggle").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Search").assertIsDisplayed()
    }

    @Test
    fun `control buttons are absent when collapsed`() {
        setContent(isExpanded = false)

        composeRule.onNodeWithContentDescription("View switcher")
            .assertDoesNotExist()
    }

    @Test
    fun `tapping a control reports it`() {
        setContent(isExpanded = true)

        composeRule.onNodeWithContentDescription("Search").performClick()

        composeRule.runOnIdle {
            assertEquals(QuickMenuControl.SEARCH, lastTappedControl)
        }
    }

    @Test
    fun `tapping chevron toggles expansion`() {
        setContent(isExpanded = true)

        composeRule.onNodeWithContentDescription("Collapse quick menu").performClick()

        composeRule.runOnIdle {
            assertEquals(1, expandedChanges)
            assertEquals(false, lastExpandedValue)
        }
    }

    @Test
    fun `undo button is absent when there is nothing to undo`() {
        setContent(isExpanded = true, canUndo = false)

        composeRule.onNodeWithContentDescription("Undo last action")
            .assertDoesNotExist()
    }

    @Test
    fun `undo button is rendered when undo is available`() {
        setContent(isExpanded = true, canUndo = true)

        composeRule.onNodeWithContentDescription("Undo last action")
            .assertIsDisplayed()
    }

    @Test
    fun `tapping undo reports it`() {
        setContent(isExpanded = true, canUndo = true)

        composeRule.onNodeWithContentDescription("Undo last action").performClick()

        composeRule.runOnIdle {
            assertEquals(QuickMenuControl.UNDO, lastTappedControl)
        }
    }

    @Test
    fun `undo button is not clickable while undo is in flight`() {
        setContent(isExpanded = true, canUndo = true, isUndoInFlight = true)

        composeRule.onNodeWithContentDescription("Undo last action").performClick()

        composeRule.runOnIdle {
            assertEquals(null, lastTappedControl)
        }
    }
}
