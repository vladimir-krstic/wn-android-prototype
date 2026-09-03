package dev.ipf.whitenoise.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class SignalEmojiTest {
    @Test
    fun atlasParserPreservesSheetOrderVariantsAndTextPresentationAliases() {
        val index = parseSignalEmojiAtlas(
            """{"emoji":{"People_0":[["d83dde00"],["2764fe0f"],["d83ddc4b","d83ddc4bd83cdffb"]]},"metrics":{"raw_width":66}}""",
        )

        assertEquals(SignalEmojiTile("People_0", 0), index["😀"])
        assertEquals(SignalEmojiTile("People_0", 1), index["❤️"])
        assertEquals(SignalEmojiTile("People_0", 1), index["❤"])
        assertEquals(SignalEmojiTile("People_0", 2), index["👋"])
        assertEquals(SignalEmojiTile("People_0", 3), index["👋🏻"])
    }

    @Test
    fun signalHexDecoderPreservesJoinersAndVariationSelectors() {
        assertEquals("❤️‍🔥", decodeSignalEmojiHex("2764fe0f200dd83ddd25"))
    }
}
