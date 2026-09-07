package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.GlobalLibraryScenario
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class GlobalLibraryScenarioTest {
    @Test fun developerOutcomeIsOneShotAndCannotBeConsumedByAnotherOwner() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
        val id = vm.uiState.activeProfileId!!
        vm.selectGlobalLibraryScenario(GlobalLibraryScenario.Partial)
        assertEquals(GlobalLibraryScenario.Failed, vm.consumeGlobalLibraryScenario("other"))
        assertEquals(GlobalLibraryScenario.Partial, vm.consumeGlobalLibraryScenario(id))
        assertEquals(GlobalLibraryScenario.Ready, vm.consumeGlobalLibraryScenario(id))
    }
    @Test fun disablingDeveloperToolsPreventsPendingFailureFromAffectingBrowsing() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
        vm.selectGlobalLibraryScenario(GlobalLibraryScenario.Failed)
        vm.setDeveloperToolsEnabled(false)
        assertEquals(GlobalLibraryScenario.Ready, vm.consumeGlobalLibraryScenario(vm.uiState.activeProfileId!!))
    }
}
