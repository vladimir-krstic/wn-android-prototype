package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseDialogChoiceRow
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModalSelectorTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun lightSelectionAndPressedFeedbackKeepRoundedCorners() = verifyCorners(AppearancePreference.Light)
    @Test fun darkSelectionAndPressedFeedbackKeepRoundedCorners() = verifyCorners(AppearancePreference.Dark)

    private fun verifyCorners(appearance: AppearancePreference) {
        var canvas = Color.Unspecified
        var selection = Color.Unspecified
        rule.setContent {
            WhiteNoiseTheme(appearance = appearance) {
                canvas = MaterialTheme.colorScheme.surfaceContainerLow
                selection = MaterialTheme.colorScheme.surfaceContainerHigh
                WhiteNoiseAlertDialog(onDismissRequest = {}, confirmButton = {}, title = { Text("When finished") }, text = {
                    WhiteNoiseDialogChoiceRow(
                        title = "Paste into draft",
                        subtitle = "Review or edit the text before sending",
                        selected = true,
                        onClick = {},
                        modifier = Modifier.testTag("choice"),
                    )
                })
            }
        }
        val node = rule.onNodeWithTag("choice")
        node.assertIsSelected()
        val resting = node.captureToImage().toPixelMap()
        assertEquals(canvas.toArgb(), resting[1, 1].toArgb())
        assertEquals(canvas.toArgb(), resting[resting.width - 2, 1].toArgb())
        assertEquals(selection.toArgb(), resting[resting.width / 2, 1].toArgb())
        node.performTouchInput { down(center) }
        val pressed = node.captureToImage().toPixelMap()
        assertEquals(canvas.toArgb(), pressed[1, 1].toArgb())
        assertEquals(canvas.toArgb(), pressed[pressed.width - 2, 1].toArgb())
        node.performTouchInput { up() }
    }

    @Test fun wholeRowSelectsOnceAndDisabledChoiceRetainsSupportingText() {
        var selected by mutableStateOf(false)
        var changes = 0
        rule.setContent {
            WhiteNoiseTheme {
                WhiteNoiseAlertDialog(onDismissRequest = {}, confirmButton = {}, text = {
                    Column(Modifier.selectableGroup()) {
                        WhiteNoiseDialogChoiceRow("Send message", selected, { selected = true; changes++ })
                        WhiteNoiseDialogChoiceRow("Unavailable voice", false, { changes++ }, enabled = false,
                            subtitle = "Download this voice in Android Settings")
                    }
                })
            }
        }
        rule.onNodeWithText("Send message").performClick().assertIsSelected()
        rule.onNodeWithText("Unavailable voice").assertIsNotEnabled().performTouchInput { click() }
        rule.onNodeWithText("Download this voice in Android Settings").assertExists()
        rule.runOnIdle { assertEquals(1, changes) }
    }
}
