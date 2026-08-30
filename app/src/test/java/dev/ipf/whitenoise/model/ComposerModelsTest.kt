package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        val uppercaseScheme = LinkPreviewDetector.first("HTTPS://Developer.Android.Com/reference")!!
        assertEquals("developer.android.com", uppercaseScheme.domain)
        assertEquals("Android Developers", uppercaseScheme.title)
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
    fun expansionPolicyClampsAndChoosesOnlyCompactOrExpandedEndpoints() {
        assertEquals(0f, ComposerExpansionPolicy.clampProgress(-0.4f))
        assertEquals(1f, ComposerExpansionPolicy.clampProgress(1.4f))
        assertFalse(ComposerExpansionPolicy.destinationExpanded(0.49f, 0f))
        assertTrue(ComposerExpansionPolicy.destinationExpanded(0.5f, 0f))
        assertTrue(ComposerExpansionPolicy.destinationExpanded(0.1f, -48f))
        assertFalse(ComposerExpansionPolicy.destinationExpanded(0.9f, 48f))
        assertEquals(10, ComposerExpansionPolicy.compactLineLimit(false))
        assertEquals(6, ComposerExpansionPolicy.compactLineLimit(true))
        assertEquals(8, ComposerExpansionPolicy.CompactTranscriptLines)
        assertTrue(ComposerExpansionPolicy.shouldPushTimeline(true))
        assertFalse(ComposerExpansionPolicy.shouldPushTimeline(false))
    }

    @Test
    fun attachmentSizingUsesBoundedVisualAspectAndNativeUtilityCards() {
        assertEquals(68, ComposerAttachmentSizing.forKind(MessageAttachmentKind.Photo, 0.1f).widthDp)
        assertEquals(200, ComposerAttachmentSizing.forKind(MessageAttachmentKind.Video, 4f).widthDp)
        assertEquals(112, ComposerAttachmentSizing.forKind(MessageAttachmentKind.Gif).heightDp)
        assertEquals(
            ComposerAttachmentSize(72, 104),
            ComposerAttachmentSizing.forKind(MessageAttachmentKind.Contact),
        )
        assertEquals(
            ComposerAttachmentSize(72, 160),
            ComposerAttachmentSizing.forKind(MessageAttachmentKind.File),
        )
    }

    @Test
    fun voiceReducerCoversRecordingReviewTranscriptionPlaybackAndRestore() {
        var state: ComposerVoiceState = ComposerVoiceReducer.start(ComposerVoiceState.Idle)
        repeat(17) { state = ComposerVoiceReducer.tick(state) }
        state = ComposerVoiceReducer.stop(state)
        assertEquals(2, (state as ComposerVoiceState.Review).durationSeconds)
        assertTrue(ComposerVoiceReducer.canSend(state))

        state = ComposerVoiceReducer.beginTranscription(state)
        assertTrue((state as ComposerVoiceState.Review).isTranscribing)
        state = ComposerVoiceReducer.finishTranscription(state, VoiceMessageFixture.transcript)
        assertEquals(VoiceMessageFormat.Both, (state as ComposerVoiceState.Review).format)
        state = ComposerVoiceReducer.selectFormat(state, VoiceMessageFormat.Text)
        state = ComposerVoiceReducer.editTranscript(state, "")
        assertFalse(ComposerVoiceReducer.canSend(state))
        state = ComposerVoiceReducer.selectFormat(state, VoiceMessageFormat.Voice)
        assertTrue(ComposerVoiceReducer.canSend(state))

        state = ComposerVoiceReducer.togglePlayback(state)
        state = ComposerVoiceReducer.advancePlayback(state)
        assertEquals(1, (state as ComposerVoiceState.Review).playbackTenths)
        val restored = ComposerVoiceReducer.restore(state) as ComposerVoiceState.Review
        assertFalse(restored.isPlaying)
        assertFalse(restored.isTranscribing)
        val restoredRecording = ComposerVoiceReducer.restore(
            ComposerVoiceState.Recording(21),
        ) as ComposerVoiceState.Review
        assertEquals(3, restoredRecording.durationSeconds)
    }

    @Test
    fun liveWaveformAdvancesCalmlyAsOneDeterministicTrailingWindow() {
        assertEquals(listOf(0, 0, 1, 1, 2), (0..4).map(ComposerWaveformPolicy::visualTick))

        val first = ComposerWaveformPolicy.liveWindow(latestTick = 3, count = 6)
        val next = ComposerWaveformPolicy.liveWindow(latestTick = 4, count = 6)

        assertEquals(ComposerWaveformPolicy.QuietSample, first.first())
        assertEquals(first.drop(1), next.dropLast(1))
        assertTrue(next.all { it in ComposerWaveformPolicy.QuietSample..1f })
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
