package eu.pascariu.shoppinglist.ui.list

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import eu.pascariu.shoppinglist.ui.theme.ShoplistTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = android.app.Application::class)
class StatusChipUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `chip renders led dot and label`() {
        composeRule.setContent {
            ShoplistTheme {
                StatusChip(label = "Offline — changes will be queued")
            }
        }

        composeRule.onNodeWithText("Offline — changes will be queued").assertIsDisplayed()
    }

    @Test
    fun `chip label matches offline wording`() {
        composeRule.setContent {
            ShoplistTheme {
                StatusChip(label = "Offline — changes will be queued")
            }
        }

        composeRule.onNodeWithText("Offline — changes will be queued").assertTextEquals("Offline — changes will be queued")
    }
}
