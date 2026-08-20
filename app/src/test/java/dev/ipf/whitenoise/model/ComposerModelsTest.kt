package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerModelsTest {
    @Test
    fun deterministicLinkPreviewUsesFirstEligibleHttpsUrl() {
        assertNull(LinkPreviewDetector.first("http://whitenoise.chat"))
        val preview = LinkPreviewDetector.first(
            "See https://developer.android.com/develop/ui/compose, then https://whitenoise.chat",
        )!!

        assertEquals("developer.android.com", preview.domain)
        assertEquals("Android Developers", preview.title)
        assertEquals("https://developer.android.com/develop/ui/compose", preview.url)
        assertEquals(MessageAttachmentKind.Link, preview.attachment("link").kind)
    }

    @Test
    fun mediaLayoutDerivesEveryAcceptedCountAndOverflow() {
        assertEquals(
            listOf(
                MediaGridLayout.Single,
                MediaGridLayout.Two,
                MediaGridLayout.Three,
                MediaGridLayout.Four,
                MediaGridLayout.Five,
                MediaGridLayout.FiveWithOverflow,
                MediaGridLayout.FiveWithOverflow,
            ),
            (1..7).map(MediaLayout::forCount),
        )
        assertEquals(5, MediaLayout.visibleCount(7))
        assertEquals(2, MediaLayout.overflowCount(7))
        assertEquals(0, MediaLayout.visibleCount(0))
    }

    @Test
    fun voiceFormatsProduceExactlyOneLogicalMessageShape() {
        val voice = VoiceMessageFixture.result("voice", VoiceMessageFormat.Voice)
        val text = VoiceMessageFixture.result("text", VoiceMessageFormat.Text)
        val both = VoiceMessageFixture.result("both", VoiceMessageFormat.Both)

        assertTrue(voice.first.isEmpty())
        assertEquals(1, voice.second.size)
        assertEquals(VoiceMessageFixture.transcript, text.first)
        assertTrue(text.second.isEmpty())
        assertEquals(VoiceMessageFixture.transcript, both.first)
        assertEquals(1, both.second.size)
        assertEquals(VoiceMessageFormat.Both, both.second.single().voiceFormat)
        assertEquals(VoiceMessageFixture.durationSeconds, both.second.single().durationSeconds)
    }

    @Test
    fun allTwelveComposerCatalogRowsHaveOneAuthoritativeSeed() {
        val ids = listOf(
            "catalog-composer-text",
            "catalog-composer-multiline",
            "catalog-composer-link",
            "catalog-composer-link-preview",
            "catalog-composer-photo",
            "catalog-composer-photo-album",
            "catalog-composer-mixed-media",
            "catalog-composer-file",
            "catalog-composer-gif",
            "catalog-composer-contact",
            "catalog-composer-reply",
            "catalog-composer-mention",
        )
        val seeds = ids.map(ComposerFixtures::seed)

        assertEquals(12, seeds.size)
        assertTrue(seeds.all { it.text.isNotBlank() || it.attachments.isNotEmpty() || it.replyMessageId != null })
        assertEquals(4, ComposerFixtures.seed("catalog-composer-photo-album").attachments.size)
        assertEquals(3, ComposerFixtures.seed("catalog-composer-mixed-media").attachments.size)
        assertEquals("https://whitenoise.chat", ComposerFixtures.seed("catalog-composer-link").suppressedLinkUrl)
        assertEquals("CMP-REPLY-source", ComposerFixtures.seed("catalog-composer-reply").replyMessageId)
    }
}
