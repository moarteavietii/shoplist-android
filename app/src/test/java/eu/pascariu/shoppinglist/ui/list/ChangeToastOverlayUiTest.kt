package eu.pascariu.shoppinglist.ui.list

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import eu.pascariu.shoppinglist.data.realtime.ToastSeverity
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
class ChangeToastOverlayUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var dismissedToastId: Long? = null

    private fun setContent(toast: ChangeToast?) {
        dismissedToastId = null
        composeRule.setContent {
            ShoplistTheme {
                ChangeToastOverlay(
                    toast = toast,
                    onDismissed = { dismissedToastId = it },
                )
            }
        }
    }

    @Test
    fun `pill renders toast text with severity led`() {
        setContent(ChangeToast(id = 1, text = "checked: Bread", severity = ToastSeverity.Info))

        composeRule.onNodeWithText("checked: Bread").assertIsDisplayed()
    }

    @Test
    fun `danger pill renders failure text`() {
        setContent(ChangeToast(id = 2, text = "Couldn't refresh the list", severity = ToastSeverity.Danger))

        composeRule.onNodeWithText("Couldn't refresh the list").assertIsDisplayed()
    }

    @Test
    fun `tap dismisses the pill`() {
        setContent(ChangeToast(id = 3, text = "checked: Bread"))

        composeRule.onNodeWithText("checked: Bread").performClick()
        composeRule.runOnIdle { assertEquals(3L, dismissedToastId) }
    }

    @Test
    fun `horizontal swipe dismisses the pill`() {
        setContent(ChangeToast(id = 4, text = "checked: Bread"))

        composeRule.onNodeWithText("checked: Bread")
            .performTouchInput {
                down(center)
                moveBy(Offset(-300f, 0f), delayMillis = 50)
                moveBy(Offset(-300f, 0f), delayMillis = 50)
                up()
            }
        composeRule.waitForIdle()
        assertTrue("Swipe should dismiss the pill", dismissedToastId != null)
    }

    @Test
    fun `nothing renders when toast is null`() {
        setContent(null)

        composeRule.onNodeWithText("checked: Bread").assertDoesNotExist()
    }
}
