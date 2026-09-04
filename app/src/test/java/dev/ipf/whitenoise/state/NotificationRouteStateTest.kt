package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class NotificationRouteStateTest {
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun target(vm: AppViewModel): NotificationTarget {
        val id=vm.createGroup("Trail", "", ProfileAvatar.Monogram,emptyList())!!
        vm.addConversationArrival(vm.uiState.activeProfileId!!,id)
        return NotificationTarget(vm.uiState.activeProfileId!!,id,vm.chat(id)!!.timeline.last().id)
    }
    private fun step(vm: AppViewModel) { vm.incoming.work!!.let { vm.incoming.advance(it.id,it.phase,it.attempt) } }
    @Test fun tapLoadsExactHistoryTargetAndCommitsAfterBoundaryCaptureOnlyOnce() {
        val vm=model(); val t=target(vm); vm.addConversationArrival(t.profileId,t.chatId); val newer=vm.chat(t.chatId)!!.timeline.last().id
        val id=vm.incoming.receive(IncomingEntry.Notification(t))!!; step(vm); val open=vm.incoming.opening(id)!!
        assertEquals(t,open.notification); vm.registerNotificationRead(open); vm.openChat(t.chatId); vm.incoming.opened(id,true)
        assertTrue(t.messageId in vm.chat(t.chatId)!!.readState!!.unreadIds)
        assertTrue(vm.commitNotificationRead(id,t.profileId,t.chatId)); assertFalse(vm.commitNotificationRead(id,t.profileId,t.chatId))
        assertFalse(t.messageId in vm.chat(t.chatId)!!.readState!!.unreadIds); assertTrue(newer in vm.chat(t.chatId)!!.readState!!.unreadIds)
    }
    @Test fun freshEqualTapRejectsOldCompletionAndKeepsNewRequest() {
        val vm=model(); val t=target(vm); val old=vm.incoming.receive(IncomingEntry.Notification(t))!!; step(vm); vm.incoming.opening(old)
        val next=vm.incoming.receive(IncomingEntry.Notification(t))!!; vm.incoming.opened(old,true)
        assertEquals(next,vm.incoming.work!!.id); assertNotEquals(IncomingPhase.Complete,vm.incoming.work!!.phase)
        assertEquals(next,vm.incoming.receive(null)); step(vm); assertEquals(next,vm.incoming.opening(next)!!.requestId)
    }
    @Test fun otherProfileTapActivatesOnlyItsOwner() {
        val vm=model(); val t=target(vm); vm.completeSignIn(OnboardingOrigin.AddProfile); val before=vm.uiState.activeProfileId
        val id=vm.incoming.receive(IncomingEntry.Notification(t))!!; step(vm); assertEquals(before,vm.uiState.activeProfileId)
        val opening=vm.incoming.opening(id)!!; vm.selectProfile(opening.profileId); vm.registerNotificationRead(opening); vm.incoming.opened(id,true)
        assertEquals(t.profileId,vm.uiState.activeProfileId); assertEquals(IncomingPhase.Complete,vm.incoming.work!!.phase)
        assertFalse(vm.commitNotificationRead(id,before!!,t.chatId)); assertTrue(vm.commitNotificationRead(id,t.profileId,t.chatId))
    }
    @Test fun chatListNotificationNeverOpensRemovedGroup() {
        val vm=model(); val t=target(vm).copy(chatId="removed",kind=NotificationTargetKind.ChatList,messageId=null)
        val id=vm.incoming.receive(IncomingEntry.Notification(t))!!; step(vm); val opening=vm.incoming.opening(id)!!
        assertTrue(opening.chatList); assertNull(opening.target); assertNull(opening.notification)
    }
    @Test fun missingOwnerAndConversationHaveRecovery() {
        val vm=model(); val t=target(vm)
        var id=vm.incoming.receive(IncomingEntry.Notification(t.copy(profileId="gone")))!!; step(vm)
        assertEquals(IncomingFailure.ProfileUnavailable,vm.incoming.work!!.failure); assertTrue(vm.incoming.goToChats(id)); assertTrue(vm.incoming.opening(id)!!.chatList)
        id=vm.incoming.receive(IncomingEntry.Notification(t.copy(chatId="gone")))!!; step(vm)
        assertEquals(IncomingFailure.TargetUnavailable,vm.incoming.work!!.failure); assertTrue(vm.incoming.goToChats(id))
    }
    @Test fun delayedInvitationWaitsForThirdProbeAndThenOpens() {
        val vm=model(); val t=target(vm).copy(kind=NotificationTargetKind.Invite,messageId=null)
        vm.incoming.choose(IncomingScenario.InviteRowDelayed); val id=vm.incoming.receive(IncomingEntry.Notification(t))!!
        repeat(2) { step(vm); assertEquals(IncomingPhase.Preparing,vm.incoming.work!!.phase) }; step(vm)
        assertEquals(t,vm.incoming.opening(id)!!.notification)
    }
    @Test fun absentInvitationExhaustsThreeProbesWithoutClaimingGone() {
        val vm=model(); val t=target(vm).copy(chatId="pending-row",kind=NotificationTargetKind.Invite,messageId=null)
        val id=vm.incoming.receive(IncomingEntry.Notification(t))!!; repeat(3) { step(vm) }
        assertEquals(IncomingFailure.InviteUnconfirmed,vm.incoming.work!!.failure); assertTrue(vm.incoming.retry(id)); assertEquals(0,vm.incoming.work!!.probe)
    }
    @Test fun authoritativeInvitationUnavailableIsDifferentFromInconclusive() {
        val vm=model(); val t=target(vm).copy(kind=NotificationTargetKind.Invite,messageId=null)
        vm.incoming.choose(IncomingScenario.InviteUnavailable); vm.incoming.receive(IncomingEntry.Notification(t)); step(vm)
        assertEquals(IncomingFailure.TargetUnavailable,vm.incoming.work!!.failure)
    }
    @Test fun lockDefersTapAndLauncherCannotDiscardIt() {
        val vm=model(); val t=target(vm); vm.incoming.chooseLock(true)
        val id=vm.incoming.receive(IncomingEntry.Notification(t))!!; assertEquals(IncomingPhase.Queued,vm.incoming.work!!.phase)
        assertEquals(id,vm.incoming.receive(null)); vm.incoming.chooseLock(false); vm.incoming.reconcile(); step(vm); assertNotNull(vm.incoming.opening(id))
    }
    @Test fun readCannotAdvanceForMissingMessageOrDifferentRouteOwner() {
        val vm=model(); val t=target(vm); val before=vm.chat(t.chatId)
        assertFalse(vm.markNotificationThrough(t.copy(messageId="gone"))); assertFalse(vm.commitNotificationRead(99,t.profileId,t.chatId)); assertEquals(before,vm.chat(t.chatId))
    }
    @Test fun membershipRoleNotificationWithoutMessageOpensChatWithoutReadCommit() {
        val vm=model(); val t=target(vm).copy(messageId=null)
        val id=vm.incoming.receive(IncomingEntry.Notification(t))!!; step(vm)
        val open=vm.incoming.opening(id)!!; assertEquals(t.chatId,open.target!!.chatId); assertNull(open.notification!!.messageId)
        vm.registerNotificationRead(open); assertFalse(vm.commitNotificationRead(id,t.profileId,t.chatId))
    }

}
