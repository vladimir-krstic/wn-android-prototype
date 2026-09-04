package dev.ipf.whitenoise

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.settings.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class ReadAloudPreferencesTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private var profile by mutableStateOf(Profile("p", "Name", "public", developerTools = DeveloperToolsState(isEnabled = true),
        chats = listOf(Chat("c", 0, ChatKind.Direct("other"), "Chat", timeline = listOf(
            ChatTimelineEntry.Message(ChatMessage("m", "other", 1, "Today", 1, "Now", "Read this sentence.")))))))
    private fun controller() = ReadAloudController().apply {
        this.profile = { this@ReadAloudPreferencesTest.profile }
        attachTestOutput({ _, _ -> true }, SpeechCatalogExamples.discovery(SpeechCatalogScenario.Voices, Locale.getDefault().toLanguageTag(), this@ReadAloudPreferencesTest.profile.settings.speech))
    }
    private fun show(c: ReadAloudController, chat: Boolean = false, developer: Boolean = false) {
        rule.setContent { WhiteNoiseTheme { CompositionLocalProvider(LocalReadAloudController provides c) {
            ReadAloudHost(profile, {}, onPreferences = { id, reduce ->
                if (profile.id == id) profile = profile.copy(settings = profile.settings.copy(speech = reduce(profile.settings.speech)))
            }) {
                when {
                    developer -> SpeechDeveloperDialog(profile, c, {})
                    chat -> ChatAutoReadSetting(profile, profile.chats.single())
                    else -> ReadAloudSettingsScreen(profile, {})
                }
            }
        } } }
    }
    @Test fun globalDefaultAndPerChatOverrideRemainIndependent() {
        val c = controller(); show(c, chat = true)
        rule.onNodeWithText("Read messages aloud").performClick()
        rule.onNodeWithText("On", useUnmergedTree = true).performClick()
        rule.runOnIdle { assertFalse(profile.settings.speech.autoReadDefault); assertTrue(profile.settings.speech.autoRead("c")) }
        rule.onNodeWithText("Read messages aloud").performClick()
        rule.onNodeWithText("Use default (Off)").performClick()
        rule.runOnIdle { assertFalse(profile.settings.speech.autoRead("c")); assertFalse(profile.settings.speech.autoReadOverrides.containsKey("c")) }
    }
    @Test fun customRateValidatesBeforeApplyingAndAcceptsDecimalComma() {
        val c = controller(); show(c)
        rule.onNodeWithText("Speech rate").performScrollTo().performClick()
        rule.onNodeWithText("Custom").performScrollTo().performClick()
        rule.onNodeWithTag("speech.custom_rate").performTextInput("20")
        rule.onNodeWithText("Apply").performClick()
        rule.onNodeWithTag("speech.custom_rate").assertExists()
        rule.runOnIdle { assertNull(profile.settings.speech.rate) }
        rule.onNodeWithTag("speech.custom_rate").performTextReplacement("1,7")
        rule.onNodeWithText("Apply").performClick()
        rule.runOnIdle { assertEquals(1.7f, profile.settings.speech.rate) }
    }
    @Test fun unavailableVoicesHaveDisabledReasons() {
        val c = controller(); show(c)
        rule.onNodeWithText("Voice", substring = false).performScrollTo().performClick()
        rule.onNodeWithText("Online voice").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Download required").performScrollTo().assertIsNotEnabled()
    }
    @Test fun engineConsentCancelKeepsSelectionAndConfirmAdoptsRequestedEngine() {
        val c = controller(); show(c)
        rule.onNodeWithText("Engine", substring = false).performScrollTo().performClick()
        rule.onNodeWithText("Alternative Speech").performClick()
        rule.onNodeWithText("External speech engine").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.runOnIdle { assertEquals(SpeechCatalogExamples.primary, c.discovery.activePackage) }
        rule.onNodeWithText("Engine", substring = false).performScrollTo().performClick()
        rule.onNodeWithText("Alternative Speech").performClick()
        rule.onNodeWithText("Use engine").performClick()
        rule.runOnIdle { assertEquals(SpeechCatalogExamples.secondary, profile.settings.speech.enginePackage) }
    }
    @Test fun playbackConsentCancelDoesNotCreateTransport() {
        val c = controller(); show(c)
        rule.runOnIdle { c.startConversation(profile, profile.chats.single(), "m") }
        rule.onNodeWithText("External speech engine").assertExists()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithTag("speech.transport").assertDoesNotExist()
    }
    @Test fun volumeIsConditionalAndSavedAcrossMixDisable() {
        val c = controller(); show(c)
        rule.onNodeWithText("Speak over playing media").performScrollTo().performClick()
        rule.onNodeWithText("Speech-over-media volume").performScrollTo().performClick()
        rule.onNodeWithText("Quiet").performClick()
        rule.onNodeWithText("Speak over playing media").performScrollTo().performClick()
        rule.onNodeWithText("Speech-over-media volume").assertDoesNotExist()
        rule.runOnIdle { assertEquals(SpeechMixVolume.Quiet, profile.settings.speech.mixVolume) }
    }
    @Test fun discoveryFailureCanRefreshToUsableCatalog() {
        val c = controller(); c.chooseCatalogScenario(SpeechCatalogScenario.Failed); show(c)
        rule.onNodeWithText("Couldn’t check speech engines.").assertExists()
        rule.onNodeWithText("Refresh").performScrollTo().performClick()
        rule.runOnIdle { assertTrue(c.discovery.usable) }
    }
    @Test fun staleNotificationCommandCannotStopNewExampleSession() {
        val c = controller(); show(c, developer = true)
        rule.onNodeWithText("New session").performScrollTo().performClick()
        rule.onNodeWithText("Send previous session command").performScrollTo().performClick()
        rule.onNodeWithTag("speech.example.state").performScrollTo().assertTextContains("Session 2 · Foreground", substring = true)
        rule.runOnIdle { assertNull(c.session) }
    }
}
