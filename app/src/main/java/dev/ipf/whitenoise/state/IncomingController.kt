package dev.ipf.whitenoise.state

import androidx.compose.runtime.*
import dev.ipf.whitenoise.model.*

enum class IncomingPhase { Queued, Preparing, Choosing, Applying, Opening, Complete, Failed, Cancelled }
enum class IncomingFailure { ContentEmpty, ContentInvalid, ContentUnavailable, ContentTooLarge, ProfileUnavailable, TargetUnavailable, Preparation, Apply, Open, SourceChanged, InviteUnconfirmed }
enum class IncomingScenario(val developerLabel: String) { Success("Share succeeds"), PreparationFailure("Content preparation fails"), ApplyFailure("Draft staging fails"), OpenFailure("Draft staged but chat cannot open"), NotificationLoadFailure("Notification history fails once"), InviteRowDelayed("Invitation row appears on third probe"), InviteUnavailable("Invitation no longer available"), InviteInconclusive("Invitation cannot be confirmed") }
data class IncomingCommit(val chatIds: List<String>, val dropped: Int)
data class IncomingOpen(val requestId: Long, val target: IncomingTarget? = null, val person: Person? = null, val profileId: String, val otherChats: Int = 0, val dropped: Int = 0, val chatList: Boolean = false, val notification: NotificationTarget? = null)
data class IncomingWork(val id: Long, val entry: IncomingEntry, val receivingProfileId: String?, val phase: IncomingPhase,
    val originRoute: String?, val selectedProfileId: String?, val awaitingActivation: Boolean = false, val selectedChatIds: List<String> = emptyList(),
    val prepared: PreparedIncoming? = null, val committed: IncomingCommit? = null, val failure: IncomingFailure? = null,
    val scenario: IncomingScenario = IncomingScenario.Success, val fallback: Boolean = false, val attempt: Int = 0, val probe: Int = 0) {
    val running get() = phase in setOf(IncomingPhase.Preparing, IncomingPhase.Applying, IncomingPhase.Opening)
}

