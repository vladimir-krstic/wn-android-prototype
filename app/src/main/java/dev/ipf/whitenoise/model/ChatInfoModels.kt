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

object SharedContentProjection {
    fun items(chat: Chat, profile: Profile, category: SharedContentCategory): List<SharedContentItem> =
        chat.timeline.filterIsInstance<ChatTimelineEntry.Message>()
            .map(ChatTimelineEntry.Message::message)
            .filterNot(ChatMessage::isDeleted)
            .flatMap { message ->
                message.attachments.mapIndexedNotNull { index, attachment ->
                    val included = when (category) {
                        SharedContentCategory.Media -> attachment.kind in setOf(
                            MessageAttachmentKind.Photo,
                            MessageAttachmentKind.Photos,
                            MessageAttachmentKind.Video,
                        )
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
