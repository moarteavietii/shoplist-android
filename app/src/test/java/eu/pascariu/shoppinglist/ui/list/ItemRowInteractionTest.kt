package eu.pascariu.shoppinglist.ui.list

import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.pascariu.shoppinglist.data.model.Item
import eu.pascariu.shoppinglist.ui.theme.ShoplistTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class ItemRowInteractionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var checkToggleCount = 0
    private var lastToggledItem: Item? = null
    private var longPressCount = 0

    private fun item(
        id: Long = 10L,
        name: String = "Milk",
        isChecked: Boolean = false,
    ) = Item(
        id = id,
        listId = 1L,
        name = name,
        isChecked = isChecked,
        orderMap = mapOf("1" to "a0"),
        quantity = 1,
        isTemporary = false,
        checkCount = 0,
        expiresAt = null,
        createdAt = "2026-10-22T09:00:00Z",
        visibleIn = listOf(1L),
    )

    private fun setContent(currentItem: Item) {
        composeRule.setContent {
            ShoplistTheme {
                ItemRow(
                    item = currentItem,
                    onCheckToggled = { toggled ->
                        checkToggleCount++
                        lastToggledItem = toggled
                    },
                    onLongPress = { longPressCount++ },
                )
            }
        }
    }

    @Test
    fun `checkbox tap toggles the item`() {
        setContent(item())

        composeRule.onNodeWithTag(ItemRowTestTags.checkbox).performClick()

        composeRule.runOnIdle {
            assertEquals(1, checkToggleCount)
            assertEquals("Milk", lastToggledItem?.name)
        }
    }

    @Test
    fun `row body tap does not toggle the item`() {
        setContent(item())

        composeRule.onNodeWithText("Milk").performClick()

        composeRule.runOnIdle {
            assertEquals(0, checkToggleCount)
            assertEquals(0, longPressCount)
        }
    }

    @Test
    fun `long press on row body opens edit`() {
        setContent(item())

        composeRule.onNodeWithText("Milk").performTouchInput { longClick() }

        composeRule.runOnIdle {
            assertEquals(1, longPressCount)
            assertEquals(0, checkToggleCount)
        }
    }

    @Test
    fun `row tap stays inert while long press still works`() {
        setContent(item())

        composeRule.onNodeWithText("Milk").performClick()
        composeRule.onNodeWithText("Milk").performTouchInput { longClick() }

        composeRule.runOnIdle {
            assertEquals(0, checkToggleCount)
            assertEquals(1, longPressCount)
        }
    }

    @Test
    fun `checkbox exposes checked state to accessibility`() {
        setContent(item(isChecked = true))

        composeRule.onNodeWithTag(ItemRowTestTags.checkbox).assertIsOn()
    }

    @Test
    fun `unchecked checkbox exposes off state`() {
        setContent(item(isChecked = false))

        composeRule.onNodeWithTag(ItemRowTestTags.checkbox).assertIsOff()
    }

    @Test
    fun `checkbox meets minimum touch target`() {
        setContent(item())

        composeRule.onNodeWithTag(ItemRowTestTags.checkbox)
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }
}
