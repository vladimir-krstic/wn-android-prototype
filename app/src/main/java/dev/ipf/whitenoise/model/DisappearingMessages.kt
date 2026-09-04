package dev.ipf.whitenoise.model

/** Serializable only for Compose saved UI selection, not a durable message store. */
data class DisappearingDuration(val seconds: Long) : java.io.Serializable {
    init { require(seconds in 0..maxSeconds) }
    val label: String get() = when (seconds) {
        0L -> "Off"
        7_776_000L -> "90 days"
        else -> RetentionUnit.entries.reversed().firstOrNull { seconds % it.seconds == 0L }?.let { unit ->
            val count = seconds / unit.seconds
            "$count ${if (count == 1L) unit.singular else unit.plural}"
        } ?: "$seconds seconds"
    }
    val compactLabel: String get() = if (seconds == 0L) "Off" else if (seconds == 7_776_000L) "90d" else RetentionUnit.entries.reversed()
        .first { seconds % it.seconds == 0L }.let { "${seconds / it.seconds}${it.symbol}" }
    companion object {
        const val maxSeconds = 315_360_000L
        val Off = DisappearingDuration(0)
        val NinetyDays = DisappearingDuration(7_776_000)
        val FourWeeks = DisappearingDuration(2_419_200)
        val OneWeek = DisappearingDuration(604_800)
        val OneDay = DisappearingDuration(86_400)
        val EightHours = DisappearingDuration(28_800)
        val OneHour = DisappearingDuration(3_600)
        val FiveMinutes = DisappearingDuration(300)
        val ThirtySeconds = DisappearingDuration(30)
        val entries = listOf(Off, NinetyDays, FourWeeks, OneWeek, OneDay, EightHours, OneHour, FiveMinutes, ThirtySeconds)
        fun fromSeconds(seconds: Long): DisappearingDuration? = seconds.takeIf { it in 0..maxSeconds }?.let(::DisappearingDuration)
    }
}

enum class RetentionUnit(val seconds: Long, val maximum: Int, val singular: String, val plural: String, val symbol: String) {
    Seconds(1, 59, "second", "seconds", "s"), Minutes(60, 59, "minute", "minutes", "m"),
    Hours(3_600, 23, "hour", "hours", "h"), Days(86_400, 6, "day", "days", "d"),
    Weeks(604_800, 4, "week", "weeks", "w"), Months(2_592_000, 12, "month", "months", "mo"),
    Years(31_536_000, 10, "year", "years", "y")
}
data class CustomRetentionInput(val value: String, val unit: RetentionUnit) {
    val duration: DisappearingDuration? get() {
        if (!value.matches(Regex("[0-9]+"))) return null
        val count = value.toLongOrNull()?.takeIf { it in 1L..unit.maximum.toLong() } ?: return null
        return DisappearingDuration.fromSeconds(count * unit.seconds)
    }
    companion object {
        fun from(duration: DisappearingDuration): CustomRetentionInput {
            val seconds = duration.seconds.takeIf { it > 0 } ?: 60
            val unit = RetentionUnit.entries.reversed().firstOrNull { seconds % it.seconds == 0L && seconds / it.seconds in 1..it.maximum }
                ?: RetentionUnit.Seconds
            return CustomRetentionInput((seconds / unit.seconds).toString(), unit)
        }
    }
}

/** A row's immutable send-time policy and first local read. Never consult the current chat timer. */
data class MessageRetention(val durationSeconds: Long, val sentAtMillis: Long, val waitingForRead: Boolean = false, val readAtMillis: Long? = null)

data class MessageExpiryPresentation(val remainingMillis: Long?, val fraction: Float?, val deadlineMillis: Long?)

