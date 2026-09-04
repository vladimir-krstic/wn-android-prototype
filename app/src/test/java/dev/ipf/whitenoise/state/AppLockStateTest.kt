package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class AppLockStateTest {
    private class Session {
        var p = ProfileFixtures.showcaseProfiles.first().copy(settings = ProfileSettings(requireDeviceAuthentication=true),developerTools=DeveloperToolsState(isEnabled=true))
        var signed = true
        val gates = mutableListOf<Boolean>()
        val c = AppLockController({p},{signed},{gates+=it})
        fun unlock() { c.credentials(true); c.complete(c.requestUnlock()!!) }
    }
    @Test fun firstFrameIsProtectedBeforeCredentialCheck() {
        val s=Session(); assertTrue(s.c.protects(s.p)); s.c.sync(); assertEquals(AppLockPhase.Evaluating,s.c.phase)
        s.c.credentials(true); assertEquals(AppLockPhase.Locked,s.c.phase); assertTrue(s.c.autoPrompt)
    }
    @Test fun unlockCompletesOnlyCurrentPrompt() {
        val s=Session(); s.c.credentials(true); val old=s.c.requestUnlock()!!; s.c.cancel(); val fresh=s.c.requestUnlock()!!
        assertFalse(s.c.complete(old)); assertTrue(s.c.blocked); assertTrue(s.c.complete(fresh)); assertFalse(s.c.blocked)
        assertFalse(s.c.complete(fresh)); assertEquals(listOf(true,false),s.gates)
    }
    @Test fun everyDelayUsesTimeAwayAndExactBoundary() {
        AutoLockDuration.entries.forEach { delay ->
            val s=Session(); s.p=s.p.copy(settings=s.p.settings.copy(autoLockDuration=delay)); s.unlock(); s.c.background(1000)
            if (delay.delayMillis>0) { s.c.resume(1000+delay.delayMillis-1); assertFalse(s.c.blocked); s.c.background(10_000_000) }
            s.c.resume((if(delay.delayMillis>0)10_000_000 else 1000)+delay.delayMillis); assertTrue(s.c.blocked)
        }
    }
    @Test fun activeReadingTimeDoesNotExpireGrace() {
        val s=Session(); s.p=s.p.copy(settings=s.p.settings.copy(autoLockDuration=AutoLockDuration.OneMinute)); s.unlock()
        s.c.background(10); s.c.resume(20); s.c.resume(100_000); assertFalse(s.c.blocked)
        s.c.background(100_001); s.c.resume(100_002); assertFalse(s.c.blocked)
    }
    @Test fun backgroundingLockedScreenCannotRefreshGraceOrKeepPromptAlive() {
        val s=Session(); s.c.credentials(true); val id=s.c.requestUnlock()!!
        s.c.background(10); assertEquals(AppLockPhase.Locked,s.c.phase); assertFalse(s.c.complete(id))
        s.c.resume(20); assertTrue(s.c.blocked); assertFalse(s.c.autoPrompt)
    }
    @Test fun failedAndCancelledPromptsStayLockedAndRetryRecovers() {
        AppUnlockOutcome.entries.filter { it!=AppUnlockOutcome.Success }.forEach { outcome ->
            val s=Session(); s.c.credentials(true); s.c.choose(outcome); s.c.complete(s.c.requestUnlock()!!)
            assertTrue(s.c.blocked); assertEquals(outcome,s.c.failure); s.c.complete(s.c.requestUnlock()!!); assertFalse(s.c.blocked)
        }
    }
    @Test fun losingCredentialDisablesEffectiveGateWithoutErasingPreference() {
        val s=Session(); s.c.credentials(true); val id=s.c.requestUnlock()!!; s.c.credentials(false)
        assertFalse(s.c.blocked); assertTrue(s.p.settings.requireDeviceAuthentication); assertFalse(s.c.complete(id))
        s.c.credentials(true); assertTrue(s.c.protects(s.p))
    }
    @Test fun preferenceAndOwnerChangesInvalidatePrompt() {
        val s=Session(); s.c.credentials(true); val id=s.c.requestUnlock()!!; s.p=s.p.copy(id="another"); s.c.sync()
        assertTrue(s.c.blocked); assertFalse(s.c.complete(id)); val next=s.c.requestUnlock()!!
        s.p=s.p.copy(settings=s.p.settings.copy(requireDeviceAuthentication=false)); s.c.sync(); assertFalse(s.c.complete(next)); assertFalse(s.c.blocked)
    }
    @Test fun signOutAndEraseInvalidatePromptAndGate() {
        val s=Session(); s.c.credentials(true); val id=s.c.requestUnlock()!!; s.signed=false; s.c.sync()
        assertFalse(s.c.blocked); assertFalse(s.c.complete(id)); s.c.erase(); assertNull(s.c.request)
    }
    @Test fun developerDeferToggleCannotUnlockRealSessionGate() {
        val vm=AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
        vm.appLock.credentials(true); vm.updateProfileSettings(vm.uiState.activeProfile!!.settings.copy(requireDeviceAuthentication=true))
        assertTrue(vm.incoming.locked); vm.incoming.chooseLock(false); assertTrue(vm.incoming.locked)
        vm.appLock.complete(vm.appLock.requestUnlock()!!); assertFalse(vm.incoming.locked)
    }
    @Test fun backgroundProtectionIsIndependentOfGraceAndRecentsPreference() {
        val s=Session(); s.p=s.p.copy(settings=s.p.settings.copy(autoLockDuration=AutoLockDuration.ThirtyMinutes)); s.unlock(); s.c.background(1)
        assertTrue(s.c.shieldsBackground); assertFalse(s.p.settings.hideScreenInRecents); s.c.resume(2); assertFalse(s.c.shieldsBackground)
    }
    @Test fun incomingOtherProfileNotificationWaitsForItsUnlockWithoutLosingRequest() {
        val vm=AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
        vm.appLock.credentials(true)
        vm.updateProfileSettings(vm.uiState.activeProfile!!.settings.copy(requireDeviceAuthentication=true))
        vm.appLock.complete(vm.appLock.requestUnlock()!!)
        val owner=vm.uiState.activeProfileId!!; val chat=vm.createGroup("Trail","",ProfileAvatar.Monogram,emptyList())!!
        vm.addConversationArrival(owner,chat); val target=NotificationTarget(owner,chat,vm.chat(chat)!!.timeline.last().id)
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        val id=vm.incoming.receive(IncomingEntry.Notification(target))!!
        vm.incoming.work!!.let { vm.incoming.advance(it.id,it.phase,it.attempt) }
        val open=vm.incoming.opening(id)!!; vm.selectProfile(open.profileId); vm.incoming.opened(id,false)
        assertTrue(vm.appLock.blocked); assertEquals(IncomingPhase.Opening,vm.incoming.work!!.phase); assertNull(vm.incoming.opening(id))
        vm.appLock.complete(vm.appLock.requestUnlock()!!); assertNotNull(vm.incoming.opening(id)); vm.incoming.opened(id,true)
        assertEquals(IncomingPhase.Complete,vm.incoming.work!!.phase)
    }
    @Test fun hiddenConversationCannotAdvanceVisibleReadState() {
        val vm=AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
        val owner=vm.uiState.activeProfileId!!; val chat=vm.createGroup("Trail","",ProfileAvatar.Monogram,emptyList())!!
        vm.addConversationArrival(owner,chat); val message=vm.chat(chat)!!.timeline.last().id
        vm.appLock.credentials(true); vm.updateProfileSettings(vm.uiState.activeProfile!!.settings.copy(requireDeviceAuthentication=true))
        val before=vm.chat(chat); assertFalse(vm.markConversationVisible(owner,chat,setOf(message))); assertEquals(before,vm.chat(chat))
    }

}
