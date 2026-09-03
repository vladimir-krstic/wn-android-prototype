package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class ChatOrganizationTest {
    private fun chat(id: String, order: Int, pinned: Boolean = false) = Chat(id, order, ChatKind.Direct(id), id, isPinned = pinned)

    @Test fun pinnedMovesAreBoundedAndPreserveOriginalUnpinnedOrder() {
        val chats = listOf(chat("u1", 1), chat("p1", 2, true), chat("u2", 3), chat("p2", 4, true))
        val moved = ChatOrganization.move(chats, "p2", -1)
        assertEquals(listOf("p2", "p1", "u1", "u2"), ChatProjection.rows(moved, ChatScope.Chats).map { it.id })
        assertEquals(chats.map { it.originalOrder }, moved.map { it.originalOrder })
        assertEquals(moved, ChatOrganization.move(moved, "p2", -1))
        assertEquals(moved, ChatOrganization.move(moved, "p1", 1))
        assertEquals(moved, ChatOrganization.move(moved, "u1", 1))
        assertEquals(listOf("u1", "p1", "u2", "p2"), ChatProjection.rows(moved.map { it.copy(isPinned = false) }, ChatScope.Chats).map { it.id })
        assertFalse(ChatOrganization.actions(moved.last(), moved).contains(ChatListAction.MoveUp))
        assertTrue(ChatOrganization.actions(moved.last(), moved).contains(ChatListAction.MoveDown))
    }
    @Test fun selectionReconcilesVisibleRowsAndMixedArchiveMeansArchive() {
        val chats = listOf(chat("a", 0), chat("b", 1).copy(isArchived = true))
        assertEquals(listOf("a"), ChatOrganization.reconcile(listOf("a", "b", "a", "missing"), chats.take(1)))
        assertEquals(ChatBulkAction.Archive, ChatOrganization.archiveAction(chats))
        assertEquals(ChatBulkAction.Unarchive, ChatOrganization.archiveAction(chats.drop(1)))
    }
    @Test fun activeInvitedAndEndedChatsOfferLocalDeleteAndSelectionWithoutDuplicateCommands() {
        ChatMembership.entries.forEach { membership ->
            val actions = ChatListActionPolicy.all(chat("a", 0).copy(membership = membership))
            assertEquals(1, actions.count { it == ChatListAction.Delete })
            assertTrue(actions.containsAll(listOf(ChatListAction.Select, ChatListAction.Folder)))
        }
    }
    @Test fun onlyAuthoritativeSoleMemberSkipsLeave() {
        val base = chat("group", 0).copy(kind = ChatKind.Group)
        assertTrue(ChatOrganization.requiresLeave(base, "me"))
        val sole = base.copy(members = listOf(GroupMember("me", GroupRole.Admin)))
        assertFalse(ChatOrganization.requiresLeave(sole, "me"))
        assertFalse(ChatOrganization.requiresAdmin(sole, "me"))
        val withOther = sole.copy(members = sole.members + GroupMember("friend", GroupRole.Member))
        assertTrue(ChatOrganization.requiresAdmin(withOther, "me"))
    }
}
