package dev.ipf.whitenoise.ui.conversation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.*
import org.junit.Test

class ComposerTextTransformationTest {
    @Test fun indicatorMapsEveryDisplayOffsetBackToTheOriginalDraft() {
        listOf("", "Hello world", "مرحبا بالعالم", "Hello 👋\nNext line").forEach { draft ->
            (0..draft.length).filterNot { it < draft.length && draft[it].isLowSurrogate() }.forEach { cursor ->
                val source = AnnotatedString(draft)
                val display = ComposerTextTransformation(emptyList(), Color.Black, cursor, Color.Gray).filter(source)
                assertEquals(draft.take(cursor) + DictationDots + draft.drop(cursor), display.text.text)
                assertEquals(draft, source.text)
                (0..draft.length).forEach { original ->
                    assertEquals(original, display.offsetMapping.transformedToOriginal(
                        display.offsetMapping.originalToTransformed(original)))
                }
                val mapped = (0..display.text.length).map(display.offsetMapping::transformedToOriginal)
                assertEquals(mapped.sorted(), mapped)
                assertTrue(mapped.all { it in 0..draft.length })
                (cursor..cursor + DictationDots.length).forEach {
                    assertEquals(cursor, display.offsetMapping.transformedToOriginal(it))
                }
            }
        }
    }

    @Test fun pausingRemovesTheIndicatorAndRestoresIdentityOffsets() {
        val source = AnnotatedString("Hello world")
        val display = ComposerTextTransformation(emptyList(), Color.Black).filter(source)
        assertEquals(source, display.text)
        (0..source.length).forEach { assertEquals(it, display.offsetMapping.originalToTransformed(it)) }
    }

    @Test fun mentionBackgroundsAndStylesFollowTheTextOnBothSidesOfTheIndicator() {
        val transformation = ComposerTextTransformation(listOf(0..9, 15..24), Color.Black, 5, Color.Gray)
        val display = transformation.filter(AnnotatedString("@Maya Chen and @maya chen!"))
        assertEquals(listOf(0..4, 10..14, 20..29), transformation.highlightRanges(25))
        transformation.highlightRanges(25).forEach { range ->
            assertTrue(display.text.spanStyles.any { it.start <= range.first && it.end > range.last && it.item.color == Color.Black })
        }
        (5 until 5 + DictationDots.length).forEach { dot ->
            assertFalse(transformation.highlightRanges(25).any { dot in it })
        }
    }
}
