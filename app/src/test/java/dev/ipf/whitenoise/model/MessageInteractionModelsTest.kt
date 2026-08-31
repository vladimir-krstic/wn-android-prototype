package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageInteractionModelsTest {
    @Test
    fun actionPolicyKeepsRequestedOrderAndConditionalCopyRetry() {
        val incomingAttachment = ChatMessage(
            "attachment",
            "maya-chen",
            3,
            "Today",
            600,
            "10:00 AM",
            attachments = listOf(MessageAttachment("file", MessageAttachmentKind.File, "Plan.pdf")),
        )
        val failedText = incomingAttachment.copy(
            id = "failed",
            authorId = ProfileFixtures.MARMOTA_ID,
            text = "Retry this",
            deliveryState = MessageDeliveryState.Failed,
        )

        assertEquals(
            listOf(MessageAction.Reply, MessageAction.Forward, MessageAction.Select, MessageAction.Info, MessageAction.Delete),
            MessageActionPolicy.available(incomingAttachment, ProfileFixtures.MARMOTA_ID),
        )
        assertEquals(MessageAction.RetrySend, MessageActionPolicy.available(failedText, ProfileFixtures.MARMOTA_ID).first())
        assertTrue(MessageAction.Copy in MessageActionPolicy.available(failedText, ProfileFixtures.MARMOTA_ID))
        assertTrue(MessageActionPolicy.available(failedText.copy(deletionState = MessageDeletionState.DeletedByCurrentProfile), ProfileFixtures.MARMOTA_ID).isEmpty())
    }

    @Test
    fun forwardingAndDeletionPoliciesEnforceBoundsAndDirection() {
        val mine = ChatMessage("mine", "me", 3, "Today", 600, "10:00 AM", "Text")
        val theirs = mine.copy(id = "theirs", authorId = "them")

        assertTrue(MessageActionPolicy.canDeleteForEveryone(listOf(mine), "me"))
        assertFalse(MessageActionPolicy.canDeleteForEveryone(listOf(mine, theirs), "me"))
        assertTrue(MessageActionPolicy.canForward(List(32) { mine.copy(id = "$it") }))
        assertFalse(MessageActionPolicy.canForward(List(33) { mine.copy(id = "$it") }))
        assertFalse(MessageActionPolicy.canForward(listOf(mine.copy(deletionState = MessageDeletionState.DeletedByOther))))
    }

    @Test
    fun quickReactionReplacementSwapsToRemainUnique() {
        val moved = ReactionCatalog.replaceQuick(ReactionCatalog.defaults, 0, "🔥")
        assertEquals("🔥", moved[0])
        assertEquals("❤", moved[2])
        assertEquals(6, moved.distinct().size)
        assertEquals(ReactionCatalog.defaults, ReactionCatalog.replaceQuick(ReactionCatalog.defaults, 9, "😀"))
        assertEquals(7, ReactionCatalog.quickStrip(ReactionCatalog.defaults, "😀").size)
    }

    @Test
    fun reactionSummaryKeepsUpToFourTypesPlusOverflowAndAdaptsToWidth() {
        val reactions = listOf("❤", "😀", "🔥", "🦫", "🚀").mapIndexed { index, emoji ->
            MessageReaction(
                emoji = emoji,
                personIds = if (index == 3) listOf("me", "other") else listOf("other"),
            )
        }

        val full = ReactionCatalog.summary(reactions, "me")
        val constrained = ReactionCatalog.summary(reactions, "me", maximumReactionPills = 2)

        assertEquals(5, full.size)
        assertEquals(listOf("❤", "😀", "🔥", "🦫", null), full.map { it.emoji })
        assertEquals(1, full.last().omittedTypeCount)
        assertEquals(3, constrained.size)
        assertEquals(listOf("❤", "😀", null), constrained.map { it.emoji })
        assertEquals(3, constrained.last().omittedTypeCount)
        assertEquals(4, constrained.last().personCount)
        assertTrue(constrained.last().selected)

        val beyondMaximum = ReactionCatalog.summary(
            (0..8).map { MessageReaction("$it", listOf("other")) },
            "me",
        )
        assertEquals(5, beyondMaximum.size)
        assertEquals(5, beyondMaximum.last().omittedTypeCount)
    }

    @Test
    fun recipientSpeechCommandsLiveInMessageActionsAndFollowTranscriptState() {
        val incomingText = ChatMessage(
            "incoming-text",
            "them",
            3,
            "Today",
            600,
            "10:00 AM",
            "Read this",
        )
        val incomingVoice = incomingText.copy(
            id = "incoming-voice",
            text = "",
            attachments = listOf(
                MessageAttachment(
                    "voice",
                    MessageAttachmentKind.Voice,
                    "Voice message",
                    durationSeconds = 8,
                ),
            ),
        )

        assertTrue(MessageAction.ReadAloud in MessageActionPolicy.available(incomingText, "me"))
        assertTrue(
            MessageAction.StopReading in MessageActionPolicy.available(
                incomingText,
                "me",
                MessageSpeechActionState(reading = true),
            ),
        )
        assertTrue(MessageAction.Transcribe in MessageActionPolicy.available(incomingVoice, "me"))
        assertTrue(
            MessageAction.ShowTranscript in MessageActionPolicy.available(
                incomingVoice,
                "me",
                MessageSpeechActionState(transcriptAvailable = true),
            ),
        )
        val visible = MessageActionPolicy.available(
            incomingVoice,
            "me",
            MessageSpeechActionState(transcriptAvailable = true, transcriptVisible = true),
        )
        assertTrue(MessageAction.HideTranscript in visible)
        assertTrue(MessageAction.CopyTranscript in visible)
    }

    @Test
    fun conversationSearchMatchesTextSenderAndAttachmentNewestFirst() {
        val profile = ProfileFixtures.marmota
        val rich = profile.chats.first { it.id == "catalog-media-rich" }
        val text = ConversationSearch.results(rich, profile, "Useful")
        val sender = ConversationSearch.results(rich, profile, "Maya Chen")
        val file = ConversationSearch.results(rich, profile, "Project Brief")
        val link = ConversationSearch.results(rich, profile, "Android Developers")

        assertEquals(listOf("RICH-02"), text.map { it.messageId })
        assertTrue(sender.any { it.messageId == "RICH-01" })
        assertEquals(listOf("RICH-01"), file.map { it.messageId })
        assertEquals(listOf("RICH-02"), link.map { it.messageId })

        val all = ConversationSearch.results(profile.chats.first { it.id == "fiatjaf" }, profile, "White Noise")
        assertEquals(all.sortedWith(compareByDescending<ConversationSearchResult> { it.dayOrdinal }.thenByDescending { it.minuteOfDay }), all)
    }

    @Test
    fun conversationSearchExcludesDeletedContentAndSenderMetadata() {
        val profile = ProfileFixtures.marmota
        val deleted = ChatTimelineEntry.Message(
            ChatMessage(
                id = "deleted-search-result",
                authorId = "maya-chen",
                dayOrdinal = 3,
                dayLabel = "Today",
                minuteOfDay = 720,
                timeLabel = "Now",
                text = "Private text that was deleted",
                deletionState = MessageDeletionState.DeletedByOther,
            ),
        )
        val chat = profile.chats.first { it.id == "fiatjaf" }.copy(timeline = listOf(deleted))

        assertTrue(ConversationSearch.results(chat, profile, "Private text").isEmpty())
        assertTrue(ConversationSearch.results(chat, profile, "Maya Chen").isEmpty())
    }
}
