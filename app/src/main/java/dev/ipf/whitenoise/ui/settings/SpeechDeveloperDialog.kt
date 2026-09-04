package dev.ipf.whitenoise.ui.settings

import dev.ipf.whitenoise.R

import androidx.compose.ui.res.stringResource

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
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.developer_read_aloud_outcomes)) }, text = {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
            item { SettingsLink(stringResource(R.string.developer_engine_catalog), controller.catalogScenario.developerLabel, { catalog = true }) }
            item { SettingsLink(stringResource(R.string.developer_audio_environment), controller.audioScenario?.toString() ?: stringResource(R.string.developer_device_audio), { audio = true }) }
            item { SettingsSection(stringResource(R.string.developer_local_background_controls_example)) }
            item { Text(stringResource(R.string.developer_this_example_does_not_speak_post_notifications_or_star)) }
            item { Text(stringResource(R.string.developer_speech_session_state, example.sessionId, example.phase.name,
                stringResource(if (example.paused) R.string.developer_paused else R.string.developer_playing),
                example.notificationVisible, example.sourceRequested), Modifier.testTag("speech.example.state")) }
            item { TextButton(onClick = {
                staleCommand = SpeechControlCommand(example.profileId, example.sessionId, SpeechControlAction.Stop)
                example = SpeechBackgroundExample(profile.id, example.sessionId + 1)
            }) { Text(stringResource(R.string.developer_new_session)) } }
            item { TextButton(onClick = { example = example.notificationStarted(true) }) { Text(stringResource(R.string.developer_notification_starts)) } }
            item { TextButton(onClick = { example = example.notificationStarted(false) }) { Text(stringResource(R.string.developer_notification_start_fails)) } }
            item { TextButton(onClick = { example = example.background(clock, null) }) { Text(stringResource(R.string.developer_background_without_lock)) } }
            item { TextButton(onClick = { example = example.background(clock, 60_000) }) { Text(stringResource(R.string.developer_background_lock_in_one_minute)) } }
            item { TextButton(onClick = { example = example.background(clock, 0) }) { Text(stringResource(R.string.developer_background_lock_immediately)) } }
            item { TextButton(onClick = { clock += 60_000; example = example.tick(clock) }) { Text(stringResource(R.string.developer_advance_one_minute)) } }
            item { TextButton(onClick = { example = example.foreground(clock) }) { Text(stringResource(R.string.developer_return_to_foreground)) } }
            SpeechControlAction.entries.forEach { action -> item {
                TextButton(onClick = { example = example.command(SpeechControlCommand(profile.id, example.sessionId, action)) }) { Text(action.name) }
            } }
            item { TextButton(onClick = { staleCommand?.let { example = example.command(it) } }, enabled = staleCommand != null) { Text(stringResource(R.string.developer_send_previous_session_command)) } }
            item { TextButton(onClick = { example = example.profileChanged(null) }) { Text(stringResource(R.string.developer_profile_exits)) } }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } })
    if (catalog) SpeechSettingsChoices(stringResource(R.string.developer_engine_catalog), SpeechCatalogScenario.entries.map { value ->
        SpeechSettingOption(value.developerLabel, controller.catalogScenario == value) { controller.chooseCatalogScenario(value); catalog = false }
    }, { catalog = false })
    if (audio) {
        val values = listOf(null, SpeechAudioEnvironment(mediaActive = true), SpeechAudioEnvironment(mediaActive = false), SpeechAudioEnvironment(focusAvailable = false))
        val labels = listOf(
            stringResource(R.string.developer_device_audio),
            stringResource(R.string.developer_other_media_playing),
            stringResource(R.string.developer_no_other_media_playing),
            stringResource(R.string.developer_audio_focus_denied),
        )
        SpeechSettingsChoices(stringResource(R.string.developer_audio_environment), values.mapIndexed { index, value ->
            SpeechSettingOption(labels[index], controller.audioScenario == value) { controller.chooseAudioScenario(value); audio = false }
        }, { audio = false })
    }
}
