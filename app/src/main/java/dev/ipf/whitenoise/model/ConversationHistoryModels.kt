package dev.ipf.whitenoise.model

enum class HistoryOperation { Older, Newer, Target, Search }
enum class HistoryScenario(val developerLabel: String) {
    Success("History available"), OlderFails("Older page fails once"), NewerFails("Newer page fails once"),
    SearchFails("History search fails once"), TargetFails("Target loading fails once"), TargetUnavailable("Target unavailable once");
    fun appliesTo(operation: HistoryOperation) = when (this) {
        Success -> true
        OlderFails -> operation == HistoryOperation.Older
        NewerFails -> operation == HistoryOperation.Newer
        SearchFails -> operation == HistoryOperation.Search
        TargetFails, TargetUnavailable -> operation == HistoryOperation.Target
    }
}
enum class HistoryPhase { Loading, Failed, Unavailable }
data class HistoryRequest(val id: Long, val operation: HistoryOperation, val scenario: HistoryScenario,
    val targetId: String? = null, val phase: HistoryPhase = HistoryPhase.Loading,
    val scrollOffset: Int = 0, val highlight: Boolean = true)

/** Stable entry IDs describe the loaded UI window; the authoritative local history stays intact. */
object ConversationHistory {
    const val pageSize = 18
    fun initial(chat: Chat): Set<String> = ConversationProjection.orderedEntries(chat).takeLast(pageSize).mapTo(linkedSetOf()) { it.id }
    fun loaded(chat: Chat, ids: Set<String>) = ConversationProjection.orderedEntries(chat).filter { it.id in ids }
    fun hasOlder(chat: Chat, ids: Set<String>) = ConversationProjection.orderedEntries(chat).indexOfFirst { it.id in ids } > 0
    fun hasNewer(chat: Chat, ids: Set<String>): Boolean {
        val last = ConversationProjection.orderedEntries(chat).indexOfLast { it.id in ids }
        return last >= 0 && last < ConversationProjection.orderedEntries(chat).lastIndex
    }
    fun page(chat: Chat, ids: Set<String>, operation: HistoryOperation): Set<String> {
        val entries = ConversationProjection.orderedEntries(chat)
        if (entries.isEmpty()) return emptySet()
        val first = entries.indexOfFirst { it.id in ids }
        val last = entries.indexOfLast { it.id in ids }
        if (first < 0 || last < 0) return initial(chat)
        val page = when (operation) {
            HistoryOperation.Older -> entries.subList((first - pageSize).coerceAtLeast(0), first)
            HistoryOperation.Newer -> entries.subList(last + 1, (last + 1 + pageSize).coerceAtMost(entries.size))
            else -> emptyList()
        }
        return (ids + page.map { it.id }).intersect(entries.mapTo(hashSetOf()) { it.id })
    }
    fun target(chat: Chat, id: String): Set<String>? {
        val index = ConversationProjection.orderedEntries(chat).indexOfFirst { it is ChatTimelineEntry.Message && it.id == id && !it.message.isDeleted }
        if (index < 0) return null
        val start = (index - pageSize / 2).coerceIn(0, (ConversationProjection.orderedEntries(chat).size - pageSize).coerceAtLeast(0))
        return ConversationProjection.orderedEntries(chat).subList(start, (start + pageSize).coerceAtMost(ConversationProjection.orderedEntries(chat).size)).mapTo(linkedSetOf()) { it.id }
    }
    fun searchCursor(results: List<ConversationSearchResult>, pinnedId: String?): Int =
        if (results.isEmpty()) -1 else results.indexOfFirst { it.messageId == pinnedId }.coerceAtLeast(0)
}

data class ConversationReadState(val unreadIds: Set<String>, val observedIds: Set<String>)

