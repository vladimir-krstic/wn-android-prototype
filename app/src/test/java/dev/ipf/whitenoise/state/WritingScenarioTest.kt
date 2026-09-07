package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.WritingScenario
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class WritingScenarioTest {
    @Test fun developerOutcomeIsOneShotAndCannotBeConsumedByAnotherOwner() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
        val id = vm.uiState.activeProfileId!!
        vm.selectWritingScenario(WritingScenario.Timeout)
        assertEquals(WritingScenario.Unavailable, vm.consumeWritingScenario("other"))
        assertEquals(WritingScenario.Timeout, vm.consumeWritingScenario(id))
        assertEquals(WritingScenario.Ready, vm.consumeWritingScenario(id))
    }
    @Test fun disablingDeveloperToolsPreventsPendingFailureFromAffectingBrowsing() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
        vm.selectWritingScenario(WritingScenario.Unavailable)
        vm.setDeveloperToolsEnabled(false)
        assertEquals(WritingScenario.Ready, vm.consumeWritingScenario(vm.uiState.activeProfileId!!))
    }
}
