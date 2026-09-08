package dev.ipf.whitenoise.ui.conversation

import org.junit.Assert.*
import org.junit.Test

class InlineFooterGeometryTest {
    private fun geometry(textWidth: Int = 60, textHeight: Int = 24, lastRight: Int = 60,
        baseline: Int = 18, footerWidth: Int = 50, footerHeight: Int = 16,
        footerBaseline: Int = 12, maxWidth: Int = 300, minWidth: Int = 0) = inlineFooterGeometry(
        textWidth, textHeight, lastRight, baseline, footerWidth, footerHeight,
        footerBaseline, maxWidth, minWidth, gap = 8)

    @Test fun shortTextSharesItsBaselineAndGrowsOnlyEnoughForTheTime() {
        val result = geometry()
        assertEquals(118, result.width)
        assertEquals(24, result.height)
        assertEquals(68, result.x)
        assertEquals(18, result.y + 12)
    }

    @Test fun wrappedTextUsesSpaceOnItsShortFinalLine() {
        val result = geometry(textWidth = 300, textHeight = 72, lastRight = 140, baseline = 66)
        assertEquals(300, result.width)
        assertEquals(72, result.height)
        assertEquals(250, result.x)
        assertEquals(66, result.y + 12)
    }

    @Test fun fullFinalLineMovesFooterBelowInsteadOfOverlappingText() {
        val result = geometry(textWidth = 300, textHeight = 72, lastRight = 285, baseline = 66)
        assertEquals(76, result.y)
        assertEquals(92, result.height)
        assertEquals(250, result.x)
    }

    @Test fun rightAlignedRtlFinalLineUsesTheSameCollisionRule() {
        val result = geometry(textWidth = 300, lastRight = 300)
        assertTrue(result.y >= 24)
        assertEquals(300, result.x + 50)
    }

    @Test fun largeTypeAndLongLocalizedFailureLabelStayInside() {
        val result = geometry(textWidth = 250, textHeight = 48, lastRight = 250,
            baseline = 36, footerWidth = 220, footerHeight = 40, footerBaseline = 30)
        assertEquals(250, result.width)
        assertEquals(30, result.x)
        assertEquals(result.height, result.y + 40)
    }

    @Test fun widthReservedByAQuoteOrReactionsKeepsFooterAtTheEdge() {
        val result = geometry(minWidth = 240)
        assertEquals(240, result.width)
        assertEquals(190, result.x)
        assertEquals(24, result.height)
    }
}
