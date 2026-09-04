package dev.ipf.whitenoise.ui.conversation

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.ipf.whitenoise.model.*
import java.util.Locale

internal val LocalReadAloudController = staticCompositionLocalOf<ReadAloudController?> { null }

/** The app shell owns one foreground engine. Immutable sessions own every callback and return. */
@Stable
internal class ReadAloudController {
    var ready by mutableStateOf(false)
        private set
    var initializationComplete by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    var session by mutableStateOf<SpeechSession?>(null)
        private set
    var modalCount by mutableIntStateOf(0)
    val activeMessageId: String? get() = session?.takeUnless { it.phase in setOf(SpeechPhase.Unavailable, SpeechPhase.Completed) }?.current?.item?.id
    val progress: Float get() = session?.progress ?: 0f
    val activePassage: MessagePassage? get() = session?.passage
    private var nextSession = 0L
    private var submitted: SpeechToken? = null
    private var engine: TextToSpeech? = null
    private var closed = true
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    var profile: (() -> Profile?)? = null
    var onSource: (SpeechReturnTarget) -> Unit = {}
    var edgeScenario by mutableStateOf(SpeechEdgeScenario.Success)
        private set
    private var scenarioProfile: String? = null
    // UI tests can drive the exact production queue without relying on an installed engine.
    private var testOutput: ((String, SpeechToken) -> Boolean)? = null

