package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class AttachmentStateTest {
    private val chatId = "fiatjaf"
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
    private fun photo(id: String = "photo") = MessageAttachment(id, MessageAttachmentKind.Photo, "Photo", images = listOf(ProfileAvatar.Asset(AvatarAsset.Fox)))
    @Test fun qualityChangePreservesDraftAndRejectsRemovedAttachmentCompletion() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        vm.updateDraftText(chatId, "Caption"); vm.addDraftAttachments(chatId, listOf(photo()))
        val expected = vm.chat(chatId)!!.draftAttachments
        assertTrue(vm.replaceDraftPhotos(owner, chatId, expected, PhotoQuality.Low, expected.map { it.copy(photoQuality = PhotoQuality.Low, sourceImages = it.images) }))
        assertEquals("Caption", vm.chat(chatId)!!.draftText); assertEquals(PhotoQuality.Low, vm.chat(chatId)!!.draftPhotoQuality)
        val old = vm.chat(chatId)!!.draftAttachments
        vm.removeDraftAttachment(chatId, "photo")
        assertFalse(vm.replaceDraftPhotos(owner, chatId, old, PhotoQuality.High, expected))
        assertTrue(vm.chat(chatId)!!.draftAttachments.isEmpty())
    }
    @Test fun completionCannotWriteToAnotherProfileWithTheSameChatId() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        vm.addDraftAttachments(chatId, listOf(photo())); val expected = vm.chat(chatId)!!.draftAttachments
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertFalse(vm.replaceDraftPhotos(owner, chatId, expected, PhotoQuality.Low, expected))
        assertEquals(expected, vm.uiState.profiles.first { it.id == owner }.chats.first { it.id == chatId }.draftAttachments)
    }
    @Test fun sendDropsRetainedSourceAndQueuesUploadWithStableAttachmentId() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        vm.addDraftAttachments(chatId, listOf(photo().copy(sourceImages = listOf(ProfileAvatar.DeviceImage(byteArrayOf(1,2,3))))))
        assertTrue(vm.sendDraft(chatId))
        val message = vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
        val sent = message.attachments.single(); assertTrue(sent.sourceImages.isEmpty()); assertEquals("photo", sent.id)
        assertEquals(AttachmentTransferDirection.Upload, sent.transfer!!.direction)
        val original = sent.transfer
        vm.attachmentTransferAction(owner, chatId, message.id, sent.id, "cancel", original.revision)
        vm.attachmentTransferAction(owner, chatId, message.id, sent.id, "advance", original.revision)
        assertEquals(AttachmentTransferPhase.Cancelled, vm.message(chatId, message.id)!!.attachments.single().transfer!!.phase)
        vm.attachmentTransferAction(owner, chatId, message.id, sent.id, "retry", original.revision + 1)
        repeat(5) { val state = vm.message(chatId, message.id)!!.attachments.single().transfer!!; vm.attachmentTransferAction(owner, chatId, message.id, sent.id, "advance", state.revision) }
        val completed = vm.message(chatId, message.id)!!.attachments.single()
        assertEquals(sent.id, completed.id); assertEquals(AttachmentTransferPhase.Available, completed.transfer!!.phase)
    }
    @Test fun deletionCannotBeUndoneByTransferCompletion() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        vm.addDraftAttachments(chatId, listOf(photo())); vm.sendDraft(chatId)
        val message = vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
        vm.deleteMessages(chatId, setOf(message.id), MessageDeletionScope.ForEveryone)
        vm.attachmentTransferAction(owner, chatId, message.id, "photo", "advance", 0)
        assertTrue(vm.message(chatId, message.id)!!.isDeleted); assertTrue(vm.message(chatId, message.id)!!.attachments.isEmpty())
    }
    @Test fun deviceContactQueuesSelectedFieldsInCardAndTextWithoutChangingOrdinaryCaption() {
        val vm = model(); val selected = SharedDeviceContact("Ada", "123", "private@example.com").selected(true, true, false)
        vm.updateDraftText(chatId,"Contact for the trip"); vm.addDraftAttachments(chatId, listOf(selected.attachment("contact")!!))
        assertTrue(vm.sendDraft(chatId))
        val sent = vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
        assertEquals("Contact for the trip\n\nAda\n123", sent.text)
        assertFalse(sent.text.contains("private@")); assertNull(sent.attachments.single().contactPersonId)
    }
    @Test fun perDraftQualityDoesNotChangeAnotherChatOrAvatar() {
        val vm=model();val profile=vm.uiState.activeProfile!!;val other=profile.chats.first { it.id != chatId }
        assertTrue(vm.replaceDraftPhotos(profile.id,chatId, emptyList(), PhotoQuality.Original, emptyList()))
        assertEquals(other.draftPhotoQuality,vm.chat(other.id)!!.draftPhotoQuality)
        assertEquals(profile.avatar, vm.uiState.activeProfile!!.avatar)
    }
    @Test fun sessionRoundTripInvalidatesOldTransferCompletion() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        vm.addDraftAttachments(chatId,listOf(photo())); vm.sendDraft(chatId)
        val message = vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
        vm.completeSignIn(OnboardingOrigin.AddProfile); vm.selectProfile(owner)
        vm.attachmentTransferAction(owner,chatId,message.id,"photo","advance",0)
        val state=vm.message(chatId,message.id)!!.attachments.single().transfer!!
        assertEquals(AttachmentTransferPhase.Cancelled,state.phase); assertEquals(1L,state.revision)
        vm.attachmentTransferAction(owner,chatId,message.id,"photo","retry",state.revision)
        assertTrue(vm.message(chatId,message.id)!!.attachments.single().transfer!!.running)
    }

}
