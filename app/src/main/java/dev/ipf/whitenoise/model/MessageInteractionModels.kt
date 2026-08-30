package dev.ipf.whitenoise.model

enum class MessageAction {
    RetrySend,
    Reply,
    Forward,
    Copy,
    Select,
    Info,
    Delete,
}

enum class MessageDeletionScope {
    ForMe,
    ForEveryone,
}

object MessageActionPolicy {
    fun available(message: ChatMessage, profileId: String): List<MessageAction> {
        if (message.isDeleted) return emptyList()
        return buildList {
            if (message.authorId == profileId && message.deliveryState == MessageDeliveryState.Failed) {
                add(MessageAction.RetrySend)
            }
            add(MessageAction.Reply)
            add(MessageAction.Forward)
            if (message.text.isNotBlank()) add(MessageAction.Copy)
            add(MessageAction.Select)
            add(MessageAction.Info)
            add(MessageAction.Delete)
        }
    }

    fun canDeleteForEveryone(messages: List<ChatMessage>, profileId: String): Boolean =
        messages.isNotEmpty() && messages.all { !it.isDeleted && it.authorId == profileId }

    fun canForward(messages: List<ChatMessage>): Boolean =
        messages.isNotEmpty() && messages.size <= 32 && messages.none(ChatMessage::isDeleted)
}

object ReactionCatalog {
    val defaults = listOf("❤", "🤘", "🔥", "😂", "🦫", "🚀")
    val categories: LinkedHashMap<String, List<String>> = linkedMapOf(
        "Recent" to defaults,
        "Smileys" to listOf("😀", "😃", "😄", "😁", "😆", "🥹", "😂", "🤣", "😊", "😎", "🤩", "🥳", "😴", "🤔", "🫡", "🤯"),
        "People" to listOf("👋", "🤚", "🖐", "✋", "🫶", "👏", "🙌", "🤝", "👍", "👎", "✊", "🤘", "👌", "🙏", "💪", "🧠"),
        "Animals" to listOf("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐸", "🦫", "🦉", "🦋", "🐝"),
        "Food" to listOf("🍏", "🍊", "🍋", "🍉", "🍇", "🍓", "🫐", "🥑", "🍕", "🌮", "🍜", "🍪", "🎂", "☕", "🍵", "🥤"),
        "Activity" to listOf("⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🎱", "🏓", "🥾", "🚲", "🏆", "🎨", "🎸", "🎮", "🧩", "🎯"),
        "Travel" to listOf("🚗", "🚌", "🚲", "✈", "🚀", "🛶", "⛰", "🏕", "🏖", "🌋", "🌅", "🌌", "🗺", "🧭", "⛺", "🌲"),
        "Objects" to listOf("⌚", "📱", "💻", "⌨", "📷", "💡", "🔦", "📚", "✏", "🔒", "🔑", "🧰", "🎁", "🔭", "🪴", "🕯"),
        "Symbols" to listOf("❤", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💯", "✨", "🔥", "✅", "❌", "⚠", "❓", "‼"),
    )

    val all: List<String> = categories.values.flatten().distinct()

    fun quickStrip(profileQuick: List<String>, selected: String?): List<String> =
        (profileQuick.take(6) + listOfNotNull(selected?.takeUnless(profileQuick::contains))).distinct()

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
        append(text)
        append(' ')
        append(if (authorId == profile.id) profile.name else profile.people.firstOrNull { it.id == authorId }?.name.orEmpty())
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