    fun attachTestOutput(output: (String, SpeechToken) -> Boolean) {
        stop(); testOutput = output; closed = false; ready = true; initializationComplete = true
    }
    fun initialize(context: Context) {
        shutdown(); closed = false; initializationComplete = false
        var created: TextToSpeech? = null
        created = TextToSpeech(context.applicationContext) { status ->
            val initialized = created
            if (closed || initialized == null || engine !== initialized) return@TextToSpeech
            val localVoiceReady = status == TextToSpeech.SUCCESS && runCatching {
                val locale = Locale.getDefault()
                if (initialized.setLanguage(locale) < TextToSpeech.LANG_AVAILABLE) return@runCatching false
                fun installedLocal(voice: android.speech.tts.Voice) = !voice.isNetworkConnectionRequired &&
                    voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) != true
                val voice = initialized.voice?.takeIf(::installedLocal) ?: initialized.voices.orEmpty()
                    .filter { installedLocal(it) && it.locale.language == locale.language }
                    .sortedWith(compareByDescending<android.speech.tts.Voice> { it.locale == locale }.thenBy { it.name })
                    .firstOrNull()
                voice != null && initialized.setVoice(voice) == TextToSpeech.SUCCESS
            }.getOrDefault(false)
            postUpdate { if (engine === initialized) { ready = localVoiceReady; initializationComplete = true } }
        }
        engine = created
        created.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = callback(utteranceId) { token -> range(token, 0) }
            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) = callback(utteranceId) { token -> range(token, end, start) }
            override fun onDone(utteranceId: String?) = callback(utteranceId, ::done)
            @Deprecated("Platform callback")
            override fun onError(utteranceId: String?) = callback(utteranceId, ::error)
        })
    }
    private fun callback(id: String?, action: (SpeechToken) -> Unit) = postUpdate {
        submitted?.takeIf { it.value == id }?.let(action)
    }
    private fun postUpdate(update: () -> Unit) { mainHandler.post { if (!closed) update() } }

    fun startConversation(profile: Profile, chat: Chat, messageId: String, sourceOffset: Int = 0, selection: MessagePassage? = null) {
        if (!ready || this.profile?.invoke()?.let { it.id != profile.id } == true || (this.profile != null && this.profile?.invoke() == null)) return
        val latestChat = this.profile?.invoke()?.chats?.firstOrNull { it.id == chat.id } ?: chat.takeIf { this.profile == null } ?: return
        val next = SpeechSession.create(++nextSession, SpeechOwner(profile.id, chat.id), SpeechOwnership.items(latestChat), messageId,
            sourceOffset, Locale.getDefault(), limit(), selection = selection) ?: return
        failed = false; submit(next)
    }
    /** Text files have already been decoded/rendered; never interpret their plain payload as Markdown again. */
    fun toggle(messageId: String, text: String) {
        if (activeMessageId == messageId) { stop(); return }
        if (!ready || text.isBlank()) return
        val owner = SpeechOwner(profile?.invoke()?.id.orEmpty(), null, messageId)
        val next = SpeechSession.create(++nextSession, owner, listOf(SpeechItem(messageId, text, plain = true)), messageId,
            locale = Locale.getDefault(), chunkLimit = limit()) ?: return
        failed = false; submit(next)
    }
    fun speakPassage(profile: Profile, chat: Chat, messageId: String, passage: MessagePassage) =
        startConversation(profile, chat, messageId, selection = passage)
    private fun limit() = if (testOutput != null) 3_900 else TextToSpeech.getMaxSpeechInputLength().coerceAtLeast(3) - 1
    fun pause() = transition { it.pause() }
    fun resume() = transition { it.resume() }
    fun move(action: SpeechMove) = transition { it.move(action) }
    fun seek(index: Int) = transition { it.seek(index, startPlaying = true).copy(following = true) }
    fun follow(value: Boolean) = transition { it.copy(following = value) }
    fun range(token: SpeechToken, end: Int, start: Int = 0) = transition { it.range(token, end, start) }
    fun done(token: SpeechToken) = transition { it.done(token) }
    fun error(token: SpeechToken) = transition { it.fail(token) }
    fun retry() = transition { if (it.phase == SpeechPhase.EdgeError) it.retryEdge() else it.resume() }
    fun setEdgeScenario(profileId: String, scenario: SpeechEdgeScenario) {
        if (profile?.invoke()?.let { it.id == profileId && it.developerTools.isEnabled } != true) return
        scenarioProfile = profileId; edgeScenario = scenario
    }
    fun settleEdge(id: Long, revision: Long) {
        reconcile()
        val current = session ?: return
        if (current.id != id || current.revision != revision || current.phase != SpeechPhase.Loading) return
        val backwards = current.edge?.targetIndex?.let { it < current.windowStart } == true
        val fail = current.owner.profileId == scenarioProfile &&
            edgeScenario == if (backwards) SpeechEdgeScenario.EarlierFailure else SpeechEdgeScenario.LaterFailure
        if (fail) edgeScenario = SpeechEdgeScenario.Success
        submit(current.settleEdge(revision, fail))
    }
    fun returnToSource(target: SpeechReturnTarget? = session?.returnTarget) {
        reconcile()
        val current = session ?: return
        if (target == null || current.returnTarget != target || !SpeechOwnership.owns(profile?.invoke(), target)) return
        submit(current.copy(following = true)); onSource(target)
    }
    fun reconcile() {
        val readProfile = profile ?: return
        val active = readProfile()
        if (scenarioProfile != null && active?.id != scenarioProfile) { scenarioProfile = null; edgeScenario = SpeechEdgeScenario.Success }
        val current = session ?: return
        if (active?.id != current.owner.profileId) { stop(); return }
        val target = current.returnTarget
        if (target != null && !SpeechOwnership.owns(active, target)) submit(current.unavailable())
    }
    private fun transition(reduce: (SpeechSession) -> SpeechSession) {
        reconcile(); session?.let { submit(reduce(it)) }
    }
    private fun submit(next: SpeechSession) {
        var safe = next
        val target = next.returnTarget
        if (profile != null && profile?.invoke()?.id != next.owner.profileId) { stop(); return }
        if (target != null && profile != null && !SpeechOwnership.owns(profile?.invoke(), target)) safe = next.unavailable()
        session = safe
        val token = safe.token
        if (token == submitted) return
        submitted = null; engine?.stop()
        if (token == null) return
        submitted = token
        val output = testOutput
        val accepted = if (output != null) output(safe.chunk, token) else runCatching {
            val currentEngine = engine ?: return@runCatching false
            if (currentEngine.voice?.isNetworkConnectionRequired != false) { ready = false; return@runCatching false }
            currentEngine.speak(safe.chunk, TextToSpeech.QUEUE_FLUSH, null, token.value) == TextToSpeech.SUCCESS
        }.getOrDefault(false)
        if (!accepted) { submitted = null; engine?.stop(); failed = true; session = safe.fail(token) }
    }
    fun stop() { submitted = null; session = null; engine?.stop() }
    fun stopAttachment(requestId: String) { if (session?.owner?.attachmentRequestId == requestId) stop() }
    fun shutdown() {
        closed = true; stop(); engine?.shutdown(); engine = null; testOutput = null
        ready = false; initializationComplete = false; failed = false
    }
}

@Composable
internal fun rememberReadAloudController(): ReadAloudController {
    LocalReadAloudController.current?.let { return it }
    return rememberOwnedReadAloudController()
}

@Composable
internal fun rememberOwnedReadAloudController(): ReadAloudController {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember { ReadAloudController() }
    DisposableEffect(context, controller, owner) {
        controller.initialize(context)
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) controller.stop() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); controller.shutdown() }
    }
    return controller
}
