package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class ProfileKeyAccessPolicyTest {
    @Test fun secretAccessRequiresAnAvailableLocalKey() {
        assertTrue(ProfileKeyAccessPolicy.canRead(ProfileFixtures.marmota))
        assertFalse(ProfileKeyAccessPolicy.canRead(ProfileFixtures.marmota.copy(localKeyAvailable = false)))
        assertFalse(ProfileKeyAccessPolicy.canRead(ProfileFixtures.marmota.copy(signingMode = ProfileSigningMode.Amber)))
    }

    @Test fun exportRejectsMissingChangedOrUnavailableOwner() {
        val profile = ProfileFixtures.marmota
        val request = ProfileKeyExportRequest(profile.id, ProfileKeyExportKind.Raw, 100, "local test payload")
        assertTrue(ProfileKeyAccessPolicy.canComplete(request, profile, 200))
        assertFalse(ProfileKeyAccessPolicy.canComplete(null, profile, 200))
        assertFalse(ProfileKeyAccessPolicy.canComplete(request, ProfileFixtures.openCircuit, 200))
        assertFalse(ProfileKeyAccessPolicy.canComplete(request, profile.copy(localKeyAvailable = false), 200))
        assertFalse(ProfileKeyAccessPolicy.canComplete(request, profile.copy(signingMode = ProfileSigningMode.Amber), 200))
    }

    @Test fun exportExpiryUsesAnInclusiveStartAndExclusiveEndAndRejectsClockRollback() {
        val profile = ProfileFixtures.marmota
        val request = ProfileKeyExportRequest(profile.id, ProfileKeyExportKind.Encrypted, 1_000, "protected test payload")
        assertTrue(ProfileKeyAccessPolicy.canComplete(request, profile, 1_000))
        assertTrue(ProfileKeyAccessPolicy.canComplete(request, profile, 30_999))
        assertFalse(ProfileKeyAccessPolicy.canComplete(request, profile, 31_000))
        assertFalse(ProfileKeyAccessPolicy.canComplete(request, profile, 999))
    }

    @Test fun rawFixtureExportCanBeUsedByTheStricterSignInShapeGate() {
        assertEquals(PrivateKeyState.Valid, PrivateKeyValidator.state(ProfileKeyFixtures.PRIVATE_KEY))
        assertTrue(ProfileKeyFixtures.rawExport(ProfileFixtures.marmota).contains(ProfileKeyFixtures.PRIVATE_KEY))
        assertFalse(ProfileKeyFixtures.encryptedExport(ProfileFixtures.marmota, "password").contains(ProfileKeyFixtures.PRIVATE_KEY))
    }
}
