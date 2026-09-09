package dev.ipf.whitenoise.scenarios

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import dev.ipf.whitenoise.R
import kotlinx.coroutines.launch
import dev.ipf.whitenoise.ui.settings.*
import dev.ipf.whitenoise.ui.theme.WhiteNoiseSpacing
import dev.ipf.whitenoise.model.*

@Composable
internal fun ScenarioCatalogScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val filtered = ScenarioCatalog.all.filter { definition ->
        listOf(definition.title, definition.group, definition.description).plus(definition.variants.flatMap { listOf(it.title, it.description) })
            .any { it.contains(query.trim(), ignoreCase = true) }
    }
    SettingsScaffold(title = "Scenarios", onBack = onBack) {
        SettingsList {
            item { SettingsCallout(title = "Test a flow", text = "Choose a scenario, then tap a variant to start immediately. Each run uses temporary accounts and chats. Exit Scenario returns to the page you started from.") }
            item { OutlinedTextField(query, { query = it }, label = { Text("Search all scenarios") }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin).testTag("scenarios.search")) }
            if (filtered.isEmpty()) item { SettingsExplainer("No scenarios match this search.") }
            filtered.groupBy { it.group }.forEach { (group, definitions) ->
                item(key = group) { SettingsSection(group) }
                definitions.forEach { definition -> item(key = definition.id) {
                    SettingsGroup { row { SettingsLink(definition.title,
                        "${definition.variants.size} variants · ${definition.description}", { onOpen(definition.id) },
                        modifier = Modifier.testTag("scenario.${definition.id}")) } }
                } }
            }
        }
    }
}

@Composable
internal fun ScenarioVariantsScreen(definition: ScenarioDefinition, onBack: () -> Unit, onRun: (ScenarioVariant) -> Unit) {
    SettingsScaffold(title = definition.title, onBack = onBack) {
        SettingsList {
            item { SettingsCallout(title = "Scenario", text = definition.description) }
            item { SettingsExplainer("Tap a variant to run it now. Restart resets its data and retry counters. Exit Scenario returns here. Tap the info icon during a run for its title and instructions.") }
            definition.deviceNote?.let { item { SettingsCallout(title = "Android integration", text = it) } }
            definition.variants.forEach { variant -> item(key = variant.id) {
                SettingsGroup { row { SettingsLink(variant.title, variant.description, { onRun(variant) },
                    modifier = Modifier.testTag("scenario.variant.${variant.id}")) } }
            } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun ScenarioControls(run: ScenarioRun, onRestart: () -> Unit, onExit: () -> Unit) {
    val tooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()
    Surface(tonalElevation = WhiteNoiseSpacing.Related) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                .padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin).testTag("scenario.controls"),
            horizontalArrangement = Arrangement.spacedBy(WhiteNoiseSpacing.Related),
        ) {
            TooltipBox(
                positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
                state = tooltipState,
                tooltip = {
                    RichTooltip(title = { Text(run.definition.title, Modifier.testTag("scenario.info.title")) }) {
                        Text(run.variant.description, Modifier.testTag("scenario.info.description"))
                    }
                },
            ) {
                IconButton(
                    onClick = {
                        if (tooltipState.isVisible) tooltipState.dismiss()
                        else scope.launch { tooltipState.show() }
                    },
                    modifier = Modifier.testTag("scenario.info"),
                ) {
                    Icon(painterResource(R.drawable.ic_info), contentDescription = "Scenario information")
                }
            }
            TextButton(onClick = onRestart, modifier = Modifier.testTag("scenario.restart")) { Text("Restart") }
            TextButton(onClick = onExit, modifier = Modifier.testTag("scenario.exit")) { Text("Exit Scenario") }
            if (run.definition.id == "incoming-lock" && run.model.incoming.locked)
                TextButton(onClick = { run.model.incoming.chooseLock(false) }) { Text("Unlock") }
            if (run.definition.id == "expiry")
                TextButton(onClick = { run.model.retention.advanceExampleClock(60_000) }) { Text("Advance Time 1 min") }
        }
    }
}

@Composable
internal fun ScenarioExampleScreen(kind: String, variant: String, onBack: () -> Unit) {
    var example by remember(kind, variant) { mutableStateOf(SpeechBackgroundExample("scenario", 2).let { start ->
        if (kind == "speech-command") start.copy(paused = variant == "Resume").command(SpeechControlCommand("scenario", 2, SpeechControlAction.valueOf(variant)))
        else when(variant) {
            "NotificationStarts" -> start.notificationStarted(true)
            "NotificationFails" -> start.notificationStarted(false)
            "Background" -> start.background(0, null)
            "LockAfterMinute" -> start.background(0, 60_000).tick(60_000)
            "LockImmediately" -> start.background(0, 0)
            "StaleCommand" -> start.command(SpeechControlCommand("scenario", 1, SpeechControlAction.Stop))
            "ProfileExits" -> start.profileChanged(null)
            else -> start
        }
    }) }
    SettingsScaffold("Background control scenario", onBack) {
        SettingsList {
            item { SettingsCallout(title = "Local example", text = "This example does not speak, post notifications or start a background service.") }
            item { SettingsGroup {
                row { SettingsMetadata("Session", example.sessionId.toString()) }
                row { SettingsMetadata("State", example.phase.name) }
                row { SettingsMetadata("Playback", if(example.paused) "Paused" else if (example.active) "Playing" else "Stopped") }
                row { SettingsMetadata("Notification visible", example.notificationVisible.toString()) }
                row { SettingsMetadata("Source requested", example.sourceRequested.toString()) }
            } }
            item { SettingsGroup { row { SettingsAction("Return to foreground", { example = example.foreground(60_000) }) } } }
        }
    }
}
