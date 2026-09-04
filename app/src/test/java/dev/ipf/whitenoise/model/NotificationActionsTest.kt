package dev.ipf.whitenoise.model

import dev.ipf.whitenoise.state.*
import org.junit.Assert.*
import org.junit.Test

class NotificationActionsTest {
    private val owner = ProfileFixtures.showcaseProfiles.first()
    private val message = ChatMessage("message","sender",1,"Today",60,"Now","Hello")
    private val chat = Chat("chat",0,ChatKind.Direct("sender"),"Trail",relayUrls=listOf("wss://relay.example"),timeline=listOf(ChatTimelineEntry.Message(message)),unreadCount=1)
    private val target = NotificationTarget(owner.id,chat.id,message.id)
    private val profile = owner.copy(chats=listOf(chat))
    private fun input(kind: NotificationActionKind=NotificationActionKind.Reply,text: String="Hello") = NotificationActionInput("request",NotificationCard("card",1,target),kind,text)
    @Test fun reactionLengthUsesCodePointsAndCurrentChoices() {
        val emoji="😀".repeat(32); assertNotNull(NotificationActions.normalize(input(NotificationActionKind.React,emoji),profile.copy(quickReactions=listOf(emoji))))
        val longer=emoji+"😀"; assertNull(NotificationActions.normalize(input(NotificationActionKind.React,longer),profile.copy(quickReactions=listOf(longer))))
    }
    @Test fun readThroughIsMonotonicAndPreservesLaterIncomingMessages() {
        val later=message.copy(id="later",minuteOfDay=70); val c=chat.copy(timeline=chat.timeline+ChatTimelineEntry.Message(later),unreadCount=2)
        val read=NotificationActions.readThrough(c,target,0)!!; assertEquals(setOf("later"),read.readState!!.unreadIds)
        assertEquals(read,NotificationActions.readThrough(read,target,0))
    }
    @Test fun expiredOrDeletedTargetCannotBeRead() {
        val expired=chat.copy(timeline=listOf(ChatTimelineEntry.Message(message.copy(expiresAtMillis=10))))
        assertNull(NotificationActions.readThrough(expired,target,10)); assertNotNull(NotificationActions.readThrough(expired,target,9))
    }
    @Test fun lockWaitIsBoundedAndDoesNotConsumeOperationRetries() {
        var locked=true; var now=0L; var writes=0
        val c=NotificationActionController({listOf(profile)},{true},{profile.id},{true},{locked},{now},{_,_->writes++;true},{true})
        c.submit(input(NotificationActionKind.MarkRead)); assertEquals(NotificationActionPhase.Waiting,c.work!!.phase)
        now=NotificationActionController.lockWaitMillis; c.reconcile(); assertEquals(NotificationActionFailure.Locked,c.work!!.failure)
        assertEquals(0,c.work!!.operationFailures); locked=false; assertTrue(c.retry(c.work!!.id))
        c.work!!.let { c.advance(it.id,it.phase,it.attempt) }; assertTrue(c.work!!.accepted); assertEquals(0,writes)
    }
    @Test fun ownerDisappearanceCancelsAndStaleCallbacksCannotMutate() {
        var signed=true; var calls=0
        val c=NotificationActionController({listOf(profile)},{signed},{profile.id},{true},{false},{0},{_,_->calls++;true},{true})
        c.submit(input()); val w=c.work!!; signed=false; c.advance(w.id,w.phase,w.attempt); assertNull(c.work); assertEquals(0,calls)
    }
    @Test fun olderCardUpdateCannotReplaceANewerGenerationAndOtherOwnerSurvivesCleanup() {
        val other=profile.copy(id="another"); val c=NotificationActionController({listOf(profile,other)},{true},{profile.id},{true},{false},{0},{_,_->true},{true})
        c.recordCard(NotificationCard("card",4,target)); assertFalse(c.recordCard(NotificationCard("card",2,target)))
        c.recordCard(NotificationCard("card",1,target.copy(profileId=other.id)))
        c.submit(input()); repeat(2) { c.work!!.let { w->c.advance(w.id,w.phase,w.attempt) } }
        assertEquals(NotificationActionPhase.Complete,c.work!!.phase); assertEquals(2,c.cards.size)
    }
    @Test fun changedEligibilityIsCheckedBeforeMutation() {
        var p=profile; var calls=0
        val c=NotificationActionController({listOf(p)},{true},{p.id},{true},{false},{0},{_,_->calls++;true},{true})
        c.submit(input()); p=p.copy(chats=listOf(chat.copy(membership=ChatMembership.Left)))
        c.work!!.let { c.advance(it.id,it.phase,it.attempt) }; assertEquals(NotificationActionFailure.TargetUnavailable,c.work!!.failure); assertEquals(0,calls)
    }
    @Test fun launchWaitAndEraseCannotReplayOldWork() {
        var ready=false; var calls=0
        val c=NotificationActionController({listOf(profile)},{true},{profile.id},{ready},{false},{0},{_,_->calls++;true},{true})
        c.submit(input()); assertEquals(NotificationActionPhase.Waiting,c.work!!.phase); ready=true; c.reconcile(); val w=c.work!!
        c.erase(); c.advance(w.id,w.phase,w.attempt); assertEquals(0,calls); assertNull(c.work); assertTrue(c.cards.isEmpty())
    }
    @Test fun unlockRetryLeavesAllThreeOperationAttemptsAvailable() {
        var locked=true
        val p=profile.copy(developerTools=profile.developerTools.copy(isEnabled=true))
        val c=NotificationActionController({listOf(p)},{true},{p.id},{true},{locked},{0},{_,_->true},{true})
        c.choose(NotificationActionScenario.AlwaysFails); val id=c.submit(input())!!
        locked=false; assertTrue(c.retry(id))
        repeat(2) { c.work!!.let { w->c.advance(w.id,w.phase,w.attempt) }; assertTrue(c.retry(id)) }
        c.work!!.let { c.advance(it.id,it.phase,it.attempt) }
        assertEquals(3,c.work!!.operationFailures); assertEquals(NotificationActionFailure.Exhausted,c.work!!.failure)
    }

}