/** Local counterpart of incoming staging/navigation. Accepted drafts and pending navigation are separate. */
@Stable
class IncomingController(
    private val profiles: () -> List<Profile>, private val activeId: () -> String?, private val signedIn: (String) -> Boolean,
    private val ready: () -> Boolean,
    private val stage: (Long, String, List<String>, PreparedIncoming) -> IncomingCommit?,
) {
    var work by mutableStateOf<IncomingWork?>(null); private set
    private var developerLocked by mutableStateOf(false)
    private var sessionLocked by mutableStateOf(false)
    val locked get() = developerLocked || sessionLocked
    fun applySessionLock(value: Boolean) { sessionLocked = value; reconcile() }
    var scenario by mutableStateOf(IncomingScenario.Success); private set
    private var scenarioProfile: String? = null
    private var lockProfile: String? = null
    private var sequence = 0L
    private var route: String? = null
    private var routeWasOnboarding = false
    private var navigating: Long? = null
    fun signedProfiles() = profiles().filter { signedIn(it.id) }
    private fun profile(id: String?) = profiles().firstOrNull { it.id == id && signedIn(it.id) }
    private fun developer() = profile(activeId())?.developerTools?.isEnabled == true
    fun choose(value: IncomingScenario) { if (developer()) { scenarioProfile = activeId(); scenario = value } }
    fun chooseLock(value: Boolean) {
        if (developer()) { lockProfile = activeId().takeIf { value }; developerLocked = value }
    }
    fun targets(profileId: String) = profile(profileId)?.let { p -> p.chats.filter { IncomingSharing.canStage(p,it) }.sortedWith(compareBy<Chat> { it.isArchived }.thenBy { it.originalOrder }) }.orEmpty()
    fun observeRoute(token: String?, onboarding: Boolean) {
        if (route != null && token != route) {
            val w = work
            val activation = w?.awaitingActivation == true && (onboarding || routeWasOnboarding)
            if (w != null && w.phase !in setOf(IncomingPhase.Complete, IncomingPhase.Cancelled) && !activation) cancel(w.id)
            else if (activation && !onboarding) work = w.copy(awaitingActivation = false, originRoute = token)
        }
        route = token; routeWasOnboarding = onboarding
    }
    /** A null entry is a launcher re-entry and must not erase an unconsumed request. */
    fun receive(incomingEntry: IncomingEntry?): Long? {
        val entry = if (incomingEntry is IncomingEntry.Notification) incomingEntry.copy(target = incomingEntry.target.normalized()) else incomingEntry
        if (entry == null) return work?.id
        navigating = null
        val id = ++sequence
        val selected = when (entry) { is IncomingEntry.Conversation -> entry.target.profileId; is IncomingEntry.Notification -> entry.target.profileId; else -> activeId() }
        work = IncomingWork(id, entry, activeId(), IncomingPhase.Queued, route, selected, awaitingActivation = !ready() || signedProfiles().isEmpty(), scenario = scenario)
        reconcile(); return id
    }
    fun reconcile() {
        if (scenarioProfile != null && (scenarioProfile != activeId() || !developer())) { scenarioProfile = null; scenario = IncomingScenario.Success }
        if (lockProfile != null && (lockProfile != activeId() || !developer())) { lockProfile = null; developerLocked = false }
        val w = work ?: return
        // Only the controller's issued navigation may activate the chosen destination profile.
        val openingDestination = w.phase == IncomingPhase.Opening && navigating == w.id && activeId() == w.selectedProfileId
        if (w.receivingProfileId != null && (profile(w.receivingProfileId) == null ||
            (activeId() != w.receivingProfileId && !openingDestination))) { cancel(w.id); return }
        if (w.phase in setOf(IncomingPhase.Complete, IncomingPhase.Cancelled)) return
        if (w.selectedProfileId != null && profile(w.selectedProfileId) == null && w.phase != IncomingPhase.Queued) {
            work = w.copy(phase = IncomingPhase.Failed, failure = IncomingFailure.ProfileUnavailable, prepared = null); return
        }
        if (w.phase == IncomingPhase.Queued && ready() && !locked && signedProfiles().isNotEmpty()) {
            val selected = w.selectedProfileId ?: activeId() ?: signedProfiles().first().id
            work = w.copy(selectedProfileId = selected, receivingProfileId = w.receivingProfileId ?: activeId(), phase = IncomingPhase.Preparing)
        }
    }
    fun chooseProfile(id: Long, profileId: String): Boolean {
        val w = work?.takeIf { it.id == id && it.phase in setOf(IncomingPhase.Choosing, IncomingPhase.Failed) && it.committed == null && it.entry is IncomingEntry.Share } ?: return false
        if (profile(profileId) == null) return false
        work = w.copy(selectedProfileId = profileId, selectedChatIds = emptyList(), phase = if (w.prepared == null) IncomingPhase.Preparing else IncomingPhase.Choosing, failure = null)
        return true
    }
    fun toggle(id: Long, chatId: String) {
        val w = work?.takeIf { it.id == id && it.phase == IncomingPhase.Choosing } ?: return
        if (targets(w.selectedProfileId.orEmpty()).none { it.id == chatId }) return
        work = w.copy(selectedChatIds = if (chatId in w.selectedChatIds) w.selectedChatIds - chatId else w.selectedChatIds + chatId, failure = null)
    }
    fun submit(id: Long): Boolean {
        reconcile(); val w = work?.takeIf { it.id == id && it.phase == IncomingPhase.Choosing && it.prepared != null } ?: return false
        val allowed = targets(w.selectedProfileId.orEmpty()).map { it.id }
        if (locked || !ready() || w.selectedChatIds.isEmpty() || !allowed.containsAll(w.selectedChatIds)) {
            work = w.copy(failure = IncomingFailure.TargetUnavailable); return false
        }
        work = w.copy(phase = IncomingPhase.Applying, failure = null); return true
    }
    fun advance(id: Long, phase: IncomingPhase, attempt: Int) {
        reconcile(); val w = work?.takeIf { it.id == id && it.phase == phase && it.attempt == attempt && it.running } ?: return
        if (locked || !ready()) return
        if (phase == IncomingPhase.Preparing) {
            if (w.scenario == IncomingScenario.PreparationFailure && w.attempt == 0) { fail(w,IncomingFailure.Preparation); return }
            when (val entry = w.entry) {
                is IncomingEntry.Share -> {
                    val result = IncomingSharing.prepare(entry.payload)
                    if (result.content == null) { fail(w,when(result.failure) {
                        IncomingContentFailure.Empty -> IncomingFailure.ContentEmpty; IncomingContentFailure.Invalid -> IncomingFailure.ContentInvalid
                        IncomingContentFailure.TooLarge -> IncomingFailure.ContentTooLarge; else -> IncomingFailure.ContentUnavailable
                    }); return }
                    val shortcut = entry.shortcut
                    val direct = shortcut?.takeIf { it.profileId == w.receivingProfileId && it.profileId == w.selectedProfileId && targets(it.profileId).any { c -> c.id == it.chatId } }
                    work = w.copy(prepared = result.content, phase = if (direct == null) IncomingPhase.Choosing else IncomingPhase.Applying,
                        selectedChatIds = direct?.let { listOf(it.chatId) }.orEmpty(), fallback = shortcut != null && direct == null)
                }
                is IncomingEntry.Conversation -> {
                    if (profile(entry.target.profileId) == null) { fail(w,IncomingFailure.ProfileUnavailable); return }
                    if (profile(entry.target.profileId)?.chats?.none { it.id == entry.target.chatId } != false) { fail(w,IncomingFailure.TargetUnavailable); return }
                    work = w.copy(phase = IncomingPhase.Opening)
                }
                is IncomingEntry.Notification -> {
                    val target = entry.target
                    if (!target.valid) { fail(w,IncomingFailure.ContentInvalid); return }
                    val p = profile(target.profileId) ?: run { fail(w,IncomingFailure.ProfileUnavailable); return }
                    if (target.kind == NotificationTargetKind.ChatList) { work = w.copy(phase = IncomingPhase.Opening); return }
                    val chat = p.chats.firstOrNull { it.id == target.chatId }
                    if (target.kind == NotificationTargetKind.Invite) {
                        if (chat?.hasEndedMembership == true || w.scenario == IncomingScenario.InviteUnavailable) { fail(w,IncomingFailure.TargetUnavailable); return }
                        val needsProbe = chat == null || w.scenario == IncomingScenario.InviteInconclusive ||
                            w.scenario == IncomingScenario.InviteRowDelayed && w.probe < 2
                        if (needsProbe) {
                            if (w.probe >= 2) fail(w,IncomingFailure.InviteUnconfirmed)
                            else work = w.copy(probe = w.probe + 1)
                            return
                        }
                    }
                    if (chat == null) { fail(w,IncomingFailure.TargetUnavailable); return }
                    if (w.scenario == IncomingScenario.NotificationLoadFailure && w.attempt == 0) { fail(w,IncomingFailure.Open); return }
                    work = w.copy(phase = IncomingPhase.Opening)
                }
                is IncomingEntry.ProfileLink -> {
                    if (ProfileLinks.parse(entry.value, recipient = true) == null) { fail(w,IncomingFailure.ContentInvalid); return }
                    work = w.copy(phase = IncomingPhase.Opening)
                }
            }; return
        }
        if (phase == IncomingPhase.Applying) {
            if (w.scenario == IncomingScenario.ApplyFailure && w.attempt == 0) { fail(w,IncomingFailure.Apply); return }
            val p = profile(w.selectedProfileId) ?: run { fail(w,IncomingFailure.ProfileUnavailable); return }
            if (w.selectedChatIds.isEmpty() || !targets(p.id).map { it.id }.containsAll(w.selectedChatIds)) { fail(w,IncomingFailure.TargetUnavailable); return }
            val result = w.prepared?.let { stage(w.id,p.id,w.selectedChatIds,it) }
            if (result == null) fail(w,IncomingFailure.Apply) else work = w.copy(phase = IncomingPhase.Opening, committed = result, prepared = null)
        }
    }
    fun opening(id: Long): IncomingOpen? {
        reconcile(); val w = work?.takeIf { it.id == id && it.phase == IncomingPhase.Opening && navigating != id } ?: return null
        if (locked || !ready()) return null
        if (w.scenario == IncomingScenario.OpenFailure && w.attempt == 0) { fail(w,IncomingFailure.Open); return null }
        val p = profile(w.selectedProfileId) ?: run { fail(w,IncomingFailure.ProfileUnavailable); return null }
        if (w.fallback && w.entry !is IncomingEntry.Share || (w.entry as? IncomingEntry.Notification)?.target?.kind == NotificationTargetKind.ChatList) {
            navigating = id; return IncomingOpen(id,profileId = p.id,chatList = true)
        }
        val target = when (val entry = w.entry) {
            is IncomingEntry.Share -> w.committed?.chatIds?.firstOrNull()?.let { IncomingTarget(p.id,it) }
            is IncomingEntry.Conversation -> entry.target
            is IncomingEntry.ProfileLink -> null
            is IncomingEntry.Notification -> IncomingTarget(entry.target.profileId,entry.target.chatId)
        }
        if (target != null && p.chats.none { it.id == target.chatId }) { fail(w,IncomingFailure.TargetUnavailable); return null }
        val notification = (w.entry as? IncomingEntry.Notification)?.target
        if (notification?.kind == NotificationTargetKind.Invite && p.chats.firstOrNull { it.id == notification.chatId }?.hasEndedMembership != false) { fail(w,IncomingFailure.TargetUnavailable); return null }
        val person = (w.entry as? IncomingEntry.ProfileLink)?.let { entry ->
            val ref = ProfileLinks.parse(entry.value,true) ?: return null
            PeopleDiscovery.resolve(p,ref.value).people.firstOrNull()?.person
        }
        if (target == null && person == null) { fail(w,IncomingFailure.TargetUnavailable); return null }
        navigating = id
        return IncomingOpen(w.id,target,person,p.id,(w.committed?.chatIds?.size ?: 1) - 1,w.committed?.dropped ?: 0, notification = notification)
    }
    fun opened(id: Long, accepted: Boolean) {
        val w = work?.takeIf { it.id == id && it.phase == IncomingPhase.Opening && navigating == id } ?: return
        navigating = null
        if (locked && activeId() == w.selectedProfileId && profile(w.selectedProfileId) != null) {
            work = w.copy(receivingProfileId = w.selectedProfileId); return
        }
        if (accepted && activeId() == w.selectedProfileId && profile(w.selectedProfileId) != null && !locked && ready())
            work = w.copy(phase = IncomingPhase.Complete, receivingProfileId = w.selectedProfileId) else fail(w,IncomingFailure.Open)
    }
    fun retry(id: Long): Boolean {
        val w = work?.takeIf { it.id == id && it.phase == IncomingPhase.Failed } ?: return false
        if (profile(w.selectedProfileId) == null) return false
        work = w.copy(attempt = w.attempt + 1, probe = 0, failure = null, phase = when {
            w.committed != null -> IncomingPhase.Opening
            w.entry is IncomingEntry.Share && w.prepared != null -> IncomingPhase.Choosing
            else -> IncomingPhase.Preparing
        }); return true
    }
    fun goToChats(id: Long): Boolean {
        val w = work?.takeIf { it.id == id && it.phase == IncomingPhase.Failed && it.entry !is IncomingEntry.Share } ?: return false
        val profile = profile(activeId()) ?: return false
        work = w.copy(selectedProfileId = profile.id, fallback = true, phase = IncomingPhase.Opening,
            scenario = IncomingScenario.Success, failure = null, attempt = w.attempt + 1)
        return true
    }
    fun cancel(id: Long) {
        if (work?.id == id) { work = null; navigating = null }
    }
    fun dismiss(id: Long) { if (work?.id == id && work?.running != true) { work = null; navigating = null } }
    private fun fail(w: IncomingWork, reason: IncomingFailure) { work = w.copy(phase = IncomingPhase.Failed, failure = reason) }
}
