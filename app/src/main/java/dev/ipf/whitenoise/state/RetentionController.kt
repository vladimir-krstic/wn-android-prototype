package dev.ipf.whitenoise.state

import androidx.compose.runtime.*
import dev.ipf.whitenoise.model.*

enum class RetentionPhase { Confirm, Applying, Refreshing, Complete, Failed, RefreshFailed }
enum class RetentionFailure { Unavailable, SourceChanged, Interrupted }
enum class RetentionScenario(val developerLabel: String) {
    Success("Timer update succeeds"), ApplyFailure("Timer update fails"), RefreshFailure("History refresh fails after timer update")
}
enum class RetentionExample(val developerLabel: String) {
    None("No retention example"), Waiting("Unread received message"), Running("Sent message counting down"), NearExpiry("Message expires in five seconds")
}
data class RetentionWork(val id: Long, val owner: GroupOwner, val before: DisappearingDuration, val after: DisappearingDuration,
    val rosterRevision: Long, val members: List<GroupMember>, val requestedAtMillis: Long, val pruneIds: Set<String>,
    val scenario: RetentionScenario, val phase: RetentionPhase, val failure: RetentionFailure? = null) {
    val running get() = phase == RetentionPhase.Applying || phase == RetentionPhase.Refreshing
}

fun Chat.canManageRetention(profileId: String): Boolean = membership == ChatMembership.Active &&
    groupLifecycle == GroupLifecycle.Active && (!isGroup || hasAuthoritativeGroupAdmin(profileId))

