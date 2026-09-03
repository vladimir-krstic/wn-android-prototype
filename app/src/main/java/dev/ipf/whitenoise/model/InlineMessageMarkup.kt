package dev.ipf.whitenoise.model

/**
 * The deterministic message subset supported by the pinned iOS prototype.
 *
 * It intentionally handles inline presentation only: strong/emphasized text,
 * named links, and bare HTTP(S) links. Unknown or incomplete syntax is kept as
 * literal text so message content is never lost.
 */
data class InlineMessageSegment(
    val text: String,
    val strong: Boolean = false,
    val emphasized: Boolean = false,
    val destination: String? = null,
)

object InlineMessageMarkup {
    private val namedLink = Regex("^\\[([^]\\n]+)]\\((https?://[^)\\s]+)\\)")
    private val bareLink = Regex("^https?://[^\\s]+")

    fun segments(source: String): List<InlineMessageSegment> {
        if (source.isEmpty()) return emptyList()
        val result = mutableListOf<InlineMessageSegment>()
        var cursor = 0
        while (cursor < source.length) {
            val remainder = source.substring(cursor)
            val link = namedLink.find(remainder)
            if (link != null) {
                result += InlineMessageSegment(
                    text = link.groupValues[1],
                    destination = link.groupValues[2],
                )
                cursor += link.value.length
                continue
            }

            if (remainder.startsWith("**")) {
                val end = source.indexOf("**", startIndex = cursor + 2)
                if (end > cursor + 2) {
                    result += InlineMessageSegment(
                        text = source.substring(cursor + 2, end),
                        strong = true,
                    )
                    cursor = end + 2
                    continue
                }
            }

            if (remainder.startsWith('*')) {
                val end = source.indexOf('*', startIndex = cursor + 1)
                if (end > cursor + 1) {
                    result += InlineMessageSegment(
                        text = source.substring(cursor + 1, end),
                        emphasized = true,
                    )
                    cursor = end + 1
                    continue
                }
            }

            val rawUrl = bareLink.find(remainder)
            if (rawUrl != null) {
                val value = rawUrl.value.trimEnd('.', ',', ';', ':', '!', '?')
                result += InlineMessageSegment(text = value, destination = value)
                cursor += value.length
                continue
            }

            val nextMarker = nextMarkupStart(source, cursor + 1)
            result += InlineMessageSegment(source.substring(cursor, nextMarker))
            cursor = nextMarker
        }
        return result.mergeAdjacentPlainSegments()
    }

    fun plainText(source: String): String = segments(source).joinToString("") { it.text }

    private fun nextMarkupStart(source: String, fromIndex: Int): Int {
        var next = source.length
        listOf(
            source.indexOf('[', fromIndex),
            source.indexOf('*', fromIndex),
            source.indexOf("http://", fromIndex),
            source.indexOf("https://", fromIndex),
        ).filter { it >= 0 }.forEach { next = minOf(next, it) }
        return next
    }

    private fun List<InlineMessageSegment>.mergeAdjacentPlainSegments(): List<InlineMessageSegment> =
        fold(mutableListOf()) { merged, segment ->
            val prior = merged.lastOrNull()
            if (
                prior != null &&
                !prior.strong && !prior.emphasized && prior.destination == null &&
                !segment.strong && !segment.emphasized && segment.destination == null
            ) {
                merged[merged.lastIndex] = prior.copy(text = prior.text + segment.text)
            } else {
                merged += segment
            }
            merged
        }
}
