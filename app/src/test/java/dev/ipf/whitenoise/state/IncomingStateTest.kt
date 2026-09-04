package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class IncomingStateTest {
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun group(vm: AppViewModel) = vm.createGroup("Trail", "", ProfileAvatar.Monogram, emptyList())!!
    private fun step(vm: AppViewModel) { vm.incoming.work!!.let { vm.incoming.advance(it.id,it.phase,it.attempt) } }
    private fun share(vm: AppViewModel, text: String = "Shared text") : Long {
        val id = vm.incoming.receive(IncomingEntry.Share(IncomingPayload(text)))!!; step(vm); return id
    }
    private fun stage(vm: AppViewModel, id: Long, vararg chats: String) {
        chats.forEach { vm.incoming.toggle(id,it) }; assertTrue(vm.incoming.submit(id)); step(vm)
    }
    @Test fun selectedDestinationsGetMergedDraftsAndNothingIsSent() {
        val vm=model(); val a=group(vm); val b=group(vm); vm.updateDraftText(a,"Existing ")
        val before=vm.chat(a)!!.timeline; val id=share(vm,"  New text  "); stage(vm,id,a,b)
        assertEquals("Existing\nNew text",vm.chat(a)!!.draftText); assertEquals("New text",vm.chat(b)!!.draftText)
        assertEquals(before,vm.chat(a)!!.timeline); assertEquals(IncomingPhase.Opening,vm.incoming.work!!.phase)
        val open=vm.incoming.opening(id)!!; assertEquals(a,open.target!!.chatId); assertEquals(1,open.otherChats)
        vm.incoming.opened(id,true); assertEquals(IncomingPhase.Complete,vm.incoming.work!!.phase)
    }
    @Test fun selectingAnotherProfileClearsChatSelectionAndDoesNotActivateIt() {
        val vm=model(); val original=vm.uiState.activeProfileId!!; val a=group(vm)
        vm.completeSignIn(OnboardingOrigin.AddProfile); val other=vm.uiState.activeProfileId!!; val b=group(vm); vm.selectProfile(original)
        val id=share(vm); vm.incoming.toggle(id,a); assertTrue(vm.incoming.chooseProfile(id,other))
        assertTrue(vm.incoming.work!!.selectedChatIds.isEmpty()); assertEquals(original,vm.uiState.activeProfileId)
        stage(vm,id,b); assertTrue(vm.chat(a)!!.draftText.isEmpty())
        assertEquals("Shared text",vm.uiState.profiles.first { it.id==other }.chats.first { it.id==b }.draftText)
        val open=vm.incoming.opening(id)!!; assertEquals(other,open.profileId)
        vm.selectProfile(other); vm.incoming.opened(id,true); assertEquals(IncomingPhase.Complete,vm.incoming.work!!.phase)
    }
    @Test fun archivedChatsCanReceiveDraftsAndUnavailableChatsCannot() {
        val vm=model(); val a=group(vm); vm.setChatArchived(a,true)
        assertTrue(vm.incoming.targets(vm.uiState.activeProfileId!!).any { it.id==a })
        val id=share(vm); vm.incoming.toggle(id,"does-not-exist"); assertFalse(vm.incoming.submit(id))
        stage(vm,id,a); assertTrue(vm.chat(a)!!.isArchived)
    }
    @Test fun cancelBeforeCommitPreservesDraftAndStaleCallbackCannotApply() {
        val vm=model(); val a=group(vm); val id=share(vm); vm.incoming.toggle(id,a); vm.incoming.submit(id); val w=vm.incoming.work!!
        vm.incoming.cancel(id); vm.incoming.advance(w.id,w.phase,w.attempt)
        assertTrue(vm.chat(a)!!.draftText.isEmpty()); assertNull(vm.incoming.work)
    }
    @Test fun equalReplacementRequestHasNewOwnershipAndOldCompletionIsIgnored() {
        val vm=model(); val a=group(vm); val first=share(vm); vm.incoming.toggle(first,a); vm.incoming.submit(first); val old=vm.incoming.work!!
        val second=share(vm); assertNotEquals(first,second); vm.incoming.advance(old.id,old.phase,old.attempt)
        assertTrue(vm.chat(a)!!.draftText.isEmpty()); stage(vm,second,a); assertEquals("Shared text",vm.chat(a)!!.draftText)
    }
    @Test fun launcherReentryPreservesThePendingRequest() {
        val vm=model(); val id=share(vm); val work=vm.incoming.work
        assertEquals(id,vm.incoming.receive(null)); assertEquals(work,vm.incoming.work)
    }
    @Test fun duplicateStagingAndOpeningCallbacksCannotConsumeTwice() {
        val vm=model(); val a=group(vm); val id=share(vm); vm.incoming.toggle(id,a); vm.incoming.submit(id); val w=vm.incoming.work!!
        step(vm); vm.incoming.advance(w.id,w.phase,w.attempt)
        assertEquals("Shared text",vm.chat(a)!!.draftText); assertNotNull(vm.incoming.opening(id)); assertNull(vm.incoming.opening(id))
        vm.incoming.opened(id,true); assertFalse(vm.incoming.retry(id))
    }
    @Test fun preparationAndApplyFailureRetryPreserveTheCorrectStage() {
        for (scenario in listOf(IncomingScenario.PreparationFailure,IncomingScenario.ApplyFailure)) {
            val vm=model(); val a=group(vm); vm.incoming.choose(scenario)
            val id=share(vm)
            if (scenario==IncomingScenario.ApplyFailure) stage(vm,id,a)
            assertEquals(IncomingPhase.Failed,vm.incoming.work!!.phase); assertTrue(vm.chat(a)!!.draftText.isEmpty())
            assertTrue(vm.incoming.retry(id)); if (vm.incoming.work!!.phase==IncomingPhase.Preparing) step(vm)
            if (a !in vm.incoming.work!!.selectedChatIds) vm.incoming.toggle(id,a)
            assertTrue(vm.incoming.submit(id)); step(vm); assertEquals("Shared text",vm.chat(a)!!.draftText)
        }
    }
    @Test fun failedOpeningRetainsAcceptedDraftsAndRetryNeverStagesAgain() {
        val vm=model(); val a=group(vm); vm.incoming.choose(IncomingScenario.OpenFailure); val id=share(vm); stage(vm,id,a)
        assertNull(vm.incoming.opening(id)); assertNotNull(vm.incoming.work!!.committed)
        assertTrue(vm.incoming.retry(id)); val open=vm.incoming.opening(id)!!
        assertEquals(a,open.target!!.chatId); assertEquals("Shared text",vm.chat(a)!!.draftText)
    }
    @Test fun explicitNavigationCancelsOutstandingWorkIncludingAnUnacknowledgedOpen() {
        val vm=model(); val a=group(vm); vm.incoming.observeRoute("origin",false)
        val id=share(vm); stage(vm,id,a); assertNotNull(vm.incoming.opening(id))
        vm.incoming.observeRoute("somewhere-else",false); vm.incoming.opened(id,true)
        assertNull(vm.incoming.work); assertEquals("Shared text",vm.chat(a)!!.draftText)
    }
    @Test fun lockedRequestsStayQueuedUntilUnlockedAndCancelDoesNotStage() {
        val vm=model(); vm.incoming.chooseLock(true); val a=group(vm)
        val id=vm.incoming.receive(IncomingEntry.Share(IncomingPayload("Locked content")))!!
        assertEquals(IncomingPhase.Queued,vm.incoming.work!!.phase)
        vm.incoming.chooseLock(false); vm.incoming.reconcile(); step(vm); stage(vm,id,a)
        assertEquals("Locked content",vm.chat(a)!!.draftText)
    }
    @Test fun requestSurvivesSignInRoutesAndBindsAfterActivation() {
        val vm=AppViewModel(); vm.incoming.observeRoute("welcome",true)
        val id=vm.incoming.receive(IncomingEntry.Share(IncomingPayload("Before sign-in")))!!
        assertEquals(IncomingPhase.Queued,vm.incoming.work!!.phase)
        vm.incoming.observeRoute("sign-in",true); vm.completeSignIn(OnboardingOrigin.Initial)
        vm.incoming.observeRoute("chats",false); step(vm)
        assertEquals(id,vm.incoming.work!!.id); assertEquals(vm.uiState.activeProfileId,vm.incoming.work!!.selectedProfileId)
    }
    @Test fun profileRoundTripCannotCommitAStalePreparedShare() {
        val vm=model(); val original=vm.uiState.activeProfileId!!; val a=group(vm); val id=share(vm)
        vm.incoming.toggle(id,a); vm.incoming.submit(id); val old=vm.incoming.work!!
        vm.completeSignIn(OnboardingOrigin.AddProfile); vm.selectProfile(original); vm.incoming.advance(old.id,old.phase,old.attempt)
        assertNull(vm.incoming.work); assertTrue(vm.chat(a)!!.draftText.isEmpty())
    }
    @Test fun validDirectShareStagesAutomaticallyButWrongOwnerFallsBack() {
        val vm=model(); val a=group(vm); val owner=vm.uiState.activeProfileId!!
        for (target in listOf(IncomingTarget("wrong",a),IncomingTarget(owner,"missing"))) {
            vm.incoming.receive(IncomingEntry.Share(IncomingPayload("Targeted"),target)); step(vm)
            assertEquals(IncomingPhase.Choosing,vm.incoming.work!!.phase); assertTrue(vm.incoming.work!!.fallback)
        }
        vm.incoming.receive(IncomingEntry.Share(IncomingPayload("Targeted"),IncomingTarget(owner,a))); step(vm); assertEquals(IncomingPhase.Applying,vm.incoming.work!!.phase)
        step(vm); assertEquals("Targeted",vm.chat(a)!!.draftText)
    }
    @Test fun shortcutNeverRebindsItsTargetToAnotherProfile() {
        val vm=model(); val a=group(vm); val original=vm.uiState.activeProfileId!!
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        val id=vm.incoming.receive(IncomingEntry.Conversation(IncomingTarget(original,a)))!!; step(vm)
        val open=vm.incoming.opening(id)!!; assertEquals(original,open.profileId); assertEquals(a,open.target!!.chatId)
        assertNotEquals(original,vm.uiState.activeProfileId)
    }
    @Test fun missingShortcutOwnerAndChatAreDistinctFailures() {
        val vm=model()
        for ((target,failure) in listOf(IncomingTarget("missing","c") to IncomingFailure.ProfileUnavailable,IncomingTarget(vm.uiState.activeProfileId!!,"missing") to IncomingFailure.TargetUnavailable)) {
            vm.incoming.receive(IncomingEntry.Conversation(target)); step(vm); assertEquals(failure,vm.incoming.work!!.failure)
        }
    }
    @Test fun malformedAndSecretProfileLinksCannotReachNavigation() {
        val vm=model()
        for (value in listOf("marmot://profile/invalid","nsec1"+"q".repeat(58),"https://example.com/npub")) {
            val id=vm.incoming.receive(IncomingEntry.ProfileLink(value))!!; step(vm)
            assertEquals(IncomingFailure.ContentInvalid,vm.incoming.work!!.failure); assertNull(vm.incoming.opening(id))
        }
    }
    @Test fun publicProfileLinkOpensTheParsedIdentityRatherThanAFixedResult() {
        val vm=model(); val person=vm.uiState.activeProfile!!.people.first { it.id=="maya-chen" }
        val id=vm.incoming.receive(IncomingEntry.ProfileLink(ProfileLinks.forKey(person.publicKey)!!.qrUri!!))!!; step(vm)
        assertEquals(person.id,vm.incoming.opening(id)!!.person!!.id)
    }
    @Test fun switchingProfilesDropsQueuedContentAndItsDeveloperLock() {
        val vm=model(); val original=vm.uiState.activeProfileId!!
        vm.completeSignIn(OnboardingOrigin.AddProfile); val other=vm.uiState.activeProfileId!!; vm.selectProfile(original)
        vm.incoming.chooseLock(true); val id=vm.incoming.receive(IncomingEntry.Share(IncomingPayload("Private")))!!
        assertEquals(IncomingPhase.Queued,vm.incoming.work!!.phase)
        vm.selectProfile(other); assertFalse(vm.incoming.locked); assertNull(vm.incoming.work)
        vm.selectProfile(original); vm.incoming.advance(id,IncomingPhase.Preparing,0); assertNull(vm.incoming.work)
    }
    @Test fun signOutOrWipeDropsPendingAndAcceptedNavigationPayloads() {
        for (wipe in listOf(false,true)) for (accepted in listOf(false,true)) {
            val vm=model(); val chat=group(vm); val id=share(vm)
            if (accepted) stage(vm,id,chat)
            vm.signOutActiveProfile(wipe); assertNull(vm.incoming.work)
            vm.incoming.opened(id,true); assertNull(vm.incoming.work)
            if (!wipe) assertEquals(if (accepted) "Shared text" else "",vm.uiState.profiles.first().chats.first { it.id==chat }.draftText)
        }
    }
    @Test fun disablingDeveloperToolsReleasesOnlyTheDeveloperDeferral() {
        val vm=model(); vm.incoming.chooseLock(true)
        vm.incoming.receive(IncomingEntry.Share(IncomingPayload("Queued")))
        vm.setDeveloperToolsEnabled(false)
        assertFalse(vm.incoming.locked); assertEquals(IncomingPhase.Preparing,vm.incoming.work!!.phase)
    }
    @Test fun missingShortcutCanRecoverToChatsWithoutRetargetingTheOriginalRequest() {
        val vm=model(); val entry=IncomingEntry.Conversation(IncomingTarget("missing","gone"))
        val id=vm.incoming.receive(entry)!!; step(vm); assertTrue(vm.incoming.goToChats(id))
        val open=vm.incoming.opening(id)!!
        assertTrue(open.chatList); assertNull(open.target); assertEquals(vm.uiState.activeProfileId,open.profileId)
        assertEquals(entry,vm.incoming.work!!.entry); vm.incoming.opened(id,true)
        assertEquals(IncomingPhase.Complete,vm.incoming.work!!.phase)
    }
    @Test fun losingATargetBeforeCommitLeavesEverySelectedDraftUnchanged() {
        val vm=model(); val a=group(vm); val b=group(vm)
        var current=vm.uiState.activeProfile!!
        var commits=0
        val incoming=IncomingController({listOf(current)},{current.id},{true},{true}) { _,_,_,_ -> commits++; IncomingCommit(listOf(a,b),0) }
        val id=incoming.receive(IncomingEntry.Share(IncomingPayload("Shared")))!!
        incoming.advance(id,IncomingPhase.Preparing,0); incoming.toggle(id,a); incoming.toggle(id,b); assertTrue(incoming.submit(id))
        current=current.copy(chats=current.chats.filterNot { it.id==b })
        incoming.advance(id,IncomingPhase.Applying,0)
        assertEquals(0,commits); assertEquals(IncomingFailure.TargetUnavailable,incoming.work!!.failure)
        assertTrue(current.chats.first { it.id==a }.draftText.isEmpty())
    }
}
