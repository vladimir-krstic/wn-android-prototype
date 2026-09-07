package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.settings.ActionColorScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActionColorEditingTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private var profile by mutableStateOf(ProfileFixtures.marmota)
    private var saves = 0
    private var exits = 0
    private fun show(theme: AppearancePreference) {
        profile = profile.copy(settings = profile.settings.copy(appearance = theme))
        rule.setContent { WhiteNoiseTheme(appearance = theme, colors = profile.settings.colors) {
            ActionColorScreen(profile, onBack = { exits++ }, onChange = {
                saves++; profile = profile.copy(settings = it)
            })
        } }
    }
    private fun editHex(value: String) = rule.onNodeWithText("Hex color").performScrollTo().performTextReplacement(value)
    private fun preview(): Color {
        val pixels = rule.onNodeWithTag("action_color.preview").assertIsDisplayed().captureToImage().toPixelMap()
        return pixels[pixels.width / 2, 3]
    }
    private fun liveSave(theme: AppearancePreference) {
        show(theme)
        val original = profile.settings
        rule.onNodeWithText("Apply color").assertDoesNotExist()
        rule.onNodeWithTag("action_color.save").assertIsNotEnabled()
        editHex("#FF0000")
        assertEquals(Color.Red, preview())
        rule.onNodeWithTag("color.hue").performScrollTo()
            .performSemanticsAction(SemanticsActions.SetProgress) { it(120f) }
        assertEquals(Color.Green, preview())
        rule.runOnIdle { assertEquals(original, profile.settings); assertEquals(0, saves) }
        rule.onNodeWithTag("action_color.reset").assert(hasAnyAncestor(hasTestTag("action_color.controls")))
        rule.onNodeWithTag("action_color.save").performClick()
        rule.runOnIdle {
            assertEquals(0xFF00FF00L, profile.settings.colors.forTheme(AppearanceColorTheme.resolve(theme, false)).actionArgb)
            assertEquals(1, saves); assertEquals(1, exits)
        }
    }
    @Test fun lightActionPreviewsUntilSave() = liveSave(AppearancePreference.Light)
    @Test fun darkActionPreviewsUntilSave() = liveSave(AppearancePreference.Dark)
    @Test fun invalidHexAndResetDoNotCommitBeforeSaveAndBackDiscards() {
        show(AppearancePreference.Light)
        val original = profile.settings
        val defaultPreview = preview()
        editHex("#FF0000")
        editHex("#GG")
        rule.onNodeWithTag("action_color.save").assertIsNotEnabled()
        assertEquals(Color.Red, preview())
        rule.onNodeWithTag("action_color.reset").performScrollTo().performClick()
        assertEquals(defaultPreview, preview())
        rule.onNodeWithTag("action_color.save").assertIsNotEnabled()
        editHex("#0000FF")
        rule.onNodeWithContentDescription("Back").performClick()
        rule.runOnIdle { assertEquals(original, profile.settings); assertEquals(0, saves); assertEquals(1, exits) }
    }
}
