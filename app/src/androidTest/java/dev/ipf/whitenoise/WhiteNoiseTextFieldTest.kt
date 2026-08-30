package dev.ipf.whitenoise

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextFieldDefaults
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhiteNoiseTextFieldTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<EmptyTestActivity>()

    @Test
    fun labelAndSupportingTextAlignWithTheInputContentLine() {
        composeRule.setContent {
            val state = rememberTextFieldState(initialText = "Pebble")
            WhiteNoiseTheme {
                WhiteNoiseTextField(
                    state = state,
                    modifier = Modifier.width(280.dp).testTag("field"),
                    label = { Text("Name", Modifier.testTag("label")) },
                    supportingText = { Text("Helper", Modifier.testTag("support")) },
                )
            }
        }

        val field = composeRule.onNodeWithTag("field").getUnclippedBoundsInRoot()
        val label = composeRule.onNodeWithTag("label", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()
        val support = composeRule.onNodeWithTag("support", useUnmergedTree = true)
            .getUnclippedBoundsInRoot()

        assertEquals(
            WhiteNoiseTextFieldDefaults.ContentInset.value,
            label.left.value - field.left.value,
            0.5f,
        )
        assertEquals(
            WhiteNoiseTextFieldDefaults.ContentInset.value,
            support.left.value - field.left.value,
            0.5f,
        )
    }

    @Test
    fun errorStateExposesTheSpecificAccessibilityMessage() {
        composeRule.setContent {
            val state = rememberTextFieldState(initialText = "invalid")
            WhiteNoiseTheme {
                WhiteNoiseTextField(
                    state = state,
                    modifier = Modifier.testTag("field"),
                    label = { Text("Relay URL") },
                    isError = true,
                    errorMessage = "Enter a unique relay URL.",
                )
            }
        }

        composeRule.onNodeWithTag("field").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                "Enter a unique relay URL.",
            ),
        )
    }
}
