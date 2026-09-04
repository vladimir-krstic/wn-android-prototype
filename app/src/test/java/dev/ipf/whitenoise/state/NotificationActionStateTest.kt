package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class NotificationActionStateTest {
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun target(vm: AppViewModel): NotificationTarget {
        val chat = vm.createGroup("Trail", "", ProfileAvatar.Monogram,emptyList())!!
        assertTrue(vm.addConversationArrival(vm.uiState.activeProfileId!!,chat))
        return NotificationTarget(vm.uiState.activeProfileId!!,chat,vm.chat(chat)!!.timeline.last().id)
    }
    private fun input(target: NotificationTarget, kind: NotificationActionKind = NotificationActionKind.Reply, text: String = " Thanks ", key: String = "a", generation: Long = 1) =
        NotificationActionInput(key,NotificationCard("card",generation,target),kind,text)
    private fun step(vm: AppViewModel) { vm.notificationActions.work!!.let { vm.notificationActions.advance(it.id,it.phase,it.attempt) } }
    private fun chat(vm: AppViewModel,t: NotificationTarget) = vm.uiState.profiles.first { it.id==t.profileId }.chats.first { it.id==t.chatId }
    private fun sent(vm: AppViewModel,t: NotificationTarget) = chat(vm,t).timeline.filterIsInstance<ChatTimelineEntry.Message>().filter { it.id.startsWith("notification-reply-") }
    @Test fun replyPreservesComposerAndCompletesOnlyOnce() {
        val vm=model(); val t=target(vm); vm.updateDraftText(t.chatId,"Draft"); vm.setDraftReply(t.chatId,t.messageId!!)
        val before=chat(vm,t); val action=input(t); vm.notificationActions.recordCard(action.card)
        val id=vm.notificationActions.submit(action)!!; val pending=vm.notificationActions.work!!; step(vm)
        assertTrue(vm.notificationActions.work!!.accepted); assertEquals(NotificationActionPhase.Finishing,vm.notificationActions.work!!.phase)
        vm.notificationActions.advance(pending.id,pending.phase,pending.attempt); step(vm)
        assertEquals(1,sent(vm,t).size); assertEquals("Thanks",sent(vm,t).single().message.text)
        assertEquals(before.draftText,chat(vm,t).draftText); assertEquals(before.draftReplyMessageId,chat(vm,t).draftReplyMessageId)
        assertEquals(before.draftAttachments,chat(vm,t).draftAttachments); assertTrue(vm.notificationActions.cards.isEmpty())
        vm.notificationActions.dismiss(id); assertEquals(id,vm.notificationActions.submit(action)); assertEquals(1,sent(vm,t).size)
    }
    @Test fun cleanupRetryDoesNotSendAgainAndNewerCardsSurvive() {
        val vm=model(); val t=target(vm); vm.notificationActions.choose(NotificationActionScenario.CleanupFails)
        val action=input(t); vm.notificationActions.recordCard(action.card); val id=vm.notificationActions.submit(action)!!; step(vm)
        vm.addConversationArrival(t.profileId,t.chatId); val newer=t.copy(messageId=chat(vm,t).timeline.last().id)
        vm.notificationActions.recordCard(NotificationCard("card",2,newer)); vm.notificationActions.recordCard(NotificationCard("mention",3,newer))
        step(vm); assertEquals(NotificationActionFailure.Cleanup,vm.notificationActions.work!!.failure)
        assertTrue(vm.notificationActions.retry(id)); step(vm)
        assertEquals(1,sent(vm,t).size); assertEquals(2,vm.notificationActions.cards.size)
        assertTrue(newer.messageId in chat(vm,t).readState!!.unreadIds); assertFalse(t.messageId in chat(vm,t).readState!!.unreadIds)
    }
    @Test fun independentIdenticalRepliesAreSeparateActions() {
        val vm=model(); val t=target(vm); vm.notificationActions.submit(input(t)); step(vm); step(vm)
        vm.notificationActions.submit(input(t,key="b")); step(vm); step(vm); assertEquals(2,sent(vm,t).size)
    }
    @Test fun actionKeepsOriginalOwnerWhenAnotherProfileIsActive() {
        val vm=model(); val t=target(vm); vm.notificationActions.submit(input(t))
        vm.completeSignIn(OnboardingOrigin.AddProfile); val other=vm.uiState.activeProfileId; val before=vm.uiState.activeProfile
        step(vm); step(vm); assertEquals(1,sent(vm,t).size); assertEquals(other,vm.uiState.activeProfileId); assertEquals(before,vm.uiState.activeProfile)
    }
    @Test fun reactionSetsWithoutTogglingAndCleanupRetryPreservesIt() {
        val vm=model(); val t=target(vm); val emoji=vm.uiState.activeProfile!!.quickReactions.first()
        vm.notificationActions.choose(NotificationActionScenario.CleanupFails)
        val id=vm.notificationActions.submit(input(t,NotificationActionKind.React,emoji))!!; step(vm); step(vm)
        assertTrue(vm.notificationActions.retry(id)); step(vm)
        vm.notificationActions.submit(input(t,NotificationActionKind.React,emoji,"b")); step(vm)
        val reactions=chat(vm,t).timeline.filterIsInstance<ChatTimelineEntry.Message>().first { it.id==t.messageId }.message.reactions
        assertEquals(listOf(t.profileId),reactions.single { it.emoji==emoji }.personIds)
    }
    @Test fun reactionChoiceMustStillBeAllowedWhenWorkRuns() {
        val vm=model(); val t=target(vm); val emoji=vm.uiState.activeProfile!!.quickReactions.first()
        vm.notificationActions.submit(input(t,NotificationActionKind.React,emoji))
        assertTrue(vm.setQuickReactions(listOf("1","2","3","4","5","6"))); step(vm)
        assertEquals(NotificationActionFailure.TargetUnavailable,vm.notificationActions.work!!.failure)
        assertTrue(chat(vm,t).timeline.filterIsInstance<ChatTimelineEntry.Message>().first { it.id==t.messageId }.message.reactions.isEmpty())
    }
    @Test fun operationRetriesStopAfterThreeAttempts() {
        val vm=model(); val t=target(vm); vm.notificationActions.choose(NotificationActionScenario.AlwaysFails)
        val id=vm.notificationActions.submit(input(t))!!
        repeat(2) { step(vm); assertTrue(vm.notificationActions.retry(id)) }; step(vm)
        assertEquals(NotificationActionFailure.Exhausted,vm.notificationActions.work!!.failure); assertFalse(vm.notificationActions.retry(id)); assertTrue(sent(vm,t).isEmpty())
    }
    @Test fun cancellationAndStaleCallbacksCannotSend() {
        val vm=model(); val t=target(vm); val id=vm.notificationActions.submit(input(t))!!; val old=vm.notificationActions.work!!
        vm.notificationActions.dismiss(id); vm.notificationActions.advance(old.id,old.phase,old.attempt)
        assertTrue(sent(vm,t).isEmpty()); assertEquals(id,vm.notificationActions.submit(input(t))); assertNull(vm.notificationActions.work)
    }
    @Test fun malformedActionsDoNotCreateWork() {
        val vm=model(); val t=target(vm)
        assertNull(vm.notificationActions.submit(input(t,text=" \n ")))
        assertNull(vm.notificationActions.submit(input(t,NotificationActionKind.React,"not allowed")))
        assertNull(vm.notificationActions.submit(input(t.copy(kind=NotificationTargetKind.Invite))))
        assertNull(vm.notificationActions.submit(input(t.copy(messageId=null))))
        assertNull(vm.notificationActions.submit(input(t).copy(card=NotificationCard("",1,t))))
    }
    @Test fun conflictingRequestIdentityCannotReplaceOriginalPayload() {
        val vm=model(); val t=target(vm); vm.notificationActions.submit(input(t))
        assertNull(vm.notificationActions.submit(input(t,text="Changed"))); step(vm); assertEquals("Thanks",sent(vm,t).single().message.text)
    }
    @Test fun markReadUsesExactMessageBoundary() {
        val vm=model(); val t=target(vm); vm.addConversationArrival(t.profileId,t.chatId)
        val later=chat(vm,t).timeline.last().id; vm.notificationActions.submit(input(t,NotificationActionKind.MarkRead)); step(vm); step(vm)
        assertFalse(t.messageId in chat(vm,t).readState!!.unreadIds); assertTrue(later in chat(vm,t).readState!!.unreadIds)
        assertTrue(sent(vm,t).isEmpty())
    }
    @Test fun lockDefersAcceptedWorkAndRejectsNewReplyUntilUnlocked() {
        val vm=model(); val t=target(vm); vm.incoming.chooseLock(true)
        val id=vm.notificationActions.submit(input(t))!!; assertEquals(NotificationActionFailure.Locked,vm.notificationActions.work!!.failure)
        assertFalse(vm.notificationActions.retry(id)); vm.incoming.chooseLock(false); assertTrue(vm.notificationActions.retry(id)); step(vm)
        vm.incoming.chooseLock(true); vm.notificationActions.reconcile(); assertEquals(NotificationActionPhase.Waiting,vm.notificationActions.work!!.phase)
        vm.incoming.chooseLock(false); vm.notificationActions.reconcile(); step(vm); assertEquals(1,sent(vm,t).size)
    }
    @Test fun signOutCancelsPendingOwnerAndEraseClearsCardsAndWork() {
        val vm=model(); val t=target(vm); val action=input(t); vm.notificationActions.recordCard(action.card); vm.notificationActions.submit(action)
        val old=vm.notificationActions.work!!; vm.signOutActiveProfile(false); vm.notificationActions.advance(old.id,old.phase,old.attempt)
        assertNull(vm.notificationActions.work); assertTrue(vm.notificationActions.cards.isEmpty())
    }
    @Test fun dismissingAcceptedStatusStillFinishesReadAndCardCleanup() {
        val vm=model(); val t=target(vm); val action=input(t); vm.notificationActions.recordCard(action.card)
        val id=vm.notificationActions.submit(action)!!; step(vm); vm.notificationActions.dismiss(id)
        assertFalse(vm.notificationActions.work!!.presented); step(vm)
        assertEquals(NotificationActionPhase.Complete,vm.notificationActions.work!!.phase)
        assertTrue(vm.notificationActions.cards.isEmpty()); assertFalse(t.messageId in chat(vm,t).readState!!.unreadIds)
        assertEquals(1,sent(vm,t).size)
    }

}
