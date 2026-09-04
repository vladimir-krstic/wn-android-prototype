package dev.ipf.whitenoise.ui.conversation

import android.content.Intent
import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.VoiceCaptureFailure
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog

@Composable
internal fun VoiceCaptureFailureDialog(failure: VoiceCaptureFailure, onDismiss: () -> Unit, onRetry: () -> Unit) {
    val context = LocalContext.current
    var settingsFailed by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(R.string.record_voice_message)) }, text = {
        Text(stringResource(if (settingsFailed) R.string.speech_settings_failed else when (failure) {
            VoiceCaptureFailure.TooShort -> R.string.voice_capture_short
            VoiceCaptureFailure.MicrophoneBusy -> R.string.dictation_microphone_busy
            VoiceCaptureFailure.PermissionDenied -> R.string.dictation_permission_denied
            VoiceCaptureFailure.PermissionPermanentlyDenied -> R.string.dictation_permission_permanent
            VoiceCaptureFailure.RecordingFailed -> R.string.voice_capture_failed
        }))
    }, confirmButton = {
        if (failure in setOf(VoiceCaptureFailure.PermissionDenied, VoiceCaptureFailure.PermissionPermanentlyDenied)) TextButton(onClick = {
            settingsFailed = runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }.isFailure
        }) { Text(stringResource(R.string.speech_android_settings)) }
        else TextButton(onClick = onRetry) { Text(stringResource(R.string.dictation_retry)) }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } })
}
