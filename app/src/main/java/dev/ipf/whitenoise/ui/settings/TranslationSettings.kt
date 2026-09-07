@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package dev.ipf.whitenoise.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.*
import dev.ipf.whitenoise.ui.components.*
import dev.ipf.whitenoise.ui.conversation.TranslationLanguagePicker
import dev.ipf.whitenoise.ui.conversation.translationLanguageName

@Composable internal fun TranslationSettingsScreen(profile: Profile, onChange: (TranslationPreferences) -> Unit, onBack: () -> Unit) {
    SettingsScaffold(stringResource(R.string.translation_title), onBack) {
        SettingsList {
            item { TranslationSettingsContent(profile.settings.translation, null, onChange) }
        }
    }
}

@Composable internal fun ChatTranslationSetting(profile: Profile, chat: Chat, onChange: (TranslationPreferences) -> Unit) {
    var open by remember(profile.id, chat.id) { mutableStateOf(false) }
    val preferences = profile.settings.translation
    SettingsLink(stringResource(R.string.translation_auto), value = stringResource(translationModeLabel(preferences.chats[chat.id]?.mode ?: TranslationOverride.Inherit)),
        onClick = { open = true }, modifier = Modifier.testTag("translation.chat_settings"))
    if (open) WhiteNoiseModalBottomSheet(onDismissRequest = { open = false }, sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))) {
        Column(Modifier.fillMaxWidth().heightIn(max = LocalWindowInfo.current.containerDpSize.height * .8f).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text(stringResource(R.string.translation_auto), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))
            TranslationSettingsContent(preferences, chat.id, onChange)
        }
    }
}
private fun translationModeLabel(mode: TranslationOverride) = when (mode) {
    TranslationOverride.Inherit -> R.string.translation_inherit
    TranslationOverride.On -> R.string.translation_on
    TranslationOverride.Off -> R.string.translation_off
}
@Composable private fun TranslationSettingsContent(preferences: TranslationPreferences, chatId: String?, onChange: (TranslationPreferences) -> Unit) {
    var languagePicker by remember { mutableStateOf(false) }
    var modePicker by remember { mutableStateOf(false) }
    val chatPreference = preferences.chats[chatId] ?: ChatTranslationPreference()
    fun updateChat(value: ChatTranslationPreference) { if (chatId != null) onChange(preferences.copy(chats = preferences.chats + (chatId to value))) }
    SettingsGroup {
        row {
            if (chatId == null) SettingsSwitch(stringResource(R.string.translation_auto), preferences.automatic,
                onCheckedChange = { onChange(preferences.copy(automatic = it)) }, subtitle = stringResource(R.string.translation_auto_detail))
            else SettingsLink(stringResource(R.string.translation_auto), value = stringResource(translationModeLabel(chatPreference.mode)),
                onClick = { modePicker = true }, modifier = Modifier.testTag("translation.mode"))
        }
        row {
            SettingsLink(stringResource(R.string.translation_target), value = translationLanguageName(if (chatId == null) preferences.target else preferences.target(chatId)),
                onClick = { languagePicker = true }, modifier = Modifier.testTag("translation.target"))
        }
        if (chatId != null && chatPreference.target != null) row {
            SettingsLink(stringResource(R.string.translation_use_default_language), onClick = { updateChat(chatPreference.copy(target = null)) })
        }
    }
    if (chatId != null) {
        Spacer(Modifier.height(16.dp))
        SettingsCallout(stringResource(if (preferences.automatic(chatId)) R.string.translation_effective_on else R.string.translation_effective_off))
    }
    if (languagePicker) TranslationLanguagePicker(onSelect = { value ->
        if (chatId == null) onChange(preferences.copy(target = value)) else updateChat(chatPreference.copy(target = value))
        languagePicker = false
    }, onDismiss = { languagePicker = false })
    if (modePicker) SpeechSettingsChoices(stringResource(R.string.translation_auto), TranslationOverride.entries.map { mode ->
        SpeechSettingOption(stringResource(translationModeLabel(mode)), mode == chatPreference.mode, action = {
            updateChat(chatPreference.copy(mode = mode)); modePicker = false
        })
    }, onDismiss = { modePicker = false })
}
