package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class GroupLifecycleStateTest {
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun group(vm: AppViewModel, solo: Boolean = false): GroupOwner {
        val ids = if (solo) emptyList() else vm.uiState.activeProfile!!.people.filter { it.id != vm.uiState.activeProfileId && it.id != "white-noise-support" }.take(2).map { it.id }
        return GroupOwner(vm.uiState.activeProfileId!!, vm.createGroup("Trail", "", ProfileAvatar.Monogram, ids)!!)
    }
    private fun target(vm: AppViewModel, owner: GroupOwner) = vm.chat(owner.chatId)!!.members.first { it.personId != owner.profileId }.personId
    private fun step(vm: AppViewModel, owner: GroupOwner) { vm.groupLifecycle.work[owner]!!.let { vm.groupLifecycle.advance(owner, it.id, it.stage) } }
    private fun count(vm: AppViewModel, owner: GroupOwner) = vm.chat(owner.chatId)!!.members.count { it.role == GroupRole.Admin }
    private fun enable(vm: AppViewModel, owner: GroupOwner) { assertTrue(vm.groupLifecycle.begin(owner, GroupLifecycleAction.EnableDisband)); step(vm, owner) }

    @Test fun transferGrantsThenStepsDownWithoutEverRemovingLastAdmin() {
        val vm = model(); val owner = group(vm); val target = target(vm, owner)
        assertTrue(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, target)); assertEquals(1, count(vm, owner))
        step(vm, owner); assertEquals(2, count(vm, owner)); assertTrue(vm.groupWork.locked(owner))
        step(vm, owner); assertEquals(1, count(vm, owner)); assertFalse(vm.groupWork.locked(owner))
        assertEquals(GroupRole.Member, vm.chat(owner.chatId)!!.members.first { it.personId == owner.profileId }.role)
    }
    @Test fun partialStepDownRetryDoesNotRepeatGrant() {
        val vm = model(); val owner = group(vm); vm.groupLifecycle.choose(GroupLifecycleScenario.StepDownFailure)
        assertTrue(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, target(vm, owner)))
        step(vm, owner); step(vm, owner); val failed = vm.groupLifecycle.work[owner]!!
        assertTrue(failed.granted); assertEquals(GroupLifecycleStage.StepDown, failed.stage); assertEquals(2, count(vm, owner))
        assertTrue(vm.groupLifecycle.retry(owner, failed.id)); step(vm, owner)
        assertEquals(GroupLifecycleStage.Complete, vm.groupLifecycle.work[owner]!!.stage); assertEquals(1, count(vm, owner))
        vm.groupLifecycle.advance(owner, failed.id, failed.stage); assertEquals(1, count(vm, owner))
    }
    @Test fun transferAndLeaveFailureRetainsMemberThenRetriesLeaveOnly() {
        val vm = model(); val owner = group(vm); vm.groupLifecycle.choose(GroupLifecycleScenario.LeaveFailure)
        assertTrue(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, target(vm, owner), thenLeave = true))
        step(vm, owner); step(vm, owner); step(vm, owner)
        val failed = vm.groupLifecycle.work[owner]!!; assertTrue(failed.steppedDown); assertEquals(GroupLifecycleStage.Leave, failed.stage)
        assertEquals(ChatMembership.Active, vm.chat(owner.chatId)!!.membership)
        assertTrue(vm.groupLifecycle.retry(owner, failed.id)); step(vm, owner)
        assertEquals(ChatMembership.Left, vm.chat(owner.chatId)!!.membership); assertEquals(1, count(vm, owner))
        assertEquals(ComposerAvailability.Left, vm.composerAvailability(owner.chatId))
    }
    @Test fun grantFailureDoesNotMutateEitherRole() {
        val vm = model(); val owner = group(vm); val before = vm.chat(owner.chatId)!!.members
        vm.groupLifecycle.choose(GroupLifecycleScenario.GrantFailure); vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, target(vm, owner)); step(vm, owner)
        assertEquals(before, vm.chat(owner.chatId)!!.members); assertFalse(vm.groupLifecycle.work[owner]!!.granted)
    }
    @Test fun soleAdminCannotDirectlyStepDownOrLeaveWithOthers() {
        val vm = model(); val owner = group(vm)
        assertFalse(vm.groupLifecycle.begin(owner, GroupLifecycleAction.StepDown)); assertFalse(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Leave))
        assertFalse(vm.leaveChat(owner.chatId))
    }
    @Test fun adminLeaveStepsDownFirstAndExposesPartialFailure() {
        val vm = model(); val owner = group(vm); vm.setGroupMemberAdmin(owner.chatId, target(vm, owner), true)
        vm.groupLifecycle.choose(GroupLifecycleScenario.LeaveFailure); assertTrue(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Leave))
        step(vm, owner); assertEquals(1, count(vm, owner)); step(vm, owner)
        assertTrue(vm.groupLifecycle.work[owner]!!.steppedDown); assertEquals(ChatMembership.Active, vm.chat(owner.chatId)!!.membership)
    }
    @Test fun soleMemberDeletionRemovesOnlySelectedGroup() {
        val vm = model(); val owner = group(vm, solo = true); val before = vm.uiState.activeProfile!!.chats.map { it.id }
        assertTrue(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Delete)); step(vm, owner)
        assertNull(vm.chat(owner.chatId)); assertEquals(before - owner.chatId, vm.uiState.activeProfile!!.chats.map { it.id })
    }
    @Test fun soleMemberDeleteFailureLeavesHistoryAndAllowsRetry() {
        val vm = model(); val owner = group(vm, solo = true); vm.groupLifecycle.choose(GroupLifecycleScenario.DeleteFailure)
        vm.groupLifecycle.begin(owner, GroupLifecycleAction.Leave); step(vm, owner); assertNotNull(vm.chat(owner.chatId))
        assertTrue(vm.groupLifecycle.retry(owner, vm.groupLifecycle.work[owner]!!.id)); step(vm, owner); assertNull(vm.chat(owner.chatId))
    }
    @Test fun groupMemberAndEditOperationsShareAdministrationLock() {
        val vm = model(); val owner = group(vm); val target = target(vm, owner); val chat = vm.chat(owner.chatId)!!
        assertTrue(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, target))
        assertFalse(vm.groupWork.beginMembers(owner, GroupMemberAction.Remove, listOf(target)))
        assertFalse(vm.groupWork.beginEdit(owner, GroupEditDraft.from(chat), GroupEditDraft.from(chat).copy(name = "Changed")))
        assertFalse(vm.setGroupMemberAdmin(owner.chatId, target, true)); assertFalse(vm.setChatDisappearing(owner.chatId, DisappearingDuration.OneDay))
    }
    @Test fun administrationCannotOverlapPendingMemberCommit() {
        val vm = model(); val owner = group(vm); val target = target(vm, owner)
        assertTrue(vm.groupWork.beginMembers(owner, GroupMemberAction.Promote, listOf(target)))
        assertFalse(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, target))
    }
    @Test fun rosterChangeRejectsPendingTransferBeforeGrant() {
        val vm = model(); val owner = group(vm); vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, target(vm, owner))
        vm.groupWork.chooseRoster(GroupRosterScenario.ColdLoading); vm.groupWork.openRoster(owner); step(vm, owner)
        assertEquals(GroupLifecycleFailure.SourceChanged, vm.groupLifecycle.work[owner]!!.failure); assertEquals(1, count(vm, owner))
    }
    @Test fun staleGrantCannotBeRetriedAfterTargetWasRemoved() {
        val vm = model(); val owner = group(vm); val target = target(vm, owner)
        vm.groupLifecycle.choose(GroupLifecycleScenario.StepDownFailure); vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, target)
        step(vm, owner); step(vm, owner); val failed = vm.groupLifecycle.work[owner]!!
        assertTrue(vm.removeGroupMember(owner.chatId, target)); assertFalse(vm.groupLifecycle.canRetry(owner, failed.id)); assertFalse(vm.groupLifecycle.retry(owner, failed.id))
        assertEquals(1, count(vm, owner))
    }
    @Test fun profileRoundTripInterruptsTransferButKeepsAcceptedGrant() {
        val vm = model(); val owner = group(vm); vm.groupLifecycle.begin(owner, GroupLifecycleAction.Transfer, target(vm, owner)); step(vm, owner)
        val pending = vm.groupLifecycle.work[owner]!!; vm.completeSignIn(OnboardingOrigin.AddProfile); vm.selectProfile(owner.profileId)
        vm.groupLifecycle.advance(owner, pending.id, pending.stage); assertEquals(2, count(vm, owner))
        assertEquals(GroupLifecycleFailure.Interrupted, vm.groupLifecycle.work[owner]!!.failure)
        assertTrue(vm.groupLifecycle.retry(owner, pending.id)); step(vm, owner); assertEquals(1, count(vm, owner))
    }
    @Test fun disbandMustBeEnabledAndBlocksAllOutboundAtAcceptance() {
        val vm = model(); val owner = group(vm)
        assertFalse(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Disband)); enable(vm, owner)
        assertTrue(vm.sendText(owner.chatId, "Before ending")); val message = vm.chat(owner.chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message
        assertTrue(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Disband)); step(vm, owner)
        assertEquals(ComposerAvailability.Disbanding, vm.composerAvailability(owner.chatId))
        assertFalse(vm.sendText(owner.chatId, "Too late")); assertFalse(vm.setMessageReaction(owner.chatId, message.id, "👍", true))
        assertFalse(vm.beginMessageEdit(owner.profileId, owner.chatId, message.id, "Edit"))
        step(vm, owner); assertEquals(GroupLifecycle.Disbanded, vm.chat(owner.chatId)!!.groupLifecycle)
        assertEquals(message.text, vm.chat(owner.chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().last().message.text)
    }
    @Test fun terminalStateWinsOverInvitationAndUnknownRoster() {
        val vm = model(); val owner = group(vm); val profile = vm.uiState.activeProfile!!
        val chat = vm.chat(owner.chatId)!!.copy(groupLifecycle = GroupLifecycle.Disbanded, membership = ChatMembership.Invited,
            groupRoster = GroupRoster(GroupRosterStatus.Unknown))
        assertEquals(ComposerAvailability.Disbanded, chat.composerAvailability(profile))
    }
    @Test fun terminalStateSurvivesProfileSwitchAndNewOperationScenario() {
        val vm = model(); val owner = group(vm); enable(vm, owner); vm.groupLifecycle.begin(owner, GroupLifecycleAction.Disband); step(vm, owner); step(vm, owner)
        vm.completeSignIn(OnboardingOrigin.AddProfile); vm.selectProfile(owner.profileId)
        vm.groupLifecycle.choose(GroupLifecycleScenario.Success); vm.groupLifecycle.open(owner)
        assertEquals(ComposerAvailability.Disbanded, vm.composerAvailability(owner.chatId))
    }
    @Test fun acceptedDisbandConvergesInItsOriginalInactiveProfile() {
        val vm = model(); val owner = group(vm); enable(vm, owner); vm.groupLifecycle.begin(owner, GroupLifecycleAction.Disband); step(vm, owner)
        vm.completeSignIn(OnboardingOrigin.AddProfile); val active = vm.uiState.activeProfileId
        step(vm, owner); assertEquals(active, vm.uiState.activeProfileId)
        assertEquals(GroupLifecycle.Disbanded, vm.uiState.profiles.first { it.id == owner.profileId }.chats.first { it.id == owner.chatId }.groupLifecycle)
    }
    @Test fun failedConvergenceRequiresAcknowledgmentBeforeRetry() {
        val vm = model(); val owner = group(vm); enable(vm, owner); vm.groupLifecycle.choose(GroupLifecycleScenario.ConvergenceFailure)
        vm.groupLifecycle.begin(owner, GroupLifecycleAction.Disband); step(vm, owner); step(vm, owner)
        assertTrue(vm.chat(owner.chatId)!!.disbandCapability.requestFailed); assertFalse(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Disband))
        vm.groupLifecycle.choose(GroupLifecycleScenario.AcknowledgeFailure); vm.groupLifecycle.begin(owner, GroupLifecycleAction.Acknowledge); step(vm, owner)
        assertTrue(vm.chat(owner.chatId)!!.disbandCapability.requestFailed)
        assertTrue(vm.groupLifecycle.retry(owner, vm.groupLifecycle.work[owner]!!.id)); step(vm, owner)
        assertFalse(vm.chat(owner.chatId)!!.disbandCapability.requestFailed)
        vm.groupLifecycle.choose(GroupLifecycleScenario.Success); assertTrue(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Disband)); step(vm, owner); step(vm, owner)
        assertEquals(GroupLifecycle.Disbanded, vm.chat(owner.chatId)!!.groupLifecycle)
    }
    @Test fun authoritativeCapabilityBlockersPreventEnableAndDisband() {
        val vm = model(); val owner = group(vm)
        for (scenario in listOf(GroupStateScenario.Unsupported, GroupStateScenario.PendingInvitations, GroupStateScenario.CapabilityUnavailable)) {
            vm.groupLifecycle.chooseState(scenario); vm.groupLifecycle.open(owner)
            assertFalse(vm.groupLifecycle.begin(owner, GroupLifecycleAction.EnableDisband)); assertFalse(vm.groupLifecycle.begin(owner, GroupLifecycleAction.Disband))
        }
    }
    @Test fun frozenRecoveryMustCompleteBeforeMessagingResumes() {
        val vm = model(); val owner = group(vm); vm.groupLifecycle.chooseState(GroupStateScenario.Frozen); vm.groupLifecycle.open(owner)
        assertEquals(ComposerAvailability.Unrecoverable, vm.composerAvailability(owner.chatId)); assertFalse(vm.sendText(owner.chatId, "No"))
        vm.groupLifecycle.choose(GroupLifecycleScenario.RecoveryFailure); vm.groupLifecycle.begin(owner, GroupLifecycleAction.Recover); step(vm, owner)
        assertEquals(GroupLifecycle.Unrecoverable, vm.chat(owner.chatId)!!.groupLifecycle)
        vm.groupLifecycle.retry(owner, vm.groupLifecycle.work[owner]!!.id); step(vm, owner); assertTrue(vm.sendText(owner.chatId, "Recovered"))
    }
    @Test fun unknownRosterBlocksMessagingButWarmMemberSeedCanKeepItAvailable() {
        val vm = model(); val owner = group(vm); val profile = vm.uiState.activeProfile!!; val chat = vm.chat(owner.chatId)!!
        for (status in listOf(GroupRosterStatus.Unknown, GroupRosterStatus.Loading, GroupRosterStatus.Failed, GroupRosterStatus.Inconsistent)) {
            assertEquals(ComposerAvailability.MembershipUnknown, chat.copy(groupRoster = GroupRoster(status)).composerAvailability(profile))
        }
        val warm = chat.copy(groupRoster = GroupRoster(GroupRosterStatus.Loading, seededSelfMember = true))
        assertEquals(ComposerAvailability.Available, warm.composerAvailability(profile)); assertFalse(warm.hasAuthoritativeGroupAdmin(profile.id))
    }
    @Test fun localDeleteEndedGroupPreservesOtherProfileAndNeverDisbandsAnotherGroup() {
        val vm = model(); val owner = group(vm); vm.groupLifecycle.chooseState(GroupStateScenario.Ended); vm.groupLifecycle.open(owner)
        val before = vm.uiState.profiles; vm.groupLifecycle.begin(owner, GroupLifecycleAction.Delete); step(vm, owner)
        assertNull(vm.chat(owner.chatId)); assertEquals(before.filter { it.id != owner.profileId }, vm.uiState.profiles.filter { it.id != owner.profileId })
    }
    @Test fun soleDeletionAlsoRemovesManualFolderAssignment() {
        val vm = model(); val owner = group(vm, solo = true)
        val folder = vm.createChatFolder(owner.profileId, "Temporary")!!; vm.assignChatFolder(owner.profileId, owner.chatId, folder)
        vm.groupLifecycle.begin(owner, GroupLifecycleAction.Delete); step(vm, owner)
        assertFalse(vm.uiState.activeProfile!!.chatFolders.first { it.id == folder }.chatIds.contains(owner.chatId))
    }
    @Test fun endedGroupOffersLocalDeleteWithoutAnImpossibleLeaveFirst() {
        val vm = model(); val owner = group(vm); vm.groupLifecycle.chooseState(GroupStateScenario.Ended); vm.groupLifecycle.open(owner)
        val chat = vm.chat(owner.chatId)!!
        assertFalse(ChatOrganization.requiresLeave(chat, owner.profileId)); assertFalse(ChatOrganization.requiresAdmin(chat, owner.profileId))
        assertFalse(ChatListActionPolicy.all(chat).contains(ChatListAction.Leave))
        assertEquals(1, ChatListActionPolicy.all(chat).count { it == ChatListAction.Delete })
        assertTrue(vm.deleteEndedChat(owner.chatId)); assertNull(vm.chat(owner.chatId))
    }

}
