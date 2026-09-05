package dev.ipf.whitenoise.ui.settings

import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.conversation.LocalComposerCapture

@Composable
internal fun DictationSettingsScreen(profile: Profile, onBack: () -> Unit) {
    val controller = LocalComposerCapture.current ?: return
    val preferences = profile.settings.dictation
    var finishOpen by rememberSaveable(profile.id) { mutableStateOf(false) }
    var resultOpen by rememberSaveable(profile.id) { mutableStateOf(false) }
    val manual = stringResource(R.string.dictation_manual)
    val paste = stringResource(R.string.dictation_paste)
    val send = stringResource(R.string.dictation_send)
    SettingsScaffold(stringResource(R.string.dictation_title), onBack) {
        SettingsList {
            item { SettingsGroup {
                row {
                    SettingsLink(stringResource(R.string.dictation_finish),
                        value = preferences.finishAfterSilenceMillis?.let {
                            pluralStringResource(R.plurals.dictation_silence, (it / 1_000).toInt(), it / 1_000)
                        } ?: manual,
                        onClick = { finishOpen = true })
                }
                row {
                    SettingsLink(stringResource(R.string.dictation_result), value = if (preferences.delivery == DictationDeliveryMode.Paste) paste else send,
                        onClick = { resultOpen = true })
                }
            } }
            item { SettingsExplainer(stringResource(R.string.dictation_settings_detail)) }
            if (preferences.delivery == DictationDeliveryMode.Send) item { SettingsExplainer(stringResource(R.string.dictation_send_safety)) }
        }
    }
    if (finishOpen) SpeechSettingsChoices(stringResource(R.string.dictation_finish), (listOf<Long?>(null) + DictationPreferences.silenceChoices).map { value ->
        SpeechSettingOption(value?.let { pluralStringResource(R.plurals.dictation_silence, (it / 1_000).toInt(), it / 1_000) } ?: manual, value == preferences.finishAfterSilenceMillis) {
            controller.changePreferences(profile.id) { it.withSilence(value) }; finishOpen = false
        }
    }, { finishOpen = false })
    if (resultOpen) SpeechSettingsChoices(stringResource(R.string.dictation_result), DictationDeliveryMode.entries.map { value ->
        SpeechSettingOption(if (value == DictationDeliveryMode.Paste) paste else send, value == preferences.delivery,
            stringResource(if (value == DictationDeliveryMode.Paste) R.string.dictation_paste_detail else R.string.dictation_send_detail)) {
            controller.changePreferences(profile.id) { it.copy(delivery = value) }; resultOpen = false
        }
    }, { resultOpen = false })
}
