package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class LocationSharingStateTest {
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
    private fun begin(vm: AppViewModel): Long {
        assertTrue(vm.openLocation(vm.uiState.activeProfileId!!, "fiatjaf"))
        val id = vm.locationSession!!.id
        assertTrue(vm.locationAction(id, LocationEvent.Latitude("45.25")))
        assertTrue(vm.locationAction(id, LocationEvent.Longitude("19.84")))
        assertTrue(vm.locationAction(id, LocationEvent.Review))
        return id
    }
    private fun complete(vm: AppViewModel, id: Long) {
        assertTrue(vm.locationAction(id, LocationEvent.Send))
        assertTrue(vm.locationAction(id, LocationEvent.Sent(vm.locationSession!!.revision)))
    }
    @Test fun sendingPreservesTheDraftAndConsumesItsUnchangedReplyOnce() {
        val vm = model()
        vm.updateDraftText("fiatjaf", "My unsent caption")
        vm.addDraftAttachments("fiatjaf", listOf(MessageAttachment("file", MessageAttachmentKind.File, "Notes.txt")))
        vm.suppressDraftLink("fiatjaf", "https://example.org")
        val reply = vm.chat("fiatjaf")!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().first().message
        assertTrue(vm.setDraftReply("fiatjaf", reply.id))
        val before = vm.chat("fiatjaf")!!
        val id = begin(vm); complete(vm, id)
        val after = vm.chat("fiatjaf")!!
        assertEquals(before.timeline.size + 1, after.timeline.size)
        assertEquals(before.draftText, after.draftText); assertEquals(before.draftAttachments, after.draftAttachments)
        assertEquals(before.suppressedDraftLinkUrl, after.suppressedDraftLinkUrl)
        assertTrue(after.isDraft); assertNull(after.draftReplyMessageId)
        val sent = (after.timeline.last() as ChatTimelineEntry.Message).message
        assertEquals(reply.id, sent.replyToMessageId)
        assertEquals(SharedLocation(45.25, 19.84), LocationSharing.fromMessage(sent))
        assertFalse(vm.locationAction(id, LocationEvent.Sent(99))); assertNull(vm.locationSession)
    }
    @Test fun cancellationPreservesTheEntireDraftAndPreventsLateCompletion() {
        val vm = model(); vm.updateDraftText("fiatjaf", "Keep me")
        val before = vm.chat("fiatjaf")!!; val id = begin(vm)
        vm.locationAction(id, LocationEvent.Send); val revision = vm.locationSession!!.revision
        assertTrue(vm.locationAction(id, LocationEvent.Close))
        assertFalse(vm.locationAction(id, LocationEvent.Sent(revision)))
        assertEquals(before, vm.chat("fiatjaf"))
    }
    @Test fun profileSwitchAndReplacedSessionInvalidateOldEvents() {
        val vm = model(); val id = begin(vm); val profile = vm.uiState.activeProfileId!!
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertNull(vm.locationSession); assertFalse(vm.locationAction(id, LocationEvent.Send))
        assertFalse(vm.openLocation(profile, "fiatjaf"))
        vm.selectProfile(profile); val replacement = begin(vm)
        assertFalse(vm.locationAction(id, LocationEvent.Close)); assertEquals(replacement, vm.locationSession!!.id)
    }
    @Test fun lostChatEligibilityAndChangedReplyCannotSend() {
        val vm = model(); val id = begin(vm)
        val reply = vm.chat("fiatjaf")!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().first().message
        vm.setDraftReply("fiatjaf", reply.id)
        assertFalse(vm.locationAction(id, LocationEvent.Send))
        assertEquals(LocationFailure.SourceChanged, vm.locationSession!!.failure)
        vm.locationAction(id, LocationEvent.Close)
        val next = begin(vm); vm.toggleBlocked("fiatjaf")
        assertFalse(vm.locationAction(next, LocationEvent.Send))
    }
    @Test fun failureRetryAppendsOnlyAfterAcceptanceAndKeepsDraft() {
        val vm = model(); vm.setDeveloperToolsEnabled(true); vm.selectLocationScenario(LocationScenario.SendFailure)
        val before = vm.chat("fiatjaf")!!; val id = begin(vm)
        complete(vm, id)
        assertEquals(LocationFailure.SendFailed, vm.locationSession!!.failure)
        assertEquals(before, vm.chat("fiatjaf"))
        complete(vm, id)
        assertNull(vm.locationSession); assertEquals(before.timeline.size + 1, vm.chat("fiatjaf")!!.timeline.size)
    }
    @Test fun scenariosRequireTheDeveloperOwnerAndAreConsumedOnce() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        vm.selectLocationScenario(LocationScenario.Precise); assertEquals(LocationScenario.Unavailable, vm.nextLocationScenario)
        vm.setDeveloperToolsEnabled(true); vm.selectLocationScenario(LocationScenario.Precise)
        vm.completeSignIn(OnboardingOrigin.AddProfile); assertEquals(LocationScenario.Unavailable, vm.nextLocationScenario)
        vm.selectProfile(owner); begin(vm)
        assertEquals(LocationScenario.Precise, vm.locationSession!!.scenario)
        assertEquals(LocationScenario.Unavailable, vm.nextLocationScenario)
    }
    @Test fun pastedLocationBodyAvoidsADuplicateGenericLinkPreview() {
        val vm = model(); vm.updateDraftText("fiatjaf", SharedLocation(0.0, 0.0).messageText)
        assertTrue(vm.sendDraft("fiatjaf"))
        val sent = (vm.chat("fiatjaf")!!.timeline.last() as ChatTimelineEntry.Message).message
        assertTrue(sent.attachments.isEmpty()); assertNotNull(LocationSharing.fromMessage(sent))
        vm.updateDraftText("fiatjaf", "Meet here: ${SharedLocation(0.0, 0.0).mapsLink}")
        assertTrue(vm.sendDraft("fiatjaf"))
        val prose = (vm.chat("fiatjaf")!!.timeline.last() as ChatTimelineEntry.Message).message
        assertTrue(prose.attachments.isNotEmpty()); assertNull(LocationSharing.fromMessage(prose))
    }
    @Test fun turningOffDeveloperToolsPreventsAQueuedCurrentLocationExample() {
        val vm = model(); vm.setDeveloperToolsEnabled(true)
        vm.selectLocationScenario(LocationScenario.Precise); vm.setDeveloperToolsEnabled(false)
        begin(vm)
        assertEquals(LocationScenario.Unavailable, vm.locationSession!!.scenario)
    }

}
