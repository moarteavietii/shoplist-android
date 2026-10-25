package eu.pascariu.shoppinglist.ui.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.pascariu.shoppinglist.data.model.Shop
import eu.pascariu.shoppinglist.ui.theme.ShoplistTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class ItemModalContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var quantityChangeCount = 0
    private var lastQuantityDelta = 0
    private var viewToggleCount = 0
    private var lastToggledViewId: Long? = null
    private var saveCount = 0
    private var discardCount = 0
    private var deleteCount = 0

    private val shops = listOf(
        Shop(id = 1L, name = "Global", createdAt = "2026-10-22T09:00:00Z"),
        Shop(id = 2L, name = "Auchan", createdAt = "2026-10-22T09:00:00Z"),
    )

    private fun setAddContent(
        title: String = "Milk",
        quantity: Int = 1,
        selectedViewIds: Set<Long> = setOf(1L),
    ) {
        composeRule.setContent {
            ShoplistTheme {
                AddItemModal(
                    modal = AddItemUiState(
                        title = title,
                        availableViews = shops,
                        selectedViewIds = selectedViewIds,
                        quantity = quantity,
                    ),
                    onTitleChanged = { },
                    onQuantityChanged = { newQuantity ->
                        quantityChangeCount++
                        lastQuantityDelta = newQuantity - quantity
                    },
                    onTemporaryToggled = { },
                    onViewToggled = { viewId ->
                        viewToggleCount++
                        lastToggledViewId = viewId
                    },
                    onToggleAll = { },
                    onSave = { saveCount++ },
                    onDiscard = { discardCount++ },
                )
            }
        }
    }

    private fun setEditContent(
        name: String = "Milk",
        quantity: Int = 1,
        selectedViewIds: Set<Long> = setOf(1L),
        canSave: Boolean = true,
    ) {
        composeRule.setContent {
            ShoplistTheme {
                EditItemModal(
                    modal = EditItemUiState(
                        itemId = 42L,
                        originalName = name,
                        name = name,
                        availableViews = shops,
                        selectedViewIds = selectedViewIds,
                        originalVisibleIn = selectedViewIds,
                        originalQuantity = quantity,
                        quantity = quantity,
                    ),
                    onNameChanged = { },
                    onQuantityChanged = { },
                    onTemporaryToggled = { },
                    onViewToggled = { viewId ->
                        viewToggleCount++
                        lastToggledViewId = viewId
                    },
                    onToggleAll = { },
                    onSave = { saveCount++ },
                    onDiscard = { discardCount++ },
                    onDelete = { deleteCount++ },
                )
            }
        }
    }

    @Test
    fun `quantity stepper sits right of the name field`() {
        setAddContent()

        composeRule.onNodeWithContentDescription("Quantity: 1").assertIsDisplayed()
        composeRule.onNodeWithText("Milk").assertIsDisplayed()
    }

    @Test
    fun `stepper plus button increases quantity`() {
        setAddContent()

        composeRule.onNodeWithContentDescription("Increase quantity").performClick()

        composeRule.runOnIdle {
            assertEquals(1, quantityChangeCount)
            assertEquals(1, lastQuantityDelta)
        }
    }

    @Test
    fun `stepper minus button decreases quantity`() {
        setAddContent(quantity = 2)

        composeRule.onNodeWithContentDescription("Decrease quantity").performClick()

        composeRule.runOnIdle {
            assertEquals(1, quantityChangeCount)
            assertEquals(-1, lastQuantityDelta)
        }
    }

    @Test
    fun `visibility row text tap does not toggle the shop`() {
        setAddContent()

        composeRule.onNodeWithText("Auchan").performClick()

        composeRule.runOnIdle {
            assertEquals(0, viewToggleCount)
        }
    }

    @Test
    fun `visibility checkbox tap toggles the shop`() {
        setAddContent()

        composeRule.onNodeWithText("Auchan").assertExists()
        composeRule.onAllNodesWithTag("item_modal_visibility_checkbox")[1].performClick()

        composeRule.runOnIdle {
            assertEquals(1, viewToggleCount)
            assertEquals(2L, lastToggledViewId)
        }
    }

    @Test
    fun `visibility checkbox exposes checked state`() {
        setAddContent(selectedViewIds = setOf(1L, 2L))

        composeRule.onAllNodesWithTag("item_modal_visibility_checkbox")[0].assertIsOn()
    }

    @Test
    fun `visibility checkbox exposes unchecked state`() {
        setAddContent(selectedViewIds = setOf(1L))

        composeRule.onAllNodesWithTag("item_modal_visibility_checkbox")[1].assertIsOff()
    }

    @Test
    fun `add modal has quiet discard and primary save buttons`() {
        setAddContent()

        composeRule.onNodeWithText("Discard").assertExists()
        composeRule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun `edit modal has delete danger button`() {
        setEditContent()

        composeRule.onNodeWithText("Delete").assertExists()
    }

    @Test
    fun `save button disabled when cannot save`() {
        setAddContent(title = "")

        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle {
            assertEquals(0, saveCount)
        }
    }

    @Test
    fun `save button enabled when can save`() {
        setAddContent()

        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle {
            assertEquals(1, saveCount)
        }
    }

    @Test
    fun `discard button invokes discard`() {
        setAddContent()

        composeRule.onNodeWithText("Discard").performClick()

        composeRule.runOnIdle {
            assertEquals(1, discardCount)
        }
    }

    @Test
    fun `delete button invokes delete`() {
        setEditContent()

        composeRule.onNodeWithText("Delete").performClick()

        composeRule.runOnIdle {
            assertEquals(1, deleteCount)
        }
    }
}
