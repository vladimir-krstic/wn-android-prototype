package dev.ipf.whitenoise

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.ui.components.WhiteNoiseButton
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhiteNoiseButtonTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<EmptyTestActivity>()

    @Test
    fun loadingPrimaryButtonShowsStatusAndBlocksRepeatActivation() {
        var clickCount = 0
        composeRule.setContent {
            WhiteNoiseTheme {
                WhiteNoiseButton(
                    onClick = { clickCount += 1 },
                    loading = true,
                    loadingLabel = "Creating Profile…",
                    modifier = Modifier.testTag("button"),
                ) {
                    Text("Sign Up")
                }
            }
        }

        composeRule.onNodeWithTag("button")
            .assertIsNotEnabled()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "In progress",
                ),
            )
            .performTouchInput { click() }
        composeRule.onNodeWithText("Creating Profile…").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, clickCount) }
    }
}
