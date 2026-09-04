package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class SpeechOwnershipTest {
    private val message = ChatMessage("m", "other", 1, "Today", 1, "Now", "Original text.")
    private val chat = Chat("c", 0, ChatKind.Direct("other"), "Chat", timeline = listOf(ChatTimelineEntry.Message(message)))
    private val profile = Profile("p", "Name", "public", chats = listOf(chat))
    private val target = SpeechReturnTarget(1, SpeechOwner(profile.id, chat.id), SpeechItem(message.id, message.text))
    private fun changed(message: ChatMessage) = profile.copy(chats = listOf(chat.copy(timeline = listOf(ChatTimelineEntry.Message(message)))))

    @Test fun matchingSourceRetainsReturnOwnershipAcrossUnrelatedProfileChanges() {
        assertTrue(SpeechOwnership.owns(profile, target))
        assertTrue(SpeechOwnership.owns(profile.copy(name = "Changed name"), target))
        assertTrue(SpeechOwnership.owns(profile.copy(chats = profile.chats + chat.copy(id = "elsewhere")), target))
    }
    @Test fun sameMessageIdentifiersNeverPermitAnotherProfileOrChat() {
        assertFalse(SpeechOwnership.owns(profile.copy(id = "other"), target))
        assertFalse(SpeechOwnership.owns(profile.copy(chats = listOf(chat.copy(id = "other"))), target))
        assertFalse(SpeechOwnership.owns(null, target))
    }
    @Test fun editedDeletedAndExpiredSourcesCannotBeReturnedToOrSpoken() {
        assertFalse(SpeechOwnership.owns(changed(message.copy(text = "Changed text.")), target))
        assertFalse(SpeechOwnership.owns(changed(message.copy(deletionState = MessageDeletionState.DeletedByCurrentProfile)), target))
        assertFalse(SpeechOwnership.owns(changed(message.copy(expiresAtMillis = MessageForwarding.nowMillis)), target))
        assertFalse(SpeechOwnership.owns(profile.copy(chats = emptyList()), target))
    }
    @Test fun replacingTextWithVoiceContentInvalidatesAnOldAuthoredQueue() {
        assertFalse(SpeechOwnership.owns(changed(message.copy(attachments = listOf(MessageAttachment("v", MessageAttachmentKind.Voice, "Voice")))), target))
    }
    @Test fun arrivalsDoNotInvalidateTheExistingSourceOrEnterItsSnapshotQueue() {
        val before = SpeechOwnership.items(chat)
        val after = profile.copy(chats = listOf(chat.copy(timeline = chat.timeline + ChatTimelineEntry.Message(message.copy(id = "new")))))
        assertTrue(SpeechOwnership.owns(after, target)); assertEquals(1, before.size)
        assertEquals(2, SpeechOwnership.items(after.chats.single()).size)
    }
}
