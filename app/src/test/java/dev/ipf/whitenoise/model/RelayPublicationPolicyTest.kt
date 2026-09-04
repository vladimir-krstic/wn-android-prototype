package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class RelayPublicationPolicyTest {
    @Test fun signatureIgnoresConnectionStateAndOrderButNotPublishedRoles() {
        val before = ProfileRelayFixtures.defaults
        val disconnected = before.reversed().map { it.copy(status = RelayConnectionStatus.Disconnected) }
        assertEquals(RelayListSignature.capture(before), RelayListSignature.capture(disconnected))
        val changed = before.mapIndexed { index, relay -> if (index == 0) relay.copy(roles = relay.roles - RelayRole.Profile) else relay }
        assertEquals(setOf(PublishedRelayList.Posting), RelayListSignature.capture(before).changedKinds(RelayListSignature.capture(changed)))
    }
    @Test fun projectionEnforcesUnavailableMissingAndPublishedStates() {
        assertEquals(RelayProjectionPhase.Published, RelayPublicationProjection().status(PublishedRelayList.Posting))
        val partial = RelayPublicationProjection(RelayProjectionPhase.Missing, setOf(PublishedRelayList.Inbox))
        assertEquals(RelayProjectionPhase.Published, partial.status(PublishedRelayList.Posting))
        assertEquals(RelayProjectionPhase.Missing, partial.status(PublishedRelayList.Inbox))
        PublishedRelayList.entries.forEach { assertEquals(RelayProjectionPhase.Unavailable,
            RelayPublicationProjection(RelayProjectionPhase.Unavailable).status(it)) }
    }
    @Test fun importedInvalidAddressesAreReportedWithoutDroppingRoles() {
        val imported = ProfileRelay("legacy", "Legacy", "https://legacy.example", RelayConnectionStatus.Disconnected,
            roles = RelayRole.entries.toSet())
        assertTrue(ProfileRelayFixtures.importedAddressNeedsAttention(imported))
        assertEquals(listOf(imported), ProfileRelayFixtures.importedAddressesNeedingAttention(listOf(imported)))
        assertEquals(RelayRole.entries.toSet(), imported.roles)
        assertFalse(ProfileRelayFixtures.importedAddressNeedsAttention(imported.copy(url = "wss://legacy.example")))
    }
}
