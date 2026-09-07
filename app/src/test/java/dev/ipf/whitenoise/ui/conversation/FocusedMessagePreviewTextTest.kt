package dev.ipf.whitenoise.ui.conversation

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.*
import org.junit.Test

class FocusedMessagePreviewTextTest {
    @Test fun ordinaryMarkupKeepsItsVisibleTextAndEmphasisWithoutClickableLinks() {
        val preview = focusedMessagePreviewText("**Plan** with *Maya* and [notes](https://example.org/private)")
        assertEquals("Plan with Maya and notes", preview.text)
        assertTrue(preview.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(preview.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
        assertTrue(preview.spanStyles.any { it.item.textDecoration == TextDecoration.Underline })
        assertFalse(preview.text.contains("https://"))
        assertTrue(preview.getLinkAnnotations(0, preview.length).isEmpty())
    }

    @Test fun longDocumentsUseAReadableExcerptProjectionWithoutMarkdownControlSyntax() {
        val source = "# Trail plan\n\n- Meet at nine\n- Bring water\n\n```text\nLast line\n```"
        val preview = focusedMessagePreviewText(source).text
        assertTrue(preview.startsWith("Trail plan"))
        assertTrue(preview.contains("• Meet at nine"))
        assertTrue(preview.contains("Last line"))
        assertFalse(preview.contains("```"))
        assertFalse(preview.contains("# Trail"))
    }
}
