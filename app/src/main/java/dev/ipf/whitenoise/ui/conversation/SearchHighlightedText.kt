package dev.ipf.whitenoise.ui.conversation

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.Normalizer
import java.util.Locale

private val AndroidSearchMatchCyan = Color(AndroidColor.CYAN)

/**
 * Draws Android's platform cyan behind matching glyph runs with the accepted rounded treatment.
 * Compose span backgrounds are square, so the layout result supplies exact per-line glyph bounds.
 */
@Composable
internal fun SearchHighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val ranges = remember(text, query) { searchMatchRanges(text, query) }
    val contentColor = if (color == Color.Unspecified) LocalContentColor.current else color
    val annotated = remember(text, ranges) {
        buildAnnotatedString {
            append(text)
            ranges.forEach { range ->
                addStyle(
                    SpanStyle(color = Color.Black),
                    start = range.first,
                    end = range.last + 1,
                )
            }
        }
    }
    var layoutResult by remember(text, query) { mutableStateOf<TextLayoutResult?>(null) }

    Box(modifier) {
        if (ranges.isNotEmpty()) {
            Canvas(Modifier.matchParentSize()) {
                layoutResult?.drawRoundedTextHighlights(
                    drawScope = this,
                    ranges = ranges,
                    color = AndroidSearchMatchCyan,
                )
            }
        }
        Text(
            text = annotated,
            color = contentColor,
            style = style,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
            onTextLayout = { layoutResult = it },
        )
    }
}

internal fun TextLayoutResult.drawRoundedTextHighlights(
    drawScope: DrawScope,
    ranges: List<IntRange>,
    color: Color,
) = with(drawScope) {
    val radius = 4.dp.toPx()
    ranges.forEach { range ->
        val lineBounds = linkedMapOf<Int, Rect>()
        range.forEach { offset ->
            if (offset !in layoutInput.text.indices || layoutInput.text[offset] == '\n') return@forEach
            val line = getLineForOffset(offset)
            val glyph = getBoundingBox(offset)
            val prior = lineBounds[line]
            lineBounds[line] = if (prior == null) {
                glyph
            } else {
                Rect(
                    left = minOf(prior.left, glyph.left),
                    top = minOf(prior.top, glyph.top),
                    right = maxOf(prior.right, glyph.right),
                    bottom = maxOf(prior.bottom, glyph.bottom),
                )
            }
        }
        lineBounds.values.forEach { bounds ->
            drawRoundRect(
                color = color,
                topLeft = bounds.topLeft,
                size = bounds.size,
                cornerRadius = CornerRadius(radius, radius),
            )
        }
    }
}

internal fun searchMatchRanges(text: String, query: String): List<IntRange> {
    val normalizedQuery = query.normalizedWithSourceIndices().text.trim()
    if (text.isEmpty() || normalizedQuery.isEmpty()) return emptyList()

    val normalizedText = text.normalizedWithSourceIndices()
    val ranges = mutableListOf<IntRange>()
    var searchFrom = 0
    while (searchFrom <= normalizedText.text.length - normalizedQuery.length) {
        val matchStart = normalizedText.text.indexOf(normalizedQuery, startIndex = searchFrom)
        if (matchStart < 0) break
        val matchEnd = matchStart + normalizedQuery.length - 1
        ranges += normalizedText.sourceStarts[matchStart]..normalizedText.sourceEnds[matchEnd]
        searchFrom = matchStart + normalizedQuery.length
    }
    return ranges
}

internal fun conversationSearchMessageAlpha(
    isSearching: Boolean,
    query: String,
    isResult: Boolean,
): Float = if (!isSearching || query.isBlank() || isResult) 1f else 0.38f

private data class IndexedNormalizedText(
    val text: String,
    val sourceStarts: List<Int>,
    val sourceEnds: List<Int>,
)

private fun String.normalizedWithSourceIndices(): IndexedNormalizedText {
    val normalized = StringBuilder()
    val starts = mutableListOf<Int>()
    val ends = mutableListOf<Int>()
    var offset = 0
    while (offset < length) {
        val codePoint = codePointAt(offset)
        val codePointLength = Character.charCount(codePoint)
        val sourceEnd = offset + codePointLength - 1
        val folded = Normalizer.normalize(
            String(Character.toChars(codePoint)).lowercase(Locale.ROOT),
            Normalizer.Form.NFD,
        ).filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
        folded.forEach { character ->
            normalized.append(character)
            starts += offset
            ends += sourceEnd
        }
        offset += codePointLength
    }
    return IndexedNormalizedText(normalized.toString(), starts, ends)
}
