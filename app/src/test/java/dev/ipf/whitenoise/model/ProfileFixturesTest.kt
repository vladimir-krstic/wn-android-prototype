package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFixturesTest {
    @Test
    fun initialAndAddedSignUpKeepDistinctCanonicalIdentities() {
        val initial = ProfileFixtures.initialSignUp("Marmota", "", null)
        val added = ProfileFixtures.addedSignUp("Pebble", "", null)

        assertEquals(ProfileFixtures.MARMOTA_ID, initial.id)
        assertEquals("marmota@whitenoise.example", initial.nostrAddress)
        assertTrue(initial.isNostrAddressVerified)
        assertEquals(ProfileFixtures.PEBBLE_ID, added.id)
        assertEquals("pebble@whitenoise.example", added.nostrAddress)
        assertTrue(added.isNostrAddressVerified)
        assertNotEquals(initial.id, added.id)
    }

    @Test
    fun signUpCarriesEditableValuesAndAvatar() {
        val avatar = ProfileAvatar.WebImage(
            asset = AvatarAsset.WebAionyHaust,
            choiceId = "3TLl_97HNJo",
        )
        val profile = ProfileFixtures.initialSignUp(
            name = "  River  ",
            about = "Available for quiet conversations.",
            avatar = avatar,
        )

        assertEquals("River", profile.name)
        assertEquals("Available for quiet conversations.", profile.about)
        assertEquals(avatar, profile.avatar)
        assertEquals("marmota@whitenoise.example", profile.nostrAddress)
    }

    @Test
    fun reactivationUpdatesOnlyEditableValues() {
        val stored = ProfileFixtures.marmota.copy(
            nostrAddress = "kept@example.com",
            isNostrAddressVerified = false,
        )
        val replacement = ProfileFixtures.initialSignUp("River", "Updated", null)
        val updated = stored.updateEditableValues(replacement)

        assertEquals("River", updated.name)
        assertEquals("Updated", updated.about)
        assertEquals("kept@example.com", updated.nostrAddress)
        assertEquals(false, updated.isNostrAddressVerified)
    }
}
