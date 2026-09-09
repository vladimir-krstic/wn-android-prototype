package dev.ipf.whitenoise.model

/** Capture the previous viewport before IME/composer remeasurement, not after it scrolls. */
internal class TimelineResizeAnchor {
    data class Position(val index: Int, val offset: Int)
    private var geometry: Pair<Int, Int>? = null

    fun update(
        height: Int,
        bottomPadding: Int,
        canScrollForward: Boolean,
        scrolling: Boolean,
        first: Position,
        last: Position?,
    ): Position? {
        val next = height to bottomPadding
        val previous = geometry
        geometry = next
        if (previous == null || previous == next || scrolling || last == null) return null
        // `last.offset` is its measured size: asking for its end clamps to the true list end,
        // including for a last message taller than the viewport. History keeps its exact offset.
        return if (canScrollForward) first else last
    }
}