object MessageRetentionPolicy {
    fun requiresPruneConfirmation(before: DisappearingDuration, after: DisappearingDuration): Boolean =
        after.seconds > 0 && (before.seconds == 0L || after.seconds < before.seconds)
    fun saturatingDeadline(anchorMillis: Long, durationSeconds: Long): Long? {
        if (anchorMillis < 0 || durationSeconds <= 0) return null
        if (durationSeconds > (Long.MAX_VALUE - anchorMillis) / 1_000L) return Long.MAX_VALUE
        return anchorMillis + durationSeconds * 1_000L
    }
    fun deadline(message: ChatMessage): Long? {
        val retention = message.retention ?: return message.expiresAtMillis?.takeIf { it > 0 }
        if (retention.waitingForRead && retention.readAtMillis == null) return null
        retention.readAtMillis?.let { return saturatingDeadline(it, retention.durationSeconds) }
        return message.expiresAtMillis?.takeIf { it > 0 } ?: saturatingDeadline(retention.sentAtMillis, retention.durationSeconds)
    }
    fun capture(message: ChatMessage, duration: DisappearingDuration, nowMillis: Long, received: Boolean): ChatMessage {
        if (duration == DisappearingDuration.Off || message.retention != null) return message
        return message.copy(retention = MessageRetention(duration.seconds, nowMillis, waitingForRead = received),
            expiresAtMillis = if (received) null else saturatingDeadline(nowMillis, duration.seconds))
    }
    fun read(message: ChatMessage, nowMillis: Long): ChatMessage {
        val retention = message.retention?.takeIf { it.waitingForRead && it.readAtMillis == null } ?: return message
        val anchor = maxOf(nowMillis, retention.sentAtMillis)
        return message.copy(retention = retention.copy(readAtMillis = anchor), expiresAtMillis = saturatingDeadline(anchor, retention.durationSeconds))
    }
    fun expired(message: ChatMessage, nowMillis: Long): Boolean = deadline(message)?.let { it <= nowMillis } == true
    fun presentation(message: ChatMessage, nowMillis: Long): MessageExpiryPresentation? {
        if (message.isDeleted || message.retention == null) return null
        val expiry = deadline(message) ?: return MessageExpiryPresentation(null, null, null)
        val duration = message.retention.durationSeconds.takeIf { it > 0 } ?: return MessageExpiryPresentation(null, null, null)
        val remaining = (expiry - nowMillis.coerceAtLeast(0)).coerceAtLeast(0)
        val fraction = (remaining.toDouble() / (duration.toDouble() * 1_000)).coerceIn(0.0, 1.0).toFloat()
        return MessageExpiryPresentation(remaining, fraction, expiry)
    }
    fun pruneIds(chat: Chat, next: DisappearingDuration, nowMillis: Long): Set<String> {
        if (!requiresPruneConfirmation(chat.disappearingDuration, next)) return emptySet()
        val cutoff = nowMillis - next.seconds * 1_000L
        return chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().filter { ConversationTranscript.timestamp(it) < cutoff }.mapTo(linkedSetOf()) { it.id }
    }
    fun remove(chat: Chat, ids: Set<String>, profileId: String): Chat {
        if (ids.isEmpty()) return chat
        val entries = chat.timeline.filterNot { it is ChatTimelineEntry.Message && it.id in ids }
        val last = ConversationProjection.orderedEntries(chat.copy(timeline = entries)).filterIsInstance<ChatTimelineEntry.Message>().lastOrNull()?.message
        val reply = chat.draftReplyMessageId?.takeUnless { it in ids }
        val updated = chat.copy(timeline = entries, draftReplyMessageId = reply,
            isDraft = chat.draftText.isNotBlank() || chat.draftAttachments.isNotEmpty() || reply != null,
            preview = last?.visibleText(profileId).orEmpty(), previewAuthor = if (last?.authorId == profileId) "You" else null, attachmentPreview = last?.let { messageAttachmentPreview(it.attachments) },
            messageDeletion = chat.messageDeletion?.copy(items = chat.messageDeletion.items.filterNot { it.messageId in ids }))
        val read = ConversationReading.reconcile(chat.readState ?: ConversationReading.initial(chat, profileId), updated, profileId)
        return updated.copy(readState = read, unreadCount = read.unreadIds.size)
    }
}
