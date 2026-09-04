package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class ChatFolderStateTest {
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
    @Test fun saveIsAtomicValidatesNameAndPreservesDefaultIdentity() {
        val vm = model(); val owner = vm.uiState.activeProfile!!; val old = owner.chatFolders.first()
        assertNull(vm.saveChatFolder(owner.id, old.id, ChatFolderDraft(" ", "Would change")))
        assertEquals(owner, vm.uiState.activeProfile)
        val draft = ChatFolderDraft(" Work ", " Plans ", setOf(owner.chats.first().id, "missing"), ChatFolderRule(keyword = " Team ", unreadOnly = true))
        assertEquals(old.id, vm.saveChatFolder(owner.id, old.id, draft))
        val saved = vm.uiState.activeProfile!!.chatFolders.first()
        assertEquals("Work", saved.name); assertEquals("Plans", saved.description); assertEquals("Team", saved.rule.keyword)
        assertEquals(setOf(owner.chats.first().id), saved.chatIds); assertEquals(old.systemKind, saved.systemKind)
        assertEquals(owner.chats, vm.uiState.activeProfile!!.chats)
    }
    @Test fun createEditAndDeleteNeverRemoveChatHistory() {
        val vm = model(); val owner = vm.uiState.activeProfile!!
        val id = vm.saveChatFolder(owner.id, null, ChatFolderDraft("Friends", chatIds = setOf(owner.chats.first().id)))!!
        assertNotEquals(owner.chatFolders.first().id, id)
        vm.saveChatFolder(owner.id, id, ChatFolderDraft("Renamed", rule = ChatFolderRule(groupsOnly = true)))
        assertEquals("Renamed", vm.uiState.activeProfile!!.chatFolders.last().name)
        assertTrue(vm.deleteChatFolder(owner.id, id))
        assertEquals(owner.chats, vm.uiState.activeProfile!!.chats)
        assertNull(vm.saveChatFolder(owner.id, id, ChatFolderDraft("Stale edit")))
    }
    @Test fun deletingAllDefaultsStaysDeletedUntilExplicitRestore() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        ChatFolders.defaults.forEach { assertTrue(vm.deleteChatFolder(owner, it.id)) }
        assertTrue(vm.uiState.activeProfile!!.chatFolders.isEmpty())
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertEquals(ChatFolders.defaults, vm.uiState.activeProfile!!.chatFolders)
        vm.selectProfile(owner); assertTrue(vm.uiState.activeProfile!!.chatFolders.isEmpty())
        vm.restoreChatFolders(owner); assertEquals(ChatFolders.defaults, vm.uiState.activeProfile!!.chatFolders)
    }
    @Test fun reorderedEditedDefaultSurvivesRestoreAndProfileSwitch() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        val first = ChatFolders.defaults.first(); val last = ChatFolders.defaults.last()
        vm.saveChatFolder(owner, first.id, ChatFolderDraft("Changed", rule = ChatFolderRule(keyword = "plans")))
        vm.moveChatFolder(owner, first.id, 1); vm.deleteChatFolder(owner, last.id); vm.restoreChatFolders(owner)
        val expected = vm.uiState.activeProfile!!.chatFolders
        assertEquals(first.id, expected[1].id); assertEquals("Changed", expected[1].name); assertEquals(last, expected.last())
        vm.completeSignIn(OnboardingOrigin.AddProfile); vm.selectProfile(owner)
        assertEquals(expected, vm.uiState.activeProfile!!.chatFolders)
    }
    @Test fun staleOwnerCannotSaveDeleteAssignMoveOrRestore() {
        val vm = model(); val owner = vm.uiState.activeProfile!!; val folder = owner.chatFolders.first().id; val chat = owner.chats.first().id
        vm.completeSignIn(OnboardingOrigin.AddProfile); val active = vm.uiState.activeProfile!!
        assertNull(vm.saveChatFolder(owner.id, folder, ChatFolderDraft("Wrong")))
        assertFalse(vm.deleteChatFolder(owner.id, folder)); assertFalse(vm.assignChatFolder(owner.id, chat, folder))
        vm.moveChatFolder(owner.id, folder, 1); vm.restoreChatFolders(owner.id)
        assertEquals(active, vm.uiState.activeProfile)
    }
    @Test fun contextualAssignmentIsIdempotentAndMissingTargetsCannotMutate() {
        val vm = model(); val owner = vm.uiState.activeProfile!!; val folder = owner.chatFolders.first().id; val chat = owner.chats.first().id
        assertFalse(vm.assignChatFolder(owner.id, "missing", folder)); assertFalse(vm.assignChatFolder(owner.id, chat, "missing"))
        repeat(2) { assertTrue(vm.assignChatFolder(owner.id, chat, folder)) }
        assertEquals(setOf(chat), vm.uiState.activeProfile!!.chatFolders.first().chatIds)
    }
    @Test fun ruleCountsFollowUnreadMuteArchiveAndPrivateNameUpdates() {
        val vm = model(); val owner = vm.uiState.activeProfile!!; val person = owner.people.first()
        val id = vm.openOrCreateDirectChat(person.id)!!
        val folderId = vm.saveChatFolder(owner.id, null, ChatFolderDraft("Live", rule = ChatFolderRule(keyword = "Private nickname", unreadOnly = true)))!!
        fun ids() = vm.uiState.activeProfile!!.let { profile -> ChatFolders.rows(profile.chats, profile.chatFolders.first { it.id == folderId }).map { it.id } }
        assertFalse(id in ids())
        vm.savePrivateContact(owner.id, person.id, "Private nickname", ""); vm.markChatUnread(id, true)
        assertTrue(id in ids()); vm.setChatMute(id, MuteDuration.OneHour); assertFalse(id in ids())
        vm.setChatMute(id, null); vm.setChatArchived(id, true); assertFalse(id in ids())
        vm.setChatArchived(id, false); assertTrue(id in ids()); vm.markChatUnread(id, false); assertFalse(id in ids())
    }
}
