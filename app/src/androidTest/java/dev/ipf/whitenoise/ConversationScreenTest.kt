package dev.ipf.whitenoise

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.ui.conversation.ConversationScreen
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
    }

    @Test
    fun deterministicLinkPreviewRendersWithoutNetwork() {
        setConversation("catalog-composer-link-preview")

        composeRule.onNodeWithText("Apple Developer").assertIsDisplayed()
    }

    @Test
    fun emptyComposerExposesNamedVoiceRecordingAction() {
        setConversation("fiatjaf")

        composeRule.onNodeWithContentDescription("Record Voice Message").assertIsDisplayed()
    }

    @Test
    fun recipientVoiceFixtureShowsPlaybackAndTranscriptActions() {
        setConversation("catalog-voice")

        composeRule.onNodeWithText("Play").assertIsDisplayed()
        composeRule.onNodeWithText("Show Transcript").assertIsDisplayed()
    }

    @Test
    fun conversationSearchStaysInPlaceAndReplacesComposerControls() {
        setConversation("catalog-direct-text")

        composeRule.onNodeWithContentDescription("Search Messages").performClick()
        composeRule.onNodeWithText("Messages").assertIsDisplayed()
        composeRule.onNodeWithText("0 matches").assertIsDisplayed()
    }

    @Test
    fun longPressOpensTheDiscoverableMessageActionSheet() {
        setConversation("fiatjaf")

        composeRule.onNodeWithText("Portable identity for the win.").performTouchInput { longClick() }
        composeRule.onNodeWithText("Reply").assertIsDisplayed()
        composeRule.onNodeWithText("Forward").assertIsDisplayed()
        composeRule.onNodeWithText("Info").assertIsDisplayed()
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
