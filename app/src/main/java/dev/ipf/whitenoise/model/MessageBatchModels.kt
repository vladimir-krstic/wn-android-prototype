package dev.ipf.whitenoise.model

enum class MessageDeletePhase { Pending, Succeeded, Failed }
enum class MessageDeleteFailure { Temporary, PermissionDenied, Unavailable, SessionChanged }
enum class MessageDeleteScenario(val developerLabel: String) {
    Success("All deletions succeed"), Partial("Some deletions fail once"), Failure("All deletions fail once")
}
data class MessageDeleteItem(val messageId: String, val scope: MessageDeletionScope,
    val phase: MessageDeletePhase = MessageDeletePhase.Pending, val failure: MessageDeleteFailure? = null)
data class MessageDeleteOperation(val id: Long, val profileId: String, val chatId: String,
    val items: List<MessageDeleteItem>, val scenario: MessageDeleteScenario,
    val attempt: Int = 0, val revision: Int = 0) {
    val isRunning get() = items.any { it.phase == MessageDeletePhase.Pending }
    val succeeded get() = items.count { it.phase == MessageDeletePhase.Succeeded }
    val failed get() = items.filter { it.phase == MessageDeletePhase.Failed }
    val canRetry get() = !isRunning && failed.any { it.failure != MessageDeleteFailure.SessionChanged }
    fun report(): String = buildString {
        appendLine("White Noise message deletion report")
        appendLine("attempted=${items.size}"); appendLine("succeeded=$succeeded"); appendLine("failed=${failed.size}")
        MessageDeletionScope.entries.forEach { scope ->
            appendLine("${scope.name}.attempted=${items.count { it.scope == scope }}")
            appendLine("${scope.name}.failed=${failed.count { it.scope == scope }}")
        }
        MessageDeleteFailure.entries.forEach { failure ->
            val count = failed.count { it.failure == failure }
            if (count > 0) appendLine("${failure.name}=$count")
        }
    }.trimEnd()
}
object MessageDeletion {
    fun canDeleteForEveryone(message: ChatMessage, profile: Profile, chat: Chat): Boolean =
        !message.isDeleted && chat.composerAvailability(profile) == ComposerAvailability.Available &&
            (!chat.isGroup || chat.members.any { it.personId == profile.id }) &&
            (message.authorId == profile.id || (chat.isGroup && chat.members.any { it.personId == profile.id && it.role == GroupRole.Admin }))

    /** Freeze the explicit per-item scope; a later permission loss never changes it. */
    fun plan(profile: Profile, chat: Chat, ids: Set<String>, scope: MessageDeletionScope): List<MessageDeleteItem>? {
        val messages = ConversationProjection.orderedEntries(chat).filterIsInstance<ChatTimelineEntry.Message>()
            .map { it.message }.filter { it.id in ids }
        if (ids.isEmpty() || messages.size != ids.size) return null
        if (scope == MessageDeletionScope.ForEveryone && messages.none { canDeleteForEveryone(it, profile, chat) }) return null
        return messages.map { MessageDeleteItem(it.id, if (scope == MessageDeletionScope.ForEveryone && canDeleteForEveryone(it, profile, chat))
            MessageDeletionScope.ForEveryone else MessageDeletionScope.ForMe) }
    }

    fun remove(chat: Chat, item: MessageDeleteItem, profileId: String): Chat {
        val timeline = when (item.scope) {
            MessageDeletionScope.ForMe -> chat.timeline.filterNot { it is ChatTimelineEntry.Message && it.id == item.messageId }
            MessageDeletionScope.ForEveryone -> chat.timeline.map { entry ->
                if (entry is ChatTimelineEntry.Message && entry.id == item.messageId) entry.copy(message = entry.message.copy(
                    text = "", attachments = emptyList(), replyToMessageId = null, reactions = emptyList(),
                    deletionState = MessageDeletionState.DeletedByCurrentProfile, editHistory = null, editAttempt = null,
                )) else entry
            }
        }
        val latest = ConversationProjection.orderedEntries(chat.copy(timeline = timeline)).filterIsInstance<ChatTimelineEntry.Message>().lastOrNull()?.message
        val reply = chat.draftReplyMessageId?.takeUnless { it == item.messageId }
        return chat.copy(timeline = timeline, preview = latest?.visibleText(profileId).orEmpty(),
            previewAuthor = latest?.let { if (it.authorId == profileId) "You" else null },
            attachmentPreview = latest?.let { messageAttachmentPreview(it.attachments) },
            draftReplyMessageId = reply,
            isDraft = chat.draftText.isNotBlank() || chat.draftAttachments.isNotEmpty() || reply != null)
    }
}

