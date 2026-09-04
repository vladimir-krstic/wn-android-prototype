package dev.ipf.whitenoise.model

import java.net.URI

enum class SharedContentCategory(val label: String) {
    Media("Photos & Videos"),
    Links("Links"),
    Documents("Documents"),
    Voice("Voice"),
}

data class SharedContentItem(
    val id: String,
    val messageId: String,
    val attachment: MessageAttachment,
    val senderName: String,
    val sentLabel: String,
    val recordedAtMillis: Long = 0,
    val mediaKey: ConversationMediaKey? = null,
)

data class ConversationMediaKey(
    val messageId: String,
    val attachmentId: String,
    val imageIndex: Int,
) {
    val stableId: String
        get() = "${messageId.length}:$messageId|${attachmentId.length}:$attachmentId|$imageIndex"
}

data class ConversationMediaItem(
    val key: ConversationMediaKey,
    val message: ChatMessage,
    val attachment: MessageAttachment,
    val image: ProfileAvatar?,
    val senderName: String,
    val sentLabel: String,
) {
    val mimeType: String
        get() = when (attachment.kind) { MessageAttachmentKind.Video -> "video/mp4"; MessageAttachmentKind.Gif -> "image/gif"; else -> "image/jpeg" }

    val suggestedFileName: String
        get() {
            val extension = when (attachment.kind) { MessageAttachmentKind.Video -> "mp4"; MessageAttachmentKind.Gif -> "gif"; else -> "jpg" }
            val fallback = if (attachment.kind == MessageAttachmentKind.Video) "Video" else "Photo"
            val stem = attachment.label
                .substringBeforeLast('.', attachment.label)
                .trim()
                .replace(Regex("[^A-Za-z0-9._ -]+"), "")
                .replace(Regex("\\s+"), " ")
                .trim('.', ' ', '_', '-')
                .ifBlank { fallback }
                .take(80)
            return "$stem.$extension"
        }
}

data class ConversationMediaSelection(
    val items: List<ConversationMediaItem>,
    val initialKey: ConversationMediaKey,
) {
    fun containsSource(item: ConversationMediaItem): Boolean = items.any {
        it.key == item.key && it.attachment == item.attachment && it.image == item.image && it.attachment.bytesAvailable
    }

    val initialIndex: Int
        get() = items.indexOfFirst { it.key == initialKey }.coerceAtLeast(0)
}

object ConversationMediaProjection {
    fun items(chat: Chat, profile: Profile): List<ConversationMediaItem> =
        chat.timeline.filterIsInstance<ChatTimelineEntry.Message>()
            .map(ChatTimelineEntry.Message::message)
            .filter { !it.isDeleted && (it.expiresAtMillis?.let { expiry -> expiry > MessageForwarding.nowMillis } != false) }
            .flatMap { message ->
                message.attachments.flatMap { attachment ->
                    if (!attachment.bytesAvailable) return@flatMap emptyList()
                    when (attachment.kind) {
                        MessageAttachmentKind.Photo,
                        MessageAttachmentKind.Photos,
                        -> attachment.images.mapIndexed { imageIndex, image ->
                            item(chat, profile, message, attachment, image, imageIndex)
                        }
                        MessageAttachmentKind.Gif -> listOf(item(chat, profile, message, attachment, attachment.images.firstOrNull(), 0))
                        MessageAttachmentKind.Video -> {
                            if (attachment.externalUri == null && attachment.images.isEmpty()) {
                                emptyList()
                            } else {
                                listOf(
                                    item(
                                        chat = chat,
                                        profile = profile,
                                        message = message,
                                        attachment = attachment,
                                        image = attachment.images.firstOrNull(),
                                        imageIndex = 0,
                                    ),
                                )
                            }
                        }
                        else -> emptyList()
                    }
                }
            }

    fun selection(
        chat: Chat,
        profile: Profile,
        key: ConversationMediaKey,
    ): ConversationMediaSelection? {
        val items = items(chat, profile)
        return if (items.any { it.key == key }) ConversationMediaSelection(items, key) else null
    }

    private fun item(
        chat: Chat,
        profile: Profile,
        message: ChatMessage,
        attachment: MessageAttachment,
        image: ProfileAvatar?,
        imageIndex: Int,
    ) = ConversationMediaItem(
        key = ConversationMediaKey(message.id, attachment.id, imageIndex),
        message = message,
        attachment = attachment,
        image = image,
        senderName = if (message.authorId == profile.id) {
            "You"
        } else {
            profile.people.firstOrNull { it.id == message.authorId }?.displayName ?: chat.title
        },
        sentLabel = "${message.dayLabel}, ${message.timeLabel}",
    )
}

enum class SharedMediaFilter { All, Images, Videos }
data class SharedContentMonth(val key: java.time.YearMonth, val items: List<SharedContentItem>)

