package dev.ipf.whitenoise.state

import androidx.compose.runtime.*
import dev.ipf.whitenoise.model.*

/** App-wide recording state with profile/route-owned consent and export leases. */
@Stable
class AuditLogController(private val profiles: () -> List<Profile>, private val activeId: () -> String?,
    private val signedIn: (String) -> Boolean, private val locked: () -> Boolean) {
    var state by mutableStateOf(AuditLogState()); private set
    var work by mutableStateOf<AuditLogWork?>(null); private set
    var scenario by mutableStateOf(AuditLogScenario.Success); private set
    private var scenarioOwner: String? = null
    private var route: String? = null
    private var sequence = 0L
    private fun owner() = profiles().firstOrNull { it.id == activeId() && signedIn(it.id) && it.developerTools.isEnabled }
    fun choose(value: AuditLogScenario) { if (owner() != null) { scenario = value; scenarioOwner = activeId() } }
    fun observeRoute(value: String?) {
        if (route != null && value != route) work = null
        route = value; reconcile()
    }
    fun reconcile() {
        state = state.copy(files = state.files.filter { file -> profiles().any { it.id == file.profileId } })
        if (scenarioOwner != null && (scenarioOwner != owner()?.id)) { scenario = AuditLogScenario.Success; scenarioOwner = null }
        if (work?.profileId != owner()?.id || locked()) work = null
    }
    fun begin(action: AuditLogAction): Long? {
        reconcile(); val p = owner() ?: return null
        if (locked() || work?.busy == true || action == AuditLogAction.Enable && state.enabled || action == AuditLogAction.Disable && !state.enabled) return null
        val w = AuditLogWork(++sequence,p.id,action,if (action == AuditLogAction.Disable) AuditLogPhase.Applying else AuditLogPhase.Consent,scenario,state.files.toList())
        work = w; return w.id
    }
    fun confirm(id: Long): Boolean {
        reconcile(); val w = work?.takeIf { it.id == id && it.phase == AuditLogPhase.Consent } ?: return false
        work = w.copy(phase = AuditLogPhase.Applying); return true
    }
    fun advance(id: Long, attempt: Int) {
        reconcile(); val w = work?.takeIf { it.id == id && it.attempt == attempt && it.phase == AuditLogPhase.Applying } ?: return
        when (w.action) {
            AuditLogAction.Enable, AuditLogAction.Disable -> {
                if (w.scenario == AuditLogScenario.UpdateFails && w.attempt == 0) { fail(w,AuditLogFailure.Update); return }
                state = state.copy(enabled = w.action == AuditLogAction.Enable)
                if (state.enabled) appendFile()
                work = w.copy(phase = AuditLogPhase.Complete)
            }
            AuditLogAction.Export -> {
                if (w.files.isEmpty()) { fail(w,AuditLogFailure.Empty); return }
                if (!state.files.containsAll(w.files)) { fail(w,AuditLogFailure.SourceChanged); return }
                if (w.scenario == AuditLogScenario.PreparationFails && w.attempt == 0) { fail(w,AuditLogFailure.Preparation); return }
                val archive = runCatching { AuditLogs.archive(w.files) }.getOrNull()
                if (archive == null) fail(w,AuditLogFailure.Preparation) else work = w.copy(phase = AuditLogPhase.ChoosingDestination,archive = archive)
            }
            AuditLogAction.Delete -> {
                val remaining = w.files.filter { it.id !in w.removed && state.files.any { f -> f.id == it.id } }
                if (remaining.isEmpty()) { if (w.removed.isEmpty()) fail(w,AuditLogFailure.Empty) else work = w.copy(phase = AuditLogPhase.Complete); return }
                if (w.scenario == AuditLogScenario.DeleteFails && w.attempt == 0) { fail(w,AuditLogFailure.Delete); return }
                val deleting = if (w.scenario == AuditLogScenario.PartialDelete && w.attempt == 0) remaining.take((remaining.size-1).coerceAtLeast(0)) else remaining
                val ids = deleting.mapTo(hashSetOf()) { it.id }
                state = state.copy(files = state.files.filterNot { it.id in ids })
                if (state.enabled && state.files.isEmpty()) appendFile()
                val removed = w.removed + ids
                if (deleting.size < remaining.size) fail(w.copy(removed = removed),AuditLogFailure.PartialDelete)
                else work = w.copy(phase = AuditLogPhase.Complete,removed = removed,archive = null)
            }
        }
    }
    fun takeForWriting(id: Long, attempt: Int): ByteArray? {
        reconcile(); val w = work?.takeIf { it.id == id && it.attempt == attempt && it.phase == AuditLogPhase.ChoosingDestination } ?: return null
        val bytes = w.archive ?: return null
        work = w.copy(phase = AuditLogPhase.Writing,archive = null)
        return bytes
    }
    fun written(id: Long, attempt: Int, success: Boolean): Boolean {
        reconcile(); val w = work?.takeIf { it.id == id && it.attempt == attempt && it.phase == AuditLogPhase.Writing } ?: return false
        if (success) work = w.copy(phase = AuditLogPhase.Complete) else fail(w,AuditLogFailure.Write)
        return success
    }
    fun interruptWriting() { reconcile(); work?.takeIf { it.phase == AuditLogPhase.Writing }?.let { fail(it,AuditLogFailure.Write) } }
    fun destinationFailed(id: Long, attempt: Int) { work?.takeIf { it.id == id && it.attempt == attempt && it.phase == AuditLogPhase.ChoosingDestination }?.let { fail(it,AuditLogFailure.Destination) } }
    fun retry(id: Long): Boolean {
        reconcile(); val w = work?.takeIf { it.id == id && it.phase == AuditLogPhase.Failed && it.failure !in setOf(AuditLogFailure.Empty,AuditLogFailure.SourceChanged) } ?: return false
        work = w.copy(phase = AuditLogPhase.Applying,attempt = w.attempt+1,failure = null,archive = null); return true
    }
    fun cancel(id: Long) { if (work?.id == id) work = null }
    fun erase() { state = AuditLogState(); work = null; scenario = AuditLogScenario.Success; scenarioOwner = null; route = null }
    private fun appendFile() { state = state.copy(files = state.files + profiles().filter { signedIn(it.id) }.map { AuditLogs.sample(++sequence,listOf(it)) }) }
    private fun fail(w: AuditLogWork, failure: AuditLogFailure) { work = w.copy(phase = AuditLogPhase.Failed,failure = failure,archive = null) }
}
