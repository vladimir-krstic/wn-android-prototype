package dev.ipf.whitenoise

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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
import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatMembership
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
            ChatInfoScreen(profile, chat, {}, {}, {}, {}, {}, {}, {}, {}, { chosen = it }, {}, {}, { true }, {})
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
                    onDeveloperTools = {},
                )
            }
        }
        composeRule.onNodeWithTag("chat_info.name").assertTextContains("Maya Chen").assertIsDisplayed()
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
                    onDeveloperTools = {},
                )
            }
        }
        composeRule.onNodeWithTag("chat_info.list").performScrollToNode(hasText("Edit Group"))
        composeRule.onNodeWithText("Edit Group").assertIsDisplayed()
        composeRule.onNodeWithText("Add People").assertIsDisplayed()
    }

    @Test
    fun directIdentityCopiesTheFullKeyAndOpensChatDeveloperTools() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "maya-chen" }
        val person = profile.people.first { it.id == "maya-chen" }
        var developerToolsOpened = false
        composeRule.setContent {
            WhiteNoiseTheme { InfoUnderTest(chat, onDeveloperTools = { developerToolsOpened = true }) }
        }
        composeRule.onNodeWithContentDescription("Verified").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_info.copy_public_key").performClick()
        composeRule.runOnIdle {
            val clipboard = composeRule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals(person.publicKey, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
        composeRule.onNodeWithTag("chat_info.list").performScrollToNode(hasText("Developer Tools"))
        composeRule.onNodeWithText("Developer Tools").assertHasClickAction().performClick()
        composeRule.runOnIdle { assertTrue(developerToolsOpened) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun groupSectionsKeepIosOrderAndSelfIsNotAProfileAction() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "weekend-walks" }
        var openedMember: String? = null
        composeRule.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(412.dp, 2400.dp))) {
                WhiteNoiseTheme { InfoUnderTest(chat, onMember = { openedMember = it }) }
            }
        }
        val expectedOrder = listOf(
            chat.description, "${chat.members.size} members", "Shared in Chat", "Photos & Videos",
            "Links", "Documents", "Advanced", "Relays", "Developer Tools", "Members",
            "Edit Group", "Add People", "Archive", "Leave Group",
        ).filter(String::isNotBlank)
        val rowTops = expectedOrder.map { label ->
            composeRule.onNodeWithText(label).assertIsDisplayed().getUnclippedBoundsInRoot().top
        }
        assertTrue("Info sections must retain the iOS product order", rowTops.zipWithNext().all { (a, b) -> a < b })
        val self = composeRule.onNodeWithTag("chat_info.member.${profile.id}")
        self.assertHasNoClickAction()
        val otherMember = chat.members.first { it.personId != profile.id }
        val memberRow = composeRule.onNodeWithTag("chat_info.member.${otherMember.personId}")
        assertTrue(memberRow.getUnclippedBoundsInRoot().top < composeRule.onNodeWithText("Edit Group").getUnclippedBoundsInRoot().top)
        memberRow.assertHasClickAction().performClick()
        composeRule.runOnIdle { assertEquals(otherMember.personId, openedMember) }
    }

    @Test
    fun endedGroupKeepsHistoryActionsWithoutManagementOrLeaveAtLargeRtlText() {
        val chat = ProfileFixtures.marmota.chats.first { it.id == "weekend-walks" }.copy(
            membership = ChatMembership.Left,
            isArchived = true,
        )
        var unarchived = false
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhiteNoiseTheme(appearance = dev.ipf.whitenoise.model.AppearancePreference.Dark) { InfoUnderTest(chat, onArchive = { unarchived = true }) }
            }
        }
        composeRule.onNodeWithTag("chat_info.list").performScrollToNode(hasText("Unarchive"))
        composeRule.onNodeWithText("Unarchive").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Edit Group").assertDoesNotExist()
        composeRule.onNodeWithText("Add People").assertDoesNotExist()
        composeRule.onNodeWithText("Leave Group").assertDoesNotExist()
        composeRule.runOnIdle { assertTrue(unarchived) }
    }

    @Composable
    private fun InfoUnderTest(
        chat: Chat,
        onDeveloperTools: () -> Unit = {},
        onMember: (String) -> Unit = {},
        onArchive: () -> Unit = {},
    ) {
        ChatInfoScreen(
            profile = ProfileFixtures.marmota,
            chat = chat,
            onBack = {},
            onAbout = {},
            onMember = onMember,
            onSharedContent = {},
            onRelays = {},
            onSearch = {},
            onEditGroup = {},
            onAddPeople = {},
            onMute = {},
            onDisappearing = {},
            onArchive = onArchive,
            onLeave = { true },
            onDeveloperTools = onDeveloperTools,
        )
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
                    onDeveloperTools = {},
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
                    onDeveloperTools = {},
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
