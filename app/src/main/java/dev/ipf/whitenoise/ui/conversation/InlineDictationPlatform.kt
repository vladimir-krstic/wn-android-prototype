package dev.ipf.whitenoise.ui.conversation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.ipf.whitenoise.model.ComposerCaptureOwner
import dev.ipf.whitenoise.model.DictationFailure
import dev.ipf.whitenoise.state.ComposerCaptureController

/** Test hosts can supply callbacks without opening a microphone or permission surface. */
internal val LocalPlatformDictationEnabled = staticCompositionLocalOf { true }

@Composable
internal fun InlineDictationPlatform(controller: ComposerCaptureController, owner: ComposerCaptureOwner) {
    if (!LocalPlatformDictationEnabled.current) return
    val context = LocalContext.current
    val session = controller.inlineDictation?.takeIf { it.owner == owner }
    var permittedId by remember(owner) { mutableStateOf<Long?>(null) }
    var pendingPermissionId by remember(owner) { mutableStateOf<Long?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val id = pendingPermissionId
        pendingPermissionId = null
        if (id != null && controller.inlineDictation?.let { it.owner == owner && it.id == id && it.capturing } == true) {
            if (granted) permittedId = id else {
                val permanent = context.dictationActivity()?.let {
                    !ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
                } == true
                controller.inlineFailure(owner, id, if (permanent) DictationFailure.PermissionPermanentlyDenied else DictationFailure.PermissionDenied)
            }
        }
    }
    LaunchedEffect(session?.id, session?.capturing) {
        if (session?.capturing == true) {
            kotlinx.coroutines.delay(session.retryDelayMillis)
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                permittedId = session.id
            } else {
                pendingPermissionId = session.id
                permission.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }
    LaunchedEffect(session?.id, session?.phase, permittedId) {
        if (session?.phase == dev.ipf.whitenoise.model.InlineDictationPhase.Preparing && permittedId == session.id) {
            kotlinx.coroutines.delay(15_000)
            controller.inlineFailure(owner, session.id, DictationFailure.TimedOut)
        }
    }
    DisposableEffect(session?.id, session?.capturing, permittedId) {
        if (session?.capturing != true || permittedId != session.id) return@DisposableEffect onDispose { }
        val id = session.id
        var disposed = false
        var terminal = false
        var recognizer: SpeechRecognizer? = null
        fun fail(error: DictationFailure) { if (!disposed) controller.inlineFailure(owner, id, error) }
        fun result(bundle: Bundle?, final: Boolean) {
            if (disposed || terminal) return
            terminal = final
            controller.inlineResult(owner, id,
                bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(), final)
        }
        try {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) fail(DictationFailure.ServiceMissing)
            else {
                val speech = SpeechRecognizer.createSpeechRecognizer(context)
                recognizer = speech
                speech.apply {
                    speech.setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) { if (!disposed && !terminal) controller.inlineReady(owner, id) }
                        override fun onBeginningOfSpeech() = Unit
                        override fun onRmsChanged(rmsdB: Float) = Unit
                        override fun onBufferReceived(buffer: ByteArray?) = Unit
                        override fun onEndOfSpeech() = Unit
                        override fun onError(error: Int) {
                            if (disposed || terminal) return
                            terminal = true
                            controller.inlineRecognitionError(owner, id, when (error) {
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> DictationFailure.PermissionDenied
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> DictationFailure.ServiceBusy
                                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> DictationFailure.NoSpeech
                                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> DictationFailure.Network
                                SpeechRecognizer.ERROR_AUDIO -> DictationFailure.MicrophoneBusy
                                else -> DictationFailure.Unknown
                            })
                        }
                        override fun onResults(results: Bundle?) = result(results, final = true)
                        override fun onPartialResults(partialResults: Bundle?) = result(partialResults, final = false)
                        override fun onEvent(eventType: Int, params: Bundle?) = Unit
                    })
                    speech.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    })
                }
            }
        } catch (_: SecurityException) {
            fail(DictationFailure.PermissionDenied)
        } catch (_: RuntimeException) {
            fail(DictationFailure.ServiceMissing)
        }
        onDispose {
            disposed = true
            recognizer?.let { speech ->
                runCatching { speech.cancel() }
                speech.destroy()
            }
        }
    }
}

private tailrec fun Context.dictationActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.dictationActivity()
    else -> null
}
