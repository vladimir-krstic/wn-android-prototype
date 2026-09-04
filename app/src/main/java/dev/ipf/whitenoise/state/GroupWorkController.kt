package dev.ipf.whitenoise.state

import androidx.compose.runtime.*
import dev.ipf.whitenoise.model.*

/** Profile-owned local operations. Accepted commits outlive navigation; pre-commit callbacks are revalidated. */
@Stable
class GroupWorkController(
    private val profiles: () -> List<Profile>,
    private val activeId: () -> String?,
    private val signedIn: (String) -> Boolean,
    private val changeRoster: (GroupOwner, GroupRoster) -> Unit,
    private val commitMembers: (GroupOwner, GroupMemberAction, List<String>) -> Boolean,
    private val commitEdit: (GroupOwner, GroupEditDraft) -> Boolean,
    private val create: (GroupEditDraft, List<String>) -> String?,
    private val applyTimer: (GroupOwner, DisappearingDuration) -> Boolean,
) {
    var memberWork by mutableStateOf<Map<GroupOwner, GroupMemberWork>>(emptyMap()); private set
    var editWork by mutableStateOf<Map<GroupOwner, GroupEditWork>>(emptyMap()); private set
    var rosterLoads by mutableStateOf<Map<GroupOwner, GroupRosterLoad>>(emptyMap()); private set
    var creation by mutableStateOf<GroupCreateWork?>(null); private set
    var rosterScenario by mutableStateOf(GroupRosterScenario.Ready); private set
    var mutationScenario by mutableStateOf(GroupMutationScenario.Success); private set
    var imageScenario by mutableStateOf(GroupImageScenario.Success); private set
    var createScenario by mutableStateOf(GroupCreateScenario.Success); private set
    private var scenarioProfile: String? = null
    private var sequence = 0L
    private var committing: GroupOwner? = null
    private val presented = mutableMapOf<GroupOwner, GroupRosterScenario>()
    private fun profile(id: String) = profiles().firstOrNull { it.id == id && signedIn(id) }
    private fun chat(owner: GroupOwner) = profile(owner.profileId)?.chats?.firstOrNull { it.id == owner.chatId }
    fun locked(owner: GroupOwner): Boolean = memberWork[owner]?.running == true || editWork[owner]?.phase == GroupWorkPhase.Applying
    fun permitsPrimitive(owner: GroupOwner): Boolean = committing == owner || !locked(owner)
    private fun developer(): Boolean {
        val p = profile(activeId() ?: return false)?.takeIf { it.developerTools.isEnabled } ?: return false
        scenarioProfile = p.id; return true
    }
    fun chooseRoster(value: GroupRosterScenario) { if (developer()) { rosterScenario = value; presented.clear() } }
    fun chooseMutation(value: GroupMutationScenario) { if (developer()) mutationScenario = value }
    fun chooseImage(value: GroupImageScenario) { if (developer()) imageScenario = value }
    fun chooseCreate(value: GroupCreateScenario) { if (developer()) createScenario = value }
    fun reconcile() {
        if (scenarioProfile != null && (scenarioProfile != activeId() || profile(scenarioProfile!!)?.developerTools?.isEnabled != true)) {
            scenarioProfile = null; rosterScenario = GroupRosterScenario.Ready; mutationScenario = GroupMutationScenario.Success
            imageScenario = GroupImageScenario.Success; createScenario = GroupCreateScenario.Success; presented.clear()
        }
        memberWork = memberWork.filterKeys { chat(it) != null }.mapValues { (owner, work) ->
            if (work.phase == GroupWorkPhase.Applying && owner.profileId != activeId()) work.copy(phase = GroupWorkPhase.Failed, failure = GroupWorkFailure.Interrupted) else work
        }
        editWork = editWork.filterKeys { chat(it) != null }.mapValues { (owner, work) ->
            if (work.phase == GroupWorkPhase.Applying && owner.profileId != activeId()) work.copy(phase = GroupWorkPhase.Failed, failure = GroupWorkFailure.Interrupted) else work
        }
        rosterLoads = rosterLoads.filterKeys { chat(it) != null && it.profileId == activeId() }
        presented.keys.removeAll { chat(it) == null }
        if (creation?.profileId != activeId()) creation = null
    }
    fun openRoster(owner: GroupOwner) {
        reconcile()
        if (owner.profileId != activeId() || chat(owner)?.isGroup != true || presented[owner] == rosterScenario) return
        presented[owner] = rosterScenario
        if (rosterScenario == GroupRosterScenario.Ready && chat(owner)?.groupRoster?.status == GroupRosterStatus.Ready) return
        loadRoster(owner, rosterScenario)
    }
    private fun loadRoster(owner: GroupOwner, scenario: GroupRosterScenario) {
        val chat = chat(owner)?.takeIf { owner.profileId == activeId() && it.isGroup } ?: return
        val status = if (scenario == GroupRosterScenario.Unknown) GroupRosterStatus.Unknown else GroupRosterStatus.Loading
        val roster = GroupRoster(status, chat.groupRoster.revision + 1, scenario == GroupRosterScenario.WarmLoading && chat.hasGroupAdmin(owner.profileId))
        changeRoster(owner, roster)
        rosterLoads = if (status == GroupRosterStatus.Unknown) rosterLoads - owner else rosterLoads + (owner to GroupRosterLoad(++sequence, owner, roster.revision, scenario))
    }
    fun retryRoster(owner: GroupOwner) { if (owner.profileId == activeId()) loadRoster(owner, GroupRosterScenario.Ready) }
    fun advanceRoster(owner: GroupOwner, id: Long) {
        reconcile(); val load = rosterLoads[owner]?.takeIf { it.id == id } ?: return
        val current = chat(owner)?.groupRoster ?: return
        rosterLoads = rosterLoads - owner
        if (current.revision != load.revision || current.status != GroupRosterStatus.Loading) return
        val status = when (load.scenario) { GroupRosterScenario.Failed -> GroupRosterStatus.Failed; GroupRosterScenario.Inconsistent -> GroupRosterStatus.Inconsistent; else -> GroupRosterStatus.Ready }
        changeRoster(owner, current.copy(status = status, revision = current.revision + 1))
    }
    fun beginMembers(owner: GroupOwner, action: GroupMemberAction, ids: List<String>, retry: Boolean = false): Boolean {
        reconcile(); val profile = profile(owner.profileId)?.takeIf { it.id == activeId() } ?: return false
        val chat = chat(owner) ?: return false
        if (locked(owner)) return false
        val work = GroupMemberWork(++sequence, owner, action, ids.distinct(), chat.groupRoster.revision, chat.members,
            if (retry) GroupMutationScenario.Success else mutationScenario)
        if (!work.eligible(profile, chat)) return false
        memberWork = memberWork + (owner to work); return true
    }
    fun canRetryMembers(owner: GroupOwner, id: Long): Boolean {
        val work = memberWork[owner]?.takeIf { it.id == id && it.phase == GroupWorkPhase.Failed } ?: return false
        val profile = profile(owner.profileId)?.takeIf { it.id == activeId() } ?: return false
        val chat = chat(owner) ?: return false
        return !locked(owner) && work.copy(rosterRevision = chat.groupRoster.revision, expectedMembers = chat.members).eligible(profile, chat)
    }
    fun retryMembers(owner: GroupOwner, id: Long): Boolean {
        if (owner.profileId != activeId()) return false
        val work = memberWork[owner]?.takeIf { it.id == id && it.phase == GroupWorkPhase.Failed } ?: return false
        if (beginMembers(owner, work.action, work.personIds, retry = true)) return true
        if (memberWork[owner]?.id == id) memberWork = memberWork + (owner to work.copy(failure = GroupWorkFailure.SourceChanged))
        return false
    }
    fun dismissMembers(owner: GroupOwner, id: Long) {
        if (owner.profileId != activeId()) return
        memberWork[owner]?.takeIf { it.id == id && !it.running }?.let { memberWork = memberWork - owner }
    }
    fun advanceMembers(owner: GroupOwner, id: Long, phase: GroupWorkPhase) {
        reconcile(); val work = memberWork[owner]?.takeIf { it.id == id && it.phase == phase } ?: return
        if (phase == GroupWorkPhase.Converging) { memberWork = memberWork + (owner to work.copy(phase = GroupWorkPhase.Complete)); return }
        if (phase != GroupWorkPhase.Applying) return
        val p = profile(owner.profileId); val chat = chat(owner)
        val failure = when {
            p == null || chat == null || owner.profileId != activeId() || !work.eligible(p, chat) || work.scenario == GroupMutationScenario.RosterChanged -> GroupWorkFailure.SourceChanged
            work.scenario == GroupMutationScenario.Failure -> GroupWorkFailure.Unavailable
            else -> null
        }
        val accepted = failure == null && try { committing = owner; commitMembers(owner, work.action, work.personIds) } finally { committing = null }
        memberWork = memberWork + (owner to work.copy(phase = if (accepted) GroupWorkPhase.Converging else GroupWorkPhase.Failed,
            failure = if (accepted) null else failure ?: GroupWorkFailure.SourceChanged))
    }
    fun beginEdit(owner: GroupOwner, expected: GroupEditDraft, draft: GroupEditDraft, retry: Boolean = false): Boolean {
        reconcile(); val chat = chat(owner) ?: return false
        if (owner.profileId != activeId() || !chat.hasAuthoritativeGroupAdmin(owner.profileId) || locked(owner) || draft.name.isBlank()) return false
        val work = GroupEditWork(++sequence, owner, expected, draft, if (retry) GroupImageScenario.Success else imageScenario, chat.groupRoster.revision)
        editWork = editWork + (owner to work); return true
    }
    fun retryEdit(owner: GroupOwner, id: Long): Boolean {
        if (owner.profileId != activeId()) return false
        val work = editWork[owner]?.takeIf { it.id == id && it.phase == GroupWorkPhase.Failed } ?: return false
        val chat = chat(owner) ?: return false
        // A changed source must be reviewed in the form; retries never silently rebase over it.
        if (GroupEditDraft.from(chat) != work.expected) {
            editWork = editWork + (owner to work.copy(failure = GroupWorkFailure.SourceChanged))
            return false
        }
        return beginEdit(owner, work.expected, work.draft, retry = true)
    }
    fun dismissEdit(owner: GroupOwner, id: Long) {
        if (owner.profileId != activeId()) return
        editWork[owner]?.takeIf { it.id == id && it.phase != GroupWorkPhase.Applying }?.let { editWork = editWork - owner }
    }
    fun advanceEdit(owner: GroupOwner, id: Long) {
        reconcile(); val work = editWork[owner]?.takeIf { it.id == id && it.phase == GroupWorkPhase.Applying } ?: return
        val chat = chat(owner)
        val failure = when {
            chat == null || owner.profileId != activeId() || !chat.hasAuthoritativeGroupAdmin(owner.profileId) || chat.groupRoster.revision != work.rosterRevision || GroupEditDraft.from(chat) != work.expected -> GroupWorkFailure.SourceChanged
            work.scenario == GroupImageScenario.UploadFailure && (work.draft.image != work.expected.image || work.draft.publicImage != work.expected.publicImage) -> GroupWorkFailure.Upload
            work.scenario == GroupImageScenario.SaveFailure -> GroupWorkFailure.Unavailable
            else -> null
        }
        val accepted = failure == null && try { committing = owner; commitEdit(owner, work.draft) } finally { committing = null }
        editWork = editWork + (owner to work.copy(phase = if (accepted) GroupWorkPhase.Complete else GroupWorkPhase.Failed,
            failure = if (accepted) null else failure ?: GroupWorkFailure.SourceChanged))
    }
    fun beginCreate(profileId: String, origin: String, draft: GroupEditDraft, ids: List<String>, timer: DisappearingDuration): Boolean {
        reconcile(); val p = profile(profileId)?.takeIf { it.id == activeId() } ?: return false
        if (creation != null || draft.name.isBlank() || p.chatRelayUrls.isEmpty() || ids.any { id -> id == p.id || id == "white-noise-support" || p.people.none { it.id == id } }) return false
        creation = GroupCreateWork(++sequence, profileId, origin, draft, ids.distinct(), timer, createScenario); return true
    }
    fun advanceCreate(id: Long, phase: GroupCreatePhase) {
        reconcile(); val work = creation?.takeIf { it.id == id && it.phase == phase && it.running } ?: return
        creation = when (phase) {
            GroupCreatePhase.Creating -> {
                val chatId = if (work.scenario == GroupCreateScenario.CreateFailure) null else create(work.draft, work.personIds)
                if (chatId == null) work.copy(phase = GroupCreatePhase.Failed) else work.copy(chatId = chatId,
                    phase = if (work.timer == DisappearingDuration.Off) GroupCreatePhase.Opening else GroupCreatePhase.ApplyingTimer)
            }
            GroupCreatePhase.ApplyingTimer -> {
                val applied = work.scenario != GroupCreateScenario.TimerFailure && applyTimer(GroupOwner(work.profileId, work.chatId!!), work.timer)
                work.copy(phase = if (applied) GroupCreatePhase.Opening else GroupCreatePhase.TimerFailed, timerApplied = applied)
            }
            GroupCreatePhase.Opening -> work.copy(phase = if (work.scenario == GroupCreateScenario.OpenFailure) GroupCreatePhase.OpenFailed else GroupCreatePhase.Ready)
            else -> work
        }
    }
    fun retryCreate(id: Long) {
        val work = creation?.takeIf { it.id == id && it.profileId == activeId() } ?: return
        val phase = when (work.phase) { GroupCreatePhase.Failed -> GroupCreatePhase.Creating; GroupCreatePhase.TimerFailed -> GroupCreatePhase.ApplyingTimer; GroupCreatePhase.OpenFailed -> GroupCreatePhase.Opening; else -> return }
        creation = work.copy(id = ++sequence, phase = phase, scenario = GroupCreateScenario.Success)
    }
    fun skipFailedTimer(id: Long) {
        creation?.takeIf { it.id == id && it.profileId == activeId() && it.phase == GroupCreatePhase.TimerFailed }
            ?.let { creation = it.copy(id = ++sequence, phase = GroupCreatePhase.Opening, scenario = GroupCreateScenario.Success) }
    }
    fun takeCreated(id: Long, origin: String): String? {
        val work = creation?.takeIf { it.id == id && it.origin == origin && it.profileId == activeId() && it.phase == GroupCreatePhase.Ready } ?: return null
        val chatId = work.chatId?.takeIf { chat(GroupOwner(work.profileId, it)) != null } ?: return null
        creation = null; return chatId
    }
    fun leaveCreation(origin: String) { if (creation?.origin == origin) creation = null }
}
