package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class MessagePinStateTest {
    private fun model() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }

    @Test fun mutationsAreOwnedByProfileAndChatAndSurviveNavigationState() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!
        val original = vm.chat("maya-chen")!!
        val message = original.timeline.filterIsInstance<ChatTimelineEntry.Message>().first { it.id !in original.pinnedMessageIds }.id
        val other = vm.chat("fiatjaf")!!
        val otherProfiles = vm.uiState.profiles.filter { it.id != owner }
        assertFalse(vm.setMessagePinned("stale-profile", original.id, message, true))
        assertFalse(vm.setMessagePinned(owner, "missing-chat", message, true))
        assertTrue(vm.setMessagePinned(owner, original.id, message, true))
        assertEquals(original.pinnedMessageIds + message, vm.chat(original.id)!!.pinnedMessageIds)
        assertEquals(other, vm.chat(other.id))
        assertEquals(otherProfiles, vm.uiState.profiles.filter { it.id != owner })
        assertTrue(vm.setMessagePinned(owner, original.id, message, false))
        assertEquals(original.pinnedMessageIds, vm.chat(original.id)!!.pinnedMessageIds)
    }

    @Test fun deletionAndRemovalExposeDistinctRecoveryWithoutRepinningMissingMessages() {
        val vm = model(); val owner = vm.uiState.activeProfileId!!; val id = "maya-shared-photo"
        assertTrue(vm.deleteMessages("maya-chen", setOf(id), MessageDeletionScope.ForEveryone))
        assertEquals(PinnedMessageStatus.Deleted, MessagePins.entries(vm.chat("maya-chen")!!).first { it.id == id }.status)
        assertTrue(vm.deleteMessages("maya-chen", setOf(id), MessageDeletionScope.ForMe))
        assertEquals(PinnedMessageStatus.Unavailable, MessagePins.entries(vm.chat("maya-chen")!!).first { it.id == id }.status)
        assertTrue(vm.setMessagePinned(owner, "maya-chen", id, false))
        assertFalse(vm.setMessagePinned(owner, "maya-chen", id, true))
    }
}
