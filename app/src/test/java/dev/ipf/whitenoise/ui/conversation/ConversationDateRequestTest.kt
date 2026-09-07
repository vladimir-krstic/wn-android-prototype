package dev.ipf.whitenoise.ui.conversation

import dev.ipf.whitenoise.model.*
import org.junit.Assert.*
import org.junit.Test

class ConversationDateRequestTest {
    private val chat = Chat("dates", 0, ChatKind.Group, "Dates", timeline =
        (0 until 60).map { ChatTimelineEntry.Event("e$it", "Event $it", minuteOfDay = it) })
    private fun state() = ConversationHistoryUiState(ConversationHistory.initial(chat), chat.timeline.mapTo(hashSetOf()) { it.id }, null)

    @Test fun failedDateSeekRetainsWindowAndRetryPreservesEventTarget() {
        val state = state(); val previous = state.windowIds
        state.target(chat, "e3", HistoryScenario.TargetFails, dateJump = true)
        state.complete(chat, state.request!!)
        assertEquals(previous, state.windowIds)
        assertEquals(HistoryPhase.Failed, state.request?.phase)
        state.retry()
        assertTrue(state.request!!.dateJump)
        state.complete(chat, state.request!!)
        assertEquals("e3", state.readyTarget?.targetId)
        assertTrue("e3" in state.windowIds)
        assertEquals(18, state.windowIds.size)
    }

    @Test fun cancelledOrSupersededRequestsCannotMoveTheWindow() {
        val state = state(); val previous = state.windowIds
        state.target(chat, "e3", HistoryScenario.Success, dateJump = true)
        val cancelled = state.request!!
        state.cancel(); state.complete(chat, cancelled)
        assertEquals(previous, state.windowIds)
        state.target(chat, "e20", HistoryScenario.Success, dateJump = true)
        val superseded = state.request!!
        state.target(chat, "e30", HistoryScenario.Success, dateJump = true)
        state.complete(chat, superseded)
        assertEquals(previous, state.windowIds)
        state.complete(chat, state.request!!)
        assertEquals("e30", state.readyTarget?.targetId)
    }

    @Test fun removedDateTargetIsUnavailableWithoutClearingTheOldWindow() {
        val state = state(); val previous = state.windowIds
        state.target(chat, "e3", HistoryScenario.Success, dateJump = true)
        state.complete(chat.copy(timeline = chat.timeline.filterNot { it.id == "e3" }), state.request!!)
        assertEquals(HistoryPhase.Unavailable, state.request?.phase)
        assertEquals(previous, state.windowIds)
        assertNull(state.readyTarget)
    }
}
