package dev.ipf.whitenoise.ui.conversation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/** Display-only dots participate in native wrapping/scrolling, never in the editable value. */
internal class ComposerTextTransformation(
    private val mentions: List<IntRange>,
    private val mentionColor: Color,
    private val dictationCursor: Int? = null,
    private val indicatorColor: Color = Color.Unspecified,
    private val dotAlphas: List<Float> = listOf(1f, 1f, 1f),
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val styled = AnnotatedString.Builder(text)
        mentions.filter { it.first >= 0 && it.last < text.length }.forEach {
            styled.addStyle(SpanStyle(color = mentionColor), it.first, it.last + 1)
        }
        val cursor = dictationCursor?.coerceIn(0, text.length)
            ?: return TransformedText(styled.toAnnotatedString(), OffsetMapping.Identity)
        val original = styled.toAnnotatedString()
        val display = AnnotatedString.Builder().apply {
            append(original.subSequence(0, cursor))
            append(DictationDots)
            repeat(3) { dot ->
                addStyle(SpanStyle(color = indicatorColor.copy(alpha = dotAlphas[dot])),
                    cursor + 1 + dot, cursor + 2 + dot)
            }
            append(original.subSequence(cursor, original.length))
        }
        return TransformedText(display.toAnnotatedString(), object : OffsetMapping {
            // Keep the caret immediately before the indicator; tapping a dot maps back to it.
            override fun originalToTransformed(offset: Int): Int =
                if (offset <= cursor) offset else offset + DictationDots.length

            override fun transformedToOriginal(offset: Int): Int = when {
                offset <= cursor -> offset
                offset <= cursor + DictationDots.length -> cursor
                else -> offset - DictationDots.length
            }
        })
    }

    /** Exclude the inserted indicator from rounded mention backgrounds, including split names. */
    fun highlightRanges(textLength: Int): List<IntRange> {
        val cursor = dictationCursor?.coerceIn(0, textLength) ?: return mentions
        return mentions.flatMap { range ->
            buildList {
                if (range.first < cursor) add(range.first..minOf(range.last, cursor - 1))
                if (range.last >= cursor) add((maxOf(range.first, cursor) + DictationDots.length)..(range.last + DictationDots.length))
            }
        }
    }
}

// Nonbreaking spaces keep the three dots together in the editor's native text layout.
internal const val DictationDots = "\u00a0···\u00a0"
