package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class AttachmentReadingStateTest {
    @Test fun examplesRequireTheActiveDeveloperProfileAndPreserveOrdinaryDrafts() {
        val vm=AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) };val owner=vm.uiState.activeProfileId!!
        val before=vm.chat("fiatjaf")!!;assertFalse(vm.addAttachmentReadingExamples(owner,"fiatjaf"))
        vm.setDeveloperToolsEnabled(true);vm.updateDraftText("fiatjaf","Unsent draft")
        assertFalse(vm.addAttachmentReadingExamples("other","fiatjaf"));assertTrue(vm.addAttachmentReadingExamples(owner,"fiatjaf"))
        val after=vm.chat("fiatjaf")!!;assertEquals("Unsent draft",after.draftText)
        assertEquals(before.timeline.size+AttachmentLocalSource.entries.size,after.timeline.size)
        val ids=after.timeline.filterIsInstance<ChatTimelineEntry.Message>().map { it.message.id };assertEquals(ids.size,ids.distinct().size)
    }
    @Test fun outcomeSelectionIsOwnedByTheSelectingProfileAndConsumedOnce() {
        val vm=AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) };val owner=vm.uiState.activeProfileId!!
        vm.selectAttachmentAccessScenario(AttachmentAccessScenario.LoadFailure)
        assertEquals(AttachmentAccessScenario.Success,vm.consumeAttachmentAccessScenario(owner))
        vm.setDeveloperToolsEnabled(true);vm.selectAttachmentAccessScenario(AttachmentAccessScenario.PackagePermission)
        vm.completeSignIn(OnboardingOrigin.AddProfile);val other=vm.uiState.activeProfileId!!
        assertEquals(AttachmentAccessScenario.Success,vm.consumeAttachmentAccessScenario(other))
        vm.selectProfile(owner);assertEquals(AttachmentAccessScenario.PackagePermission,vm.consumeAttachmentAccessScenario(owner))
        assertEquals(AttachmentAccessScenario.Success,vm.consumeAttachmentAccessScenario(owner))
    }
}
