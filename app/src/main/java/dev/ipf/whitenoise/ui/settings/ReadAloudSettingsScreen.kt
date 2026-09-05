@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.ipf.whitenoise.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog
import dev.ipf.whitenoise.ui.components.WhiteNoiseLazyColumn as LazyColumn
import dev.ipf.whitenoise.ui.components.WhiteNoiseDialogChoiceRow
import dev.ipf.whitenoise.ui.components.WhiteNoiseTextField
import dev.ipf.whitenoise.ui.conversation.LocalReadAloudController
import java.text.NumberFormat
import java.util.Locale

private enum class SpeechSettingChoice { Engine, Voice, Rate, Volume }
internal data class SpeechSettingOption(val title: String, val selected: Boolean, val subtitle: String? = null,
    val enabled: Boolean = true, val action: () -> Unit)

@Composable
internal fun SpeechSettingsChoices(title: String, options: List<SpeechSettingOption>, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp).selectableGroup()) {
            itemsIndexed(options) { index, option -> WhiteNoiseDialogChoiceRow(option.title, option.selected,
                onClick = option.action, subtitle = option.subtitle, enabled = option.enabled,
                modifier = Modifier.testTag("speech.choice.$index")) }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}

internal fun speechRateLabel(rate: Float): String = NumberFormat.getNumberInstance().apply { maximumFractionDigits = 2 }.format(rate) + "×"

@Composable
internal fun ReadAloudSettingsScreen(profile: Profile, onBack: () -> Unit) {
    val controller = LocalReadAloudController.current ?: return
    val preferences = profile.settings.speech
    val discovery = controller.discovery
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var choice by rememberSaveable(profile.id) { mutableStateOf<SpeechSettingChoice?>(null) }
    var custom by rememberSaveable(profile.id) { mutableStateOf(false) }
    var settingsFailure by remember(profile.id) { mutableStateOf(false) }
    DisposableEffect(controller, profile.id, lifecycle) {
        var paused = false
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) paused = true
            if (event == Lifecycle.Event.ON_RESUME && paused) { paused = false; controller.refresh() }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); controller.cancelSelection() }
    }
    val system = stringResource(R.string.speech_rate_system)
    val automatic = stringResource(R.string.speech_voice_automatic)
    val customLabel = stringResource(R.string.speech_custom)
    val engineLabel = discovery.engines.firstOrNull { it.packageName == discovery.activePackage }?.label
    val effectiveVoice = discovery.voices.effective
    val voiceLabel = effectiveVoice?.let { "${it.name} · ${Locale.forLanguageTag(it.localeTag).displayName}" }
    val status = when (discovery.phase) {
        SpeechDiscoveryPhase.Discovering -> R.string.speech_discovering
        SpeechDiscoveryPhase.Empty -> R.string.speech_no_engine
        SpeechDiscoveryPhase.Failed -> R.string.speech_discovery_failed
        SpeechDiscoveryPhase.Ready -> if (discovery.usable) null else R.string.speech_no_offline_voice
    }
    SettingsScaffold(stringResource(R.string.read_aloud), onBack) {
        SettingsList {
            item { SettingsGroup {
                row {
                    SettingsLink(stringResource(R.string.speech_engine), value = engineLabel,
                        enabled = discovery.engines.isNotEmpty(), onClick = { choice = SpeechSettingChoice.Engine })
                }
                row {
                    SettingsLink(stringResource(R.string.speech_voice), value = if (preferences.voice(discovery.activePackage.orEmpty()) == null) automatic else voiceLabel,
                        subtitle = if (discovery.voices.usingFallback) stringResource(R.string.speech_voice_fallback, voiceLabel.orEmpty()) else voiceLabel,
                        enabled = discovery.activePackage != null, onClick = { choice = SpeechSettingChoice.Voice })
                }
                row {
                    SettingsLink(stringResource(R.string.speech_rate), value = preferences.rate?.let(::speechRateLabel) ?: system,
                        onClick = { choice = SpeechSettingChoice.Rate })
                }
            } }
            if (status != null) item { SettingsCallout(stringResource(status), icon = if (discovery.phase == SpeechDiscoveryPhase.Discovering) R.drawable.ic_info else R.drawable.ic_warning) }
            if (controller.discoveryFailed && discovery.phase != SpeechDiscoveryPhase.Failed) item { SettingsCallout(stringResource(R.string.speech_discovery_failed), icon = R.drawable.ic_warning) }
            if (controller.engineChange?.phase == SpeechEngineChangePhase.Initializing) item { SettingsCallout(stringResource(R.string.speech_changing_engine)) }
            if (controller.engineChange?.phase == SpeechEngineChangePhase.Failed) item { SettingsCallout(stringResource(R.string.speech_selection_failed), icon = R.drawable.ic_warning) }
            item { SettingsGroup {
                row {
                    SettingsSwitch(stringResource(R.string.speech_auto_default), preferences.autoReadDefault,
                        onCheckedChange = { enabled -> controller.changePreferences { it.copy(autoReadDefault = enabled) } },
                        subtitle = stringResource(R.string.speech_auto_detail))
                }
                row {
                    SettingsSwitch(stringResource(R.string.speech_mix), preferences.mixWithMedia,
                        onCheckedChange = { enabled -> controller.changePreferences { it.copy(mixWithMedia = enabled) } },
                        subtitle = stringResource(R.string.speech_mix_detail))
                }
                if (preferences.mixWithMedia) {
                    row {
                        SettingsLink(stringResource(R.string.speech_mix_volume), value = speechVolumeLabel(preferences.mixVolume),
                            onClick = { choice = SpeechSettingChoice.Volume })
                    }
                }
            } }
            item { SettingsGroup {
                row {
                    SettingsLink(stringResource(R.string.speech_refresh), onClick = controller::refresh)
                }
                row {
                    SettingsLink(stringResource(R.string.speech_android_settings), onClick = {
                        settingsFailure = runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }.isFailure
                    })
                }
            } }
            if (settingsFailure) item { SettingsCallout(stringResource(R.string.speech_settings_failed), icon = R.drawable.ic_warning) }
        }
    }
    when (choice) {
        SpeechSettingChoice.Engine -> SpeechSettingsChoices(stringResource(R.string.speech_engine), discovery.engines.map { engine ->
            SpeechSettingOption(engine.label, engine.packageName == discovery.activePackage,
                subtitle = stringResource(if (engine.trust == SpeechEngineTrust.OnDevice) R.string.speech_engine_on_device else R.string.speech_engine_external)) {
                choice = null; controller.selectEngine(engine.packageName)
            }
        }, { choice = null })
        SpeechSettingChoice.Voice -> {
            val engine = discovery.activePackage.orEmpty()
            val saved = preferences.voice(engine)
            SpeechSettingsChoices(stringResource(R.string.speech_voice), listOf(SpeechSettingOption(automatic, saved == null) {
                choice = null; controller.selectEngine(engine, null, changingVoice = true)
            }) + discovery.voices.options.map { voice ->
                val reason = stringResource(when (voice.unavailable) {
                    null -> R.string.speech_voice_offline
                    SpeechVoiceUnavailable.NotInstalled -> R.string.speech_voice_not_installed
                    SpeechVoiceUnavailable.RequiresNetwork -> R.string.speech_voice_network
                    SpeechVoiceUnavailable.Ambiguous -> R.string.speech_voice_duplicate
                    SpeechVoiceUnavailable.InvalidIdentity -> R.string.speech_voice_unavailable
                })
                SpeechSettingOption(voice.label, voice.key == saved,
                    "${Locale.forLanguageTag(voice.key.localeTag).displayName} · $reason", voice.selectable) {
                    choice = null; controller.selectEngine(engine, voice.key, changingVoice = true)
                }
            }, { choice = null })
        }
        SpeechSettingChoice.Rate -> SpeechSettingsChoices(stringResource(R.string.speech_rate),
            listOf(SpeechSettingOption(system, preferences.rate == null) { choice = null; controller.changePreferences { it.withRate(null) } }) +
                SpeechRates.presets.map { rate -> SpeechSettingOption(speechRateLabel(rate), preferences.rate == rate) {
                    choice = null; controller.changePreferences { it.withRate(rate) }
                } } + SpeechSettingOption(customLabel, preferences.rate != null && preferences.rate !in SpeechRates.presets) { choice = null; custom = true },
            { choice = null })
        SpeechSettingChoice.Volume -> SpeechSettingsChoices(stringResource(R.string.speech_mix_volume), SpeechMixVolume.entries.map { volume ->
            SpeechSettingOption(speechVolumeLabel(volume), volume == preferences.mixVolume) {
                choice = null; controller.changePreferences { it.copy(mixVolume = volume) }
            }
        }, { choice = null })
        null -> Unit
    }
    if (custom) {
        val input = remember { TextFieldState(preferences.rate?.let { NumberFormat.getNumberInstance().format(it) }.orEmpty()) }
        val parsed = SpeechRates.parse(input.text.toString())
        var attempted by remember { mutableStateOf(false) }
        AlertDialog(onDismissRequest = { custom = false }, title = { Text(stringResource(R.string.speech_custom_rate)) }, text = {
            WhiteNoiseTextField(input, Modifier.fillMaxWidth().testTag("speech.custom_rate"),
                label = { Text(stringResource(R.string.speech_rate)) }, lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = attempted && parsed == null,
                supportingText = { Text(stringResource(R.string.speech_rate_range)) })
        }, confirmButton = { TextButton(onClick = {
            attempted = true
            if (parsed != null) { controller.changePreferences { it.withRate(parsed) }; custom = false }
        }) { Text(stringResource(R.string.speech_apply)) } }, dismissButton = {
            TextButton(onClick = { custom = false }) { Text(stringResource(R.string.cancel)) }
        })
    }
}

