package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class ForwardedMessageTest {
    private val profile = ProfileFixtures.marmota
    private val target = profile.chats.first { it.id == "maya-chen" }
    private fun forward(source: ChatMessage, destination: String = profile.id) =
        MessageForwarding.copyForDestination(source, 42, target, destination, 0, 3, 700, profile.id)

    @Test fun ownOriginalMessagesStayUnmarkedWhileReceivedAndReforwardedContentKeepAttribution() {
        val own = ChatMessage("own", profile.id, 3, "Today", 600, "10:00", "A plan")
        assertFalse(forward(own).isForwarded)
        assertFalse(forward(own, "another-profile").isForwarded)
        val received = own.copy(authorId = "maya-chen")
        val forwarded = forward(received)
        assertTrue(forwarded.isForwarded)
        assertEquals(profile.id, forwarded.authorId)
        assertTrue(forward(forwarded).isForwarded)
        assertFalse(received.isForwarded)
        assertEquals(received.text, forwarded.text)
    }

    @Test fun everyAttachmentKindAndMixedContentRetainPayloadWithFreshIdentity() {
        val examples = MessageAttachmentKind.entries.map { kind ->
            MessageAttachment("source-${kind.name}", kind, "Attachment ${kind.name}")
        }
        (examples.map(::listOf) + listOf(examples)).forEach { attachments ->
            val source = ChatMessage("original", "other", 1, "Yesterday", 600, "10:00", "Read [this](https://example.org)",
                attachments = attachments, replyToMessageId = "old-source")
            val copy = forward(source)
            assertTrue(copy.isForwarded)
            assertEquals(source.text, copy.text)
            assertEquals(attachments.map { it.kind }, copy.attachments.map { it.kind })
            assertEquals(attachments.map { it.label }, copy.attachments.map { it.label })
            assertNull(copy.replyToMessageId)
            assertNotEquals(source.id, copy.id)
            assertTrue(copy.attachments.zip(attachments).all { (next, previous) -> next.id != previous.id })
        }
    }

    @Test fun mayaContainsIncomingTextAndOutgoingPhotoExamplesOnly() {
        val examples = target.timeline.filterIsInstance<ChatTimelineEntry.Message>().map { it.message }.filter { it.isForwarded }
        assertEquals(listOf("maya-shared-text", "maya-shared-photo"), examples.map { it.id })
        assertEquals("maya-chen", examples.first().authorId)
        assertTrue(examples.first().attachments.isEmpty())
        assertEquals(profile.id, examples.last().authorId)
        assertEquals(MessageAttachmentKind.Photo, examples.last().attachments.single().kind)
        assertFalse(profile.chats.filterNot { it.id == target.id }.flatMap { it.timeline }
            .filterIsInstance<ChatTimelineEntry.Message>().any { it.message.isForwarded })
    }
}
