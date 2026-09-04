package dev.ipf.whitenoise.ui.conversation

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dev.ipf.whitenoise.model.*
import java.util.Locale

/** No authored text is passed during discovery. A candidate is owned until adopted or released. */
internal class SpeechPlatform(context: Context, private val onFocusLoss: () -> Unit) {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var generation = 0L
    private var candidate: TextToSpeech? = null
    private var active: TextToSpeech? = null
    private var discovery = SpeechDiscovery()
    private var request: AudioFocusRequest? = null
    private var legacyListener: AudioManager.OnAudioFocusChangeListener? = null
    private var focusGeneration = 0L
    private var focused = false
    var onRange: (String?, Int, Int) -> Unit = { _, _, _ -> }
    var onDone: (String?) -> Unit = {}
    var onError: (String?) -> Unit = {}

    val mediaActive: Boolean get() = audio.isMusicActive
    val systemRate: Float get() = SpeechRates.resolved(null,
        runCatching { Settings.Secure.getInt(context.contentResolver, Settings.Secure.TTS_DEFAULT_RATE).toFloat() }.getOrNull())

    fun cancelDiscovery() { generation++; candidate?.shutdown(); candidate = null }

    fun discover(preferences: SpeechPreferences, requested: String? = null, selectedVoice: SpeechVoiceKey? = null,
        valid: () -> Boolean = { true },
        complete: (SpeechDiscovery, Boolean) -> Unit) {
        cancelDiscovery()
        val ticket = generation
        var initializing: TextToSpeech? = null
        var finished = false
        fun finish(status: Int) {
            val engine = initializing ?: return
            if (finished || ticket != generation || candidate !== engine) return
            finished = true
            if (!valid()) { engine.shutdown(); candidate = null; return }
            val result = runCatching {
                if (status != TextToSpeech.SUCCESS) error("Speech initialization failed")
                val engines = engine.engines.orEmpty().map {
                    SpeechEngineOption(it.name, it.label?.toString()?.ifBlank { it.name } ?: it.name, SpeechEngines.declaredTrust(it.name))
                }.distinctBy { it.packageName }.sortedWith(compareBy<SpeechEngineOption> { it.label }.thenBy { it.packageName })
                val preferred = requested ?: SpeechEngines.preferred(engines, engine.defaultEngine, preferences.enginePackage)
                if (requested == null && preferred != null) {
                    engine.shutdown(); candidate = null
                    if (active != null && discovery.usable && preferred == discovery.activePackage &&
                        preferences.voice(preferred) == discovery.voices.requested) {
                        discovery = discovery.copy(revision = ticket, engines = engines)
                        complete(discovery, false)
                    } else discover(preferences, preferred, preferences.voice(preferred), valid, complete)
                    return
                }
                val nativeVoices = engine.voices.orEmpty().toList()
                val voices = nativeVoices.map { SpeechVoice(it.name, it.locale.toLanguageTag(),
                    it.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true, it.isNetworkConnectionRequired, it.quality) }
                val resolution = if (preferred == null) SpeechVoiceResolution() else SpeechVoices.resolve(
                    preferred, Locale.getDefault().toLanguageTag(), voices, selectedVoice)
                val applied = resolution.candidates.firstOrNull { key ->
                    val voice = nativeVoices.singleOrNull { it.name == key.name && it.locale.toLanguageTag() == key.localeTag }
                    voice != null && engine.setVoice(voice) == TextToSpeech.SUCCESS
                }
                SpeechDiscovery(ticket, if (engines.isEmpty()) SpeechDiscoveryPhase.Empty else SpeechDiscoveryPhase.Ready,
                    engines, engine.defaultEngine, preferred, resolution.applied(applied), SpeechEngineTrust.Unknown)
            }.getOrElse { discovery.copy(revision = ticket, phase = SpeechDiscoveryPhase.Failed) }
            if (ticket != generation) { engine.shutdown(); return }
            candidate = null
            if (result.usable) {
                // Android exposes requested metadata, not a verified binding; runtime trust stays Unknown.
                active?.stop(); active?.shutdown(); releaseFocus(); active = engine
                engine.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    private fun deliver(action: () -> Unit) { handler.post { if (active === engine) action() } }
                    override fun onStart(id: String?) = Unit
                    override fun onRangeStart(id: String?, start: Int, end: Int, frame: Int) = deliver { onRange(id, start, end) }
                    override fun onDone(id: String?) = deliver { onDone.invoke(id) }
                    @Deprecated("Platform callback")
                    override fun onError(id: String?) = deliver { onError.invoke(id) }
                })
                discovery = result
                complete(result, true)
            } else {
                engine.shutdown()
                if (active == null) discovery = result
                complete(result, false)
            }
        }
        try {
            initializing = if (requested == null) TextToSpeech(context) { status -> handler.post { finish(status) } }
                else TextToSpeech(context, { status -> handler.post { finish(status) } }, requested)
            candidate = initializing
            handler.postDelayed({ finish(TextToSpeech.ERROR) }, 10_000)
        } catch (_: RuntimeException) {
            finished = true; initializing?.shutdown(); candidate = null
            if (valid()) complete(discovery.copy(revision = ticket, phase = SpeechDiscoveryPhase.Failed), false)
        }
    }

    fun prepare(preferences: SpeechPreferences): SpeechStartRefusal? {
        if (preferences.mixWithMedia) {
            releaseFocus()
            return if (mediaActive) null else SpeechStartRefusal.MediaNotActive
        }
        if (focused) return null
        val ticket = ++focusGeneration
        val listener = AudioManager.OnAudioFocusChangeListener { change -> handler.post {
            if (ticket == focusGeneration && change in setOf(AudioManager.AUDIOFOCUS_LOSS,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)) {
                releaseFocus(); onFocusLoss()
            }
        } }
        val result = if (Build.VERSION.SDK_INT >= 26) {
            val next = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                .setWillPauseWhenDucked(true).setOnAudioFocusChangeListener(listener, handler).build()
            request = next; audio.requestAudioFocus(next)
        } else {
            legacyListener = listener
            @Suppress("DEPRECATION")
            audio.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        focused = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (!focused) releaseFocus()
        return if (focused) null else SpeechStartRefusal.FocusUnavailable
    }

    fun speak(text: String, token: SpeechToken, preferences: SpeechPreferences): Boolean = runCatching {
        val engine = active ?: return@runCatching false
        val voice = engine.voice ?: return@runCatching false
        if (voice.isNetworkConnectionRequired || voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true) return@runCatching false
        if (engine.setSpeechRate(preferences.rate ?: systemRate) != TextToSpeech.SUCCESS) return@runCatching false
        val parameters = Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, SpeechAudioPolicy.volume(preferences)) }
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, parameters, token.value) == TextToSpeech.SUCCESS
    }.getOrDefault(false)

    fun stop(release: Boolean = true) { active?.stop(); if (release) releaseFocus() }
    private fun releaseFocus() {
        focusGeneration++; focused = false
        if (Build.VERSION.SDK_INT >= 26) request?.let { audio.abandonAudioFocusRequest(it) }
        @Suppress("DEPRECATION")
        legacyListener?.let { audio.abandonAudioFocus(it) }
        request = null; legacyListener = null
    }
    fun close() { cancelDiscovery(); stop(); active?.shutdown(); active = null }
}
