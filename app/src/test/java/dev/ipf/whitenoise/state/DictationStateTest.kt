package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class DictationStateTest {
    private val chatId = "fiatjaf"
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
    private fun start(vm: AppViewModel, send: Boolean = false): ComposerCaptureOwner {
        val owner = ComposerCaptureOwner(vm.uiState.activeProfileId!!, chatId)
        val capture = vm.composerCapture
        capture.changePreferences(owner.profileId) {
            it.copy(disclosureAccepted = true, delivery = if (send) DictationDeliveryMode.Send else DictationDeliveryMode.Paste)
        }
        capture.open(owner)
        val text = vm.chat(chatId)!!.draftText
        assertTrue(capture.begin(owner, text, text.length, text.length))
        repeat(16) { advance(vm, owner) }
        return owner
    }
    private fun advance(vm: AppViewModel, owner: ComposerCaptureOwner) {
        vm.composerCapture.attempts[owner]!!.let { vm.composerCapture.advance(owner, it.id, it.revision) }
    }
    private fun finish(vm: AppViewModel, owner: ComposerCaptureOwner) {
        vm.composerCapture.finish(owner, vm.composerCapture.attempts[owner]!!.id)
        advance(vm, owner)
    }
    @Test fun optedInSendUsesExistingDraftSubmissionIncludingAttachmentAndReplyExactlyOnce() {
        val vm = model()
        val reply = vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().first { !it.message.isDeleted }.message.id
        vm.updateDraftText(chatId, "Caption")
        vm.addDraftAttachments(chatId, listOf(MessageAttachment("photo", MessageAttachmentKind.Photo, "Photo", images = listOf(ProfileAvatar.Asset(AvatarAsset.Fox)))))
        assertTrue(vm.setDraftReply(chatId, reply))
        val before = vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().size
        val owner = start(vm, send = true)
        finish(vm, owner)
        val chat = vm.chat(chatId)!!
        val sent = chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
        assertEquals("Caption ${DictationExamples.transcript}", sent.text)
        assertEquals("photo", sent.attachments.single().id)
        assertEquals(reply, sent.replyToMessageId)
        assertEquals(DictationPhase.Complete, vm.composerCapture.attempts[owner]!!.phase)
        assertEquals("", chat.draftText); assertTrue(chat.draftAttachments.isEmpty()); assertNull(chat.draftReplyMessageId)
        finish(vm, owner)
        assertEquals(before + 1, vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().size)
    }
    @Test fun appDraftMutationsTrackEditAndRevertThenExplicitInsertKeepsLatestPayload() {
        val vm = model(); vm.updateDraftText(chatId, "Original")
        val owner = start(vm, send = true)
        vm.updateDraftText(chatId, "Edited"); vm.updateDraftText(chatId, "Original")
        finish(vm, owner)
        val review = vm.composerCapture.attempts[owner]!!
        assertEquals(DictationPhase.Review, review.phase)
        vm.updateDraftText(chatId, "Latest")
        assertTrue(vm.composerCapture.insertAtEnd(owner, review.id))
        assertEquals("Latest ${DictationExamples.transcript}", vm.chat(chatId)!!.draftText)
    }
    @Test fun appProfileExitClearsReviewWithoutDiscardingRetainedOrdinaryDraft() {
        val vm = model(); vm.updateDraftText(chatId, "Keep my draft")
        val owner = start(vm)
        vm.composerCapture.background()
        assertEquals(DictationPhase.Review, vm.composerCapture.attempts[owner]!!.phase)
        vm.signOutActiveProfile(wipeData = false)
        assertTrue(vm.composerCapture.attempts.isEmpty()); assertNull(vm.composerCapture.lease)
        assertEquals("Keep my draft", vm.uiState.retainedProfiles.single().chats.first { it.id == chatId }.draftText)
        assertFalse(vm.composerCapture.insertAtEnd(owner, 1))
    }
}
