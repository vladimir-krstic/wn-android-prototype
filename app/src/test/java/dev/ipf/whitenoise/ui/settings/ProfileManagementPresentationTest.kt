package dev.ipf.whitenoise.ui.settings

import dev.ipf.whitenoise.model.Chat
import dev.ipf.whitenoise.model.ChatKind
import dev.ipf.whitenoise.model.ChatMembership
import dev.ipf.whitenoise.model.ProfileFixtures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ProfileManagementPresentationTest {
    @Test
    fun noInactiveProfileProjectsAdd() {
        assertSame(
            ProfileManagementPresentation.Add,
            profileManagementPresentation(
                profiles = listOf(ProfileFixtures.marmota),
                activeProfileId = ProfileFixtures.MARMOTA_ID,
            ),
        )
    }

    @Test
    fun oneInactiveProfileProjectsTheStoredAlternate() {
        val result = profileManagementPresentation(
            profiles = listOf(ProfileFixtures.marmota, ProfileFixtures.pebble),
            activeProfileId = ProfileFixtures.MARMOTA_ID,
        ) as ProfileManagementPresentation.SingleAlternate

        assertEquals(ProfileFixtures.PEBBLE_ID, result.profile.id)
    }

    @Test
    fun multipleInactiveProfilesPreserveOrderAndCalculateRemainingCount() {
        val profiles = listOf(
            ProfileFixtures.marmota,
            ProfileFixtures.pebble,
            *ProfileFixtures.showcaseProfiles.toTypedArray(),
        )
        val result = profileManagementPresentation(
            profiles = profiles,
            activeProfileId = ProfileFixtures.MARMOTA_ID,
        ) as ProfileManagementPresentation.MultipleAlternates

        assertEquals(
            listOf("pebble", "open-quill", "cipher-wheel"),
            result.previewProfiles.map { it.id },
        )
        assertEquals(3, result.remainingCount)
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
