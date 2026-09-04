package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class DeveloperParityStateTest {
    private class Fixture(available: Boolean = true) {
        var p = ProfileFixtures.marmota.copy(developerTools = DeveloperToolsState(isEnabled = true, debugMode = true))
        var signed = true
        var clock = 1000L
        val c = DeveloperParityController({ p }, { signed }, { 1 }, { id, reduce ->
            if (p.id != id) false else { p = reduce(p); true }
        }, available, { clock })
        fun open(surface: String = "packages") { c.open(p.id, surface); finish() }
        fun finish() { c.work?.let { c.complete(it.id) } }
        fun run(op: DeveloperOperation, target: String? = null) { assertTrue(c.begin(op, target)); finish() }
        val packages get() = DeveloperInspection.packages(p)
    }
    @Test fun republishKeepsIdentityAndRotateRetainsPriorLocalMaterial() {
        val f = Fixture(); f.open(); val initial = f.p.developerTools.keyPackage.id
        f.run(DeveloperOperation.Republish)
        assertEquals(initial, f.p.developerTools.keyPackage.id)
        f.run(DeveloperOperation.PublishNew)
        assertNotEquals(initial, f.p.developerTools.keyPackage.id)
        assertTrue(f.packages.first { it.id == initial }.relays.isEmpty())
        assertTrue(f.packages.first { it.id == initial }.local)
        assertEquals(1, f.packages.count { it.relays.isNotEmpty() })
    }
    @Test fun repeatedRotationHasDistinctIdentitiesAndDuplicateCompletionIsInert() {
        val f = Fixture(); f.open(); f.run(DeveloperOperation.PublishNew)
        val id = f.c.work!!.id; val after = f.p
        f.c.complete(id); assertEquals(after, f.p)
        f.run(DeveloperOperation.PublishNew)
        assertNotEquals(after.developerTools.keyPackage.id, f.p.developerTools.keyPackage.id)
    }
    @Test fun deleteDismissalIsInertAndConfirmedDeletionRetainsLocalMaterial() {
        val f = Fixture(); f.open(); val before = f.p
        assertTrue(f.c.begin(DeveloperOperation.DeletePackage, f.packages.first().id))
        f.finish(); assertEquals(before, f.p)
        f.c.dismiss(f.c.work!!.id); assertEquals(before, f.p)
        assertTrue(f.c.begin(DeveloperOperation.DeletePackage, f.packages.first().id))
        f.c.confirm(f.c.work!!.id); f.finish()
        assertFalse(f.p.connectionInformationPublished)
        assertTrue(f.packages.single().local); assertTrue(f.packages.single().relays.isEmpty())
    }
    @Test fun relayOnlyDeletionRemovesRecordAndCannotRepublishUnavailableLocalMaterial() {
        val f = Fixture(); f.c.inventoryExample(PackageInventoryExample.RelayOnly); f.open()
        assertFalse(f.c.begin(DeveloperOperation.Republish))
        assertTrue(f.c.begin(DeveloperOperation.DeletePackage, f.packages.single().id))
        f.c.confirm(f.c.work!!.id); f.finish(); assertTrue(f.packages.isEmpty())
    }
    @Test fun partialRotationRetryDoesNotCreateAnotherPackage() {
        val f = Fixture(); f.open(); f.c.chooseOutcome(DeveloperOutcome.Partial); f.run(DeveloperOperation.PublishNew)
        assertEquals(DeveloperPhase.Partial, f.c.work!!.phase)
        val material = f.p.developerTools.keyPackage.id; val count = f.packages.size
        f.c.retry(f.c.work!!.id); f.finish()
        assertEquals(DeveloperPhase.Complete, f.c.work!!.phase)
        assertEquals(material, f.p.developerTools.keyPackage.id); assertEquals(count, f.packages.size)
        assertEquals(f.p.settings.relays.map { it.url }.distinct(), f.packages.first { it.id == material }.relays)
    }
    @Test fun partialDeletionRetriesOnlyRemainingRelays() {
        val f = Fixture(); f.open(); f.c.chooseOutcome(DeveloperOutcome.Partial)
        val id = f.packages.single().id
        assertTrue(f.c.begin(DeveloperOperation.DeletePackage, id)); f.c.confirm(f.c.work!!.id); f.finish()
        assertEquals(DeveloperPhase.Partial, f.c.work!!.phase); val remaining = f.packages.single().relays.size
        assertTrue(remaining > 0); val old = f.c.work!!.id
        f.c.retry(old); f.c.complete(old); assertEquals(remaining, f.packages.single().relays.size)
        f.finish(); assertTrue(f.packages.single().relays.isEmpty())
    }
    @Test fun rotatingDoesNotDeleteOtherRelayOnlyPublication() {
        val f = Fixture(); f.c.inventoryExample(PackageInventoryExample.Mixed); f.open()
        val remote = f.packages.first { !it.local }
        f.run(DeveloperOperation.PublishNew); assertEquals(remote, f.packages.first { !it.local })
    }
    @Test fun failedAndUnavailableMutationsKeepInventoryAndRetryRecovers() {
        DeveloperOutcome.entries.filter { it in listOf(DeveloperOutcome.Failure, DeveloperOutcome.Unavailable) }.forEach { outcome ->
            val f = Fixture(); f.open(); val before = f.p
            f.c.chooseOutcome(outcome); f.run(DeveloperOperation.PublishNew)
            assertEquals(before, f.p); f.c.retry(f.c.work!!.id); f.finish()
            assertTrue(f.p.connectionInformationPublished); assertNotEquals(before.developerTools.keyPackage, f.p.developerTools.keyPackage)
        }
    }
    @Test fun emptyInventoryIsLoadedAndNewPublicationRecovers() {
        val f = Fixture(); f.c.inventoryExample(PackageInventoryExample.Empty); f.open()
        assertEquals(DeveloperPhase.Complete, f.c.work!!.phase); assertTrue(f.packages.isEmpty())
        assertFalse(f.c.begin(DeveloperOperation.Republish)); f.run(DeveloperOperation.PublishNew)
        assertEquals(1, f.packages.size)
    }
    @Test fun noRelaysDoesNotClaimPublicationOrSelfSendSuccess() {
        val f = Fixture(); f.p = f.p.copy(settings = f.p.settings.copy(relays = emptyList()))
        f.open(); val before = f.p; f.run(DeveloperOperation.PublishNew)
        assertEquals(DeveloperPhase.Unavailable, f.c.work!!.phase); assertEquals(before, f.p)
        f.c.close(f.p.id,"packages"); f.open("diagnostics"); f.run(DeveloperOperation.SendToSelf)
        assertEquals(DeveloperPhase.Unavailable, f.c.work!!.phase)
    }
    @Test fun routeExitAndReopenRejectsOldCompletions() {
        val f = Fixture(); f.open(); f.c.begin(DeveloperOperation.PublishNew); val old = f.c.work!!.id
        f.c.close(f.p.id,"packages"); f.c.open(f.p.id,"packages"); val current = f.c.work
        f.c.complete(old); assertEquals(current, f.c.work)
        assertEquals(KeyPackage.Fixture, f.p.developerTools.keyPackage)
    }
    @Test fun wrongRouteCannotStartPackageMutationAndOldRouteDisposalIsInert() {
        val f = Fixture(); f.open("diagnostics"); f.c.close(f.p.id,"packages")
        assertFalse(f.c.begin(DeveloperOperation.PublishNew)); assertTrue(f.c.begin(DeveloperOperation.SendToSelf))
    }
    @Test fun developerOffOrSignedOutRejectsLateMutation() {
        for (disable in listOf(true,false)) {
            val f = Fixture(); f.open(); f.c.begin(DeveloperOperation.PublishNew); val id = f.c.work!!.id
            if (disable) f.p = f.p.copy(developerTools = f.p.developerTools.withEnabled(false)) else f.signed = false
            val before = f.p; f.c.complete(id); assertEquals(before,f.p); assertNull(f.c.work)
        }
    }
    @Test fun sourceChangeInvalidatesPendingMutation() {
        val f = Fixture(); f.open(); f.c.begin(DeveloperOperation.PublishNew)
        f.p = f.p.copy(settings = f.p.settings.copy(relays = emptyList())); val before = f.p
        f.finish(); assertNull(f.c.work); assertEquals(before, f.p)
    }
    @Test fun busyStateRejectsDuplicateAndDifferentCommands() {
        val f = Fixture(); f.c.open(f.p.id,"packages")
        assertFalse(f.c.begin(DeveloperOperation.PublishNew)); f.finish()
        f.c.begin(DeveloperOperation.DeletePackage, f.packages.single().id)
        assertFalse(f.c.begin(DeveloperOperation.RefreshPackages))
    }
    @Test fun healthRefreshFailureRetainsPreviousSnapshotThenRecovers() {
        val f = Fixture(); f.open("diagnostics"); val before = f.p.developerTools.health!!
        assertEquals(before.total, before.connected + before.connecting + before.disconnected)
        assertTrue(before.attempts >= before.successes)
        f.c.chooseOutcome(DeveloperOutcome.Failure); f.run(DeveloperOperation.RefreshHealth)
        assertEquals(before, f.p.developerTools.health); assertEquals(DeveloperPhase.Failed, f.c.work!!.phase)
        f.c.retry(f.c.work!!.id); f.finish(); assertEquals(DeveloperPhase.Complete, f.c.work!!.phase)
    }
    @Test fun selfSendAddsSanitizedResultWithoutCreatingChatsOrChangingMessages() {
        val f = Fixture(); f.open("diagnostics"); val before = f.p.chats
        f.run(DeveloperOperation.SendToSelf); assertEquals(before, f.p.chats)
        val events = f.p.developerTools.diagnosticEvents
        assertTrue(events.last().text.contains("temporary chat removed")); assertFalse(events.last().text.contains(f.p.publicKey))
        f.finish(); assertEquals(events, f.p.developerTools.diagnosticEvents)
    }
    @Test fun performanceExpiresAtThirtyMinutesEvenAwayFromRoute() {
        val f = Fixture(); f.open("diagnostics"); f.c.performance(true)
        assertEquals(1_800_000L,f.c.remainingMillis()); f.c.close(f.p.id,"diagnostics")
        f.clock += 1_799_999; assertEquals(1L,f.c.remainingMillis())
        f.clock++; assertEquals(0L,f.c.remainingMillis())
    }
    @Test fun performanceEnableIsIdempotentAndCanStopEarly() {
        val f = Fixture(); f.c.performance(true); f.clock += 1000; f.c.performance(true)
        assertEquals(1_799_000L,f.c.remainingMillis()); f.c.performance(false); assertEquals(0L,f.c.remainingMillis())
    }
    @Test fun releaseGateAndDeveloperGateRejectPerformanceAndStreaming() {
        val f = Fixture(false); f.c.performance(true); assertEquals(0L,f.c.remainingMillis())
        f.p = f.p.copy(developerTools = f.p.developerTools.withEnabled(false)); val before = f.p
        f.c.streaming(true); f.c.performance(true); assertEquals(before,f.p)
    }
    @Test fun disablingDeveloperStopsPerformanceAndStreamButKeepsArtifacts() {
        val f = Fixture(); f.open(); f.c.streaming(true); f.c.performance(true)
        val before = f.p.developerTools; val disabled = before.withEnabled(false)
        assertFalse(disabled.streamingDebug); assertNull(disabled.performanceUntilMillis); assertEquals(before.keyPackages,disabled.keyPackages)
    }
    @Test fun inspectionSummaryExcludesDetailedPushValuesAndStreamControlsDoNotChangeHistory() {
        val f = Fixture(); val before = f.p.chats; f.c.streaming(true); assertEquals(before,f.p.chats)
        val snapshot = ConversationDebugPolicy.snapshot(f.p,"weekend-walks")!!
        snapshot.push.members.forEach { token ->
            assertFalse(snapshot.diagnosticSummary.contains(token.fingerprint))
            assertFalse(snapshot.diagnosticSummary.contains(token.serverKey))
        }
        assertEquals(snapshot.push.totalTokenCount,snapshot.push.members.size)
    }
    @Test fun pushRefreshRequiresExistingChatAndHonorsFailureRecovery() {
        val f = Fixture(); f.c.open(f.p.id,"push:missing"); assertNull(f.c.work)
        f.c.chooseOutcome(DeveloperOutcome.Unavailable); f.c.open(f.p.id,"push:fiatjaf"); f.finish()
        assertEquals(DeveloperPhase.Unavailable,f.c.work!!.phase); f.c.retry(f.c.work!!.id); f.finish()
        assertEquals(DeveloperPhase.Complete,f.c.work!!.phase)
    }
    @Test fun viewModelProfileRoundTripCannotResumeAnOldLease() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
        val p = vm.uiState.activeProfile!!; val c = vm.developerParity; c.open(p.id,"packages"); c.complete(c.work!!.id)
        c.begin(DeveloperOperation.PublishNew); val old = c.work!!.id
        vm.setDeveloperToolsEnabled(false); vm.setDeveloperToolsEnabled(true); c.open(p.id,"packages"); c.complete(old)
        assertEquals(p.developerTools.keyPackage,vm.uiState.activeProfile!!.developerTools.keyPackage)
    }
}
