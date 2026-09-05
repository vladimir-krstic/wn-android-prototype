package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class ConversationUnreadDividerTest {
    private fun message(i: Int, author: String = "friend") = ChatTimelineEntry.Message(
        ChatMessage("m$i", author, 3, "Today", i, "$i", "Message $i"))
    private fun chat() = Chat("chat", 0, ChatKind.Group, "Plans", unreadCount = 3,
        timeline = (0..4).map { message(it) })

    @Test fun capturesFirstUnreadAndCountWithoutChangingReadState() {
        val chat = chat()
        val divider = ConversationUnreadDivider.capture(chat, "me")!!
        assertEquals("m2", divider.firstId)
        assertEquals(setOf("m2", "m3", "m4"), divider.messageIds)
        assertNull(chat.readState)
        assertEquals(3, chat.unreadCount)
    }

    @Test fun readingDoesNotMoveOrShrinkTheDividerDuringTheVisit() {
        val chat = chat()
        val divider = ConversationUnreadDivider.capture(chat, "me")!!
        val read = ConversationReading.initial(chat, "me")
        val partiallyRead = chat.copy(readState = ConversationReading.seen(read, setOf("m2")))
        assertEquals(divider, divider.reconcile(partiallyRead, "me"))
        val fullyRead = chat.copy(readState = read.copy(unreadIds = emptySet()), unreadCount = 0)
        assertEquals(divider, divider.reconcile(fullyRead, "me"))
        // Reopening starts a new visit and therefore has no unread separator.
        assertNull(ConversationUnreadDivider.capture(fullyRead, "me"))
    }

    @Test fun incomingMessagesGrowTheSectionWithoutMovingItsAnchor() {
        val chat = chat().let { it.copy(readState = ConversationReading.initial(it, "me")) }
        val divider = ConversationUnreadDivider.capture(chat, "me")!!
        val updated = divider.reconcile(chat.copy(timeline = chat.timeline + message(5)), "me")!!
        assertEquals("m2", updated.firstId)
        assertEquals(setOf("m2", "m3", "m4", "m5"), updated.messageIds)
    }

    @Test fun replyingClearsTheSeparatorWithoutBulkReading() {
        val chat = chat().let { it.copy(readState = ConversationReading.initial(it, "me")) }
        val divider = ConversationUnreadDivider.capture(chat, "me")!!
        val replied = chat.copy(timeline = chat.timeline + message(5, "me"))
        assertNull(divider.reconcile(replied, "me"))
        assertEquals(setOf("m2", "m3", "m4"), replied.readState!!.unreadIds)
    }

    @Test fun deletedAnchorFallsForwardAndAnEmptySectionDisappears() {
        val chat = chat()
        val divider = ConversationUnreadDivider.capture(chat, "me")!!
        val pruned = chat.copy(timeline = chat.timeline.filterNot { it.id == "m2" },
            readState = ConversationReading.initial(chat, "me"))
        assertEquals("m3", divider.reconcile(pruned, "me")!!.firstId)
        assertEquals(setOf("m3", "m4"), divider.reconcile(pruned, "me")!!.messageIds)
        assertNull(divider.reconcile(chat.copy(timeline = chat.timeline.take(2)), "me"))
    }

    @Test fun ownMessagesAndEventsDoNotInflateUnreadCount() {
        val base = chat().copy(timeline = listOf(message(0, "me"),
            ChatTimelineEntry.Notice("notice", "Group updated"), message(1), message(2)), unreadCount = 2)
        assertEquals(setOf("m1", "m2"), ConversationUnreadDivider.capture(base, "me")!!.messageIds)
        assertNull(ConversationUnreadDivider.capture(base.copy(unreadCount = 0), "me"))
    }
}
