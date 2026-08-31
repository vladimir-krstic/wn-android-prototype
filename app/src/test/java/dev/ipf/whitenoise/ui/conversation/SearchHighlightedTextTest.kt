package dev.ipf.whitenoise.ui.conversation

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchHighlightedTextTest {
    @Test
    fun matchRangesPreserveSourceGlyphsAcrossCaseDiacriticsAndOccurrences() {
        assertEquals(
            listOf(0..3, 5..8, 10..13),
            searchMatchRanges("Café cafe CAFÉ", " cafe "),
        )
    }

    @Test
    fun matchingMessagesStayAtFullContrastWhileOnlyNonmatchesAreSubdued() {
        assertEquals(1f, conversationSearchMessageAlpha(true, "out", true))
        assertEquals(0.38f, conversationSearchMessageAlpha(true, "out", false))
        assertEquals(1f, conversationSearchMessageAlpha(true, "", false))
        assertEquals(1f, conversationSearchMessageAlpha(false, "out", false))
    }

    @Test
    fun mentionRangesMatchCompleteNamesWithoutStylingEmbeddedText() {
        assertEquals(
            listOf(0..9, 15..24),
            mentionRanges(
                text = "@Maya Chen and @maya chen",
                mentionNames = listOf("Maya Chen", "Maya"),
            ),
        )
        assertEquals(emptyList<IntRange>(), mentionRanges("email@Maya Chen", listOf("Maya Chen")))
    }
}
