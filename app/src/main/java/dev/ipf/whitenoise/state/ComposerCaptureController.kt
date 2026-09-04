package dev.ipf.whitenoise.state

import androidx.compose.runtime.*
import dev.ipf.whitenoise.model.*

data class DictationInsertion(val requestId: Long, val owner: ComposerCaptureOwner, val value: DictationTextResult)

/** App-owned, in-memory capture coordination. No microphone or recognition service is started. */
@Stable
class ComposerCaptureController(
    private val profiles: () -> List<Profile>,
    private val activeProfileId: () -> String?,
    private val signedIn: (String) -> Boolean,
    private val writeDraft: (ComposerCaptureOwner, String) -> Boolean,
    private val sendDraft: (ComposerCaptureOwner, DictationDraft, String) -> Boolean,
    private val updatePreferences: (String, (DictationPreferences) -> DictationPreferences) -> Unit,
) {
    var attempts by mutableStateOf<Map<ComposerCaptureOwner, DictationAttempt>>(emptyMap())
        private set
    var lease by mutableStateOf<ComposerCaptureLease?>(null)
        private set
    var insertion by mutableStateOf<DictationInsertion?>(null)
        private set
    var scenario by mutableStateOf(DictationScenario.Success)
        private set
    var voiceScenario by mutableStateOf(VoiceCaptureScenario.Success)
        private set
    fun chooseVoiceScenario(value: VoiceCaptureScenario) {
        val profile = profiles().firstOrNull { it.id == activeProfileId() && it.developerTools.isEnabled } ?: return
        scenarioProfile = profile.id; voiceScenario = value
    }
    private var scenarioProfile: String? = null
    var presentationRevision by mutableLongStateOf(0)
        private set
    var presentationOwner by mutableStateOf<ComposerCaptureOwner?>(null)
        private set
    fun present(owner: ComposerCaptureOwner) { if (owner.profileId == activeProfileId()) { presentationOwner = owner; presentationRevision++ } }
    private var visible: ComposerCaptureOwner? = null
    private var nextId = 0L
    private val versions = mutableMapOf<ComposerCaptureOwner, Long>()
    private val snapshots = mutableMapOf<ComposerCaptureOwner, DictationDraft>()

    private fun profile(owner: ComposerCaptureOwner): Profile? = profiles().firstOrNull { it.id == owner.profileId && signedIn(it.id) }
    private fun chat(owner: ComposerCaptureOwner): Chat? = profile(owner)?.chats?.firstOrNull { it.id == owner.chatId }
    fun available(owner: ComposerCaptureOwner): Boolean = profile(owner)?.let { profile ->
        profile.id == activeProfileId() && chat(owner)?.composerAvailability(profile) == ComposerAvailability.Available
    } == true
    private fun snapshot(owner: ComposerCaptureOwner, start: Int? = null, end: Int? = start): DictationDraft? = chat(owner)?.let {
        DictationDraft.capture(it, versions[owner] ?: 0, start ?: it.draftText.length, end ?: it.draftText.length)
    }
    private val memberships = mutableMapOf<ComposerCaptureOwner, Pair<ChatMembership, List<GroupMember>>>()
    private val eligibility = mutableMapOf<ComposerCaptureOwner, ComposerAvailability>()
    fun reconcile() {
        val retained = attempts.filterKeys { profile(it) != null && chat(it) != null }
        if (retained != attempts) attempts = retained
        snapshots.keys.toList().forEach { owner ->
            val current = snapshot(owner)
            val profile = profile(owner); val chat = chat(owner)
            if (current == null || profile == null || chat == null) {
                snapshots.remove(owner); versions.remove(owner); memberships.remove(owner); eligibility.remove(owner)
            } else {
                val membership = chat.membership to chat.members
                val status = chat.composerAvailability(profile)
                if (!snapshots.getValue(owner).matches(current) || memberships[owner] != membership || eligibility[owner] != status) {
                    versions[owner] = (versions[owner] ?: 0) + 1
                }
                snapshots[owner] = current.copy(revision = versions[owner] ?: 0)
                memberships[owner] = membership; eligibility[owner] = status
            }
        }
        if (scenarioProfile != null && (scenarioProfile != activeProfileId() || profiles().firstOrNull { it.id == scenarioProfile }?.developerTools?.isEnabled != true)) {
            scenario = DictationScenario.Success; voiceScenario = VoiceCaptureScenario.Success; scenarioProfile = null
        }
        lease?.let { capture ->
            if (!available(capture.owner) || capture.owner != visible) {
                attempts[capture.owner]?.takeIf { it.id == capture.requestId }?.let { publish(it.interrupt()) }
                if (lease == capture) lease = null
            }
        }
        attempts.values.filter { it.owner.profileId != activeProfileId() && !it.terminal }.forEach { publish(it.interrupt()) }
        insertion?.takeIf { profile(it.owner) == null }?.let { insertion = null }
    }
    fun open(owner: ComposerCaptureOwner) {
        if (visible != owner) visible?.let(::close)
        visible = owner; reconcile()
    }
    fun close(owner: ComposerCaptureOwner) {
        if (visible != owner) return
        visible = null
        attempts[owner]?.let { publish(it.interrupt()) }
        if (lease?.owner == owner) lease = null
    }
    fun background() {
        attempts.values.filter { !it.terminal }.forEach { publish(it.interrupt()) }
        lease = null
    }
    fun chooseScenario(value: DictationScenario) {
        val active = profiles().firstOrNull { it.id == activeProfileId() && it.developerTools.isEnabled } ?: return
        scenarioProfile = active.id; scenario = value
    }
    fun changePreferences(profileId: String, reduce: (DictationPreferences) -> DictationPreferences) {
        if (profileId == activeProfileId()) updatePreferences(profileId, reduce)
    }
    fun beginVoice(owner: ComposerCaptureOwner): Long? {
        val id = ++nextId
        return id.takeIf { acquireVoice(owner, id) }
    }
    fun retry(owner: ComposerCaptureOwner, id: Long): Boolean {
        val attempt = owned(owner, id)?.takeIf { it.phase == DictationPhase.Failed } ?: return false
        val text = chat(owner)?.draftText ?: return false
        return begin(owner, text, if (text == attempt.draft.text) attempt.draft.selectionStart else text.length,
            if (text == attempt.draft.text) attempt.draft.selectionEnd else text.length)
    }
    fun acquireVoice(owner: ComposerCaptureOwner, requestId: Long): Boolean {
        reconcile()
        if (visible != owner || !available(owner) || lease != null) return false
        lease = ComposerCaptureLease(owner, requestId, ComposerCaptureMode.Voice); return true
    }
    fun releaseVoice(owner: ComposerCaptureOwner, requestId: Long) {
        if (lease == ComposerCaptureLease(owner, requestId, ComposerCaptureMode.Voice)) lease = null
    }
    fun begin(owner: ComposerCaptureOwner, expectedText: String, start: Int, end: Int): Boolean {
        reconcile()
        if (visible != owner || !available(owner)) return false
        val existing = attempts[owner]
        if (existing != null && !existing.terminal && existing.phase != DictationPhase.Failed) return false
        val chat = chat(owner) ?: return false
        if (chat.draftText != expectedText) return false
        val draft = snapshot(owner, start, end) ?: return false
        snapshots[owner] = draft; versions.putIfAbsent(owner, 0)
        memberships[owner] = chat.membership to chat.members
        eligibility[owner] = chat.composerAvailability(profile(owner)!!)
        val attempt = DictationAttempt(++nextId, owner, draft, profile(owner)!!.settings.dictation, scenario)
        if (lease != null) publish(attempt.copy(phase = DictationPhase.Failed, failure = DictationFailure.MicrophoneBusy))
        else {
            publish(attempt)
            if (attempt.capturing) lease = ComposerCaptureLease(owner, attempt.id, ComposerCaptureMode.Dictation)
        }
        present(owner)
        return true
    }
    fun acceptDisclosure(owner: ComposerCaptureOwner, id: Long) {
        val attempt = owned(owner, id) ?: return
        if (attempt.phase != DictationPhase.Disclosure || visible != owner || !available(owner)) return
        updatePreferences(owner.profileId) { it.copy(disclosureAccepted = true) }
        val ready = attempt.acceptDisclosure()
        if (lease != null) publish(ready.fail(ready.revision, DictationFailure.MicrophoneBusy)) else {
            publish(ready); lease = ComposerCaptureLease(owner, id, ComposerCaptureMode.Dictation)
        }
    }
    private fun owned(owner: ComposerCaptureOwner, id: Long): DictationAttempt? {
        reconcile(); return attempts[owner]?.takeIf { it.id == id && owner.profileId == activeProfileId() }
    }
    private fun publish(attempt: DictationAttempt) {
        attempts = attempts + (attempt.owner to attempt)
        if (!attempt.capturing && lease == ComposerCaptureLease(attempt.owner, attempt.id, ComposerCaptureMode.Dictation)) lease = null
    }
    fun cancel(owner: ComposerCaptureOwner, id: Long) { owned(owner, id)?.let { publish(it.cancel()) } }
    fun finish(owner: ComposerCaptureOwner, id: Long) { owned(owner, id)?.let { publish(it.finish()) } }
    fun advance(owner: ComposerCaptureOwner, id: Long, revision: Long) {
        val attempt = owned(owner, id)?.takeIf { it.revision == revision } ?: return
        when (attempt.phase) {
            DictationPhase.Preparing -> {
                val available = DictationService.available(DictationExamples.selectedService, DictationExamples.services(attempt.scenario))
                val refusal = if (!available) DictationFailure.ServiceMissing else when (attempt.scenario) {
                    DictationScenario.PermissionDenied, DictationScenario.PermissionPermanentlyDenied, DictationScenario.MicrophoneBusy,
                    DictationScenario.ServiceBusy, DictationScenario.ReadinessTimeout -> attempt.scenario.failure
                    else -> null
                }
                publish(if (refusal == null) attempt.ready(revision) else attempt.fail(revision, refusal))
            }
            DictationPhase.Listening -> {
                var next = attempt.tick(revision, 100)
                if (next.phase == DictationPhase.Listening) {
                    if (next.elapsedMillis == 500L && attempt.scenario != DictationScenario.NoSpeech)
                        next = next.segment(next.revision, "The trail is quiet this morning.", final = false)
                    if (next.elapsedMillis == 1_500L && attempt.scenario != DictationScenario.NoSpeech)
                        next = next.segment(next.revision, DictationExamples.transcript, final = true)
                    if ((next.elapsedMillis >= 2_000 && attempt.scenario == DictationScenario.PartialThenFailure) ||
                        (next.elapsedMillis >= 1_000 && attempt.scenario in setOf(DictationScenario.NoSpeech, DictationScenario.Network, DictationScenario.Failure)))
                        next = next.fail(next.revision, attempt.scenario.failure!!)
                }
                publish(next)
            }
            DictationPhase.Processing -> deliver(attempt)
            else -> Unit
        }
    }
    private fun deliver(attempt: DictationAttempt) {
        if (attempt.scenario == DictationScenario.ProcessingTimeout) { publish(attempt.fail(attempt.revision, DictationFailure.TimedOut)); return }
        if (attempt.retainedText.isBlank()) { publish(attempt.fail(attempt.revision, DictationFailure.NoSpeech)); return }
        val current = snapshot(attempt.owner)
        if (current == null || !attempt.deliveryAllowed(current, visible, available(attempt.owner))) {
            publish(attempt.review(DictationReviewReason.OriginChanged)); return
        }
        val text = DictationText.insert(attempt.draft, attempt.retainedText)
        val accepted = attempt.scenario != DictationScenario.CommitFailure && when (attempt.preferences.delivery) {
            DictationDeliveryMode.Paste -> writeDraft(attempt.owner, text.text)
            DictationDeliveryMode.Send -> sendDraft(attempt.owner, current, text.text)
        }
        if (accepted) {
            if (attempt.preferences.delivery == DictationDeliveryMode.Paste) insertion = DictationInsertion(attempt.id, attempt.owner, text)
            publish(attempt.complete())
        } else publish(attempt.review(DictationReviewReason.CommitRejected))
    }
    fun insertAtEnd(owner: ComposerCaptureOwner, id: Long): Boolean {
        val attempt = owned(owner, id)?.takeIf { it.phase == DictationPhase.Review && it.retainedText.isNotBlank() } ?: return false
        if (visible != owner || !available(owner)) return false
        val current = snapshot(owner) ?: return false
        val result = DictationText.append(current.text, attempt.retainedText)
        if (!writeDraft(owner, result.text)) return false
        insertion = DictationInsertion(id, owner, result); publish(attempt.complete()); return true
    }
}
