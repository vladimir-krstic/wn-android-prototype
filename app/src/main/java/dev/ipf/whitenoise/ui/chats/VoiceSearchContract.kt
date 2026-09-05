package dev.ipf.whitenoise.ui.chats

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.result.contract.ActivityResultContract

internal sealed interface VoiceSearchResult {
    data class Recognized(val text: String) : VoiceSearchResult
    data object Cancelled : VoiceSearchResult
    data object Unavailable : VoiceSearchResult
}

/** The installed recognizer owns listening, permissions, language, and its entire UI. */
internal class VoiceSearchContract : ActivityResultContract<Unit, VoiceSearchResult>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

    override fun parseResult(resultCode: Int, intent: Intent?): VoiceSearchResult = when (resultCode) {
        Activity.RESULT_CANCELED -> VoiceSearchResult.Cancelled
        Activity.RESULT_OK -> intent?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull { it.isNotBlank() }?.trim()?.let(VoiceSearchResult::Recognized)
            ?: VoiceSearchResult.Unavailable
        else -> VoiceSearchResult.Unavailable
    }
}
