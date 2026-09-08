package dev.ipf.whitenoise.scenarios

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.AppRoute
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel
import org.junit.Assert.*
import org.junit.Test

class ScenarioCatalogTest {
    @Test fun everyVariantPreparesAValidDestinationWithItsOwnState() {
        println("Catalog: ${ScenarioCatalog.all.size} scenarios, ${ScenarioCatalog.all.sumOf { it.variants.size }} variants")
        ScenarioCatalog.all.forEach { definition -> definition.variants.forEach { variant ->
            val run = ScenarioRun(1, definition, variant)
            try {
                val vm = run.model
                if (definition.id != "account-entry") assertNotNull("${definition.id}/${variant.id}", vm.uiState.activeProfile)
                when (val route = run.destination.route) {
                    is AppRoute.Conversation -> assertNotNull(vm.chat(route.chatId))
                    is AppRoute.ChatInfo -> assertNotNull(vm.chat(route.chatId))
                    is AppRoute.EditGroup -> assertTrue(vm.chat(route.chatId)!!.hasAuthoritativeGroupAdmin(vm.uiState.activeProfileId!!))
                    is AppRoute.AddGroupMembers -> assertNotNull(vm.chat(route.chatId))
                    is AppRoute.GroupMembers -> assertNotNull(vm.chat(route.chatId))
                    is AppRoute.PersonProfile -> assertNotNull(vm.person(route.personId))
                    is AppRoute.ProfileSetup -> assertEquals(AccessPhase.ProfileSetup, vm.accessAttempt!!.phase)
                    else -> Unit
                }
            } catch (failure: Throwable) { throw AssertionError("${definition.id}/${variant.id}", failure) }
            finally { run.close() }
        } }
    }
    @Test fun catalogIdsAreUniqueAndEveryVariantHasInstructions() {
        assertEquals(ScenarioCatalog.all.size, ScenarioCatalog.all.map { it.id }.toSet().size)
        ScenarioCatalog.all.forEach {
            assertTrue(it.description.isNotBlank())
            assertTrue(it.variants.isNotEmpty())
            assertEquals(it.variants.size, it.variants.map { v -> v.id }.toSet().size)
            it.variants.forEach { variant -> assertTrue(variant.description.length > variant.title.length) }
        }
    }
    @Test fun destructiveRunsAndRestartsCannotChangeOriginalSession() {
        val original = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        val before = original.uiState
        val definition = ScenarioCatalog.find("sign-out")!!
        val sessions = ScenarioSessions()
        sessions.start(definition, definition.variants.first())
        val first = sessions.run!!
        assertTrue(first.model.eraseAppData(WipeConfirmationPhrase.make(first.model.uiState.profiles.map { it.id })))
        assertTrue(first.model.uiState.profiles.isEmpty())
        sessions.restart()
        val second = sessions.run!!
        assertNotSame(first.model, second.model)
        assertTrue(second.generation > first.generation)
        assertNotNull(second.model.uiState.activeProfile)
        assertEquals(before, original.uiState)
        assertFalse(original.uiState.activeProfile!!.developerTools.isEnabled)
        sessions.exit()
        assertNull(sessions.run)
        assertEquals(before, original.uiState)
    }
    @Test fun setupVariantsLaunchChecksAndRestartClearsProgress() {
        val definition = ScenarioCatalog.find("setup")!!
        val sessions = ScenarioSessions()
        definition.variants.forEach { variant ->
            sessions.start(definition, variant)
            val first = sessions.run!!
            assertEquals(ProfileSetupScenario.valueOf(variant.id), first.model.profileSetup.session!!.scenario)
            sessions.restart()
            assertNotSame(first.model.profileSetup, sessions.run!!.model.profileSetup)
            assertEquals(first.model.profileSetup.session, sessions.run!!.model.profileSetup.session)
        }
        sessions.exit()
    }
    @Test fun crossAccountNotificationTargetsAnotherPreparedAccount() {
        val d = ScenarioCatalog.find("incoming-example")!!
        val run = ScenarioRun(1, d, d.variants.first { it.id == "NotificationOtherProfile" })
        assertEquals(2, run.model.uiState.signedInProfiles.count { it.chats.isNotEmpty() })
        run.close()
    }
}
