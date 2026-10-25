package eu.pascariu.shoppinglist.ui.list

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.pascariu.shoppinglist.ui.theme.ShoplistTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class ShopModalTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var saveCount = 0
    private var discardCount = 0
    private var deleteTappedCount = 0

    private fun setContent(mode: ShopModalMode) {
        composeRule.setContent {
            ShoplistTheme {
                ShopModal(
                    modal = ShopModalUiState(
                        mode = mode,
                        shopName = "Global",
                        name = if (mode == ShopModalMode.EDIT) "Global Market" else "",
                    ),
                    onNameChanged = { },
                    onSave = { saveCount++ },
                    onDiscard = { discardCount++ },
                    onDeleteTapped = { deleteTappedCount++ },
                    onDeleteConfirmed = { },
                    onDeleteCancelled = { },
                )
            }
        }
    }

    @Test
    fun `edit mode shows delete discard and save buttons`() {
        setContent(ShopModalMode.EDIT)

        composeRule.onNodeWithText("Delete").assertExists()
        composeRule.onNodeWithText("Discard").assertExists()
        composeRule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun `create mode hides delete button`() {
        setContent(ShopModalMode.CREATE)

        composeRule.onNodeWithText("Delete").assertDoesNotExist()
        composeRule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun `save button invokes save`() {
        setContent(ShopModalMode.EDIT)

        composeRule.onNodeWithText("Save").performClick()

        composeRule.runOnIdle {
            assertEquals(1, saveCount)
        }
    }

    @Test
    fun `discard button invokes discard`() {
        setContent(ShopModalMode.EDIT)

        composeRule.onNodeWithText("Discard").performClick()

        composeRule.runOnIdle {
            assertEquals(1, discardCount)
        }
    }

    @Test
    fun `delete button invokes delete tap`() {
        setContent(ShopModalMode.EDIT)

        composeRule.onNodeWithText("Delete").performClick()

        composeRule.runOnIdle {
            assertEquals(1, deleteTappedCount)
        }
    }
}
