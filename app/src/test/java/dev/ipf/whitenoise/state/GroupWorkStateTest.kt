package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class GroupWorkStateTest {
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun draft() = GroupEditDraft("Trail", "Trip details", ProfileAvatar.Asset(AvatarAsset.Fox), ProfileAvatar.Monogram)
    private fun create(vm: AppViewModel, timer: DisappearingDuration = DisappearingDuration.OneDay): GroupCreateWork {
        assertTrue(vm.groupWork.beginCreate(vm.uiState.activeProfileId!!, "setup", draft(), emptyList(), timer))
        return vm.groupWork.creation!!
    }
    private fun step(vm: AppViewModel) { vm.groupWork.creation!!.let { vm.groupWork.advanceCreate(it.id, it.phase) } }
    private fun group(vm: AppViewModel): GroupOwner {
        val member = vm.uiState.activeProfile!!.people.first { it.id != vm.uiState.activeProfileId && it.id != "white-noise-support" }.id
        val id = vm.createGroup("Trail", "Details", ProfileAvatar.Monogram, listOf(member))!!
        return GroupOwner(vm.uiState.activeProfileId!!, id)
    }
    private fun member(vm: AppViewModel, owner: GroupOwner) = vm.chat(owner.chatId)!!.members.first { it.personId != owner.profileId }.personId
    private fun outsider(vm: AppViewModel, owner: GroupOwner) = vm.uiState.activeProfile!!.people.first { it.id != owner.profileId && it.id != "white-noise-support" && vm.chat(owner.chatId)!!.members.none { m -> m.personId == it.id } }.id
    private fun memberStep(vm: AppViewModel, owner: GroupOwner) { vm.groupWork.memberWork[owner]!!.let { vm.groupWork.advanceMembers(owner, it.id, it.phase) } }

    @Test fun soloGroupIncludesOnlyCreatorAndAppliesInitialTimerBeforeOpeningOnce() {
        val vm = model(); val before = vm.uiState.activeProfile!!.chats.size; val request = create(vm)
        assertFalse(vm.groupWork.beginCreate(request.profileId, "setup", draft(), emptyList(), request.timer))
        step(vm); val id = vm.groupWork.creation!!.chatId!!
        assertEquals(listOf(GroupMember(request.profileId, GroupRole.Admin)), vm.chat(id)!!.members)
        assertEquals(DisappearingDuration.Off, vm.chat(id)!!.disappearingDuration)
        assertEquals(ProfileAvatar.Monogram, vm.chat(id)!!.publicInviteAvatar)
        step(vm); assertEquals(DisappearingDuration.OneDay, vm.chat(id)!!.disappearingDuration)
        step(vm); assertEquals(id, vm.groupWork.takeCreated(request.id, "setup")); assertNull(vm.groupWork.takeCreated(request.id, "setup"))
        assertEquals(before + 1, vm.uiState.activeProfile!!.chats.size)
    }
    @Test fun timerFailureKeepsCreatedGroupAndRetriesOnlyTheTimer() {
        val vm = model(); vm.groupWork.chooseCreate(GroupCreateScenario.TimerFailure); create(vm); step(vm); step(vm)
        val failed = vm.groupWork.creation!!; val count = vm.uiState.activeProfile!!.chats.size
        assertEquals(GroupCreatePhase.TimerFailed, failed.phase); assertNotNull(vm.chat(failed.chatId!!))
        vm.groupWork.retryCreate(failed.id); step(vm); step(vm)
        assertEquals(failed.chatId, vm.groupWork.creation!!.chatId); assertEquals(count, vm.uiState.activeProfile!!.chats.size)
        assertEquals(DisappearingDuration.OneDay, vm.chat(failed.chatId)!!.disappearingDuration)
        vm.groupWork.advanceCreate(failed.id, GroupCreatePhase.ApplyingTimer)
        assertEquals(GroupCreatePhase.Ready, vm.groupWork.creation!!.phase)
    }
    @Test fun timerFailureCanOpenTheUsableGroupWithoutClaimingTimerSuccess() {
        val vm = model(); vm.groupWork.chooseCreate(GroupCreateScenario.TimerFailure); create(vm); step(vm); step(vm)
        vm.groupWork.skipFailedTimer(vm.groupWork.creation!!.id); step(vm)
        val ready = vm.groupWork.creation!!; assertEquals(GroupCreatePhase.Ready, ready.phase); assertFalse(ready.timerApplied)
        assertEquals(DisappearingDuration.Off, vm.chat(ready.chatId!!)!!.disappearingDuration)
    }
    @Test fun failedOpenDoesNotRecreateOrReapplyAnAlreadySetTimer() {
        val vm = model(); vm.groupWork.chooseCreate(GroupCreateScenario.OpenFailure); create(vm); repeat(3) { step(vm) }
        val failed = vm.groupWork.creation!!; val chat = vm.chat(failed.chatId!!)!!
        assertEquals(GroupCreatePhase.OpenFailed, failed.phase)
        vm.groupWork.retryCreate(failed.id); step(vm)
        assertEquals(chat, vm.chat(chat.id)); assertEquals(GroupCreatePhase.Ready, vm.groupWork.creation!!.phase)
    }
    @Test fun failedCreationAndAbandonedCreatedOriginDoNotCreateDuplicates() {
        val vm = model(); val before = vm.uiState.activeProfile!!.chats.size
        vm.groupWork.chooseCreate(GroupCreateScenario.CreateFailure); create(vm); step(vm)
        assertEquals(before, vm.uiState.activeProfile!!.chats.size)
        vm.groupWork.retryCreate(vm.groupWork.creation!!.id); step(vm)
        val id = vm.groupWork.creation!!.chatId!!; vm.groupWork.leaveCreation("setup")
        assertNull(vm.groupWork.creation); assertNotNull(vm.chat(id))
    }
    @Test fun staleCreateCallbacksCannotRunAfterProfileExit() {
        val vm = model(); val work = create(vm); vm.completeSignIn(OnboardingOrigin.AddProfile)
        vm.selectProfile(work.profileId); vm.groupWork.advanceCreate(work.id, work.phase)
        assertNull(vm.groupWork.creation); assertTrue(vm.uiState.activeProfile!!.chats.none { it.title == "Trail" })
    }
    @Test fun warmRosterAllowsPresentationButNeverMutationBeforeReady() {
        val vm = model(); val owner = group(vm); val target = outsider(vm, owner)
        vm.groupWork.chooseRoster(GroupRosterScenario.WarmLoading); vm.groupWork.openRoster(owner)
        val chat = vm.chat(owner.chatId)!!
        assertTrue(chat.canPresentMemberAdministration(owner.profileId)); assertFalse(chat.hasAuthoritativeGroupAdmin(owner.profileId))
        assertFalse(vm.groupWork.beginMembers(owner, GroupMemberAction.Invite, listOf(target)))
        assertFalse(vm.addGroupMembers(owner.chatId, listOf(target)))
        vm.groupWork.advanceRoster(owner, vm.groupWork.rosterLoads[owner]!!.id)
        assertTrue(vm.groupWork.beginMembers(owner, GroupMemberAction.Invite, listOf(target)))
    }
    @Test fun unknownColdFailedAndInconsistentRosterBlockRoleCommandsAndRetryFreshly() {
        GroupRosterScenario.entries.filter { it !in setOf(GroupRosterScenario.Ready, GroupRosterScenario.WarmLoading) }.forEach { scenario ->
            val vm = model(); val owner = group(vm); vm.groupWork.chooseRoster(scenario); vm.groupWork.openRoster(owner)
            assertFalse(vm.chat(owner.chatId)!!.canPresentMemberAdministration(owner.profileId))
            vm.groupWork.rosterLoads[owner]?.let { vm.groupWork.advanceRoster(owner, it.id) }
            if (scenario != GroupRosterScenario.ColdLoading) {
                assertFalse(vm.setGroupMemberAdmin(owner.chatId, member(vm, owner), true))
                vm.groupWork.retryRoster(owner); vm.groupWork.advanceRoster(owner, vm.groupWork.rosterLoads[owner]!!.id)
            }
            assertTrue(vm.chat(owner.chatId)!!.hasAuthoritativeGroupAdmin(owner.profileId))
        }
    }
    @Test fun staleRosterCompletionCannotReplaceNewerRefresh() {
        val vm = model(); val owner = group(vm); vm.groupWork.chooseRoster(GroupRosterScenario.Failed); vm.groupWork.openRoster(owner)
        val old = vm.groupWork.rosterLoads[owner]!!; vm.groupWork.retryRoster(owner)
        vm.groupWork.advanceRoster(owner, old.id); assertEquals(GroupRosterStatus.Loading, vm.chat(owner.chatId)!!.groupRoster.status)
        vm.groupWork.advanceRoster(owner, vm.groupWork.rosterLoads[owner]!!.id); assertEquals(GroupRosterStatus.Ready, vm.chat(owner.chatId)!!.groupRoster.status)
    }
    @Test fun pendingInviteLocksDuplicateAndCompetingRoleCommandsThenConvergesOnce() {
        val vm = model(); val owner = group(vm); val target = outsider(vm, owner)
        assertTrue(vm.groupWork.beginMembers(owner, GroupMemberAction.Invite, listOf(target, target)))
        assertEquals(listOf(target), vm.groupWork.memberWork[owner]!!.personIds)
        assertFalse(vm.chat(owner.chatId)!!.members.any { it.personId == target })
        assertFalse(vm.groupWork.beginMembers(owner, GroupMemberAction.Invite, listOf(target)))
        assertFalse(vm.setGroupMemberAdmin(owner.chatId, member(vm, owner), true))
        memberStep(vm, owner); assertEquals(GroupWorkPhase.Converging, vm.groupWork.memberWork[owner]!!.phase)
        val chat = vm.chat(owner.chatId); memberStep(vm, owner); memberStep(vm, owner)
        assertEquals(chat, vm.chat(owner.chatId)); assertFalse(vm.groupWork.locked(owner))
    }
    @Test fun memberFailureRetainsTargetsAndRetryCannotBeOverwrittenByOldCompletion() {
        val vm = model(); val owner = group(vm); val target = member(vm, owner)
        vm.groupWork.chooseMutation(GroupMutationScenario.Failure); vm.groupWork.beginMembers(owner, GroupMemberAction.Promote, listOf(target)); memberStep(vm, owner)
        val old = vm.groupWork.memberWork[owner]!!; assertEquals(GroupWorkPhase.Failed, old.phase)
        assertTrue(vm.groupWork.retryMembers(owner, old.id)); val replacement = vm.groupWork.memberWork[owner]!!
        vm.groupWork.advanceMembers(owner, old.id, GroupWorkPhase.Applying); assertEquals(replacement, vm.groupWork.memberWork[owner])
        memberStep(vm, owner); assertEquals(GroupRole.Admin, vm.chat(owner.chatId)!!.members.first { it.personId == target }.role)
    }
    @Test fun rosterRefreshBetweenIntentAndCommitRejectsEvenTheSameMemberRows() {
        val vm = model(); val owner = group(vm); val target = member(vm, owner)
        vm.groupWork.beginMembers(owner, GroupMemberAction.Promote, listOf(target))
        vm.groupWork.retryRoster(owner); vm.groupWork.advanceRoster(owner, vm.groupWork.rosterLoads[owner]!!.id)
        memberStep(vm, owner)
        assertEquals(GroupWorkFailure.SourceChanged, vm.groupWork.memberWork[owner]!!.failure)
        assertEquals(GroupRole.Member, vm.chat(owner.chatId)!!.members.first { it.personId == target }.role)
    }
    @Test fun acceptedMemberChangeSurvivesProfileSwitchButUnacceptedChangeDoesNot() {
        val vm = model(); val owner = group(vm); val target = member(vm, owner)
        vm.groupWork.beginMembers(owner, GroupMemberAction.Promote, listOf(target)); memberStep(vm, owner)
        vm.completeSignIn(OnboardingOrigin.AddProfile); memberStep(vm, owner)
        assertEquals(GroupWorkPhase.Complete, vm.groupWork.memberWork[owner]!!.phase)
        vm.selectProfile(owner.profileId); vm.groupWork.beginMembers(owner, GroupMemberAction.Remove, listOf(target))
        val pending = vm.groupWork.memberWork[owner]!!; vm.completeSignIn(OnboardingOrigin.AddProfile); vm.selectProfile(owner.profileId)
        vm.groupWork.advanceMembers(owner, pending.id, pending.phase)
        assertEquals(GroupWorkFailure.Interrupted, vm.groupWork.memberWork[owner]!!.failure); assertTrue(vm.chat(owner.chatId)!!.members.any { it.personId == target })
    }
    @Test fun revokeAndRemovePreserveTheCreatorAndRevalidateTargets() {
        val vm = model(); val owner = group(vm); val target = member(vm, owner)
        assertFalse(vm.groupWork.beginMembers(owner, GroupMemberAction.Remove, listOf(owner.profileId)))
        assertFalse(vm.groupWork.beginMembers(owner, GroupMemberAction.Revoke, listOf(target)))
        vm.setGroupMemberAdmin(owner.chatId, target, true)
        vm.groupWork.beginMembers(owner, GroupMemberAction.Revoke, listOf(target)); repeat(2) { memberStep(vm, owner) }
        vm.groupWork.beginMembers(owner, GroupMemberAction.Remove, listOf(target)); repeat(2) { memberStep(vm, owner) }
        assertEquals(listOf(GroupMember(owner.profileId, GroupRole.Admin)), vm.chat(owner.chatId)!!.members)
    }
    @Test fun imageUploadFailureRetainsBothCommittedPreviewsAndRetryUsesPreparedDraft() {
        val vm = model(); val owner = group(vm); val base = GroupEditDraft.from(vm.chat(owner.chatId)!!)
        val draft = base.copy(image = ProfileAvatar.Asset(AvatarAsset.Fox), publicImage = ProfileAvatar.Asset(AvatarAsset.GardenClub))
        vm.groupWork.chooseImage(GroupImageScenario.UploadFailure); vm.groupWork.beginEdit(owner, base, draft)
        vm.groupWork.advanceEdit(owner, vm.groupWork.editWork[owner]!!.id)
        val failed = vm.groupWork.editWork[owner]!!; assertEquals(GroupWorkFailure.Upload, failed.failure); assertEquals(base, GroupEditDraft.from(vm.chat(owner.chatId)!!))
        assertTrue(vm.groupWork.retryEdit(owner, failed.id)); vm.groupWork.advanceEdit(owner, vm.groupWork.editWork[owner]!!.id)
        assertEquals(draft, GroupEditDraft.from(vm.chat(owner.chatId)!!))
    }
    @Test fun privatePhotoChangeCannotReplacePublicInvitationPreview() {
        val vm = model(); val owner = group(vm)
        vm.editGroup(owner.chatId, "Trail", "Details", ProfileAvatar.Monogram, ProfileAvatar.Asset(AvatarAsset.GardenClub))
        val base = GroupEditDraft.from(vm.chat(owner.chatId)!!); val draft = base.copy(image = ProfileAvatar.Asset(AvatarAsset.Fox))
        vm.groupWork.beginEdit(owner, base, draft); vm.groupWork.advanceEdit(owner, vm.groupWork.editWork[owner]!!.id)
        val chat = vm.chat(owner.chatId)!!; assertEquals(draft.image, chat.visibleAvatar)
        assertEquals(base.publicImage, chat.copy(membership = ChatMembership.Invited).visibleAvatar)
    }
    @Test fun staleGroupEditNeedsExplicitReviewRatherThanRebasingOnRetry() {
        val vm = model(); val owner = group(vm); val base = GroupEditDraft.from(vm.chat(owner.chatId)!!)
        vm.editGroup(owner.chatId, "Changed elsewhere", base.description, base.image)
        vm.groupWork.beginEdit(owner, base, base.copy(name = "My name")); vm.groupWork.advanceEdit(owner, vm.groupWork.editWork[owner]!!.id)
        assertEquals(GroupWorkFailure.SourceChanged, vm.groupWork.editWork[owner]!!.failure)
        assertFalse(vm.groupWork.retryEdit(owner, vm.groupWork.editWork[owner]!!.id))
        assertEquals("Changed elsewhere", vm.chat(owner.chatId)!!.title)
    }
    @Test fun rosterRefreshInvalidatesAnImageSaveUntilExplicitRetry() {
        val vm = model(); val owner = group(vm); val base = GroupEditDraft.from(vm.chat(owner.chatId)!!)
        vm.groupWork.beginEdit(owner, base, base.copy(name = "New name"))
        vm.groupWork.retryRoster(owner); vm.groupWork.advanceRoster(owner, vm.groupWork.rosterLoads[owner]!!.id)
        vm.groupWork.advanceEdit(owner, vm.groupWork.editWork[owner]!!.id)
        assertEquals(GroupWorkFailure.SourceChanged, vm.groupWork.editWork[owner]!!.failure)
        assertEquals(base.name, vm.chat(owner.chatId)!!.title)
        assertTrue(vm.groupWork.retryEdit(owner, vm.groupWork.editWork[owner]!!.id))
        vm.groupWork.advanceEdit(owner, vm.groupWork.editWork[owner]!!.id)
        assertEquals("New name", vm.chat(owner.chatId)!!.title)
    }
    @Test fun signingOutPrunesPendingMemberAndImageWork() {
        val vm = model(); val owner = group(vm); val other = group(vm)
        val base = GroupEditDraft.from(vm.chat(other.chatId)!!)
        vm.groupWork.beginEdit(other, base, base.copy(name = "Edited"))
        vm.groupWork.beginMembers(owner, GroupMemberAction.Promote, listOf(member(vm, owner)))
        val image = vm.groupWork.editWork[other]!!
        vm.signOutActiveProfile(false); assertTrue(vm.groupWork.memberWork.isEmpty()); assertTrue(vm.groupWork.editWork.isEmpty()); assertTrue(vm.groupWork.rosterLoads.isEmpty())
        vm.groupWork.advanceEdit(other, image.id)
        assertEquals(base.name, vm.uiState.retainedProfiles.single().chats.first { it.id == other.chatId }.title)
    }
    @Test fun failedMemberRetryExplainsATargetRemovedSinceTheFailure() {
        val vm = model(); val owner = group(vm); val target = member(vm, owner)
        vm.groupWork.chooseMutation(GroupMutationScenario.Failure)
        vm.groupWork.beginMembers(owner, GroupMemberAction.Promote, listOf(target)); memberStep(vm, owner)
        val work = vm.groupWork.memberWork[owner]!!
        assertTrue(vm.removeGroupMember(owner.chatId, target))
        assertFalse(vm.groupWork.canRetryMembers(owner, work.id)); assertFalse(vm.groupWork.retryMembers(owner, work.id))
        assertEquals(GroupWorkFailure.SourceChanged, vm.groupWork.memberWork[owner]!!.failure)
    }
    @Test fun imageRetryAfterNewMetadataRequiresReviewAndKeepsPreparedImage() {
        val vm = model(); val owner = group(vm); val base = GroupEditDraft.from(vm.chat(owner.chatId)!!)
        val draft = base.copy(image = ProfileAvatar.Asset(AvatarAsset.Fox))
        vm.groupWork.chooseImage(GroupImageScenario.UploadFailure); vm.groupWork.beginEdit(owner, base, draft)
        vm.groupWork.advanceEdit(owner, vm.groupWork.editWork[owner]!!.id)
        val work = vm.groupWork.editWork[owner]!!
        vm.editGroup(owner.chatId, "Changed", base.description, base.image)
        assertFalse(vm.groupWork.retryEdit(owner, work.id))
        assertEquals(GroupWorkFailure.SourceChanged, vm.groupWork.editWork[owner]!!.failure)
        assertEquals(draft, vm.groupWork.editWork[owner]!!.draft)
    }
    @Test fun invalidRecipientsCannotBecomeASoloGroupAndScenariosRequireDeveloperAccess() {
        val vm = model(); vm.setDeveloperToolsEnabled(false)
        vm.groupWork.chooseCreate(GroupCreateScenario.CreateFailure); assertEquals(GroupCreateScenario.Success, vm.groupWork.createScenario)
        assertFalse(vm.groupWork.beginCreate(vm.uiState.activeProfileId!!, "setup", draft(), listOf("missing"), DisappearingDuration.Off))
        assertNull(vm.createGroup("Bad", "", ProfileAvatar.Monogram, listOf("missing")))
    }
}
