package dev.ipf.whitenoise.model

sealed interface ConversationItem {
    val id: String

    data class DayHeader(
        val label: String,
        override val id: String = "day-$label",
    ) : ConversationItem

    data class MessageItem(
        val message: ChatMessage,
        val startsCluster: Boolean,
        val endsCluster: Boolean,
        val resolvedReply: ChatMessage?,
        val hasUnavailableReply: Boolean,
    ) : ConversationItem {
        override val id: String get() = message.id
    }

    data class EventItem(val entry: ChatTimelineEntry.Event) : ConversationItem {
        override val id: String get() = entry.id
    }

    data class NoticeItem(val entry: ChatTimelineEntry.Notice) : ConversationItem {
        override val id: String get() = entry.id
    }
}

object ConversationProjection {
    fun items(chat: Chat): List<ConversationItem> {
        val ordered = chat.timeline.sortedWith(
            compareBy<ChatTimelineEntry> { it.dayOrdinal }
                .thenBy { it.minuteOfDay }
                .thenBy { it.id },
        )
        val messagesById = ordered.mapNotNull { (it as? ChatTimelineEntry.Message)?.message }
            .associateBy(ChatMessage::id)
        val result = mutableListOf<ConversationItem>()
        var currentDay: String? = null

        ordered.forEachIndexed { index, entry ->
            if (entry.dayLabel.isNotBlank() && entry.dayLabel != currentDay) {
                currentDay = entry.dayLabel
                result += ConversationItem.DayHeader(entry.dayLabel, "day-${entry.dayOrdinal}-${entry.dayLabel}")
            }
            when (entry) {
                is ChatTimelineEntry.Event -> result += ConversationItem.EventItem(entry)
                is ChatTimelineEntry.Notice -> result += ConversationItem.NoticeItem(entry)
                is ChatTimelineEntry.Message -> {
                    val previous = ordered.getOrNull(index - 1) as? ChatTimelineEntry.Message
                    val next = ordered.getOrNull(index + 1) as? ChatTimelineEntry.Message
                    val source = entry.message.replyToMessageId?.let(messagesById::get)
                    result += ConversationItem.MessageItem(
                        message = entry.message,
                        startsCluster = !clusters(previous?.message, entry.message),
                        endsCluster = !clusters(entry.message, next?.message),
                        resolvedReply = source?.takeUnless(ChatMessage::isDeleted),
                        hasUnavailableReply = entry.message.replyToMessageId != null &&
                            (source == null || source.isDeleted),
                    )
                }
            }
        }
        return result
    }

    private fun clusters(first: ChatMessage?, second: ChatMessage?): Boolean =
        first != null &&
            second != null &&
            first.authorId == second.authorId &&
            first.dayOrdinal == second.dayOrdinal &&
            second.minuteOfDay - first.minuteOfDay in 0..5
}

fun ChatMessage.visibleText(currentProfileId: String): String = when (deletionState) {
    MessageDeletionState.None -> text
    MessageDeletionState.DeletedByCurrentProfile -> "You deleted this message."
    MessageDeletionState.DeletedByOther -> if (authorId == currentProfileId) {
        "You deleted this message."
    } else {
        "This message was deleted."
    }
}

fun ChatMessage.plainVisibleText(currentProfileId: String): String =
    InlineMessageMarkup.plainText(visibleText(currentProfileId))

enum class ComposerAvailability {
    PendingInvitation,
    Available,
    Left,
    Removed,
    Blocked,
    MissingRelays,
}

fun Chat.composerAvailability(profile: Profile): ComposerAvailability = when (membership) {
    ChatMembership.Invited -> ComposerAvailability.PendingInvitation
    ChatMembership.Left -> ComposerAvailability.Left
    ChatMembership.Removed -> ComposerAvailability.Removed
    ChatMembership.Active -> {
        val directPerson = (kind as? ChatKind.Direct)?.personId
        when {
            directPerson != null && profile.people.firstOrNull { it.id == directPerson }?.isBlocked == true -> {
                ComposerAvailability.Blocked
            }
            relayUrls.isEmpty() -> ComposerAvailability.MissingRelays
            else -> ComposerAvailability.Available
        }
    }
}
