package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AvatarWebImageCatalogTest {
    @Test
    fun catalogKeepsTwentyOneUniqueChoices() {
        assertEquals(21, AvatarWebImageCatalog.choices.size)
        assertEquals(21, AvatarWebImageCatalog.choices.map { it.id }.toSet().size)
        assertEquals(
            21,
            AvatarWebImageCatalog.choices.map { it.accessibilityLabel }.toSet().size,
        )
    }

    @Test
    fun displayUrlRoundTripsToChoice() {
        AvatarWebImageCatalog.choices.forEach { choice ->
            assertEquals(
                choice,
                AvatarWebImageCatalog.choiceMatchingUrl(
                    AvatarWebImageCatalog.displayUrl(choice),
                ),
            )
        }
    }

    @Test
    fun validUnknownUrlMapsDeterministicallyAndInvalidUrlDoesNot() {
        val first = AvatarWebImageCatalog.choiceMatchingUrl("https://example.org/a.jpg")
        val second = AvatarWebImageCatalog.choiceMatchingUrl("https://example.org/a.jpg")
        assertEquals(first, second)
        assertTrue(first != null)
        assertNull(AvatarWebImageCatalog.choiceMatchingUrl("not a url"))
        assertNotEquals(
            AvatarWebImageCatalog.results("river").first(),
            AvatarWebImageCatalog.results("signal").first(),
        )
    }
}
