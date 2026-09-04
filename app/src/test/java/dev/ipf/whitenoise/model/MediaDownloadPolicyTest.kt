package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class MediaDownloadPolicyTest {
    @Test fun allSixteenCellsCanChangeWithoutChangingPeers() {
        DownloadMediaType.entries.forEach { type -> DownloadNetwork.entries.forEach { network ->
            val empty = MediaDownloadMatrix(emptySet())
            val changed = empty.change(type, network, true)
            assertEquals(setOf(type to network), changed.enabled)
            assertTrue(changed.allows(type, setOf(network)))
            assertEquals(empty, changed.change(type, network, false))
        } }
    }
    @Test fun defaultsPreservePhotosAndVoiceOnUnmeteredWifiOnly() {
        assertEquals(setOf(DownloadMediaType.Photos to DownloadNetwork.Wifi, DownloadMediaType.Audio to DownloadNetwork.Wifi), MediaDownloadMatrix().enabled)
        assertFalse(MediaDownloadMatrix().allows(DownloadMediaType.Photos, DownloadNetworkExample.MeteredWifi.conditions))
    }
    @Test fun everyOverlappingConditionMustAllowAndUnknownNeverAdmits() {
        val all = MediaDownloadMatrix(DownloadMediaType.entries.flatMap { type -> DownloadNetwork.entries.map { type to it } }.toSet())
        DownloadNetworkExample.entries.forEach { example ->
            assertEquals(example.conditions.isNotEmpty(), all.allows(DownloadMediaType.Photos, example.conditions))
            example.conditions.forEach { refused ->
                assertFalse(all.change(DownloadMediaType.Photos, refused, false).allows(DownloadMediaType.Photos, example.conditions))
            }
        }
    }
    @Test fun importedAudioUsesAudioPolicyAndContactLinkCardsAreNotDownloads() {
        assertEquals(DownloadMediaType.Audio, MessageAttachment("a", MessageAttachmentKind.File, "clip", mimeType = "audio/ogg").downloadMediaType)
        assertEquals(DownloadMediaType.Files, MessageAttachment("a", MessageAttachmentKind.File, "notes", mimeType = "text/plain").downloadMediaType)
        assertNull(MessageAttachment("a", MessageAttachmentKind.Contact, "Person").downloadMediaType)
        assertNull(MessageAttachment("a", MessageAttachmentKind.Link, "Link").downloadMediaType)
    }
    @Test fun stopClearsOnlyQueuedAutomaticAndInvalidatesOldCallbacks() {
        val queued = AttachmentTransfer(phase = AttachmentTransferPhase.Idle).admitAutomatically()
        val stopped = queued.stopAutomatic()
        assertEquals(AttachmentTransferPhase.Idle, stopped.phase)
        assertEquals(stopped, stopped.advance(queued.revision))
        val active = queued.advance(queued.revision)
        assertEquals(active, active.stopAutomatic())
        val manual = queued.requestManual()
        assertEquals(AttachmentTransferOrigin.Manual, manual.origin)
        assertEquals(manual, manual.stopAutomatic())
        assertEquals(queued.attempt, manual.attempt)
    }
    @Test fun cancellationSuppressesAutomaticUntilExplicitRecovery() {
        val cancelled = AttachmentTransfer(phase = AttachmentTransferPhase.Idle).admitAutomatically().cancel()
        assertTrue(cancelled.automaticSuppressed)
        assertEquals(cancelled, cancelled.admitAutomatically())
        assertEquals(AttachmentTransferOrigin.Manual, cancelled.requestManual().origin)
        val restarted = cancelled.restartAutomatic()
        assertFalse(restarted.automaticSuppressed)
        assertEquals(AttachmentTransferPhase.Queued, restarted.admitAutomatically().phase)
    }
    @Test fun alreadyAcceptedRequestDoesNotNeedPolicyToAdvance() {
        var transfer = AttachmentTransfer(phase = AttachmentTransferPhase.Idle).admitAutomatically()
        repeat(5) { transfer = transfer.advance(transfer.revision) }
        assertEquals(AttachmentTransferPhase.Available, transfer.phase)
    }
    @Test fun qualityUsesApprovedPhotoDefaultAndProductionVoicePolicy() {
        assertEquals(SentMediaQuality.High, ProfileSettings().sentMediaQuality)
        assertEquals(95, SentMediaQuality.High.photoQuality.jpegQuality)
        assertEquals(listOf(32_000, 64_000, 96_000, 96_000), SentMediaQuality.entries.map { it.voiceBitrate })
        assertEquals(PhotoQuality.entries, SentMediaQuality.entries.map { it.photoQuality })
    }
    @Test fun voiceQualitySurvivesRecordingReviewRestoreAndTranscriptEdits() {
        SentMediaQuality.entries.forEach { quality ->
            val recording = ComposerVoiceReducer.start(ComposerVoiceState.Idle, quality = quality)
            val review = ComposerVoiceReducer.restore(ComposerVoiceReducer.tick(recording)) as ComposerVoiceState.Review
            val edited = ComposerVoiceReducer.editTranscript(review, "Hello") as ComposerVoiceState.Review
            assertEquals(quality, edited.quality)
            val attachment = VoiceMessageFixture.result("id", VoiceMessageFormat.Both, "Hello", edited.durationSeconds, edited.quality).second.single()
            assertEquals(quality, attachment.voiceQuality)
        }
    }
}
