package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiCatalogTest {
    @Test
    fun catalogKeepsCompleteOrderedSectionsAndComplexEmoji() {
        assertEquals(EmojiCategory.entries.toList(), EmojiCatalog.sections.map { it.category })
        assertTrue(EmojiCatalog.all.size > 700)
        assertTrue("🐻‍❄️" in EmojiCatalog.all)
        assertTrue("🏳️‍🌈" in EmojiCatalog.all)
        assertTrue("🇷🇸" in EmojiCatalog.all)
        assertTrue("0️⃣" in EmojiCatalog.all)
    }

    @Test
    fun graphemeSplitterKeepsJoinersFlagsVariationAndKeycapsTogether() {
        assertEquals(
            listOf("❤️‍🔥", "🙂‍↕️", "🇷🇸", "0️⃣"),
            splitEmojiGraphemes("❤️‍🔥🙂‍↕️🇷🇸0️⃣"),
        )
    }

    @Test
    fun searchMatchesAliasesAndWholeCategories() {
        assertTrue(ReactionCatalog.search("beaver").any { "🦫" in it.emoji })
        assertTrue(ReactionCatalog.search("rocket").any { "🚀" in it.emoji })
        assertEquals(
            EmojiCatalog.sections.first { it.category == EmojiCategory.FoodAndDrink }.emoji,
            ReactionCatalog.search("food").single { it.category == EmojiCategory.FoodAndDrink }.emoji,
        )
        assertTrue(ReactionCatalog.search("this-will-not-match").isEmpty())
    }
}
