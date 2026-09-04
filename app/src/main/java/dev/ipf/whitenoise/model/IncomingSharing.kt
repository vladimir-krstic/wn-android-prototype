package dev.ipf.whitenoise.model

import java.util.Locale

/** Resolved local descriptor. A future platform adapter owns URI permissions and provider I/O. */
data class IncomingStream(val identity: String, val attachment: MessageAttachment, val providerMime: String? = null, val readable: Boolean = true, val failure: IncomingContentFailure? = null)
data class IncomingPayload(val text: String = "", val streams: List<IncomingStream> = emptyList(), val intentMime: String? = null)
enum class IncomingContentFailure { Empty, Invalid, Unavailable, TooLarge }
data class PreparedIncoming(val text: String, val media: List<MessageAttachment>, val documents: List<MessageAttachment>)
data class IncomingPreparation(val content: PreparedIncoming? = null, val failure: IncomingContentFailure? = null)
data class IncomingDraftResult(val chat: Chat, val dropped: Int)

data class IncomingTarget(val profileId: String, val chatId: String)
sealed interface IncomingEntry {
    data class Share(val payload: IncomingPayload, val shortcut: IncomingTarget? = null) : IncomingEntry
    data class Conversation(val target: IncomingTarget) : IncomingEntry
    data class Notification(val target: NotificationTarget) : IncomingEntry
    data class ProfileLink(val value: String) : IncomingEntry
}

object IncomingSharing {
    const val shelfLimit = 10
    fun prepare(payload: IncomingPayload): IncomingPreparation {
        payload.streams.firstNotNullOfOrNull { it.failure }?.let { return IncomingPreparation(failure = it) }
        val text = payload.text.trim(); val streams = payload.streams.distinctBy { it.identity }
        if (streams.any { it.identity.isBlank() || it.identity.length > 4096 || it.attachment.label.isBlank() }) return IncomingPreparation(failure = IncomingContentFailure.Invalid)
        if (streams.any { !it.readable || !it.attachment.isAvailable }) return IncomingPreparation(failure = IncomingContentFailure.Unavailable)
        if (text.isEmpty() && streams.isEmpty()) return IncomingPreparation(failure = IncomingContentFailure.Empty)
        val media = mutableListOf<MessageAttachment>(); val documents = mutableListOf<MessageAttachment>()
        streams.forEach { stream ->
            val mime = (stream.providerMime?.takeIf { it.isNotBlank() } ?: payload.intentMime.orEmpty()).lowercase(Locale.ROOT)
            val attachment = stream.attachment.copy(id = "incoming-stream:${stream.identity}", mimeType = mime.takeIf { it.isNotBlank() } ?: stream.attachment.mimeType)
            if (mime.startsWith("image/") || mime.startsWith("video/")) media += attachment else documents += attachment
        }
        return IncomingPreparation(PreparedIncoming(text, media, documents))
    }
    fun canStage(profile: Profile, chat: Chat): Boolean = chat.composerAvailability(profile) == ComposerAvailability.Available
    private fun mediaCount(items: List<MessageAttachment>): Int = items.sumOf { if (it.kind in setOf(MessageAttachmentKind.Photo, MessageAttachmentKind.Photos, MessageAttachmentKind.Video, MessageAttachmentKind.Gif)) it.images.size.coerceAtLeast(1) else 0 }
    fun stage(chat: Chat, prepared: PreparedIncoming, requestId: Long): IncomingDraftResult {
        val mediaRoom = (shelfLimit - mediaCount(chat.draftAttachments)).coerceAtLeast(0)
        val documentRoom = (shelfLimit - chat.draftAttachments.count { it.kind !in setOf(MessageAttachmentKind.Photo, MessageAttachmentKind.Photos, MessageAttachmentKind.Video, MessageAttachmentKind.Gif) }).coerceAtLeast(0)
        val accepted = prepared.media.take(mediaRoom) + prepared.documents.take(documentRoom)
        val additions = accepted.map { it.copy(id = "$requestId:${it.id}") }
        val text = when { prepared.text.isBlank() -> chat.draftText; chat.draftText.isBlank() -> prepared.text; else -> "${chat.draftText.trimEnd()}\n${prepared.text}" }
        return IncomingDraftResult(chat.copy(draftText = text, draftAttachments = chat.draftAttachments + additions,
            isDraft = text.isNotBlank() || chat.draftAttachments.isNotEmpty() || additions.isNotEmpty() || chat.draftReplyMessageId != null),
            prepared.media.size + prepared.documents.size - accepted.size)
    }
}
