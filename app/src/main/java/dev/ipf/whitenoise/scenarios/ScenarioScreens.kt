package dev.ipf.whitenoise.scenarios

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
            item { SettingsCallout(title = "Test a flow", text = "Choose a scenario, then tap a variant to start immediately. Each run uses temporary accounts and chats. Exit Scenario restores your app session.") }
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
            item { SettingsExplainer("Tap a variant to run it now. Restart resets its data and retry counters. Change Variant returns here. Exit Scenario restores your previous app session.") }
            definition.deviceNote?.let { item { SettingsCallout(title = "Android integration", text = it) } }
            definition.variants.forEach { variant -> item(key = variant.id) {
                SettingsGroup { row { SettingsLink(variant.title, variant.description, { onRun(variant) },
                    modifier = Modifier.testTag("scenario.variant.${variant.id}")) } }
            } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScenarioControls(run: ScenarioRun, onRestart: () -> Unit, onChange: () -> Unit, onExit: () -> Unit) {
    var details by rememberSaveable(run.generation) { mutableStateOf(false) }
    Surface(tonalElevation = WhiteNoiseSpacing.Related) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = WhiteNoiseSpacing.CompactScreenMargin)) {
            TextButton(onClick = { details = !details }, modifier = Modifier.testTag("scenario.running")) {
                Text("Scenario: ${run.definition.title} · ${run.variant.title}", style = MaterialTheme.typography.labelMedium)
            }
            if (details) Text(run.variant.description, style = MaterialTheme.typography.bodySmall)
            FlowRow {
                TextButton(onClick = onRestart, modifier = Modifier.testTag("scenario.restart")) { Text("Restart") }
                TextButton(onClick = onChange, modifier = Modifier.testTag("scenario.change")) { Text("Change Variant") }
                TextButton(onClick = onExit, modifier = Modifier.testTag("scenario.exit")) { Text("Exit Scenario") }
                if (run.definition.id == "incoming-lock" && run.model.incoming.locked)
                    TextButton(onClick = { run.model.incoming.chooseLock(false) }) { Text("Unlock") }
                if (run.definition.id == "expiry")
                    TextButton(onClick = { run.model.retention.advanceExampleClock(60_000) }) { Text("Advance Time 1 min") }
            }
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
