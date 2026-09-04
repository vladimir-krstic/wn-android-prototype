package dev.ipf.whitenoise.state

import androidx.compose.runtime.*
import dev.ipf.whitenoise.model.*

enum class NotificationActionPhase { Waiting, Pending, Finishing, Complete, Failed }
enum class NotificationActionFailure { Invalid, Locked, TargetUnavailable, Operation, Exhausted, Cleanup }
enum class NotificationActionScenario(val label: String) { Success("Action succeeds"), FailsOnce("Action fails once"), AlwaysFails("Action keeps failing"), CleanupFails("Action accepted; cleanup fails once") }
data class NotificationActionWork(val id: Long, val input: NotificationActionInput, val phase: NotificationActionPhase,
    val scenario: NotificationActionScenario, val attempt: Int = 0, val accepted: Boolean = false,
    val failure: NotificationActionFailure? = null, val lockDeadline: Long? = null, val operationFailures: Int = 0, val presented: Boolean = true) {
    val running get() = phase in setOf(NotificationActionPhase.Waiting,NotificationActionPhase.Pending,NotificationActionPhase.Finishing)
}

/** In-memory worker seam. Accepted mutation proof is independent of UI lifetime and cleanup. */
@Stable
class NotificationActionController(private val profiles: () -> List<Profile>, private val signedIn: (String) -> Boolean,
    private val activeId: () -> String?, private val ready: () -> Boolean, private val locked: () -> Boolean,
    private val now: () -> Long, private val mutate: (Long, NotificationActionInput) -> Boolean,
    private val read: (NotificationTarget) -> Boolean) {
    var work by mutableStateOf<NotificationActionWork?>(null); private set
    var cards by mutableStateOf<List<NotificationCard>>(emptyList()); private set
    var scenario by mutableStateOf(NotificationActionScenario.Success); private set
    private var scenarioOwner: String? = null
    private var sequence = 0L
    private var exampleSequence = 0L
    fun nextExampleId(): Long = ++exampleSequence
    // Keep completed/cancelled identities even after the status dialog is dismissed.
    private val requests = mutableMapOf<String,NotificationActionWork>()
    private fun profile(id: String) = profiles().firstOrNull { it.id == id && signedIn(id) }
    fun choose(value: NotificationActionScenario) { if (profile(activeId().orEmpty())?.developerTools?.isEnabled == true) { scenario = value; scenarioOwner = activeId() } }
    fun recordCard(card: NotificationCard): Boolean {
        if (!card.target.valid || card.key.isBlank() || card.generation < 0 || profile(card.target.profileId) == null) return false
        val previous = cards.firstOrNull { it.key == card.key && it.target.profileId == card.target.profileId }
        if (previous != null && previous.generation >= card.generation) return previous == card
        cards = cards.filterNot { it.key == card.key && it.target.profileId == card.target.profileId } + card
        return true
    }
    fun submit(input: NotificationActionInput): Long? {
        reconcile()
        val p = profile(input.card.target.profileId) ?: return null
        val normalized = NotificationActions.normalize(input,p) ?: return null
        requests[normalized.requestKey]?.let { return it.id.takeIf { _ -> it.input == normalized } }
        if (work?.running == true) return null
        val initial = when { locked() && input.kind != NotificationActionKind.MarkRead -> NotificationActionPhase.Failed
            locked() || !ready() -> NotificationActionPhase.Waiting; else -> NotificationActionPhase.Pending }
        val w = NotificationActionWork(++sequence,normalized,initial,scenario,
            failure = NotificationActionFailure.Locked.takeIf { initial == NotificationActionPhase.Failed },
            lockDeadline = (now() + lockWaitMillis).takeIf { locked() })
        save(w); return w.id
    }
    fun reconcile() {
        if (scenarioOwner != null && (scenarioOwner != activeId() || profile(scenarioOwner!!)?.developerTools?.isEnabled != true)) { scenario = NotificationActionScenario.Success; scenarioOwner = null }
        cards = cards.filter { profile(it.target.profileId) != null }
        val w = work ?: return
        if (profile(w.input.card.target.profileId) == null) { work = null; return }
        if (!w.running) return
        if (locked() || !ready()) {
            val deadline = w.lockDeadline ?: (now() + lockWaitMillis)
            if (now() >= deadline) save(w.copy(phase = NotificationActionPhase.Failed, failure = NotificationActionFailure.Locked))
            else if (w.phase != NotificationActionPhase.Waiting || w.lockDeadline == null) save(w.copy(phase = NotificationActionPhase.Waiting,lockDeadline = deadline))
        } else if (w.phase == NotificationActionPhase.Waiting) save(w.copy(phase = if (w.accepted) NotificationActionPhase.Finishing else NotificationActionPhase.Pending,lockDeadline = null))
    }
    fun advance(id: Long, phase: NotificationActionPhase, attempt: Int) {
        reconcile()
        val w = work?.takeIf { it.id == id && it.phase == phase && it.attempt == attempt && it.running && it.phase != NotificationActionPhase.Waiting } ?: return
        val target = w.input.card.target
        val p = profile(target.profileId) ?: return
        if (locked() || !ready()) return
        if (phase == NotificationActionPhase.Pending) {
            val chat = p.chats.firstOrNull { it.id == target.chatId }
            if (chat == null || NotificationActions.message(chat,target,now()) == null ||
                (w.input.kind != NotificationActionKind.MarkRead && chat.composerAvailability(p) != ComposerAvailability.Available) ||
                NotificationActions.normalize(w.input,p) == null) { save(w.copy(phase = NotificationActionPhase.Failed,failure = NotificationActionFailure.TargetUnavailable)); return }
            if (w.scenario == NotificationActionScenario.AlwaysFails || w.scenario == NotificationActionScenario.FailsOnce && w.operationFailures == 0) {
                save(w.copy(phase = NotificationActionPhase.Failed, failure = if (w.operationFailures >= 2) NotificationActionFailure.Exhausted else NotificationActionFailure.Operation, operationFailures = w.operationFailures + 1)); return
            }
            val success = if (w.input.kind == NotificationActionKind.MarkRead) read(target) else mutate(w.id,w.input)
            if (!success) { save(w.copy(phase = NotificationActionPhase.Failed,failure = NotificationActionFailure.TargetUnavailable)); return }
            save(w.copy(accepted = true,phase = NotificationActionPhase.Finishing)); return
        }
        if (w.scenario == NotificationActionScenario.CleanupFails && w.attempt == 0) { save(w.copy(phase = NotificationActionPhase.Failed,failure = NotificationActionFailure.Cleanup)); return }
        // Best-effort read after accepted send: inability to update a removed row cannot unsend it.
        if (w.input.kind != NotificationActionKind.MarkRead) read(target)
        cards = cards.filterNot { it.target.kind == NotificationTargetKind.Message && it.target.profileId == target.profileId && it.target.chatId == target.chatId &&
            it.generation <= w.input.card.generation && (it.key != w.input.card.key || it.target.messageId == target.messageId) }
        save(w.copy(phase = NotificationActionPhase.Complete,failure = null))
    }
    fun retry(id: Long): Boolean {
        reconcile(); val w = work?.takeIf { it.id == id && it.phase == NotificationActionPhase.Failed && it.failure != NotificationActionFailure.Exhausted } ?: return false
        if (profile(w.input.card.target.profileId) == null || locked() || !ready()) return false
        save(w.copy(phase = if (w.accepted) NotificationActionPhase.Finishing else NotificationActionPhase.Pending, attempt = w.attempt + 1, failure = null,lockDeadline = null)); return true
    }
    fun dismiss(id: Long) {
        val w = work?.takeIf { it.id == id } ?: return
        // Closing accepted status must not cancel the worker's read/dismiss cleanup.
        if (w.accepted && w.phase != NotificationActionPhase.Complete) {
            save(w.copy(presented = false,phase = NotificationActionPhase.Finishing,attempt = w.attempt + 1,failure = null))
            reconcile()
        } else work = null
    }
    fun erase() { work = null; cards = emptyList(); requests.clear(); scenario = NotificationActionScenario.Success; scenarioOwner = null }
    private fun save(w: NotificationActionWork) { work = w; requests[w.input.requestKey] = w }
    companion object { const val lockWaitMillis = 24 * 60 * 60_000L }
}