/** One fixed clock and profile-owned timer requests; foreground/UI callbacks never supply authority. */
@Stable
class RetentionController(
    private val profiles: () -> List<Profile>, private val activeId: () -> String?, private val signedIn: (String) -> Boolean,
    private val otherLocked: (GroupOwner) -> Boolean,
    private val commit: (GroupOwner, DisappearingDuration, DisappearingDuration, Set<String>) -> Boolean,
    private val removeExpired: (GroupOwner, Set<String>) -> Unit,
    private val addExample: (GroupOwner, RetentionExample, Long) -> Unit,
    private val onClockAdvanced: (Long) -> Unit = {},
) {
    var nowMillis by mutableLongStateOf(MessageForwarding.nowMillis); private set
    var work by mutableStateOf<Map<GroupOwner, RetentionWork>>(emptyMap()); private set
    var scenario by mutableStateOf(RetentionScenario.Success); private set
    var example by mutableStateOf(RetentionExample.None); private set
    private var exampleGeneration = 0L
    private val presented = mutableMapOf<GroupOwner, Long>()
    private var scenarioProfile: String? = null
    private var sequence = 0L
    private fun profile(id: String) = profiles().firstOrNull { it.id == id && signedIn(id) }
    private fun chat(owner: GroupOwner) = profile(owner.profileId)?.chats?.firstOrNull { it.id == owner.chatId }
    fun locked(owner: GroupOwner) = work[owner]?.running == true
    private fun developer(): Boolean {
        val p = profile(activeId() ?: return false)?.takeIf { it.developerTools.isEnabled } ?: return false
        scenarioProfile = p.id; return true
    }
    fun choose(value: RetentionScenario) { if (developer()) scenario = value }
    fun chooseExample(value: RetentionExample) { if (developer()) { example = value; exampleGeneration++ } }
    fun open(owner: GroupOwner) {
        reconcile()
        if (scenarioProfile != owner.profileId || owner.profileId != activeId() || example == RetentionExample.None || presented[owner] == exampleGeneration) return
        presented[owner] = exampleGeneration; addExample(owner, example, nowMillis)
    }
    fun reconcile() {
        if (scenarioProfile != null && (scenarioProfile != activeId() || profile(scenarioProfile!!)?.developerTools?.isEnabled != true)) {
            scenarioProfile = null; scenario = RetentionScenario.Success; example = RetentionExample.None; presented.clear()
        }
        work = work.filterKeys { chat(it) != null }.mapValues { (owner, w) ->
            if (w.phase in setOf(RetentionPhase.Confirm, RetentionPhase.Applying) && owner.profileId != activeId())
                w.copy(phase = RetentionPhase.Failed, failure = RetentionFailure.Interrupted) else w
        }
        presented.keys.removeAll { chat(it) == null }
    }
    fun begin(owner: GroupOwner, after: DisappearingDuration): Boolean {
        reconcile(); val chat = chat(owner) ?: return false
        if (owner.profileId != activeId() || !chat.canManageRetention(owner.profileId) || locked(owner) || otherLocked(owner) || chat.disappearingDuration == after) return false
        val phase = if (MessageRetentionPolicy.requiresPruneConfirmation(chat.disappearingDuration, after)) RetentionPhase.Confirm else RetentionPhase.Applying
        work = work + (owner to RetentionWork(++sequence, owner, chat.disappearingDuration, after, chat.groupRoster.revision,
            chat.members, nowMillis, MessageRetentionPolicy.pruneIds(chat, after, nowMillis), scenario, phase))
        return true
    }
    private fun valid(w: RetentionWork): Boolean {
        val chat = chat(w.owner) ?: return false
        return w.owner.profileId == activeId() && chat.canManageRetention(w.owner.profileId) && !otherLocked(w.owner) &&
            chat.disappearingDuration == w.before && chat.groupRoster.revision == w.rosterRevision && chat.members == w.members &&
            MessageRetentionPolicy.pruneIds(chat, w.after, w.requestedAtMillis) == w.pruneIds
    }
    fun confirm(owner: GroupOwner, id: Long): Boolean {
        reconcile(); val w = work[owner]?.takeIf { it.id == id && it.phase == RetentionPhase.Confirm } ?: return false
        if (!valid(w)) { work = work + (owner to w.copy(phase = RetentionPhase.Failed, failure = RetentionFailure.SourceChanged)); return false }
        work = work + (owner to w.copy(phase = RetentionPhase.Applying)); return true
    }
    fun advance(owner: GroupOwner, id: Long, phase: RetentionPhase) {
        reconcile(); val w = work[owner]?.takeIf { it.id == id && it.phase == phase && it.running } ?: return
        if (phase == RetentionPhase.Refreshing) {
            work = work + (owner to w.copy(phase = if (w.scenario == RetentionScenario.RefreshFailure) RetentionPhase.RefreshFailed else RetentionPhase.Complete)); return
        }
        val failure = when { !valid(w) -> RetentionFailure.SourceChanged; w.scenario == RetentionScenario.ApplyFailure -> RetentionFailure.Unavailable; else -> null }
        val accepted = failure == null && commit(owner, w.before, w.after, w.pruneIds)
        work = work + (owner to w.copy(phase = if (accepted) RetentionPhase.Refreshing else RetentionPhase.Failed,
            failure = if (accepted) null else failure ?: RetentionFailure.SourceChanged))
    }
    fun retry(owner: GroupOwner, id: Long): Boolean {
        reconcile(); val w = work[owner]?.takeIf { it.id == id && it.owner.profileId == activeId() } ?: return false
        if (w.phase == RetentionPhase.RefreshFailed) {
            work = work + (owner to w.copy(id = ++sequence, phase = RetentionPhase.Refreshing, scenario = RetentionScenario.Success)); return true
        }
        if (w.phase != RetentionPhase.Failed) return false
        val chosen = scenario; scenario = RetentionScenario.Success
        return try { begin(owner, w.after) } finally { scenario = chosen }
    }
    fun dismiss(owner: GroupOwner, id: Long) {
        if (owner.profileId == activeId() && work[owner]?.let { it.id == id && !it.running } == true) work = work - owner
    }
    fun hasDeadlines(): Boolean = profiles().filter { signedIn(it.id) }.any { p -> p.chats.any { c ->
        c.timeline.filterIsInstance<ChatTimelineEntry.Message>().any { MessageRetentionPolicy.deadline(it.message) != null }
    } }
    fun tick(expectedNow: Long) { if (expectedNow == nowMillis) advanceClock(1_000) }
    fun advanceExampleClock(milliseconds: Long) { if (developer() && milliseconds in 1..86_400_000L) advanceClock(milliseconds) }
    private fun advanceClock(milliseconds: Long) {
        nowMillis = if (nowMillis > Long.MAX_VALUE - milliseconds) Long.MAX_VALUE else nowMillis + milliseconds
        profiles().filter { signedIn(it.id) }.forEach { p -> p.chats.forEach { c ->
            val expired = c.timeline.filterIsInstance<ChatTimelineEntry.Message>().filter { MessageRetentionPolicy.expired(it.message, nowMillis) }.mapTo(linkedSetOf()) { it.id }
            if (expired.isNotEmpty()) removeExpired(GroupOwner(p.id, c.id), expired)
        } }
        onClockAdvanced(nowMillis)
        reconcile()
    }
}
