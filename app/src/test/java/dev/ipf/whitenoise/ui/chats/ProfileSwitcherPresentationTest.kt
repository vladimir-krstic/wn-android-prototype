package dev.ipf.whitenoise.ui.chats

import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatKind
import dev.ipf.whitenoise.model.ChatMembership
import dev.ipf.whitenoise.model.ProfileFixtures
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileSwitcherPresentationTest {
    @Test
    fun emptyProfilesProduceNoSwitcherRows() {
        assertEquals(emptyList<ProfileSwitcherPresentation>(), profileSwitcherPresentation(emptyList(), null))
    }

    @Test
    fun aSingleActiveProfileStillAppearsAsSelected() {
        val result = profileSwitcherPresentation(listOf(ProfileFixtures.marmota), ProfileFixtures.MARMOTA_ID)
        assertEquals(listOf(ProfileFixtures.MARMOTA_ID), result.map { it.profile.id })
        assertEquals(listOf(true), result.map { it.isActive })
    }

    @Test
    fun absentActiveProfilePreservesStoredOrderWithoutSelectingAnAlternate() {
        val profiles = listOf(ProfileFixtures.pebble, ProfileFixtures.openCircuit)
        val result = profileSwitcherPresentation(profiles, "removed")
        assertEquals(profiles, result.map { it.profile })
        assertEquals(listOf(false, false), result.map { it.isActive })
    }

    @Test
    fun switcherPlacesActiveFirstAndKeepsOtherProfilesInStoredOrder() {
        val profiles = listOf(
            ProfileFixtures.marmota,
            ProfileFixtures.pebble,
            ProfileFixtures.openCircuit,
        )

        val result = profileSwitcherPresentation(profiles, ProfileFixtures.PEBBLE_ID)

        assertEquals(
            listOf("pebble", "marmota", "open-circuit"),
            result.map { it.profile.id },
        )
        assertEquals(listOf(true, false, false), result.map { it.isActive })
    }

    @Test
    fun switcherAggregatesVisibleUnreadAndManualUnreadOnly() {
        val alternate = ProfileFixtures.pebble.copy(
            chats = listOf(
                chat("unread", unreadCount = 3),
                chat("manual", isMarkedUnread = true),
                chat("archived", unreadCount = 8, isArchived = true),
                chat("left", unreadCount = 5, membership = ChatMembership.Left),
            ),
        )

        val result = profileSwitcherPresentation(
            profiles = listOf(ProfileFixtures.marmota, alternate),
            activeProfileId = ProfileFixtures.MARMOTA_ID,
        )

        assertEquals(4, result.single { it.profile.id == ProfileFixtures.PEBBLE_ID }.unreadCount)
    }

    private fun chat(
        id: String,
        unreadCount: Int = 0,
        isMarkedUnread: Boolean = false,
        isArchived: Boolean = false,
        membership: ChatMembership = ChatMembership.Active,
    ) = Chat(
        id = id,
        originalOrder = 0,
        kind = ChatKind.Direct("person-$id"),
        title = id,
        unreadCount = unreadCount,
        isMarkedUnread = isMarkedUnread,
        isArchived = isArchived,
        membership = membership,
    )
}
