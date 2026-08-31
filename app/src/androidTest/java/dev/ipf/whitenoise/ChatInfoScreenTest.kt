package dev.ipf.whitenoise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ConversationMediaProjection
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.MuteDuration
import dev.ipf.whitenoise.model.SharedContentCategory
import dev.ipf.whitenoise.ui.conversation.AddGroupMembersScreen
import dev.ipf.whitenoise.ui.conversation.ChatInfoScreen
import dev.ipf.whitenoise.ui.conversation.ChatRelaysScreen
import dev.ipf.whitenoise.ui.conversation.EditGroupScreen
import dev.ipf.whitenoise.ui.conversation.SharedContentScreen
import dev.ipf.whitenoise.ui.chats.PersonProfileScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatInfoScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<EmptyTestActivity>()

    @Test fun muteUsesTheSameImmediateSelectionDialogAsChats() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "maya-chen" }.copy(muteDuration = null)
        var chosen: MuteDuration? = null
        composeRule.setContent { WhiteNoiseTheme {
            ChatInfoScreen(profile, chat, {}, {}, {}, {}, {}, {}, {}, {}, { chosen = it }, {}, {}, { true })
        } }
        composeRule.onNodeWithContentDescription("Mute").performClick()
        composeRule.onNodeWithTag("mute.duration.dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.runOnIdle { org.junit.Assert.assertNull(chosen) }
        composeRule.onNodeWithContentDescription("Mute").performClick()
        composeRule.onNodeWithText("1 Week").performClick()
        composeRule.runOnIdle { org.junit.Assert.assertEquals(MuteDuration.OneWeek, chosen) }
    }

    @Test
    fun directInfoShowsIdentityQuickActionsAndSharedCategories() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "maya-chen" }
        var searchOpened = false
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
                    onSearch = { searchOpened = true },
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
        composeRule.onNodeWithContentDescription("About").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Search").assertIsDisplayed().performClick()
        composeRule.runOnIdle { check(searchOpened) }
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
    fun sharedMediaUsesExactFrameViewerAndGoToMessageCallback() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-media-viewer" }
        val selected = ConversationMediaProjection.items(chat, profile).first {
            it.key.attachmentId == "viewer-gallery" && it.key.imageIndex == 1
        }
        var targetMessageId: String? = null
        composeRule.setContent {
            WhiteNoiseTheme {
                SharedContentScreen(
                    profile = profile,
                    chat = chat,
                    category = SharedContentCategory.Media,
                    onBack = {},
                    onGoToMessage = { targetMessageId = it },
                )
            }
        }

        composeRule.onNodeWithTag(
            "conversation.shared.media.${selected.key.stableId}",
        ).performClick()
        composeRule.onNodeWithTag("conversation.media.viewer.position")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Go to Message").performClick()

        composeRule.runOnIdle { org.junit.Assert.assertEquals("MED-VIEW-01", targetMessageId) }
    }

    @Test
    fun sharedMediaPagerUsesTheWholeChatAndUpdatesSenderTimeAndPosition() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-media-gallery" }
        val first = ConversationMediaProjection.items(chat, profile).first()
        composeRule.setContent {
            WhiteNoiseTheme {
                SharedContentScreen(profile, chat, SharedContentCategory.Media, {})
            }
        }

        composeRule.onNodeWithTag(
            "conversation.shared.media.${first.key.stableId}",
        ).performClick()
        composeRule.onNodeWithTag("conversation.media.viewer.sender")
            .assertTextContains("Media - Gallery Layouts")
        composeRule.onNodeWithTag("conversation.media.viewer.position")
            .assertTextContains("Today, 10:00 AM · 1 of 27")

        repeat(2) {
            composeRule.onNodeWithTag("conversation.media.viewer.pager")
                .performTouchInput { swipeLeft() }
            composeRule.waitForIdle()
        }
        composeRule.onNodeWithTag("conversation.media.viewer.sender").assertTextContains("You")
        composeRule.onNodeWithTag("conversation.media.viewer.position")
            .assertTextContains("Today, 10:08 AM · 3 of 27")
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
        var showAddPeople by mutableStateOf(false)
        composeRule.setContent {
            WhiteNoiseTheme {
                if (showAddPeople) {
                    AddGroupMembersScreen(profile, chat, {}, { true })
                } else {
                    EditGroupScreen(chat, {}, { _, _, _ -> true })
                }
            }
        }
        composeRule.onNodeWithText("Edit Group").assertIsDisplayed()

        composeRule.runOnIdle { showAddPeople = true }
        composeRule.onNodeWithText("Name or npub").assertIsDisplayed()
    }

    @Test
    fun disappearingSelectionUsesAVisibleMaterialChoice() {
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

        composeRule.onNodeWithContentDescription("Disappearing").performClick()
        composeRule.onNodeWithText("Disappearing messages").assertIsDisplayed()
        composeRule.onNodeWithText(chat.disappearingDuration.label).assertIsDisplayed()
    }

    @Test
    fun ordinaryGroupMemberDoesNotSeeAdminActions() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-group-member" }
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

        composeRule.onNodeWithText("Edit Group").assertIsNotDisplayed()
        composeRule.onNodeWithText("Add People").assertIsNotDisplayed()
    }

    @Test
    fun memberProfileSeparatesRelationshipAndGroupActions() {
        val profile = ProfileFixtures.marmota
        val person = profile.people.first { it.id == "maya-chen" }
        composeRule.setContent {
            WhiteNoiseTheme {
                PersonProfileScreen(
                    profile = profile,
                    person = person,
                    onBack = {},
                    onMessage = { true },
                    onToggleFollow = {},
                    onToggleBlock = {},
                    showMessageAction = false,
                    groupRole = dev.ipf.whitenoise.model.GroupRole.Member,
                    canManageGroup = true,
                )
            }
        }

        composeRule.onNodeWithText("Profile Actions").assertIsDisplayed()
        composeRule.onNodeWithText("Group Actions").assertIsDisplayed()
        composeRule.onNodeWithText("Remove from Group").assertIsDisplayed()
    }

    @Test
    fun emptyRelayConfigurationExplainsHistoryPreservingRecovery() {
        val chat = ProfileFixtures.marmota.chats.first { it.id == "fiatjaf" }.copy(relayUrls = emptyList())
        composeRule.setContent {
            WhiteNoiseTheme { ChatRelaysScreen(chat, {}, { true }, { true }, { true }) }
        }

        composeRule.onNodeWithText("No chat relays").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Add a relay to send new messages. Your history remains available.",
        ).assertIsDisplayed()
    }
}
