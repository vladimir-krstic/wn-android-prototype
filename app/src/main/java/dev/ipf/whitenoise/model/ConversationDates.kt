package dev.ipf.whitenoise.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ConversationDateEntry(val id: String, val date: LocalDate, val timestamp: Long)

/** A small immutable index over authoritative local entries, not a second history cache. */
class ConversationDates(chat: Chat, val zone: ZoneId, val today: LocalDate = GlobalSearchClock.today) {
    val entries: List<ConversationDateEntry> = chat.timeline.mapNotNull { entry ->
        val message = (entry as? ChatTimelineEntry.Message)?.message
        if (message?.isDeleted == true || entry is ChatTimelineEntry.Notice) return@mapNotNull null
        val explicit = message?.receivedAtMillis ?: message?.createdAtMillis
        val date = explicit?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
            ?: GlobalSearchClock.date(message ?: ChatMessage(entry.id, "", entry.dayOrdinal, entry.dayLabel, entry.minuteOfDay, ""))
        val timestamp = explicit ?: date.atStartOfDay(zone).plusMinutes(entry.minuteOfDay.toLong()).toInstant().toEpochMilli()
        ConversationDateEntry(entry.id, date, timestamp).takeIf { date <= today }
    }.sortedWith(compareBy<ConversationDateEntry> { it.timestamp }.thenBy { it.id })
    val earliest: LocalDate? = entries.minOfOrNull { it.date }
    fun selectable(date: LocalDate): Boolean = earliest?.let { date >= it && date <= today } == true
    fun target(date: LocalDate): ConversationDateEntry? {
        if (!selectable(date)) return null
        val midnight = date.atStartOfDay(zone).toInstant().toEpochMilli()
        return entries.firstOrNull { it.timestamp >= midnight } ?: entries.lastOrNull()
    }
    fun anchor(id: String?): LocalDate = entries.firstOrNull { it.id == id }?.date
        ?: today
}
