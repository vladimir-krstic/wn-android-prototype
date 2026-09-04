package dev.ipf.whitenoise

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.conversation.FullConversationComposer
import dev.ipf.whitenoise.ui.settings.AppearanceScreen
import dev.ipf.whitenoise.ui.settings.ActionColorScreen
import dev.ipf.whitenoise.ui.settings.AppLocale
import dev.ipf.whitenoise.ui.settings.ChatBubbleColorsScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppearanceInputInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private var profile by mutableStateOf(ProfileFixtures.marmota)
    private var chat by mutableStateOf(ProfileFixtures.marmota.chats.first { it.id == "fiatjaf" }.copy(draftText = "",draftAttachments = emptyList()))
    private val sent = mutableListOf<String>()
    private fun composer(behavior: EnterKeyBehavior) {
        profile = profile.copy(settings = profile.settings.copy(enterKeyBehavior = behavior))
        compose.setContent { WhiteNoiseTheme {
            Box(Modifier.fillMaxSize(),contentAlignment = Alignment.BottomCenter) {
                FullConversationComposer(profile, chat,
                    onDraftTextChanged = { chat = chat.copy(draftText = it) }, onAddAttachments = {}, onRemoveAttachment = {},
                    onSuppressLink = {}, onCancelReply = {}, onSendDraft = {
                        if (chat.draftText.isBlank()) false else { sent += chat.draftText; chat = chat.copy(draftText = ""); true }
                    }, onSendVoice = { false })
            }
        } }
    }
    @Test fun appearanceRetainsIndependentSettingsAndAppliesSizeAndTheme() {
        compose.setContent { WhiteNoiseTheme(appearance = profile.settings.appearance,fontSize = profile.settings.fontSize) {
            AppearanceScreen(profile,{}, { profile = profile.copy(settings = it) },{})
        } }
        compose.onNodeWithText("AMOLED").performScrollTo().performClick()
        compose.onNodeWithText("Font size").performScrollTo().performClick()
        compose.onNodeWithText("Extra large").performClick()
        compose.runOnIdle {
            assertEquals(AppearancePreference.Amoled,profile.settings.appearance)
            assertEquals(AppFontSize.ExtraLarge,profile.settings.fontSize)
            assertEquals(EnterKeyBehavior.NewLine,profile.settings.enterKeyBehavior)
        }
    }
    @Test fun optionalTypefaceCanBeChangedAndRestoredToSystem() {
        compose.setContent { WhiteNoiseTheme(fontFamily = profile.settings.fontFamily) {
            AppearanceScreen(profile,{}, { profile = profile.copy(settings = it) },{})
        } }
        compose.onNodeWithText("App font").performScrollTo().performClick()
        compose.onNodeWithText("Outfit").performClick()
        compose.runOnIdle { assertEquals(AppFontFamily.Outfit,profile.settings.fontFamily) }
        compose.onNodeWithText("App font").performScrollTo().performClick()
        compose.onNodeWithText("System").performClick()
        compose.runOnIdle { assertEquals(AppFontFamily.System,profile.settings.fontFamily) }
    }
    @Test fun dismissingEnterChoiceIsInertAndSelectionIsImmediate() {
        compose.setContent { WhiteNoiseTheme { AppearanceScreen(profile,{}, { profile = profile.copy(settings = it) },{}) } }
        compose.onNodeWithText("Enter key behavior").performScrollTo().performClick()
        compose.onNodeWithText("Cancel").performClick()
        compose.runOnIdle { assertEquals(EnterKeyBehavior.NewLine,profile.settings.enterKeyBehavior) }
        compose.onNodeWithText("Enter key behavior").performClick()
        compose.onNodeWithText("Send message").performClick()
        compose.runOnIdle { assertEquals(EnterKeyBehavior.SendMessage,profile.settings.enterKeyBehavior) }
    }
    @Test fun softwareSendUsesExistingSubmissionAndClearsDraftOnce() {
        composer(EnterKeyBehavior.SendMessage)
        val editor = compose.onNodeWithTag("conversation.composer.editor")
        editor.performTextInput("Hello"); editor.performImeAction()
        compose.runOnIdle { assertEquals(listOf("Hello"),sent); assertEquals("",chat.draftText) }
        editor.performImeAction()
        compose.runOnIdle { assertEquals(1,sent.size) }
    }
    @Test fun hardwareEnterAndNumpadEnterSubmitWithoutNewline() {
        composer(EnterKeyBehavior.SendMessage)
        val editor = compose.onNodeWithTag("conversation.composer.editor")
        editor.performTextInput("One"); editor.performKeyInput { pressKey(Key.Enter) }
        editor.performTextInput("Two"); editor.performKeyInput { pressKey(Key.NumPadEnter) }
        compose.runOnIdle { assertEquals(listOf("One","Two"),sent) }
    }
    @Test fun shiftEnterRemainsNewlineInSendMode() {
        composer(EnterKeyBehavior.SendMessage)
        val editor = compose.onNodeWithTag("conversation.composer.editor")
        editor.performTextInput("Line")
        editor.performKeyInput { keyDown(Key.ShiftLeft); pressKey(Key.Enter); keyUp(Key.ShiftLeft) }
        compose.runOnIdle { assertTrue(sent.isEmpty()); assertEquals("Line\n",chat.draftText) }
    }
    @Test fun defaultEnterAndPastedMultilineNeverTriggerSend() {
        composer(EnterKeyBehavior.NewLine)
        val editor = compose.onNodeWithTag("conversation.composer.editor")
        editor.performTextInput("First\nSecond"); editor.performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertTrue(sent.isEmpty()); assertEquals("First\nSecond\n",chat.draftText) }
    }
    @Test fun actionColorCanUseAFullSpectrumHexAndReset() {
        compose.setContent { WhiteNoiseTheme(colors = profile.settings.colors) {
            ActionColorScreen(profile,{}, { profile = profile.copy(settings = it) })
        } }
        compose.onNodeWithText("Hex color").performScrollTo().performTextClearance()
        compose.onNodeWithText("Hex color").performTextInput("#336699")
        compose.onNodeWithText("Apply color").performClick()
        compose.runOnIdle { assertEquals(0xFF336699L, profile.settings.colors.light.actionArgb) }
        compose.onNodeWithText("Reset to default").performScrollTo().performClick()
        compose.runOnIdle { assertNull(profile.settings.colors.light.actionArgb) }
    }
    @Test fun chatBubbleOverrideDoesNotMutateGlobalColor() {
        val global = profile.settings.colors.updateTheme(AppearanceColorTheme.Light) { it.copy(mineBubbleArgb = 0xFF1D4ED8L) }
        profile = profile.copy(settings = profile.settings.copy(appearance = AppearancePreference.Light, colors = global))
        compose.setContent { WhiteNoiseTheme(appearance = AppearancePreference.Light, colors = profile.settings.colors) {
            ChatBubbleColorsScreen(profile,chat,{}, { profile = profile.copy(settings = it) }, { chat = chat.copy(bubbleColors = it) })
        } }
        compose.onNodeWithContentDescription("Color #B91C1C").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(0xFFB91C1CL, chat.bubbleColors.mineArgb)
            assertEquals(0xFF1D4ED8L, profile.settings.colors.light.mineBubbleArgb)
        }
        compose.onNodeWithText("Reset to global colors").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(ChatBubbleColorOverrides(), chat.bubbleColors) }
    }

    @Test fun selectedLocaleChangesAppOwnedResourcesImmediately() {
        compose.setContent { AppLocale(LanguagePreference.Russian) { WhiteNoiseTheme {
            AppearanceScreen(profile, {}, { profile = profile.copy(settings = it) }, {})
        } } }
        compose.onNodeWithText("Внешний вид").assertExists()
        compose.onNodeWithText("Тема").assertExists()
    }
}
