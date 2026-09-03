package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class ProfileEditorStateTest {
    private fun signedIn() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun advance(vm: AppViewModel) = vm.profileSaveAttempt!!.let { vm.advanceProfileSave(it.id, it.phase) }
    private fun draft(vm: AppViewModel) = ProfileEditDraft.from(vm.uiState.activeProfile!!).copy(name = "Updated profile", lightningAddress = "friend@payments.example", banner = ProfileAvatar.Asset(AvatarAsset.GardenClub))

    @Test fun allFieldsApplyTogetherAfterLightningCheckAndPublishing() {
        val vm = signedIn(); val before = vm.uiState.activeProfile!!; val draft = draft(vm)
        assertTrue(vm.beginProfileSave(before.id, draft))
        assertEquals(ProfileSavePhase.CheckingLightning, vm.profileSaveAttempt!!.phase)
        assertEquals(before, vm.uiState.activeProfile)
        assertFalse(advance(vm))
        assertEquals(ProfileSavePhase.Publishing, vm.profileSaveAttempt!!.phase)
        assertEquals(before, vm.uiState.activeProfile)
        assertTrue(advance(vm))
        assertEquals(draft, ProfileEditDraft.from(vm.uiState.activeProfile!!))
        assertNull(vm.profileSaveAttempt)
    }
    @Test fun checkAndPublishFailuresNeverPartiallySaveOtherFields() {
        for (scenario in listOf(ProfileSaveScenario.UnresolvedLightning, ProfileSaveScenario.NoConnection, ProfileSaveScenario.PublishFailure)) {
            val vm = signedIn(); val before = vm.uiState.activeProfile!!
            vm.selectProfileSaveScenario(scenario)
            vm.beginProfileSave(before.id, draft(vm))
            advance(vm)
            if (vm.profileSaveAttempt!!.isBusy) advance(vm)
            assertEquals(scenario.toString(), before, vm.uiState.activeProfile)
            assertEquals(ProfileSavePhase.Failed, vm.profileSaveAttempt!!.phase)
        }
    }
    @Test fun retryUsesCorrectedDraftAndAOneShotOutcome() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!
        vm.selectProfileSaveScenario(ProfileSaveScenario.UnresolvedLightning)
        vm.beginProfileSave(owner, draft(vm)); advance(vm)
        val failedId = vm.profileSaveAttempt!!.id
        val corrected = draft(vm).copy(lightningAddress = "corrected@payments.example")
        assertTrue(vm.beginProfileSave(owner, corrected))
        assertFalse(vm.advanceProfileSave(failedId, ProfileSavePhase.CheckingLightning))
        advance(vm); assertTrue(advance(vm))
        assertEquals(corrected.lightningAddress, vm.uiState.activeProfile!!.lightningAddress)
    }
    @Test fun cancelAndDuplicateCallbacksCannotPublish() {
        val vm = signedIn(); val before = vm.uiState.activeProfile!!
        vm.beginProfileSave(before.id, draft(vm)); val attempt = vm.profileSaveAttempt!!
        assertFalse(vm.beginProfileSave(before.id, draft(vm).copy(name = "Duplicate")))
        assertFalse(vm.advanceProfileSave(attempt.id, ProfileSavePhase.Publishing))
        vm.cancelProfileSave(before.id)
        assertFalse(vm.advanceProfileSave(attempt.id, attempt.phase))
        assertEquals(before, vm.uiState.activeProfile)
    }
    @Test fun ownerChangeDropsPendingSaveAndImageDraft() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!
        val edited = draft(vm)
        vm.retainProfileImages(owner, edited.avatar, edited.banner)
        vm.beginProfileSave(owner, edited); val request = vm.profileSaveAttempt!!
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertNull(vm.profileSaveAttempt); assertNull(vm.profileImageDraft)
        assertFalse(vm.advanceProfileSave(request.id, request.phase))
        assertFalse(vm.beginProfileSave(owner, edited))
        vm.retainProfileImages(owner, edited.avatar, edited.banner)
        assertNull(vm.profileImageDraft)
    }
    @Test fun clearingLightningAndBannerSkipsLookupAndPreservesOtherProfileData() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!
        vm.beginProfileSave(owner, draft(vm)); advance(vm); advance(vm)
        val before = vm.uiState.activeProfile!!
        vm.beginProfileSave(owner, ProfileEditDraft.from(before).copy(lightningAddress = "", banner = null))
        assertEquals(ProfileSavePhase.Publishing, vm.profileSaveAttempt!!.phase)
        assertTrue(advance(vm))
        assertEquals("", vm.uiState.activeProfile!!.lightningAddress)
        assertNull(vm.uiState.activeProfile!!.banner)
        assertEquals(before.chats, vm.uiState.activeProfile!!.chats)
        assertEquals(before.people, vm.uiState.activeProfile!!.people)
    }
    @Test fun imageFailureIsConsumedOnceAndRetainedRemovalIsExplicit() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!
        vm.selectProfileImageFailure(true)
        assertTrue(vm.consumeProfileImageFailure(owner)); assertFalse(vm.consumeProfileImageFailure(owner))
        vm.retainProfileImages(owner, ProfileAvatar.Monogram, null)
        assertNotNull(vm.profileImageDraft)
        assertNull(vm.profileImageDraft!!.banner)
        vm.cancelProfileSave(owner)
        assertNull(vm.profileImageDraft)
    }
    @Test fun invalidDraftIsRejectedBeforeAnyAttempt() {
        val vm = signedIn(); val before = vm.uiState.activeProfile!!
        assertFalse(vm.beginProfileSave(before.id, draft(vm).copy(lightningAddress = "invalid")))
        assertNull(vm.profileSaveAttempt); assertEquals(before, vm.uiState.activeProfile)
    }
}
