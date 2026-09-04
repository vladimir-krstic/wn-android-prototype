package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.ui.conversation.ConversationHistoryUiState
import org.junit.Assert.*
import org.junit.Test

class ConversationReadingStateTest {
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
    @Test fun openingCapturesUnreadIdsAndVisibleAcknowledgementsArePartialAndIdempotent() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!; val before = vm.chat("catalog-direct-replies")!!
        vm.openChat(before.id); val opened = vm.chat(before.id)!!
        assertEquals(before.unreadCount, opened.unreadCount); assertTrue(opened.isUnread)
        val first = opened.readState!!.unreadIds.first(); val remaining = opened.readState.unreadIds - first
        repeat(2) { assertTrue(vm.markConversationVisible(owner, before.id, setOf(first))) }
        assertEquals(remaining, vm.chat(before.id)!!.readState!!.unreadIds)
        assertEquals(remaining.size, vm.chat(before.id)!!.unreadCount)
        vm.openChat(before.id); assertEquals(remaining, vm.chat(before.id)!!.readState!!.unreadIds)
    }
    @Test fun staleOwnerOrMissingTargetCannotReadAfterProfileSwitch() {
        val vm = model(); val owner = vm.uiState.activeProfile!!; val chat = owner.chats.first { it.timeline.any { entry -> entry is ChatTimelineEntry.Message } }
        val message = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().first().id
        val before = vm.uiState
        assertFalse(vm.markConversationVisible(owner.id, chat.id, setOf("missing"))); assertFalse(vm.markConversationThrough(owner.id, chat.id, "missing"))
        assertEquals(before, vm.uiState)
        vm.completeSignIn(OnboardingOrigin.AddProfile); val switched = vm.uiState
        assertFalse(vm.markConversationVisible(owner.id, chat.id, setOf(message))); assertFalse(vm.markConversationThrough(owner.id, chat.id, message))
        assertEquals(switched, vm.uiState)
    }
    @Test fun intentionalMentionReadAdvancesOnlyThroughResolvedTarget() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!; val id = "catalog-direct-replies"
        vm.openChat(id); val chat = vm.chat(id)!!; val read = chat.readState!!
        val first = ConversationReading.firstUnread(read, chat)!!
        val expected = ConversationReading.through(read, chat, first)
        assertTrue(vm.markConversationThrough(owner, id, first))
        assertEquals(expected.unreadIds, vm.chat(id)!!.readState!!.unreadIds)
    }
    @Test fun incomingMentionStaysUnreadThroughOwnSendAndProfileRoundTrip() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!; val id = "maya-chen"
        vm.markAllChatsRead(); vm.openChat(id); vm.setDeveloperToolsEnabled(true)
        assertTrue(vm.addConversationArrival(owner, id))
        val incoming = vm.chat(id)!!.readState!!.unreadIds.single()
        assertEquals(listOf(incoming), ConversationReading.mentions(vm.chat(id)!!.readState!!, vm.chat(id)!!, vm.uiState.activeProfile!!))
        vm.sendText(id, "I will check later.")
        val own = vm.chat(id)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().id
        assertTrue(vm.markConversationVisible(owner, id, setOf(own)))
        assertEquals(setOf(incoming), vm.chat(id)!!.readState!!.unreadIds)
        vm.completeSignIn(OnboardingOrigin.AddProfile); vm.selectProfile(owner); vm.openChat(id)
        assertEquals(setOf(incoming), vm.chat(id)!!.readState!!.unreadIds)
    }
    @Test fun deletionPrunesUnreadAndExplicitReadUndoRestoresItsCapturedIds() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!; val id = "catalog-direct-replies"
        vm.openChat(id); val before = vm.chat(id)!!; val unread = before.readState!!.unreadIds.first()
        val undo = ChatListUndo.capture(owner, before, ChatListAction.Read)
        vm.markChatUnread(id, false); assertTrue(vm.chat(id)!!.readState!!.unreadIds.isEmpty())
        vm.undoChatListAction(undo)
        assertEquals(before.readState, vm.chat(id)!!.readState)
        vm.deleteMessages(id, setOf(unread), MessageDeletionScope.ForMe)
        assertFalse(unread in vm.chat(id)!!.readState!!.unreadIds)
        assertFalse(vm.markConversationThrough(owner, id, unread))
        vm.markAllChatsRead(); assertTrue(vm.chat(id)!!.readState!!.unreadIds.isEmpty())
    }
    @Test fun historyScenarioIsConsumedOnlyByMatchingOperationForCurrentOwner() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        vm.selectHistoryScenario(HistoryScenario.SearchFails); assertEquals(HistoryScenario.Success, vm.nextHistoryScenario)
        vm.setDeveloperToolsEnabled(true); vm.selectHistoryScenario(HistoryScenario.SearchFails)
        assertEquals(HistoryScenario.TargetUnavailable, vm.consumeHistoryScenario("wrong", HistoryOperation.Search))
        assertEquals(HistoryScenario.Success, vm.consumeHistoryScenario(owner, HistoryOperation.Older))
        assertEquals(HistoryScenario.SearchFails, vm.consumeHistoryScenario(owner, HistoryOperation.Search))
        assertEquals(HistoryScenario.Success, vm.consumeHistoryScenario(owner, HistoryOperation.Search))
    }
    @Test fun cancelledAndSupersededWindowRequestsCannotReplaceCurrentWindow() {
        val chat = ProfileFixtures.marmota.chats.first { it.timeline.size > 20 }
        val state = ConversationHistoryUiState(ConversationHistory.initial(chat), chat.timeline.mapTo(hashSetOf()) { it.id }, null)
        state.page(HistoryOperation.Older, HistoryScenario.Success); val old = state.request!!; val before = state.windowIds
        state.cancel(); state.complete(chat, old); assertEquals(before, state.windowIds)
        state.page(HistoryOperation.Older, HistoryScenario.OlderFails); val failed = state.request!!
        state.complete(chat, failed); assertEquals(HistoryPhase.Failed, state.request!!.phase); assertEquals(before, state.windowIds)
        state.retry(); val retry = state.request!!; state.complete(chat, old); assertEquals(retry, state.request)
        state.complete(chat, retry); assertTrue(state.windowIds.size > before.size)
    }
    @Test fun targetDeletedDuringLoadingBecomesUnavailableAndCannotProduceReadIntent() {
        val chat = ProfileFixtures.marmota.chats.first { it.timeline.size > 20 }
        val target = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().first().id
        val state = ConversationHistoryUiState(emptySet(), emptySet(), null)
        state.target(chat, target, HistoryScenario.Success, markThrough = true)
        state.complete(chat.copy(timeline = chat.timeline.filterNot { it.id == target }), state.request!!)
        assertEquals(HistoryPhase.Unavailable, state.request!!.phase); assertNull(state.readyTarget)
    }
}
