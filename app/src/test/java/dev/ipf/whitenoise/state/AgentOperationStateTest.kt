package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.AgentConnector
import dev.ipf.whitenoise.model.AgentConversationExamples
import dev.ipf.whitenoise.model.AgentOperation
import dev.ipf.whitenoise.model.AgentOperationPhase
import dev.ipf.whitenoise.model.AgentSetupPolicy
import dev.ipf.whitenoise.model.ChatMembership
import dev.ipf.whitenoise.model.ChatTimelineEntry
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentOperationStateTest {
    private fun model(): AppViewModel = AppViewModel().apply {
        completeSignIn(OnboardingOrigin.Initial)
        setDeveloperToolsEnabled(true)
    }

    @Test
    fun setupAcceptsOnlyUsablePublicKeysAndKeepsAllNamedConnectors() {
        assertEquals(
            listOf(AgentConnector.Hermes, AgentConnector.OpenClaw, AgentConnector.OpenCode, AgentConnector.Codex),
            AgentSetupPolicy.connectors,
        )
        assertEquals(ProfileFixtures.marmota.publicKey, AgentSetupPolicy.publicKeyOrNull(ProfileFixtures.marmota))
        assertNull(AgentSetupPolicy.publicKeyOrNull(ProfileFixtures.marmota.copy(publicKey = "")))
        assertNull(AgentSetupPolicy.publicKeyOrNull(ProfileFixtures.marmota.copy(publicKey = "nsec1-not-public")))
    }

    @Test
    fun operationProgressIsBoundedAndOnlyPositiveTotalsProduceProgress() {
        val running = AgentOperation("Read", "Reading", AgentOperationPhase.Running, completedSteps = 7, totalSteps = 4)
        assertEquals(4, running.boundedCompletedSteps)
        assertEquals(1f, running.progress)
        assertTrue(running.isInProgress)
        assertNull(running.copy(totalSteps = 0).progress)
        assertFalse(running.copy(phase = AgentOperationPhase.Succeeded).isInProgress)
    }

    @Test
    fun examplesCoverStreamingProgressAndEveryTerminalOutcome() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "fiatjaf" }
        val updated = AgentConversationExamples.add(chat, profile)!!
        val examples = updated.timeline.filter { it.id.startsWith(AgentConversationExamples.IdPrefix) }
            .filterIsInstance<ChatTimelineEntry.Message>().map { it.message }
        assertTrue(examples.first().deliveryState.name == "Streaming")
        assertEquals(
            AgentOperationPhase.entries.toSet(),
            examples.mapNotNull { it.agentOperation?.phase }.toSet(),
        )
        assertTrue(examples.zipWithNext().all { (first, second) ->
            (second.dayOrdinal * 1_440 + second.minuteOfDay) -
                (first.dayOrdinal * 1_440 + first.minuteOfDay) >= 6
        })
        assertEquals(0.5f, examples.first { it.agentOperation?.phase == AgentOperationPhase.Running }.agentOperation!!.progress)
        assertEquals("Calendar access is unavailable", updated.preview)
    }

    @Test
    fun reinsertingExamplesReplacesTheOwnedRowsWithoutDuplicates() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first { it.id == "fiatjaf" }
        val once = AgentConversationExamples.add(chat, profile)!!
        val twice = AgentConversationExamples.add(once, profile)!!
        assertEquals(
            once.timeline.count { it.id.startsWith(AgentConversationExamples.IdPrefix) },
            twice.timeline.count { it.id.startsWith(AgentConversationExamples.IdPrefix) },
        )
    }

    @Test
    fun endedConversationCannotReceiveAgentExamples() {
        val profile = ProfileFixtures.marmota
        val chat = profile.chats.first().copy(membership = ChatMembership.Left)
        assertNull(AgentConversationExamples.add(chat, profile))
    }

    @Test
    fun viewModelRequiresDeveloperGateAndExactActiveProfile() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        val profileId = vm.uiState.activeProfileId!!
        val chatId = vm.uiState.activeProfile!!.chats.first { it.id == "fiatjaf" }.id
        assertFalse(vm.addAgentConversationExamples(profileId, chatId))
        vm.setDeveloperToolsEnabled(true)
        assertFalse(vm.addAgentConversationExamples("another-profile", chatId))
        assertTrue(vm.addAgentConversationExamples(profileId, chatId))
    }

    @Test
    fun profileSwitchRejectsAStaleExampleCallback() {
        val vm = model()
        val firstProfileId = vm.uiState.activeProfileId!!
        val firstChatId = vm.uiState.activeProfile!!.chats.first { it.id == "fiatjaf" }.id
        vm.completeSignIn(OnboardingOrigin.AddProfile)
        vm.setDeveloperToolsEnabled(true)
        assertFalse(vm.addAgentConversationExamples(firstProfileId, firstChatId))
        assertFalse(
            vm.uiState.profiles.first { it.id == firstProfileId }.chats.first { it.id == firstChatId }
                .timeline.any { it.id.startsWith(AgentConversationExamples.IdPrefix) },
        )
    }

    @Test
    fun ordinaryOperationRowsRemainAfterDeveloperToolsAreDisabled() {
        val vm = model()
        val profileId = vm.uiState.activeProfileId!!
        val chatId = vm.uiState.activeProfile!!.chats.first { it.id == "fiatjaf" }.id
        assertTrue(vm.addAgentConversationExamples(profileId, chatId))
        vm.setDeveloperToolsEnabled(false)
        assertFalse(vm.uiState.activeProfile!!.developerTools.isEnabled)
        assertTrue(vm.chat(chatId)!!.timeline.filterIsInstance<ChatTimelineEntry.Message>().any {
            it.message.agentOperation != null
        })
    }
}