object SharedContentProjection {
    fun isAudio(attachment: MessageAttachment): Boolean = attachment.kind == MessageAttachmentKind.Voice ||
        (attachment.kind == MessageAttachmentKind.File && (TextAttachments.normalizedMime(attachment.mimeType).startsWith("audio/") ||
            TextAttachments.safeName(attachment.label).substringAfterLast('.', "").lowercase(java.util.Locale.ROOT) in setOf("mp3", "m4a", "wav", "ogg", "opus", "aac", "flac")))
    fun timestamp(message: ChatMessage) = message.createdAtMillis ?: GlobalSearchClock.timestamp(message)
    fun items(chat: Chat, profile: Profile, category: SharedContentCategory): List<SharedContentItem> =
        chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().map { it.message }
            .filter { !it.isDeleted && (it.expiresAtMillis?.let { expiry -> expiry > MessageForwarding.nowMillis } != false) }
            .sortedByDescending(::timestamp).flatMap { message ->
                val sender = if (message.authorId == profile.id) "You" else profile.people.firstOrNull { it.id == message.authorId }?.displayName ?: chat.title
                fun entry(attachment: MessageAttachment, index: Int, frame: Int? = null): SharedContentItem {
                    val key = frame?.let { ConversationMediaKey(message.id, attachment.id, it) }
                    return SharedContentItem(key?.stableId ?: "${message.id.length}:${message.id}|${attachment.id.length}:${attachment.id}|$index",
                        message.id, if (frame == null) attachment else attachment.copy(images = listOfNotNull(attachment.images.getOrNull(frame))),
                        sender, "${message.dayLabel}, ${message.timeLabel}", timestamp(message), key)
                }
                val attachments = message.attachments.flatMapIndexed { index, attachment ->
                    when (category) {
                        SharedContentCategory.Media -> if (attachment.kind in setOf(MessageAttachmentKind.Photo, MessageAttachmentKind.Photos, MessageAttachmentKind.Video, MessageAttachmentKind.Gif))
                            (0 until (if (attachment.kind in setOf(MessageAttachmentKind.Video, MessageAttachmentKind.Gif)) 1 else attachment.images.size.coerceAtLeast(1))).map { frame -> entry(attachment, index, frame) } else emptyList()
                        SharedContentCategory.Voice -> if (isAudio(attachment)) listOf(entry(attachment,index)) else emptyList()
                        SharedContentCategory.Documents -> if (attachment.kind == MessageAttachmentKind.File && !isAudio(attachment)) listOf(entry(attachment,index)) else emptyList()
                        SharedContentCategory.Links -> if (attachment.kind == MessageAttachmentKind.Link) listOf(entry(attachment,index)) else emptyList()
                    }
                }
                if (category != SharedContentCategory.Links) attachments else {
                    val existing = attachments.mapNotNull { it.attachment.externalUri }.toSet()
                    val urls = MessageDocuments.inline(message.text).mapNotNull { it.destination }
                        .filter { it.startsWith("https://",true) || it.startsWith("http://",true) }.distinct().filterNot { it in existing }
                    attachments + urls.mapIndexed { index, url -> entry(MessageAttachment("body-link-$index", MessageAttachmentKind.Link, url,
                        externalUri = url, linkTitle = url, linkDomain = runCatching { URI(url).host }.getOrNull()), message.attachments.size + index) }
                }
            }

    fun filtered(items: List<SharedContentItem>, filter: SharedMediaFilter): List<SharedContentItem> = items.filter {
        when (filter) { SharedMediaFilter.All -> true; SharedMediaFilter.Images -> it.attachment.kind != MessageAttachmentKind.Video; SharedMediaFilter.Videos -> it.attachment.kind == MessageAttachmentKind.Video }
    }
    fun months(items: List<SharedContentItem>, zone: java.time.ZoneId): List<SharedContentMonth> = items.groupBy {
        java.time.YearMonth.from(java.time.Instant.ofEpochMilli(it.recordedAtMillis).atZone(zone))
    }.map { (month, rows) -> SharedContentMonth(month,rows) }
    fun counts(chat: Chat, profile: Profile): Map<SharedContentCategory, Int> = SharedContentCategory.entries.associateWith { items(chat, profile, it).size }
}

object ChatRelayPolicy {
    fun normalize(value: String): String? {
        val trimmed = value.trim()
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        if (!uri.scheme.equals("wss", ignoreCase = true) || uri.host.isNullOrBlank()) return null
        if (uri.userInfo != null || uri.query != null || uri.fragment != null) return null
        val host = uri.host.lowercase()
        val port = if (uri.port == -1) "" else ":${uri.port}"
        val path = uri.path.orEmpty().trimEnd('/').takeUnless { it == "/" }.orEmpty()
        return "wss://$host$port$path"
    }

    fun add(current: List<String>, value: String): List<String>? {
        val normalized = normalize(value) ?: return null
        if (current.any { normalize(it) == normalized }) return null
        return current + normalized
    }
}

fun Profile.groupsInCommon(personId: String): List<Chat> = chats.filter { chat ->
    chat.isGroup && chat.membership == ChatMembership.Active &&
        chat.members.any { it.personId == id } && chat.members.any { it.personId == personId }
}
