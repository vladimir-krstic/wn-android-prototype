package dev.ipf.whitenoise.model

enum class MessageAction {
    RetrySend,
    Edit,
    EditHistory,
    RetryEdit,
    DiscardEdit,
    OpenMessage,
    SelectText,
    Reply,
    Forward,
    Copy,
    CopyMarkdown,
    ReadAloud,
    StopReading,
    Transcribe,
    ShowTranscript,
    HideTranscript,
    CopyTranscript,
    Select,
    Info,
    Delete,
}

data class MessageSpeechActionState(
    val transcriptAvailable: Boolean = false,
    val transcriptVisible: Boolean = false,
    val reading: Boolean = false,
    val canReadAloud: Boolean = true,
)

enum class MessageDeletionScope {
    ForMe,
    ForEveryone,
}

object MessageActionPolicy {
    fun available(
        message: ChatMessage,
        profileId: String,
        speech: MessageSpeechActionState = MessageSpeechActionState(
            transcriptAvailable = message.attachments
                .firstOrNull { it.kind == MessageAttachmentKind.Voice }
                ?.transcript != null,
        ),
        canWrite: Boolean = true,
    ): List<MessageAction> {
        if (message.isDeleted) return listOf(MessageAction.Delete)
        val hasVoice = message.attachments.any { it.kind == MessageAttachmentKind.Voice }
        val incoming = message.authorId != profileId
        return buildList {
            if (message.authorId == profileId && message.deliveryState == MessageDeliveryState.Failed) {
                add(MessageAction.RetrySend)
            }
            if (message.editAttempt?.phase == MessageEditPhase.Failed) {
                if (canWrite) add(MessageAction.RetryEdit)
                add(MessageAction.DiscardEdit)
            }
            if (canWrite && MessageEditing.eligible(message, profileId)) add(MessageAction.Edit)
            if (message.editHistory != null) add(MessageAction.EditHistory)
            if (message.text.isNotBlank()) {
                add(MessageAction.OpenMessage)
                add(MessageAction.SelectText)
            }
            add(MessageAction.Reply)
            add(MessageAction.Forward)
            if (message.text.isNotBlank()) {
                add(if (hasVoice) MessageAction.CopyTranscript else MessageAction.Copy)
                if (!hasVoice && MessageDocuments.plainText(message.text) != message.text) add(MessageAction.CopyMarkdown)
            }
            if (incoming && hasVoice && message.text.isBlank()) {
                if (speech.transcriptAvailable) {
                    add(
                        if (speech.transcriptVisible) {
                            MessageAction.HideTranscript
                        } else {
                            MessageAction.ShowTranscript
                        },
                    )
                    if (speech.transcriptVisible) add(MessageAction.CopyTranscript)
                } else {
                    add(MessageAction.Transcribe)
                }
            } else if (message.text.isNotBlank() && (incoming || !hasVoice) &&
                (speech.reading || speech.canReadAloud)
            ) {
                add(if (speech.reading) MessageAction.StopReading else MessageAction.ReadAloud)
            }
            add(MessageAction.Select)
            add(MessageAction.Info)
            add(MessageAction.Delete)
        }
    }

    fun canDeleteForEveryone(messages: List<ChatMessage>, profileId: String): Boolean =
        messages.isNotEmpty() && messages.all { !it.isDeleted && it.authorId == profileId }

    fun canForward(messages: List<ChatMessage>): Boolean =
        MessageForwarding.sourceFailure(messages) == null
}

object ReactionCatalog {
    data class SummaryItem(
        val emoji: String?,
        val personCount: Int,
        val selected: Boolean,
        val omittedTypeCount: Int = 0,
    )

    val defaults = listOf("❤️", "🤘", "🔥", "😂", "🦫", "🚀")
    val sections: List<EmojiSection> = EmojiCatalog.sections
    val categories: LinkedHashMap<String, List<String>> = LinkedHashMap<String, List<String>>().apply {
        sections.forEach { put(it.category.title, it.emoji) }
    }
    val all: List<String> = EmojiCatalog.all

    fun search(query: String): List<EmojiSection> = EmojiCatalog.search(query)

    fun quickStrip(profileQuick: List<String>, selected: String?): List<String> =
        (profileQuick.take(6) + listOfNotNull(selected?.takeUnless(profileQuick::contains))).distinct()

    fun summary(
        reactions: List<MessageReaction>,
        profileId: String,
        maximumReactionPills: Int = 4,
    ): List<SummaryItem> {
        require(maximumReactionPills >= 0)
        if (reactions.size <= maximumReactionPills) {
            return reactions.map { reaction ->
                SummaryItem(
                    emoji = reaction.emoji,
                    personCount = reaction.personIds.size,
                    selected = profileId in reaction.personIds,
                )
            }
        }
        val visible = reactions.take(maximumReactionPills).map { reaction ->
            SummaryItem(
                emoji = reaction.emoji,
                personCount = reaction.personIds.size,
                selected = profileId in reaction.personIds,
            )
        }
        val omitted = reactions.drop(maximumReactionPills)
        return visible + SummaryItem(
            emoji = null,
            personCount = omitted.sumOf { it.personIds.size },
            selected = omitted.any { profileId in it.personIds },
            omittedTypeCount = omitted.size,
        )
    }

    fun replaceQuick(current: List<String>, index: Int, emoji: String): List<String> {
        if (index !in 0..5 || current.size != 6) return current
        val result = current.toMutableList()
        val existing = result.indexOf(emoji)
        if (existing >= 0) {
            val old = result[index]
            result[index] = emoji
            result[existing] = old
        } else {
            result[index] = emoji
        }
        return result
    }
}

data class ConversationSearchResult(
    val messageId: String,
    val dayOrdinal: Int,
    val minuteOfDay: Int,
)

object ConversationSearch {
    fun results(chat: Chat, profile: Profile, query: String): List<ConversationSearchResult> {
        val normalized = query.normalizedSearchText()
        if (normalized.isEmpty()) return emptyList()
        return chat.timeline.filterIsInstance<ChatTimelineEntry.Message>()
            .map(ChatTimelineEntry.Message::message)
            .filterNot(ChatMessage::isDeleted)
            .filter { message -> message.searchableText(profile).contains(normalized) }
            .sortedWith(
                compareByDescending<ChatMessage> { it.dayOrdinal }
                    .thenByDescending { it.minuteOfDay },
            )
            .map { ConversationSearchResult(it.id, it.dayOrdinal, it.minuteOfDay) }
    }

    private fun ChatMessage.searchableText(profile: Profile): String = buildString {
        append(InlineMessageMarkup.plainText(text))
        append(' ')
        append(if (authorId == profile.id) profile.name else profile.people.firstOrNull { it.id == authorId }?.displayName.orEmpty())
        attachments.forEach { attachment ->
            append(' ')
            append(attachment.label)
            append(' ')
            append(attachment.linkTitle.orEmpty())
            append(' ')
            append(attachment.linkDomain.orEmpty())
        }
    }.normalizedSearchText()
}
