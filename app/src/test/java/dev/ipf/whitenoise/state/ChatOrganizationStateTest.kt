package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class ChatOrganizationStateTest {
    private fun signedIn() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
    private fun AppViewModel.finish() {
        repeat(80) {
            val a = chatBatchAttempt ?: return
            if (!a.isBusy) return
            assertTrue(advanceChatBatch(a.id, a.index, a.phase))
        }
        fail("Chat batch did not finish")
    }
    private fun AppViewModel.direct(): String = openOrCreateDirectChat(uiState.activeProfile!!.people.first().id)!!
    private fun AppViewModel.group(): String = createGroup("Test group", "", ProfileAvatar.Monogram, listOf(uiState.activeProfile!!.people.first().id))!!

    @Test fun pinAppendMoveUnpinAndArchiveKeepUnpinnedOrder() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!
        val available = vm.uiState.activeProfile!!.chats.filter { !it.isArchived && !it.isPinned }.take(2)
        val original = available.map { it.originalOrder }
        available.forEach { vm.toggleChatPin(it.id) }
        assertEquals(available.map { it.id }, ChatOrganization.pinned(vm.uiState.activeProfile!!.chats).takeLast(2).map { it.id })
        vm.movePinnedChat(owner, available.last().id, -1)
        vm.toggleChatPin(available.first().id)
        vm.setChatArchived(available.last().id, true)
        assertEquals(original, available.map { vm.chat(it.id)!!.originalOrder })
        assertTrue(available.none { vm.chat(it.id)!!.isPinned })
    }
    @Test fun partialBulkRetriesOnlyFailuresAndPreservesCompletedMutation() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!
        val ids = vm.uiState.activeProfile!!.chats.take(3).map { it.id }
        vm.selectChatBatchScenario(ChatBatchScenario.PartialApply)
        assertTrue(vm.beginChatBatch(owner, ids, ChatBulkAction.Archive)); vm.finish()
        assertEquals(listOf(ids[1]), vm.chatBatchAttempt!!.failedIds)
        assertTrue(vm.chat(ids[0])!!.isArchived)
        vm.setChatArchived(ids[0], false)
        assertTrue(vm.retryChatBatch()); vm.finish()
        assertFalse(vm.chat(ids[0])!!.isArchived)
        assertTrue(vm.chat(ids[1])!!.isArchived)
        assertTrue(vm.chatBatchAttempt!!.failedIds.isEmpty())
    }
    @Test fun bulkReadAndUnreadAreExplicitAndIdempotent() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!; val ids = listOf(vm.direct())
        listOf(ChatBulkAction.Read, ChatBulkAction.Read, ChatBulkAction.Unread).forEach { action ->
            vm.beginChatBatch(owner, ids, action); vm.finish()
            assertEquals(action == ChatBulkAction.Unread, vm.chat(ids.single())!!.isUnread)
        }
    }
    @Test fun localOnlyDeleteDoesNotRequireSoleAdminLeave() {
        val vm = signedIn(); val id = vm.group(); val owner = vm.uiState.activeProfileId!!
        assertTrue(vm.chat(id)!!.isSoleAdmin(owner))
        vm.selectChatBatchScenario(ChatBatchScenario.LeaveFailure)
        vm.beginChatBatch(owner, listOf(id), ChatBulkAction.Delete); vm.finish()
        assertNull(vm.chat(id)); assertTrue(vm.chatBatchAttempt!!.failedIds.isEmpty())
    }
    @Test fun leaveFailureKeepsHistoryAndRetryCanComplete() {
        val vm = signedIn(); val id = vm.direct(); val owner = vm.uiState.activeProfileId!!; val before = vm.chat(id)!!
        vm.selectChatBatchScenario(ChatBatchScenario.LeaveFailure)
        vm.beginChatBatch(owner, listOf(id), ChatBulkAction.Delete, leaveFirst = true); vm.finish()
        assertEquals(ChatBatchFailure.LeaveFailed, vm.chatBatchAttempt!!.results.single().failure)
        assertEquals(before, vm.chat(id))
        vm.retryChatBatch(); vm.finish(); assertNull(vm.chat(id))
    }
    @Test fun deleteFailureAfterLeaveKeepsLeftHistoryAndRetrySkipsLeave() {
        val vm = signedIn(); val id = vm.direct(); val owner = vm.uiState.activeProfileId!!
        vm.selectChatBatchScenario(ChatBatchScenario.DeleteFailure)
        vm.beginChatBatch(owner, listOf(id), ChatBulkAction.Delete, leaveFirst = true); vm.finish()
        assertEquals(ChatMembership.Left, vm.chat(id)!!.membership)
        assertTrue(vm.chatBatchAttempt!!.results.single().leftBeforeDeletion)
        vm.selectChatBatchScenario(ChatBatchScenario.LeaveFailure)
        vm.retryChatBatch(); vm.finish()
        assertNull(vm.chat(id))
    }
    @Test fun soleAdminCannotLeaveAndDeleteUntilAnotherAdminExists() {
        val vm = signedIn(); val id = vm.group(); val owner = vm.uiState.activeProfileId!!; val before = vm.chat(id)!!
        vm.beginChatBatch(owner, listOf(id), ChatBulkAction.Delete, leaveFirst = true); vm.finish()
        assertEquals(ChatBatchFailure.NeedsAdmin, vm.chatBatchAttempt!!.results.single().failure)
        assertEquals(before, vm.chat(id))
        val person = before.members.first { it.personId != owner }.personId
        vm.applyContactToGroups(owner, person, listOf(id), GroupContactAction.Promote)
        vm.retryChatBatch(); vm.finish(); assertNull(vm.chat(id))
    }
    @Test fun stalePhaseDuplicateCompletionAndOwnerCannotMutateChats() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!; val id = vm.direct()
        vm.beginChatBatch(owner, listOf(id), ChatBulkAction.Delete, leaveFirst = true)
        val a = vm.chatBatchAttempt!!
        assertFalse(vm.advanceChatBatch(a.id + 1, 0, a.phase))
        assertTrue(vm.advanceChatBatch(a.id, 0, a.phase))
        assertFalse(vm.advanceChatBatch(a.id, 0, a.phase))
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertFalse(vm.advanceChatBatch(a.id, 0, ChatBatchPhase.Leaving))
        vm.selectProfile(owner)
        assertNotNull(vm.chat(id)); assertNull(vm.chatBatchAttempt)
        assertFalse(vm.beginChatBatch("another-owner", listOf(id), ChatBulkAction.Read))
    }
    @Test fun folderCreationAssignmentAndDeletionAreOwnedAndDeduplicated() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!; val id = vm.direct()
        assertNull(vm.createChatFolder(owner, "  ")); assertNull(vm.createChatFolder("other", "Name"))
        val folder = vm.createChatFolder(owner, "  Friends  ")!!
        repeat(2) { vm.beginChatBatch(owner, listOf(id, id), ChatBulkAction.Folder, folder); vm.finish() }
        assertEquals(ChatFolder(folder, "Friends", setOf(id)), vm.uiState.activeProfile!!.chatFolders.single())
        assertFalse(vm.beginChatBatch(owner, listOf(id), ChatBulkAction.Folder, "missing"))
        vm.beginChatBatch(owner, listOf(id), ChatBulkAction.Delete); vm.finish()
        assertTrue(vm.uiState.activeProfile!!.chatFolders.single().chatIds.isEmpty())
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertTrue(vm.uiState.activeProfile!!.chatFolders.isEmpty())
    }
    @Test fun recoveryRequiresCurrentGenerationAndPreservesChatsAndDrafts() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!; val id = vm.direct()
        vm.updateDraftText(id, "Keep this draft")
        val before = vm.uiState.activeProfile!!.chats
        vm.selectChatConnectionScenario(ChatConnectionScenario.RetryFailure)
        vm.retryChatConnection(owner)
        var state = vm.uiState.activeProfile!!.chatConnection
        assertTrue(vm.advanceChatConnection(owner, state.generation, state.phase))
        assertEquals(ChatConnectionPhase.Failed, vm.uiState.activeProfile!!.chatConnection.phase)
        vm.retryChatConnection(owner)
        assertFalse(vm.advanceChatConnection(owner, state.generation, ChatConnectionPhase.Connecting))
        state = vm.uiState.activeProfile!!.chatConnection
        vm.advanceChatConnection(owner, state.generation, state.phase)
        assertEquals(ChatConnectionPhase.CatchingUp, vm.uiState.activeProfile!!.chatConnection.phase)
        vm.advanceChatConnection(owner, state.generation, ChatConnectionPhase.CatchingUp)
        assertEquals(ChatConnectionPhase.Online, vm.uiState.activeProfile!!.chatConnection.phase)
        assertEquals(before, vm.uiState.activeProfile!!.chats)
    }
    @Test fun recoveryCompletionCannotCrossProfileSwitchOrNewConnectionScenario() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!
        vm.selectChatConnectionScenario(ChatConnectionScenario.CatchingUp)
        val state = vm.uiState.activeProfile!!.chatConnection
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertFalse(vm.advanceChatConnection(owner, state.generation, state.phase))
        vm.selectProfile(owner)
        assertFalse(vm.advanceChatConnection(owner, state.generation, state.phase))
        vm.selectChatConnectionScenario(ChatConnectionScenario.Offline)
        assertFalse(vm.advanceChatConnection(owner, state.generation, state.phase))
        assertEquals(ChatConnectionPhase.Offline, vm.uiState.activeProfile!!.chatConnection.phase)
    }
    @Test fun soleMemberDeletionSkipsLeaveAndDoesNotRemoveHistoryEarly() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!; val id = vm.group()
        val other = vm.chat(id)!!.members.first { it.personId != owner }.personId
        assertTrue(vm.removeGroupMember(id, other))
        vm.selectChatBatchScenario(ChatBatchScenario.LeaveFailure)
        vm.beginChatBatch(owner, listOf(id), ChatBulkAction.Delete, leaveFirst = true)
        val request = vm.chatBatchAttempt!!
        assertNotNull(vm.chat(id))
        vm.advanceChatBatch(request.id, 0, request.phase)
        assertEquals(ChatBatchPhase.Deleting, vm.chatBatchAttempt!!.phase)
        assertNotNull(vm.chat(id))
        vm.finish(); assertNull(vm.chat(id)); assertEquals(1, vm.chatBatchAttempt!!.completedCount)
    }
    @Test fun partialDeletionReportsSuccessAndRetryDoesNotRecreateRemovedChats() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!
        val ids = vm.uiState.activeProfile!!.chats.take(3).map { it.id }
        vm.selectChatBatchScenario(ChatBatchScenario.PartialApply)
        vm.beginChatBatch(owner, ids, ChatBulkAction.Delete); vm.finish()
        assertNull(vm.chat(ids[0])); assertNull(vm.chat(ids[2])); assertNotNull(vm.chat(ids[1]))
        assertEquals(2, vm.chatBatchAttempt!!.completedCount)
        vm.retryChatBatch(); vm.finish()
        assertTrue(ids.all { vm.chat(it) == null }); assertEquals(1, vm.chatBatchAttempt!!.completedCount)
    }
    @Test fun navigationCancellationStopsRemainingTargetsWithoutRollingBackSuccess() {
        val vm = signedIn(); val owner = vm.uiState.activeProfileId!!
        val ids = vm.uiState.activeProfile!!.chats.filter { !it.isArchived }.take(2).map { it.id }
        vm.beginChatBatch(owner, ids, ChatBulkAction.Archive)
        val request = vm.chatBatchAttempt!!
        vm.advanceChatBatch(request.id, 0, request.phase)
        vm.dismissChatBatch()
        assertFalse(vm.advanceChatBatch(request.id, 1, request.phase))
        assertTrue(vm.chat(ids[0])!!.isArchived); assertFalse(vm.chat(ids[1])!!.isArchived)
    }

}
