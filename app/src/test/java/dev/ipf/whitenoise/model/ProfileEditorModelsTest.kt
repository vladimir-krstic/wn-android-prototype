package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class ProfileEditorModelsTest {
    @Test fun lightningAddressNormalizesDomainAndPreservesLocalCase() {
        assertEquals("Alice@payments.example", LightningAddress.normalize(" Alice@PAYMENTS.Example "))
        assertEquals("", LightningAddress.normalize(" \n"))
    }
    @Test fun malformedAndPathLikeLightningAddressesAreRejected() {
        listOf("alice", "alice@localhost", "a b@domain.example", "a@-domain.example", "a@domain-.example", "a@domain.example:443", "a@domain.example/path", "a/b@domain.example", "..@domain.example", ".@domain.example", "a@@domain.example", "a@"+"z".repeat(64)+".example").forEach {
            assertNull(it, LightningAddress.normalize(it))
        }
    }
    @Test fun suggestionsAreDeterministicAndNeverRepeatTheCurrentName() {
        assertEquals(ProfileNameSuggestions.next("Marmota", 2), ProfileNameSuggestions.next("Marmota", 2))
        val first = ProfileNameSuggestions.next("Marmota", 0)
        assertNotEquals(first, ProfileNameSuggestions.next(first, 0))
        assertNotEquals(first, ProfileNameSuggestions.next(first, -1))
    }
    @Test fun draftNormalizationIsAllOrNothing() {
        val original = ProfileEditDraft.from(ProfileFixtures.marmota)
        assertNull(original.copy(name = "", lightningAddress = "a@b").normalized())
        assertNull(original.copy(lightningAddress = "not an address").normalized())
        val valid = original.copy(name = " New Name ", lightningAddress = "Person@PAYMENTS.EXAMPLE").normalized()!!
        assertEquals("New Name", valid.name)
        assertEquals("Person@payments.example", valid.lightningAddress)
        assertEquals(ProfileFixtures.marmota.name, original.name)
    }
    @Test fun zoomPanRespectsFittedImageAndZeroDimensions() {
        assertEquals(0f to 0f, ProfileImageZoom.maxPan(400, 800, 1600, 800, 1f))
        assertEquals(200f to 0f, ProfileImageZoom.maxPan(400, 800, 1600, 800, 2f))
        assertEquals(0f to 0f, ProfileImageZoom.maxPan(0, 800, 1600, 800, 4f))
        assertEquals(200f to 200f, ProfileImageZoom.maxPan(400, 400, 400, 400, 2f))
    }
}
