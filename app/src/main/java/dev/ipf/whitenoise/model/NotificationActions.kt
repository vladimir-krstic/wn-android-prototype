package dev.ipf.whitenoise.model

enum class NotificationTargetKind { Message, Invite, ChatList }
data class NotificationTarget(val profileId: String, val chatId: String, val messageId: String? = null,
    val kind: NotificationTargetKind = NotificationTargetKind.Message) {
    val valid get() = profileId.isNotBlank() && chatId.isNotBlank()
    fun normalized() = copy(messageId = messageId?.takeIf { kind == NotificationTargetKind.Message && it.isNotBlank() })
}
enum class NotificationActionKind { Reply, React, MarkRead }
/** Local descriptor only; no Android notification is posted by this model. */
data class NotificationCard(val key: String, val generation: Long, val target: NotificationTarget)
data class NotificationActionInput(val requestKey: String, val card: NotificationCard, val kind: NotificationActionKind, val text: String = "")

object NotificationActions {
    fun normalize(input: NotificationActionInput, profile: Profile): NotificationActionInput? {
        val text = input.text.trim()
        if (input.requestKey.isBlank() || input.card.key.isBlank() || input.card.generation < 0 ||
            !input.card.target.valid || input.card.target.kind != NotificationTargetKind.Message || input.card.target.messageId.isNullOrBlank() || profile.id != input.card.target.profileId) return null
        if (input.kind == NotificationActionKind.Reply && text.isBlank()) return null
        if (input.kind == NotificationActionKind.React && (text.isBlank() || text.codePointCount(0,text.length) > 32 || text !in profile.quickReactions)) return null
        return input.copy(text = if (input.kind == NotificationActionKind.MarkRead) "" else text)
    }

    fun message(chat: Chat, target: NotificationTarget, now: Long) = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>()
        .firstOrNull { it.id == target.messageId && !it.message.isDeleted && it.message.expiresAtMillis?.let { deadline -> deadline <= now } != true }?.message

    fun readThrough(chat: Chat, target: NotificationTarget, now: Long): Chat? {
        if (message(chat,target,now) == null) return null
        val entries = ConversationProjection.orderedEntries(chat)
        val ids = entries.take(entries.indexOfFirst { it.id == target.messageId } + 1).filterIsInstance<ChatTimelineEntry.Message>()
            .filterNot { it.message.isDeleted }.mapTo(hashSetOf()) { it.id }
        val read = ConversationReading.seen(ConversationReading.reconcile(chat.readState ?: ConversationReading.initial(chat,target.profileId),chat,target.profileId),ids)
        return chat.copy(readState = read, unreadCount = read.unreadIds.size, isMarkedUnread = false,
            timeline = chat.timeline.map { if (it is ChatTimelineEntry.Message && it.id in ids) it.copy(message = MessageRetentionPolicy.read(it.message,now)) else it })
    }

    fun react(message: ChatMessage, owner: String, emoji: String): ChatMessage {
        val others = message.reactions.mapNotNull { r -> r.copy(personIds = r.personIds - owner).takeIf { it.personIds.isNotEmpty() } }
        val reactions = if (others.any { it.emoji == emoji }) others.map { if (it.emoji == emoji) it.copy(personIds = it.personIds + owner) else it }
            else others + MessageReaction(emoji,listOf(owner))
        return message.copy(reactions = reactions)
    }
}
