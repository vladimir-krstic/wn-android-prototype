package dev.ipf.whitenoise.state

import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import org.junit.Assert.*
import org.junit.Test

class TranslationPreferencesTest {
    @Test fun wrongProfileCannotChangePreferences() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial) }
        vm.updateTranslationPreferences("other", TranslationPreferences(automatic = true))
        assertFalse(vm.uiState.activeProfile!!.settings.translation.automatic)
        vm.updateTranslationPreferences(vm.uiState.activeProfileId!!, TranslationPreferences(automatic = true, target = TranslationLanguage.French))
        assertEquals(TranslationLanguage.French, vm.uiState.activeProfile!!.settings.translation.target)
    }
    @Test fun developerScenarioIsOwnerBoundAndOneShot() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
        vm.selectTranslationScenario(TranslationScenario.MissingPack)
        assertEquals(TranslationScenario.Unavailable, vm.consumeTranslationScenario("other"))
        assertEquals(TranslationScenario.MissingPack, vm.consumeTranslationScenario(vm.uiState.activeProfileId!!))
        assertEquals(TranslationScenario.Ready, vm.consumeTranslationScenario(vm.uiState.activeProfileId!!))
    }
    @Test fun pendingScenarioCannotRunAfterDeveloperToolsAreDisabled() {
        val vm = AppViewModel().apply { completeSignIn(OnboardingOrigin.Initial); setDeveloperToolsEnabled(true) }
        vm.selectTranslationScenario(TranslationScenario.Timeout); vm.setDeveloperToolsEnabled(false)
        assertEquals(TranslationScenario.Ready, vm.consumeTranslationScenario(vm.uiState.activeProfileId!!))
    }
}
