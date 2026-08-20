package dev.ipf.whitenoise

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.SharedContentCategory
import dev.ipf.whitenoise.ui.conversation.AddGroupMembersScreen
import dev.ipf.whitenoise.ui.conversation.ChatInfoScreen
import dev.ipf.whitenoise.ui.conversation.ChatRelaysScreen
import dev.ipf.whitenoise.ui.conversation.EditGroupScreen
import dev.ipf.whitenoise.ui.conversation.SharedContentScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatInfoScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun directInfoShowsIdentityQuickActionsAndSharedCategories() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "maya-chen" }
        composeRule.setContent {
            WhiteNoiseTheme {
                ChatInfoScreen(
                    profile = profile,
                    chat = chat,
                    onBack = {},
                    onAbout = {},
                    onMember = {},
                    onSharedContent = {},
                    onRelays = {},
                    onSearch = {},
                    onEditGroup = {},
                    onAddPeople = {},
                    onMute = {},
                    onDisappearing = {},
                    onArchive = {},
                    onLeave = { true },
                )
            }
        }
        composeRule.onNodeWithText("Chat Info").assertIsDisplayed()
        composeRule.onNodeWithText("Shared in Chat").assertIsDisplayed()
    }

    @Test
    fun adminGroupInfoShowsEditAndAddPeople() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "weekend-walks" }
        composeRule.setContent {
            WhiteNoiseTheme {
                ChatInfoScreen(
                    profile = profile,
                    chat = chat,
                    onBack = {},
                    onAbout = {},
                    onMember = {},
                    onSharedContent = {},
                    onRelays = {},
                    onSearch = {},
                    onEditGroup = {},
                    onAddPeople = {},
                    onMute = {},
                    onDisappearing = {},
                    onArchive = {},
                    onLeave = { true },
                )
            }
        }
        composeRule.onNodeWithText("Edit Group").assertIsDisplayed()
        composeRule.onNodeWithText("Add People").assertIsDisplayed()
    }

    @Test
    fun sharedDocumentsUseAuthoritativeAttachmentCards() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-media-rich" }
        composeRule.setContent {
            WhiteNoiseTheme { SharedContentScreen(profile, chat, SharedContentCategory.Documents, {}) }
        }
        composeRule.onNodeWithText("Project Brief.pdf").assertIsDisplayed()
    }

    @Test
    fun relayEditorShowsIndependentExplanationAndRestore() {
        val chat = ProfileFixtures.marmota.chats.first { it.id == "fiatjaf" }
        composeRule.setContent {
            WhiteNoiseTheme { ChatRelaysScreen(chat, {}, { true }, { true }, { true }) }
        }
        composeRule.onNodeWithText("These relays are used only to deliver messages in this chat.").assertIsDisplayed()
        composeRule.onNodeWithText("Restore Default Relays").assertIsDisplayed()
    }

    @Test
    fun groupEditAndAddMembersDestinationsCompileWithNativeControls() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "weekend-walks" }
        composeRule.setContent {
            WhiteNoiseTheme { EditGroupScreen(chat, {}, { _, _, _ -> true }) }
        }
        composeRule.onNodeWithText("Edit Group").assertIsDisplayed()

        composeRule.setContent {
            WhiteNoiseTheme { AddGroupMembersScreen(profile, chat, {}, { true }) }
        }
        composeRule.onNodeWithText("Add People").assertIsDisplayed()
    }
}
