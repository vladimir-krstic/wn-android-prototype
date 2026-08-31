package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.ipf.whitenoise.model.ChatTimelineEntry
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.VoiceDraftSubmission
import dev.ipf.whitenoise.ui.conversation.ConversationScreen
import dev.ipf.whitenoise.ui.conversation.MessageDetailsScreen
import dev.ipf.whitenoise.ui.conversation.SearchHighlightedText
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import kotlin.math.abs
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<EmptyTestActivity>()

    @Test
    fun directConversationShowsSharedTimelineAndComposer() {
        setConversation("fiatjaf")

        composeRule.onNodeWithText("Fiatjaf").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Search Messages").assertDoesNotExist()
        composeRule.onNodeWithText("Portable identity for the win.").assertIsDisplayed()
        composeRule.onNodeWithText("Message").assertIsDisplayed()
    }

    @Test
    fun groupConversationShowsMemberSubtitleAndAuthors() {
        setConversation("weekend-walks")

        composeRule.onNodeWithText("Weekend Walks").assertIsDisplayed()
        composeRule.onAllNodesWithText("Maya Chen")[0].assertIsDisplayed()
        val avatar = composeRule.onNodeWithTag(
            "conversation.header.avatar",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val text = composeRule.onNodeWithTag(
            "conversation.header.text",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val title = composeRule.onNodeWithTag(
            "conversation.header.title",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val metadata = composeRule.onNodeWithTag(
            "conversation.header.metadata",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        assertTrue(abs(avatar.center.y - text.center.y) < 1f)
        assertTrue(metadata.top < title.bottom)
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

        composeRule.onAllNodesWithText("A few from today.")[0].assertIsDisplayed()
        composeRule.onNodeWithContentDescription("4 draft attachments").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Badger").performClick()
        composeRule.onNodeWithText("Preview").assertIsDisplayed()
        composeRule.onNodeWithText("Included").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("2 of 4").assertIsDisplayed()
    }

    @Test
    fun horizontalAttachmentShelfGestureDoesNotExpandTheComposer() {
        setConversation("catalog-composer-photo-album")
        val before = composeRule.onNodeWithTag("conversation.composer.surface")
            .fetchSemanticsNode().boundsInRoot.height

        composeRule.onNodeWithTag("conversation.composer.attachments")
            .performTouchInput { swipeLeft() }

        val after = composeRule.onNodeWithTag("conversation.composer.surface")
            .fetchSemanticsNode().boundsInRoot.height
        assertTrue(abs(before - after) < 1f)
    }

    @Test
    fun deterministicLinkPreviewRendersWithoutNetwork() {
        setConversation("catalog-composer-link-preview")

        composeRule.onNodeWithText("Apple Developer").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Remove Link Preview").assertIsDisplayed()
    }

    @Test
    fun allTwelveDeterministicComposerFixturesRenderInTheSharedHost() {
        val profile = ProfileFixtures.marmota
        val ids = listOf(
            "catalog-composer-text",
            "catalog-composer-multiline",
            "catalog-composer-link",
            "catalog-composer-link-preview",
            "catalog-composer-photo",
            "catalog-composer-photo-album",
            "catalog-composer-mixed-media",
            "catalog-composer-file",
            "catalog-composer-gif",
            "catalog-composer-contact",
            "catalog-composer-reply",
            "catalog-composer-mention",
        )
        val current = mutableStateOf(profile.chats.first { it.id == ids.first() })
        composeRule.setContent {
            WhiteNoiseTheme {
                ConversationScreen(
                    profile = profile,
                    chat = current.value,
                    onBack = {},
                    onSend = { true },
                    onRetry = {},
                    onAcceptInvitation = {},
                    onDeclineInvitation = {},
                )
            }
        }

        ids.forEach { id ->
            composeRule.runOnIdle { current.value = profile.chats.first { it.id == id } }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("conversation.composer.host").assertIsDisplayed()
            composeRule.onNodeWithTag("conversation.composer.surface").assertIsDisplayed()
        }
    }

    @Test
    fun composerAttachmentMenuUsesNamedMaterialActions() {
        setConversation("fiatjaf")

        composeRule.onNodeWithContentDescription("Add Attachment").performClick()
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
        composeRule.onNodeWithText("Photos and videos").assertIsDisplayed()
        composeRule.onNodeWithText("Files").assertIsDisplayed()
        composeRule.onNodeWithText("Contact").assertIsDisplayed()
        composeRule.onNodeWithText("GIF").assertDoesNotExist()

        val add = composeRule.onNodeWithTag("conversation.attachment.add").fetchSemanticsNode()
        val menu = composeRule.onNodeWithTag("conversation.attachment.menu").fetchSemanticsNode()
        val expectedGap = 10.dp.value * composeRule.density.density
        val actualGap = add.positionOnScreen.y - menu.positionOnScreen.y - menu.size.height
        assertTrue(abs(actualGap - expectedGap) < 1.5f)
    }

    @Test
    fun composerAttachmentMenuKeepsEditorFocusAndKeyboard() {
        setConversation("fiatjaf")

        composeRule.onNodeWithTag("conversation.composer.editor").performClick().assertIsFocused()
        composeRule.waitUntil(timeoutMillis = 5_000) { isImeVisible() }

        composeRule.onNodeWithContentDescription("Add Attachment").performClick()

        composeRule.onNodeWithTag("conversation.composer.editor").assertIsFocused()
        composeRule.onNodeWithTag("conversation.attachment.menu").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 5_000) { isImeVisible() }
    }

    @Test
    fun emptyComposerExposesNamedVoiceRecordingAction() {
        setConversation("fiatjaf")

        composeRule.onNodeWithContentDescription("Record Voice Message").assertIsDisplayed()
        composeRule.onNodeWithTag("conversation.voice.icon")
            .assertWidthIsEqualTo(24.dp)
            .assertHeightIsEqualTo(24.dp)
    }

    @Test
    fun voiceRecordingReviewExposesTranscriptionAndFormatChoice() {
        setConversation("fiatjaf")

        composeRule.onNodeWithContentDescription("Record Voice Message")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithContentDescription("Stop Recording").performClick()
        composeRule.onNodeWithTag("conversation.voice.transcribe").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithContentDescription(
                "Message Format: Both",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().any { it.boundsInRoot.width > 0f }
        }
        composeRule.onNodeWithContentDescription("Message Format: Both", useUnmergedTree = true)
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag("conversation.voice.format").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag(
            "conversation.voice.format.visual",
            useUnmergedTree = true,
        ).assertHeightIsEqualTo(32.dp)
        val formatNode = composeRule.onNodeWithTag("conversation.voice.format")
            .fetchSemanticsNode()
        val format = formatNode.boundsInRoot
        val formatVisual = composeRule.onNodeWithTag(
            "conversation.voice.format.visual",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        assertTrue(format.height > formatVisual.height)
        assertTrue(format.width > formatVisual.width)
        val menu = composeRule.onNodeWithTag("conversation.voice.format.menu").fetchSemanticsNode()
        val expectedGap = 2.dp.value * composeRule.density.density
        val actualGap = formatNode.positionOnScreen.y - menu.positionOnScreen.y - menu.size.height
        org.junit.Assert.assertEquals(
            "Voice-format menu gap",
            expectedGap,
            actualGap,
            1.5f,
        )
        val radioItem = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
        composeRule.onNode(hasText("Both").and(radioItem)).assertIsSelected()
        composeRule.onNode(hasText("Text").and(radioItem)).performClick()
        composeRule.onNodeWithContentDescription("Send Text Message").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Message Format: Text").performClick()
        composeRule.onNode(hasText("Text").and(radioItem)).assertIsSelected().performClick()
    }

    @Test
    fun compactComposerOwnsThe48DpEditorWithoutAFullWidthBackingOrDivider() {
        setConversation("fiatjaf")

        composeRule.onNodeWithTag("conversation.composer.surface").assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag("conversation.composer.host").assertIsDisplayed()
        composeRule.onNodeWithTag("conversation.composer.divider").assertDoesNotExist()
        composeRule.onNodeWithTag("conversation.composer.backing").assertDoesNotExist()
    }

    @Test
    fun availableComposerKeepsTheTimelineViewportEdgeToEdgeAndTheControlsSafe() {
        setConversation("catalog-media-single")

        val root = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val timeline = composeRule.onNodeWithTag("conversation.timeline")
            .fetchSemanticsNode().boundsInRoot
        val composer = composeRule.onNodeWithTag("conversation.composer.host")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(abs(timeline.bottom - root.bottom) < 1.5f)
        assertTrue(composer.bottom < timeline.bottom)
    }

    @Test
    fun composerRemainsReachableInDarkLargeTextRtlAndExpandedWidth() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-composer-multiline" }
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 1f, fontScale = 2f),
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                WhiteNoiseTheme(appearance = AppearancePreference.Dark) {
                    Box(Modifier.requiredSize(width = 700.dp, height = 900.dp)) {
                        ConversationScreen(profile, chat, {}, { true }, {}, {}, {})
                    }
                }
            }
        }

        composeRule.onNodeWithTag("conversation.composer.host").assertIsDisplayed()
        composeRule.onNodeWithTag("conversation.composer.editor").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Add Attachment").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun physicalTapDoesNotRecordButHoldDoesAndReviewStaysInline() {
        setConversation("fiatjaf")
        val voice = composeRule.onNodeWithTag("conversation.voice")

        voice.performTouchInput { click() }
        composeRule.onNodeWithContentDescription("Stop Recording").assertDoesNotExist()
        voice.performTouchInput { longClick(durationMillis = 450) }
        composeRule.onNodeWithTag("conversation.voice.recording.waveform").assertHeightIsEqualTo(24.dp)
        composeRule.onNodeWithTag("conversation.voice.stop").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag(
            "conversation.voice.stop.icon",
            useUnmergedTree = true,
        )
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
        composeRule.onNodeWithText("Stop Recording").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Stop Recording").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed()
        composeRule.onNodeWithTag(
            "conversation.voice.play.container",
            useUnmergedTree = true,
        )
            .assertWidthIsEqualTo(32.dp)
            .assertHeightIsEqualTo(32.dp)
        composeRule.onNodeWithText("Transcribe").assertIsDisplayed()
        composeRule.onNodeWithTag("conversation.voice.transcribe").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithTag(
            "conversation.voice.transcribe.visual",
            useUnmergedTree = true,
        ).assertHeightIsEqualTo(32.dp)
        val transcribeTarget = composeRule.onNodeWithTag("conversation.voice.transcribe")
            .fetchSemanticsNode().boundsInRoot
        val transcribeVisual = composeRule.onNodeWithTag(
            "conversation.voice.transcribe.visual",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        assertTrue(transcribeTarget.height > transcribeVisual.height)
        assertTrue(transcribeTarget.width > transcribeVisual.width)
        composeRule.onNodeWithTag(
            "conversation.voice.transcribe.icon",
            useUnmergedTree = true,
        )
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
        val transcribeIcon = composeRule.onNodeWithTag(
            "conversation.voice.transcribe.icon",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val transcribeLabel = composeRule.onNodeWithTag(
            "conversation.voice.transcribe.label",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val expectedContentGap = 8.dp.value * composeRule.density.density
        assertTrue(abs((transcribeLabel.left - transcribeIcon.right) - expectedContentGap) < 1.5f)
        composeRule.onNodeWithText("Voice Message Review").assertDoesNotExist()
    }

    @Test
    fun composerAccessibilityActionsExpandCollapseAndHideKeyboardWithoutVisibleButtons() {
        setConversation("catalog-composer-multiline")
        val surface = composeRule.onNodeWithTag("conversation.composer.surface")
        val compactHeight = surface.fetchSemanticsNode().boundsInRoot.height
        val actions = surface.fetchSemanticsNode().config[SemanticsActions.CustomActions]

        composeRule.runOnIdle { actions.first { it.label == "Expand Message" }.action() }
        composeRule.waitForIdle()
        val expanded = composeRule.onNodeWithTag("conversation.composer.surface")
        assertTrue(expanded.fetchSemanticsNode().boundsInRoot.height > compactHeight)
        val expandedActions = expanded.fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertTrue(expandedActions.any { it.label == "Collapse Message" })
        assertTrue(expandedActions.any { it.label == "Hide Keyboard" })
        composeRule.onNodeWithText("Expand Message").assertDoesNotExist()
        composeRule.onNodeWithText("Collapse Message").assertDoesNotExist()
    }

    @Test
    fun voiceSubmissionKeepsChosenFormatTranscriptAndDurationInOneCallback() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "fiatjaf" }
        var submission: VoiceDraftSubmission? = null
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
                    onSendVoice = { submission = it; true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Record Voice Message")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.mainClock.advanceTimeBy(1_250)
        composeRule.onNodeWithContentDescription("Stop Recording").performClick()
        composeRule.onNodeWithTag("conversation.voice.transcribe").performClick()
        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithContentDescription(
                "Message Format: Both",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().any { it.boundsInRoot.width > 0f }
        }
        composeRule.onNodeWithContentDescription("Message Format: Both", useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Send Voice and Text Message").performClick()
        composeRule.runOnIdle {
            assertTrue(submission?.durationSeconds == 2)
            assertTrue(submission?.format?.name == "Both")
            assertTrue(submission?.transcript?.isNotBlank() == true)
        }
    }

    @Test
    fun contactAcquisitionUsesExpandedSearchableGroupedSheet() {
        setConversation("fiatjaf")

        composeRule.onNodeWithContentDescription("Add Attachment").performClick()
        composeRule.onNodeWithText("Contact").performClick()

        composeRule.onNodeWithText("Share Contact").assertIsDisplayed()
        composeRule.onNodeWithTag("conversation.contact.search").assertHeightIsAtLeast(56.dp)
        val sheet = composeRule.onNodeWithTag("conversation.contact.sheet").fetchSemanticsNode().boundsInRoot
        val windowHeight = composeRule.activity.window.decorView.height.toFloat()
        assertTrue(sheet.height > windowHeight * 0.85f)
        composeRule.onNodeWithText("Name or Public Key").assertIsDisplayed()
        composeRule.onNodeWithTag("conversation.contact.catalog-direct-text").assertIsDisplayed()
    }

    @Test
    fun recipientVoiceFixtureShowsPlaybackAndTranscriptActions() {
        setConversation("catalog-voice")

        composeRule.onAllNodesWithContentDescription("Play")[0].assertIsDisplayed()
        composeRule.onNodeWithText("Transcribe").assertIsDisplayed()
        composeRule.onAllNodesWithText("Show Transcript")[0].assertIsDisplayed()
    }

    @Test
    fun conversationSearchStaysInPlaceAndReplacesComposerControls() {
        setConversation("catalog-direct-text", initialSearch = true)

        composeRule.onNodeWithContentDescription("Search Messages").assertIsDisplayed()
        composeRule.onNodeWithTag("conversation.searchField").assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag("conversation.searchControls").assertIsDisplayed()
        composeRule.onNodeWithText("0 matches").assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.onNodeWithTag("conversation.searchField").assertDoesNotExist()
        composeRule.onNodeWithText("Message").assertIsDisplayed()
    }

    @Test
    fun conversationSearchExposesClearAndNamedResultNavigation() {
        setConversation("catalog-direct-text", initialSearch = true)

        composeRule.onNodeWithContentDescription("Search Messages").performTextInput("Failed outgoing")
        composeRule.onNodeWithText("1 of 1 match").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear search").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Previous Match").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Next Match").assertIsDisplayed()
    }

    @Test
    fun conversationSearchUsesRoundedAndroidCyanMatchHighlight() {
        composeRule.setContent {
            WhiteNoiseTheme {
                SearchHighlightedText(
                    text = "Available file",
                    query = "ava",
                    modifier = Modifier.testTag("search.highlight"),
                )
            }
        }

        val pixels = composeRule.onNodeWithTag("search.highlight")
            .captureToImage()
            .toPixelMap()
        assertTrue(
            (0 until pixels.width).any { x ->
                (0 until pixels.height).any { y -> pixels[x, y] == Color.Cyan }
            },
        )
    }

    @Test
    fun longPressOpensTheDiscoverableMessageActionSheet() {
        setConversation("fiatjaf")

        composeRule.onNodeWithTag("conversation.message.fiatjaf-8")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithText("Message Actions").assertIsDisplayed()
        composeRule.onNodeWithText("More Reactions").assertIsDisplayed()
        composeRule.onNodeWithText("Reply").assertIsDisplayed()
        composeRule.onNodeWithText("Forward").assertIsDisplayed()
        composeRule.onNodeWithText("Info").assertIsDisplayed()
    }

    @Test
    fun messageSelectionUsesNamedTopAndBottomControls() {
        setConversation("fiatjaf")

        composeRule.onNodeWithTag("conversation.message.fiatjaf-8")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithText("Select").performClick()
        composeRule.onAllNodesWithText("1 Selected")[0].assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close Selection").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Delete Selected Messages").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Forward Selected Messages").assertIsDisplayed()
    }

    @Test
    fun forwardingUsesSearchAndAnExplicitSelectionLimit() {
        setConversation("fiatjaf")

        composeRule.onNodeWithTag("conversation.message.fiatjaf-8")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithText("Forward").performClick()
        composeRule.onNodeWithText("Select up to 5 chats.").assertIsDisplayed()
        composeRule.onNodeWithText("Search Chats").assertIsDisplayed()
    }

    @Test
    fun reactionConfigurationKeepsNamedSlotsAndActions() {
        setConversation("fiatjaf")

        composeRule.onNodeWithTag("conversation.message.fiatjaf-8")
            .performSemanticsAction(SemanticsActions.OnLongClick)
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

        composeRule.onAllNodesWithText("Maya Chen")[0].assertIsDisplayed()
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

    private fun setConversation(chatId: String, initialSearch: Boolean = false) {
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
                    initialSearch = initialSearch,
                )
            }
        }
    }

    private fun isImeVisible(): Boolean = ViewCompat.getRootWindowInsets(
        composeRule.activity.window.decorView,
    )?.isVisible(WindowInsetsCompat.Type.ime()) == true
}
