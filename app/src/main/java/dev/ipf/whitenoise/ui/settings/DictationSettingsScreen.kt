package dev.ipf.whitenoise.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.Profile

@Composable
internal fun DictationSettingsScreen(profile: Profile, onBack: () -> Unit) {
    val context = LocalContext.current
    var settingsFailed by remember(profile.id) { mutableStateOf(false) }
    SettingsScaffold(stringResource(R.string.dictation_title), onBack) {
        SettingsList {
            item { SettingsExplainer(stringResource(R.string.dictation_settings_detail)) }
            item { SettingsGroup {
                row {
                    SettingsAction(stringResource(R.string.speech_android_settings),
                        onClick = {
                            settingsFailed = runCatching { context.startActivity(Intent(Settings.ACTION_VOICE_INPUT_SETTINGS)) }
                                .recoverCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }.isFailure
                        },
                        leading = { Icon(painterResource(R.drawable.ic_mic), contentDescription = null) })
                }
            } }
            if (settingsFailed) item { Text(stringResource(R.string.speech_settings_failed)) }
        }
    }
}