enum class MessageForwardPhase { Preparing, Running, Completed, PartialFailure, Failed, Cancelled }
enum class MessageForwardTargetPhase { Waiting, Uploading, Sending, Completed, Failed, Cancelled }
enum class MessageForwardFailure {
    Preparation, PreparationTimeout, Upload, Send, PayloadTooLarge, Expired, SessionChanged, SourceUnavailable,
    Invitation, Left, Removed, Blocked, MissingRelays, TargetUnavailable;
    val retryable get() = this in setOf(Preparation, PreparationTimeout, Upload, Send, Invitation, Left, Removed, Blocked, MissingRelays, TargetUnavailable)
}
enum class MessageForwardScenario(val developerLabel: String) {
    Success("All forwards succeed"), PreparationFails("Preparation fails once"), PreparationTimeout("Preparation times out once"),
    PartialUpload("Second destination upload fails once"), PartialSend("Second destination send fails once"),
    PartialSendUntilRetried("Second destination needs manual retry"),
    Expired("Source expires"), SessionChanged("Session changes"), PayloadTooLarge("Media is too large")
}
data class MessageForwardTarget(val chatId: String, val title: String,
    val phase: MessageForwardTargetPhase = MessageForwardTargetPhase.Waiting,
    val uploaded: Int = 0, val sent: Int = 0, val failure: MessageForwardFailure? = null)
