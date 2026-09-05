@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ListItemDefaults
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.ui.settings.SettingsAction
import dev.ipf.whitenoise.ui.settings.SettingsExplainer
import dev.ipf.whitenoise.ui.settings.SettingsGroup
import dev.ipf.whitenoise.ui.settings.SettingsLink
import dev.ipf.whitenoise.ui.settings.SettingsList
import dev.ipf.whitenoise.ui.settings.SettingsSwitch
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsLayoutTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun conditionalRowsKeepSeparationAndOneSwitchAction() {
        var showVolume by mutableStateOf(false)
        var changes = 0
        var gap = 0f
        rule.setContent {
            WhiteNoiseTheme {
                gap = with(LocalDensity.current) { ListItemDefaults.SegmentedGap.toPx() }
                SettingsGroup {
                    row {
                        SettingsSwitch("Mix with media", showVolume, {
                            showVolume = it
                            changes++
                        })
                    }
                    if (showVolume) row { SettingsLink("Volume", onClick = {}) }
                    row { SettingsAction("Refresh", {}, enabled = false) }
                }
            }
        }
        rule.onNodeWithText("Mix with media")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .performClick()
        val toggle = rule.onNodeWithText("Mix with media").fetchSemanticsNode().boundsInRoot
        val volume = rule.onNodeWithText("Volume").fetchSemanticsNode().boundsInRoot
        val refresh = rule.onNodeWithText("Refresh").fetchSemanticsNode().boundsInRoot
        assertEquals(gap, volume.top - toggle.bottom, 1f)
        assertEquals(gap, refresh.top - volume.bottom, 1f)
        rule.onNodeWithText("Refresh").assertIsNotEnabled()
        rule.onNodeWithText("Mix with media").performClick()
        rule.onNodeWithText("Volume").assertDoesNotExist()
        rule.runOnIdle { assertEquals(2, changes) }
    }

    @Test fun longEngineValueStaysBelowItsLabelAtLargeTypeInRtl() {
        val engine = "Speech Recognition and Synthesis from Google"
        rule.setContent {
            WhiteNoiseTheme(appearance = dev.ipf.whitenoise.model.AppearancePreference.Dark) {
                CompositionLocalProvider(
                    LocalDensity provides Density(LocalDensity.current.density, fontScale = 2f),
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                ) {
                    Box(Modifier.width(320.dp)) {
                        SettingsGroup {
                            row { SettingsLink("Engine", value = engine, onClick = {}) }
                        }
                    }
                }
            }
        }
        val label = rule.onNodeWithText("Engine", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val value = rule.onNodeWithText(engine, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("The title must not wrap one character per line", label.width > label.height)
        assertTrue("The value belongs below the label", value.top >= label.bottom)
        assertEquals("RTL text shares the same start inset", label.right, value.right, 1f)
    }

    @Test fun helperSpacingMatchesForSeparateAndCombinedLazyItems() {
        var related = 0f
        rule.setContent {
            WhiteNoiseTheme {
                related = with(LocalDensity.current) { 8.dp.toPx() }
                SettingsList {
                    item { SettingsGroup { row { SettingsAction("First", {}) } } }
                    item { SettingsExplainer("First helper") }
                    item {
                        SettingsGroup { row { SettingsAction("Second", {}) } }
                        SettingsExplainer("Second helper")
                    }
                }
            }
        }
        listOf("First", "Second").forEach { title ->
            val row = rule.onNodeWithText(title).fetchSemanticsNode().boundsInRoot
            val helper = rule.onNodeWithText("$title helper", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val label = rule.onNodeWithText(title, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertEquals(related, helper.top - row.bottom, 1f)
            assertEquals(label.left, helper.left, 1f)
        }
    }
}
