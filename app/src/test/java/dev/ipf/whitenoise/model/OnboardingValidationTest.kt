package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingValidationTest {
    @Test
    fun privateKeyValidationMatchesPrototypeContract() {
        assertEquals(PrivateKeyState.Empty, PrivateKeyValidator.state("  "))
        assertEquals(PrivateKeyState.Invalid, PrivateKeyValidator.state("NSEC1abc"))
        assertEquals(PrivateKeyState.Invalid, PrivateKeyValidator.state("npub1abc"))
        assertEquals(PrivateKeyState.Valid, PrivateKeyValidator.state("  nsec1abc  "))
    }

    @Test
    fun verifiedAddressRequiresEmailShapedValue() {
        assertTrue(VerifiedNostrAddress.isValid("marmota@whitenoise.example"))
        assertFalse(VerifiedNostrAddress.isValid("marmota"))
        assertFalse(VerifiedNostrAddress.isValid("marmota@localhost"))
        assertFalse(VerifiedNostrAddress.isValid("@whitenoise.example"))
    }

    @Test
    fun restoringStoredVerifiedAddressRestoresVerification() {
        assertTrue(
            VerifiedNostrAddress.isVerifiedDraft(
                value = "  marmota@whitenoise.example ",
                matching = "marmota@whitenoise.example",
                storedIsVerified = true,
            ),
        )
        assertFalse(
            VerifiedNostrAddress.isVerifiedDraft(
                value = "river@example.com",
                matching = "marmota@whitenoise.example",
                storedIsVerified = true,
            ),
        )
    }
}
