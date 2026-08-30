package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.pressBack
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.model.MuteDuration
import dev.ipf.whitenoise.ui.components.MuteDurationDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseModalBottomSheet
import dev.ipf.whitenoise.ui.components.WhiteNoiseSheetHeader
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class MaterialSheetTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun ordinarySheetHasOneSurfaceAndNativeHandleToHeaderSpacingInBothThemes() {
        val dark = mutableStateOf(false)
        var expected = Color.Unspecified
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                WhiteNoiseTheme(if (dark.value) AppearancePreference.Dark else AppearancePreference.Light) {
                    expected = MaterialTheme.colorScheme.surfaceContainerLow
                    WhiteNoiseModalBottomSheet(
                        {},
                        sheetState = rememberBottomSheetState(
                            initialValue = SheetValue.Hidden,
                            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
                        ),
                    ) {
                        WhiteNoiseSheetHeader("Sheet title")
                        ListItem(
                            headlineContent = { Text("An ordinary option") },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.testTag("sheet.option"),
                        )
                        Box(Modifier.fillMaxWidth().height(16.dp).testTag("sheet.spacer"))
                    }
                }
            }
        }
        for (isDark in listOf(false, true)) {
            rule.runOnIdle { dark.value = isDark }
            val title = rule.onNodeWithText("Sheet title").fetchSemanticsNode().boundsInRoot
            val header = rule.onNodeWithTag("sheet.header").fetchSemanticsNode().boundsInRoot
            val row = rule.onNodeWithTag("sheet.option").fetchSemanticsNode().boundsInRoot
            val handle = rule.onNodeWithTag("sheet.dragHandle", useUnmergedTree = true)
                .fetchSemanticsNode().boundsInRoot
            val density = rule.activity.resources.displayMetrics.density
            assertEquals(48f * density, title.top - handle.top, 1f)
            assertEquals(24f * density, title.left - header.left, 1f)
            assertEquals(8f * density, row.top - title.bottom, 1f)
            // Sample empty leading space in each composited child, avoiding text and state layers.
            for (tag in listOf("sheet.header", "sheet.option", "sheet.spacer")) {
                val pixels = rule.onNodeWithTag(tag).captureToImage().toPixelMap()
                val color = pixels[8, pixels.height / 2]
                assertEquals(expected.red, color.red, .01f)
                assertEquals(expected.green, color.green, .01f)
                assertEquals(expected.blue, color.blue, .01f)
            }
        }
    }

    @Test fun headerWrapsAtLargeRtlTextAndCloseRetainsNativeTarget() {
        var dismissed = false
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f), LocalLayoutDirection provides LayoutDirection.Rtl) {
                WhiteNoiseTheme(AppearancePreference.Dark) {
                    WhiteNoiseModalBottomSheet({ dismissed = true }) {
                        WhiteNoiseSheetHeader("Help Improve White Noise", onClose = { dismissed = true })
                        Text("Scrollable content follows the title.")
                    }
                }
            }
        }
        val title = rule.onNodeWithText("Help Improve White Noise").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val close = rule.onNodeWithContentDescription("Close")
            .assertTouchHeightIsEqualTo(48.dp)
            .assertTouchWidthIsEqualTo(48.dp)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(close.right <= title.left)
        rule.onNodeWithContentDescription("Close").performClick()
        rule.runOnIdle { assertTrue(dismissed) }
    }

    @Test fun muteDialogKeepsAllDurationsAndSafeDismissalsAtLargeFont() {
        val visible = mutableStateOf(true)
        var result: MuteDuration? = null
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                WhiteNoiseTheme {
                    if (visible.value) {
                        MuteDurationDialog(
                            onDismiss = { visible.value = false },
                            selectedDuration = MuteDuration.OneDay,
                        ) {
                            result = it
                            visible.value = false
                        }
                    }
                }
            }
        }
        rule.onNodeWithText("Always").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("1 Day").assertIsSelected()
        rule.onNodeWithText("1 Hour").assertIsNotSelected()
        val title = rule.onNodeWithText("Mute for").fetchSemanticsNode().boundsInRoot
        rule.onNodeWithTag("mute.duration.OneHour").assertHeightIsAtLeast(56.dp)
        val firstRadio = rule.onAllNodesWithTag(
            testTag = "dialog.choice.radio",
            useUnmergedTree = true,
        )[0].fetchSemanticsNode().boundsInRoot
        assertEquals(title.left, firstRadio.left, 1f)
        pressBack()
        rule.runOnIdle { assertNull(result); visible.value = true }
        val reopenedDialog = rule.onNodeWithTag("mute.duration.dialog").fetchSemanticsNode()
        injectScreenTap(
            reopenedDialog.positionOnScreen.x + reopenedDialog.size.width + 8f,
            reopenedDialog.positionOnScreen.y + reopenedDialog.size.height / 2f,
        )
        rule.waitForIdle()
        rule.runOnIdle { assertNull(result); assertFalse(visible.value); visible.value = true }
        for (duration in MuteDuration.entries) {
            rule.onNodeWithText(duration.label).performScrollTo().performClick()
            rule.runOnIdle { assertEquals(duration, result); visible.value = true }
        }
    }
}
