package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class PeopleDiscoveryTest {
    private val profile = ProfileFixtures.marmota

    @Test fun nicknameCleaningBoundsControlsAndUnicode() {
        assertEquals("Maya Chen", PrivateContactDetails.nickname(" \nMaya\tChen\u202e\u0000 "))
        assertEquals(80, PrivateContactDetails.nickname("a".repeat(100)).length)
        assertFalse(PrivateContactDetails.nickname("a".repeat(79) + "😃").last().isHighSurrogate())
        assertEquals("line 1\nline 2", PrivateContactDetails.notes(" line 1\nline 2\u0000 "))
    }
    @Test fun privateNameAndPublishedNameBothRemainSearchable() {
        val person = profile.people.first().copy(nickname = "Café Friend")
        assertTrue(person.matchesPeopleQuery("cafe"))
        assertTrue(person.matchesPeopleQuery(person.name))
        assertTrue(person.matchesPeopleQuery(person.nostrAddress))
        assertTrue(person.matchesPeopleQuery("nostr:${person.publicKey}"))
    }
    @Test fun invalidIdentifierNeverCreatesAnActionablePerson() {
        listOf("npub1bad", LoginPrototypeData.privateKey, "ncryptsec1invalid", "name@missing", "https://unknown.example/test").forEach {
            val result = PeopleDiscovery.resolve(profile, it)
            assertEquals(it, PeopleSearchStatus.InvalidIdentifier, result.status)
            assertTrue(result.people.isEmpty())
        }
    }
    @Test fun resolvesAddressAndNameFromDirectoryAndMarksNetworkSource() {
        listOf("River", "river@whitenoise.example").forEach { query ->
            val result = PeopleDiscovery.resolve(profile, query)
            assertEquals(PeopleSearchStatus.Ready, result.status)
            assertEquals("river-song", result.people.single().person.id)
            assertEquals(PersonSource.Network, result.people.single().source)
        }
    }
    @Test fun noProfilePublicKeyCanStillBeUsedAndUnknownAddressCannot() {
        val key = "npub1" + "q".repeat(58)
        val result = PeopleDiscovery.resolve(profile, "https://whitenoise.chat/$key")
        assertEquals(PeopleSearchStatus.NoProfile, result.status)
        assertEquals(key, result.people.single().person.publicKey)
        assertEquals(PeopleSearchStatus.AddressNotFound, PeopleDiscovery.resolve(profile, "missing@whitenoise.example").status)
    }
    @Test fun partialAndUnavailablePreserveLocalMatchesAndRetryCanResolve() {
        val person = profile.people.first()
        val unavailable = PeopleDiscovery.resolve(profile, person.name, PeopleSearchScenario.Unavailable)
        assertEquals(PeopleSearchStatus.Unavailable, unavailable.status)
        assertTrue(unavailable.people.any { it.person.id == person.id })
        assertEquals(PeopleSearchStatus.Partial, PeopleDiscovery.resolve(profile, "River", PeopleSearchScenario.Partial).status)
        assertEquals(PeopleSearchStatus.Ready, PeopleDiscovery.resolve(profile, "River").status)
    }
    @Test fun selfAndSupportAreExcludedAndDirectoryDuplicatesAreNotAdded() {
        val remote = PeopleDiscovery.directory.first()
        val custom = profile.copy(people = profile.people + remote)
        assertEquals(1, PeopleDiscovery.resolve(custom, remote.publicKey).people.size)
        assertTrue(PeopleDiscovery.local(profile, "").none { it.person.id == profile.id || it.person.id == "white-noise-support" })
    }
}