object ConversationReading {
    fun actuallyVisible(itemStart: Int, itemSize: Int, viewportStart: Int, viewportEnd: Int): Boolean {
        if (itemSize <= 0 || viewportEnd <= viewportStart) return false
        val overlap = minOf(itemStart + itemSize, viewportEnd) - maxOf(itemStart, viewportStart)
        return overlap >= minOf(itemSize, viewportEnd - viewportStart) - 1
    }
    private fun received(chat: Chat, profileId: String) = ConversationProjection.orderedEntries(chat).filterIsInstance<ChatTimelineEntry.Message>()
        .map { it.message }.filter { it.authorId != profileId && !it.isDeleted }
    fun initial(chat: Chat, profileId: String) = ConversationReadState(
        received(chat, profileId).takeLast(chat.unreadCount.coerceAtLeast(0)).mapTo(linkedSetOf()) { it.id },
        ConversationProjection.orderedEntries(chat).mapTo(hashSetOf()) { it.id },
    )
    fun reconcile(state: ConversationReadState, chat: Chat, profileId: String): ConversationReadState {
        val incoming = received(chat, profileId).map { it.id }
        return ConversationReadState(state.unreadIds.intersect(incoming.toSet()) + incoming.filter { it !in state.observedIds },
            ConversationProjection.orderedEntries(chat).mapTo(hashSetOf()) { it.id })
    }
    fun seen(state: ConversationReadState, ids: Set<String>) = state.copy(unreadIds = state.unreadIds - ids)
    fun through(state: ConversationReadState, chat: Chat, target: String): ConversationReadState {
        val index = ConversationProjection.orderedEntries(chat).indexOfFirst { it is ChatTimelineEntry.Message && !it.message.isDeleted && it.id == target }
        return if (index < 0) state else seen(state, ConversationProjection.orderedEntries(chat).take(index + 1).mapTo(hashSetOf()) { it.id })
    }
    fun firstUnread(state: ConversationReadState, chat: Chat) = ConversationProjection.orderedEntries(chat).firstOrNull { it.id in state.unreadIds }?.id
    fun mentions(state: ConversationReadState, chat: Chat, profile: Profile): List<String> {
        val names = listOf(profile.name, profile.publicKey).filter(String::isNotBlank)
        return received(chat, profile.id).filter { message -> message.id in state.unreadIds && names.any { name ->
            Regex("(?i)(?<![\\p{L}\\p{N}_])@${Regex.escape(name)}(?![\\p{L}\\p{N}_])").containsMatchIn(message.text)
        } }.map { it.id }
    }

}

/** Tail distance uses stable row keys, not an average that changes with each visible gallery. */
class ConversationTailJump {
    data class Row(val id: String, val offsetPx: Int, val sizePx: Int)
    private val measuredHeights = mutableMapOf<String, Int>()
    private var measuredWidth = 0
    private var visible = false

    fun update(
        keys: List<String>, rows: List<Row>, viewportPx: Int, viewportEndPx: Int,
        afterPaddingPx: Int, spacingPx: Int, widthPx: Int, hasNewer: Boolean = false,
    ): Boolean {
        // Keep the previous state through transient empty/re-measuring layouts.
        if (viewportPx <= 0 || widthPx <= 0 || rows.isEmpty()) return visible
        if (measuredWidth != widthPx) {
            measuredHeights.clear()
            measuredWidth = widthPx
        }
        measuredHeights.keys.retainAll(keys.toSet())
        rows.filter { it.sizePx > 0 }.forEach { measuredHeights[it.id] = it.sizePx }
        val last = rows.last()
        val lastIndex = keys.indexOf(last.id)
        if (lastIndex < 0) return visible
        var distance = last.offsetPx.toLong() + last.sizePx + afterPaddingPx - viewportEndPx
        for (index in lastIndex + 1 until keys.size) {
            // A distant target/restored window may contain unmeasured newer rows.
            // Use a fixed viewport fallback, never a changing visible-row average.
            distance += (measuredHeights[keys[index]] ?: viewportPx).toLong() + spacingPx.coerceAtLeast(0)
        }
        visible = hasNewer || distance >= viewportPx
        return visible
    }
}

/** A visit-owned reading boundary. Acknowledging visible messages never moves it. */
data class ConversationUnreadDivider(val firstId: String, val messageIds: Set<String>) {
    fun reconcile(chat: Chat, profileId: String): ConversationUnreadDivider? {
        val messages = ConversationProjection.orderedEntries(chat).filterIsInstance<ChatTimelineEntry.Message>()
            .map { it.message }.filterNot { it.isDeleted }
        val anchor = messages.indexOfFirst { it.id == firstId || it.id in messageIds }
        if (anchor < 0) return null
        val following = messages.drop(anchor)
        // Replying ends the unread section for this visit, as in Signal.
        if (following.any { it.authorId == profileId }) return null
        val unread = ConversationReading.reconcile(chat.readState ?: ConversationReading.initial(chat, profileId), chat, profileId).unreadIds
        val ids = following.filter { it.id in messageIds || it.id in unread }.mapTo(linkedSetOf()) { it.id }
        return ids.firstOrNull()?.let { ConversationUnreadDivider(it, ids) }
    }

    companion object {
        fun capture(chat: Chat, profileId: String): ConversationUnreadDivider? {
            val read = ConversationReading.reconcile(chat.readState ?: ConversationReading.initial(chat, profileId), chat, profileId)
            val first = ConversationReading.firstUnread(read, chat) ?: return null
            return ConversationUnreadDivider(first, read.unreadIds).reconcile(chat, profileId)
        }
    }
}
