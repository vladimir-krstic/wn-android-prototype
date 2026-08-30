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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
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
                    expected = MaterialTheme.colorScheme.surfaceContainer
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
                        Box(Modifier.fillMaxWidth().height(16.dp))
                    }
                }
            }
        }
        for (isDark in listOf(false, true)) {
            rule.runOnIdle { dark.value = isDark }
            val surface = rule.onNodeWithTag("sheet.surface").fetchSemanticsNode().boundsInRoot
            val title = rule.onNodeWithText("Sheet title").fetchSemanticsNode().boundsInRoot
            val header = rule.onNodeWithTag("sheet.header").fetchSemanticsNode().boundsInRoot
            val row = rule.onNodeWithTag("sheet.option").fetchSemanticsNode().boundsInRoot
            assertEquals(48f, title.top - surface.top, 1f)
            assertEquals(24f, title.left - surface.left, 1f)
            assertEquals(8f, row.top - title.bottom, 1f)
            val pixels = rule.onNodeWithTag("sheet.surface").captureToImage().toPixelMap()
            // Sample empty leading space, avoiding text, corners, handle and touch state layers.
            for (y in listOf(header.center.y, row.center.y, row.bottom + 4f)) {
                val color = pixels[8, (y - surface.top).toInt()]
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
        val close = rule.onNodeWithContentDescription("Close").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
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
        val dialog = rule.onNodeWithTag("mute.duration.dialog")
            .fetchSemanticsNode().boundsInRoot
        val firstChoice = rule.onNodeWithTag("mute.duration.OneHour")
            .fetchSemanticsNode().boundsInRoot
        val firstRadio = rule.onAllNodesWithTag(
            testTag = "dialog.choice.radio",
            useUnmergedTree = true,
        )[0].fetchSemanticsNode().boundsInRoot
        assertEquals(8f, firstChoice.left - dialog.left, 1f)
        assertEquals(8f, dialog.right - firstChoice.right, 1f)
        assertEquals(16f, title.left - firstChoice.left, 1f)
        assertEquals(title.left, firstRadio.left, 1f)
        rule.onNodeWithTag("mute.duration.dialog").performKeyInput { pressKey(Key.Back) }
        rule.runOnIdle { assertNull(result); visible.value = true }
        rule.onNodeWithTag("mute.duration.dialog").performTouchInput { click(Offset(-20f, -20f)) }
        rule.runOnIdle { assertNull(result); assertFalse(visible.value); visible.value = true }
        for (duration in MuteDuration.entries) {
            rule.onNodeWithText(duration.label).performScrollTo().performClick()
            rule.runOnIdle { assertEquals(duration, result); visible.value = true }
        }
    }
}
