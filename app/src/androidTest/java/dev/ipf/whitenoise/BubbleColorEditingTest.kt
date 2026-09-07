package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.settings.ChatBubbleColorsScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BubbleColorEditingTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private var profile by mutableStateOf(ProfileFixtures.marmota)
    private var saves = 0
    private var exits = 0

    @Composable private fun Screen() {
        WhiteNoiseTheme(appearance = profile.settings.appearance, colors = profile.settings.colors) {
            ChatBubbleColorsScreen(profile = profile, onBack = { exits++ }, onProfileChange = {
                saves++; profile = profile.copy(settings = it)
            })
        }
    }
    private fun field(side: String) = rule.onNode(hasText("Hex color") and hasAnyAncestor(hasTestTag("bubble_colors.$side.picker")))
    private fun hex(side: String, value: String) {
        rule.onNodeWithTag("settings.list").performScrollToNode(hasTestTag("bubble_colors.$side.picker"))
        field(side).performScrollTo().performTextReplacement(value)
    }
    private fun preview(side: String): Color {
        val pixels = rule.onNodeWithTag("bubble_colors.$side.preview").assertIsDisplayed().captureToImage().toPixelMap()
        return pixels[pixels.width / 2, 3]
    }
    private fun slider(side: String, channel: String, value: Float) {
        rule.onNode(hasTestTag("color.$channel") and hasAnyAncestor(hasTestTag("bubble_colors.$side.picker")))
            .performScrollTo().performSemanticsAction(SemanticsActions.SetProgress) { it(value) }
    }
    private fun livePreview(theme: AppearancePreference) {
        profile = profile.copy(settings = profile.settings.copy(appearance = theme))
        val original = profile.settings
        rule.setContent { Screen() }
        rule.onNodeWithTag("bubble_colors.save").assertIsNotEnabled()
        rule.onNodeWithText("Apply color").assertDoesNotExist()
        hex("mine", "#FF0000")
        assertEquals(Color.Red, preview("mine"))
        slider("mine", "hue", 120f)
        assertEquals(Color.Green, preview("mine"))
        slider("mine", "saturation", 0f)
        assertEquals(Color.White, preview("mine"))
        // A zero-saturation RGB preview must not reset the user's hue.
        slider("mine", "hue", 240f)
        slider("mine", "saturation", 1f)
        assertEquals(Color.Blue, preview("mine"))
        slider("mine", "brightness", 0f)
        assertEquals(Color.Black, preview("mine"))
        slider("mine", "brightness", 1f)
        assertEquals(Color.Blue, preview("mine"))
        hex("other", "#00FF00")
        assertEquals(Color.Green, preview("other"))
        rule.runOnIdle { assertEquals(original, profile.settings); assertEquals(0, saves) }
        rule.onNodeWithTag("bubble_colors.save").performClick()
        rule.runOnIdle {
            val colors = profile.settings.colors.forTheme(AppearanceColorTheme.resolve(theme, false))
            assertEquals(0xFF0000FFL, colors.mineBubbleArgb)
            assertEquals(0xFF00FF00L, colors.otherBubbleArgb)
            assertEquals(1, saves); assertEquals(1, exits)
        }
    }
    @Test fun lightSlidersPreviewBothBubblesBeforeOneSave() = livePreview(AppearancePreference.Light)
    @Test fun darkSlidersPreviewBothBubblesBeforeOneSave() = livePreview(AppearancePreference.Dark)

    @Test fun invalidHexBlocksSaveAndRestorationKeepsUncommittedDraft() {
        profile = profile.copy(settings = profile.settings.copy(appearance = AppearancePreference.Light))
        val original = profile.settings
        val restoration = StateRestorationTester(rule)
        restoration.setContent { Screen() }
        hex("mine", "#FF0000")
        hex("mine", "#GG")
        assertEquals(Color.Red, preview("mine"))
        rule.onNodeWithTag("bubble_colors.save").assertIsNotEnabled()
        restoration.emulateSavedInstanceStateRestore()
        rule.onNodeWithTag("bubble_colors.save").assertIsNotEnabled()
        assertEquals(Color.Red, preview("mine"))
        hex("mine", "#0000FF")
        rule.onNodeWithTag("bubble_colors.save").assertIsEnabled()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.runOnIdle { assertEquals(original, profile.settings); assertEquals(0, saves); assertEquals(1, exits) }
    }

    @Test fun switchingProfileDropsAnotherProfilesUncommittedColors() {
        rule.setContent { Screen() }
        hex("mine", "#FF0000")
        rule.onNodeWithTag("bubble_colors.save").assertIsEnabled()
        rule.runOnIdle { profile = profile.copy(id = "another-profile") }
        rule.onNodeWithTag("bubble_colors.save").assertIsNotEnabled()
        rule.runOnIdle { assertEquals(0, saves) }
    }
}
