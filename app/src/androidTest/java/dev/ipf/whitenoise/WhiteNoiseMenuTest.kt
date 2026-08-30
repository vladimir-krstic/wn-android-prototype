package dev.ipf.whitenoise

import android.graphics.Rect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.pressBack
import androidx.test.filters.SdkSuppress
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.ui.components.WhiteNoiseDropdownMenu
import dev.ipf.whitenoise.ui.components.WhiteNoiseMenuItem
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WhiteNoiseMenuTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun commandsKeepNativeTargetsAndDisabledStateAndDismissBeforeDispatch() {
        val expanded = mutableStateOf(true)
        val events = mutableListOf<String>()
        rule.setContent {
            WhiteNoiseTheme {
                Box {
                    Text("Anchor")
                    WhiteNoiseDropdownMenu(
                        expanded = expanded.value,
                        onDismissRequest = { expanded.value = false; events += "dismiss" },
                        items = listOf(
                            WhiteNoiseMenuItem("Archive", { events += "archive" }, R.drawable.ic_archive),
                            WhiteNoiseMenuItem("Delete", { events += "delete" }, R.drawable.ic_delete,
                                enabled = false, destructive = true),
                        ),
                        modifier = Modifier.testTag("menu"),
                    )
                }
            }
        }
        rule.onNodeWithText("Archive")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchHeightIsEqualTo(48.dp)
        rule.onNodeWithText("Delete").assertIsNotEnabled().performTouchInput { click() }
        rule.runOnIdle { assertTrue(events.isEmpty()) }
        rule.onNodeWithText("Archive").performClick()
        rule.onNodeWithTag("menu").assertDoesNotExist()
        rule.runOnIdle { assertEquals(listOf("dismiss", "archive"), events) }
    }

    @Test fun menuUsesTheNativeStandardSurfaceInLightAndDarkThemes() {
        val dark = mutableStateOf(false)
        var expected = Color.Unspecified
        rule.setContent {
            WhiteNoiseTheme(if (dark.value) AppearancePreference.Dark else AppearancePreference.Light) {
                expected = MaterialTheme.colorScheme.surfaceContainerLow
                Box {
                    Text("Anchor")
                    WhiteNoiseDropdownMenu(
                        expanded = true,
                        onDismissRequest = {},
                        items = listOf(
                            WhiteNoiseMenuItem("Read", {}, R.drawable.ic_check),
                            WhiteNoiseMenuItem("Pin", {}, R.drawable.ic_push_pin),
                            WhiteNoiseMenuItem("Mute", {}, R.drawable.ic_notifications_off),
                        ),
                        modifier = Modifier.testTag("menu"),
                    )
                }
            }
        }
        for (isDark in listOf(false, true)) {
            rule.runOnIdle { dark.value = isDark }
            val pixels = rule.onNodeWithTag("menu").captureToImage().toPixelMap()
            // Empty leading inset, away from group corners, icons and item state layers.
            val actual = pixels[2, pixels.height / 2]
            assertEquals(expected.red, actual.red, .01f)
            assertEquals(expected.green, actual.green, .01f)
            assertEquals(expected.blue, actual.blue, .01f)
        }
    }

    @Test fun edgeAnchorsAndShortMenusStayReachableAcrossPaneWidthsAndLargeRtlText() {
        val expanded = mutableStateOf(false)
        val width = mutableStateOf(320)
        val bottom = mutableStateOf(false)
        val largeRtl = mutableStateOf(false)
        var chosen = -1
        rule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f, if (largeRtl.value) 2f else 1f),
                LocalLayoutDirection provides if (largeRtl.value) LayoutDirection.Rtl else LayoutDirection.Ltr,
            ) {
                WhiteNoiseTheme(if (largeRtl.value) AppearancePreference.Dark else AppearancePreference.Light) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Box(Modifier.widthIn(max = width.value.dp).fillMaxSize()) {
                            Box(Modifier.align(if (bottom.value) Alignment.BottomEnd else Alignment.TopStart)) {
                                TextButton(onClick = { expanded.value = true }) { Text("Open menu") }
                                WhiteNoiseDropdownMenu(
                                    expanded = expanded.value,
                                    onDismissRequest = { expanded.value = false },
                                    items = List(12) { index ->
                                        WhiteNoiseMenuItem("Option $index", { chosen = index })
                                    },
                                    // Constrain the available height without replacing native popup placement.
                                    modifier = Modifier.heightIn(max = 240.dp).testTag("menu"),
                                )
                            }
                        }
                    }
                }
            }
        }
        for (paneWidth in listOf(320, 840)) {
            for (rtl in listOf(false, true)) {
                for (atBottom in listOf(false, true)) {
                    rule.runOnIdle { width.value = paneWidth; largeRtl.value = rtl; bottom.value = atBottom }
                    rule.onNodeWithText("Open menu").performClick()
                    assertMenuInsideVisibleWindow()
                    rule.onNodeWithText("Option 0").assertIsDisplayed()
                    rule.onNodeWithText("Option 11").performScrollTo().assertIsDisplayed()
                    rule.onNodeWithText("Option 0").performScrollTo().assertIsDisplayed()
                    rule.onNodeWithText("Option 11").performScrollTo().performClick()
                    rule.runOnIdle { assertEquals(11, chosen) }
                    rule.onNodeWithTag("menu").assertDoesNotExist()
                }
            }
        }
    }

    @Test fun backAndOutsideTapDismissWithoutInvokingAnAction() {
        val expanded = mutableStateOf(true)
        var actions = 0
        var dismissals = 0
        rule.setContent {
            WhiteNoiseTheme {
                Box {
                    Text("Anchor")
                    WhiteNoiseDropdownMenu(
                        expanded = expanded.value,
                        onDismissRequest = { expanded.value = false; dismissals++ },
                        items = listOf(WhiteNoiseMenuItem("Delete", { actions++ }, destructive = true)),
                        modifier = Modifier.testTag("menu"),
                    )
                }
            }
        }
        pressBack()
        rule.onNodeWithTag("menu").assertDoesNotExist()
        rule.runOnIdle { assertEquals(0, actions); assertEquals(1, dismissals); expanded.value = true }
        val menu = rule.onNodeWithTag("menu").fetchSemanticsNode()
        injectScreenTap(
            menu.positionOnScreen.x + menu.size.width + 8f,
            menu.positionOnScreen.y + menu.size.height / 2f,
        )
        rule.waitForIdle()
        rule.onNodeWithTag("menu").assertDoesNotExist()
        rule.runOnIdle { assertEquals(0, actions); assertEquals(2, dismissals) }
    }

    @Test @SdkSuppress(minSdkVersion = 30)
    fun openingAndDismissingAboveTheKeyboardPreservesTheEditor() {
        val expanded = mutableStateOf(false)
        val draft = mutableStateOf("")
        rule.setContent {
            WhiteNoiseTheme {
                Column(Modifier.fillMaxSize()) {
                    TextField(draft.value, { draft.value = it }, Modifier.fillMaxWidth().testTag("editor"))
                    Box {
                        TextButton(onClick = { expanded.value = true }) { Text("Open menu") }
                        WhiteNoiseDropdownMenu(
                            expanded = expanded.value,
                            onDismissRequest = { expanded.value = false },
                            items = List(8) { WhiteNoiseMenuItem("Option $it", {}) },
                            modifier = Modifier.testTag("menu"),
                        )
                    }
                }
            }
        }
        rule.onNodeWithTag("editor").performClick().performTextInput("Keep this draft")
        rule.waitUntil(5_000) {
            ViewCompat.getRootWindowInsets(rule.activity.window.decorView)
                ?.isVisible(WindowInsetsCompat.Type.ime()) == true
        }
        rule.onNodeWithText("Open menu").performClick()
        assertMenuInsideVisibleWindow()
        rule.onNodeWithText("Option 7").performScrollTo().assertIsDisplayed()
        pressBack()
        rule.onNodeWithTag("menu").assertDoesNotExist()
        rule.onNodeWithTag("editor").assertIsFocused().assertTextEquals("Keep this draft")
    }

    private fun assertMenuInsideVisibleWindow() {
        val menu = rule.onNodeWithTag("menu").fetchSemanticsNode()
        val visible = rule.runOnIdle {
            Rect().also { rule.activity.window.decorView.getWindowVisibleDisplayFrame(it) }
        }
        assertTrue(menu.positionOnScreen.x >= visible.left)
        assertTrue(menu.positionOnScreen.y >= visible.top)
        assertTrue(menu.positionOnScreen.x + menu.size.width <= visible.right)
        assertTrue(menu.positionOnScreen.y + menu.size.height <= visible.bottom)
    }
}
