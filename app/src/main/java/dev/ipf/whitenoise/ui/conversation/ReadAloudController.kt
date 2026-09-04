package dev.ipf.whitenoise.ui.conversation

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.ipf.whitenoise.model.*
import java.util.Locale

internal data class SpeechConsent(val id: Long, val profileId: String, val enginePackage: String, val selectingEngine: Boolean)
internal data class SpeechAutoEntry(val owner: SpeechOwner, val cursor: SpeechArrivalCursor,
    val claimed: Set<String>, val unreadIds: Set<String>, val openedGeneration: Long,
    val started: Boolean = false, val resumeGeneration: Long? = null)

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
    private var platform: SpeechPlatform? = null
    private var context: Context? = null
    private var boundProfile: String? = null
    var discovery by mutableStateOf(SpeechDiscovery())
        private set
    var discoveryFailed by mutableStateOf(false)
        private set
    var engineChange by mutableStateOf<SpeechEngineChange?>(null)
        private set
    var pendingConsent by mutableStateOf<SpeechConsent?>(null)
        private set
    var startRefusal by mutableStateOf<SpeechStartRefusal?>(null)
        private set
    var updatePreferences: (String, (SpeechPreferences) -> SpeechPreferences) -> Unit = { _, _ -> }
    val preferences: SpeechPreferences get() = profile?.invoke()?.settings?.speech ?: SpeechPreferences()
    private var nextChange = 0L
    private var pendingSession: SpeechSession? = null
    private var pendingAutomatic = false
    private var automaticOwner: SpeechOwner? = null
    private var manualGeneration = 0L
    private var autoEntry: SpeechAutoEntry? = null
    var isForeground by mutableStateOf(true)
        private set
    var audioScenario by mutableStateOf<SpeechAudioEnvironment?>(null)
        private set

    private var closed = true
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    var profile: (() -> Profile?)? = null
    var onSource: (SpeechReturnTarget) -> Unit = {}
    var edgeScenario by mutableStateOf(SpeechEdgeScenario.Success)
        private set
    private var scenarioProfile: String? = null
    // UI tests can drive the exact production queue without relying on an installed engine.
    private var testOutput: ((String, SpeechToken) -> Boolean)? = null
    private var testRequiresConsent = false
    var catalogScenario by mutableStateOf(SpeechCatalogScenario.Device)
        private set
    private var catalogProfile: String? = null


    fun attachTestOutput(output: (String, SpeechToken) -> Boolean, catalog: SpeechDiscovery? = null) {
        stop(); testOutput = output; closed = false; ready = catalog?.usable ?: true; initializationComplete = true
        testRequiresConsent = catalog != null
        if (catalog != null) discovery = catalog
    }
    fun chooseCatalogScenario(value: SpeechCatalogScenario) {
        val active = profile?.invoke()?.takeIf { it.developerTools.isEnabled } ?: return
        cancelConsent(); cancelSelection(); stop(); platform?.close(); platform = null
        catalogScenario = value; catalogProfile = active.id
        if (value == SpeechCatalogScenario.Device) { testOutput = null; context?.let { initialize(it) }; return }
        if (value == SpeechCatalogScenario.MissingVoice) updatePreferences(active.id) {
            it.withVoice(SpeechCatalogExamples.primary, SpeechVoiceKey(SpeechCatalogExamples.primary, "Previously selected", Locale.getDefault().toLanguageTag()))
        }
        discovery = SpeechCatalogExamples.discovery(value, Locale.getDefault().toLanguageTag(), preferences)
        testRequiresConsent = true; closed = false
        testOutput = { text, token ->
            mainHandler.postDelayed({ if (!closed) { range(token, text.length); done(token) } }, 1_500)
            true
        }
        ready = discovery.usable; initializationComplete = value != SpeechCatalogScenario.Checking
    }

    fun initialize(context: Context) {
        shutdown(); this.context = context.applicationContext; closed = false
        platform = SpeechPlatform(context) { pause() }.also {
            it.onRange = { id, start, end -> callback(id) { token -> range(token, end, start) } }
            it.onDone = { id -> callback(id, ::done) }; it.onError = { id -> callback(id, ::error) }
        }
        boundProfile = profile?.invoke()?.id
        refresh()
    }
    fun refresh() {
        if (testOutput != null) {
            if (catalogScenario != SpeechCatalogScenario.Device) chooseCatalogScenario(SpeechCatalogScenario.Voices)
            return
        }
        cancelSelection()
        val current = profile?.invoke()
        val expected = current?.id
        val previous = discovery
        initializationComplete = false; discoveryFailed = false
        discovery = previous.copy(phase = SpeechDiscoveryPhase.Discovering)
        platform?.discover(current?.settings?.speech ?: SpeechPreferences(), valid = { !closed && profile?.invoke()?.id == expected }) { result, adopted ->
            if (closed || profile?.invoke()?.id != expected) return@discover
            initializationComplete = true; discoveryFailed = result.phase == SpeechDiscoveryPhase.Failed
            if (adopted) { stop(clearAutoEntry = false); discovery = result; ready = true }
            else { discovery = if (result.usable || !previous.usable) result else previous; ready = discovery.usable }
        }
    }
    fun selectEngine(packageName: String, voice: SpeechVoiceKey? = preferences.voice(packageName), changingVoice: Boolean = false) {
        val active = profile?.invoke() ?: return
        if (discovery.engines.none { it.packageName == packageName }) return
        if (changingVoice && voice != null && discovery.voices.options.singleOrNull { it.key == voice }?.selectable != true) return
        cancelSelection()
        val consent = preferences.needsConsent(packageName, SpeechEngineTrust.Unknown)
        val change = SpeechEngineChange(++nextChange, active.id, packageName, voice,
            if (consent) SpeechEngineChangePhase.Consent else SpeechEngineChangePhase.Initializing)
        engineChange = change
        if (consent) pendingConsent = SpeechConsent(change.id, active.id, packageName, selectingEngine = true)
        else applySelection(change)
    }
    private fun applySelection(change: SpeechEngineChange) {
        val prefs = preferences.withVoice(change.requestedPackage, change.requestedVoice)
        val complete: (SpeechDiscovery, Boolean) -> Unit = complete@ { result, adopted ->
            val current = engineChange ?: return@complete
            if (current.id != change.id || current.revision != change.revision || profile?.invoke()?.id != change.profileId) return@complete
            val settled = current.settle(profile?.invoke()?.id, change.revision, result)
            engineChange = settled
            if (adopted && settled.phase == SpeechEngineChangePhase.Applied) {
                stop(); discovery = result; ready = true; initializationComplete = true
                updatePreferences(change.profileId) { it.copy(enginePackage = change.requestedPackage).withVoice(change.requestedPackage, change.requestedVoice) }
            }
        }
        if (testOutput != null && testRequiresConsent) {
            val candidate = if (catalogScenario == SpeechCatalogScenario.SelectionFailure) discovery.copy(phase = SpeechDiscoveryPhase.Failed)
                else SpeechCatalogExamples.discovery(SpeechCatalogScenario.Voices, Locale.getDefault().toLanguageTag(), prefs, change.requestedPackage)
            complete(candidate, candidate.usable)
        } else platform?.discover(prefs, change.requestedPackage, change.requestedVoice,
            valid = { !closed && profile?.invoke()?.id == change.profileId && engineChange == change }, complete = complete)
    }

    fun cancelSelection() {
        if (engineChange?.phase == SpeechEngineChangePhase.Initializing) platform?.cancelDiscovery()
        engineChange = engineChange?.cancel()
        if (pendingConsent?.selectingEngine == true) pendingConsent = null
    }
    fun cancelConsent() {
        if (pendingConsent?.selectingEngine == true) cancelSelection()
        pendingConsent = null; pendingSession = null
    }
    fun confirmConsent(id: Long) {
        val consent = pendingConsent ?: return
        if (consent.id != id || profile?.invoke()?.id != consent.profileId || !isForeground) { cancelConsent(); return }
        pendingConsent = null
        updatePreferences(consent.profileId) { it.acknowledge(consent.enginePackage) }
        if (consent.selectingEngine) {
            val change = engineChange?.takeIf { it.id == id }?.consent() ?: return
            engineChange = change; applySelection(change)
        } else {
            val pending = pendingSession; pendingSession = null
            if (pending != null && discovery.activePackage == consent.enginePackage) begin(pending, pendingAutomatic, consentGranted = true)
        }
    }
    fun dismissRefusal() { startRefusal = null }
    fun changePreferences(reduce: (SpeechPreferences) -> SpeechPreferences) {
        profile?.invoke()?.id?.let { updatePreferences(it, reduce) }
    }
    fun chooseAudioScenario(value: SpeechAudioEnvironment?) {
        if (profile?.invoke()?.developerTools?.isEnabled == true) {
            audioScenario = value
            if (value?.focusAvailable == false && !preferences.mixWithMedia) pause()
        }
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
        begin(next)
    }
    /** Text files have already been decoded/rendered; never interpret their plain payload as Markdown again. */
    fun toggle(messageId: String, text: String) {
        if (activeMessageId == messageId) { stop(); return }
        if (!ready || text.isBlank()) return
        val owner = SpeechOwner(profile?.invoke()?.id.orEmpty(), null, messageId)
        val next = SpeechSession.create(++nextSession, owner, listOf(SpeechItem(messageId, text, plain = true)), messageId,
            locale = Locale.getDefault(), chunkLimit = limit()) ?: return
        begin(next)
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
        val activeId = profile?.invoke()?.id
        if (autoEntry?.owner?.profileId?.let { it != activeId } == true) autoEntry = null
        if (catalogProfile != null && (activeId != catalogProfile || profile?.invoke()?.developerTools?.isEnabled != true)) {
            catalogProfile = null; catalogScenario = SpeechCatalogScenario.Device; testRequiresConsent = false
            testOutput = null; cancelConsent(); stop(); audioScenario = null
            context?.let { initialize(it) }
        }
        if (testOutput == null && activeId != boundProfile) {
            boundProfile = activeId; cancelConsent(); cancelSelection(); stop(); autoEntry = null
            audioScenario = null; platform?.close(); context?.let { initialize(it) }
        }
        pendingConsent?.takeIf { it.profileId != activeId }?.let { cancelConsent() }
        if (automaticOwner?.let { !preferences.autoRead(it.chatId.orEmpty()) } == true) stop()
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
        submitted = null; platform?.stop(release = token == null)
        if (token == null) return
        submitted = token
        val output = testOutput
        val refusal = audioRefusal()
        val accepted = refusal == null && if (output != null) output(safe.chunk, token) else platform?.speak(safe.chunk, token, preferences) == true
        if (!accepted) { submitted = null; platform?.stop(); failed = true; startRefusal = refusal; session = safe.fail(token) }
    }
    private fun audioRefusal(): SpeechStartRefusal? {
        val environment = audioScenario
        if (environment != null) return SpeechAudioPolicy.refusal(preferences, environment)
        return if (testOutput == null) platform?.prepare(preferences) else null
    }
    private fun begin(next: SpeechSession, automatic: Boolean = false, consentGranted: Boolean = false) {
        if (!isForeground || !ready || (profile != null && profile?.invoke()?.id != next.owner.profileId)) return
        if (automatic && (!preferences.autoRead(next.owner.chatId.orEmpty()) || autoEntry?.owner != next.owner)) return
        if (next.returnTarget?.let { profile != null && !SpeechOwnership.owns(profile?.invoke(), it) } == true) return
        val packageName = discovery.activePackage
        if ((testOutput == null || testRequiresConsent) && !consentGranted && packageName != null && preferences.needsConsent(packageName, discovery.runtimeTrust)) {
            pendingSession = next; pendingAutomatic = automatic
            pendingConsent = SpeechConsent(++nextChange, next.owner.profileId, packageName, selectingEngine = false)
            return
        }
        val refusal = audioRefusal()
        if (refusal != null) { startRefusal = refusal; return }
        val previous = session
        failed = false; startRefusal = null; submit(next)
        if (session?.phase == SpeechPhase.EngineError && previous != null) {
            session = previous.pause(); submitted = null
            return
        }
        if (session?.id == next.id && session?.phase == SpeechPhase.Speaking) {
            automaticOwner = if (automatic) next.owner else null
            if (!automatic) { manualGeneration++; autoEntry = null }
        }
    }

    fun stop() = stop(clearAutoEntry = true)
    private fun stop(clearAutoEntry: Boolean) {
        if (pendingConsent?.selectingEngine == false) cancelConsent()
        submitted = null; session = null; automaticOwner = null
        if (clearAutoEntry) autoEntry = null
        platform?.stop()
    }
    fun openChat(profileId: String, chat: Chat, unreadIds: Set<String>) {
        if (session != null && automaticOwner == null) { autoEntry = null; return }
        val owner = SpeechOwner(profileId, chat.id)
        if (autoEntry?.owner == owner) return
        autoEntry = SpeechAutoEntry(owner, SpeechArrivalCursor.capture(chat), emptySet(), unreadIds, manualGeneration)
    }
    fun observeChat(chat: Chat) {
        reconcile()
        val entry = autoEntry ?: return
        if (pendingConsent != null) return
        if (entry.owner.profileId != profile?.invoke()?.id || entry.owner.chatId != chat.id || !isForeground || !ready || !preferences.autoRead(chat.id)) return
        if (entry.openedGeneration != manualGeneration) { autoEntry = null; return }
        val current = session
        if (current != null && automaticOwner != entry.owner && current.phase !in setOf(SpeechPhase.Completed, SpeechPhase.Unavailable)) return
        val items = if (!entry.started) SpeechAutoRead.backlog(chat, entry.unreadIds, entry.claimed)
            else SpeechAutoRead.arrivals(chat, entry.cursor, entry.claimed)
        if (entry.resumeGeneration != null && !SpeechAutoRead.mayResume(entry.owner, profile?.invoke()?.id, isForeground, false,
                entry.resumeGeneration, manualGeneration, session)) return
        autoEntry = entry.copy(cursor = entry.cursor.advance(chat), claimed = entry.claimed + items.map { it.id }, started = true, resumeGeneration = null)
        if (items.isEmpty()) return
        if (current != null && automaticOwner == entry.owner && current.phase in setOf(SpeechPhase.Speaking, SpeechPhase.Paused, SpeechPhase.Loading, SpeechPhase.EdgeError)) {
            submit(current.append(items))
        } else {
            val next = SpeechSession.create(++nextSession, entry.owner, items, items.first().id, locale = Locale.getDefault(), chunkLimit = limit()) ?: return
            begin(next, automatic = true)
        }
    }
    fun background() {
        isForeground = false; cancelConsent(); cancelSelection()
        val entry = autoEntry?.copy(resumeGeneration = manualGeneration)
        stop(); autoEntry = entry
    }
    fun foreground() { isForeground = true; reconcile() }

    fun stopAttachment(requestId: String) { if (session?.owner?.attachmentRequestId == requestId) stop() }
    fun shutdown() {
        closed = true; cancelConsent(); stop(); platform?.close(); platform = null; testOutput = null
        ready = false; initializationComplete = false; failed = false
        discovery = SpeechDiscovery(); discoveryFailed = false; engineChange = null; testRequiresConsent = false
    }
}

@Composable
internal fun rememberReadAloudController(): ReadAloudController {
    LocalReadAloudController.current?.let { return it }
    val controller = rememberOwnedReadAloudController()
    SpeechConsentDialog(controller)
    return controller
}

@Composable
internal fun rememberOwnedReadAloudController(): ReadAloudController {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val controller = remember { ReadAloudController() }
    DisposableEffect(context, controller, owner) {
        controller.initialize(context)
        val observer = LifecycleEventObserver { _, event -> when (event) {
            Lifecycle.Event.ON_STOP -> controller.background()
            Lifecycle.Event.ON_START -> controller.foreground()
            else -> Unit
        } }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); controller.shutdown() }
    }
    return controller
}
