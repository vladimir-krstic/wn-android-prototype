package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class FloatingMessagesTest {
    private val keys = (1..4).map { FloatingMessageKey("profile", "chat", "$it") }
    private val stack = keys.fold(FloatingMessageStack()) { stack, key -> stack.add(key) }

    @Test fun selectedReferenceSurvivesRemovingEarlierMessages() {
        val kept = stack.select(keys[2]).remove(keys[0])
        assertEquals(keys[2], kept.selected)
        assertEquals(keys.drop(1), kept.keys)
    }
    @Test fun removingSelectedUsesNextThenPreviousAtEnd() {
        assertEquals(keys[2], stack.select(keys[1]).remove(keys[1]).selected)
        assertEquals(keys[2], stack.remove(keys[3]).selected)
    }
    @Test fun invalidSelectionCannotEscapeStackAndEmptyRetentionClearsSelection() {
        assertEquals(stack, stack.select(FloatingMessageKey("other", "chat", "1")))
        assertEquals(FloatingMessageStack(), stack.retain(emptySet()))
    }
    @Test fun pagerWrapsBothEndsAndRecentersOnTheSameMessage() {
        for (count in listOf(2, 3, 8)) {
            val pages = FloatingMessagePages(count)
            assertEquals(count - 1, pages.messageAt(pages.pageFor(0) - 1))
            assertEquals(0, pages.messageAt(pages.pageFor(count - 1) + 1))
            for (boundary in listOf(0, pages.pageCount - 1)) {
                assertTrue(pages.isBoundary(boundary))
                val message = pages.messageAt(boundary)
                assertEquals(message, pages.messageAt(pages.pageFor(message)))
                assertFalse(pages.isBoundary(pages.pageFor(message)))
            }
        }
    }
    @Test fun oneMessageHasNoDuplicatePagesOrWrap() {
        val pages = FloatingMessagePages(1)
        assertEquals(1, pages.pageCount)
        assertEquals(0, pages.pageFor(0))
        assertEquals(0, pages.messageAt(0))
        assertFalse(pages.isBoundary(0))
    }
}
