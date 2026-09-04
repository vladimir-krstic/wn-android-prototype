package dev.ipf.whitenoise.model

/** One projection for the visible preview and searchable text; membership wins over a stale draft. */
data class ChatListPresentation(
    val prefix: String?,
    val text: String,
    val attachment: AttachmentPreview?,
    val isDraft: Boolean,
    val status: ChatListStatus,
) {
    val showsFailure: Boolean get() = status == ChatListStatus.Failure
    val searchableText: String
        get() = listOfNotNull(prefix, text.takeIf(String::isNotEmpty)).joinToString(": ")

    companion object {
        fun from(chat: Chat): ChatListPresentation {
            if (chat.membership != ChatMembership.Active) {
                return ChatListPresentation(null, chat.visiblePreview, null, false, status(chat, visibleFailure = false))
            }
            if (chat.hasDraft) {
                val text = chat.draftText.ifBlank {
                    chat.draftAttachments.firstOrNull()?.label
                        ?: chat.preview.takeIf { chat.isDraft }.orEmpty()
                }
                return ChatListPresentation("Draft".takeIf { text.isNotEmpty() }, text, null, true, status(chat, visibleFailure = false))
            }
            return ChatListPresentation(
                chat.previewAuthor,
                chat.visiblePreview,
                chat.attachmentPreview,
                false,
                status(chat, visibleFailure = chat.deliveryState == ChatDeliveryState.Failed),
            )
        }

        private fun status(chat: Chat, visibleFailure: Boolean): ChatListStatus = when {
            chat.membership == ChatMembership.Invited -> ChatListStatus.Invitation
            visibleFailure -> ChatListStatus.Failure
            chat.unreadCount > 0 -> ChatListStatus.UnreadCount(chat.unreadCount)
            chat.isMarkedUnread -> ChatListStatus.ManuallyUnread
            else -> ChatListStatus.None
        }
    }
}

/** Display precedence only: hiding an unread marker never changes the underlying read state. */
sealed interface ChatListStatus {
    data object Invitation : ChatListStatus
    data object Failure : ChatListStatus
    data class UnreadCount(val count: Int) : ChatListStatus
    data object ManuallyUnread : ChatListStatus
    data object None : ChatListStatus
}

enum class ChatListAction {
    Read, Unread, Pin, Unpin, Mute, Unmute, Archive, Unarchive, Leave, Delete, Select, MoveUp, MoveDown, Folder;

    val isDestructive: Boolean get() = this == Leave || this == Delete
}

/** Matches the scoped iOS action availability, independently of gesture/accessibility presentation. */
object ChatListActionPolicy {
    fun leading(chat: Chat): List<ChatListAction> = buildList {
        if (chat.isUnread) add(ChatListAction.Read)
        else if (!chat.isArchived) add(ChatListAction.Unread)
        else return@buildList
        if (!chat.isArchived) add(if (chat.isPinned) ChatListAction.Unpin else ChatListAction.Pin)
    }

    /** Ordered from the row toward the trailing edge. */
    fun trailing(chat: Chat): List<ChatListAction> = buildList {
        if (chat.membership == ChatMembership.Active && !chat.isArchived) {
            add(if (chat.muteDuration == null) ChatListAction.Mute else ChatListAction.Unmute)
        }
        add(if (chat.isArchived) ChatListAction.Unarchive else ChatListAction.Archive)
        if (chat.hasEndedMembership) add(ChatListAction.Delete)
        else if (chat.isGroup && chat.membership == ChatMembership.Active) add(ChatListAction.Leave)
    }

    fun all(chat: Chat): List<ChatListAction> = leading(chat) + trailing(chat) +
        listOfNotNull(ChatListAction.Delete.takeUnless { chat.hasEndedMembership }, ChatListAction.Folder, ChatListAction.Select)
}

/** A conditional field-level inverse: never replace a whole chat or a newer read/archive change. */
data class ChatListUndo(
    val profileId: String,
    val chatId: String,
    val action: ChatListAction,
    val unreadCount: Int,
    val markedUnread: Boolean,
    val archived: Boolean,
    val pinned: Boolean,
    val readState: ConversationReadState? = null,
) {
    fun restore(chat: Chat): Chat = when (action) {
        ChatListAction.Read -> if (chat.unreadCount == 0 && !chat.isMarkedUnread) {
            chat.copy(unreadCount = unreadCount, isMarkedUnread = markedUnread, readState = readState)
        } else chat
        ChatListAction.Unread -> if (chat.unreadCount == 0 && chat.isMarkedUnread) {
            chat.copy(unreadCount = unreadCount, isMarkedUnread = markedUnread, readState = readState)
        } else chat
        ChatListAction.Archive, ChatListAction.Unarchive ->
            if (chat.isArchived == !archived && chat.isPinned == (pinned && action != ChatListAction.Archive)) {
                chat.copy(isArchived = archived, isPinned = pinned)
            } else chat
        else -> chat
    }

    companion object {
        fun capture(profileId: String, chat: Chat, action: ChatListAction) = ChatListUndo(
            profileId, chat.id, action, chat.unreadCount, chat.isMarkedUnread, chat.isArchived, chat.isPinned, chat.readState,
        )
    }
}
