package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimelineResizeAnchorTest {
    private val first = TimelineResizeAnchor.Position(5, 37)
    private val last = TimelineResizeAnchor.Position(12, 1400)

    @Test fun bottomFollowsComposerAndKeyboardIncludingAnOversizedLastMessage() {
        val anchor = TimelineResizeAnchor()
        assertNull(anchor.update(800, 60, false, false, first, last))
        assertEquals(last, anchor.update(800, 84, false, false, first, last))
        assertEquals(last, anchor.update(700, 108, false, false, first, last))
        assertEquals(last, anchor.update(600, 108, false, false, first, last))
        assertEquals(last, anchor.update(800, 60, false, false, first, last))
    }

    @Test fun partiallyVisibleLastMessageDoesNotCountAsBottom() {
        val anchor = TimelineResizeAnchor()
        anchor.update(800, 60, true, false, first, last)
        assertEquals(first, anchor.update(600, 108, true, false, first, last))
    }

    @Test fun historyKeepsExactFirstItemAndPixelOffsetAcrossEveryResizeFrame() {
        val anchor = TimelineResizeAnchor()
        anchor.update(800, 60, true, false, first, last)
        repeat(20) { frame ->
            assertEquals(first, anchor.update(790 - frame * 10, 62 + frame * 2, true, false, first, last))
        }
    }

    @Test fun stableGeometryDoesNotOverrideNavigationOrNewMessages() {
        val anchor = TimelineResizeAnchor()
        anchor.update(800, 60, false, false, first, last)
        assertNull(anchor.update(800, 60, true, false, TimelineResizeAnchor.Position(2, 0), last))
    }

    @Test fun resizingNeverCancelsAnActiveUserScroll() {
        val anchor = TimelineResizeAnchor()
        anchor.update(800, 60, false, false, first, last)
        assertNull(anchor.update(600, 108, true, true, first, last))
    }

    @Test fun emptyListAndInitialLayoutDoNotRequestAScroll() {
        val anchor = TimelineResizeAnchor()
        assertNull(anchor.update(800, 0, false, false, first, null))
        assertNull(anchor.update(800, 60, false, false, first, null))
    }
}
