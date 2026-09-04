package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class PhotoEditorStateTest {
    private val chatId = "fiatjaf"
    private val album = MessageAttachment("album",MessageAttachmentKind.Photos,"Trip",images = listOf(ProfileAvatar.Asset(AvatarAsset.Fox),ProfileAvatar.Asset(AvatarAsset.Marmot)),photoQuality = PhotoQuality.High)
    private val frame = MessageAttachment("renderer-only",MessageAttachmentKind.Photo,"Untrusted label",images = listOf(ProfileAvatar.DeviceImage(byteArrayOf(1,2,3))),pixelWidth = 100,pixelHeight = 80,mimeType = "image/png",metadataPolicy = PhotoMetadataPolicy.Reencoded)
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); addDraftAttachments(chatId,listOf(album)) }
    private fun open(vm: AppViewModel,index: Int = 1): Long {
        assertTrue(vm.openPhotoEditor(vm.uiState.activeProfileId!!,chatId,"album",index)); val id = vm.photoEditorSession!!.id
        assertTrue(vm.photoEditorAction(id,PhotoEditorEvent.Loaded(0,4000,3000))); return id
    }
    @Test fun editAndCloseNeverMutateTheOrdinaryDraft() {
        val vm = model(); vm.updateDraftText(chatId,"Caption"); val before = vm.chat(chatId)!!
        val id = open(vm); vm.photoEditorAction(id,PhotoEditorEvent.Rotate); assertTrue(vm.photoEditorSession!!.dirty)
        vm.photoEditorAction(id,PhotoEditorEvent.Close); assertEquals(before,vm.chat(chatId)); assertNull(vm.photoEditorSession)
    }
    @Test fun saveCommitsOnlySelectedFrameAndKeepsOriginalsCaptionReplyAndSiblingQuality() {
        val vm = model(); vm.updateDraftText(chatId,"Caption"); val before = vm.chat(chatId)!!; val id = open(vm)
        vm.photoEditorAction(id,PhotoEditorEvent.Rotate); vm.photoEditorAction(id,PhotoEditorEvent.SelectQuality(PhotoQuality.Low)); vm.photoEditorAction(id,PhotoEditorEvent.Save)
        assertTrue(vm.photoEditorAction(id,PhotoEditorEvent.Saved(vm.photoEditorSession!!.revision,frame)))
        val chat = vm.chat(chatId)!!; val saved = chat.draftAttachments.single()
        assertEquals(album.id,saved.id); assertEquals(album.label,saved.label); assertEquals(album.kind,saved.kind)
        assertEquals(album.images[0],saved.images[0]); assertEquals(frame.images[0],saved.images[1]); assertEquals(album.images,saved.sourceImages)
        assertEquals(PhotoQuality.High,saved.photoQuality); assertEquals(mapOf(1 to PhotoQuality.Low),saved.photoFrameQualities)
        assertEquals(1,saved.photoEdits[1]!!.quarterTurns); assertEquals(before.draftText,chat.draftText); assertEquals(before.draftReplyMessageId,chat.draftReplyMessageId)
        assertEquals(before.draftPhotoQuality,chat.draftPhotoQuality); assertNull(vm.photoEditorSession)
    }
    @Test fun staleLoadSaveDuplicateSaveAndCloseDuringSaveCannotChangeTheDraft() {
        val vm = model(); val id = open(vm); val old = vm.photoEditorSession!!.revision
        assertFalse(vm.photoEditorAction(id,PhotoEditorEvent.Loaded(0,10,10)))
        vm.photoEditorAction(id,PhotoEditorEvent.Rotate); vm.photoEditorAction(id,PhotoEditorEvent.Save)
        assertFalse(vm.photoEditorAction(id,PhotoEditorEvent.Save)); assertFalse(vm.photoEditorAction(id,PhotoEditorEvent.Close))
        assertFalse(vm.photoEditorAction(id,PhotoEditorEvent.Saved(old,frame))); assertEquals(album,vm.chat(chatId)!!.draftAttachments.single())
        val revision = vm.photoEditorSession!!.revision; assertTrue(vm.photoEditorAction(id,PhotoEditorEvent.Saved(revision,frame)))
        assertFalse(vm.photoEditorAction(id,PhotoEditorEvent.Saved(revision,frame)))
    }
    @Test fun failureRetainsRecipeAndRetryHasANewRevision() {
        val vm = model(); val id = open(vm); vm.photoEditorAction(id,PhotoEditorEvent.Rotate); vm.photoEditorAction(id,PhotoEditorEvent.Save)
        val revision = vm.photoEditorSession!!.revision
        vm.photoEditorAction(id,PhotoEditorEvent.Saved(revision,null,PhotoEditorFailure.SaveFailed))
        assertEquals(PhotoEditorPhase.Failed,vm.photoEditorSession!!.phase); assertTrue(vm.photoEditorSession!!.dirty)
        assertEquals(album,vm.chat(chatId)!!.draftAttachments.single()); assertTrue(vm.photoEditorAction(id,PhotoEditorEvent.Retry))
        assertTrue(vm.photoEditorSession!!.revision > revision); assertFalse(vm.photoEditorAction(id,PhotoEditorEvent.Saved(revision,frame)))
        assertTrue(vm.photoEditorAction(id,PhotoEditorEvent.Saved(vm.photoEditorSession!!.revision,frame)))
    }
    @Test fun deletedAttachmentAndChangedQualityRejectSaveCompletion() {
        for (remove in listOf(true,false)) {
            val vm = model(); val id = open(vm); vm.photoEditorAction(id,PhotoEditorEvent.Save); val revision = vm.photoEditorSession!!.revision
            if (remove) vm.removeDraftAttachment(chatId,album.id) else {
                val expected = vm.chat(chatId)!!.draftAttachments
                vm.replaceDraftPhotos(vm.uiState.activeProfileId!!,chatId,expected,PhotoQuality.Low,expected.map { it.copy(photoQuality = PhotoQuality.Low) })
            }
            assertFalse(vm.photoEditorAction(id,PhotoEditorEvent.Saved(revision,frame)))
            assertEquals(PhotoEditorFailure.SourceChanged,vm.photoEditorSession!!.failure); assertFalse(vm.photoEditorAction(id,PhotoEditorEvent.Retry))
            if (remove) assertTrue(vm.chat(chatId)!!.draftAttachments.isEmpty()) else assertEquals(album.images,vm.chat(chatId)!!.draftAttachments.single().images)
        }
    }
    @Test fun profileRoundTripInvalidatesSessionAndLateCallbacks() {
        val vm = model(); val profile = vm.uiState.activeProfileId!!; val id = open(vm)
        vm.photoEditorAction(id,PhotoEditorEvent.Save); val revision = vm.photoEditorSession!!.revision
        vm.completeSignIn(OnboardingOrigin.AddProfile); vm.selectProfile(profile)
        assertNull(vm.photoEditorSession); assertFalse(vm.photoEditorAction(id,PhotoEditorEvent.Saved(revision,frame)))
        assertEquals(album,vm.chat(chatId)!!.draftAttachments.single()); assertTrue(open(vm) > id)
    }
    @Test fun reopenedEditorUsesOriginalAndSavedRecipeAndResetRestoresOpeningQuality() {
        val vm = model(); val id = open(vm); vm.photoEditorAction(id,PhotoEditorEvent.Rotate); vm.photoEditorAction(id,PhotoEditorEvent.Save)
        vm.photoEditorAction(id,PhotoEditorEvent.Saved(vm.photoEditorSession!!.revision,frame))
        val reopened = open(vm); assertEquals(album.images[1],vm.photoEditorSession!!.source); assertFalse(vm.photoEditorSession!!.dirty)
        vm.photoEditorAction(reopened,PhotoEditorEvent.SelectQuality(PhotoQuality.Original)); assertTrue(vm.photoEditorSession!!.dirty)
        vm.photoEditorAction(reopened,PhotoEditorEvent.Rotate); vm.photoEditorAction(reopened,PhotoEditorEvent.Reset)
        assertEquals(1,vm.photoEditorSession!!.history.current.quarterTurns); assertEquals(PhotoQuality.High,vm.photoEditorSession!!.requestedQuality); assertFalse(vm.photoEditorSession!!.dirty)
    }
    @Test fun invalidSourceAndFrameCannotBecomeReadyOrOverwriteDraft() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        assertFalse(vm.openPhotoEditor(owner,chatId,"album",2)); assertFalse(vm.openPhotoEditor("other",chatId,"album",0))
        assertTrue(vm.openPhotoEditor(owner,chatId,"album",0)); val id = vm.photoEditorSession!!.id
        vm.photoEditorAction(id,PhotoEditorEvent.Loaded(0,Int.MAX_VALUE,1)); assertEquals(PhotoEditorFailure.InvalidSource,vm.photoEditorSession!!.failure)
        assertFalse(vm.photoEditorAction(id,PhotoEditorEvent.Save)); vm.photoEditorAction(id,PhotoEditorEvent.Retry)
        vm.photoEditorAction(id,PhotoEditorEvent.Loaded(vm.photoEditorSession!!.revision,100,100)); vm.photoEditorAction(id,PhotoEditorEvent.Save)
        vm.photoEditorAction(id,PhotoEditorEvent.Saved(vm.photoEditorSession!!.revision,frame.copy(images = emptyList())))
        assertEquals(PhotoEditorFailure.SaveFailed,vm.photoEditorSession!!.failure); assertEquals(album,vm.chat(chatId)!!.draftAttachments.single())
    }
    @Test fun sendingDropsOriginalsRecipesAndFrameOverrides() {
        val vm = model(); val id = open(vm); vm.photoEditorAction(id,PhotoEditorEvent.Rotate); vm.photoEditorAction(id,PhotoEditorEvent.Save)
        vm.photoEditorAction(id,PhotoEditorEvent.Saved(vm.photoEditorSession!!.revision,frame)); assertTrue(vm.sendDraft(chatId))
        val sent = vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message.attachments.single()
        assertTrue(sent.sourceImages.isEmpty()); assertTrue(sent.photoEdits.isEmpty()); assertTrue(sent.photoFrameQualities.isEmpty()); assertEquals(frame.images[0],sent.images[1])
    }
}
