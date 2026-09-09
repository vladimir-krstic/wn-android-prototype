package dev.ipf.whitenoise

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.ui.components.BackgroundKeyboardDismissalHost
import dev.ipf.whitenoise.ui.conversation.ComposerKeyboardInsets
import dev.ipf.whitenoise.ui.conversation.ConversationScreen
import dev.ipf.whitenoise.ui.conversation.LocalComposerKeyboardInsets
import dev.ipf.whitenoise.ui.settings.IncognitoKeyboardScope
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises real pointer passes and input focus with a controlled IME lifecycle. */
@OptIn(ExperimentalLayoutApi::class)
@RunWith(AndroidJUnit4::class)
class ComposerKeyboardDismissalFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val ime = MutableWindowInsets()
    private val source = MutableWindowInsets()
    private val target = MutableWindowInsets()
    private var hides = 0
    private val keyboard = object : SoftwareKeyboardController {
        override fun show() = Unit
        override fun hide() { hides++; target.insets = WindowInsets(bottom = 0) }
    }

    private fun show() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-direct-text" }
            .copy(draftText = "", draftAttachments = emptyList(), draftReplyMessageId = null)
        rule.setContent {
            CompositionLocalProvider(
                LocalSoftwareKeyboardController provides keyboard,
                LocalComposerKeyboardInsets provides ComposerKeyboardInsets(ime, source, target),
            ) {
                IncognitoKeyboardScope(enabled = false, blocked = true) {
                    WhiteNoiseTheme {
                        Box(Modifier.fillMaxWidth().height(600.dp)) {
                            BackgroundKeyboardDismissalHost(ime, source, target) {
                                Column {
                                    Spacer(Modifier.fillMaxWidth().height(40.dp).testTag("outside.editor"))
                                    Box(Modifier.weight(1f)) {
                                        ConversationScreen(profile, chat, {}, { true }, {}, {}, {})
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        rule.onNodeWithTag("conversation.composer.editor").performTouchInput { click() }
        rule.onNodeWithTag("conversation.composer.editor").assertIsFocused()
        rule.runOnIdle {
            val open = WindowInsets(bottom = with(rule.density) { 240.dp.roundToPx() })
            ime.insets = open; source.insets = open; target.insets = open
        }
    }

    @Test fun backgroundDismissalRetainsInputFocusUntilLastKeyboardFrameCompletes() {
        show()
        rule.onNodeWithTag("outside.editor").performTouchInput { click() }
        rule.onNodeWithTag("conversation.composer.editor").assertIsFocused()
        rule.runOnIdle { assertEquals(1, hides); ime.insets = WindowInsets(bottom = 0) }
        // A zero current inset alone is not animation completion.
        rule.onNodeWithTag("conversation.composer.editor").assertIsFocused()
        rule.runOnIdle { source.insets = WindowInsets(bottom = 0) }
        rule.onNodeWithTag("conversation.composer.editor").assertIsNotFocused()
    }

    @Test fun editorRetapIsConsumedBeforeBackgroundObserverAndCancelsPendingFocusClear() {
        show()
        rule.runOnIdle { assertEquals(0, hides) }
        rule.onNodeWithTag("outside.editor").performTouchInput { click() }
        rule.onNodeWithTag("conversation.composer.editor").performTouchInput { click() }
        rule.runOnIdle {
            assertEquals("An editor tap must not be treated as a background dismissal", 1, hides)
            ime.insets = WindowInsets(bottom = 0)
            source.insets = WindowInsets(bottom = 0)
        }
        rule.onNodeWithTag("conversation.composer.editor").assertIsFocused()
    }
}
