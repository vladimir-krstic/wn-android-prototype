package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class MessageRetentionPolicyTest {
    private fun message(id: String = "message") = ChatMessage(id, "other", 3, "Today", 10, "Now", text = "A message", createdAtMillis = 100_000)
    @Test fun productionPresetsHaveExactValuesAndOrder() {
        assertEquals(listOf(0L, 7_776_000L, 2_419_200L, 604_800L, 86_400L, 28_800L, 3_600L, 300L, 30L), DisappearingDuration.entries.map { it.seconds })
    }
    @Test fun everyCustomUnitHasPositiveBoundedInput() {
        for (unit in RetentionUnit.entries) {
            assertEquals(unit.seconds, CustomRetentionInput("1", unit).duration!!.seconds)
            assertEquals(unit.maximum * unit.seconds, CustomRetentionInput(unit.maximum.toString(), unit).duration!!.seconds)
            assertNull(CustomRetentionInput((unit.maximum + 1).toString(), unit).duration)
            for (invalid in listOf("", "0", "-1", "+1", "1.5", "text", "9999999999999999999999999")) assertNull(CustomRetentionInput(invalid, unit).duration)
        }
    }
    @Test fun monthAndYearUseProductionFixedUnits() {
        assertEquals(30 * 86_400L, RetentionUnit.Months.seconds)
        assertEquals(365 * 86_400L, RetentionUnit.Years.seconds)
        assertNotEquals(CustomRetentionInput("12", RetentionUnit.Months).duration, CustomRetentionInput("1", RetentionUnit.Years).duration)
    }
    @Test fun customEditorRestoresExactUnitWithoutRounding() {
        val original = CustomRetentionInput("12", RetentionUnit.Months).duration!!
        assertEquals(CustomRetentionInput("12", RetentionUnit.Months), CustomRetentionInput.from(original))
        val odd = CustomRetentionInput.from(DisappearingDuration.fromSeconds(601)!!)
        assertEquals("601", odd.value); assertNull(odd.duration)
    }
    @Test fun onlyEnablingAndShorteningRequirePruneConfirmation() {
        assertTrue(MessageRetentionPolicy.requiresPruneConfirmation(DisappearingDuration.Off, DisappearingDuration.OneDay))
        assertTrue(MessageRetentionPolicy.requiresPruneConfirmation(DisappearingDuration.OneWeek, DisappearingDuration.OneDay))
        assertFalse(MessageRetentionPolicy.requiresPruneConfirmation(DisappearingDuration.OneDay, DisappearingDuration.Off))
        assertFalse(MessageRetentionPolicy.requiresPruneConfirmation(DisappearingDuration.OneDay, DisappearingDuration.OneWeek))
        assertFalse(MessageRetentionPolicy.requiresPruneConfirmation(DisappearingDuration.OneDay, DisappearingDuration.OneDay))
    }
    @Test fun receivedCountdownWaitsForFirstReadAndNeverExtendsOnReread() {
        val waiting = MessageRetentionPolicy.capture(message(), DisappearingDuration.ThirtySeconds, 100_000, received = true)
        assertNull(MessageRetentionPolicy.deadline(waiting)); assertFalse(MessageRetentionPolicy.expired(waiting, 900_000))
        val read = MessageRetentionPolicy.read(waiting, 200_000)
        assertEquals(230_000L, MessageRetentionPolicy.deadline(read)); assertEquals(read, MessageRetentionPolicy.read(read, 220_000))
        assertFalse(MessageRetentionPolicy.expired(read, 229_999)); assertTrue(MessageRetentionPolicy.expired(read, 230_000))
    }
    @Test fun outgoingNeverWaitsForReceivedReadWatermark() {
        val sent = MessageRetentionPolicy.capture(message(), DisappearingDuration.ThirtySeconds, 100_000, received = false)
        assertEquals(130_000L, MessageRetentionPolicy.deadline(sent)); assertTrue(MessageRetentionPolicy.expired(sent, 130_000))
    }
    @Test fun existingPinnedMessageDoesNotAdoptNewChatPolicy() {
        val sent = MessageRetentionPolicy.capture(message(), DisappearingDuration.ThirtySeconds, 100_000, received = false)
        assertEquals(sent, MessageRetentionPolicy.capture(sent, DisappearingDuration.OneWeek, 100_000, received = false))
        assertEquals(sent, MessageRetentionPolicy.capture(sent, DisappearingDuration.Off, 100_000, received = false))
        assertTrue(MessageRetentionPolicy.expired(sent, 130_000))
    }
    @Test fun prePolicyMessageHasNoInventedDeadline() {
        assertNull(MessageRetentionPolicy.deadline(message())); assertFalse(MessageRetentionPolicy.expired(message(), Long.MAX_VALUE))
        assertNull(MessageRetentionPolicy.presentation(message(), 200_000))
    }
    @Test fun explicitProjectedExpiryWinsOverSendArithmeticButUnreadStillDefers() {
        val message = message().copy(retention = MessageRetention(30, 100_000), expiresAtMillis = 170_000)
        assertEquals(170_000L, MessageRetentionPolicy.deadline(message))
        assertNull(MessageRetentionPolicy.deadline(message.copy(retention = message.retention!!.copy(waitingForRead = true))))
    }
    @Test fun overflowAndFutureReadAnchorsCannotExpireImmediately() {
        assertEquals(Long.MAX_VALUE, MessageRetentionPolicy.saturatingDeadline(Long.MAX_VALUE - 10, 30))
        assertNull(MessageRetentionPolicy.saturatingDeadline(-1, 30)); assertNull(MessageRetentionPolicy.saturatingDeadline(100, 0))
        val future = MessageRetentionPolicy.capture(message(), DisappearingDuration.ThirtySeconds, 500_000, true)
        assertEquals(530_000L, MessageRetentionPolicy.deadline(MessageRetentionPolicy.read(future, 100_000)))
    }
    @Test fun indicatorDistinguishesWaitingAndBoundedRemainingTime() {
        val waiting = MessageRetentionPolicy.capture(message(), DisappearingDuration.ThirtySeconds, 100_000, true)
        assertNull(MessageRetentionPolicy.presentation(waiting, 100_000)!!.fraction)
        val read = MessageRetentionPolicy.read(waiting, 100_000)
        assertEquals(0.5f, MessageRetentionPolicy.presentation(read, 115_000)!!.fraction)
        assertEquals(0f, MessageRetentionPolicy.presentation(read, 140_000)!!.fraction)
        assertEquals(1f, MessageRetentionPolicy.presentation(read, 1)!!.fraction)
        assertNull(MessageRetentionPolicy.presentation(read.copy(deletionState = MessageDeletionState.DeletedByOther), 100_000))
    }
    @Test fun pruningAndExpiryPreserveGroupSystemEventsAndUnrelatedDraftContent() {
        val profile = ProfileFixtures.openCircuit
        val current = Chat("chat", 0, ChatKind.Group, "Group", disappearingDuration = DisappearingDuration.Off, timeline = listOf(
            ChatTimelineEntry.Event("event", "Group updated", 1), ChatTimelineEntry.Message(message("old")),
            ChatTimelineEntry.Message(message("recent").copy(createdAtMillis = 200_000))), draftText = "Unsent", draftReplyMessageId = "old")
        val ids = MessageRetentionPolicy.pruneIds(current, DisappearingDuration.ThirtySeconds, 200_000)
        assertEquals(setOf("old"), ids)
        val cleaned = MessageRetentionPolicy.remove(current, ids, profile.id)
        assertEquals(listOf("event", "recent"), cleaned.timeline.map { it.id }); assertNull(cleaned.draftReplyMessageId); assertEquals("Unsent", cleaned.draftText)
    }
    @Test fun expiryRemovesUnreadBadgeWithoutRequiringAPreviouslyOpenedConversation() {
        val incoming = message("unread")
        val own = message("sent").copy(authorId = "me", minuteOfDay = 20)
        val chat = Chat("chat", 0, ChatKind.Direct("other"), "Chat", unreadCount = 1,
            timeline = listOf(ChatTimelineEntry.Message(incoming), ChatTimelineEntry.Message(own)))
        assertNull(chat.readState)
        val cleaned = MessageRetentionPolicy.remove(chat, setOf(incoming.id), "me")
        assertEquals(0, cleaned.unreadCount); assertTrue(cleaned.readState!!.unreadIds.isEmpty())
        assertEquals(listOf("sent"), cleaned.timeline.map { it.id })
    }

}
