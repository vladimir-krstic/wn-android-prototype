package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Test

class InlineMessageMarkupTest {
    @Test
    fun `pinned markdown scenario preserves presentation and strips syntax from plain text`() {
        val source = "TXT-09: **Bold**, *emphasis*, and [White Noise](https://whitenoise.chat)"

        assertEquals(
            listOf(
                InlineMessageSegment("TXT-09: "),
                InlineMessageSegment("Bold", strong = true),
                InlineMessageSegment(", "),
                InlineMessageSegment("emphasis", emphasized = true),
                InlineMessageSegment(", and "),
                InlineMessageSegment("White Noise", destination = "https://whitenoise.chat"),
            ),
            InlineMessageMarkup.segments(source),
        )
        assertEquals(
            "TXT-09: Bold, emphasis, and White Noise",
            InlineMessageMarkup.plainText(source),
        )
    }

    @Test
    fun `bare url is actionable without changing visible content`() {
        val source = "TXT-10: https://developer.apple.com/design/human-interface-guidelines"

        assertEquals(source, InlineMessageMarkup.plainText(source))
        assertEquals(
            "https://developer.apple.com/design/human-interface-guidelines",
            InlineMessageMarkup.segments(source).last().destination,
        )
    }

    @Test
    fun `unfinished syntax remains literal`() {
        assertEquals("Keep **everything", InlineMessageMarkup.plainText("Keep **everything"))
        assertEquals("Keep [everything", InlineMessageMarkup.plainText("Keep [everything"))
    }
}