data class MessageForwardOperation(val id: Long, val sourceProfileId: String, val sourceChatId: String,
    val destinationProfileId: String, val sourceMessageIds: Set<String>, val messages: List<ChatMessage>,
    val targets: List<MessageForwardTarget>, val scenario: MessageForwardScenario,
    val phase: MessageForwardPhase = MessageForwardPhase.Preparing, val prepared: Int = 0,
    val attempt: Int = 0, val revision: Int = 0, val automaticRetries: Int = 0, val manualRetries: Int = 0) {
    val totalAttachments get() = messages.sumOf { it.attachments.size }
    val isRunning get() = phase in setOf(MessageForwardPhase.Preparing, MessageForwardPhase.Running)
    val succeeded get() = targets.count { it.phase == MessageForwardTargetPhase.Completed }
    val canCancel get() = isRunning && targets.none { it.phase == MessageForwardTargetPhase.Sending || it.sent > 0 }
    val canRetry get() = !isRunning && targets.any { it.phase == MessageForwardTargetPhase.Failed && it.failure?.retryable == true }
    val canAutomaticallyRetry get() = canRetry && automaticRetries < 3 && targets.none { it.failure == MessageForwardFailure.PreparationTimeout }
    val automaticRetryDelayMillis get() = 1_000L shl automaticRetries.coerceAtMost(2)
    val progress: Float get() {
        val total = totalAttachments + targets.size * (totalAttachments + messages.size)
        return if (total == 0) 0f else ((prepared + targets.sumOf { it.uploaded + it.sent }).toFloat() / total).coerceIn(0f, 1f)
    }
    fun settled(): MessageForwardOperation {
        if (targets.any { it.phase in setOf(MessageForwardTargetPhase.Waiting, MessageForwardTargetPhase.Uploading, MessageForwardTargetPhase.Sending) }) return this
        return copy(phase = when { succeeded == targets.size -> MessageForwardPhase.Completed; succeeded > 0 -> MessageForwardPhase.PartialFailure; else -> MessageForwardPhase.Failed })
    }
}
object MessageForwarding {
    val nowMillis: Long get() = GlobalSearchClock.today.atTime(12, 0).atZone(GlobalSearchClock.zone).toInstant().toEpochMilli()
    fun sourceFailure(messages: List<ChatMessage>, now: Long = nowMillis): MessageForwardFailure? = when {
        messages.isEmpty() || messages.any { it.isDeleted || (it.text.isBlank() && it.attachments.isEmpty()) } -> MessageForwardFailure.SourceUnavailable
        messages.any { it.expiresAtMillis?.let { deadline -> deadline <= now } == true } -> MessageForwardFailure.Expired
        else -> null
    }
    fun targetFailure(profile: Profile, chat: Chat?): MessageForwardFailure? = if (chat?.isGroup == true && chat.members.none { it.personId == profile.id }) MessageForwardFailure.Removed else when (chat?.composerAvailability(profile)) {
        ComposerAvailability.Available -> null
        ComposerAvailability.PendingInvitation -> MessageForwardFailure.Invitation
        ComposerAvailability.Left -> MessageForwardFailure.Left
        ComposerAvailability.Removed -> MessageForwardFailure.Removed
        ComposerAvailability.Blocked -> MessageForwardFailure.Blocked
        ComposerAvailability.MissingRelays -> MessageForwardFailure.MissingRelays
        null -> MessageForwardFailure.TargetUnavailable
    }
    fun folderMembers(profile: Profile, sourceProfileId: String, sourceChatId: String, folder: ChatFolder): List<String> =
        ChatFolders.rows(profile.chats, folder).filter { (profile.id != sourceProfileId || it.id != sourceChatId) && targetFailure(profile, it) == null }.map { it.id }
    fun toggleFolder(selected: Set<String>, members: List<String>): Set<String> =
        if (members.isNotEmpty() && members.all { it in selected }) selected - members.toSet() else selected + members
    fun payload(profile: Profile, chat: Chat, ids: Set<String>, mediaKey: ConversationMediaKey? = null, caption: String = ""): List<ChatMessage>? {
        val messages = ConversationProjection.orderedEntries(chat).filterIsInstance<ChatTimelineEntry.Message>().map { it.message }.filter { it.id in ids }
        if (messages.size != ids.size || sourceFailure(messages) != null) return null
        if (mediaKey == null) return messages
        if (ids != setOf(mediaKey.messageId)) return null
        val media = ConversationMediaProjection.items(chat, profile).firstOrNull { it.key == mediaKey } ?: return null
        val attachment = media.attachment.copy(kind = if (media.attachment.kind == MessageAttachmentKind.Video) MessageAttachmentKind.Video else MessageAttachmentKind.Photo,
            label = if (media.attachment.kind == MessageAttachmentKind.Photos) "Photo" else media.attachment.label, images = listOfNotNull(media.image))
        return listOf(media.message.copy(text = caption.trim(), attachments = listOf(attachment)))
    }
    fun copyForDestination(source: ChatMessage, operationId: Long, target: Chat, profileId: String, index: Int, day: Int, minute: Int): ChatMessage =
        source.copy(id = "${target.id}-forward-$operationId-$index", authorId = profileId, dayOrdinal = day,
            dayLabel = "Today", minuteOfDay = minute, timeLabel = "Now", replyToMessageId = null, reactions = emptyList(),
            deliveryState = MessageDeliveryState.Sent, deletionState = MessageDeletionState.None,
            editHistory = null, editAttempt = null, createdAtMillis = null, receivedAtMillis = null, expiresAtMillis = null,
            attachments = source.attachments.mapIndexed { attachmentIndex, it -> it.copy(id = "${target.id}-forward-$operationId-$index-media-$attachmentIndex", sourceImages = emptyList(), transfer = null) })
}

fun messageAttachmentPreview(attachments: List<MessageAttachment>): AttachmentPreview? {
        if (attachments.isEmpty()) return null
        val visualCount = attachments.count {
            it.kind == MessageAttachmentKind.Photo || it.kind == MessageAttachmentKind.Photos
        }
        if (visualCount > 1 && visualCount == attachments.size) {
            return AttachmentPreview.Photos(visualCount)
        }
        val first = attachments.first()
        return when (first.kind) {
            MessageAttachmentKind.Photo -> AttachmentPreview.Photo
            MessageAttachmentKind.Photos -> AttachmentPreview.Photos(first.images.size.coerceAtLeast(1))
            MessageAttachmentKind.Video -> AttachmentPreview.Video
            MessageAttachmentKind.Voice -> AttachmentPreview.VoiceMessage
            MessageAttachmentKind.File -> AttachmentPreview.File(first.label)
            MessageAttachmentKind.Contact -> AttachmentPreview.Contact(first.label.removePrefix("Contact: "))
            MessageAttachmentKind.Link -> AttachmentPreview.Link
            MessageAttachmentKind.Gif -> AttachmentPreview.Gif
        }
    }
