package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class DiagnosticsConsentTest {
    private fun signedIn() = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }

    @Test fun initialSignInWaitsForChatsAndDoesNotRecordPresentationAsDismissal() {
        val vm = signedIn()
        assertNull(vm.uiState.diagnosticsPromptProfile(chatsResumed = false))
        val profile = vm.uiState.diagnosticsPromptProfile(chatsResumed = true)!!
        assertFalse(profile.diagnostics.analyticsEnabled)
        assertFalse(profile.diagnostics.loggingEnabled)
        assertFalse(profile.diagnostics.hasSeenPrompt)
        assertTrue(profile.diagnostics.records.isEmpty())
        assertNull(vm.uiState.diagnosticsPromptProfile(false))
        assertEquals(profile.id, vm.uiState.diagnosticsPromptProfile(true)?.id)
    }

    @Test fun bothSignUpOriginsAndAddSignInScheduleTheNewProfile() {
        for (origin in OnboardingOrigin.entries) {
            val vm = AppViewModel()
            vm.completeSignUp(origin, "New profile", "", null)
            assertEquals(vm.uiState.activeProfileId, vm.uiState.diagnosticsPromptProfile(true)?.id)
        }
        val vm = signedIn()
        vm.dismissDiagnosticsPrompt(vm.uiState.activeProfileId!!)
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertEquals(ProfileFixtures.openCircuit.id, vm.uiState.diagnosticsPromptProfile(true)?.id)
    }

    @Test fun dismissalIsProfileScopedAndStoredChoicesSurviveSignOut() {
        val vm = signedIn()
        val id = vm.uiState.activeProfileId!!
        vm.setAnalyticsEnabled(id, true)
        vm.dismissDiagnosticsPrompt("wrong-profile")
        assertNotNull(vm.uiState.diagnosticsPromptProfile(true))
        vm.dismissDiagnosticsPrompt(id)
        assertNull(vm.uiState.diagnosticsPromptProfile(true))
        vm.signOutActiveProfile(wipeData = false)
        vm.completeSignIn(OnboardingOrigin.Initial)
        assertTrue(vm.uiState.activeProfile!!.diagnostics.analyticsEnabled)
        assertTrue(vm.uiState.activeProfile!!.diagnostics.hasSeenPrompt)
        assertNull(vm.uiState.diagnosticsPromptProfile(true))
    }

    @Test fun profilesDoNotShareConsentOrRecordsAndSwitchingDoesNotPrompt() {
        val vm = signedIn()
        val first = vm.uiState.activeProfileId!!
        vm.setDiagnosticLoggingEnabled(first, true)
        vm.dismissDiagnosticsPrompt(first)
        val original = vm.uiState.activeProfile!!.diagnostics
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        assertEquals(DiagnosticsState(), vm.uiState.activeProfile!!.diagnostics)
        val second = vm.uiState.activeProfileId!!
        vm.dismissDiagnosticsPrompt(second)
        vm.selectProfile(first)
        assertNull(vm.uiState.diagnosticsPromptProfile(true))
        assertEquals(original, vm.uiState.activeProfile!!.diagnostics)
    }

    @Test fun developerGateAndLoggingConsentAreIndependent() {
        val vm = signedIn()
        val id = vm.uiState.activeProfileId!!
        assertTrue(vm.setAnalyticsEnabled(id, true))
        assertTrue(vm.setDiagnosticLoggingEnabled(id, true))
        val choices = vm.uiState.activeProfile!!.diagnostics
        vm.setDeveloperToolsEnabled(true)
        vm.setDebugMode(true)
        vm.setDeveloperToolsEnabled(false)
        assertEquals(choices, vm.uiState.activeProfile!!.diagnostics)
        assertFalse(vm.uiState.activeProfile!!.developerTools.debugMode)
    }

    @Test fun disablingLoggingRetainsDataAndClearingDoesNotChangeConsentOrReseed() {
        val vm = signedIn()
        val id = vm.uiState.activeProfileId!!
        vm.setDiagnosticLoggingEnabled(id, true)
        val files = vm.uiState.activeProfile!!.diagnostics.records
        assertEquals(32_000L, vm.uiState.activeProfile!!.diagnostics.storedBytes)
        vm.setDiagnosticLoggingEnabled(id, false)
        assertEquals(files, vm.uiState.activeProfile!!.diagnostics.records)
        assertTrue(vm.clearDiagnosticRecords(id))
        assertFalse(vm.uiState.activeProfile!!.diagnostics.loggingEnabled)
        assertEquals(listOf(0, 0), vm.uiState.activeProfile!!.diagnostics.records.map { it.byteCount })
        vm.setDiagnosticLoggingEnabled(id, true)
        assertEquals(0L, vm.uiState.activeProfile!!.diagnostics.storedBytes)
        assertFalse(vm.clearDiagnosticRecords(id))
    }

    @Test fun summariesCoverIndependentConsentAndWipeRemovesIt() {
        assertEquals("Off", DiagnosticsState().summary)
        assertEquals("Analytics", DiagnosticsState(analyticsEnabled = true).summary)
        assertEquals("Logs", DiagnosticsState(loggingEnabled = true).summary)
        assertEquals("On", DiagnosticsState(analyticsEnabled = true, loggingEnabled = true).summary)
        val vm = signedIn()
        vm.dismissDiagnosticsPrompt(vm.uiState.activeProfileId!!)
        vm.signOutActiveProfile(wipeData = true)
        vm.completeSignIn(OnboardingOrigin.Initial)
        assertFalse(vm.uiState.activeProfile!!.diagnostics.hasSeenPrompt)
    }

    @Test fun diagnosticExportContainsOnlySanitizedDeterministicLogContent() {
        val diagnostics = DiagnosticsState().withLogging(
            enabled = true,
            profileId = ProfileFixtures.marmota.id,
            profileName = ProfileFixtures.marmota.name,
        )

        val export = diagnostics.diagnosticLogExportText

        assertTrue(export.startsWith("White Noise Diagnostic Logs\n"))
        assertTrue(export.endsWith("info | message.pipeline.ready\n"))
        assertEquals(2, "Log file:".toRegex().findAll(export).count())
        assertTrue(export.contains("Recorded size: 24000 bytes"))
        assertTrue(export.contains("info | relay.connected"))
        assertFalse(export.contains(ProfileFixtures.marmota.id))
        assertFalse(export.contains(ProfileFixtures.marmota.name))
        assertFalse(export.contains(diagnostics.records.first().filename))
    }

    @Test fun readAndArchiveUndoCannotMutateAnotherProfile() {
        val vm = signedIn()
        val id = vm.uiState.activeProfileId!!
        val original = vm.chat("catalog-direct-replies")!!
        val undo = ChatListUndo.capture(id, original, ChatListAction.Read)
        vm.markChatUnread(original.id, false)
        vm.undoChatListAction(undo)
        assertEquals(original.unreadCount, vm.chat(original.id)!!.unreadCount)
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        val second = vm.uiState.activeProfile
        vm.undoChatListAction(undo)
        assertEquals(second, vm.uiState.activeProfile)
    }
}
