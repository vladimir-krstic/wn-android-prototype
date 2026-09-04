package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class MessageBatchStateTest {
    private val sourceId = "fiatjaf"
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun AppViewModel.owner() = uiState.activeProfileId!!
    private fun AppViewModel.ids(chat: String = sourceId) = chat(chat)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().map { it.id }.toSet()
    private fun AppViewModel.authored(count: Int = 3): Set<String> {
        val before = ids(); repeat(count) { sendText(sourceId, "Forward body $it") }; return ids() - before
    }
    private fun AppViewModel.finishDelete(chatId: String) {
        var steps=0
        while (chat(chatId)!!.messageDeletion?.isRunning == true) {
            val op=chat(chatId)!!.messageDeletion!!; assertTrue(advanceMessageDeletion(owner(), chatId, op.id, op.revision)); check(++steps<500)
        }
    }
    private fun AppViewModel.stepForward() {
        val op=messageForwards.getValue(owner()); assertTrue(advanceMessageForward(owner(), op.id, op.revision))
    }
    private fun AppViewModel.finishForward() { var steps=0; while(messageForwards.getValue(owner()).isRunning) { stepForward(); check(++steps<1000) } }
    private fun AppViewModel.copies(target: String, profileId: String = owner()) = uiState.profiles.first { it.id==profileId }.chats.first { it.id==target }
        .timeline.filterIsInstance<ChatTimelineEntry.Message>().map { it.message }.filter { "-forward-" in it.id }

    @Test fun deletionPolicySeparatesAdminMemberDirectEndedAndTombstone() {
        val p=ProfileFixtures.marmota; val group=p.chats.first { it.isGroup && it.membership==ChatMembership.Active }
            .copy(relayUrls=listOf("wss://relay.example"), members=listOf(GroupMember(p.id, GroupRole.Admin)))
        val theirs=ChatMessage("other", "other", 1, "Today", 1, "Now", "Other text")
        assertTrue(MessageDeletion.canDeleteForEveryone(theirs,p,group))
        assertFalse(MessageDeletion.canDeleteForEveryone(theirs,p,group.copy(kind=ChatKind.Direct("other"))))
        assertFalse(MessageDeletion.canDeleteForEveryone(theirs,p,group.copy(members=listOf(GroupMember(p.id,GroupRole.Member)))))
        assertFalse(MessageDeletion.canDeleteForEveryone(theirs,p,group.copy(membership=ChatMembership.Left)))
        assertFalse(MessageDeletion.canDeleteForEveryone(theirs.copy(deletionState=MessageDeletionState.DeletedByOther),p,group))
        assertTrue(MessageDeletion.canDeleteForEveryone(theirs.copy(authorId=p.id),p,group.copy(members=listOf(GroupMember(p.id,GroupRole.Member)))))
    }
    @Test fun mixedDeletionFreezesRemoteAndLocalOperationsAndRetriesOnlyFailures() {
        val vm=model(); val own=vm.authored(1).single(); val other=vm.chat(sourceId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().first { it.message.authorId!=vm.owner() }.id
        vm.selectMessageDeleteScenario(MessageDeleteScenario.Partial)
        assertTrue(vm.beginMessageDeletion(vm.owner(),sourceId,setOf(own,other),MessageDeletionScope.ForEveryone))
        val plan=vm.chat(sourceId)!!.messageDeletion!!
        assertEquals(MessageDeletionScope.ForEveryone,plan.items.first { it.messageId==own }.scope)
        assertEquals(MessageDeletionScope.ForMe,plan.items.first { it.messageId==other }.scope)
        vm.finishDelete(sourceId); val partial=vm.chat(sourceId)!!.messageDeletion!!
        assertEquals(1,partial.succeeded); assertEquals(1,partial.failed.size)
        val successful=partial.items.single { it.phase==MessageDeletePhase.Succeeded }
        assertTrue(vm.retryMessageDeletion(vm.owner(),sourceId,partial.id)); vm.finishDelete(sourceId)
        val done=vm.chat(sourceId)!!.messageDeletion!!
        assertEquals(2,done.succeeded); assertEquals(successful,done.items.first { it.messageId==successful.messageId })
        assertNull(vm.message(sourceId,other)); assertTrue(vm.message(sourceId,own)!!.isDeleted)
        assertFalse(vm.advanceMessageDeletion(vm.owner(),sourceId,partial.id,partial.revision))
    }
    @Test fun adminRemovalClearsPayloadAndTombstoneCanThenBeRemovedLocally() {
        val vm=model();val p=vm.uiState.activeProfile!!
        val chat=p.chats.first { it.isGroup && it.composerAvailability(p)==ComposerAvailability.Available && it.members.any { m->m.personId==p.id&&m.role==GroupRole.Admin } }
        val message=chat.timeline.filterIsInstance<ChatTimelineEntry.Message>().first { it.message.authorId!=p.id }.message
        assertTrue(vm.beginMessageDeletion(p.id,chat.id,setOf(message.id),MessageDeletionScope.ForEveryone));vm.finishDelete(chat.id)
        val deleted=vm.message(chat.id,message.id)!!
        assertTrue(deleted.isDeleted);assertEquals("",deleted.text);assertTrue(deleted.attachments.isEmpty());assertTrue(deleted.reactions.isEmpty());assertNull(deleted.editHistory)
        assertFalse(vm.beginMessageDeletion(p.id,chat.id,setOf(message.id),MessageDeletionScope.ForEveryone))
        assertTrue(vm.beginMessageDeletion(p.id,chat.id,setOf(message.id),MessageDeletionScope.ForMe));vm.finishDelete(chat.id);assertNull(vm.message(chat.id,message.id))
    }
    @Test fun lostPublicationPermissionNeverSilentlyFallsBackToLocalRemoval() {
        val vm=model(); val own=vm.authored(1)
        assertTrue(vm.beginMessageDeletion(vm.owner(),sourceId,own,MessageDeletionScope.ForEveryone)); assertTrue(vm.leaveChat(sourceId));vm.finishDelete(sourceId)
        assertEquals(MessageDeleteFailure.PermissionDenied,vm.chat(sourceId)!!.messageDeletion!!.failed.single().failure)
        assertFalse(vm.message(sourceId,own.single())!!.isDeleted)
    }
    @Test fun localDeletionAfterMembershipEndsAndMissingItemRecoveryAreIndependent() {
        val vm=model(); val own=vm.authored(2);vm.leaveChat(sourceId)
        assertTrue(vm.beginMessageDeletion(vm.owner(),sourceId,own,MessageDeletionScope.ForMe))
        assertTrue(vm.deleteMessages(sourceId,setOf(own.first()),MessageDeletionScope.ForMe));vm.finishDelete(sourceId)
        val op=vm.chat(sourceId)!!.messageDeletion!!;assertEquals(1,op.succeeded);assertEquals(MessageDeleteFailure.Unavailable,op.failed.single().failure)
        assertTrue(own.none { vm.message(sourceId,it)!=null })
    }
    @Test fun profileSwitchInvalidatesDeletionAndReportContainsOnlyCountsAndCategories() {
        val vm=model();val ids=vm.authored();val owner=vm.owner()
        assertTrue(vm.beginMessageDeletion(owner,sourceId,ids,MessageDeletionScope.ForEveryone));val before=vm.chat(sourceId)!!.messageDeletion!!
        vm.completeSignIn(OnboardingOrigin.AddProfile);vm.selectProfile(owner)
        val op=vm.chat(sourceId)!!.messageDeletion!!;assertFalse(op.isRunning);assertFalse(op.canRetry)
        assertFalse(vm.advanceMessageDeletion(owner,sourceId,before.id,before.revision))
        val report=op.report();assertTrue(report.contains("succeeded=0"));assertTrue(ids.none { it in report });assertFalse(report.contains("Forward body"));assertFalse(report.contains(owner))
    }
    @Test fun largeSelectionsAndFoldersHaveNoLegacyCountTruncation() {
        val vm=model();val ids=vm.authored(33);val p=vm.uiState.activeProfile!!
        val targets=p.chats.filter { it.id!=sourceId&&it.composerAvailability(p)==ComposerAvailability.Available }.take(6).map { it.id }
        assertEquals(6,targets.size); assertTrue(MessageActionPolicy.canForward(ids.map { vm.message(sourceId,it)!! }))
        val folder=ChatFolder("folder","All",(targets+sourceId).toSet())
        val members=MessageForwarding.folderMembers(p,p.id,sourceId,folder);assertEquals(targets.toSet(),members.toSet())
        assertEquals(targets.toSet(),MessageForwarding.toggleFolder(emptySet(),members));assertTrue(MessageForwarding.toggleFolder(targets.toSet(),members).isEmpty())
        assertTrue(vm.beginMessageForward(p.id,sourceId,ids,p.id,targets));vm.finishForward()
        assertEquals(6,vm.messageForwards.getValue(p.id).succeeded);targets.forEach { assertEquals(33,vm.copies(it).size) }
    }
    @Test fun partialPublishRetainsSentPrefixAndRetryDoesNotReplayCompletedDestinations() {
        val vm=model();val ids=vm.authored();val owner=vm.owner();vm.selectMessageForwardScenario(MessageForwardScenario.PartialSend)
        assertTrue(vm.beginMessageForward(owner,sourceId,ids,owner,listOf("maya-chen","theo-grant")));vm.finishForward()
        val op=vm.messageForwards.getValue(owner);assertEquals(MessageForwardPhase.PartialFailure,op.phase)
        assertEquals(3,vm.copies("maya-chen").size);assertEquals(1,vm.copies("theo-grant").size)
        val first=vm.copies("maya-chen");val second=vm.copies("theo-grant").single()
        assertTrue(vm.retryMessageForward(owner,op.id));vm.finishForward()
        assertEquals(first,vm.copies("maya-chen"));assertEquals(second,vm.copies("theo-grant").first());assertEquals(3,vm.copies("theo-grant").size)
        assertEquals(listOf("Forward body 0","Forward body 1","Forward body 2"),vm.copies("theo-grant").map { it.text })
    }
    @Test fun cancellationIsAllowedBeforePublishingAndRejectedOncePublishingStarts() {
        val vm=model();val ids=vm.authored(1);val owner=vm.owner()
        vm.beginMessageForward(owner,sourceId,ids,owner,listOf("maya-chen"));val op=vm.messageForwards.getValue(owner)
        assertTrue(vm.cancelMessageForward(owner,op.id));assertTrue(vm.copies("maya-chen").isEmpty());assertFalse(vm.advanceMessageForward(owner,op.id,op.revision))
        vm.beginMessageForward(owner,sourceId,ids,owner,listOf("maya-chen"));vm.stepForward();vm.stepForward()
        val sending=vm.messageForwards.getValue(owner);assertEquals(MessageForwardTargetPhase.Sending,sending.targets.single().phase)
        assertFalse(vm.cancelMessageForward(owner,sending.id));vm.finishForward();assertEquals(1,vm.copies("maya-chen").size)
    }
    @Test fun preparationFailuresRetryButExpiryAndSessionFailuresAreTerminal() {
        for (scenario in listOf(MessageForwardScenario.PreparationFails,MessageForwardScenario.PreparationTimeout,MessageForwardScenario.Expired,MessageForwardScenario.SessionChanged,MessageForwardScenario.PayloadTooLarge)) {
            val vm=model();val ids=vm.authored(1);vm.selectMessageForwardScenario(scenario)
            vm.beginMessageForward(vm.owner(),sourceId,ids,vm.owner(),listOf("maya-chen"));vm.finishForward()
            val op=vm.messageForwards.getValue(vm.owner());assertEquals(0,op.succeeded);assertTrue(vm.copies("maya-chen").isEmpty())
            val retryable=scenario in listOf(MessageForwardScenario.PreparationFails,MessageForwardScenario.PreparationTimeout)
            assertEquals(retryable,op.canRetry)
            if(retryable){assertTrue(vm.retryMessageForward(vm.owner(),op.id));vm.finishForward();assertEquals(1,vm.copies("maya-chen").size)}
        }
    }
    @Test fun targetBecomingBlockedAfterConfirmationDoesNotBlockOtherDestinations() {
        val vm=model();val ids=vm.authored(1);val owner=vm.owner()
        vm.beginMessageForward(owner,sourceId,ids,owner,listOf("maya-chen","theo-grant"));vm.toggleBlocked("maya-chen");vm.finishForward()
        val op=vm.messageForwards.getValue(owner);assertEquals(MessageForwardFailure.Blocked,op.targets.first().failure);assertEquals(1,op.succeeded)
        vm.toggleBlocked("maya-chen");assertTrue(vm.retryMessageForward(owner,op.id));vm.finishForward();assertEquals(1,vm.copies("maya-chen").size);assertEquals(1,vm.copies("theo-grant").size)
    }
    @Test fun removedSourceAndStaleStepCannotSendRetainedPayload() {
        val vm=model();val ids=vm.authored(1);val owner=vm.owner()
        vm.beginMessageForward(owner,sourceId,ids,owner,listOf("maya-chen"));val first=vm.messageForwards.getValue(owner);vm.stepForward()
        assertFalse(vm.advanceMessageForward(owner,first.id,first.revision));vm.deleteMessages(sourceId,ids,MessageDeletionScope.ForMe);vm.finishForward()
        assertEquals(MessageForwardFailure.SourceUnavailable,vm.messageForwards.getValue(owner).targets.single().failure);assertTrue(vm.copies("maya-chen").isEmpty())
    }
    @Test fun destinationProfileChoiceKeepsActiveProfileAndSameChatIdsIsolated() {
        val vm=model();val owner=vm.owner();val ids=vm.authored(1)
        vm.completeSignIn(OnboardingOrigin.AddProfile);val destination=vm.owner()
        val target=vm.openOrCreateDirectChat("fiatjaf", requestedChatId=sourceId)!!;vm.selectProfile(owner)
        assertTrue(vm.beginMessageForward(owner,sourceId,ids,destination,listOf(target)));assertEquals(owner,vm.owner());vm.finishForward()
        assertEquals(1,vm.copies(target,destination).size);assertEquals(destination,vm.copies(target,destination).single().authorId)
        assertTrue(vm.uiState.activeProfile!!.chats.firstOrNull { it.id==target }?.timeline?.none { "-forward-" in it.id }!=false)
    }
    @Test fun profileSwitchCannotReplayForwardAfterReturningToSource() {
        val vm=model();val owner=vm.owner();val ids=vm.authored(1);vm.beginMessageForward(owner,sourceId,ids,owner,listOf("maya-chen"))
        val before=vm.messageForwards.getValue(owner);vm.completeSignIn(OnboardingOrigin.AddProfile);vm.selectProfile(owner)
        assertEquals(MessageForwardFailure.SessionChanged,vm.messageForwards.getValue(owner).targets.single().failure)
        assertFalse(vm.advanceMessageForward(owner,before.id,before.revision));assertFalse(vm.retryMessageForward(owner,before.id));assertTrue(vm.copies("maya-chen").isEmpty())
    }
    @Test fun sourceExpiryAndSingleMediaFramePreserveEligibilityAndFreshMetadata() {
        val source=ChatMessage("source","owner",1,"Today",1,"Now","Caption",expiresAtMillis=MessageForwarding.nowMillis)
        assertEquals(MessageForwardFailure.Expired,MessageForwarding.sourceFailure(listOf(source)))
        val vm=model();val p=vm.uiState.activeProfile!!; val chat=vm.chat("catalog-media-gallery")!!
        val media=ConversationMediaProjection.items(chat,p).first { it.key.messageId=="MED-04" && it.key.attachmentId=="MED-04-photo-2" }
        val payload=MessageForwarding.payload(p,chat,setOf(media.message.id),media.key,"  A new caption  ")!!.single()
        assertEquals("A new caption",payload.text);assertEquals(MessageAttachmentKind.Photo,payload.attachments.single().kind);assertEquals(listOf(media.image),payload.attachments.single().images)
        val copy=MessageForwarding.copyForDestination(payload.copy(editHistory=MessageEditHistory("Original",1,emptyList())),99,chat,p.id,0,3,700)
        assertNull(copy.editHistory);assertNull(copy.expiresAtMillis);assertNull(copy.replyToMessageId);assertTrue(copy.reactions.isEmpty());assertNotEquals(payload.attachments.single().id,copy.attachments.single().id)
    }
    @Test fun mediaPreparationAndUploadFailureRetainCompletedDestinationOnRetry() {
        val vm=model();val owner=vm.owner();val source="catalog-media-gallery"
        vm.selectMessageForwardScenario(MessageForwardScenario.PartialUpload)
        assertTrue(vm.beginMessageForward(owner,source,setOf("MED-04"),owner,listOf("maya-chen","theo-grant")))
        val initial=vm.messageForwards.getValue(owner);assertTrue(initial.totalAttachments>0);vm.stepForward()
        assertEquals(1,vm.messageForwards.getValue(owner).prepared);vm.finishForward()
        val op=vm.messageForwards.getValue(owner);assertEquals(MessageForwardFailure.Upload,op.targets[1].failure)
        val sent=vm.copies("maya-chen");assertTrue(sent.isNotEmpty());assertTrue(vm.copies("theo-grant").isEmpty())
        assertTrue(vm.retryMessageForward(owner,op.id));vm.finishForward()
        assertEquals(sent,vm.copies("maya-chen"));assertEquals(sent.size,vm.copies("theo-grant").size)
    }
    @Test fun selectingSameProfileKeepsWorkButWipingItsDataErasesRetainedForwardPayload() {
        val vm=model();val owner=vm.owner();val ids=vm.authored(1)
        vm.beginMessageForward(owner,sourceId,ids,owner,listOf("maya-chen"));val op=vm.messageForwards.getValue(owner)
        vm.selectProfile(owner);assertEquals(op,vm.messageForwards[owner])
        vm.signOutActiveProfile(wipeData=true);assertTrue(vm.messageForwards.isEmpty());assertTrue(vm.uiState.profiles.none { it.id==owner })
        assertFalse(vm.advanceMessageForward(owner,op.id,op.revision))
    }

    @Test fun automaticRetriesBackOffThreeTimesAndManualRetryKeepsSentPrefix() {
        val vm=model();val owner=vm.owner();val ids=vm.authored(3)
        vm.selectMessageForwardScenario(MessageForwardScenario.PartialSendUntilRetried)
        vm.beginMessageForward(owner,sourceId,ids,owner,listOf("maya-chen","theo-grant"));vm.finishForward()
        val completed=vm.copies("maya-chen");val prefix=vm.copies("theo-grant").single()
        repeat(3) { i ->
            val op=vm.messageForwards.getValue(owner);assertTrue(op.canAutomaticallyRetry);assertEquals(1_000L shl i,op.automaticRetryDelayMillis)
            assertTrue(vm.retryMessageForward(owner,op.id,automatic=true,expectedRevision=op.revision));vm.finishForward()
        }
        val exhausted=vm.messageForwards.getValue(owner);assertFalse(exhausted.canAutomaticallyRetry);assertTrue(exhausted.canRetry)
        assertFalse(vm.retryMessageForward(owner,exhausted.id,automatic=true,expectedRevision=exhausted.revision))
        assertTrue(vm.retryMessageForward(owner,exhausted.id));vm.finishForward()
        assertEquals(completed,vm.copies("maya-chen"));assertEquals(prefix,vm.copies("theo-grant").first());assertEquals(3,vm.copies("theo-grant").size)
    }

}