@Composable
internal fun speechVolumeLabel(volume: SpeechMixVolume): String = stringResource(when (volume) {
    SpeechMixVolume.Quiet -> R.string.speech_volume_quiet
    SpeechMixVolume.Medium -> R.string.speech_volume_medium
    SpeechMixVolume.Loud -> R.string.speech_volume_loud
})

@Composable
internal fun ChatAutoReadSetting(profile: Profile, chat: Chat) {
    val controller = LocalReadAloudController.current ?: return
    val preferences = profile.settings.speech
    var open by rememberSaveable(profile.id, chat.id) { mutableStateOf(false) }
    val inherited = stringResource(if (preferences.autoReadDefault) R.string.speech_default_on else R.string.speech_default_off)
    val on = stringResource(R.string.speech_on); val off = stringResource(R.string.speech_off)
    val override = preferences.autoReadOverrides[chat.id]
    SettingsLink(stringResource(R.string.speech_auto_chat), value = when (override) {
        null -> inherited; SpeechAutoReadOverride.On -> on; SpeechAutoReadOverride.Off -> off
    }, onClick = { open = true })
    if (open) SpeechSettingsChoices(stringResource(R.string.speech_auto_chat), listOf(null, SpeechAutoReadOverride.On, SpeechAutoReadOverride.Off).map { value ->
        SpeechSettingOption(when (value) { null -> inherited; SpeechAutoReadOverride.On -> on; SpeechAutoReadOverride.Off -> off }, value == override) {
            controller.changePreferences { it.withAutoRead(chat.id, value) }; open = false
        }
    }, { open = false })
}
