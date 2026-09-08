package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.model.WipeConfirmationPhrase
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class QuickAccountSwitchingTest {
    private fun multiple() = AppViewModel().apply {
        completeSignIn(OnboardingOrigin.Initial)
        completeSignIn(OnboardingOrigin.AddProfile)
    }

    @Test fun offByDefaultAndRequiresTwoSignedInAccounts() {
        val vm = AppViewModel()
        assertNull(vm.quickSwitchAccount())
        vm.completeSignIn(OnboardingOrigin.Initial)
        vm.setQuickAccountSwitching(true)
        assertNull(vm.uiState.nextQuickSwitchProfile)
        assertNull(vm.quickSwitchAccount())
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertNotNull(vm.uiState.nextQuickSwitchProfile)
        vm.setQuickAccountSwitching(false)
        val active = vm.uiState.activeProfileId
        assertNull(vm.quickSwitchAccount())
        assertEquals(active, vm.uiState.activeProfileId)
    }

    @Test fun repeatedTapsCycleInStableOrderAndKeepDraftsAndPreference() {
        val vm = multiple()
        vm.setQuickAccountSwitching(true)
        val order = vm.uiState.signedInProfiles.map { it.id }
        val drafts = vm.uiState.profiles.associate { it.id to it.chats.map { chat -> chat.draftText } }
        var index = order.indexOf(vm.uiState.activeProfileId)
        repeat(order.size * 2) {
            index = (index + 1) % order.size
            assertEquals(order[index], vm.quickSwitchAccount()?.id)
            assertEquals(order, vm.uiState.signedInProfiles.map { it.id })
            assertTrue(vm.uiState.quickAccountSwitching)
        }
        assertEquals(drafts, vm.uiState.profiles.associate { it.id to it.chats.map { chat -> chat.draftText } })
    }

    @Test fun signedOutRetainedAccountsAreSkippedAndSignOutKeepsPreference() {
        val vm = multiple()
        vm.setQuickAccountSwitching(true)
        val departing = vm.uiState.activeProfileId!!
        vm.signOutActiveProfile(wipeData = false)
        assertTrue(vm.uiState.profiles.any { it.id == departing })
        assertTrue(vm.uiState.quickAccountSwitching)
        repeat(vm.uiState.signedInProfiles.size * 2) { assertNotEquals(departing, vm.quickSwitchAccount()?.id) }
    }

    @Test fun removingAccountsDownToOneHidesShortcutAndAddingRestoresIt() {
        val vm = multiple()
        vm.setQuickAccountSwitching(true)
        while (vm.uiState.signedInProfiles.size > 1) vm.signOutActiveProfile(wipeData = true)
        assertNull(vm.uiState.nextQuickSwitchProfile)
        assertTrue(vm.uiState.quickAccountSwitching)
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertNotNull(vm.uiState.nextQuickSwitchProfile)
    }

    @Test fun missingActiveAccountNeverGuessesADestination() {
        val profile = ProfileFixtures.marmota
        val state = AppUiState(listOf(profile, profile.copy(id = "other")), "missing", setOf(profile.id, "other"),
            quickAccountSwitching = true)
        assertNull(state.nextQuickSwitchProfile)
    }

    @Test fun erasingAppDataResetsOptIn() {
        val vm = multiple()
        vm.setQuickAccountSwitching(true)
        assertTrue(vm.eraseAppData(WipeConfirmationPhrase.make(vm.uiState.profiles.map { it.id })))
        assertFalse(vm.uiState.quickAccountSwitching)
        assertNull(vm.quickSwitchAccount())
    }
}
