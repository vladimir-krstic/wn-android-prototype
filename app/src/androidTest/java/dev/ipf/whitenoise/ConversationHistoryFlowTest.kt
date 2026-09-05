package dev.ipf.whitenoise

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.*
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.conversation.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConversationHistoryFlowTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private val profile = ProfileFixtures.marmota.copy(id = "me", name = "Me", people = listOf(Person("friend", "Friend")))
    private fun chat(unread: Int = 0): Chat = Chat("history", 0, ChatKind.Group, "Plans", unreadCount = unread,
        members = listOf(GroupMember(profile.id, GroupRole.Admin), GroupMember("friend", GroupRole.Member)),
        relayUrls = profile.chatRelayUrls, draftText = "Keep this draft",
        timeline = (0 until 60).map { i -> ChatTimelineEntry.Message(ChatMessage("h$i", "friend", 3, "Today", 300 + i, "10:00",
            when (i) { 0 -> "The earlier needle is here."; 30 -> "Please check this, @Me."; else -> "Update $i" })) })
    @Composable private fun Screen(chat: Chat = chat(), search: Boolean = false, target: String? = null,
        scenario: (HistoryOperation) -> HistoryScenario = { HistoryScenario.Success },
        onVisible: (Set<String>) -> Unit = {}, onMention: (String) -> Unit = {}) {
        ConversationScreen(profile, chat, {}, { true }, {}, {}, {}, initialSearch = search, initialMessageId = target,
            onHistoryScenario = scenario, onMessagesVisible = onVisible, onReadThroughMention = onMention)
    }
    private fun scrollTo(tag: String) { rule.onNodeWithTag("conversation.timeline").performScrollToNode(hasTestTag(tag)) }
    private fun waitForNoTarget() { rule.waitUntil(4_000) { rule.onAllNodesWithTag("history.target").fetchSemanticsNodes().isEmpty() } }
    private fun waitForMessage(id: String) {
        rule.waitUntil(4_000) { rule.onAllNodesWithTag("conversation.message.$id").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun latestJumpIsIconOnlyAndReturnsFromOlderHistory() {
        rule.setContent { WhiteNoiseTheme { Screen() } }
        waitForMessage("h59")
        rule.onNodeWithTag("history.jumpUnread").assertDoesNotExist()
        scrollTo("history.Older")
        rule.onNodeWithText("Load older messages").performClick()
        rule.waitUntil(3_000) { rule.onAllNodesWithText("Loading messages…").fetchSemanticsNodes().isEmpty() }
        scrollTo("conversation.message.h24")
        rule.onNodeWithText("Jump to latest messages").assertDoesNotExist()
        rule.onNodeWithContentDescription("Jump to latest messages").assertIsDisplayed().performClick()
        waitForMessage("h59")
        rule.onNodeWithTag("conversation.message.h59").assertIsDisplayed()
        rule.onNodeWithTag("history.jumpUnread").assertDoesNotExist()
    }

    @Test fun olderFailureKeepsLoadedRowsAndRetryPrependsHistory() {
        var failed = false
        rule.setContent { WhiteNoiseTheme { Screen(scenario = { op -> if (op == HistoryOperation.Older && !failed) { failed = true; HistoryScenario.OlderFails } else HistoryScenario.Success }) } }
        scrollTo("history.Older"); rule.onNodeWithText("Load older messages").performClick()
        rule.waitUntil(3_000) { rule.onAllNodesWithText("Couldn’t load more messages.").fetchSemanticsNodes().isNotEmpty() }
        scrollTo("conversation.message.h42"); rule.onNodeWithTag("conversation.message.h42").assertExists()
        scrollTo("history.Older"); rule.onNodeWithText("Retry").performClick()
        rule.waitUntil(3_000) { rule.onAllNodesWithText("Loading messages…").fetchSemanticsNodes().isEmpty() }
        scrollTo("conversation.message.h24"); rule.onNodeWithTag("conversation.message.h24").assertExists()
    }
    @Test fun failedExhaustiveSearchRetriesThenLoadsUnloadedMatchAndNewerPages() {
        var failSearch = true; var failNewer = true
        rule.setContent { WhiteNoiseTheme { Screen(search = true, scenario = { op -> when {
            op == HistoryOperation.Search && failSearch -> { failSearch = false; HistoryScenario.SearchFails }
            op == HistoryOperation.Newer && failNewer -> { failNewer = false; HistoryScenario.NewerFails }
            else -> HistoryScenario.Success
        } }) } }
        rule.onNodeWithTag("conversation.searchField").performTextInput("needle")
        rule.waitUntil(3_000) { rule.onAllNodesWithText("Couldn’t search all history. Matches in loaded messages are shown.").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Retry").assertIsDisplayed().performClick()
        waitForMessage("h0"); rule.onNodeWithTag("conversation.message.h0").assertExists()
        scrollTo("history.Newer"); rule.onNodeWithText("Load newer messages").performClick()
        rule.waitUntil(3_000) { rule.onAllNodesWithText("Couldn’t load more messages.").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Retry").performClick()
        rule.waitUntil(3_000) { rule.onAllNodesWithText("Loading messages…").fetchSemanticsNodes().isEmpty() }
        scrollTo("conversation.message.h35"); rule.onNodeWithTag("conversation.message.h35").assertExists()
    }
    @Test fun unavailableEntryTargetCanBeCancelledWithoutBlankingTimelineOrDraft() {
        rule.setContent { WhiteNoiseTheme { Screen(target = "missing") } }
        rule.waitUntil(3_000) { rule.onAllNodesWithText("This message is no longer available.").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("This message is no longer available.").assertIsDisplayed()
        rule.onNodeWithText("Cancel").performClick(); waitForNoTarget()
        scrollTo("conversation.message.h59"); rule.onNodeWithTag("conversation.message.h59").assertExists()
        rule.onNodeWithText("Keep this draft").assertExists()
    }
    @Test fun pagingWindowSurvivesStateRestoration() {
        val restore = StateRestorationTester(rule)
        restore.setContent { WhiteNoiseTheme { Screen() } }
        scrollTo("history.Older"); rule.onNodeWithText("Load older messages").performClick()
        rule.waitUntil(3_000) { rule.onAllNodesWithText("Loading messages…").fetchSemanticsNodes().isEmpty() }
        scrollTo("conversation.message.h24")
        restore.emulateSavedInstanceStateRestore()
        scrollTo("conversation.message.h24"); rule.onNodeWithTag("conversation.message.h24").assertExists()
    }
    @Test fun pagingAfterUnavailableEntryDoesNotReplayItsFailedTarget() {
        rule.setContent { WhiteNoiseTheme { Screen(target = "missing") } }
        rule.waitUntil(3_000) { rule.onAllNodesWithText("This message is no longer available.").fetchSemanticsNodes().isNotEmpty() }
        scrollTo("history.Older"); rule.onNodeWithText("Load older messages").performClick()
        rule.waitUntil(3_000) { rule.onAllNodesWithText("Loading messages…").fetchSemanticsNodes().isEmpty() }
        scrollTo("conversation.message.h24")
        rule.mainClock.advanceTimeBy(1_000)
        rule.onNodeWithTag("history.target").assertDoesNotExist()
        rule.onNodeWithText("Keep this draft").assertExists()
    }
    @Test fun backgroundingInitialTargetCancelsJumpAndRestoresReadableWindow() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle = registry
        }
        val seen = mutableSetOf<String>()
        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        rule.mainClock.autoAdvance = false
        rule.setContent { CompositionLocalProvider(LocalLifecycleOwner provides owner) {
            WhiteNoiseTheme { Screen(target = "h0", onVisible = { seen += it }) }
        } }
        rule.mainClock.advanceTimeBy(32)
        rule.onNodeWithTag("history.target").assertExists()
        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        rule.mainClock.autoAdvance = true
        rule.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        waitForNoTarget()
        rule.waitUntil(4_000) { seen.isNotEmpty() }
        rule.runOnIdle { assertFalse("h0" in seen) }
        rule.onNodeWithText("Keep this draft").assertExists()
    }
    @Test fun visibleAcknowledgementsCannotConsumeUnloadedUnreadTail() {
        val seen = mutableSetOf<String>()
        rule.setContent { WhiteNoiseTheme { Screen(chat(30), onVisible = { seen += it }) } }
        rule.waitUntil(4_000) { seen.isNotEmpty() }
        rule.runOnIdle { assertTrue("h30" in seen); assertFalse("h59" in seen); assertTrue(seen.size < 30) }
    }
    @Test fun mentionFailureDoesNotAdvanceReadUntilRetryRevealsExactTarget() {
        var first = true; val readThrough = mutableListOf<String>()
        rule.setContent { WhiteNoiseTheme { Screen(chat(30), target = "h58", scenario = { op ->
            if (op == HistoryOperation.Target && first) { first = false; HistoryScenario.TargetFails } else HistoryScenario.Success
        }, onMention = { readThrough += it }) } }
        rule.onNodeWithTag("history.jumpMention").performClick()
        rule.waitUntil(3_000) { rule.onAllNodesWithText("Couldn’t load this message.").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { assertTrue(readThrough.isEmpty()) }
        rule.onNodeWithText("Retry").performClick()
        rule.waitUntil(4_000) { readThrough.isNotEmpty() }
        rule.runOnIdle { assertEquals(listOf("h30"), readThrough) }
        rule.onNodeWithTag("conversation.message.h30").assertExists()
    }
    @Test fun changingQueryCancelsOldTargetAndSearchDoesNotMarkRowsRead() {
        val seen = mutableSetOf<String>()
        rule.setContent { WhiteNoiseTheme { Screen(search = true, onVisible = { seen += it }) } }
        rule.onNodeWithTag("conversation.searchField").performTextInput("needle")
        rule.onNodeWithTag("conversation.searchField").performTextReplacement("Update 59")
        waitForMessage("h59")
        rule.waitUntil(3_000) { rule.onAllNodesWithTag("history.searchStatus").fetchSemanticsNodes().isEmpty() }
        rule.runOnIdle { assertTrue(seen.isEmpty()) }
        rule.onNodeWithTag("conversation.searchField").assertTextContains("Update 59")
    }
    @Test fun mediaViewerCanReturnToSourceOutsideLoadedHistory() {
        val photo = ProfileFixtures.marmota.chats.first { it.id == "catalog-media-viewer" }.timeline
            .filterIsInstance<ChatTimelineEntry.Message>().flatMap { it.message.attachments }
            .first { it.kind == MessageAttachmentKind.Photo && it.images.isNotEmpty() }
        val mediaChat = chat().let { base -> base.copy(timeline = base.timeline.map { entry ->
            if (entry is ChatTimelineEntry.Message && entry.id in setOf("h0", "h59")) {
                entry.copy(message = entry.message.copy(attachments = listOf(photo.copy(id = "${entry.id}-photo", images = photo.images.take(1)))))
            } else entry
        }) }
        rule.setContent { WhiteNoiseTheme { Screen(mediaChat) } }
        scrollTo("conversation.message.h59")
        rule.onNodeWithTag("conversation.media.tile.h59-photo.0").performClick()
        rule.onNodeWithTag("conversation.media.viewer.pager").performTouchInput { swipeRight() }
        rule.onNodeWithTag("conversation.media.viewer.position").assertTextContains("1 of 2", substring = true)
        rule.onNodeWithContentDescription("More options").performClick()
        rule.onNodeWithText("Go to Message").performClick()
        waitForMessage("h0")
        rule.onNodeWithTag("conversation.media.viewer.pager").assertDoesNotExist()
        rule.onNodeWithTag("conversation.message.h0").assertExists()
    }
    @Test fun chatInfoSearchReusesConversationEntryAndCloseDoesNotReopenOnReturn() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); dismissDiagnosticsPrompt(uiState.activeProfileId!!) }
        lateinit var nav: NavHostController
        rule.setContent { nav = rememberNavController(); WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) } }
        rule.runOnIdle { nav.navigate(AppRoute.Conversation("maya-chen")) }
        var entry = ""
        rule.runOnIdle { entry = nav.currentBackStackEntry!!.id; nav.navigate(AppRoute.ChatInfo("maya-chen")) }
        rule.onNodeWithText("Search").performClick()
        rule.onNodeWithTag("conversation.searchField").assertExists()
        rule.runOnIdle { assertEquals(entry, nav.currentBackStackEntry!!.id) }
        rule.onNodeWithContentDescription("Close search").performClick()
        rule.runOnIdle { nav.navigate(AppRoute.ChatInfo("maya-chen")); nav.popBackStack() }
        rule.onNodeWithTag("conversation.searchField").assertDoesNotExist()
    }
    @Test fun detailsShowReceiptSenderTimeExpiryStreamingAndExactCopyData() {
        val message = ChatMessage("message-to-copy", "friend", 3, "Today", 600, "10:00", "In progress", deliveryState = MessageDeliveryState.Streaming,
            createdAtMillis = 100_000, receivedAtMillis = 120_000, expiresAtMillis = 200_000)
        rule.setContent { WhiteNoiseTheme { MessageDetailsScreen(profile, chat(), message, {}) } }
        rule.onNodeWithText("Streaming").assertExists()
        rule.onNodeWithText("Sender’s sent time").assertExists(); rule.onNodeWithText("Disappears").assertExists()
        rule.onNodeWithContentDescription("Copy Message ID").performScrollTo().performClick()
        rule.runOnIdle {
            val clipboard = rule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals(message.id, clipboard.primaryClip!!.getItemAt(0).text.toString())
        }
        rule.onNodeWithContentDescription("Copy Sender Public Key").performScrollTo().performClick()
        rule.runOnIdle {
            val clipboard = rule.activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            assertEquals(profile.people.first().publicKey, clipboard.primaryClip!!.getItemAt(0).text.toString())
        }
    }
}
