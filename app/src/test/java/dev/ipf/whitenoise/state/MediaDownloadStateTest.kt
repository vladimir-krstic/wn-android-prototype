package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class MediaDownloadStateTest {
    private data class Target(val chat: String, val message: String, val attachment: MessageAttachment)
    private fun setup(): AppViewModel = AppViewModel().apply {
        completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true); loadDownloadQueueExample()
    }
    private fun targets(vm: AppViewModel): List<Target> = vm.uiState.activeProfile!!.chats.flatMap { chat ->
        chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().flatMap { entry ->
            entry.message.attachments.filter { it.transfer != null }.map { Target(chat.id, entry.message.id, it) }
        }
    }
    private fun current(vm: AppViewModel, target: Target) = targets(vm).first { it.chat == target.chat && it.message == target.message && it.attachment.id == target.attachment.id }.attachment.transfer!!
    private fun action(vm: AppViewModel, target: Target, action: String, revision: Long = current(vm, target).revision) =
        vm.attachmentTransferAction(vm.uiState.activeProfileId!!, target.chat, target.message, target.attachment.id, action, revision)
    private fun allowAll(vm: AppViewModel) = vm.updateProfileSettings(vm.uiState.activeProfile!!.settings.copy(downloadMatrix =
        MediaDownloadMatrix(DownloadMediaType.entries.flatMap { type -> DownloadNetwork.entries.map { type to it } }.toSet())))

    @Test fun stopPreservesActiveAndManualWhileRestartReadmitsEligibleWaitingRequests() {
        val vm = setup(); val id = vm.uiState.activeProfileId!!; val before = targets(vm)
        assertTrue(before.any { it.attachment.transfer!!.phase == AttachmentTransferPhase.Queued && it.attachment.transfer!!.origin == AttachmentTransferOrigin.Automatic })
        vm.pauseAutomaticDownloads(id, true)
        before.forEach { target ->
            val old = target.attachment.transfer!!
            assertEquals(if (old.phase == AttachmentTransferPhase.Queued && old.origin == AttachmentTransferOrigin.Automatic) old.stopAutomatic() else old, current(vm, target))
        }
        allowAll(vm); vm.admitAutomaticDownloads(id)
        assertEquals(0, vm.uiState.activeProfile!!.downloadQueueCounts().automatic)
        vm.pauseAutomaticDownloads(id, false); vm.admitAutomaticDownloads(id)
        assertTrue(vm.uiState.activeProfile!!.downloadQueueCounts().automatic > 0)
    }
    @Test fun explicitTapPromotesOneRequestAndOldAdvanceCannotWinAfterStop() {
        val vm = setup(); val id = vm.uiState.activeProfileId!!
        val target = targets(vm).first { it.attachment.transfer!!.phase == AttachmentTransferPhase.Queued && it.attachment.transfer!!.origin == AttachmentTransferOrigin.Automatic }
        val oldRevision = current(vm, target).revision
        action(vm, target, "start"); val promoted = current(vm, target)
        assertEquals(AttachmentTransferOrigin.Manual, promoted.origin)
        vm.pauseAutomaticDownloads(id, true)
        assertEquals(promoted, current(vm, target))
        vm.holdDownloadTransfers(false); action(vm, target, "advance", oldRevision)
        assertEquals(promoted, current(vm, target))
        action(vm, target, "advance"); assertEquals(AttachmentTransferPhase.Active, current(vm, target).phase)
    }
    @Test fun tightRulesDoNotRevokeAcceptedWorkAndCapacityEventuallyOpens() {
        val vm = setup(); allowAll(vm); vm.admitAutomaticDownloads(vm.uiState.activeProfileId!!)
        vm.updateProfileSettings(vm.uiState.activeProfile!!.settings.copy(downloadMatrix = MediaDownloadMatrix(emptySet())))
        val queued = targets(vm).filter { it.attachment.transfer!!.phase == AttachmentTransferPhase.Queued }
        assertTrue(queued.size > 2)
        vm.holdDownloadTransfers(false)
        action(vm, queued[0], "advance"); action(vm, queued[1], "advance")
        assertEquals(AttachmentTransferPhase.Queued, current(vm, queued[1]).phase)
        repeat(4) { action(vm, queued[0], "advance") }
        action(vm, queued[1], "advance")
        assertEquals(AttachmentTransferPhase.Active, current(vm, queued[1]).phase)
    }
    @Test fun manualCancellationRemainsSuppressedUntilRestartInSameProfile() {
        val vm = setup(); allowAll(vm); val id = vm.uiState.activeProfileId!!
        val target = targets(vm).first { it.attachment.transfer!!.phase == AttachmentTransferPhase.Queued }
        action(vm, target, "cancel"); val cancelled = current(vm, target)
        vm.admitAutomaticDownloads(id); assertEquals(cancelled, current(vm, target))
        vm.pauseAutomaticDownloads(id, false); vm.admitAutomaticDownloads(id)
        assertEquals(AttachmentTransferPhase.Queued, current(vm, target).phase)
    }
    @Test fun wrongProfileCallbacksCannotStopOrAdvanceAnotherProfile() {
        val vm = setup(); val first = vm.uiState.activeProfileId!!; val target = targets(vm).first()
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        val second = vm.uiState.activeProfile!!
        vm.pauseAutomaticDownloads(first, true); vm.admitAutomaticDownloads(first)
        vm.attachmentTransferAction(first, target.chat, target.message, target.attachment.id, "advance", target.attachment.transfer!!.revision)
        assertEquals(second, vm.uiState.activeProfile)
        assertFalse(vm.uiState.profiles.first { it.id == first }.settings.automaticDownloadsPaused)
    }
    @Test fun unknownNetworkAdmitsNothingAndDisabledDeveloperGateDoesNotKeepHold() {
        val vm = setup(); val id = vm.uiState.activeProfileId!!; allowAll(vm)
        vm.chooseDownloadNetwork(DownloadNetworkExample.Unknown)
        val before = targets(vm); vm.admitAutomaticDownloads(id); assertEquals(before, targets(vm))
        vm.setDeveloperToolsEnabled(false)
        assertFalse(vm.downloadTransfersHeld); assertEquals(DownloadNetworkExample.Wifi, vm.downloadNetworkExample)
    }
    @Test fun globalQualityAffectsNextImportUntilExplicitDraftOverrideAndResetsAfterSend() {
        val vm = setup(); val chatId = vm.uiState.activeProfile!!.chats.first().id
        vm.updateProfileSettings(vm.uiState.activeProfile!!.settings.copy(sentMediaQuality = SentMediaQuality.Low))
        fun chat() = vm.uiState.activeProfile!!.chats.first { it.id == chatId }
        assertEquals(PhotoQuality.Low, chat().effectivePhotoQuality(vm.uiState.activeProfile!!.settings))
        assertTrue(vm.replaceDraftPhotos(vm.uiState.activeProfileId!!, chatId, chat().draftAttachments, PhotoQuality.Original, emptyList()))
        vm.updateProfileSettings(vm.uiState.activeProfile!!.settings.copy(sentMediaQuality = SentMediaQuality.Standard))
        assertEquals(PhotoQuality.Original, chat().effectivePhotoQuality(vm.uiState.activeProfile!!.settings))
        vm.updateDraftText(chatId, "Hello"); assertTrue(vm.sendDraft(chatId))
        assertFalse(chat().draftPhotoQualityExplicit)
        assertEquals(PhotoQuality.Standard, chat().effectivePhotoQuality(vm.uiState.activeProfile!!.settings))
    }
    @Test fun voiceSubmissionKeepsCapturedQualityWhenGlobalSelectionChanges() {
        val vm = setup(); val chatId = vm.uiState.activeProfile!!.chats.first().id
        val submission = VoiceDraftSubmission(VoiceMessageFormat.Voice, "", 5, SentMediaQuality.Low)
        vm.updateProfileSettings(vm.uiState.activeProfile!!.settings.copy(sentMediaQuality = SentMediaQuality.Original))
        assertTrue(vm.sendVoice(chatId, submission))
        val sent = vm.uiState.activeProfile!!.chats.first { it.id == chatId }.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
        assertEquals(SentMediaQuality.Low, sent.attachments.single().voiceQuality)
    }
    @Test fun staleSettingsSnapshotCannotUnpauseQueueOrOverwriteAnotherProfile() {
        val vm = setup(); val first = vm.uiState.activeProfile!!
        vm.pauseAutomaticDownloads(first.id, true)
        assertTrue(vm.updateDataUsageSettings(first.id, first.settings.copy(sentMediaQuality = SentMediaQuality.Low)))
        assertTrue(vm.uiState.activeProfile!!.settings.automaticDownloadsPaused)
        vm.completeSignIn(OnboardingOrigin.AddProfile); val second = vm.uiState.activeProfile!!
        assertFalse(vm.updateDataUsageSettings(first.id, first.settings))
        assertEquals(second, vm.uiState.activeProfile)
    }
    @Test fun failureRetryUsesSameAttachmentAndKeepsOtherQueueEntries() {
        val vm = setup(); vm.selectAttachmentTransferScenario(AttachmentTransferScenario.Failure); vm.loadDownloadQueueExample()
        vm.holdDownloadTransfers(false)
        val target = targets(vm).first { it.attachment.transfer!!.phase == AttachmentTransferPhase.Active }
        val peers = targets(vm).filter { it != target }
        repeat(3) { action(vm, target, "advance") }
        assertEquals(AttachmentTransferPhase.Failed, current(vm, target).phase)
        action(vm, target, "retry")
        repeat(5) { action(vm, target, "advance") }
        assertEquals(AttachmentTransferPhase.Available, current(vm, target).phase)
        peers.forEach { assertEquals(it.attachment.transfer, current(vm, it)) }
    }
}
