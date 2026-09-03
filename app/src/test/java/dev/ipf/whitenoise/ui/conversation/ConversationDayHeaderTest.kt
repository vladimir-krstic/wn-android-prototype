package dev.ipf.whitenoise.ui.conversation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationDayHeaderTest {
    private val dayHeaderIndices = listOf(0, 3, 7)

    @Test
    fun currentDayPinsOnlyAfterItsInlineHeaderLeavesTheViewport() {
        assertEquals(
            7,
            pinnedConversationDayHeaderIndex(
                dayHeaderIndices = dayHeaderIndices,
                topVisibleItemIndex = 9,
                isHeaderVisible = { it in setOf(9, 10, 11) },
            ),
        )
    }

    @Test
    fun visibleInlineHeaderSuppressesThePinnedReplacement() {
        assertNull(
            pinnedConversationDayHeaderIndex(
                dayHeaderIndices = dayHeaderIndices,
                topVisibleItemIndex = 7,
                isHeaderVisible = { it in setOf(7, 8, 9) },
            ),
        )
    }

    @Test
    fun reachingTheNextInlineHeaderSwitchesBackToTranscriptText() {
        assertNull(
            pinnedConversationDayHeaderIndex(
                dayHeaderIndices = dayHeaderIndices,
                topVisibleItemIndex = 3,
                isHeaderVisible = { it in setOf(3, 4, 5) },
            ),
        )
    }

    @Test
    fun layoutWithoutVisibleContentDoesNotInventAPinnedHeader() {
        assertNull(
            pinnedConversationDayHeaderIndex(
                dayHeaderIndices = dayHeaderIndices,
                topVisibleItemIndex = null,
                isHeaderVisible = { false },
            ),
        )
    }
}
