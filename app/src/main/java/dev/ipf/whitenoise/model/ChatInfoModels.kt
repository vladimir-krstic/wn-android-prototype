package dev.ipf.whitenoise.model

import java.net.URI

enum class SharedContentCategory(val label: String) {
    Media("Photos & Videos"),
    Links("Links"),
    Documents("Documents"),
}

data class SharedContentItem(
    val id: String,
    val messageId: String,
    val attachment: MessageAttachment,
    val senderName: String,
    val sentLabel: String,
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
        get() = if (attachment.kind == MessageAttachmentKind.Video) "video/mp4" else "image/jpeg"

    val suggestedFileName: String
        get() {
            val extension = if (attachment.kind == MessageAttachmentKind.Video) "mp4" else "jpg"
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
    val initialIndex: Int
        get() = items.indexOfFirst { it.key == initialKey }.coerceAtLeast(0)
}

object ConversationMediaProjection {
    fun items(chat: Chat, profile: Profile): List<ConversationMediaItem> =
        chat.timeline.filterIsInstance<ChatTimelineEntry.Message>()
            .map(ChatTimelineEntry.Message::message)
            .filterNot(ChatMessage::isDeleted)
            .flatMap { message ->
                message.attachments.flatMap { attachment ->
                    when (attachment.kind) {
                        MessageAttachmentKind.Photo,
                        MessageAttachmentKind.Photos,
                        -> attachment.images.mapIndexed { imageIndex, image ->
                            item(chat, profile, message, attachment, image, imageIndex)
                        }
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
            profile.people.firstOrNull { it.id == message.authorId }?.name ?: chat.title
        },
        sentLabel = "${message.dayLabel}, ${message.timeLabel}",
    )
}

object SharedContentProjection {
    fun items(chat: Chat, profile: Profile, category: SharedContentCategory): List<SharedContentItem> {
        if (category == SharedContentCategory.Media) {
            return ConversationMediaProjection.items(chat, profile).map { media ->
                SharedContentItem(
                    id = media.key.stableId,
                    messageId = media.message.id,
                    attachment = media.attachment.copy(images = listOfNotNull(media.image)),
                    senderName = media.senderName,
                    sentLabel = media.sentLabel,
                )
            }
        }
        return chat.timeline.filterIsInstance<ChatTimelineEntry.Message>()
            .map(ChatTimelineEntry.Message::message)
            .filterNot(ChatMessage::isDeleted)
            .flatMap { message ->
                message.attachments.mapIndexedNotNull { index, attachment ->
                    val included = when (category) {
                        SharedContentCategory.Media -> error("Media uses ConversationMediaProjection")
                        SharedContentCategory.Links -> attachment.kind == MessageAttachmentKind.Link
                        SharedContentCategory.Documents -> attachment.kind == MessageAttachmentKind.File
                    }
                    if (!included) return@mapIndexedNotNull null
                    SharedContentItem(
                        id = "${message.id}-${attachment.id}-$index",
                        messageId = message.id,
                        attachment = attachment,
                        senderName = if (message.authorId == profile.id) {
                            "You"
                        } else {
                            profile.people.firstOrNull { it.id == message.authorId }?.name ?: chat.title
                        },
                        sentLabel = "${message.dayLabel}, ${message.timeLabel}",
                    )
                }
            }
    }

    fun counts(chat: Chat, profile: Profile): Map<SharedContentCategory, Int> =
        SharedContentCategory.entries.associateWith { items(chat, profile, it).size }
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
