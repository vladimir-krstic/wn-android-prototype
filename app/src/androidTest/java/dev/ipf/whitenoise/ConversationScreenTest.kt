package dev.ipf.whitenoise

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ChatTimelineEntry
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.ui.conversation.ConversationScreen
import dev.ipf.whitenoise.ui.conversation.MessageDetailsScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun directConversationShowsSharedTimelineAndComposer() {
        setConversation("fiatjaf")

        composeRule.onNodeWithText("Fiatjaf").assertIsDisplayed()
        composeRule.onNodeWithText("Portable identity for the win.").assertIsDisplayed()
        composeRule.onNodeWithText("Message").assertIsDisplayed()
    }

    @Test
    fun groupConversationShowsMemberSubtitleAndAuthors() {
        setConversation("weekend-walks")

        composeRule.onNodeWithText("Weekend Walks").assertIsDisplayed()
        composeRule.onNodeWithText("Maya Chen").assertIsDisplayed()
    }

    @Test
    fun invitationReplacesComposerWithExplicitDecisions() {
        setConversation("catalog-direct-invitation")

        composeRule.onNodeWithText("Decline").assertIsDisplayed()
        composeRule.onNodeWithText("Accept").assertIsDisplayed()
    }

    @Test
    fun endedConversationKeepsHistoryAndMembershipStatus() {
        setConversation("catalog-group-removed")

        composeRule.onNodeWithText("You were removed from this group.").assertIsDisplayed()
    }

    @Test
    fun photoAlbumDraftUsesTheSharedComposerShelf() {
        setConversation("catalog-composer-photo-album")

        composeRule.onNodeWithText("A few from today.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("4 draft attachments").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Badger").performClick()
        composeRule.onNodeWithText("Preview").assertIsDisplayed()
        composeRule.onNodeWithText("Included").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("2 of 4").assertIsDisplayed()
    }

    @Test
    fun deterministicLinkPreviewRendersWithoutNetwork() {
        setConversation("catalog-composer-link-preview")

        composeRule.onNodeWithText("Apple Developer").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Remove Link Preview").assertIsDisplayed()
    }

    @Test
    fun composerAttachmentMenuUsesNamedMaterialActions() {
        setConversation("fiatjaf")

        composeRule.onNodeWithContentDescription("Add Attachment").performClick()
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
        composeRule.onNodeWithText("Photos and videos").assertIsDisplayed()
        composeRule.onNodeWithText("File").assertIsDisplayed()
        composeRule.onNodeWithText("Contact").assertIsDisplayed()
        composeRule.onNodeWithText("GIF").assertIsDisplayed()
    }

    @Test
    fun emptyComposerExposesNamedVoiceRecordingAction() {
        setConversation("fiatjaf")

        composeRule.onNodeWithContentDescription("Record Voice Message").assertIsDisplayed()
    }

    @Test
    fun voiceRecordingReviewExposesTranscriptionAndFormatChoice() {
        setConversation("fiatjaf")

        composeRule.onNodeWithContentDescription("Record Voice Message").performClick()
        composeRule.onNodeWithText("Stop Recording").performClick()
        composeRule.onNodeWithText("Voice Message Review").assertIsDisplayed()
        composeRule.onNodeWithText("Transcribe").performClick()
        composeRule.onNodeWithText("Message Format").assertIsDisplayed()
        composeRule.onNodeWithText("Both").assertIsDisplayed()
        composeRule.onNodeWithText("Send Voice and Text Message").assertIsDisplayed()
        composeRule.onNodeWithText("Message Format").performClick()
        val radioItem = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        composeRule.onNode(hasText("Both").and(radioItem)).assertIsSelected()
        composeRule.onNode(hasText("Text").and(radioItem)).performClick()
        composeRule.onNodeWithText("Send Text Message").assertIsDisplayed()
        composeRule.onNodeWithText("Message Format").performClick()
        composeRule.onNode(hasText("Text").and(radioItem)).assertIsSelected().performClick()
    }

    @Test
    fun recipientVoiceFixtureShowsPlaybackAndTranscriptActions() {
        setConversation("catalog-voice")

        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeRule.onNodeWithText("Transcribe").assertIsDisplayed()
        composeRule.onNodeWithText("Show Transcript").assertIsDisplayed()
    }

    @Test
    fun conversationSearchStaysInPlaceAndReplacesComposerControls() {
        setConversation("catalog-direct-text")

        composeRule.onNodeWithContentDescription("Search Messages").performClick()
        composeRule.onNodeWithContentDescription("Search Messages").assertIsDisplayed()
        composeRule.onNodeWithText("0 matches").assertIsDisplayed()
    }

    @Test
    fun conversationSearchExposesClearAndNamedResultNavigation() {
        setConversation("catalog-direct-text")

        composeRule.onNodeWithContentDescription("Search Messages").performClick()
        composeRule.onNodeWithContentDescription("Search Messages").performTextInput("Failed outgoing")
        composeRule.onNodeWithText("1 of 1 match").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear search").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Previous Match").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Next Match").assertIsDisplayed()
    }

    @Test
    fun longPressOpensTheDiscoverableMessageActionSheet() {
        setConversation("fiatjaf")

        composeRule.onNodeWithText("Portable identity for the win.").performTouchInput { longClick() }
        composeRule.onNodeWithText("Message Actions").assertIsDisplayed()
        composeRule.onNodeWithText("More Reactions").assertIsDisplayed()
        composeRule.onNodeWithText("Reply").assertIsDisplayed()
        composeRule.onNodeWithText("Forward").assertIsDisplayed()
        composeRule.onNodeWithText("Info").assertIsDisplayed()
    }

    @Test
    fun messageSelectionUsesNamedTopAndBottomControls() {
        setConversation("fiatjaf")

        composeRule.onNodeWithText("Portable identity for the win.").performTouchInput { longClick() }
        composeRule.onNodeWithText("Select").performClick()
        composeRule.onAllNodesWithText("1 Selected")[0].assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close Selection").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Delete Selected Messages").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Forward Selected Messages").assertIsDisplayed()
    }

    @Test
    fun forwardingUsesSearchAndAnExplicitSelectionLimit() {
        setConversation("fiatjaf")

        composeRule.onNodeWithText("Portable identity for the win.").performTouchInput { longClick() }
        composeRule.onNodeWithText("Forward").performClick()
        composeRule.onNodeWithText("Select up to 5 chats.").assertIsDisplayed()
        composeRule.onNodeWithText("Search Chats").assertIsDisplayed()
    }

    @Test
    fun reactionConfigurationKeepsNamedSlotsAndActions() {
        setConversation("fiatjaf")

        composeRule.onNodeWithText("Portable identity for the win.").performTouchInput { longClick() }
        composeRule.onNodeWithText("More Reactions").performClick()
        composeRule.onNodeWithContentDescription("Configure Reactions").performClick()
        composeRule.onNodeWithText("Tap an emoji to replace it.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reaction 1, ❤. Double tap to replace.").assertIsDisplayed()
        composeRule.onNodeWithText("Reset").assertIsDisplayed()
        composeRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun messageDetailsGroupsMessageAndDeliveryInformation() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-direct-text" }
        val message = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>()
            .first { it.message.id == "DLV-03" }
            .message
        composeRule.setContent {
            WhiteNoiseTheme {
                MessageDetailsScreen(profile, chat, message, onBack = {})
            }
        }

        composeRule.onNodeWithText("Message Details").assertIsDisplayed()
        composeRule.onNodeWithText("DLV-03: Failed outgoing message").assertIsDisplayed()
        composeRule.onNodeWithText("Not Delivered").assertIsDisplayed()
    }

    @Test
    fun groupMentionDraftShowsOnlyMatchingMembers() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-composer-mention" }.copy(draftText = "@Ma")
        composeRule.setContent {
            WhiteNoiseTheme {
                ConversationScreen(profile, chat, {}, { true }, {}, {}, {})
            }
        }

        composeRule.onNodeWithText("Maya Chen").assertIsDisplayed()
    }

    @Test
    fun conversationIdentityOpensTheEstablishedInfoDestination() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "fiatjaf" }
        var infoOpened = false
        composeRule.setContent {
            WhiteNoiseTheme {
                ConversationScreen(
                    profile = profile,
                    chat = chat,
                    onBack = {},
                    onSend = { true },
                    onRetry = {},
                    onAcceptInvitation = {},
                    onDeclineInvitation = {},
                    onOpenChatInfo = { infoOpened = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Fiatjaf").performClick()
        composeRule.runOnIdle { check(infoOpened) }
    }

    @Test
    fun missingRelaysOfferDirectRecoveryThroughChatInfo() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-direct-missing-relays" }
        var infoOpened = false
        composeRule.setContent {
            WhiteNoiseTheme {
                ConversationScreen(
                    profile = profile,
                    chat = chat,
                    onBack = {},
                    onSend = { true },
                    onRetry = {},
                    onAcceptInvitation = {},
                    onDeclineInvitation = {},
                    onOpenChatInfo = { infoOpened = true },
                )
            }
        }

        composeRule.onNodeWithText("Chat Relays Required").assertIsDisplayed()
        composeRule.onNodeWithText("Check Chat Relays").performClick()
        composeRule.runOnIdle { check(infoOpened) }
    }

    @Test
    fun failedMessageKeepsVisibleNamedRetryRecovery() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-direct-text" }
        var retriedMessageId: String? = null
        composeRule.setContent {
            WhiteNoiseTheme {
                ConversationScreen(
                    profile = profile,
                    chat = chat,
                    onBack = {},
                    onSend = { true },
                    onRetry = { retriedMessageId = it },
                    onAcceptInvitation = {},
                    onDeclineInvitation = {},
                )
            }
        }

        composeRule.onNodeWithText("Not delivered, tap to retry").performClick()
        composeRule.runOnIdle { check(retriedMessageId == "DLV-03") }
    }

    @Test
    fun supportGuidanceRemainsTimelineInformationRatherThanAMessage() {
        setConversation("white-noise-support")

        composeRule.onNodeWithText(
            "How can we help? Ask a question, report a problem, or share a suggestion. We’ll reply here.",
        ).assertIsDisplayed()
    }

    private fun setConversation(chatId: String) {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == chatId }
        composeRule.setContent {
            WhiteNoiseTheme {
                ConversationScreen(
                    profile = profile,
                    chat = chat,
                    onBack = {},
                    onSend = { true },
                    onRetry = {},
                    onAcceptInvitation = {},
                    onDeclineInvitation = {},
                )
            }
        }
    }
}
