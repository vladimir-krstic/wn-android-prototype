package dev.ipf.whitenoise.state

import androidx.compose.runtime.*
import dev.ipf.whitenoise.model.*

@Stable
class TranscriptController(private val activeProfile: () -> Profile?) {
    var work by mutableStateOf<TranscriptWork?>(null); private set
    var scenario by mutableStateOf(TranscriptScenario.Success); private set
    private var scenarioProfile: String? = null
    private var sequence = 0L
    fun choose(value: TranscriptScenario) {
        val profile = activeProfile()?.takeIf { it.developerTools.isEnabled } ?: return
        scenarioProfile = profile.id; scenario = value
    }
    private fun current(source: TranscriptSource): Boolean {
        val profile = activeProfile()?.takeIf { it.id == source.profileId } ?: return false
        val chat = profile.chats.firstOrNull { it.id == source.chatId } ?: return false
        return TranscriptSource.capture(profile, chat) == source
    }
    fun reconcile() {
        if (scenarioProfile != activeProfile()?.id || activeProfile()?.developerTools?.isEnabled != true) {
            scenarioProfile = null; scenario = TranscriptScenario.Success
        }
        val w = work ?: return
        if (w.source.profileId != activeProfile()?.id) { work = null; return }
        if (w.phase !in setOf(TranscriptPhase.Saved, TranscriptPhase.Cancelled, TranscriptPhase.Failed) && !current(w.source))
            work = w.copy(phase = TranscriptPhase.Failed, failure = TranscriptFailure.SourceUnavailable, document = null)
    }
    fun begin(owner: GroupOwner): Boolean {
        reconcile(); if (work?.busy == true) return false
        val p = activeProfile()?.takeIf { it.id == owner.profileId } ?: return false
        val chat = p.chats.firstOrNull { it.id == owner.chatId } ?: return false
        work = TranscriptWork(++sequence, TranscriptSource.capture(p, chat), scenario)
        return true
    }
    fun advance(id: Long) {
        reconcile(); val w = work?.takeIf { it.id == id && it.phase == TranscriptPhase.Reading } ?: return
        if (w.scenario == TranscriptScenario.SourceUnavailable || w.scenario == TranscriptScenario.PreparationFailure) {
            work = w.copy(phase = TranscriptPhase.Failed, failure = if (w.scenario == TranscriptScenario.SourceUnavailable) TranscriptFailure.SourceUnavailable else TranscriptFailure.Preparation)
            return
        }
        val source = ConversationTranscript.ordered(w.source)
        val page = source.drop(w.readCount).take(ConversationTranscript.pageSize)
        val count = w.readCount + page.size
        work = w.copy(readCount = count, entries = w.entries + page,
            phase = if (count >= source.size) TranscriptPhase.Encoding else TranscriptPhase.Reading)
    }
    fun encoded(id: Long, document: String?) {
        reconcile(); val w = work?.takeIf { it.id == id && it.phase == TranscriptPhase.Encoding } ?: return
        work = w.copy(document = document, phase = if (document == null) TranscriptPhase.Failed else TranscriptPhase.Ready,
            failure = if (document == null) TranscriptFailure.Preparation else null)
    }
    fun save(id: Long) {
        reconcile(); val w = work?.takeIf { it.id == id && it.phase == TranscriptPhase.Ready } ?: return
        work = w.copy(phase = TranscriptPhase.ChoosingDestination)
    }
    fun takeForWriting(id: Long): String? {
        reconcile(); val w = work?.takeIf { it.id == id && it.phase == TranscriptPhase.ChoosingDestination } ?: return null
        work = w.copy(phase = TranscriptPhase.Writing)
        return w.document
    }
    fun saved(id: Long, success: Boolean): Boolean {
        reconcile(); val w = work?.takeIf { it.id == id && it.phase == TranscriptPhase.Writing } ?: return false
        work = w.copy(phase = if (success) TranscriptPhase.Saved else TranscriptPhase.Failed,
            failure = if (success) null else TranscriptFailure.Write, document = null)
        return true
    }
    fun destinationFailed(id: Long) {
        val w = work?.takeIf { it.id == id && it.phase == TranscriptPhase.ChoosingDestination } ?: return
        work = w.copy(phase = TranscriptPhase.Failed, failure = TranscriptFailure.Destination, document = null)
    }
    fun cancel(id: Long) {
        val w = work?.takeIf { it.id == id && it.phase != TranscriptPhase.Writing } ?: return
        work = w.copy(phase = TranscriptPhase.Cancelled, document = null)
    }
    fun retry(id: Long): Boolean {
        val w = work?.takeIf { it.id == id && it.phase == TranscriptPhase.Failed } ?: return false
        scenario = TranscriptScenario.Success
        return begin(GroupOwner(w.source.profileId, w.source.chatId))
    }
    fun interruptWriting() {
        val w = work?.takeIf { it.phase == TranscriptPhase.Writing } ?: return
        work = w.copy(phase = TranscriptPhase.Failed, failure = TranscriptFailure.Write, document = null)
    }
    fun dismiss(id: Long) { if (work?.let { it.id == id && !it.busy } == true) work = null }
}
