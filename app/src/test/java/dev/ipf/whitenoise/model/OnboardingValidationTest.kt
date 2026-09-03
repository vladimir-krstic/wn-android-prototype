package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingValidationTest {
    @Test
    fun privateKeyValidationRejectsMalformedAndWrongKeyKindsWithoutDecoding() {
        assertEquals(PrivateKeyState.Empty, PrivateKeyValidator.state("  "))
        assertEquals(PrivateKeyState.Invalid, PrivateKeyValidator.state("NSEC1abc"))
        assertEquals(PrivateKeyState.Invalid, PrivateKeyValidator.state("npub1abc"))
        assertEquals(PrivateKeyState.Invalid, PrivateKeyValidator.state("  nsec1abc  "))
        assertEquals(PrivateKeyState.Valid, PrivateKeyValidator.state("  ${LoginPrototypeData.privateKey}  "))
        assertEquals(PrivateKeyState.PublicKey, PrivateKeyValidator.state("npub1" + "q".repeat(58)))
        assertEquals(PrivateKeyState.EncryptedKey, PrivateKeyValidator.state("ncryptsec1" + "q".repeat(60)))
        assertEquals(PrivateKeyState.Invalid, PrivateKeyValidator.state("nsec1" + "b".repeat(58)))
    }

    @Test
    fun scanAndPasteNormalizationKeepsRecognizedWrongKeyTypesForHelpfulErrors() {
        assertEquals(LoginPrototypeData.privateKey, PrivateKeyValidator.normalize(" NOSTR:${LoginPrototypeData.privateKey} "))
        assertEquals("npub1" + "q".repeat(58), PrivateKeyValidator.scannedValue(" nostr:npub1" + "q".repeat(58)))
        assertEquals(null, PrivateKeyValidator.scannedValue("https://example.com/not-a-key"))
        assertEquals(null, PrivateKeyValidator.scannedValue(" "))
    }

    @Test
    fun knownProfileLinksNormalizeOnlyPublicKeysAndRejectLookalikeHosts() {
        val publicKey = "npub1" + "q".repeat(58)
        listOf("marmot://profile/$publicKey?from=qr", "whitenoise:profile/$publicKey", "https://whitenoise.chat/profile/$publicKey").forEach {
            assertEquals(publicKey, PrivateKeyValidator.scannedValue(it))
            assertEquals(PrivateKeyState.PublicKey, PrivateKeyValidator.state(it))
        }
        assertEquals(null, PrivateKeyValidator.scannedValue("https://whitenoise.chat.evil.example/$publicKey"))
        assertEquals(null, PrivateKeyValidator.scannedValue("marmot://profile/${LoginPrototypeData.privateKey}"))
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
