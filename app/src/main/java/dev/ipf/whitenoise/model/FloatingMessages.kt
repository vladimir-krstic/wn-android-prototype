package dev.ipf.whitenoise.model

/** References only: floating messages always resolve against the owning profile's live timeline. */
data class FloatingMessageKey(val profileId: String, val chatId: String, val messageId: String)

data class FloatingMessageStack(
    val keys: List<FloatingMessageKey> = emptyList(),
    val selected: FloatingMessageKey? = null,
) {
    fun add(key: FloatingMessageKey) = copy(keys = if (key in keys) keys else keys + key, selected = key)
    fun select(key: FloatingMessageKey) = if (key in keys) copy(selected = key) else this
    fun retain(valid: Set<FloatingMessageKey>): FloatingMessageStack {
        val remaining = keys.filter { it in valid }
        val index = keys.indexOf(selected).coerceAtLeast(0).coerceAtMost((remaining.size - 1).coerceAtLeast(0))
        return copy(keys = remaining, selected = selected?.takeIf { it in remaining } ?: remaining.getOrNull(index))
    }
    fun remove(key: FloatingMessageKey) = retain((keys - key).toSet())
}

data class FloatingMessageEntry(val key: FloatingMessageKey, val profile: Profile, val chat: Chat, val message: ChatMessage)

object FloatingMessages {
    fun resolve(key: FloatingMessageKey, profiles: List<Profile>, signedIn: Set<String>, nowMillis: Long): FloatingMessageEntry? {
        if (key.profileId !in signedIn) return null
        val profile = profiles.firstOrNull { it.id == key.profileId } ?: return null
        val chat = profile.chats.firstOrNull { it.id == key.chatId } ?: return null
        val message = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().firstOrNull { it.id == key.messageId }?.message ?: return null
        if (message.isDeleted || MessageRetentionPolicy.expired(message, nowMillis)) return null
        return FloatingMessageEntry(key, profile, chat, message)
    }
}

/** A copy at each end lets the native pager wrap without exposing an enormous virtual range. */
internal class FloatingMessagePages(private val count: Int) {
    init { require(count > 0) }
    val pageCount: Int = if (count == 1) 1 else count + 2
    fun pageFor(messageIndex: Int): Int {
        require(messageIndex in 0 until count)
        return if (count == 1) 0 else messageIndex + 1
    }
    fun messageAt(page: Int): Int {
        require(page in 0 until pageCount)
        return if (count == 1) 0 else (page + count - 1) % count
    }
    fun isBoundary(page: Int): Boolean = count > 1 && (page == 0 || page == pageCount - 1)
}
