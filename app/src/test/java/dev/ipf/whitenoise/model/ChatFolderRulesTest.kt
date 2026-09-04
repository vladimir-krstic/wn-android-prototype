package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class ChatFolderRulesTest {
    private fun direct(id: String) = Chat(id, 0, ChatKind.Direct("person-$id"), "Chat $id")
    private fun group(id: String) = Chat(id, 1, ChatKind.Group, "Group $id", description = "Outdoor plans", members = listOf(GroupMember("me", GroupRole.Admin), GroupMember("friend", GroupRole.Member)))

    @Test fun emptyRuleAndIncludeMutedAloneCannotSwallowList() {
        val chats = listOf(direct("a"), group("b"))
        assertTrue(ChatFolders.rows(chats, ChatFolder("f", "Empty")).isEmpty())
        assertTrue(ChatFolders.rows(chats, ChatFolder("f", "Empty", rule = ChatFolderRule(includeMuted = true))).isEmpty())
    }
    @Test fun manualMembersBypassEveryAutomaticConstraintAndAreDeduplicated() {
        val chat = direct("a").copy(isArchived = true, muteDuration = MuteDuration.OneHour)
        val rule = ChatFolderRule(keyword = "unmatched", unreadOnly = true, groupsOnly = true)
        assertEquals(listOf(chat), ChatFolders.rows(listOf(chat), ChatFolder("f", "Manual", setOf(chat.id, "missing"), rule = rule)))
        assertEquals(listOf(chat), ChatFolders.rows(listOf(chat), ChatFolder("f", "Both", setOf(chat.id), rule = ChatFolderRule(archivedOnly = true, includeMuted = true))))
    }
    @Test fun peopleOrKeywordMatchesDirectCounterpartAndGroupRoster() {
        val a = direct("a"); val b = group("b"); val c = direct("c")
        val rule = ChatFolderRule(personIds = setOf("person-a"), keyword = "OUTDOOR")
        assertEquals(listOf(a, b), ChatFolders.rows(listOf(a, b, c), ChatFolder("f", "Either", rule = rule)))
        assertTrue(ChatFolders.matches(b, rule.copy(keyword = "", personIds = setOf("friend"))))
        assertFalse(ChatFolders.matches(c, rule))
    }
    @Test fun unreadGroupsArchiveAndMuteConstrainOnlyRuleMatches() {
        val base = group("a").copy(isMarkedUnread = true)
        val rule = ChatFolderRule(groupsOnly = true, unreadOnly = true)
        assertTrue(ChatFolders.matches(base, rule))
        assertFalse(ChatFolders.matches(base.copy(isMarkedUnread = false), rule))
        assertFalse(ChatFolders.matches(base.copy(kind = ChatKind.Direct("friend")), rule))
        assertFalse(ChatFolders.matches(base.copy(isArchived = true), rule))
        assertFalse(ChatFolders.matches(base.copy(muteDuration = MuteDuration.Always), rule))
        assertTrue(ChatFolders.matches(base.copy(muteDuration = MuteDuration.Always), rule.copy(includeMuted = true)))
        assertTrue(ChatFolders.matches(base.copy(isArchived = true), rule.copy(archivedOnly = true)))
        assertFalse(ChatFolders.matches(base, rule.copy(archivedOnly = true)))
    }
    @Test fun categoriesWorkWithoutPeopleAndKeywordButDefaultRulesIncludeMuted() {
        val unread = direct("u").copy(unreadCount = 1, muteDuration = MuteDuration.OneHour)
        val archived = direct("a").copy(isArchived = true, muteDuration = MuteDuration.OneDay)
        val group = group("g").copy(muteDuration = MuteDuration.Always)
        val chats = listOf(unread, archived, group)
        assertEquals(listOf(unread), ChatFolders.rows(chats, ChatFolders.defaults[0]))
        assertEquals(listOf(archived), ChatFolders.rows(chats, ChatFolders.defaults[1]))
        assertEquals(listOf(group), ChatFolders.rows(chats, ChatFolders.defaults[2]))
    }
    @Test fun keywordUsesVisibleTitleAndGroupDescriptionWithInvariantCaseFold() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"))
            assertTrue(ChatFolders.matches(direct("a").copy(title = "PRIVATE FRIEND"), ChatFolderRule(keyword = "  private friend  ")))
            assertTrue(ChatFolders.matches(group("a"), ChatFolderRule(keyword = "outdoor")))
            assertFalse(ChatFolders.matches(direct("a").copy(preview = "outdoor"), ChatFolderRule(keyword = "outdoor")))
        } finally { Locale.setDefault(old) }
    }
    @Test fun liveMembershipChangesWithoutChangingStoredFolderAndUnselectManualMayStillMatch() {
        val chat = group("a").copy(isMarkedUnread = true)
        val folder = ChatFolder("f", "Live", setOf(chat.id), rule = ChatFolderRule(unreadOnly = true))
        assertEquals(listOf(chat), ChatFolders.rows(listOf(chat), folder.copy(chatIds = emptySet())))
        assertTrue(ChatFolders.rows(listOf(chat.copy(isMarkedUnread = false)), folder.copy(chatIds = emptySet())).isEmpty())
        assertEquals(setOf(chat.id), folder.chatIds)
    }
    @Test fun moveBoundsAndRestorePreserveEditedDefaultsAndAppendOnlyMissingOnes() {
        val edited = ChatFolders.defaults[0].copy(name = "My unread", rule = ChatFolderRule(keyword = "friend"))
        val custom = ChatFolder("custom", "Custom")
        val folders = listOf(custom, edited)
        assertEquals(folders, ChatFolders.move(folders, custom.id, -1))
        assertEquals(listOf(edited, custom), ChatFolders.move(folders, custom.id, 1))
        assertEquals(listOf(custom, edited) + ChatFolders.defaults.drop(1), ChatFolders.restore(folders))
        assertEquals(ChatFolders.restore(folders), ChatFolders.restore(ChatFolders.restore(folders)))
    }
}
