package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class MessagePinsTest {
    private val profile = ProfileFixtures.marmota
    private val original: Chat = profile.chats.first { it.id == "maya-chen" }
    private val messages: List<ChatMessage> = original.timeline.filterIsInstance<ChatTimelineEntry.Message>().map { it.message }.filterNot { it.isDeleted }
    private val first: String = messages.first().id
    private val second: String = messages.last().id
    private val chat = original.copy(pinnedMessageIds = emptyList())

    @Test fun orderIsStableDuplicatesDoNothingAndRepinningAppends() {
        val one = MessagePins.setPinned(chat, profile.id, first, true)
        val two = MessagePins.setPinned(one, profile.id, second, true)
        assertEquals(listOf(first, second), two.pinnedMessageIds)
        assertEquals(two, MessagePins.setPinned(two, profile.id, first, true))
        val removed = MessagePins.setPinned(two, profile.id, first, false)
        assertEquals(listOf(second, first), MessagePins.setPinned(removed, profile.id, first, true).pinnedMessageIds)
        assertEquals(chat.timeline, two.timeline)
    }

    @Test fun deletedAndMissingSourcesKeepStatusAndCanBeUnpinned() {
        val pins = chat.copy(pinnedMessageIds = listOf(first, second, "missing"), timeline = chat.timeline.map {
            if (it is ChatTimelineEntry.Message && it.id == first) it.copy(message = it.message.copy(deletionState = MessageDeletionState.DeletedByOther)) else it
        })
        assertEquals(listOf(PinnedMessageStatus.Deleted, PinnedMessageStatus.Available, PinnedMessageStatus.Unavailable), MessagePins.entries(pins).map { it.status })
        val removed = MessagePins.setPinned(pins, profile.id, "missing", false)
        assertEquals(listOf(first, second), removed.pinnedMessageIds)
        assertFalse(MessagePins.setPinned(chat, profile.id, "missing", true).pinnedMessageIds.contains("missing"))
    }

    @Test fun editsAreProjectedFromSourceAndNoOldPayloadIsCached() {
        val pinned = chat.copy(pinnedMessageIds = listOf(first))
        val changed = pinned.copy(timeline = pinned.timeline.map {
            if (it is ChatTimelineEntry.Message && it.id == first) it.copy(message = it.message.copy(text = "Changed")) else it
        })
        assertEquals("Changed", MessagePins.entries(changed).single().message!!.text)
        assertEquals(PinnedMessageStatus.Unavailable, MessagePins.entries(changed.copy(timeline = emptyList())).single().status)
    }

    @Test fun fixturePermissionsDistinguishMemberAdminAndEndedMembership() {
        val group = chat.copy(kind = ChatKind.Group, messagePinPermission = MessagePinPermission.Enabled,
            members = listOf(GroupMember(profile.id, GroupRole.Member)))
        assertEquals(group, MessagePins.setPinned(group, profile.id, first, true))
        val admin = group.copy(members = listOf(GroupMember(profile.id, GroupRole.Admin)))
        assertEquals(listOf(first), MessagePins.setPinned(admin, profile.id, first, true).pinnedMessageIds)
        for (membership in listOf(ChatMembership.Invited, ChatMembership.Left, ChatMembership.Removed)) {
            assertFalse(MessagePins.canManage(admin.copy(membership = membership), profile.id))
        }
        assertFalse(MessagePins.canManage(admin.copy(groupLifecycle = GroupLifecycle.Disbanded), profile.id))
        assertFalse(MessagePins.canManage(chat.copy(messagePinPermission = MessagePinPermission.Disabled), profile.id))
    }

    @Test fun ordinaryGroupsAlwaysRequireAdminAndDemotionAlsoBlocksUnpin() {
        val group = chat.copy(kind = ChatKind.Group, members = listOf(GroupMember(profile.id, GroupRole.Member)))
        assertFalse(MessagePins.canManage(group, profile.id))
        assertEquals(group, MessagePins.setPinned(group, profile.id, first, true))
        val admin = group.copy(members = listOf(GroupMember(profile.id, GroupRole.Admin)))
        val pinned = MessagePins.setPinned(admin, profile.id, first, true)
        val demoted = pinned.copy(members = group.members)
        assertEquals(demoted, MessagePins.setPinned(demoted, profile.id, first, false))
        assertEquals(listOf(first), MessagePins.entries(demoted).map { it.id })
        assertTrue(MessagePins.canManage(chat, profile.id))
    }

    @Test fun unsentDeletedAndSystemEntriesCannotBePinned() {
        for (state in listOf(MessageDeliveryState.Sending, MessageDeliveryState.Failed)) {
            val invalid = chat.copy(timeline = listOf(ChatTimelineEntry.Message(messages.first().copy(deliveryState = state))))
            assertEquals(invalid, MessagePins.setPinned(invalid, profile.id, first, true))
        }
        val deleted = chat.copy(timeline = listOf(ChatTimelineEntry.Message(messages.first().copy(deletionState = MessageDeletionState.DeletedByOther))))
        assertEquals(deleted, MessagePins.setPinned(deleted, profile.id, first, true))
    }

    @Test fun menuUsesExplicitPermissionAndSwitchesPinToUnpin() {
        val message = messages.first()
        assertTrue(MessageAction.Pin in MessageActionPolicy.available(message, profile.id, canPin = true))
        assertTrue(MessageAction.Unpin in MessageActionPolicy.available(message, profile.id, canPin = true, pinned = true))
        assertFalse(MessageAction.Pin in MessageActionPolicy.available(message, profile.id, canPin = false))
        assertFalse(MessageAction.Pin in MessageActionPolicy.available(message.copy(deliveryState = MessageDeliveryState.Failed), profile.id, canPin = true))
    }
}
