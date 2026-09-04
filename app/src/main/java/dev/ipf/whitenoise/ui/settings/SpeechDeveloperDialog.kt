package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import dev.ipf.whitenoise.ui.conversation.ReadAloudController

/** Developer-only controls exercise local outcomes; no service or notification is created. */
@Composable
internal fun SpeechDeveloperDialog(profile: Profile, controller: ReadAloudController, onDismiss: () -> Unit) {
    if (!profile.developerTools.isEnabled) return
    var catalog by remember { mutableStateOf(false) }
    var audio by remember { mutableStateOf(false) }
    var example by remember(profile.id) { mutableStateOf(SpeechBackgroundExample(profile.id, 1)) }
    var clock by remember(profile.id) { mutableLongStateOf(0L) }
    var staleCommand by remember(profile.id) { mutableStateOf<SpeechControlCommand?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Read Aloud outcomes") }, text = {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
            item { SettingsLink("Engine catalog", controller.catalogScenario.developerLabel, { catalog = true }) }
            item { SettingsLink("Audio environment", controller.audioScenario?.toString() ?: "Device audio", { audio = true }) }
            item { SettingsSection("Local background-controls example") }
            item { Text("This example does not speak, post notifications or start a background service.") }
            item { Text("Session ${example.sessionId} · ${example.phase.name} · ${if (example.paused) "Paused" else "Playing"}\nNotification visible: ${example.notificationVisible}\nSource requested: ${example.sourceRequested}", Modifier.testTag("speech.example.state")) }
            item { TextButton(onClick = {
                staleCommand = SpeechControlCommand(example.profileId, example.sessionId, SpeechControlAction.Stop)
                example = SpeechBackgroundExample(profile.id, example.sessionId + 1)
            }) { Text("New session") } }
            item { TextButton(onClick = { example = example.notificationStarted(true) }) { Text("Notification starts") } }
            item { TextButton(onClick = { example = example.notificationStarted(false) }) { Text("Notification start fails") } }
            item { TextButton(onClick = { example = example.background(clock, null) }) { Text("Background without lock") } }
            item { TextButton(onClick = { example = example.background(clock, 60_000) }) { Text("Background; lock in one minute") } }
            item { TextButton(onClick = { example = example.background(clock, 0) }) { Text("Background; lock immediately") } }
            item { TextButton(onClick = { clock += 60_000; example = example.tick(clock) }) { Text("Advance one minute") } }
            item { TextButton(onClick = { example = example.foreground(clock) }) { Text("Return to foreground") } }
            SpeechControlAction.entries.forEach { action -> item {
                TextButton(onClick = { example = example.command(SpeechControlCommand(profile.id, example.sessionId, action)) }) { Text(action.name) }
            } }
            item { TextButton(onClick = { staleCommand?.let { example = example.command(it) } }, enabled = staleCommand != null) { Text("Send previous session command") } }
            item { TextButton(onClick = { example = example.profileChanged(null) }) { Text("Profile exits") } }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
    if (catalog) SpeechSettingsChoices("Engine catalog", SpeechCatalogScenario.entries.map { value ->
        SpeechSettingOption(value.developerLabel, controller.catalogScenario == value) { controller.chooseCatalogScenario(value); catalog = false }
    }, { catalog = false })
    if (audio) {
        val values = listOf(null, SpeechAudioEnvironment(mediaActive = true), SpeechAudioEnvironment(mediaActive = false), SpeechAudioEnvironment(focusAvailable = false))
        val labels = listOf("Device audio", "Other media playing", "No other media playing", "Audio focus denied")
        SpeechSettingsChoices("Audio environment", values.mapIndexed { index, value ->
            SpeechSettingOption(labels[index], controller.audioScenario == value) { controller.chooseAudioScenario(value); audio = false }
        }, { audio = false })
    }
}
