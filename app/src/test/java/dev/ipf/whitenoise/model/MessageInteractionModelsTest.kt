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
            listOf(MessageAction.Reply, MessageAction.Forward, MessageAction.Share, MessageAction.SaveAttachments, MessageAction.Select, MessageAction.Info, MessageAction.Delete),
            MessageActionPolicy.available(incomingAttachment, ProfileFixtures.MARMOTA_ID),
        )
        assertEquals(MessageAction.RetrySend, MessageActionPolicy.available(failedText, ProfileFixtures.MARMOTA_ID).first())
        assertTrue(MessageAction.Copy in MessageActionPolicy.available(failedText, ProfileFixtures.MARMOTA_ID))
        assertEquals(listOf(MessageAction.Delete), MessageActionPolicy.available(failedText.copy(deletionState = MessageDeletionState.DeletedByCurrentProfile), ProfileFixtures.MARMOTA_ID))
    }

    @Test
    fun forwardingPreservesLargeSelectionsWhileDeletionKeepsAuthorshipWithoutGroupContext() {
        val mine = ChatMessage("mine", "me", 3, "Today", 600, "10:00 AM", "Text")
        val theirs = mine.copy(id = "theirs", authorId = "them")

        assertTrue(MessageActionPolicy.canDeleteForEveryone(listOf(mine), "me"))
        assertFalse(MessageActionPolicy.canDeleteForEveryone(listOf(mine, theirs), "me"))
        assertTrue(MessageActionPolicy.canForward(List(32) { mine.copy(id = "$it") }))
        assertTrue(MessageActionPolicy.canForward(List(33) { mine.copy(id = "$it") }))
        assertFalse(MessageActionPolicy.canForward(listOf(mine.copy(deletionState = MessageDeletionState.DeletedByOther))))
    }

    @Test
    fun quickReactionReplacementSwapsToRemainUnique() {
        val moved = ReactionCatalog.replaceQuick(ReactionCatalog.defaults, 0, "🔥")
        assertEquals("🔥", moved[0])
        assertEquals("❤️", moved[2])
        assertEquals(6, moved.distinct().size)
        assertEquals(ReactionCatalog.defaults, ReactionCatalog.replaceQuick(ReactionCatalog.defaults, 9, "😀"))
        assertEquals(7, ReactionCatalog.quickStrip(ReactionCatalog.defaults, "😀").size)
    }

    @Test
    fun reactionSummaryKeepsUpToFourTypesPlusOverflowAndAdaptsToWidth() {
        val reactions = listOf("❤️", "😀", "🔥", "🦫", "🚀").mapIndexed { index, emoji ->
            MessageReaction(
                emoji = emoji,
                personIds = if (index == 3) listOf("me", "other") else listOf("other"),
            )
        }

        val full = ReactionCatalog.summary(reactions, "me")
        val constrained = ReactionCatalog.summary(reactions, "me", maximumReactionPills = 2)

        assertEquals(5, full.size)
        assertEquals(listOf("❤️", "😀", "🔥", "🦫", null), full.map { it.emoji })
        assertEquals(1, full.last().omittedTypeCount)
        assertEquals(3, constrained.size)
        assertEquals(listOf("❤️", "😀", null), constrained.map { it.emoji })
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
    fun sentAndReceivedTextWithoutVoiceOfferReadAloudIncludingCaptionsAndReplies() {
        val plain = ChatMessage("text", "me", 3, "Today", 600, "10:00 AM", "Read this")
        val caption = plain.copy(
            attachments = listOf(MessageAttachment("photo", MessageAttachmentKind.Photo, "Photo")),
        )
        val reply = plain.copy(replyToMessageId = "original")
        for (author in listOf("me", "them")) {
            for (message in listOf(plain, caption, reply)) {
                val actions = MessageActionPolicy.available(message.copy(authorId = author), "me")
                assertTrue(MessageAction.ReadAloud in actions)
                assertEquals(actions.indexOf(MessageAction.Copy) + 1, actions.indexOf(MessageAction.ReadAloud))
                assertFalse(MessageAction.Transcribe in actions)
            }
        }
    }

    @Test
    fun speechReadinessGatesStartingButActiveReadingAlwaysOffersStop() {
        val message = ChatMessage("text", "me", 3, "Today", 600, "10:00 AM", "Read this")
        val unavailable = MessageActionPolicy.available(
            message, "me", MessageSpeechActionState(canReadAloud = false),
        )
        assertFalse(MessageAction.ReadAloud in unavailable)
        assertFalse(MessageAction.StopReading in unavailable)
        val reading = MessageActionPolicy.available(
            message, "me", MessageSpeechActionState(reading = true, canReadAloud = false),
        )
        assertTrue(MessageAction.StopReading in reading)
        assertFalse(MessageAction.ReadAloud in reading)
    }

    @Test
    fun emptyDeletedAndSentVoiceMessagesDoNotGainTextReadAloud() {
        val text = ChatMessage("text", "me", 3, "Today", 600, "10:00 AM", "Read this")
        val voice = text.copy(
            attachments = listOf(MessageAttachment("voice", MessageAttachmentKind.Voice, "Voice message")),
        )
        val excluded = listOf(
            text.copy(text = "  \n"),
            text.copy(deletionState = MessageDeletionState.DeletedByCurrentProfile),
            voice,
            voice.copy(text = ""),
            voice.copy(authorId = "them", text = ""),
        )
        excluded.forEach { message ->
            assertFalse(MessageAction.ReadAloud in MessageActionPolicy.available(message, "me"))
        }
        assertTrue(MessageAction.ReadAloud in MessageActionPolicy.available(voice.copy(authorId = "them"), "me"))
    }

    @Test
    fun conversationSearchMatchesTextSenderAndAttachmentNewestFirst() {
        val profile = ProfileFixtures.marmota
        val rich = profile.chats.first { it.id == "catalog-media-rich" }
        val text = ConversationSearch.results(rich, profile, "GIF")
        val contact = ConversationSearch.results(rich, profile, "Avery Stone")
        val file = ConversationSearch.results(rich, profile, "Project Brief")
        val link = ConversationSearch.results(rich, profile, "Human Interface Guidelines")

        assertEquals(listOf("RICH-01"), text.map { it.messageId })
        assertEquals(listOf("RICH-05"), contact.map { it.messageId })
        assertEquals(listOf("FILE-01"), file.map { it.messageId })
        assertEquals(listOf("LINK-01"), link.map { it.messageId })

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
