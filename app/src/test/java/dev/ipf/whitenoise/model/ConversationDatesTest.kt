package dev.ipf.whitenoise.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.*
import org.junit.Test

class ConversationDatesTest {
    private val today = GlobalSearchClock.today
    private fun day(offset: Long) = today.plusDays(offset)
    private fun message(id: String, offset: Long, minute: Int = 600) = ChatMessage(
        id, "friend", offset.toInt() + 3,
        day(offset).format(java.time.format.DateTimeFormatter.ofPattern("MMM d, uuuu", java.util.Locale.ENGLISH)),
        minute, "10:00", "Message $id",
    )
    private fun chat(vararg entries: ChatTimelineEntry) = Chat("dates", 0, ChatKind.Group, "Dates", timeline = entries.toList())
    private fun dates(vararg messages: ChatMessage) = ConversationDates(chat(*messages.map(ChatTimelineEntry::Message).toTypedArray()), ZoneOffset.UTC)

    @Test fun exactDayFindsFirstEntryAndEmptyDaysFindNextAvailableDay() {
        val dates = dates(message("newest", -1), message("later", -4, 650), message("first", -4, 600), message("oldest", -7))
        assertEquals("first", dates.target(day(-4))?.id)
        assertTrue(dates.selectable(day(-6)))
        assertEquals("first", dates.target(day(-6))?.id)
        assertEquals("newest", dates.target(today)?.id)
        assertFalse(dates.selectable(day(-8)))
        assertFalse(dates.selectable(day(1)))
        assertNull(dates.target(day(1)))
    }

    @Test fun deletedOnlyDaysAreSkippedAndNoticesNeverBecomeTargets() {
        val removed = message("deleted", -3).copy(deletionState = MessageDeletionState.DeletedByOther)
        val dates = ConversationDates(chat(
            ChatTimelineEntry.Message(message("earliest", -5)), ChatTimelineEntry.Message(removed),
            ChatTimelineEntry.Notice("notice", "Chat information", 1, "Yesterday"),
            ChatTimelineEntry.Message(message("next", -1)),
        ), ZoneOffset.UTC)
        assertEquals("next", dates.target(day(-3))?.id)
        assertEquals(listOf("earliest", "next"), dates.entries.map { it.id })
    }

    @Test fun eventCanBeFirstEntryOnDayAndLoadsThroughSameBoundedHistoryWindow() {
        val messages = (0 until 60).map { ChatTimelineEntry.Message(message("m$it", -1, 600 + it)) }
        val event = ChatTimelineEntry.Event("event", "Friend joined", 2, "Yesterday", 590)
        val chat = chat(*(messages + event).toTypedArray())
        assertEquals("event", ConversationDates(chat, ZoneOffset.UTC).target(day(-1))?.id)
        assertNull(ConversationHistory.target(chat, "event"))
        val window = ConversationHistory.target(chat, "event", includeEvents = true)!!
        assertEquals(ConversationHistory.pageSize, window.size)
        assertTrue("event" in window)
        assertTrue(ConversationHistory.hasNewer(chat, window))
    }

    @Test fun emptyDeletedAndFutureOnlyHistoryCannotJump() {
        val empty = dates()
        assertNull(empty.earliest)
        assertFalse(empty.selectable(today))
        assertNull(empty.target(today))
        val unavailable = dates(message("removed", -1).copy(deletionState = MessageDeletionState.DeletedByCurrentProfile), message("future", 1))
        assertNull(unavailable.earliest)
        assertNull(unavailable.target(today))
    }

    @Test fun midnightUsesCapturedLocalZoneAndReceiptTimeBeforeCreationTime() {
        val timestamp = Instant.parse("2026-08-02T00:30:00Z").toEpochMilli()
        val chat = chat(ChatTimelineEntry.Message(message("m", -1).copy(receivedAtMillis = timestamp, createdAtMillis = timestamp - 86_400_000)))
        val west = ConversationDates(chat, ZoneId.of("America/Los_Angeles"))
        val east = ConversationDates(chat, ZoneId.of("Asia/Tokyo"))
        assertEquals(LocalDate.of(2026, 8, 1), west.earliest)
        assertEquals(LocalDate.of(2026, 8, 2), east.earliest)
        assertEquals(timestamp, west.entries.single().timestamp)
        assertEquals("m", west.target(LocalDate.of(2026, 8, 1))?.id)
    }

    @Test fun daylightSavingBoundaryIncludesLocalMidnightAndExcludesPreviousCivilDay() {
        val zone = ZoneId.of("America/New_York")
        for (date in listOf(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 11, 1))) {
            val midnight = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val chat = chat(*listOf(-1L, 0L, 3_600_000L).mapIndexed { i, delta ->
                ChatTimelineEntry.Message(message("m$i", 0).copy(createdAtMillis = midnight + delta))
            }.toTypedArray())
            assertEquals("m1", ConversationDates(chat, zone, date.plusDays(1)).target(date)?.id)
        }
    }

    @Test fun duplicateTimestampsHaveDeterministicOrderAndMissingAnchorUsesToday() {
        val dates = dates(message("z", -1), message("a", -1))
        assertEquals("a", dates.target(day(-1))?.id)
        assertEquals(day(-1), dates.anchor("z"))
        assertEquals(today, dates.anchor("gone"))
    }

    @Test fun pickerUtcMidnightPreservesCivilDateAcrossZoneOffsets() {
        for (date in listOf(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 8), LocalDate.of(2026, 11, 1))) {
            assertEquals(date, LocalDate.ofEpochDay(GlobalSearchClock.pickerDay(GlobalSearchClock.pickerMillis(date.toEpochDay()))))
        }
    }
}
