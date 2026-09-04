package dev.ipf.whitenoise.model

import androidx.compose.ui.text.buildAnnotatedString
import dev.ipf.whitenoise.ui.conversation.MessageSourceAnnotation
import dev.ipf.whitenoise.ui.conversation.selectedMessagePassage
import org.junit.Assert.*
import org.junit.Test

class MessageSelectionTest {
    private fun annotated(source: SourceText) = buildAnnotatedString {
        append(source.text)
        source.offsets.indices.forEach { i -> addStringAnnotation(MessageSourceAnnotation, "${source.offsets[i]}:${source.ends[i]}", i, i + 1) }
    }
    @Test fun nativeSubsequenceRetainsExactSecondOccurrenceDespiteEqualText() {
        val source = "echo **echo** echo"
        val middle = MessageDocuments.inline(source)[1].source
        val fragment = annotated(middle).subSequence(1, 4)
        assertEquals(MessagePassage("cho", 8, 11), selectedMessagePassage(source, listOf(fragment)))
    }
    @Test fun multiLeafSelectionKeepsExactAuthoredSpanAcrossFormatting() {
        val source = "# Title\n\nBody **tail**"
        val doc = MessageDocuments.parse(source)
        val title = (doc.blocks.first() as DocumentBlock.Heading).runs.single().source
        val body = (doc.blocks.last() as DocumentBlock.Paragraph).runs.last().source
        val selected = selectedMessagePassage(source, listOf(annotated(title).subSequence(2, 5), annotated(body).subSequence(0, 2)))!!
        assertEquals("tle\nta", selected.text); assertEquals(4, selected.sourceStart); assertEquals(source.indexOf("tail") + 2, selected.sourceEnd)
    }
    @Test fun selectionWithoutSourceAnnotationsCannotClaimAnInventedOffset() {
        assertNull(selectedMessagePassage("echo echo", listOf(buildAnnotatedString { append("echo") })))
    }
    @Test fun entitySelectionRetainsItsWholeAuthoredEncoding() {
        val source = "&#x1F9AB;"
        val selected = selectedMessagePassage(source, listOf(annotated(MessageDocuments.inline(source).single().source)))!!
        assertEquals(MessagePassage("🦫", 0, source.length), selected)
    }
}
