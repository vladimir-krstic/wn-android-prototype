package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class SpeechAutoReadTest {
    private fun message(id: String, minute: Int, body: String = "Text.") = ChatMessage(id, "other", 1, "Today", minute, "Now", body)
    private fun chat(vararg messages: ChatMessage) = Chat("c", 0, ChatKind.Direct("other"), "Chat", timeline = messages.map { ChatTimelineEntry.Message(it) })
    private fun command(action: SpeechControlAction) = SpeechControlCommand("p", 1, action)

    @Test fun backlogStartsAtCapturedUnreadBoundaryAndIncludesLaterAuthoredCaptions() {
        val chat = chat(message("old", 0), message("unread", 1), message("reply", 2).copy(authorId = "p"), message("last", 3))
        assertEquals(listOf("unread", "reply", "last"), SpeechAutoRead.backlog(chat, setOf("unread")).map { it.id })
        assertEquals(listOf("reply", "last"), SpeechAutoRead.backlog(chat, setOf("unread"), setOf("unread")).map { it.id })
        assertTrue(SpeechAutoRead.backlog(chat, setOf("removed")).isEmpty())
    }
    @Test fun backlogBoundsProjectionAndFiltersVoiceDeletedAndExpiredContent() {
        val messages = (0..150).map { message("m$it", it) }.toMutableList()
        messages[1] = messages[1].copy(attachments = listOf(MessageAttachment("v", MessageAttachmentKind.Voice, "Voice")))
        messages[2] = messages[2].copy(deletionState = MessageDeletionState.DeletedByOther)
        messages[3] = messages[3].copy(expiresAtMillis = MessageForwarding.nowMillis)
        val result = SpeechAutoRead.backlog(chat(*messages.toTypedArray()), setOf("m0"))
        assertEquals(50, result.size); assertFalse(result.any { it.id in setOf("m1", "m2", "m3") })
    }
    @Test fun exactCursorRejectsHistoricalPagesRepeatedSnapshotsAndEdits() {
        val original = chat(message("a", 3), message("b", 4)); val cursor = SpeechArrivalCursor.capture(original)
        val updated = chat(message("historical", 1), message("a", 3, "Edited."), message("b", 4), message("new", 5))
        assertEquals(listOf("new"), SpeechAutoRead.arrivals(updated, cursor).map { it.id })
        val advanced = cursor.advance(updated)
        assertTrue(SpeechAutoRead.arrivals(updated, advanced).isEmpty())
    }
    @Test fun trimmedAnchorUsesOrderAndCursorNeverMovesBackIntoHistory() {
        val cursor = SpeechArrivalCursor.capture(chat(message("a", 3), message("b", 4)))
        val trimmed = chat(message("old", 2), message("new", 5))
        assertEquals(listOf("new"), SpeechAutoRead.arrivals(trimmed, cursor).map { it.id })
        val older = cursor.advance(chat(message("very-old", 1)))
        assertEquals(cursor.anchorId, older.anchorId); assertEquals(cursor.order, older.order)
        assertTrue(SpeechAutoRead.arrivals(chat(message("a", 3)), older).isEmpty())
    }
    @Test fun removedAndReinsertedIdentityNeverSpeaksTwice() {
        val cursor = SpeechArrivalCursor.capture(chat(message("a", 3)))
        val updated = chat(message("a", 4), message("new", 5), message("new", 5))
        assertEquals(listOf("new"), SpeechAutoRead.arrivals(updated, cursor).map { it.id })
        assertTrue(SpeechAutoRead.arrivals(updated, cursor, setOf("new")).isEmpty())
    }
    @Test fun onlyForegroundUnlockedSameOwnerAndUnchangedManualGenerationMayResume() {
        val owner = SpeechOwner("p", "c")
        fun allowed(profile: String? = "p", foreground: Boolean = true, locked: Boolean = false, generation: Long = 4) =
            SpeechAutoRead.mayResume(owner, profile, foreground, locked, 4, generation, null)
        assertTrue(allowed()); assertFalse(allowed("other")); assertFalse(allowed(null)); assertFalse(allowed(foreground = false)); assertFalse(allowed(locked = true)); assertFalse(allowed(generation = 5))
        val active = SpeechSession.create(1, owner, listOf(SpeechItem("m", "Text.")), "m")!!
        assertFalse(SpeechAutoRead.mayResume(owner, "p", true, false, 4, 4, active))
        assertFalse(SpeechAutoRead.mayResume(owner, "p", true, false, 4, 4, active.pause()))
    }
    @Test fun notificationStartFailureEndsTheExampleAndLateControlsCannotResumeIt() {
        val failed = SpeechBackgroundExample("p", 1).notificationStarted(false)
        assertEquals(SpeechLifecyclePhase.Failed, failed.phase); assertFalse(failed.notificationVisible)
        assertEquals(failed, failed.command(command(SpeechControlAction.Resume))); assertEquals(failed, failed.notificationStarted(true))
    }
    @Test fun notificationControlsAreSessionAndProfileOwned() {
        val state = SpeechBackgroundExample("p", 1).notificationStarted(true)
        assertEquals(state, state.command(SpeechControlCommand("other", 1, SpeechControlAction.Stop)))
        assertEquals(state, state.command(SpeechControlCommand("p", 2, SpeechControlAction.Stop)))
        assertTrue(state.command(command(SpeechControlAction.Pause)).paused)
        assertFalse(state.command(command(SpeechControlAction.Pause)).command(command(SpeechControlAction.Resume)).paused)
        assertTrue(state.command(command(SpeechControlAction.Source)).sourceRequested)
        assertEquals(SpeechLifecyclePhase.Ended, state.command(command(SpeechControlAction.Stop)).phase)
    }
    @Test fun lockExpiryStopsControlsAndReturningBeforeExpiryCancelsTheDeadline() {
        val background = SpeechBackgroundExample("p", 1).notificationStarted(true).background(1_000, 60_000)
        assertEquals(background, background.tick(60_999))
        val locked = background.tick(61_000)
        assertEquals(SpeechLifecyclePhase.Locked, locked.phase); assertFalse(locked.notificationVisible)
        assertEquals(locked, locked.command(command(SpeechControlAction.Resume)))
        assertEquals(locked, locked.foreground(62_000))
        val returned = background.foreground(2_000)
        assertNull(returned.lockDeadlineMillis); assertEquals(returned, returned.tick(62_000))
    }
    @Test fun immediateLockAndProfileExitTerminateWithoutLeakingASourceRequest() {
        val state = SpeechBackgroundExample("p", 1).command(command(SpeechControlAction.Source))
        assertEquals(SpeechLifecyclePhase.Locked, state.background(1_000, 0).phase)
        val ended = state.profileChanged(null)
        assertFalse(ended.sourceRequested); assertFalse(ended.notificationVisible); assertEquals(SpeechLifecyclePhase.Ended, ended.phase)
        assertEquals(ended, ended.command(command(SpeechControlAction.Resume)))
    }
}
