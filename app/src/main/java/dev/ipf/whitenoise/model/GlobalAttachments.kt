package dev.ipf.whitenoise.model

enum class GlobalLibraryScenario(val developerLabel: String) {
    Ready("All attachments available"), Partial("Partial results once"), Failed("Results fail once")
}

data class GlobalAttachmentItem(
    val profileId: String, val chatId: String, val chatTitle: String,
    val message: ChatMessage, val attachment: MessageAttachment, val attachmentIndex: Int,
    val imageIndex: Int, val senderName: String, val timestamp: Long,
    val type: GlobalSearchContent,
) {
    val id: String get() = listOf(profileId, chatId, message.id, attachment.id).joinToString("|") { "${it.length}:$it" } + "|$attachmentIndex|$imageIndex"
    val media: ConversationMediaItem? get() {
        if (type != GlobalSearchContent.ImagesVideo || !attachment.bytesAvailable) return null
        val image = attachment.images.getOrNull(imageIndex)
        if (image == null && (attachment.kind in setOf(MessageAttachmentKind.Photo, MessageAttachmentKind.Photos) || attachment.externalUri == null)) return null
        // The existing viewer has conversation-local keys. Namespace them for this library.
        return ConversationMediaItem(ConversationMediaKey(id, attachment.id, imageIndex), message, attachment,
            image, "$senderName · $chatTitle", "${message.dayLabel}, ${message.timeLabel}")
    }
    val available get() = attachment.bytesAvailable && (type != GlobalSearchContent.ImagesVideo || media != null)
    fun shared() = SharedContentItem(id, message.id, attachment, senderName, "${message.dayLabel}, ${message.timeLabel}", timestamp)
}

object GlobalAttachments {
    val types = setOf(GlobalSearchContent.ImagesVideo, GlobalSearchContent.VoiceAudio, GlobalSearchContent.Files, GlobalSearchContent.AnyAttachment)
    fun browsing(filters: GlobalSearchFilters) = filters.content.isNotEmpty() && types.containsAll(filters.content)
    fun type(attachment: MessageAttachment): GlobalSearchContent? = when {
        attachment.kind in setOf(MessageAttachmentKind.Photo, MessageAttachmentKind.Photos, MessageAttachmentKind.Video, MessageAttachmentKind.Gif) -> GlobalSearchContent.ImagesVideo
        SharedContentProjection.isAudio(attachment) -> GlobalSearchContent.VoiceAudio
        attachment.kind == MessageAttachmentKind.File -> GlobalSearchContent.Files
        else -> null
    }
    /** Expand the existing search result projection, preserving distinct sources and gallery frames. */
    fun items(profile: Profile, results: GlobalSearchResults, filters: GlobalSearchFilters): List<GlobalAttachmentItem> {
        if (!browsing(filters) || !filters.valid) return emptyList()
        return results.messages.flatMap { result ->
            val chat = profile.chats.firstOrNull { it.id == result.chatId } ?: return@flatMap emptyList()
            val message = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().firstOrNull { it.id == result.message.id }?.message
                ?: return@flatMap emptyList()
            if (message.isDeleted || message.expiresAtMillis?.let { it <= MessageForwarding.nowMillis } == true) return@flatMap emptyList()
            message.attachments.flatMapIndexed { index, attachment ->
                val type = type(attachment) ?: return@flatMapIndexed emptyList()
                if (GlobalSearchContent.AnyAttachment !in filters.content && type !in filters.content) return@flatMapIndexed emptyList()
                val frames = if (attachment.kind in setOf(MessageAttachmentKind.Photo, MessageAttachmentKind.Photos)) attachment.images.size.coerceAtLeast(1) else 1
                (0 until frames).map { frame -> GlobalAttachmentItem(profile.id, chat.id, chat.title, message, attachment,
                    index, frame, GlobalSearch.senderName(profile, message.authorId), GlobalSearchClock.timestamp(message), type) }
            }
        }.sortedWith(compareByDescending<GlobalAttachmentItem> { it.timestamp }.thenBy { it.chatId }.thenBy { it.message.id }
            .thenBy { it.attachmentIndex }.thenBy { it.imageIndex }).distinctBy { it.id }
    }
    fun results(profile: Profile, query: String, filters: GlobalSearchFilters) = items(profile, GlobalSearch.results(profile, query, filters), filters)
}
