package dev.ipf.whitenoise

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.compose.rememberNavController
import dev.ipf.whitenoise.model.AgentOperation
import dev.ipf.whitenoise.model.AgentOperationPhase
import dev.ipf.whitenoise.model.ProfileFixtures
import dev.ipf.whitenoise.navigation.AppRoute
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.navigation.WhiteNoiseNavHost
import dev.ipf.whitenoise.state.AppViewModel
import dev.ipf.whitenoise.ui.conversation.AgentOperationCard
import dev.ipf.whitenoise.ui.settings.AiAgentsScreen
import dev.ipf.whitenoise.ui.theme.WhiteNoiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AgentFeaturesInteractionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun settingsEntryNavigatesToAgentSetupAndBack() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        lateinit var navigate: (AppRoute) -> Unit
        compose.setContent {
            val nav = rememberNavController()
            navigate = nav::navigate
            WhiteNoiseTheme { WhiteNoiseNavHost(nav, vm) }
        }
        compose.runOnIdle { navigate(AppRoute.Settings()) }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("AI Agents"))
        compose.onNodeWithText("AI Agents").performClick()
        compose.onNodeWithText("Choose an agent").assertIsDisplayed()
        compose.onNodeWithText("Manual setup").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("AI Agents").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun everyConnectorCanRevealAProfileOwnedPublicPrompt() {
        val copied = mutableListOf<String>()
        compose.setContent {
            WhiteNoiseTheme {
                AiAgentsScreen(
                    ProfileFixtures.marmota,
                    onBack = {},
                    copyOverride = { _, value -> copied += value },
                    openDocsOverride = { true },
                )
            }
        }
        listOf("Hermes", "OpenClaw", "OpenCode", "Codex").forEachIndexed { index, name ->
            val id = name.lowercase()
            compose.onNodeWithText(name).performScrollTo().performClick()
            compose.onNodeWithText("Set up $name").assertIsDisplayed()
            compose.onNodeWithTag("ai_agents.prompt.$id").assertIsDisplayed()
            compose.onNodeWithTag("ai_agents.copy.$id").assertIsDisplayed().performClick()
            compose.onNodeWithTag("ai_agents.copy_feedback").assertIsDisplayed()
            compose.runOnIdle {
                assertEquals(index + 1, copied.size)
                assertTrue(copied.last().contains(ProfileFixtures.marmota.publicKey))
                assertTrue(copied.last().contains("approval"))
                assertTrue(copied.last().contains(name))
            }
            compose.onNodeWithContentDescription("Close").performClick()
            compose.onNodeWithTag("sheet.surface").assertDoesNotExist()
        }
    }

    @Test
    fun closingSetupWithoutCopyingPerformsNoAction() {
        var copies = 0
        compose.setContent {
            WhiteNoiseTheme {
                AiAgentsScreen(ProfileFixtures.marmota, onBack = {}, copyOverride = { _, _ -> copies++ })
            }
        }
        compose.onNodeWithText("Hermes").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithTag("sheet.surface").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, copies) }
    }

    @Test
    fun manualSetupCopiesOnlyThePublicKeyAndReportsDocsFailure() {
        val copied = mutableListOf<Pair<String, String>>()
        compose.setContent {
            WhiteNoiseTheme {
                AiAgentsScreen(
                    ProfileFixtures.marmota,
                    onBack = {},
                    copyOverride = { label, value -> copied += label to value },
                    openDocsOverride = { false },
                )
            }
        }
        compose.onNodeWithTag("ai_agents.copy_public_key").performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Public key" to ProfileFixtures.marmota.publicKey, copied.single()) }
        compose.onNodeWithText("Agent connector documentation").performScrollTo().performClick()
        compose.onNodeWithText("The connector documentation could not be opened.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun unavailablePublicKeyDisablesEveryPromptAction() {
        compose.setContent {
            WhiteNoiseTheme {
                AiAgentsScreen(ProfileFixtures.marmota.copy(publicKey = ""), onBack = {})
            }
        }
        compose.onNodeWithText("Your public key is unavailable. Return to Settings and try another profile.")
            .performScrollTo().assertIsDisplayed()
        listOf("Hermes", "OpenClaw", "OpenCode", "Codex").forEach { name ->
            compose.onNodeWithText(name).performScrollTo().assertIsNotEnabled()
        }
        compose.onNodeWithTag("ai_agents.copy_public_key").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithTag("sheet.surface").assertDoesNotExist()
    }

    @Test
    fun runningOperationShowsOwnedProgressAndExpandableDetails() {
        val operation = AgentOperation(
            "Review project files",
            "Reviewing the project files",
            AgentOperationPhase.Running,
            arguments = "Folder: Project Notes",
            statusDetail = "Reading documents",
            completedSteps = 2,
            totalSteps = 4,
        )
        compose.setContent {
            WhiteNoiseTheme { AgentOperationCard("running", operation, onLongPress = null) }
        }
        compose.onNodeWithText("In progress").assertIsDisplayed()
        compose.onNodeWithText("2 of 4 steps").assertIsDisplayed()
        compose.onNodeWithTag("conversation.agent_operation.progress.running").assertIsDisplayed()
        compose.onNodeWithText("Folder: Project Notes").assertDoesNotExist()
        compose.onNodeWithTag("conversation.agent_operation.running").performClick()
        compose.onNodeWithText("Folder: Project Notes").assertIsDisplayed()
    }

    @Test
    fun terminalOperationStatusesUseTextAndRemainInteractiveWithoutDeveloperMode() {
        val phases = listOf(
            AgentOperationPhase.Succeeded to "Completed",
            AgentOperationPhase.Failed to "Failed",
            AgentOperationPhase.Cancelled to "Cancelled",
            AgentOperationPhase.Unavailable to "Unavailable",
        )
        var selected by mutableStateOf(phases.first().first)
        compose.setContent {
            WhiteNoiseTheme {
                AgentOperationCard(
                    selected.name,
                    AgentOperation(selected.name, "$selected outcome", selected, statusDetail = "$selected detail"),
                    onLongPress = null,
                )
            }
        }
        phases.forEach { (phase, status) ->
            compose.runOnIdle { selected = phase }
            compose.onNodeWithText(status).assertIsDisplayed()
            compose.onNodeWithTag("conversation.agent_operation.${phase.name}").assertIsEnabled()
        }
        compose.onAllNodesWithText("Agent operation").assertCountEquals(1)
    }

    @Test
    fun largeTypeCanReachManualSetupAndDocumentation() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides androidx.compose.ui.unit.Density(density.density, 2f)) {
                WhiteNoiseTheme { AiAgentsScreen(ProfileFixtures.marmota, onBack = {}) }
            }
        }
        compose.onNodeWithText("Manual setup").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Agent connector documentation").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("ai_agents.copy_public_key").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hermes").performScrollTo().performClick()
        compose.onNodeWithTag("ai_agents.copy.hermes").assertIsDisplayed()
        compose.onNodeWithTag("ai_agents.setup_content").performScrollToNode(
            hasText("After setup, add the agent’s public key in New Message to start chatting."),
        )
        compose.onNodeWithTag("ai_agents.copy.hermes").assertIsDisplayed()
        compose.onNodeWithContentDescription("Close").performClick()
        compose.onNodeWithTag("sheet.surface").assertDoesNotExist()
    }
}
