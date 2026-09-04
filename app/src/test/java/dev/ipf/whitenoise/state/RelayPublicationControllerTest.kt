package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import org.junit.Assert.*
import org.junit.Test

class RelayPublicationControllerTest {
    private class Fixture {
        var profiles = listOf(ProfileFixtures.marmota.copy(developerTools = DeveloperToolsState(isEnabled = true)), ProfileFixtures.openCircuit)
        var active = profiles.first().id
        val controller = RelayPublicationController({ profiles }, { active }, { id -> profiles.any { it.id == id } })
        val profile get() = profiles.first { it.id == active }
        fun open(surface: String = "route") = controller.open(active, surface)
        fun refresh(scenario: RelayPublicationScenario = RelayPublicationScenario.Published) {
            controller.chooseScenario(scenario); assertTrue(controller.begin(RelayPublicationOperation.Refresh)); controller.complete(controller.work!!.id)
        }
    }
    @Test fun everyRefreshProjectionIsDistinctFromSocketStatus() {
        val f = Fixture(); f.open()
        val expected = mapOf(
            RelayPublicationScenario.Published to emptySet(),
            RelayPublicationScenario.MissingPosting to setOf(PublishedRelayList.Posting),
            RelayPublicationScenario.MissingInbox to setOf(PublishedRelayList.Inbox),
            RelayPublicationScenario.MissingBoth to PublishedRelayList.entries.toSet(),
        )
        expected.forEach { (scenario, missing) ->
            f.refresh(scenario); assertEquals(missing, f.controller.projection(f.profile).missing)
            f.profiles = f.profiles.map { if (it.id == f.active) it.copy(settings = it.settings.copy(
                relays = it.settings.relays.map { relay -> relay.copy(status = RelayConnectionStatus.Disconnected) })) else it }
            assertEquals(missing, f.controller.projection(f.profile).missing)
        }
    }
    @Test fun failureKeepsLastProjectionAndRetryUsesNewOwnedRequest() {
        val f = Fixture(); f.open(); f.refresh(RelayPublicationScenario.MissingInbox)
        val before = f.controller.projection(f.profile)
        f.controller.chooseScenario(RelayPublicationScenario.Failure)
        assertTrue(f.controller.begin(RelayPublicationOperation.Refresh)); val failedId = f.controller.work!!.id
        f.controller.complete(failedId)
        assertEquals(RelayPublicationWorkPhase.Failed, f.controller.work!!.phase); assertEquals(before, f.controller.projection(f.profile))
        f.controller.retry(failedId); assertTrue(f.controller.work!!.id > failedId)
        f.controller.complete(f.controller.work!!.id); assertEquals(RelayProjectionPhase.Published, f.controller.projection(f.profile).phase)
    }
    @Test fun unavailableRefreshIsTextualStateAndCanRecover() {
        val f = Fixture(); f.open(); f.refresh(RelayPublicationScenario.Unavailable)
        assertEquals(RelayProjectionPhase.Unavailable, f.controller.projection(f.profile).phase)
        assertEquals(RelayPublicationWorkPhase.Unavailable, f.controller.work!!.phase)
        f.controller.retry(f.controller.work!!.id); f.controller.complete(f.controller.work!!.id)
        assertEquals(RelayProjectionPhase.Published, f.controller.projection(f.profile).phase)
    }
    @Test fun roleAndUrlChangesMarkOnlyAffectedLists() {
        val f = Fixture(); f.open(); val before = f.profile.settings.relays
        val profileChanged = before.mapIndexed { index, relay -> if (index == 0) relay.copy(roles = relay.roles - RelayRole.Profile) else relay }
        f.controller.relaySettingsChanged(f.active, before, profileChanged)
        assertEquals(setOf(PublishedRelayList.Posting), f.controller.projection(f.profile).missing)
        val socketOnly = profileChanged.map { it.copy(status = RelayConnectionStatus.Reconnecting) }
        f.controller.relaySettingsChanged(f.active, profileChanged, socketOnly)
        assertEquals(setOf(PublishedRelayList.Posting), f.controller.projection(f.profile).missing)
        val chatOnly = socketOnly.mapIndexed { index, relay -> if (index == 0) relay.copy(roles = relay.roles - RelayRole.ChatMessages) else relay }
        f.controller.relaySettingsChanged(f.active, socketOnly, chatOnly)
        assertEquals(setOf(PublishedRelayList.Posting), f.controller.projection(f.profile).missing)
    }
    @Test fun changingCapturedRelaysCancelsPendingWorkAndStaleCompletion() {
        val f = Fixture(); f.open(); assertTrue(f.controller.begin(RelayPublicationOperation.Refresh)); val id = f.controller.work!!.id
        val before = f.profile.settings.relays; val after = before.mapIndexed { i, r -> if (i == 0) r.copy(roles = r.roles - RelayRole.Inbox) else r }
        f.controller.relaySettingsChanged(f.active, before, after)
        assertNull(f.controller.work); f.controller.complete(id)
        assertEquals(setOf(PublishedRelayList.Inbox), f.controller.projection(f.profile).missing)
    }
    @Test fun publishClearsOnlyOwningProfilesMissingProjection() {
        val f = Fixture(); f.open(); f.refresh(RelayPublicationScenario.MissingBoth)
        val other = f.profiles[1]
        val otherAfter = other.settings.relays.mapIndexed { index, relay ->
            if (index == 0) relay.copy(roles = relay.roles - RelayRole.Inbox) else relay
        }
        f.controller.relaySettingsChanged(other.id, other.settings.relays, otherAfter)
        val otherBefore = f.controller.projection(other)
        assertTrue(f.controller.begin(RelayPublicationOperation.PublishMissing)); f.controller.complete(f.controller.work!!.id)
        assertEquals(RelayProjectionPhase.Published, f.controller.projection(f.profile).phase)
        assertEquals(otherBefore, f.controller.projection(other))
    }
    @Test fun backAndProfileSwitchInvalidateRouteLease() {
        val f = Fixture(); f.open(); assertTrue(f.controller.begin(RelayPublicationOperation.Refresh)); val id = f.controller.work!!.id
        f.controller.close(f.active, "route"); f.controller.complete(id); assertNull(f.controller.work)
        f.open("new-route"); assertTrue(f.controller.begin(RelayPublicationOperation.Refresh)); val second = f.controller.work!!.id
        f.active = f.profiles[1].id; f.controller.reconcile(); f.controller.complete(second)
        assertNull(f.controller.work); assertEquals(RelayPublicationScenario.Current, f.controller.scenario)
    }
    @Test fun publishRequiresMissingProjectionAndUnavailablePublishKeepsIt() {
        val f = Fixture(); f.open(); assertFalse(f.controller.begin(RelayPublicationOperation.PublishMissing))
        f.refresh(RelayPublicationScenario.MissingPosting); f.controller.chooseScenario(RelayPublicationScenario.Unavailable)
        assertTrue(f.controller.begin(RelayPublicationOperation.PublishMissing)); f.controller.complete(f.controller.work!!.id)
        assertEquals(setOf(PublishedRelayList.Posting), f.controller.projection(f.profile).missing)
    }
    @Test fun eraseDropsRetainedProfileProjectionsAndLeases() {
        val f = Fixture(); f.open(); f.refresh(RelayPublicationScenario.MissingBoth); f.controller.erase()
        assertNull(f.controller.work); assertTrue(f.controller.projections.isEmpty())
    }
    @Test fun ordinaryRefreshKeepsKnownMissingListsUntilTheyArePublished() {
        val f = Fixture(); f.open(); val before = f.profile.settings.relays
        val after = before.mapIndexed { index, relay -> if (index == 0) relay.copy(roles = relay.roles - RelayRole.Profile) else relay }
        f.controller.relaySettingsChanged(f.active, before, after)
        assertTrue(f.controller.begin(RelayPublicationOperation.Refresh)); f.controller.complete(f.controller.work!!.id)
        assertEquals(setOf(PublishedRelayList.Posting), f.controller.projection(f.profile).missing)
        assertTrue(f.controller.begin(RelayPublicationOperation.PublishMissing)); f.controller.complete(f.controller.work!!.id)
        assertEquals(RelayProjectionPhase.Published, f.controller.projection(f.profile).phase)
    }
}
