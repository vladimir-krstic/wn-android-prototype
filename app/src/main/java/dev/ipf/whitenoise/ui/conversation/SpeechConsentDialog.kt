package dev.ipf.whitenoise.ui.conversation

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.ipf.whitenoise.R
import dev.ipf.whitenoise.model.SpeechStartRefusal
import dev.ipf.whitenoise.ui.components.WhiteNoiseAlertDialog as AlertDialog

@Composable
internal fun SpeechConsentDialog(controller: ReadAloudController) {
    controller.pendingConsent?.let { consent ->
        AlertDialog(onDismissRequest = controller::cancelConsent,
            title = { Text(stringResource(R.string.speech_engine_external)) },
            text = { Text(stringResource(R.string.speech_trust_detail)) },
            confirmButton = { TextButton(onClick = { controller.confirmConsent(consent.id) }) {
                Text(stringResource(if (consent.selectingEngine) R.string.speech_use_engine else R.string.read_aloud))
            } }, dismissButton = { TextButton(onClick = controller::cancelConsent) { Text(stringResource(R.string.cancel)) } })
    }
    controller.startRefusal?.let { refusal ->
        AlertDialog(onDismissRequest = controller::dismissRefusal, title = { Text(stringResource(R.string.read_aloud)) },
            text = { Text(stringResource(if (refusal == SpeechStartRefusal.MediaNotActive) R.string.speech_media_required else R.string.speech_focus_unavailable)) },
            confirmButton = { TextButton(onClick = controller::dismissRefusal) { Text(stringResource(R.string.speech_dismiss)) } })
    }
}
