package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationProjectionTest {
    @Test
    fun fixtureGraphKeepsOnlyTheRetainedDraftWithoutSentTimelineContent() {
        val chats = ChatFixtures.populatedChats(ProfileFixtures.MARMOTA_ID)

        assertEquals(listOf("mina-park"), chats.filter { it.timeline.isEmpty() }.map { it.id })
        assertEquals(listOf("mina-park"), chats.filter { ConversationProjection.items(it).isEmpty() }.map { it.id })
        assertEquals(8, chats.chat("fiatjaf").timeline.filterIsInstance<ChatTimelineEntry.Message>().size)
        assertEquals(9, chats.chat("catalog-group-colors").timeline.filterIsInstance<ChatTimelineEntry.Message>().size)
    }

    @Test
    fun entriesSortChronologicallyAndEmitOneHeaderPerDay() {
        val chat = ChatFixtures.populatedChats(ProfileFixtures.MARMOTA_ID).chat("catalog-direct-dates")
        val items = ConversationProjection.items(chat)
        val headers = items.filterIsInstance<ConversationItem.DayHeader>()

        assertEquals(
            listOf("Dec 8, 2025", "Mon, Jul 14", "Wednesday", "Thursday", "Friday", "Saturday", "Yesterday", "Today"),
            headers.map { it.label },
        )
        assertEquals("DATE-01", items.filterIsInstance<ConversationItem.MessageItem>().first().message.id)
        assertEquals("DATE-15", items.filterIsInstance<ConversationItem.MessageItem>().last().message.id)
    }

    @Test
    fun clusteringUsesAuthorDayFiveMinutesAndEventBoundaries() {
        val chat = Chat(
            id = "cluster",
            originalOrder = 0,
            kind = ChatKind.Group,
            title = "Cluster",
            timeline = listOf(
                entry("one", "maya", 600),
                entry("two", "maya", 605),
                entry("three", "maya", 611),
                ChatTimelineEntry.Event("event", "Event", 3, "Today", 612),
                entry("four", "maya", 613),
            ),
        )
        val messages = ConversationProjection.items(chat).filterIsInstance<ConversationItem.MessageItem>()

        assertTrue(messages[0].startsCluster)
        assertFalse(messages[0].endsCluster)
        assertFalse(messages[1].startsCluster)
        assertTrue(messages[1].endsCluster)
        assertTrue(messages[2].startsCluster)
        assertTrue(messages[2].endsCluster)
        assertTrue(messages[3].startsCluster)
    }

    @Test
    fun replyResolutionRetainsAvailableAndMissingFallbacks() {
        val chat = ChatFixtures.populatedChats(ProfileFixtures.MARMOTA_ID).chat("catalog-direct-replies")
        val messages = ConversationProjection.items(chat).filterIsInstance<ConversationItem.MessageItem>()

        val available = messages.first { it.message.id == "RPL-02" }
        val deleted = messages.first { it.message.id == "RPL-03" }
        val missing = messages.first { it.message.id == "RPL-04" }
        assertEquals("RPL-02-source", available.resolvedReply?.id)
        assertTrue(deleted.hasUnavailableReply)
        assertTrue(missing.hasUnavailableReply)
    }

    @Test
    fun supportNoticeDoesNotInventTodayHeaderBeforeFirstMessage() {
        val support = ChatFixtures.populatedChats(ProfileFixtures.MARMOTA_ID).chat(ChatFixtures.SUPPORT_CHAT_ID)
        val items = ConversationProjection.items(support)

        assertTrue(items.single() is ConversationItem.NoticeItem)
    }

    @Test
    fun deletedCopyDependsOnDeletionOwner() {
        val mine = ChatMessage("mine", "marmota", 3, "Today", 600, "10:00 AM", deletionState = MessageDeletionState.DeletedByCurrentProfile)
        val theirs = ChatMessage("theirs", "maya", 3, "Today", 601, "10:01 AM", deletionState = MessageDeletionState.DeletedByOther)

        assertEquals("You deleted this message.", mine.visibleText("marmota"))
        assertEquals("This message was deleted.", theirs.visibleText("marmota"))
    }

    @Test
    fun composerAvailabilityCoversAllLifecycleBlocks() {
        val profile = ProfileFixtures.marmota
        assertEquals(ComposerAvailability.PendingInvitation, profile.chats.chat("catalog-direct-invitation").composerAvailability(profile))
        assertEquals(ComposerAvailability.Left, profile.chats.chat("catalog-direct-left").composerAvailability(profile))
        assertEquals(ComposerAvailability.Removed, profile.chats.chat("catalog-group-removed").composerAvailability(profile))
        assertEquals(ComposerAvailability.Blocked, profile.chats.chat("catalog-direct-blocked").composerAvailability(profile))
        assertEquals(ComposerAvailability.MissingRelays, profile.chats.chat("catalog-direct-missing-relays").composerAvailability(profile))
        assertEquals(ComposerAvailability.Available, profile.chats.chat("fiatjaf").composerAvailability(profile))
    }

    private fun entry(id: String, author: String, minute: Int) = ChatTimelineEntry.Message(
        ChatMessage(id, author, 3, "Today", minute, "10:00 AM", id),
    )

    private fun List<Chat>.chat(id: String): Chat = firstOrNull { it.id == id }.also(::assertNotNull)!!
}
