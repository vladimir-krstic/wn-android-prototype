package dev.ipf.whitenoise

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.click
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.swipe
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import dev.ipf.whitenoise.model.ChatTimelineEntry
import dev.ipf.whitenoise.model.AppearancePreference
import dev.ipf.whitenoise.model.ComposerAvailability
import dev.ipf.whitenoise.model.MessageReaction
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.ReactionCatalog
import dev.ipf.whitenoise.model.VoiceDraftSubmission
import dev.ipf.whitenoise.model.composerAvailability
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
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.week-msg-21"))
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
    fun groupIdentityAlignsToTheBubbleRatherThanItsMetadata() {
        setConversation("weekend-walks")
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.week-msg-21"))

        val bubble = composeRule.onNodeWithTag(
            "conversation.message.bubble.week-msg-21",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val avatar = composeRule.onNodeWithTag(
            "conversation.message.avatar.week-msg-21",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val author = composeRule.onNodeWithTag(
            "conversation.message.author.week-msg-21",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val twelveDp = with(composeRule.density) { 12.dp.toPx() }

        assertTrue(abs(avatar.bottom - bubble.bottom) < 1.5f)
        assertTrue(abs(author.left - bubble.left - twelveDp) < 1.5f)
    }

    @Test
    fun dateHeaderIsInlineTextUntilItsPinnedReplacementIsNeeded() {
        setConversation("catalog-direct-dates")
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("conversation.date.pinned").assertIsDisplayed()

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.date.inline.day-7-Today"))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("conversation.date.inline.day-7-Today").assertIsDisplayed()
        composeRule.onNodeWithTag("conversation.date.pinned").assertDoesNotExist()

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.DATE-15"))
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("conversation.date.pinned").assertIsDisplayed()
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
        composeRule.onNodeWithContentDescription("Badger in grass").performClick()
        composeRule.onNodeWithText("Preview").assertIsDisplayed()
        composeRule.onNodeWithTag("conversation.media.inclusion.target").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Included"),
        )
        composeRule.onNodeWithContentDescription("2 of 4").assertIsDisplayed()
        composeRule.onAllNodesWithTag("conversation.media.thumbnail.unselected", useUnmergedTree = true)
            .fetchSemanticsNodes()
            .also { assertTrue(it.isNotEmpty()) }
        composeRule.onAllNodesWithTag(
            "conversation.media.thumbnail.target",
            useUnmergedTree = true,
        )[0]
            .assertWidthIsEqualTo(56.dp)
            .assertHeightIsEqualTo(56.dp)
        composeRule.onAllNodesWithTag(
            "conversation.media.thumbnail.unselected",
            useUnmergedTree = true,
        )[0]
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
    }

    @Test
    fun composerRemoveUsesOneAccessibleTargetAndConcentricVisualCircle() {
        setConversation("catalog-composer-photo")

        composeRule.onNodeWithTag("conversation.composer.remove.target", useUnmergedTree = true)
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag("conversation.composer.remove.visual", useUnmergedTree = true)
            .assertWidthIsEqualTo(20.dp)
            .assertHeightIsEqualTo(20.dp)
        val target = composeRule.onNodeWithTag(
            "conversation.composer.remove.target",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val visual = composeRule.onNodeWithTag(
            "conversation.composer.remove.visual",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val sixDp = with(composeRule.density) { 6.dp.toPx() }
        assertTrue(abs(visual.top - target.top - sixDp) < 1f)
        assertTrue(abs(target.right - visual.right - sixDp) < 1f)
    }

    @Test
    fun utilityCardsExposeContactAndFullFilenameWhileKeepingCompactGeometry() {
        val profile = ProfileFixtures.marmota
        val fileChat = profile.chats.first { it.id == "catalog-composer-file" }
        val contactChat = profile.chats.first { it.id == "catalog-composer-contact" }
        val chat = fileChat.copy(
            draftAttachments = fileChat.draftAttachments + contactChat.draftAttachments,
        )
        composeRule.setContent {
            WhiteNoiseTheme {
                ConversationScreen(profile, chat, {}, { true }, {}, {}, {})
            }
        }

        composeRule.onAllNodesWithContentDescription("Project Brief.pdf")[0].assertIsDisplayed()
        composeRule.onNodeWithText("Maya Chen", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun utilityCardsRemainReadableAtLargeTypeInRtlMixedShelf() {
        val profile = ProfileFixtures.marmota
        val fileChat = profile.chats.first { it.id == "catalog-composer-file" }
        val contactChat = profile.chats.first { it.id == "catalog-composer-contact" }
        val chat = fileChat.copy(
            draftAttachments = fileChat.draftAttachments + contactChat.draftAttachments,
        )
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

        composeRule.onAllNodesWithContentDescription("Project Brief.pdf")[0].assertIsDisplayed()
        composeRule.onNodeWithText("Maya Chen", useUnmergedTree = true).assertIsDisplayed()
    }

    @Test
    fun composerReplyUsesAlignedAccentAndConcentricInset() {
        setConversation("catalog-composer-reply")
        val composerSurface = composeRule.onNodeWithTag(
            "conversation.composer.surface",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val composerContainer = composeRule.onNodeWithTag(
            "conversation.composer.quote.container",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val composerBar = composeRule.onNodeWithTag(
            "conversation.composer.quote.bar",
            useUnmergedTree = true,
        ).assertWidthIsEqualTo(3.dp).fetchSemanticsNode().boundsInRoot
        val eightDp = with(composeRule.density) { 8.dp.toPx() }
        val twelveDp = with(composeRule.density) { 12.dp.toPx() }

        assertTrue(abs(composerContainer.left - composerSurface.left - eightDp) < 1f)
        assertTrue(abs(composerSurface.right - composerContainer.right - eightDp) < 1f)
        assertTrue(abs(composerContainer.top - composerSurface.top - eightDp) < 1f)
        assertTrue(abs(composerBar.left - composerContainer.left - twelveDp) < 1f)
    }

    @Test
    fun messageReplyUsesAlignedAccentAndConcentricInset() {
        setConversation("catalog-group-messages")
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.GRP-RPL-02"))
        val messageBubble = composeRule.onNodeWithTag(
            "conversation.message.bubble.GRP-RPL-02",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val messageContainer = composeRule.onNodeWithTag(
            "conversation.message.quote.GRP-RPL-02.container",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val messageBar = composeRule.onNodeWithTag(
            "conversation.message.quote.GRP-RPL-02.bar",
            useUnmergedTree = true,
        ).assertWidthIsEqualTo(3.dp).fetchSemanticsNode().boundsInRoot
        val eightDp = with(composeRule.density) { 8.dp.toPx() }
        val twelveDp = with(composeRule.density) { 12.dp.toPx() }

        assertTrue(abs(messageContainer.left - messageBubble.left - eightDp) < 1f)
        assertTrue(abs(messageBubble.right - messageContainer.right - eightDp) < 1f)
        assertTrue(abs(messageContainer.top - messageBubble.top - eightDp) < 1f)
        assertTrue(abs(messageBar.left - messageContainer.left - twelveDp) < 1f)
    }

    @Test
    fun linkPreviewUsesTheSameConcentricComposerInset() {
        setConversation("catalog-composer-link-preview")
        val composerSurface = composeRule.onNodeWithTag(
            "conversation.composer.surface",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val linkPreview = composeRule.onNodeWithTag(
            "conversation.composer.linkPreview",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val eightDp = with(composeRule.density) { 8.dp.toPx() }

        assertTrue(abs(linkPreview.left - composerSurface.left - eightDp) < 1f)
        assertTrue(abs(composerSurface.right - linkPreview.right - eightDp) < 1f)
        assertTrue(abs(linkPreview.top - composerSurface.top - eightDp) < 1f)
    }

    @Test
    fun singleDraftMediaHidesThumbnailRailAndUsesCompactInclusionControl() {
        setConversation("catalog-composer-photo")

        composeRule.onNodeWithContentDescription("Fox in grass").performClick()

        val image = composeRule.onNodeWithTag(
            "conversation.media.preview.image.0",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val target = composeRule.onNodeWithTag(
            "conversation.media.inclusion.target",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val visual = composeRule.onNodeWithTag(
            "conversation.media.inclusion.visual",
            useUnmergedTree = true,
        )
            .assertWidthIsEqualTo(22.dp)
            .assertHeightIsEqualTo(22.dp)
            .fetchSemanticsNode().boundsInRoot
        val sixDp = with(composeRule.density) { 6.dp.toPx() }

        assertTrue(abs(target.right - image.right) < 1f)
        assertTrue(abs(target.bottom - image.bottom) < 1f)
        assertTrue(abs(image.right - visual.right - sixDp) < 1f)
        assertTrue(abs(image.bottom - visual.bottom - sixDp) < 1f)
        composeRule.onNodeWithTag("conversation.media.thumbnail.target").assertDoesNotExist()
    }

    @Test
    fun sentGalleryReturnsFromShortPullKeepsPagingAndProtectsZoomBeforeDownDismissal() {
        setConversation("catalog-media-viewer")
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.MED-VIEW-02"))
        composeRule.onNodeWithTag("conversation.media.tile.MED-VIEW-02-photo.0").performClick()
        val pager = composeRule.onNodeWithTag("conversation.media.viewer.pager")
        val shortPull = with(composeRule.density) { 24.dp.toPx() }
        pager.performTouchInput {
            swipe(Offset(centerX, height * 0.35f), Offset(centerX, height * 0.35f + shortPull), 500)
        }
        composeRule.onNodeWithTag("conversation.media.viewer.position")
            .assertTextContains("2 of 5", substring = true)
        pager.performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag("conversation.media.viewer.position")
            .assertTextContains("3 of 5", substring = true)

        val zoomIn = composeRule.onNodeWithTag("conversation.media.viewer.page.2", useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == "Zoom In" }
        composeRule.runOnIdle { assertTrue(zoomIn.action()) }
        pager.performTouchInput {
            swipe(Offset(centerX, height * 0.35f), Offset(centerX, height * 0.8f), 400)
        }
        pager.assertIsDisplayed()
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        pager.performTouchInput {
            swipe(Offset(centerX, height * 0.35f), Offset(centerX, height * 0.8f), 400)
        }
        pager.assertDoesNotExist()
    }

    @Test
    fun composerGalleryDownDismissalCancelsStagedExclusionAndRetainsHorizontalPaging() {
        setConversation("catalog-composer-photo-album")
        composeRule.onNodeWithContentDescription("Marmot on a rock").performClick()
        val pager = composeRule.onNodeWithTag("conversation.media.preview.pager")
        val shortPull = with(composeRule.density) { 24.dp.toPx() }
        pager.performTouchInput {
            swipe(Offset(centerX, height * 0.35f), Offset(centerX, height * 0.35f + shortPull), 500)
        }
        pager.assertIsDisplayed()
        pager.performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag("conversation.media.preview.image.1", useUnmergedTree = true)
            .assertIsDisplayed()
        pager.performTouchInput { swipeRight() }
        composeRule.onNodeWithTag("conversation.media.inclusion.target").performClick()
        composeRule.onNodeWithTag("conversation.media.inclusion.target").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.Off),
        )
        pager.performTouchInput {
            swipe(Offset(centerX, height * 0.35f), Offset(centerX, height * 0.8f), 400)
        }
        pager.assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Marmot on a rock").performClick()
        composeRule.onNodeWithTag("conversation.media.inclusion.target").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.ToggleableState, ToggleableState.On),
        )
        composeRule.onNodeWithContentDescription("Cancel Media Changes").performClick()
    }

    @Test
    fun sentViewerStartsOnExactAlbumTileAndPagesAcrossTheChatWithoutThumbnails() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-media-viewer" }
        val lastDestinationId = profile.chats.last {
            it.id != chat.id && it.composerAvailability(profile) == ComposerAvailability.Available
        }.id
        setConversation(chat.id)

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.MED-VIEW-02"))
        composeRule.onNodeWithTag("conversation.media.tile.MED-VIEW-02-photo.0").performClick()

        composeRule.onNodeWithTag("conversation.media.viewer.sender")
            .assertTextContains("You")
        composeRule.onNodeWithTag("conversation.media.viewer.position")
            .assertTextContains("2 of 5", substring = true)
        composeRule.onNodeWithTag("conversation.media.thumbnail.target").assertDoesNotExist()
        composeRule.onNodeWithTag("conversation.media.viewer.share")
            .assertIsDisplayed()
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag("conversation.media.viewer.forward")
            .assertIsDisplayed()
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithText("Share").assertDoesNotExist()
        composeRule.onNodeWithText("Forward").assertDoesNotExist()

        val zoomIn = composeRule.onNodeWithTag(
            "conversation.media.viewer.page.1",
            useUnmergedTree = true,
        ).fetchSemanticsNode().config[SemanticsActions.CustomActions]
            .first { it.label == "Zoom In" }
        composeRule.runOnIdle { assertTrue(zoomIn.action()) }
        composeRule.onNodeWithTag("conversation.media.viewer.pager").performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag("conversation.media.viewer.position")
            .assertTextContains("2 of 5", substring = true)

        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag("conversation.media.viewer.pager").performTouchInput { swipeLeft() }
        composeRule.onNodeWithTag("conversation.media.viewer.position")
            .assertTextContains("3 of 5", substring = true)

        composeRule.onNodeWithTag("conversation.media.viewer.forward").performClick()
        composeRule.onNodeWithTag("conversation.forward.search")
            .assertIsDisplayed()
            .assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag("conversation.forward.composer")
            .assertIsDisplayed()
            .assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag("conversation.forward.message")
            .assertIsDisplayed()
            .assertHeightIsEqualTo(48.dp)
        composeRule.onNodeWithTag("conversation.forward.submit")
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
        val destinationBounds = composeRule.onNodeWithTag("conversation.forward.destinations")
            .fetchSemanticsNode().boundsInRoot
        val composerBounds = composeRule.onNodeWithTag("conversation.forward.composer")
            .fetchSemanticsNode().boundsInRoot
        val forwardContentBounds = composeRule.onNodeWithTag("conversation.forward.content")
            .fetchSemanticsNode().boundsInRoot
        val rootHeight = composeRule.runOnIdle {
            composeRule.activity.window.decorView.height.toFloat()
        }
        assertTrue(destinationBounds.bottom > composerBounds.top)
        assertTrue(abs(destinationBounds.bottom - rootHeight) < 1f)
        assertTrue(forwardContentBounds.height >= rootHeight * 0.8f)
        assertTrue(forwardContentBounds.top > 0f)

        val firstDestination = composeRule.onNodeWithTag(
            "conversation.forward.destination.catalog-direct-text",
        ).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox),
        ).fetchSemanticsNode().boundsInRoot
        val secondDestination = composeRule.onNodeWithTag(
            "conversation.forward.destination.catalog-direct-dates",
        ).fetchSemanticsNode().boundsInRoot
        val twoDp = with(composeRule.density) { 2.dp.toPx() }
        assertTrue(abs(secondDestination.top - firstDestination.bottom - twoDp) < 1f)

        composeRule.onNodeWithTag("conversation.forward.destination.catalog-direct-text")
            .performClick()
        composeRule.onNodeWithTag(
            "conversation.forward.destination.catalog-direct-text.check",
            useUnmergedTree = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Forward to 1 Chat").assertIsDisplayed()
        composeRule.onNodeWithText("Forward to 1 Chat").assertDoesNotExist()

        val capSampleY = (
            forwardContentBounds.top - with(composeRule.density) { 4.dp.toPx() }
        ).toInt()
        val restingSheet = composeRule.onNodeWithTag("sheet.surface")
            .captureToImage().toPixelMap()
        val restingCap = restingSheet[restingSheet.width / 2, capSampleY]
        composeRule.onNodeWithTag("conversation.forward.destinations")
            .performTouchInput { swipeUp() }
        composeRule.waitForIdle()
        val scrolledSheet = composeRule.onNodeWithTag("sheet.surface")
            .captureToImage().toPixelMap()
        val scrolledCap = scrolledSheet[scrolledSheet.width / 2, capSampleY]
        val scrolledTop = composeRule.onNodeWithTag("conversation.forward.top")
            .captureToImage().toPixelMap()[0, 0]
        assertTrue(
            scrolledCap.red + scrolledCap.green + scrolledCap.blue <
                restingCap.red + restingCap.green + restingCap.blue,
        )
        assertTrue(abs(scrolledCap.red - scrolledTop.red) < 0.01f)
        assertTrue(abs(scrolledCap.green - scrolledTop.green) < 0.01f)
        assertTrue(abs(scrolledCap.blue - scrolledTop.blue) < 0.01f)

        composeRule.onNodeWithTag("conversation.forward.destinations")
            .performScrollToNode(hasTestTag("conversation.forward.destination.$lastDestinationId"))
        val lastDestination = composeRule.onNodeWithTag(
            "conversation.forward.destination.$lastDestinationId",
        ).fetchSemanticsNode().boundsInRoot
        val eightDp = with(composeRule.density) { 8.dp.toPx() }
        assertTrue(lastDestination.bottom <= composerBounds.top - eightDp)
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
    fun recipientVoiceFixtureKeepsSpeechCommandsInTheFocusedActions() {
        setConversation("catalog-voice")

        composeRule.onAllNodesWithContentDescription("Play")[0].assertIsDisplayed()
        composeRule.onNodeWithText("Transcribe").assertDoesNotExist()
        composeRule.onNodeWithText("Show Transcript").assertDoesNotExist()
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.VOICE-01"))
        composeRule.onNodeWithTag("conversation.message.VOICE-01")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithText("Transcribe").assertIsDisplayed()
        composeRule.onNodeWithText("Transcribe").performClick()
        composeRule.onNodeWithTag("conversation.message.VOICE-01")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithText("Hide Transcript").assertIsDisplayed()
        composeRule.onNodeWithText("Copy Transcript").assertIsDisplayed()
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
    fun composerMentionUsesTheSharedRoundedMediumNeutralHighlight() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-composer-mention" }
        composeRule.setContent {
            WhiteNoiseTheme {
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(
                        outlineVariant = Color.Magenta,
                        onSurface = Color.Yellow,
                    ),
                ) {
                    ConversationScreen(profile, chat, {}, { true }, {}, {}, {})
                }
            }
        }

        val pixels = composeRule.onNodeWithTag("conversation.composer.editor")
            .captureToImage()
            .toPixelMap()
        val highlightPixels = buildList {
            repeat(pixels.width) { x ->
                repeat(pixels.height) { y ->
                    if (pixels[x, y] == Color.Magenta) add(x to y)
                }
            }
        }
        assertTrue(highlightPixels.isNotEmpty())
        assertTrue(
            (0 until pixels.width).any { x ->
                (0 until pixels.height).any { y -> pixels[x, y] == Color.Yellow }
            },
        )
        val left = highlightPixels.minOf { it.first }
        val top = highlightPixels.minOf { it.second }
        assertTrue(pixels[left, top] != Color.Magenta)
    }

    @Test
    fun longPressOpensTheSourcePreservingMessageActionOverlay() {
        setConversation("catalog-direct-reactions")

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.ACT-01"))
        composeRule.onNodeWithTag("conversation.message.ACT-01")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithTag("message.actions.overlay").assertIsDisplayed()
        val overlay = composeRule.onNodeWithTag("message.actions.overlay")
            .fetchSemanticsNode().boundsInRoot
        val backdrop = composeRule.onNodeWithTag(
            "message.actions.backdrop",
            useUnmergedTree = true,
        )
            .assertExists()
            .fetchSemanticsNode().boundsInRoot
        assertTrue(abs(overlay.left - backdrop.left) < 1f)
        assertTrue(abs(overlay.top - backdrop.top) < 1f)
        assertTrue(abs(overlay.right - backdrop.right) < 1f)
        assertTrue(abs(overlay.bottom - backdrop.bottom) < 1f)
        composeRule.onNodeWithText("Message Actions").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("More Reactions").assertIsDisplayed()
        composeRule.onNodeWithTag(
            "message.actions.preview",
            useUnmergedTree = true,
        ).fetchSemanticsNode()
        composeRule.onNodeWithText("Reply")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchHeightIsEqualTo(48.dp)
        composeRule.onNodeWithText("Forward").assertIsDisplayed()
        composeRule.onNodeWithText("Read Aloud").assertIsDisplayed()
        composeRule.onNodeWithText("Info").assertIsDisplayed()
        composeRule.onNodeWithText("Delete")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertTouchHeightIsEqualTo(48.dp)
    }

    @Test
    fun messagePressIndicationMatchesTheBubbleInsteadOfTheFullRow() {
        setConversation("catalog-direct-reactions")

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.ACT-04"))
        val row = composeRule.onNodeWithTag("conversation.message.ACT-04")
            .fetchSemanticsNode().boundsInRoot
        val bubble = composeRule.onNodeWithTag(
            "conversation.message.bubble.ACT-04",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val pressLayer = composeRule.onNodeWithTag(
            "conversation.message.pressLayer.ACT-04",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot

        assertTrue(bubble.width < row.width)
        assertTrue(abs(pressLayer.left - bubble.left) < 1f)
        assertTrue(abs(pressLayer.top - bubble.top) < 1f)
        assertTrue(abs(pressLayer.right - bubble.right) < 1f)
        assertTrue(abs(pressLayer.bottom - bubble.bottom) < 1f)
    }

    @Test
    fun attachmentOnlyMediaAndFileUseEqualConcentricBubbleInsets() {
        setConversation("catalog-direct-reactions")

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.ACT-04"))
        assertRichAttachmentInsets(
            messageId = "ACT-04",
            attachmentTag = "conversation.media.tile.ACT-photo-outgoing.0",
            equalOnAllSides = true,
        )

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.ACT-05"))
        assertRichAttachmentInsets(
            messageId = "ACT-05",
            attachmentTag = "conversation.attachment.ACT-05-file",
            equalOnAllSides = true,
        )
    }

    @Test
    fun mixedMediaAndTextShareTheMediaCanvasWidth() {
        setConversation("catalog-media-single")

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.MED-11"))
        assertRichAttachmentInsets(
            messageId = "MED-11",
            attachmentTag = "conversation.media.grid.MED-11",
        )

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.MED-SINGLE-08"))
        assertRichAttachmentInsets(
            messageId = "MED-SINGLE-08",
            attachmentTag = "conversation.media.grid.MED-SINGLE-08",
        )
    }

    @Test
    fun oneTapOnVisualEmptySpaceDismissesFocusedMessageActions() {
        setConversation("catalog-direct-reactions")

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.ACT-01"))
        composeRule.onNodeWithTag("conversation.message.ACT-01")
            .performSemanticsAction(SemanticsActions.OnLongClick)

        val overlay = composeRule.onNodeWithTag("message.actions.overlay")
        val overlayBounds = overlay.fetchSemanticsNode().boundsInRoot
        val bubbleBounds = composeRule.onNodeWithTag(
            "conversation.message.bubble.ACT-01",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val emptySpaceInsetPx = with(composeRule.density) { 20.dp.toPx() }

        overlay.performTouchInput {
            click(
                Offset(
                    x = width.toFloat() - emptySpaceInsetPx,
                    y = bubbleBounds.center.y - overlayBounds.top,
                ),
            )
        }

        composeRule.onNodeWithTag("message.actions.overlay").assertDoesNotExist()
    }

    @Test
    fun focusedReactionTargetsUseCircularStateLayersAndEqualRailInsets() {
        setConversation("catalog-direct-reactions")
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.ACT-01"))
        composeRule.onNodeWithTag("conversation.message.ACT-01")
            .performSemanticsAction(SemanticsActions.OnLongClick)

        val rail = composeRule.onNodeWithTag(
            "message.actions.reactions",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val firstTarget = composeRule.onNodeWithTag(
            "message.actions.reaction.target.0",
            useUnmergedTree = true,
        )
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
            .fetchSemanticsNode().boundsInRoot
        val firstStateLayer = composeRule.onNodeWithTag(
            "message.actions.reaction.stateLayer.0",
            useUnmergedTree = true,
        )
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(40.dp)
            .fetchSemanticsNode().boundsInRoot
        val secondTarget = composeRule.onNodeWithTag(
            "message.actions.reaction.target.1",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val moreTarget = composeRule.onNodeWithTag(
            "message.actions.reaction.more",
            useUnmergedTree = true,
        )
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
            .fetchSemanticsNode().boundsInRoot
        val moreStateLayer = composeRule.onNodeWithTag(
            "message.actions.reaction.more.stateLayer",
            useUnmergedTree = true,
        )
            .assertWidthIsEqualTo(40.dp)
            .assertHeightIsEqualTo(40.dp)
            .fetchSemanticsNode().boundsInRoot
        (0 until 6).forEach { index ->
            composeRule.onNodeWithTag(
                "message.actions.reaction.stateLayer.$index",
                useUnmergedTree = true,
            )
                .assertWidthIsEqualTo(40.dp)
                .assertHeightIsEqualTo(40.dp)
            composeRule.onNodeWithTag(
                "message.actions.reaction.emoji.$index",
                useUnmergedTree = true,
            )
                .assertWidthIsEqualTo(28.dp)
                .assertHeightIsEqualTo(28.dp)
        }
        val fourDp = with(composeRule.density) { 4.dp.toPx() }

        assertTrue(abs((firstTarget.left - rail.left) - fourDp) < 1.5f)
        assertTrue(abs((firstTarget.top - rail.top) - fourDp) < 1.5f)
        assertTrue(abs((rail.bottom - firstTarget.bottom) - fourDp) < 1.5f)
        assertTrue(abs((rail.right - moreTarget.right) - fourDp) < 1.5f)
        assertTrue(abs((secondTarget.left - firstTarget.right) - fourDp) < 1.5f)
        assertTrue(abs((firstStateLayer.left - firstTarget.left) - fourDp) < 1.5f)
        assertTrue(abs((firstStateLayer.top - firstTarget.top) - fourDp) < 1.5f)
        assertTrue(abs((firstTarget.right - firstStateLayer.right) - fourDp) < 1.5f)
        assertTrue(abs((firstTarget.bottom - firstStateLayer.bottom) - fourDp) < 1.5f)
        assertTrue(abs((moreStateLayer.left - moreTarget.left) - fourDp) < 1.5f)
        assertTrue(abs((moreStateLayer.top - moreTarget.top) - fourDp) < 1.5f)
        assertTrue(abs((moreTarget.right - moreStateLayer.right) - fourDp) < 1.5f)
        assertTrue(abs((moreTarget.bottom - moreStateLayer.bottom) - fourDp) < 1.5f)
    }

    @Test
    fun newestMessageSettlesFullyAboveTheMeasuredComposer() {
        setConversation("fiatjaf")
        composeRule.waitForIdle()

        val newest = composeRule.onNodeWithTag("conversation.message.fiatjaf-8")
            .fetchSemanticsNode().boundsInRoot
        val composer = composeRule.onNodeWithTag("conversation.composer.host")
            .fetchSemanticsNode().boundsInRoot

        assertTrue(newest.bottom <= composer.top)
    }

    @Test
    fun reactionPillsKeepCompactConsistentGeometryAndGrowForCounts() {
        setConversation("catalog-direct-reactions")
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.RCT-05"))

        val bubble = composeRule.onNodeWithTag(
            "conversation.message.bubble.RCT-05",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val firstTarget = composeRule.onNodeWithTag("conversation.reaction.RCT-05.0")
            .assertIsDisplayed()
            .assertHeightIsEqualTo(48.dp)
            .fetchSemanticsNode().boundsInRoot
        val countedTarget = composeRule.onNodeWithTag("conversation.reaction.RCT-05.1")
            .assertIsDisplayed()
            .assertHeightIsEqualTo(48.dp)
            .fetchSemanticsNode().boundsInRoot
        val firstPill = composeRule.onNodeWithTag(
            "conversation.reaction.pill.RCT-05.0",
            useUnmergedTree = true,
        )
            .assertHeightIsEqualTo(23.dp)
            .assertWidthIsEqualTo(31.dp)
            .fetchSemanticsNode().boundsInRoot
        val countedPill = composeRule.onNodeWithTag(
            "conversation.reaction.pill.RCT-05.1",
            useUnmergedTree = true,
        )
            .assertHeightIsEqualTo(23.dp)
            .fetchSemanticsNode().boundsInRoot
        val lastPill = composeRule.onNodeWithTag(
            "conversation.reaction.pill.RCT-05.2",
            useUnmergedTree = true,
        )
            .assertHeightIsEqualTo(23.dp)
            .fetchSemanticsNode().boundsInRoot
        val time = composeRule.onNodeWithTag(
            "conversation.message.time.RCT-05",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val nineDp = with(composeRule.density) { 9.dp.toPx() }
        val twoDp = with(composeRule.density) { 2.dp.toPx() }
        val threeDp = with(composeRule.density) { 3.dp.toPx() }
        val twelveDp = with(composeRule.density) { 12.dp.toPx() }

        assertTrue(abs(firstTarget.center.y - countedTarget.center.y) < 1f)
        assertTrue(countedPill.width > firstPill.width)
        assertTrue(lastPill.width > firstPill.width)
        assertTrue(abs(countedPill.width - lastPill.width) < 1.5f)
        assertTrue(abs((countedPill.left - firstPill.right) - threeDp) < 1.5f)
        assertTrue(abs((lastPill.left - countedPill.right) - threeDp) < 1.5f)
        assertTrue(abs((bubble.bottom - firstPill.top) - nineDp) < 1.5f)
        assertTrue(abs((time.top - bubble.bottom) - twoDp) < 1.5f)
        assertTrue(abs((time.left - bubble.left) - twelveDp) < 1.5f)
        assertTrue(abs((bubble.right - lastPill.right) - twelveDp) < 1.5f)
    }

    @Test
    fun timestampsFollowMessageDirectionWithAndWithoutReactions() {
        setConversation("catalog-direct-reactions")
        val twoDp = with(composeRule.density) { 2.dp.toPx() }
        val twelveDp = with(composeRule.density) { 12.dp.toPx() }

        fun assertTimestamp(messageId: String, outgoing: Boolean) {
            composeRule.onNodeWithTag("conversation.timeline")
                .performScrollToNode(hasTestTag("conversation.message.$messageId"))
            val bubble = composeRule.onNodeWithTag(
                "conversation.message.bubble.$messageId",
                useUnmergedTree = true,
            ).fetchSemanticsNode().boundsInRoot
            val time = composeRule.onNodeWithTag(
                "conversation.message.time.$messageId",
                useUnmergedTree = true,
            ).fetchSemanticsNode().boundsInRoot

            assertTrue(abs((time.top - bubble.bottom) - twoDp) < 1.5f)
            if (outgoing) {
                assertTrue(abs((bubble.right - time.right) - twelveDp) < 1.5f)
                composeRule.onNodeWithTag(
                    "conversation.message.delivery.$messageId",
                    useUnmergedTree = true,
                )
                    .assertIsDisplayed()
                    .assertWidthIsEqualTo(14.dp)
                    .assertHeightIsEqualTo(14.dp)
                composeRule.onNodeWithTag(
                    "conversation.message.delivery.icon.$messageId",
                    useUnmergedTree = true,
                )
                    .assertWidthIsEqualTo(10.dp)
                    .assertHeightIsEqualTo(10.dp)
            } else {
                assertTrue(abs((time.left - bubble.left) - twelveDp) < 1.5f)
                composeRule.onNodeWithTag("conversation.message.delivery.$messageId")
                    .assertDoesNotExist()
            }
        }

        assertTimestamp("RCT-10", outgoing = false)
        assertTimestamp("RCT-11", outgoing = true)
        assertTimestamp("ACT-01", outgoing = false)
        assertTimestamp("ACT-04", outgoing = true)
    }

    @Test
    fun reactionOverflowStillCompactsAfterVisibleSpacingCorrection() {
        val profile = ProfileFixtures.marmota
        val sourceChat = profile.chats.first { it.id == "catalog-direct-reactions" }
        val overflowChat = sourceChat.copy(
            timeline = sourceChat.timeline.map { entry ->
                if (entry is ChatTimelineEntry.Message && entry.message.id == "RCT-13") {
                    entry.copy(
                        message = entry.message.copy(
                            reactions = ReactionCatalog.all.take(10).map { emoji ->
                                MessageReaction(emoji, listOf("maya-chen"))
                            },
                        ),
                    )
                } else {
                    entry
                }
            },
        )
        val overflowProfile = profile.copy(
            chats = profile.chats.map { chat ->
                if (chat.id == overflowChat.id) overflowChat else chat
            },
        )
        val overflowMatcher = SemanticsMatcher("overflow reaction summary") { node ->
            if (SemanticsProperties.ContentDescription !in node.config) {
                false
            } else {
                node.config[SemanticsProperties.ContentDescription]
                    .any { description -> "more reaction types" in description }
            }
        }
        composeRule.setContent {
            WhiteNoiseTheme {
                ConversationScreen(overflowProfile, overflowChat, {}, { true }, {}, {}, {})
            }
        }
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.RCT-13"))

        composeRule.onNode(overflowMatcher).assertIsDisplayed()
        (0..4).forEach { index ->
            composeRule.onNodeWithTag("conversation.reaction.RCT-13.$index")
                .assertIsDisplayed()
        }
        composeRule.onNodeWithTag("conversation.reaction.RCT-13.5")
            .assertDoesNotExist()
    }

    @Test
    fun focusedActionMenuUsesTheSameGapAboveAndBelowVisibleReactions() {
        setConversation("catalog-direct-reactions")
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.RCT-13"))
        composeRule.onNodeWithTag("conversation.message.RCT-13")
            .performSemanticsAction(SemanticsActions.OnLongClick)

        val quickReactions = composeRule.onNodeWithTag(
            "message.actions.reactions",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val preview = composeRule.onNodeWithTag(
            "message.actions.preview",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val menu = composeRule.onNodeWithTag(
            "message.actions.menu",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val topShadowGutter = composeRule.onNodeWithTag(
            "message.actions.shadowGutter.top",
            useUnmergedTree = true,
        )
            .assertHeightIsEqualTo(8.dp)
            .fetchSemanticsNode().boundsInRoot
        val bottomShadowGutter = composeRule.onNodeWithTag(
            "message.actions.shadowGutter.bottom",
            useUnmergedTree = true,
        )
            .assertHeightIsEqualTo(8.dp)
            .fetchSemanticsNode().boundsInRoot
        val expectedGap = with(composeRule.density) { 8.dp.toPx() }
        val upperGap = preview.top - quickReactions.bottom
        val lowerGap = menu.top - preview.bottom

        assertTrue(abs(upperGap - expectedGap) < 1.5f)
        assertTrue(abs(lowerGap - expectedGap) < 1.5f)
        assertTrue(
            "top gutter ended at ${topShadowGutter.bottom}px and rail began at ${quickReactions.top}px",
            abs(quickReactions.top - topShadowGutter.bottom) < 1.5f,
        )
        assertTrue(
            "menu ended at ${menu.bottom}px and bottom gutter began at ${bottomShadowGutter.top}px",
            abs(bottomShadowGutter.top - menu.bottom) < 1.5f,
        )
    }

    @Test
    fun reactionOverlapAndFocusedGapSurviveTwoHundredPercentType() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-direct-reactions" }
        val physicalDensity = composeRule.density.density
        composeRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = physicalDensity, fontScale = 2f),
            ) {
                WhiteNoiseTheme {
                    ConversationScreen(profile, chat, {}, { true }, {}, {}, {})
                }
            }
        }
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.RCT-13"))

        val bubble = composeRule.onNodeWithTag(
            "conversation.message.bubble.RCT-13",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val pill = composeRule.onNodeWithTag(
            "conversation.reaction.pill.RCT-13.0",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val target = composeRule.onNodeWithTag(
            "conversation.reaction.RCT-13.0",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val metadata = composeRule.onNodeWithTag(
            "conversation.message.metadata.RCT-13",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val time = composeRule.onNodeWithTag(
            "conversation.message.time.RCT-13",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val nineDp = with(composeRule.density) { 9.dp.toPx() }
        val twoDp = with(composeRule.density) { 2.dp.toPx() }
        val overlap = bubble.bottom - pill.top
        assertTrue(
            "Expected a 9 dp reaction overlap at 200% type; overlap=$overlap expected=$nineDp " +
                "bubble=$bubble pill=$pill target=$target metadata=$metadata",
            abs(overlap - nineDp) < 1.5f,
        )
        assertTrue(
            "Expected a 2 dp timestamp gap at 200% type; bubble=$bubble time=$time",
            abs((time.top - bubble.bottom) - twoDp) < 1.5f,
        )

        composeRule.onNodeWithTag("conversation.message.RCT-13")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        val quickReactions = composeRule.onNodeWithTag(
            "message.actions.reactions",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val preview = composeRule.onNodeWithTag(
            "message.actions.preview",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val menu = composeRule.onNodeWithTag(
            "message.actions.menu",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        assertTrue(abs((preview.top - quickReactions.bottom) - (menu.top - preview.bottom)) < 1.5f)
    }

    @Test
    fun selectionControlsUseOneStableLeadingColumnForBothDirections() {
        setConversation("catalog-direct-text")
        composeRule.onNodeWithTag("conversation.message.DLV-03")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithText("Select").performClick()

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.TXT-06"))
        val incoming = composeRule.onNodeWithTag(
            "conversation.selection.control.TXT-06",
            useUnmergedTree = true,
        )
            .assertWidthIsEqualTo(48.dp)
            .fetchSemanticsNode().boundsInRoot
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.DLV-03"))
        val outgoing = composeRule.onNodeWithTag(
            "conversation.selection.control.DLV-03",
            useUnmergedTree = true,
        )
            .assertWidthIsEqualTo(48.dp)
            .fetchSemanticsNode().boundsInRoot

        assertTrue(abs(incoming.left - outgoing.left) < 1f)
    }

    @Test
    fun semanticLeadingSwipeBeginsReplyWithoutOpeningActions() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "fiatjaf" }
        var repliedTo: String? = null
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
                    onReply = {
                        repliedTo = it
                        true
                    },
                )
            }
        }

        composeRule.onNodeWithTag("conversation.message.fiatjaf-8")
            .performTouchInput { swipeRight() }

        composeRule.runOnIdle { check(repliedTo == "fiatjaf-8") }
        composeRule.onNodeWithTag("message.actions.overlay").assertDoesNotExist()
    }

    @Test
    fun replyIndicatorStaysAttachedToTheBubbleDuringDragAndReturn() {
        setConversation("fiatjaf")
        val messageTag = "conversation.message.fiatjaf-6"
        val bubbleTag = "conversation.message.bubble.fiatjaf-6"
        val indicatorTag = "conversation.message.swipeReply.fiatjaf-6"
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag(messageTag))
        val restingBubble = composeRule.onNodeWithTag(
            bubbleTag,
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val twoDp = with(composeRule.density) { 2.dp.toPx() }

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithTag(messageTag)
                .performTouchInput { swipeRight(durationMillis = 300) }
            composeRule.mainClock.advanceTimeByFrame()

            val movedBubble = composeRule.onNodeWithTag(
                bubbleTag,
                useUnmergedTree = true,
            ).fetchSemanticsNode().boundsInRoot
            val indicator = composeRule.onNodeWithTag(
                indicatorTag,
                useUnmergedTree = true,
            ).fetchSemanticsNode().boundsInRoot

            assertTrue(movedBubble.left > restingBubble.left)
            assertTrue(indicator.center.x > restingBubble.left)
            assertTrue(indicator.center.x < movedBubble.left)
            assertTrue(abs(indicator.center.y - restingBubble.center.y) < twoDp)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun replySwipeMirrorsToLeadingDirectionInRtl() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "fiatjaf" }
        var repliedTo: String? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                WhiteNoiseTheme {
                    ConversationScreen(
                        profile = profile,
                        chat = chat,
                        onBack = {},
                        onSend = { true },
                        onRetry = {},
                        onAcceptInvitation = {},
                        onDeclineInvitation = {},
                        onReply = {
                            repliedTo = it
                            true
                        },
                    )
                }
            }
        }

        composeRule.onNodeWithTag("conversation.message.fiatjaf-8")
            .performTouchInput { swipeLeft() }

        composeRule.runOnIdle { check(repliedTo == "fiatjaf-8") }
    }

    @Test
    fun replyIndicatorRemainsBubbleAttachedInRtl() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "fiatjaf" }
        composeRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
        val messageTag = "conversation.message.fiatjaf-6"
        val bubbleTag = "conversation.message.bubble.fiatjaf-6"
        val indicatorTag = "conversation.message.swipeReply.fiatjaf-6"
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag(messageTag))
        val restingBubble = composeRule.onNodeWithTag(
            bubbleTag,
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val twoDp = with(composeRule.density) { 2.dp.toPx() }

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithTag(messageTag)
                .performTouchInput { swipeLeft(durationMillis = 300) }
            composeRule.mainClock.advanceTimeByFrame()

            val movedBubble = composeRule.onNodeWithTag(
                bubbleTag,
                useUnmergedTree = true,
            ).fetchSemanticsNode().boundsInRoot
            val indicator = composeRule.onNodeWithTag(
                indicatorTag,
                useUnmergedTree = true,
            ).fetchSemanticsNode().boundsInRoot

            assertTrue(movedBubble.right < restingBubble.right)
            assertTrue(indicator.center.x < restingBubble.right)
            assertTrue(indicator.center.x > movedBubble.right)
            assertTrue(abs(indicator.center.y - restingBubble.center.y) < twoDp)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun recipientSpeechCommandsDoNotPermanentlyInflateTextBubbles() {
        setConversation("fiatjaf")

        composeRule.onNodeWithText("Read Aloud").assertDoesNotExist()
        composeRule.onNodeWithTag("conversation.message.fiatjaf-8")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Read Aloud").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Read Aloud").assertIsDisplayed()
    }

    @Test
    fun sentTextOffersReadAloudAndStopReadingWithProgress() {
        setConversation("catalog-direct-reactions")
        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.ACT-02"))
        composeRule.onNodeWithText("Read Aloud").assertDoesNotExist()
        val message = composeRule.onNodeWithTag("conversation.message.ACT-02")
        message.performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodesWithText("Read Aloud").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Read Aloud").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("message.actions.overlay").assertDoesNotExist()
        composeRule.onNodeWithTag("conversation.readAloud.progress.ACT-02").assertIsDisplayed()

        message.performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithText("Read Aloud").assertDoesNotExist()
        composeRule.onNodeWithText("Stop Reading").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("conversation.readAloud.progress.ACT-02").assertDoesNotExist()
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
        composeRule.onNodeWithContentDescription("More Reactions").performClick()
        composeRule.onNodeWithContentDescription("Configure Reactions").performClick()
        composeRule.onNodeWithText("Tap an emoji to replace it.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reaction 1, ❤️. Double tap to replace.").assertIsDisplayed()
        composeRule.onNodeWithText("Reset").assertIsDisplayed()
        composeRule.onNodeWithText("Done").assertIsDisplayed()
    }

    @Test
    fun emojiPickerUsesSectionedGridSearchAndPinnedCategoryNavigation() {
        setConversation("fiatjaf")

        composeRule.onNodeWithTag("conversation.message.fiatjaf-8")
            .performSemanticsAction(SemanticsActions.OnLongClick)
        composeRule.onNodeWithContentDescription("More Reactions").performClick()

        composeRule.onNodeWithTag("emoji.picker").assertIsDisplayed()
        composeRule.onNodeWithText("Search emoji").assertIsDisplayed()
        composeRule.onNodeWithText("Recently Used").assertIsDisplayed()
        composeRule.onNodeWithTag("emoji.picker.categories").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Configure Reactions").assertIsDisplayed()
        composeRule.onNodeWithTag("emoji.picker.item.recent.0", useUnmergedTree = true)
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)

        composeRule.onNodeWithContentDescription("Animals & Nature").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("emoji.picker.header.animals").assertIsDisplayed()

        composeRule.onNodeWithTag("emoji.picker.search").performTextInput("beaver")
        composeRule.onNodeWithTag("emoji.picker.categories").assertDoesNotExist()
        composeRule.onAllNodesWithContentDescription("🦫")[0].assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Clear search").performClick()
        composeRule.onNodeWithTag("emoji.picker.categories").assertIsDisplayed()
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
    fun messageDetailsShowsEveryReactionTypeAndPeople() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-direct-reactions" }
        val message = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>()
            .first { it.message.id == "RCT-13" }
            .message
        composeRule.setContent {
            WhiteNoiseTheme {
                MessageDetailsScreen(profile, chat, message, onBack = {})
            }
        }

        composeRule.onNodeWithText("Reactions").assertIsDisplayed()
        repeat(7) { index ->
            composeRule.onNodeWithTag("message.details.reaction.$index")
                .assertExists()
        }
        composeRule.onNodeWithTag("message.details.reaction.7").assertDoesNotExist()
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

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.DLV-03"))
        val bubble = composeRule.onNodeWithTag(
            "conversation.message.bubble.DLV-03",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val failure = composeRule.onNodeWithTag(
            "conversation.message.time.DLV-03",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val twoDp = with(composeRule.density) { 2.dp.toPx() }
        val twelveDp = with(composeRule.density) { 12.dp.toPx() }

        composeRule.onNodeWithText("Not delivered, tap to retry").assertIsDisplayed()
        composeRule.onNodeWithTag(
            "conversation.message.delivery.DLV-03",
            useUnmergedTree = true,
        )
            .assertWidthIsEqualTo(14.dp)
            .assertHeightIsEqualTo(14.dp)
        assertTrue(abs((failure.top - bubble.bottom) - twoDp) < 1.5f)
        assertTrue(abs((bubble.right - failure.right) - twelveDp) < 1.5f)

        composeRule.onNodeWithTag("conversation.message.DLV-03").performClick()
        composeRule.runOnIdle { check(retriedMessageId == "DLV-03") }
    }

    @Test
    fun markdownMessageRendersInlineContentInsteadOfSourcePunctuation() {
        setConversation("catalog-direct-text")

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.TXT-09"))
        composeRule.onNodeWithText(
            "TXT-09: Bold, emphasis, and White Noise",
            substring = true,
        ).assertIsDisplayed()
        composeRule.onNodeWithText("**Bold**", substring = true).assertDoesNotExist()
    }

    @Test
    fun replyQuoteScrollsToAndHighlightsItsAvailableSource() {
        setConversation("catalog-direct-replies")

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.RPL-01"))
        composeRule.onNodeWithTag(
            "conversation.message.quote.target.RPL-01",
            useUnmergedTree = true,
        ).performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) {
            composeRule.onAllNodesWithTag(
                "conversation.message.highlight.RPL-01-source",
                useUnmergedTree = true,
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("conversation.message.RPL-01-source").assertIsDisplayed()
    }

    @Test
    fun holdingAReplyQuoteOpensMessageActionsInsteadOfOpeningTheSource() {
        setConversation("catalog-direct-replies")

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.RPL-01"))
        composeRule.onNodeWithTag(
            "conversation.message.quote.target.RPL-01",
            useUnmergedTree = true,
        ).performTouchInput { longClick(durationMillis = 650) }

        composeRule.onNodeWithTag("message.actions.overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("conversation.message.highlight.RPL-01-source")
            .assertDoesNotExist()
    }

    @Test
    fun holdingAMediaTileOpensMessageActionsInsteadOfTheGallery() {
        setConversation("catalog-media-viewer")

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.MED-VIEW-02"))
        composeRule.onNodeWithTag(
            "conversation.media.tile.MED-VIEW-02-photo.0",
            useUnmergedTree = true,
        ).performTouchInput { longClick(durationMillis = 650) }

        composeRule.onNodeWithTag("message.actions.overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("conversation.media.viewer.pager").assertDoesNotExist()
    }

    @Test
    fun holdingALinkCardOpensMessageActionsInsteadOfTheLink() {
        setConversation("catalog-media-rich")

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.LINK-01"))
        composeRule.onNodeWithTag(
            "conversation.attachment.LINK-01-link",
            useUnmergedTree = true,
        ).performTouchInput { longClick(durationMillis = 650) }

        composeRule.onNodeWithTag("message.actions.overlay").assertIsDisplayed()
    }

    @Test
    fun holdingVoicePlaybackOpensMessageActionsInsteadOfTogglingPlayback() {
        setConversation("catalog-voice")

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.VOICE-01"))
        composeRule.onNodeWithTag(
            "conversation.voice.play.VOICE-01-audio",
            useUnmergedTree = true,
        ).performTouchInput { longClick(durationMillis = 650) }

        composeRule.onNodeWithTag("message.actions.overlay").assertIsDisplayed()
    }

    @Test
    fun validContactCardOpensTheReferencedProfile() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-media-rich" }
        var openedPersonId: String? = null
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
                    onOpenPersonProfile = { openedPersonId = it },
                )
            }
        }

        composeRule.onNodeWithTag(
            "conversation.attachment.RICH-05-contact",
            useUnmergedTree = true,
        ).performClick()
        composeRule.runOnIdle { check(openedPersonId == "avery-stone") }
    }

    @Test
    fun holdingAContactCardOpensMessageActionsInsteadOfTheProfile() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "catalog-media-rich" }
        var openedPersonId: String? = null
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
                    onOpenPersonProfile = { openedPersonId = it },
                )
            }
        }

        composeRule.onNodeWithTag(
            "conversation.attachment.RICH-05-contact",
            useUnmergedTree = true,
        ).performTouchInput { longClick(durationMillis = 650) }

        composeRule.onNodeWithTag("message.actions.overlay").assertIsDisplayed()
        composeRule.runOnIdle { check(openedPersonId == null) }
    }

    @Test
    fun longVoiceFixtureUsesMinuteSecondDuration() {
        setConversation("catalog-voice")

        composeRule.onNodeWithTag("conversation.timeline")
            .performScrollToNode(hasTestTag("conversation.message.VOICE-03"))
        composeRule.onNodeWithText("1:22").assertIsDisplayed()
    }

    @Test
    fun supportGuidanceRemainsTimelineInformationRatherThanAMessage() {
        setConversation("white-noise-support")

        composeRule.onNodeWithText(
            "How can we help? Ask a question, report a problem, or share a suggestion. We’ll reply here.",
        ).assertIsDisplayed()
    }

    private fun assertRichAttachmentInsets(
        messageId: String,
        attachmentTag: String,
        equalOnAllSides: Boolean = false,
    ) {
        val bubble = composeRule.onNodeWithTag(
            "conversation.message.bubble.$messageId",
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val attachment = composeRule.onNodeWithTag(
            attachmentTag,
            useUnmergedTree = true,
        ).fetchSemanticsNode().boundsInRoot
        val expectedInset = with(composeRule.density) { 6.dp.toPx() }
        val tolerance = with(composeRule.density) { 1.dp.toPx() }
        val left = attachment.left - bubble.left
        val right = bubble.right - attachment.right
        val top = attachment.top - bubble.top

        assertTrue(abs(left - expectedInset) <= tolerance)
        assertTrue(abs(right - expectedInset) <= tolerance)
        assertTrue(abs(top - expectedInset) <= tolerance)
        if (equalOnAllSides) {
            val bottom = bubble.bottom - attachment.bottom
            assertTrue(abs(bottom - expectedInset) <= tolerance)
        }
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
