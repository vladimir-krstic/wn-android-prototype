package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class ConversationTailJumpTest {
    private val keys = listOf("a", "b", "c", "d")
    private fun ConversationTailJump.layout(vararg rows: ConversationTailJump.Row,
        viewport: Int = 500, width: Int = 400, newer: Boolean = false, ids: List<String> = keys,
    ) = update(ids, rows.toList(), viewport, 600, 100, 8, width, newer)
    private fun row(id: String, offset: Int, height: Int) = ConversationTailJump.Row(id, offset, height)

    @Test fun appearsAtOneUnobscuredViewportAndHidesOnReturn() {
        val tracker = ConversationTailJump()
        assertFalse(tracker.layout(row("d", -400, 900)))
        assertFalse(tracker.layout(row("d", 99, 900)))
        assertTrue(tracker.layout(row("d", 100, 900)))
        assertFalse(tracker.layout(row("d", 99, 900)))
        assertFalse(tracker.layout(row("d", -400, 900)))
    }

    @Test fun differentHeightGalleriesLeavingTheViewportDoNotBlink() {
        val tracker = ConversationTailJump()
        tracker.layout(row("d", -400, 900))
        assertTrue(tracker.layout(row("c", -100, 300), row("d", 208, 900)))
        assertTrue(tracker.layout(row("c", 292, 300)))
        assertTrue(tracker.layout(row("b", -1100, 1500), row("c", 408, 300)))
        assertTrue(tracker.layout(row("b", -950, 1500)))
        // Reversing past a row boundary still uses the measured trailing gallery.
        assertTrue(tracker.layout(row("c", 292, 300)))
        assertTrue(tracker.layout(row("c", -100, 300), row("d", 208, 900)))
    }

    @Test fun prependingHistoryKeepsMeasurementsByKey() {
        val tracker = ConversationTailJump()
        tracker.layout(row("d", -400, 900))
        assertTrue(tracker.layout(row("c", 292, 300), ids = listOf("older") + keys))
        assertFalse(tracker.layout(row("d", -400, 900), ids = listOf("older") + keys))
    }

    @Test fun remeasurementGapsRetainVisibilityUntilValidLayout() {
        val tracker = ConversationTailJump()
        assertTrue(tracker.layout(row("d", 100, 900)))
        assertTrue(tracker.layout())
        assertTrue(tracker.layout(row("d", 100, 900), viewport = 0))
        assertFalse(tracker.layout(row("d", -400, 900)))
    }

    @Test fun viewportResizeChangesThresholdAndWidthClearsStaleHeights() {
        val tracker = ConversationTailJump()
        assertTrue(tracker.layout(row("d", 100, 900)))
        assertFalse(tracker.layout(row("d", 100, 900), viewport = 700))
        assertFalse(tracker.layout(row("d", 0, 400), width = 700))
        // The old 900px trailing height must no longer be used.
        assertFalse(tracker.layout(row("c", -300, 300), width = 700))
    }

    @Test fun distantTargetsAndNewerUnloadedHistoryKeepRecoveryAvailable() {
        val tracker = ConversationTailJump()
        assertTrue(tracker.layout(row("b", 0, 600)))
        assertTrue(tracker.layout(row("d", -400, 900), newer = true))
        assertFalse(tracker.layout(row("d", -400, 900)))
    }

    @Test fun largeMeasuredRowsDoNotOverflow() {
        assertTrue(ConversationTailJump().layout(row("d", 100, Int.MAX_VALUE)))
    }
}
