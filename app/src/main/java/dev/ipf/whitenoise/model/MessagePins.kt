package dev.ipf.whitenoise.model

/** Local fixture capability, not a protocol authority or a group setting. */
enum class MessagePinPermission { Enabled, Disabled }
enum class PinnedMessageStatus { Available, Deleted, Unavailable }
data class PinnedMessageEntry(val id: String, val message: ChatMessage?, val status: PinnedMessageStatus)

object MessagePins {
    fun canManage(chat: Chat, profileId: String): Boolean =
        chat.membership == ChatMembership.Active && chat.groupLifecycle == GroupLifecycle.Active &&
            chat.messagePinPermission != MessagePinPermission.Disabled &&
            (!chat.isGroup || chat.members.any { it.personId == profileId && it.role == GroupRole.Admin })

    fun entries(chat: Chat): List<PinnedMessageEntry> {
        val messages = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().associate { it.id to it.message }
        return chat.pinnedMessageIds.distinct().map { id ->
            val message = messages[id]
            PinnedMessageEntry(id, message, when {
                message == null -> PinnedMessageStatus.Unavailable
                message.isDeleted -> PinnedMessageStatus.Deleted
                else -> PinnedMessageStatus.Available
            })
        }
    }

    fun setPinned(chat: Chat, profileId: String, messageId: String, pinned: Boolean): Chat {
        if (!canManage(chat, profileId)) return chat
        val ids = chat.pinnedMessageIds
        if (!pinned) return if (messageId in ids) chat.copy(pinnedMessageIds = ids - messageId) else chat
        if (messageId in ids) return chat
        val message = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().firstOrNull { it.id == messageId }?.message ?: return chat
        if (message.isDeleted || message.deliveryState != MessageDeliveryState.Sent) return chat
        return chat.copy(pinnedMessageIds = ids + messageId)
    }
}
