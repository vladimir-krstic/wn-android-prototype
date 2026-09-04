package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class RetentionStateTest {
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun group(vm: AppViewModel) = GroupOwner(vm.uiState.activeProfileId!!, vm.createGroup("Trail", "", ProfileAvatar.Monogram, listOf("maya-chen"))!!)
    private fun step(vm: AppViewModel, owner: GroupOwner) { vm.retention.work.getValue(owner).let { vm.retention.advance(owner, it.id, it.phase) } }
    private fun set(vm: AppViewModel, owner: GroupOwner, value: DisappearingDuration) {
        assertTrue(vm.retention.begin(owner, value)); val w = vm.retention.work.getValue(owner)
        if (w.phase == RetentionPhase.Confirm) assertTrue(vm.retention.confirm(owner, w.id))
        step(vm, owner); step(vm, owner)
    }
    private fun send(vm: AppViewModel, owner: GroupOwner, text: String = "A retained message"): ChatMessage {
        assertTrue(vm.sendText(owner.chatId, text)); return vm.chat(owner.chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
    }
    private fun incoming(vm: AppViewModel, owner: GroupOwner): ChatMessage {
        vm.retention.chooseExample(RetentionExample.Waiting); vm.retention.open(owner)
        return vm.chat(owner.chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
    }
    @Test fun enablingStagesDestructiveConfirmationAndCancelPreservesHistory() {
        val vm = model(); val owner = group(vm); send(vm, owner); vm.retention.advanceExampleClock(60_000)
        val before = vm.chat(owner.chatId)!!; assertTrue(vm.retention.begin(owner, DisappearingDuration.ThirtySeconds))
        val w = vm.retention.work.getValue(owner); assertEquals(RetentionPhase.Confirm, w.phase); assertEquals(1, w.pruneIds.size)
        vm.retention.dismiss(owner, w.id); assertEquals(before, vm.chat(owner.chatId))
        assertFalse(vm.retention.confirm(owner, w.id))
    }
    @Test fun confirmedShortWindowRemovesOldMessagesAndKeepsEvents() {
        val vm = model(); val owner = group(vm); val old = send(vm, owner); vm.retention.advanceExampleClock(60_000)
        set(vm, owner, DisappearingDuration.ThirtySeconds)
        assertNull(vm.message(owner.chatId, old.id)); assertTrue(vm.chat(owner.chatId)!!.timeline.any { it is ChatTimelineEntry.Event })
        assertEquals(DisappearingDuration.ThirtySeconds, vm.chat(owner.chatId)!!.disappearingDuration)
    }
    @Test fun disablingAndLengtheningDoNotRequestPruning() {
        val vm = model(); val owner = group(vm); set(vm, owner, DisappearingDuration.ThirtySeconds)
        assertTrue(vm.retention.begin(owner, DisappearingDuration.OneHour)); assertEquals(RetentionPhase.Applying, vm.retention.work.getValue(owner).phase)
        step(vm, owner); step(vm, owner); assertTrue(vm.retention.begin(owner, DisappearingDuration.Off))
        assertEquals(RetentionPhase.Applying, vm.retention.work.getValue(owner).phase)
    }
    @Test fun existingDeadlinesSurviveOffAndExpireAtOriginalBoundary() {
        val vm = model(); val owner = group(vm); set(vm, owner, DisappearingDuration.ThirtySeconds)
        val message = send(vm, owner); set(vm, owner, DisappearingDuration.Off)
        assertEquals(message.retention, vm.message(owner.chatId, message.id)!!.retention)
        vm.retention.advanceExampleClock(29_999); assertNotNull(vm.message(owner.chatId, message.id))
        vm.retention.advanceExampleClock(1); assertNull(vm.message(owner.chatId, message.id))
        assertEquals(DisappearingDuration.Off, vm.chat(owner.chatId)!!.disappearingDuration)
    }
    @Test fun prePolicyHistoryNotPrunedAtChangeNeverBorrowsCurrentTimer() {
        val vm = model(); val owner = group(vm); val old = send(vm, owner)
        vm.retention.advanceExampleClock(20_000); set(vm, owner, DisappearingDuration.ThirtySeconds); vm.retention.advanceExampleClock(60_000)
        assertNotNull(vm.message(owner.chatId, old.id)); assertNull(vm.message(owner.chatId, old.id)!!.retention)
    }
    @Test fun applyFailurePreservesPolicyAndHistoryAndRetriesWithConsent() {
        val vm = model(); val owner = group(vm); send(vm, owner); vm.retention.advanceExampleClock(60_000)
        val before = vm.chat(owner.chatId)!!; vm.retention.choose(RetentionScenario.ApplyFailure)
        vm.retention.begin(owner, DisappearingDuration.ThirtySeconds); vm.retention.confirm(owner, vm.retention.work.getValue(owner).id); step(vm, owner)
        val failed = vm.retention.work.getValue(owner); assertEquals(RetentionFailure.Unavailable, failed.failure); assertEquals(before, vm.chat(owner.chatId))
        assertTrue(vm.retention.retry(owner, failed.id)); assertEquals(RetentionPhase.Confirm, vm.retention.work.getValue(owner).phase)
        vm.retention.confirm(owner, vm.retention.work.getValue(owner).id); step(vm, owner); assertEquals(DisappearingDuration.ThirtySeconds, vm.chat(owner.chatId)!!.disappearingDuration)
    }
    @Test fun refreshFailureRetainsAcceptedTimerAndRetryNeverRepeatsCommit() {
        val vm = model(); val owner = group(vm); send(vm, owner); vm.retention.advanceExampleClock(60_000)
        vm.retention.choose(RetentionScenario.RefreshFailure); set(vm, owner, DisappearingDuration.ThirtySeconds)
        val failed = vm.retention.work.getValue(owner); val accepted = vm.chat(owner.chatId)!!
        assertEquals(RetentionPhase.RefreshFailed, failed.phase); assertEquals(DisappearingDuration.ThirtySeconds, accepted.disappearingDuration)
        assertTrue(vm.retention.retry(owner, failed.id)); step(vm, owner)
        assertEquals(accepted, vm.chat(owner.chatId)); assertEquals(RetentionPhase.Complete, vm.retention.work.getValue(owner).phase)
    }
    @Test fun staleConsentCannotOverwriteAnotherTimerChange() {
        val vm = model(); val owner = group(vm); vm.retention.begin(owner, DisappearingDuration.ThirtySeconds)
        val request = vm.retention.work.getValue(owner); assertTrue(vm.setChatDisappearing(owner.chatId, DisappearingDuration.OneDay))
        assertFalse(vm.retention.confirm(owner, request.id)); assertEquals(RetentionFailure.SourceChanged, vm.retention.work.getValue(owner).failure)
    }
    @Test fun refreshedRosterInvalidatesConsentAndUnknownCannotSubmit() {
        val vm = model(); val owner = group(vm); vm.retention.begin(owner, DisappearingDuration.OneDay); val w = vm.retention.work.getValue(owner)
        vm.groupWork.chooseRoster(GroupRosterScenario.ColdLoading); vm.groupWork.openRoster(owner)
        assertFalse(vm.retention.confirm(owner, w.id)); assertFalse(vm.retention.begin(owner, DisappearingDuration.OneWeek))
    }
    @Test fun nonAdminAndEndedGroupsCannotChangeTimer() {
        val vm = model(); val owner = group(vm); vm.setGroupMemberAdmin(owner.chatId, "maya-chen", true)
        vm.groupLifecycle.begin(owner, GroupLifecycleAction.StepDown)
        vm.groupLifecycle.work.getValue(owner).let { vm.groupLifecycle.advance(owner, it.id, it.stage) }
        assertFalse(vm.retention.begin(owner, DisappearingDuration.OneDay)); assertFalse(vm.setChatDisappearing(owner.chatId, DisappearingDuration.OneDay))
        vm.groupLifecycle.chooseState(GroupStateScenario.Ended); vm.groupLifecycle.open(owner)
        assertFalse(vm.retention.begin(owner, DisappearingDuration.OneDay))
    }
    @Test fun retentionAndAdministrationShareTheSameCommitLock() {
        val vm = model(); val owner = group(vm); vm.retention.begin(owner, DisappearingDuration.OneDay)
        vm.retention.confirm(owner, vm.retention.work.getValue(owner).id)
        assertFalse(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, "maya-chen"))
        assertFalse(vm.groupWork.beginMembers(owner, GroupMemberAction.Remove, listOf("maya-chen")))
        assertFalse(vm.setChatDisappearing(owner.chatId, DisappearingDuration.OneWeek))
    }
    @Test fun memberMutationPreventsTimerRequest() {
        val vm = model(); val owner = group(vm); assertTrue(vm.groupWork.beginMembers(owner, GroupMemberAction.Promote, listOf("maya-chen")))
        assertFalse(vm.retention.begin(owner, DisappearingDuration.OneDay))
    }
    @Test fun profileRoundTripCannotCompleteAnOldTimerRequest() {
        val vm = model(); val owner = group(vm); vm.retention.begin(owner, DisappearingDuration.OneDay)
        vm.retention.confirm(owner, vm.retention.work.getValue(owner).id); val w = vm.retention.work.getValue(owner)
        vm.completeSignIn(OnboardingOrigin.AddProfile); vm.selectProfile(owner.profileId); vm.retention.advance(owner, w.id, w.phase)
        assertEquals(DisappearingDuration.Off, vm.chat(owner.chatId)!!.disappearingDuration)
        assertEquals(RetentionFailure.Interrupted, vm.retention.work.getValue(owner).failure)
    }
    @Test fun unreadReceivedMessageWaitsUntilVisibleThenUsesFirstReadOnly() {
        val vm = model(); val owner = group(vm); val incoming = incoming(vm, owner)
        vm.retention.advanceExampleClock(60_000); assertNotNull(vm.message(owner.chatId, incoming.id))
        assertTrue(vm.markConversationVisible(owner.profileId, owner.chatId, setOf(incoming.id)))
        val deadline = MessageRetentionPolicy.deadline(vm.message(owner.chatId, incoming.id)!!)
        vm.retention.advanceExampleClock(20_000); vm.markConversationVisible(owner.profileId, owner.chatId, setOf(incoming.id))
        assertEquals(deadline, MessageRetentionPolicy.deadline(vm.message(owner.chatId, incoming.id)!!))
        vm.retention.advanceExampleClock(10_000); assertNull(vm.message(owner.chatId, incoming.id))
    }
    @Test fun markUnreadDoesNotStartOrResetRetentionAndExplicitMarkReadAnchorsIt() {
        val vm = model(); val owner = group(vm); val incoming = incoming(vm, owner)
        vm.markChatUnread(owner.chatId, true); assertNull(vm.message(owner.chatId, incoming.id)!!.retention!!.readAtMillis)
        vm.markChatUnread(owner.chatId, false); val anchor = vm.message(owner.chatId, incoming.id)!!.retention!!.readAtMillis
        assertNotNull(anchor); vm.retention.advanceExampleClock(10_000); vm.markChatUnread(owner.chatId, true); vm.markChatUnread(owner.chatId, false)
        assertEquals(anchor, vm.message(owner.chatId, incoming.id)!!.retention!!.readAtMillis)
    }
    @Test fun expiryClearsReplyReferenceButPreservesUnrelatedUnsentText() {
        val vm = model(); val owner = group(vm); set(vm, owner, DisappearingDuration.ThirtySeconds); val message = send(vm, owner)
        vm.setDraftReply(owner.chatId, message.id); vm.updateDraftText(owner.chatId, "Keep this draft")
        vm.retention.advanceExampleClock(30_000)
        assertNull(vm.chat(owner.chatId)!!.draftReplyMessageId); assertEquals("Keep this draft", vm.chat(owner.chatId)!!.draftText)
    }
    @Test fun expiredSourceStopsPendingForwardAndInvalidatesTranscript() {
        val vm = model(); val owner = group(vm); set(vm, owner, DisappearingDuration.ThirtySeconds); val message = send(vm, owner)
        assertTrue(vm.beginMessageForward(owner.profileId, owner.chatId, setOf(message.id), owner.profileId, listOf("fiatjaf")))
        vm.transcript.begin(owner); val export = vm.transcript.work!!; vm.retention.advanceExampleClock(30_000)
        assertFalse(vm.messageForwards.getValue(owner.profileId).isRunning)
        assertEquals(MessageForwardFailure.Expired, vm.messageForwards.getValue(owner.profileId).targets.single().failure)
        assertEquals(TranscriptFailure.SourceUnavailable, vm.transcript.work!!.failure); assertNull(vm.transcript.takeForWriting(export.id))
    }
    @Test fun expiryInInactiveProfileNeverSwitchesTheCurrentProfile() {
        val vm = model(); val owner = group(vm); set(vm, owner, DisappearingDuration.ThirtySeconds); val message = send(vm, owner)
        vm.completeSignIn(OnboardingOrigin.AddProfile); val active = vm.uiState.activeProfileId
        repeat(30) { vm.retention.tick(vm.retention.nowMillis) }
        assertEquals(active, vm.uiState.activeProfileId)
        assertFalse(vm.uiState.profiles.first { it.id == owner.profileId }.chats.first { it.id == owner.chatId }.timeline.any { it.id == message.id })
    }
    @Test fun duplicateClockCallbacksCannotCountTheSameSecondTwice() {
        val vm = model(); val now = vm.retention.nowMillis
        vm.retention.tick(now); vm.retention.tick(now); assertEquals(now + 1_000, vm.retention.nowMillis)
    }
    @Test fun customInitialTimerSurvivesCreateAndOpenStages() {
        val vm = model(); val duration = CustomRetentionInput("3", RetentionUnit.Months).duration!!
        assertTrue(vm.groupWork.beginCreate(vm.uiState.activeProfileId!!, "setup", GroupEditDraft("Trail", "", ProfileAvatar.Monogram, ProfileAvatar.Monogram), emptyList(), duration))
        repeat(3) { vm.groupWork.creation!!.let { vm.groupWork.advanceCreate(it.id, it.phase) } }
        assertEquals(duration, vm.chat(vm.groupWork.creation!!.chatId!!)!!.disappearingDuration)
    }
    @Test fun forwardedCopyCapturesItsDestinationsTimerAndIndependentDeadline() {
        val vm = model(); val source = group(vm); val target = group(vm)
        set(vm, source, DisappearingDuration.ThirtySeconds); set(vm, target, DisappearingDuration.OneHour)
        val original = send(vm, source); vm.retention.advanceExampleClock(10_000)
        assertTrue(vm.beginMessageForward(source.profileId, source.chatId, setOf(original.id), target.profileId, listOf(target.chatId)))
        var steps = 0
        while (vm.messageForwards.getValue(source.profileId).isRunning) {
            val op = vm.messageForwards.getValue(source.profileId)
            assertTrue(vm.advanceMessageForward(source.profileId, op.id, op.revision)); check(++steps < 20)
        }
        val copy = vm.chat(target.chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().single().message
        assertEquals(3_600L, copy.retention!!.durationSeconds)
        assertEquals(vm.retention.nowMillis + 3_600_000L, MessageRetentionPolicy.deadline(copy))
        vm.retention.advanceExampleClock(20_000)
        assertNull(vm.message(source.chatId, original.id)); assertNotNull(vm.message(target.chatId, copy.id))
    }
    @Test fun markAllReadAnchorsVisibleChatScopesButLeavesArchivedMessagesWaiting() {
        val vm = model(); val visible = group(vm); val archived = group(vm)
        val read = incoming(vm, visible); val waiting = incoming(vm, archived)
        vm.setChatArchived(archived.chatId, true); vm.markAllChatsRead()
        assertNotNull(vm.message(visible.chatId, read.id)!!.retention!!.readAtMillis)
        assertNull(vm.message(archived.chatId, waiting.id)!!.retention!!.readAtMillis)
    }

}
