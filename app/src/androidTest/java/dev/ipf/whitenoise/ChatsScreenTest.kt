package dev.ipf.whitenoise

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.state.AppUiState
import dev.ipf.whitenoise.ui.chats.ChatsScreen
import dev.ipf.whitenoise.ui.chats.NewChatScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun populatedChatsExposeCatalogAndNativeCreationAction() {
        val profile = ProfileFixtures.marmota
        composeRule.setContent {
            WhiteNoiseTheme {
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
                )
            }
        }

        composeRule.onNodeWithText("Direct - Text & Delivery").assertIsDisplayed()
        composeRule.onNodeWithText("New Message").assertIsDisplayed()
    }

    @Test
    fun newMessageStartsWithGroupThenPeople() {
        composeRule.setContent {
            WhiteNoiseTheme {
                NewChatScreen(
                    profile = ProfileFixtures.marmota,
                    onBack = {},
                    onNewGroup = {},
                    onPerson = {},
                )
            }
        }

        composeRule.onNodeWithText("New Group").assertIsDisplayed()
        composeRule.onNodeWithText("People").assertIsDisplayed()
    }
}
