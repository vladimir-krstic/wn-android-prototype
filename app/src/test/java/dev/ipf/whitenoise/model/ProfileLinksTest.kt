package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class ProfileLinksTest {
    private val key = PublicReferenceEncoding.fixtureKey("river")
    @Test fun canonicalLinkAndQrProvenanceRoundTrip() {
        val profile = ProfileLinks.forKey(key)!!
        assertEquals("marmot://profile/$key", profile.uri)
        assertEquals(profile.copy(fromQr = true), ProfileLinks.parse(profile.qrUri!!))
    }
    @Test fun acceptsEveryProductionLegacySchemeAndKnownWebHost() {
        for (scheme in listOf("marmot", "whitenoise", "whitenoise-staging", "whitenoise-dev")) {
            for (value in listOf("$scheme://profile/$key", "$scheme://$key", "$scheme:profile/$key", "$scheme:$key")) assertEquals(key, ProfileLinks.parse(value)?.value)
        }
        for (host in listOf("whitenoise.chat", "www.whitenoise.chat", "marmot.app", "www.marmot.app")) {
            assertEquals(key, ProfileLinks.parse("https://$host/profile/$key?from=share")?.value)
            assertEquals(key, ProfileLinks.parse("https://$host/$key")?.value)
        }
        assertEquals(key, ProfileLinks.parse(" nostr:$key ")?.value)
    }
    @Test fun checksumsLengthsPaddingAndMixedCaseAreRejected() {
        assertEquals(key, ProfileLinks.parse(key.uppercase())?.value)
        for (bad in listOf(key.dropLast(1) + if (key.last() == 'q') "p" else "q", "npub1" + "q".repeat(58), key.replaceFirst('n','N'), key.dropLast(1), key + "q")) assertNull(ProfileLinks.parse(bad))
        assertNull(ProfileLinks.parse(PublicReferenceEncoding.encode("npub", List(31) { 1 })))
    }
    @Test fun secretUnknownHostAndMalformedRoutesNeverOpenAProfile() {
        for (bad in listOf("nsec1" + "q".repeat(58), "ncryptsec1" + "q".repeat(80), "https://example.com/$key", "https://marmot.app@evil.example/$key", "https://user@marmot.app/$key", "marmot://profile/$key/extra", "https://marmot.app/profile/$key/extra", "javascript:$key")) assertNull(ProfileLinks.parse(bad, recipient = true))
    }
    @Test fun nprofileResolvesOnePublicKeyAndRejectsMalformedOrDuplicateTlv() {
        val bytes = PublicReferenceEncoding.decode(key)!!.second
        assertEquals(key, ProfileLinks.parse(PublicReferenceEncoding.encode("nprofile", listOf(0,32) + bytes + listOf(1,3,97,98,99)))?.value)
        for (bad in listOf(listOf(0,32)+bytes+listOf(0,32)+bytes, listOf(0,33)+bytes, listOf(1,0), listOf(0,32)+bytes+listOf(1)))
            assertNull(ProfileLinks.parse(PublicReferenceEncoding.encode("nprofile",bad)))
    }
    @Test fun recipientHexAndAddressAreRestrictedToRecipientUse() {
        val hex = "ab".repeat(32)
        assertNull(ProfileLinks.parse(hex)); assertEquals(PublicReferenceEncoding.encode("npub", List(32){0xab}), ProfileLinks.parse(hex, true)?.value)
        assertNull(ProfileLinks.parse("river@whitenoise.example")); assertTrue(ProfileLinks.parse("river@whitenoise.example",true)!!.isAddress)
        assertNull(ProfileLinks.parse("river@@whitenoise.example",true))
    }
    @Test fun matchesIndependentNip19PublicExamples() {
        // Published public examples from https://github.com/nostr-protocol/nips/blob/master/19.md.
        val hex = "3bf0c63fcb93463407af97a5e5ee64fa883d107ef9e558472c4eb9aaaefa459d"
        val npub = "npub180cvv07tjdrrgpa0j7j7tmnyl2yr6yr7l8j4s3evf6u64th6gkwsyjh6w6"
        assertEquals(npub, PublicReferenceEncoding.encode("npub",hex.chunked(2).map { it.toInt(16) }))
        assertEquals(hex.chunked(2).map { it.toInt(16) },PublicReferenceEncoding.decode(npub)!!.second)
        assertEquals(npub,ProfileLinks.parse("nprofile1qqsrhuxx8l9ex335q7he0f09aej04zpazpl0ne2cgukyawd24mayt8gpp4mhxue69uhhytnc9e3k7mgpz4mhxue69uhkg6nzv9ejuumpv34kytnrdaksjlyr9p")!!.value)
    }
    @Test fun allBundledProfileAndPersonKeysAreValidForCrossClientSharing() {
        for (p in listOf(ProfileFixtures.marmota,ProfileFixtures.pebble,ProfileFixtures.openCircuit) + ProfileFixtures.showcaseProfiles) {
            assertEquals(p.publicKey,ProfileLinks.parse(ProfileLinks.forKey(p.publicKey)!!.qrUri!!)!!.value)
            p.people.forEach { assertNotNull(ProfileLinks.forKey(it.publicKey)) }
        }
    }
    @Test fun recipientSearchResolvesLinksNprofileAndHexToTheSamePerson() {
        val profile=ProfileFixtures.marmota; val person=profile.people.first { it.id=="maya-chen" }
        val bytes=PublicReferenceEncoding.decode(person.publicKey)!!.second
        for (reference in listOf(ProfileLinks.forKey(person.publicKey)!!.uri!!,
            PublicReferenceEncoding.encode("nprofile",listOf(0,32)+bytes), bytes.joinToString("") { "%02x".format(it) })) {
            assertEquals(person.id,PeopleDiscovery.resolve(profile,reference).people.single().person.id)
            assertEquals(person.id,GlobalSearch.people(profile,reference,PeopleSearchScenario.Success).people.single().person.id)
        }
    }

}
