package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class ProfileSetupTest {
    @Test fun clearingOutgoingSignInKeyCannotCancelSetupForEitherOrigin() {
        OnboardingOrigin.entries.forEach { origin ->
            val vm = AppViewModel()
            if (origin == OnboardingOrigin.AddProfile) vm.completeSignIn(OnboardingOrigin.Initial)
            val owner = vm.uiState.activeProfileId
            assertTrue(vm.beginPrivateKeySignIn(origin, LoginPrototypeData.privateKey))
            val request = vm.accessAttempt!!
            vm.signInKeyEdited(request.id)
            assertEquals(request, vm.accessAttempt)
            assertFalse(vm.advanceAccess(request.id, request.phase))
            val setup = vm.profileSetup.session!!
            // The outgoing form receives the host's credential clearing after setup begins.
            vm.signInKeyEdited(request.id)
            assertEquals(setup, vm.profileSetup.session)
            assertEquals(AccessPhase.ProfileSetup, vm.accessAttempt!!.phase)
            assertEquals(owner, vm.uiState.activeProfileId)
            assertTrue(vm.profileSetup.complete(setup.id, setup.work!!))
        }
    }

    @Test fun credentialEditsClearOnlyTheMatchingFailedSignIn() {
        val vm = AppViewModel(AccessScenario.SignInFailure)
        vm.beginPrivateKeySignIn(OnboardingOrigin.Initial, LoginPrototypeData.privateKey)
        val first = vm.accessAttempt!!
        vm.advanceAccess(first.id, first.phase)
        assertEquals(AccessPhase.Failed, vm.accessAttempt!!.phase)
        vm.signInKeyEdited(first.id + 1)
        assertNotNull(vm.accessAttempt)
        vm.signInKeyEdited(first.id)
        assertNull(vm.accessAttempt)
        vm.beginPrivateKeySignIn(OnboardingOrigin.Initial, LoginPrototypeData.privateKey)
        val second = vm.accessAttempt!!
        vm.signInKeyEdited(first.id)
        assertEquals(second, vm.accessAttempt)
    }

    private fun start(scenario: ProfileSetupScenario = ProfileSetupScenario.Ready) = AppViewModel(initialSetupScenario = scenario).apply {
        assertTrue(beginPrivateKeySignIn(OnboardingOrigin.Initial, LoginPrototypeData.privateKey))
        accessAttempt!!.let { assertFalse(advanceAccess(it.id, it.phase)) }
        assertEquals(AccessPhase.ProfileSetup, accessAttempt!!.phase)
    }

    private fun drain(vm: AppViewModel) {
        repeat(12) {
            val session = vm.profileSetup.session ?: return
            val work = session.work ?: return
            assertTrue(vm.profileSetup.complete(session.id, work))
        }
        fail("Automatic work must stop at a decision or completion")
    }

    private fun act(vm: AppViewModel, step: ProfileSetupStep, action: ProfileSetupAction) {
        assertTrue(vm.profileSetup.act(vm.profileSetup.session!!.id, step, action))
        drain(vm)
    }

    @Test fun readyRequiresDeviceAcknowledgmentAndExplicitOpenChats() {
        val vm = start()
        val id = vm.accessAttempt!!.id
        assertFalse(vm.finishProfileSetup(id))
        drain(vm)
        val setup = vm.profileSetup.session!!
        assertEquals(ProfileSetupStep.entries, setup.checks.map { it.step })
        assertEquals(ProfileSetupStatus.Attention, setup.check(ProfileSetupStep.Device).status)
        assertEquals(ProfileSetupStatus.Waiting, setup.check(ProfileSetupStep.Messaging).status)
        assertFalse(vm.finishProfileSetup(id))
        assertTrue(vm.uiState.profiles.isEmpty())
        act(vm, ProfileSetupStep.Device, ProfileSetupAction.Acknowledge)
        assertTrue(vm.profileSetup.session!!.ready)
        assertNull(vm.uiState.activeProfile)
        assertTrue(vm.finishProfileSetup(id))
        assertEquals(ProfileFixtures.MARMOTA_ID, vm.uiState.activeProfileId)
        assertNull(vm.profileSetup.session)
        assertFalse(vm.finishProfileSetup(id))
    }

    @Test fun attentionAllowsOptionalSkipAndRequiresExplicitDefaults() {
        val vm = start(ProfileSetupScenario.Attention)
        drain(vm)
        act(vm, ProfileSetupStep.Profile, ProfileSetupAction.Skip)
        val setup = vm.profileSetup.session!!
        assertEquals(ProfileSetupStatus.Skipped, setup.check(ProfileSetupStep.Profile).status)
        assertEquals(ProfileSetupStatus.Skipped, setup.check(ProfileSetupStep.Follows).status)
        assertFalse(vm.profileSetup.act(setup.id, ProfileSetupStep.Relays, ProfileSetupAction.Skip))
        assertFalse(vm.finishProfileSetup(setup.id))
        act(vm, ProfileSetupStep.Relays, ProfileSetupAction.UseDefaults)
        act(vm, ProfileSetupStep.Device, ProfileSetupAction.Acknowledge)
        assertTrue(vm.profileSetup.session!!.ready)
        assertEquals(ProfileSetupPolicy.defaultRelays, vm.profileSetup.session!!.candidate.settings.relays)
    }

    @Test fun recoveryPreservesFailedSaveDraftAndRetriesMessagingWithoutAnotherAcknowledgment() {
        val vm = start(ProfileSetupScenario.Recovery)
        drain(vm)
        assertEquals(ProfileSetupIssue.ProfileLookup, vm.profileSetup.session!!.check(ProfileSetupStep.Profile).issue)
        act(vm, ProfileSetupStep.Profile, ProfileSetupAction.Retry)
        val id = vm.profileSetup.session!!.id
        val draft = SetupProfileDraft("Updated name", "Keep this draft", ProfileAvatar.Monogram)
        vm.profileSetup.updateDraft(id, draft)
        act(vm, ProfileSetupStep.Profile, ProfileSetupAction.SaveProfile)
        assertEquals(ProfileSetupIssue.ProfileSave, vm.profileSetup.session!!.check(ProfileSetupStep.Profile).issue)
        assertEquals(draft, vm.profileSetup.session!!.draft)
        assertNotEquals(draft.name, vm.profileSetup.session!!.candidate.name)
        act(vm, ProfileSetupStep.Profile, ProfileSetupAction.SaveProfile)
        assertEquals(draft.name, vm.profileSetup.session!!.candidate.name)
        assertFalse(vm.profileSetup.act(id, ProfileSetupStep.Relays, ProfileSetupAction.UseDefaults))
        assertFalse(vm.profileSetup.act(id, ProfileSetupStep.Relays, ProfileSetupAction.FindSettings))
        assertTrue(vm.profileSetup.session!!.invalidDiscoveryUrl)
        vm.profileSetup.updateDiscoveryUrl(id, "wss://discovery.example.com")
        val before = vm.profileSetup.session!!.candidate
        act(vm, ProfileSetupStep.Relays, ProfileSetupAction.FindSettings)
        assertEquals(before, vm.profileSetup.session!!.candidate)
        act(vm, ProfileSetupStep.Inbox, ProfileSetupAction.UseDefaults)
        assertEquals(SetupDeviceDiscovery.Possible, vm.profileSetup.session!!.deviceDiscovery)
        act(vm, ProfileSetupStep.Device, ProfileSetupAction.Acknowledge)
        assertEquals(ProfileSetupIssue.Messaging, vm.profileSetup.session!!.check(ProfileSetupStep.Messaging).issue)
        assertFalse(vm.finishProfileSetup(id))
        act(vm, ProfileSetupStep.Messaging, ProfileSetupAction.Retry)
        assertEquals(ProfileSetupStatus.Done, vm.profileSetup.session!!.check(ProfileSetupStep.Device).status)
        assertTrue(vm.finishProfileSetup(id))
        assertEquals(draft.name, vm.uiState.activeProfile!!.name)
    }

    @Test fun cancellationInvalidatesWorkAndFreshAttemptKeepsOnlyAppliedValues() {
        val vm = start(ProfileSetupScenario.ProfileSaveFailure)
        drain(vm)
        val oldId = vm.profileSetup.session!!.id
        vm.profileSetup.updateDraft(oldId, SetupProfileDraft("Published", "Applied", ProfileAvatar.Monogram))
        act(vm, ProfileSetupStep.Profile, ProfileSetupAction.SaveProfile)
        act(vm, ProfileSetupStep.Profile, ProfileSetupAction.SaveProfile)
        val oldWork = vm.profileSetup.session!!.let { session ->
            vm.profileSetup.act(session.id, ProfileSetupStep.Device, ProfileSetupAction.Acknowledge)
            vm.profileSetup.session!!.work!!
        }
        vm.cancelAccess()
        assertNull(vm.profileSetup.session)
        assertTrue(vm.uiState.retainedProfiles.isEmpty())
        assertFalse(vm.profileSetup.complete(oldId, oldWork))
        vm.beginPrivateKeySignIn(OnboardingOrigin.Initial, LoginPrototypeData.privateKey)
        vm.accessAttempt!!.let { vm.advanceAccess(it.id, it.phase) }
        val fresh = vm.profileSetup.session!!
        assertNotEquals(oldId, fresh.id)
        assertEquals("Published", fresh.candidate.name)
        assertEquals(ProfileSetupStatus.Checking, fresh.check(ProfileSetupStep.Profile).status)
        assertEquals(ProfileSetupScenario.Ready, fresh.scenario)
        assertFalse(vm.profileSetup.complete(oldId, oldWork))
        assertNull(vm.uiState.activeProfile)
    }

    @Test fun cancelledUnconfirmedDraftDoesNotReturn() {
        val vm = start(ProfileSetupScenario.Attention)
        drain(vm)
        vm.profileSetup.updateDraft(vm.profileSetup.session!!.id, SetupProfileDraft("Unconfirmed", "", ProfileAvatar.Monogram))
        vm.cancelAccess()
        vm.beginPrivateKeySignIn(OnboardingOrigin.Initial, LoginPrototypeData.privateKey)
        vm.accessAttempt!!.let { vm.advanceAccess(it.id, it.phase) }
        assertEquals(ProfileFixtures.marmota.name, vm.profileSetup.session!!.draft.name)
    }

    @Test fun duplicateAndForeignCommandsCannotAdvanceOrApply() {
        val vm = start()
        val setup = vm.profileSetup.session!!
        val work = setup.work!!
        assertFalse(vm.profileSetup.complete(setup.id + 1, work))
        assertTrue(vm.profileSetup.complete(setup.id, work))
        val after = vm.profileSetup.session
        assertFalse(vm.profileSetup.complete(setup.id, work))
        assertEquals(after, vm.profileSetup.session)
        assertFalse(vm.profileSetup.act(setup.id, ProfileSetupStep.Device, ProfileSetupAction.Acknowledge))
        assertFalse(vm.beginPrivateKeySignIn(OnboardingOrigin.Initial, LoginPrototypeData.privateKey))
    }

    @Test fun addProfileKeepsExistingProfileAndSwitchingInvalidatesSetup() {
        val vm = AppViewModel()
        vm.completeSignIn(OnboardingOrigin.Initial)
        val existing = vm.uiState.activeProfile!!
        vm.beginPrivateKeySignIn(OnboardingOrigin.AddProfile, LoginPrototypeData.privateKey)
        vm.accessAttempt!!.let { vm.advanceAccess(it.id, it.phase) }
        val setup = vm.profileSetup.session!!
        assertEquals(existing, vm.uiState.activeProfile)
        vm.cancelAccess()
        assertEquals(existing, vm.uiState.activeProfile)
        assertFalse(vm.profileSetup.complete(setup.id, setup.work!!))
        vm.beginPrivateKeySignIn(OnboardingOrigin.AddProfile, LoginPrototypeData.privateKey)
        vm.accessAttempt!!.let { vm.advanceAccess(it.id, it.phase) }
        drain(vm)
        act(vm, ProfileSetupStep.Device, ProfileSetupAction.Acknowledge)
        assertEquals(existing, vm.uiState.activeProfile)
        assertTrue(vm.finishProfileSetup(vm.profileSetup.session!!.id))
        assertEquals(ProfileFixtures.openCircuit.id, vm.uiState.activeProfileId)
        assertEquals(existing, vm.uiState.profiles.first { it.id == existing.id })
    }

    @Test fun developerSelectionIsOneShotAndSignUpAmberDoNotConsumeIt() {
        val vm = AppViewModel()
        vm.selectSetupScenario(ProfileSetupScenario.Recovery)
        assertEquals(ProfileSetupScenario.Ready, vm.nextSetupScenario)
        vm.completeSignIn(OnboardingOrigin.Initial)
        vm.setDeveloperToolsEnabled(true)
        vm.selectSetupScenario(ProfileSetupScenario.Recovery)
        vm.beginProfileCreation(OnboardingOrigin.AddProfile, "Name", "", null)
        vm.cancelAccess()
        vm.beginAmberSignIn(OnboardingOrigin.AddProfile)
        vm.cancelAccess()
        assertEquals(ProfileSetupScenario.Recovery, vm.nextSetupScenario)
        vm.signOutActiveProfile(false)
        vm.beginPrivateKeySignIn(OnboardingOrigin.Initial, LoginPrototypeData.privateKey)
        assertEquals(ProfileSetupScenario.Recovery, vm.accessAttempt!!.setupScenario)
        assertEquals(ProfileSetupScenario.Ready, vm.nextSetupScenario)
    }

    @Test fun profileSwitchInvalidatesPendingSetupAndWipeRemovesAppliedValues() {
        val vm = AppViewModel(initialSetupScenario = ProfileSetupScenario.Attention)
        vm.completeSignIn(OnboardingOrigin.Initial)
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        vm.selectProfile(ProfileFixtures.MARMOTA_ID)
        vm.beginPrivateKeySignIn(OnboardingOrigin.AddProfile, LoginPrototypeData.privateKey)
        vm.accessAttempt!!.let { vm.advanceAccess(it.id, it.phase) }
        val old = vm.profileSetup.session!!
        vm.selectProfile(ProfileFixtures.openCircuit.id)
        assertNull(vm.profileSetup.session)
        assertFalse(vm.profileSetup.complete(old.id, old.work!!))

        val initial = start(ProfileSetupScenario.Attention)
        drain(initial)
        initial.profileSetup.updateDraft(initial.profileSetup.session!!.id, SetupProfileDraft("Applied name", "", ProfileAvatar.Monogram))
        act(initial, ProfileSetupStep.Profile, ProfileSetupAction.SaveProfile)
        act(initial, ProfileSetupStep.Relays, ProfileSetupAction.UseDefaults)
        act(initial, ProfileSetupStep.Device, ProfileSetupAction.Acknowledge)
        initial.finishProfileSetup(initial.profileSetup.session!!.id)
        initial.signOutActiveProfile(wipeData = true)
        initial.beginPrivateKeySignIn(OnboardingOrigin.Initial, LoginPrototypeData.privateKey)
        initial.accessAttempt!!.let { initial.advanceAccess(it.id, it.phase) }
        assertEquals(ProfileFixtures.marmota.name, initial.profileSetup.session!!.candidate.name)
    }

    @Test fun successfulDiscoveryOfNoUsableRoutesRequiresExplicitDefaults() {
        var applied = 0
        val controller = ProfileSetupController({ it == 1L }, { applied++ })
        val profile = ProfileFixtures.marmota.copy(settings = ProfileFixtures.marmota.settings.copy(relays = emptyList()))
        controller.start(1L, profile, ProfileSetupScenario.InconclusiveRelays)
        repeat(3) { controller.session!!.work?.let { controller.complete(1L, it) } }
        assertFalse(controller.act(1L, ProfileSetupStep.Relays, ProfileSetupAction.UseDefaults))
        controller.updateDiscoveryUrl(1L, "wss://relay.example.com")
        assertTrue(controller.act(1L, ProfileSetupStep.Relays, ProfileSetupAction.FindSettings))
        controller.complete(1L, controller.session!!.work!!)
        assertEquals(0, applied)
        assertEquals(ProfileSetupIssue.MissingRelays, controller.session!!.check(ProfileSetupStep.Relays).issue)
        assertFalse(controller.session!!.ready)
        assertTrue(controller.act(1L, ProfileSetupStep.Relays, ProfileSetupAction.UseDefaults))
        controller.complete(1L, controller.session!!.work!!)
        assertEquals(1, applied)
        assertEquals(ProfileSetupPolicy.defaultRelays, controller.session!!.candidate.settings.relays)
    }

    @Test fun everyFocusedScenarioIsCompletableAndKeepsAllRows() {
        ProfileSetupScenario.entries.forEach { scenario ->
            val vm = start(scenario)
            repeat(20) {
                drain(vm)
                val setup = vm.profileSetup.session!!
                val attention = setup.checks.firstOrNull { it.status == ProfileSetupStatus.Attention }
                if (attention != null) {
                    val actions = setup.actions(attention.step)
                    val action = listOf(ProfileSetupAction.Skip, ProfileSetupAction.UseDefaults, ProfileSetupAction.Retry, ProfileSetupAction.Acknowledge).first { it in actions }
                    act(vm, attention.step, action)
                }
            }
            assertTrue(scenario.name, vm.profileSetup.session!!.ready)
            assertEquals(6, vm.profileSetup.session!!.checks.size)
        }
    }

    @Test fun roleReadinessNeedsOneUsableEndpointAndNoReadonlySubstitute() {
        val profile = ProfileFixtures.marmota
        assertTrue(ProfileSetupPolicy.hasRoute(profile, RelayRole.Inbox))
        val dead = profile.copy(settings = profile.settings.copy(relays = profile.settings.relays.map { it.copy(status = RelayConnectionStatus.Disconnected) }))
        assertFalse(ProfileSetupPolicy.hasRoute(dead, RelayRole.Inbox))
        val readOnly = profile.copy(settings = profile.settings.copy(relays = profile.settings.relays.map { it.copy(isReadOnly = true) }))
        assertFalse(ProfileSetupPolicy.hasRoute(readOnly, RelayRole.Profile))
        val vm = start(ProfileSetupScenario.PartialRelayFailure)
        drain(vm)
        assertEquals(ProfileSetupStatus.Done, vm.profileSetup.session!!.check(ProfileSetupStep.Relays).status)
        assertEquals(ProfileSetupStatus.Done, vm.profileSetup.session!!.check(ProfileSetupStep.Inbox).status)
    }

    @Test fun discoveryValidationRejectsUnsafeValuesWithoutNetworkAccess() {
        listOf("ws://relay.example.com", "wss://user@relay.example.com", "wss://localhost", "wss://127.0.0.1",
            "wss://[::1]", "wss://relay.example.com/\u202Ehidden", "wss://relay.example.com\n", "wss://relay.example.com:99999", "wss://relay.example.com/%0A", "wss://relay.example.com/%E2%80%AE")
            .forEach { assertNull(it, ProfileSetupPolicy.discoveryUrl(it)) }
        assertEquals("wss://relay.example.com", ProfileSetupPolicy.discoveryUrl(" wss://RELAY.example.com/ "))
    }
}
