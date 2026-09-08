package dev.ipf.whitenoise.scenarios

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.ipf.whitenoise.navigation.OnboardingOrigin
import dev.ipf.whitenoise.state.AppViewModel

internal class ScenarioRun(val generation: Long, val definition: ScenarioDefinition, val variant: ScenarioVariant) {
    private val store = ViewModelStore()
    val model = ViewModelProvider.create(store, viewModelFactory { initializer { AppViewModel() } })[AppViewModel::class]
    val destination: ScenarioDestination
    var entryConsumed by mutableStateOf(false)
    init {
        model.completeSignIn(OnboardingOrigin.Initial)
        model.setDeveloperToolsEnabled(true)
        model.dismissDiagnosticsPrompt(model.uiState.activeProfileId!!)
        destination = variant.prepare(model)
    }
    fun close() { store.clear() }
}

/** Retained across configuration changes, deliberately not persisted across process death. */
internal class ScenarioSessions : ViewModel() {
    var run by mutableStateOf<ScenarioRun?>(null); private set
    private var generation = 0L
    fun start(definition: ScenarioDefinition, variant: ScenarioVariant) {
        val next = ScenarioRun(++generation, definition, variant)
        val previous = run
        run = next
        previous?.close()
    }
    fun restart() { run?.let { start(it.definition, it.variant) } }
    fun exit() { val previous = run; run = null; previous?.close() }
    override fun onCleared() { exit() }
}

internal val LocalScenarioRun = staticCompositionLocalOf<ScenarioRun?> { null }
internal val LocalScenarioSessions = staticCompositionLocalOf<ScenarioSessions?> { null }

/** A production screen handles only its own entry action, once per fresh run. */
@Composable
internal fun ScenarioEntry(accepts: (String) -> Boolean, enter: (String) -> Unit) {
    val run = LocalScenarioRun.current
    val action = run?.destination?.action
    LaunchedEffect(run?.generation, action) {
        if (run != null && action != null && !run.entryConsumed && accepts(action)) {
            run.entryConsumed = true
            enter(action)
        }
    }
}
