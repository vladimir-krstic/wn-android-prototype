package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.chats.ChatsScreen
import dev.ipf.whitenoise.ui.conversation.ConversationScreen
import dev.ipf.whitenoise.ui.settings.SupportScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AdaptiveLayoutTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun settingsSurfaceKeepsPrimaryContentAtLargeTextAndRtl() {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhiteNoiseTheme { SupportScreen(ProfileFixtures.marmota, {}, {}, {}) }
            }
        }

        composeRule.onNodeWithText("Chat with support").assertIsDisplayed()
        composeRule.onNodeWithText("Start Chat").assertIsDisplayed()
    }

    @Test
    fun chatsSurfaceConstrainsContentAtExpandedWidth() {
        val profile = ProfileFixtures.marmota
        composeRule.setContent {
            WhiteNoiseTheme {
                Box(Modifier.requiredWidth(900.dp)) {
                    ChatsScreen(
                        uiState = AppUiState(listOf(profile), profile.id, setOf(profile.id)),
                        onSelectProfile = {},
                        onAddProfile = {},
                        onNewMessage = {},
                        onOpenChat = {},
                        onMarkUnread = { _, _ -> },
                        onReadAll = {},
                        onTogglePin = {},
                        onMute = { _, _ -> },
                        onArchive = { _, _ -> },
                        onLeave = { false },
                        onDelete = {},
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Chats").assertIsDisplayed()
        composeRule.onNodeWithText("Direct - Text & Delivery").assertIsDisplayed()
    }

    @Test
    fun conversationSurfaceKeepsTitleAtLargeTextAndRtl() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "fiatjaf" }
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhiteNoiseTheme {
                    ConversationScreen(profile, chat, {}, { true }, {}, {}, {})
                }
            }
        }

        composeRule.onNodeWithText("Fiatjaf").assertIsDisplayed()
    }
}
