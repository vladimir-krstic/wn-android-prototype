package dev.ipf.whitenoise

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.rememberNavController
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
class ReadAloudTransportTest {
    @get:Rule val rule = createAndroidComposeRule<EmptyTestActivity>()
    private fun profile(body: String = "First sentence. Second sentence.", count: Int = 16): Profile {
        val messages = (0 until count).map { ChatTimelineEntry.Message(ChatMessage("m$it", "other", 1, "Today", it, "Now", body)) }
        return Profile("p", "Name", "public", chats = listOf(Chat("c", 0, ChatKind.Direct("other"), "Chat", timeline = messages)))
    }
    private fun speech(profile: () -> Profile, output: (String, SpeechToken) -> Boolean = { _, _ -> true }) = ReadAloudController().apply {
        attachTestOutput(output); this.profile = profile
    }
    private fun start(controller: ReadAloudController, p: Profile, id: String = "m0") = controller.startConversation(p, p.chats.single(), id)
    private fun transport(controller: ReadAloudController) { rule.setContent { WhiteNoiseTheme { ReadAloudTransport(controller) } } }

    @Test fun pauseAndPausedNavigationPreserveSourceUntilExplicitResume() {
        val p = profile(); var enqueued = 0; val c = speech({ p }) { _, _ -> enqueued++; true }; start(c, p); transport(c)
        rule.onNodeWithTag("speech.pauseResume").performScrollTo().performClick()
        rule.onNodeWithTag("speech.nextSentence").performClick()
        rule.onNodeWithTag("speech.nextMessage").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, enqueued); assertEquals(SpeechPhase.Paused, c.session!!.phase); assertEquals("m1", c.activeMessageId) }
        rule.onNodeWithContentDescription("Resume reading").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(2, enqueued); assertEquals(SpeechPhase.Speaking, c.session!!.phase) }
    }
    @Test fun stopInvalidatesQueuedCallbacksAndRemovesTransport() {
        val p = profile(); val c = speech({ p }); start(c, p); val token = c.session!!.token!!; transport(c)
        rule.onNodeWithTag("speech.stop").performClick()
        rule.runOnIdle { c.done(token); c.range(token, 2); c.error(token); assertNull(c.session) }
        rule.onNodeWithTag("speech.transport").assertDoesNotExist()
    }
    @Test fun engineErrorShowsRetryAndRetainsTheSource() {
        val p = profile(); var requests = 0; val c = speech({ p }) { _, _ -> ++requests > 1 }; start(c, p); transport(c)
        rule.onNodeWithText("Couldn’t read this text aloud.").assertExists()
        rule.onNodeWithTag("speech.retry").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(2, requests); assertEquals("m0", c.activeMessageId); assertEquals(SpeechPhase.Speaking, c.session!!.phase) }
    }
    @Test fun historyFailureRetrySettlesTheSameEdge() {
        val p = profile().copy(developerTools = DeveloperToolsState(isEnabled = true)); val c = speech({ p })
        start(c, p, "m7"); c.setEdgeScenario(p.id, SpeechEdgeScenario.LaterFailure)
        while (c.session!!.phase != SpeechPhase.Loading) c.move(SpeechMove.NextMessage)
        val before = c.session!!; c.settleEdge(before.id, before.revision); transport(c)
        rule.onNodeWithText("Couldn’t load more messages.").assertExists()
        rule.onNodeWithTag("speech.retry").performScrollTo().performClick()
        rule.runOnIdle { val loading = c.session!!; c.settleEdge(loading.id, loading.revision); assertEquals(before.edge!!.targetIndex, c.session!!.messageIndex) }
        rule.onNodeWithText("Couldn’t load more messages.").assertDoesNotExist()
    }
    @Test fun sentenceChooserTargetsTheSecondRepeatedMarkdownPassage() {
        val body = "**Repeat this.**\n\n*Repeat this.*"; val p = profile(body); val c = speech({ p })
        val message = (p.chats.single().timeline.first() as ChatTimelineEntry.Message).message
        var open by mutableStateOf(true)
        rule.setContent { WhiteNoiseTheme { if (open) SpeechSentenceChooser(message, { offset -> c.startConversation(p, p.chats.single(), message.id, sourceOffset = offset); open = false }, { open = false }) } }
        rule.onNodeWithTag("speech.sentence.1").performClick()
        rule.runOnIdle { assertEquals(1, c.session!!.sentenceIndex); assertEquals(body.lastIndexOf("Repeat"), c.activePassage!!.sourceStart) }
    }
    @Test fun sourceHighlightIdentifiesOnlyTheRepeatedOccurrenceBeingSpoken() {
        val source = "**Repeat this.**\n\n*Repeat this.*"; val start = source.lastIndexOf("Repeat")
        rule.setContent { WhiteNoiseTheme { MessageDocumentContent(MessageDocuments.parse(source), emptyList(), {}, spokenRange = start until start + 12) } }
        rule.onAllNodes(hasText("Repeat this.")).assertCountEquals(2)
        val current = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Current spoken sentence")
        rule.onAllNodes(current).assertCountEquals(1)
        rule.onAllNodes(hasText("Repeat this."))[1].assert(current)
    }
    @Test fun readerChooserDoesNotRequireSelectionOrLongPress() {
        val p = profile(); val c = speech({ p }); val chat = p.chats.single(); val message = (chat.timeline.first() as ChatTimelineEntry.Message).message
        rule.setContent { WhiteNoiseTheme { MessageReaderDialog(p, chat, message, false, MessageSpeechActionState(), c, {}, {}, {}, {}) } }
        rule.onNodeWithTag("speech.choose").performScrollTo().performClick()
        rule.onNodeWithTag("speech.sentence.1").performClick()
        rule.runOnIdle { assertEquals(1, c.session!!.sentenceIndex); assertEquals(SpeechPhase.Speaking, c.session!!.phase) }
    }
    @Test fun manualReaderScrollSuspendsFollowAndExplicitResumeRestoresIt() {
        val p = profile((1..60).joinToString("\n\n") { "Paragraph $it has readable text." }); val c = speech({ p })
        val chat = p.chats.single(); val message = (chat.timeline.first() as ChatTimelineEntry.Message).message; start(c, p)
        rule.setContent { WhiteNoiseTheme { MessageReaderDialog(p, chat, message, false, MessageSpeechActionState(reading = true), c, {}, {}, {}, {}) } }
        rule.onNodeWithText("Paragraph 1 has readable text.").performTouchInput { swipeUp() }
        rule.runOnIdle { assertFalse(c.session!!.following) }
        rule.onNodeWithTag("speech.follow").performScrollTo().performClick()
        rule.runOnIdle { assertTrue(c.session!!.following) }
    }
    @Test fun shellSourceEditInvalidatesPlaybackWhileAnotherRouteIsVisible() {
        var p by mutableStateOf(profile()); val c = speech({ p }); start(c, p)
        rule.setContent { CompositionLocalProvider(LocalReadAloudController provides c) { WhiteNoiseTheme { ReadAloudHost(p, {}) { Text("Settings", it) } } } }
        rule.runOnIdle {
            val chat = p.chats.single(); val entry = chat.timeline.first() as ChatTimelineEntry.Message
            p = p.copy(chats = listOf(chat.copy(timeline = listOf(entry.copy(message = entry.message.copy(text = "Edited"))) + chat.timeline.drop(1))))
        }
        rule.onNodeWithText("This message is no longer available.").assertExists()
        rule.onNodeWithTag("speech.return").assertDoesNotExist()
        rule.runOnIdle { assertNull(c.session!!.token) }
    }
    @Test fun profileSwitchClearsSpeechAndCancelsAnOldReturnAction() {
        var p by mutableStateOf(profile()); val c = speech({ p }); start(c, p); val target = c.session!!.returnTarget!!; var returns = 0
        rule.setContent { CompositionLocalProvider(LocalReadAloudController provides c) { WhiteNoiseTheme { ReadAloudHost(p, { returns++ }) { Text("Settings", it) } } } }
        rule.runOnIdle { p = p.copy(id = "other") }
        rule.runOnIdle { c.returnToSource(target); assertNull(c.session); assertEquals(0, returns) }
        rule.onNodeWithTag("speech.transport").assertDoesNotExist()
    }
    @Test fun attachmentSpeechReplacesTheChatQueueAndOnlyItsOwnDismissalStopsIt() {
        val p = profile(); val c = speech({ p }); start(c, p); val token = c.session!!.token!!; transport(c)
        rule.runOnIdle { c.toggle("attachment:p:m0:a", "File text. Next sentence."); c.done(token); c.stopAttachment("other") }
        rule.runOnIdle { assertEquals("attachment:p:m0:a", c.activeMessageId); assertNull(c.session!!.returnTarget) }
        rule.onNodeWithTag("speech.nextSentence").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(1, c.session!!.sentenceIndex); c.stopAttachment("attachment:p:m0:a"); assertNull(c.session) }
    }
    @Test fun sourceReturnFromSettingsOpensTheOwningMessageWithoutRestartingPlayback() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); dismissDiagnosticsPrompt(uiState.activeProfileId!!); sendText("fiatjaf", "Return to this sentence. Another sentence.") }
        val p = vm.uiState.activeProfile!!; val chat = vm.chat("fiatjaf")!!; val message = (chat.timeline.last() as ChatTimelineEntry.Message).message
        val c = speech({ vm.uiState.activeProfile!! }); c.startConversation(p, chat, message.id); val token = c.session!!.token
        lateinit var nav: androidx.navigation.NavHostController
        rule.setContent { CompositionLocalProvider(LocalReadAloudController provides c) { nav = rememberNavController(); WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) } } }
        rule.runOnIdle { nav.navigate(AppRoute.Settings()) }
        rule.onNodeWithTag("speech.return").performScrollTo().performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithTag("conversation.message.${message.id}").fetchSemanticsNodes().isNotEmpty() }
        rule.runOnIdle { assertEquals(token, c.session!!.token); assertEquals(p.id, vm.uiState.activeProfileId) }
    }
    @Test fun followingDoesNotAcknowledgeMessagesAsRead() {
        val p = profile(); val chat = p.chats.single(); val c = speech({ p }); start(c, p, "m5"); val seen = mutableSetOf<String>()
        rule.setContent { CompositionLocalProvider(LocalReadAloudController provides c) { WhiteNoiseTheme { ConversationScreen(p, chat, {}, { false }, {}, {}, {}, onMessagesVisible = { seen += it }) } } }
        rule.waitForIdle()
        rule.runOnIdle { assertTrue(seen.isEmpty()) }
    }
    @Test fun largeTextRtlTransportKeepsAllNamedControlsReachable() {
        val p = profile(); val c = speech({ p }); start(c, p)
        rule.setContent { val density = LocalDensity.current; CompositionLocalProvider(LocalDensity provides Density(density.density, 2f), LocalLayoutDirection provides LayoutDirection.Rtl) { WhiteNoiseTheme { ReadAloudTransport(c) } } }
        listOf("speech.previousMessage", "speech.nextMessage", "speech.previousSentence", "speech.nextSentence", "speech.pauseResume", "speech.follow", "speech.stop").forEach {
            rule.onNodeWithTag(it).performScrollTo().assertIsDisplayed().assertIsEnabled()
        }
    }
    @Test fun completionAllowsTheSameFileToBeReadAgainWithoutAnExtraStop() {
        val p = profile(); val c = speech({ p }); c.toggle("file", "One sentence."); transport(c)
        rule.runOnIdle { c.done(c.session!!.token!!); assertNull(c.activeMessageId); assertNull(c.activePassage) }
        rule.onNodeWithText("Reading finished.").assertExists()
        rule.runOnIdle { c.toggle("file", "One sentence."); assertEquals(SpeechPhase.Speaking, c.session!!.phase); assertEquals("file", c.activeMessageId) }
    }

}
