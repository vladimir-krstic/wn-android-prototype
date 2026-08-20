package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatFixturesTest {
    private val chats = ChatFixtures.populatedChats(ProfileFixtures.MARMOTA_ID)

    @Test
    fun populatedFixtureHasExactParityCountsAndArchiveSet() {
        assertEquals(77, chats.size)
        assertEquals(72, chats.count { !it.isArchived })
        assertEquals(5, chats.count(Chat::isArchived))
        assertEquals(ChatFixtures.archivedIds, chats.filter(Chat::isArchived).map(Chat::id).toSet())
        assertEquals(1, chats.count(Chat::isPinned))
    }

    @Test
    fun firstViewportOrderMatchesCatalogContract() {
        assertEquals(
            listOf(
                "Direct - Text & Delivery",
                "Direct - Dates & Scrolling",
                "Direct - Replies & Deletion",
                "Direct - Reactions & Actions",
                "Direct - New Chat & Draft",
                "Composer - Text",
                "Composer - Multiline",
                "Composer - Link",
                "Composer - Link Preview",
                "Composer - Photo",
            ),
            chats.take(10).map(Chat::title),
        )
    }

    @Test
    fun scopesDeriveFromAuthoritativeMembershipAndArchiveState() {
        assertEquals(67, ChatProjection.rows(chats, ChatScope.Chats).size)
        assertEquals(5, ChatProjection.rows(chats, ChatScope.Archived).size)
        assertEquals(5, ChatProjection.rows(chats, ChatScope.Left).size)
        assertTrue(ChatProjection.rows(chats, ChatScope.Unread).all(Chat::isUnread))
        assertTrue(ChatProjection.rows(chats, ChatScope.Left).all(Chat::hasEndedMembership))
    }

    @Test
    fun searchIsCaseAndDiacriticInsensitiveAcrossTitleAndPreview() {
        val sample = listOf(
            Chat(
                id = "cafe",
                originalOrder = 0,
                kind = ChatKind.Direct("cafe"),
                title = "Café Circle",
                preview = "Résumé ready",
            ),
        )

        assertEquals("cafe", ChatProjection.rows(sample, ChatScope.Chats, "CAFE").single().id)
        assertEquals("cafe", ChatProjection.rows(sample, ChatScope.Chats, "resume").single().id)
    }

    @Test
    fun peopleDirectoryExcludesSupportAndContainsCreationContacts() {
        val people = ChatFixtures.people()
        assertFalse(people.any { it.id == ChatFixtures.SUPPORT_CHAT_ID })
        assertTrue(people.any { it.id == "maya-chen" })
        assertTrue(people.any { it.id == "avery-stone" })
        assertTrue(people.any { it.id == "identity-color-22" })
    }
}
