package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class ConversationHistoryTest {
    private fun message(i: Int, author: String = "friend", text: String = "Message $i") = ChatMessage("m$i", author, 3, "Today", i, "$i", text)
    private fun chat(count: Int = 60) = Chat("chat", 0, ChatKind.Group, "Plans", timeline = (0 until count).map { ChatTimelineEntry.Message(message(it)) })
    @Test fun adjacentPagesAreBoundedOrderedDeduplicatedAndExhaustible() {
        val chat = chat(); var ids = ConversationHistory.initial(chat)
        assertEquals((42..59).map { "m$it" }, ConversationHistory.loaded(chat, ids).map { it.id })
        assertTrue(ConversationHistory.hasOlder(chat, ids)); assertFalse(ConversationHistory.hasNewer(chat, ids))
        ids = ConversationHistory.page(chat, ids, HistoryOperation.Older)
        assertEquals((24..59).map { "m$it" }, ConversationHistory.loaded(chat, ids).map { it.id })
        repeat(3) { ids = ConversationHistory.page(chat, ids, HistoryOperation.Older) }
        assertEquals(60, ids.size); assertFalse(ConversationHistory.hasOlder(chat, ids))
        assertEquals(ids, ConversationHistory.page(chat, ids, HistoryOperation.Newer))
    }
    @Test fun distantTargetLoadsSurroundingWindowWithBothDirectionsAvailable() {
        val chat = chat(); val ids = ConversationHistory.target(chat, "m30")!!
        assertEquals(18, ids.size); assertTrue("m30" in ids)
        assertTrue(ConversationHistory.hasOlder(chat, ids)); assertTrue(ConversationHistory.hasNewer(chat, ids))
        assertEquals((21..56).map { "m$it" }, ConversationHistory.loaded(chat, ConversationHistory.page(chat, ids, HistoryOperation.Newer)).map { it.id })
        assertNull(ConversationHistory.target(chat, "missing"))
    }
    @Test fun deletedTargetsFailAndRemovedWindowIdsNeverReturnPhantomRows() {
        val chat = chat(2).copy(timeline = listOf(ChatTimelineEntry.Message(message(0).copy(deletionState = MessageDeletionState.DeletedByOther)), ChatTimelineEntry.Event("event", "joined")))
        assertNull(ConversationHistory.target(chat, "m0")); assertNull(ConversationHistory.target(chat, "event"))
        assertTrue(ConversationHistory.loaded(chat, setOf("gone")).isEmpty())
        assertEquals(setOf("event", "m0"), ConversationHistory.page(chat, setOf("gone"), HistoryOperation.Older))
    }
    @Test fun pagingAndProjectionShareChronologyAndRepliesResolveOutsideWindow() {
        val source = message(0); val reply = message(40).copy(replyToMessageId = source.id)
        val chat = chat().copy(timeline = chat().timeline.reversed().map { if (it.id == reply.id) ChatTimelineEntry.Message(reply) else it })
        val window = ConversationHistory.target(chat, reply.id)!!
        assertFalse(source.id in window)
        val projected = ConversationProjection.items(chat, window).filterIsInstance<ConversationItem.MessageItem>()
        assertEquals(source, projected.first { it.id == reply.id }.resolvedReply)
        assertEquals(ConversationHistory.loaded(chat, window).map { it.id }, projected.map { it.id })
    }
    @Test fun resultIdentitySurvivesHistoryExpansionAndDeletedPinFallsBack() {
        val chat = chat(); val profile = Profile("me", "Me", "key")
        val loaded = ConversationSearch.results(chat.copy(timeline = ConversationHistory.loaded(chat, ConversationHistory.initial(chat))), profile, "Message")
        val all = ConversationSearch.results(chat, profile, "Message")
        val pin = loaded[4].messageId
        assertEquals(pin, all[ConversationHistory.searchCursor(all, pin)].messageId)
        assertEquals(0, ConversationHistory.searchCursor(all, "gone")); assertEquals(-1, ConversationHistory.searchCursor(emptyList(), pin))
    }
    @Test fun readCaptureIgnoresOutgoingDeletedAndSystemEntriesAndVisibleReadsArePartial() {
        val chat = chat(4).copy(unreadCount = 3, timeline = listOf(ChatTimelineEntry.Message(message(0)), ChatTimelineEntry.Event("event", "joined"),
            ChatTimelineEntry.Message(message(1, "me")), ChatTimelineEntry.Message(message(2).copy(deletionState = MessageDeletionState.DeletedByOther)), ChatTimelineEntry.Message(message(3))))
        val read = ConversationReading.initial(chat, "me")
        assertEquals(setOf("m0", "m3"), read.unreadIds)
        assertEquals("m0", ConversationReading.firstUnread(read, chat))
        assertEquals(setOf("m0"), ConversationReading.seen(read, setOf("m3", "event", "missing")).unreadIds)
        assertEquals(setOf("m0", "m3"), read.unreadIds)
    }
    @Test fun visibleReadingRejectsCoveredClippedAndZeroHeightViewports() {
        assertTrue(ConversationReading.actuallyVisible(20, 100, 0, 200))
        assertFalse(ConversationReading.actuallyVisible(150, 100, 0, 200))
        assertFalse(ConversationReading.actuallyVisible(-50, 100, 0, 200))
        assertFalse(ConversationReading.actuallyVisible(0, 100, 0, 0))
        assertFalse(ConversationReading.actuallyVisible(0, 100, 0, -20))
        assertTrue(ConversationReading.actuallyVisible(-50, 500, 0, 200))
    }
    @Test fun arrivingReceivedMessagesBecomeUnreadButOwnSendAndDeletionDoNotInventUnread() {
        val base = chat(2); val read = ConversationReading.initial(base, "me")
        val next = base.copy(timeline = base.timeline + ChatTimelineEntry.Message(message(2)) + ChatTimelineEntry.Message(message(3, "me")))
        val reconciled = ConversationReading.reconcile(read, next, "me")
        assertEquals(setOf("m2"), reconciled.unreadIds)
        assertEquals(reconciled, ConversationReading.reconcile(reconciled, next, "me"))
        assertTrue(ConversationReading.reconcile(reconciled, base, "me").unreadIds.isEmpty())
    }
    @Test fun offTailUnreadStackFreezesFirstArrivalAndCannotRetargetUntilCleared() {
        val chat = chat(4)
        var jump = ConversationReading.jump(ConversationUnreadJump(), ConversationReadState(emptySet(), emptySet()), chat, false)
        jump = ConversationReading.jump(jump, ConversationReadState(setOf("m1"), emptySet()), chat, false)
        assertEquals("m1", jump.pendingId)
        assertEquals(jump, ConversationReading.jump(jump, ConversationReadState(setOf("m1", "m3"), emptySet()), chat, false))
        val consumed = ConversationReading.jump(jump, ConversationReadState(setOf("m3"), emptySet()), chat, false)
        assertNull(consumed.pendingId); assertTrue(consumed.stackActive)
        assertNull(ConversationReading.jump(consumed, ConversationReadState(setOf("m3"), emptySet()), chat, false).pendingId)
        val cleared = ConversationReading.jump(consumed, ConversationReadState(emptySet(), emptySet()), chat, false)
        assertEquals("m3", ConversationReading.jump(cleared, ConversationReadState(setOf("m3"), emptySet()), chat, false).pendingId)
    }
    @Test fun entryUnreadAndNearTailArrivalsDoNotCreateOffTailJump() {
        val chat = chat(2); val read = ConversationReadState(setOf("m1"), emptySet())
        assertNull(ConversationReading.jump(ConversationUnreadJump(), read, chat, false).pendingId)
        assertNull(ConversationReading.jump(ConversationUnreadJump(initialized = true), read, chat, true).pendingId)
    }
    @Test fun mentionJumpUsesCompleteNamesAndOnlyUnreadReceivedMessages() {
        val profile = Profile("me", "Maya Chen", "npub-key")
        val messages = listOf(message(0, text = "Hello @Maya Chen!"), message(1, text = "Hello @Maya Chens!"), message(2, "me", "@Maya Chen"), message(3, text = "@npub-key check this"))
        val chat = chat().copy(timeline = messages.map(ChatTimelineEntry::Message))
        val read = ConversationReadState(messages.mapTo(hashSetOf()) { it.id }, messages.mapTo(hashSetOf()) { it.id })
        assertEquals(listOf("m0", "m3"), ConversationReading.mentions(read, chat, profile))
        assertEquals(setOf("m3"), ConversationReading.through(read, chat, "m2").unreadIds)
        assertEquals(read, ConversationReading.through(read, chat, "missing"))
    }
}
