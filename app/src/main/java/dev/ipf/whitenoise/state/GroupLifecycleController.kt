package dev.ipf.whitenoise.state

import androidx.compose.runtime.*
import dev.ipf.whitenoise.model.*

@Stable
class GroupLifecycleController(
    private val profiles: () -> List<Profile>, private val activeId: () -> String?,
    private val signedIn: (String) -> Boolean, private val otherLocked: (GroupOwner) -> Boolean,
    private val commit: (GroupOwner, GroupLifecycleStage, String?, Boolean) -> Boolean,
    private val setScenario: (GroupOwner, GroupStateScenario) -> Unit,
) {
    var work by mutableStateOf<Map<GroupOwner, GroupLifecycleWork>>(emptyMap()); private set
    var scenario by mutableStateOf(GroupLifecycleScenario.Success); private set
    var stateScenario by mutableStateOf(GroupStateScenario.Active); private set
    private var scenarioProfile: String? = null
    private var stateScenarioProfile: String? = null
    private var sequence = 0L
    private val presented = mutableMapOf<GroupOwner, GroupStateScenario>()
    private fun profile(id: String) = profiles().firstOrNull { it.id == id && signedIn(id) }
    private fun chat(owner: GroupOwner) = profile(owner.profileId)?.chats?.firstOrNull { it.id == owner.chatId }
    fun locked(owner: GroupOwner) = work[owner]?.running == true
    fun choose(value: GroupLifecycleScenario) {
        val p = profile(activeId() ?: return)?.takeIf { it.developerTools.isEnabled } ?: return
        scenarioProfile = p.id; scenario = value
    }
    fun chooseState(value: GroupStateScenario) {
        val p = profile(activeId() ?: return)?.takeIf { it.developerTools.isEnabled } ?: return
        scenarioProfile = p.id; stateScenarioProfile = p.id; stateScenario = value; presented.clear()
    }
    fun open(owner: GroupOwner) {
        reconcile()
        if (owner.profileId != activeId() || chat(owner)?.isGroup != true || presented[owner] == stateScenario || locked(owner) || otherLocked(owner)) return
        presented[owner] = stateScenario
        // Default opening must never reset a real accepted terminal state.
        if (stateScenarioProfile == owner.profileId) {
            setScenario(owner, stateScenario)
            if (stateScenario == GroupStateScenario.Disbanding) {
                val current = chat(owner) ?: return
                work = work + (owner to GroupLifecycleWork(++sequence, owner, GroupLifecycleAction.Disband, null, false,
                    GroupLifecycleStage.Converge, current.members, current.groupRoster.revision, scenario))
            }
        }
    }
    fun reconcile() {
        if (scenarioProfile != null && (scenarioProfile != activeId() || profile(scenarioProfile!!)?.developerTools?.isEnabled != true)) {
            scenarioProfile = null; stateScenarioProfile = null; scenario = GroupLifecycleScenario.Success; stateScenario = GroupStateScenario.Active; presented.clear()
        }
        work = work.filterKeys { signedIn(it.profileId) && (chat(it) != null || work[it]?.stage == GroupLifecycleStage.Complete) }.mapValues { (owner, w) ->
            if (w.running && owner.profileId != activeId() && w.stage != GroupLifecycleStage.Converge) w.copy(failure = GroupLifecycleFailure.Interrupted) else w
        }
        presented.keys.removeAll { chat(it) == null }
    }
    fun begin(owner: GroupOwner, action: GroupLifecycleAction, targetId: String? = null, thenLeave: Boolean = false): Boolean {
        reconcile(); val chat = chat(owner) ?: return false
        if (owner.profileId != activeId() || locked(owner) || otherLocked(owner)) return false
        val stage = when (action) {
            GroupLifecycleAction.Transfer -> GroupLifecycleStage.Grant
            GroupLifecycleAction.StepDown -> GroupLifecycleStage.StepDown
            GroupLifecycleAction.Leave -> if (chat.isSoleMember(owner.profileId)) GroupLifecycleStage.Delete
                else if (chat.hasGroupAdmin(owner.profileId)) GroupLifecycleStage.StepDown else GroupLifecycleStage.Leave
            GroupLifecycleAction.Delete -> GroupLifecycleStage.Delete
            GroupLifecycleAction.EnableDisband -> GroupLifecycleStage.Enable
            GroupLifecycleAction.Disband -> GroupLifecycleStage.AcceptDisband
            GroupLifecycleAction.Acknowledge -> GroupLifecycleStage.Acknowledge
            GroupLifecycleAction.Recover -> GroupLifecycleStage.Recover
        }
        if (!GroupLifecyclePolicy.permits(chat, owner.profileId, stage, targetId)) return false
        work = work + (owner to GroupLifecycleWork(++sequence, owner, action, targetId,
            thenLeave || action == GroupLifecycleAction.Leave, stage, chat.members, chat.groupRoster.revision, scenario))
        return true
    }
    fun canRetry(owner: GroupOwner, id: Long): Boolean {
        val w = work[owner]?.takeIf { it.id == id && it.failure != null } ?: return false
        val chat = chat(owner) ?: return false
        return owner.profileId == activeId() && !otherLocked(owner) &&
            (!w.granted || chat.members.any { it.personId == w.targetId && it.role == GroupRole.Admin }) &&
            GroupLifecyclePolicy.permits(chat, owner.profileId, w.stage, w.targetId)
    }
    fun retry(owner: GroupOwner, id: Long): Boolean {
        if (!canRetry(owner, id)) return false
        val w = work.getValue(owner); val chat = chat(owner) ?: return false
        work = work + (owner to w.copy(id = ++sequence, expectedMembers = chat.members, revision = chat.groupRoster.revision,
            scenario = GroupLifecycleScenario.Success, failure = null)); return true
    }
    fun dismiss(owner: GroupOwner, id: Long) {
        if (owner.profileId == activeId() && work[owner]?.let { it.id == id && !it.running } == true) work = work - owner
    }
    fun advance(owner: GroupOwner, id: Long, stage: GroupLifecycleStage) {
        reconcile(); val w = work[owner]?.takeIf { it.id == id && it.stage == stage && it.running } ?: return
        val chat = chat(owner)
        val failure = when {
            chat == null || otherLocked(owner) || (stage != GroupLifecycleStage.Converge &&
                (owner.profileId != activeId() || chat.members != w.expectedMembers || chat.groupRoster.revision != w.revision)) ||
                !GroupLifecyclePolicy.permits(chat, owner.profileId, stage, w.targetId) ||
                (w.granted && chat.members.none { it.personId == w.targetId && it.role == GroupRole.Admin }) -> GroupLifecycleFailure.SourceChanged
            fails(w.scenario, stage) -> GroupLifecycleFailure.Unavailable
            else -> null
        }
        if (failure != null) { work = work + (owner to w.copy(failure = failure)); return }
        val accepted = commit(owner, stage, w.targetId, w.scenario == GroupLifecycleScenario.ConvergenceFailure)
        if (!accepted) { work = work + (owner to w.copy(failure = GroupLifecycleFailure.SourceChanged)); return }
        val next = when (stage) {
            GroupLifecycleStage.Grant -> GroupLifecycleStage.StepDown
            GroupLifecycleStage.StepDown -> if (w.thenLeave) GroupLifecycleStage.Leave else GroupLifecycleStage.Complete
            GroupLifecycleStage.AcceptDisband -> GroupLifecycleStage.Converge
            else -> GroupLifecycleStage.Complete
        }
        val updated = chat(owner)
        work = work + (owner to w.copy(stage = next, expectedMembers = updated?.members ?: emptyList(),
            revision = updated?.groupRoster?.revision ?: w.revision, granted = w.granted || stage == GroupLifecycleStage.Grant,
            steppedDown = w.steppedDown || stage == GroupLifecycleStage.StepDown))
    }
    private fun fails(scenario: GroupLifecycleScenario, stage: GroupLifecycleStage) = when (scenario) {
        GroupLifecycleScenario.GrantFailure -> stage == GroupLifecycleStage.Grant
        GroupLifecycleScenario.StepDownFailure -> stage == GroupLifecycleStage.StepDown
        GroupLifecycleScenario.LeaveFailure -> stage == GroupLifecycleStage.Leave
        GroupLifecycleScenario.EnableFailure -> stage == GroupLifecycleStage.Enable
        GroupLifecycleScenario.DisbandFailure -> stage == GroupLifecycleStage.AcceptDisband
        GroupLifecycleScenario.AcknowledgeFailure -> stage == GroupLifecycleStage.Acknowledge
        GroupLifecycleScenario.RecoveryFailure -> stage == GroupLifecycleStage.Recover
        GroupLifecycleScenario.DeleteFailure -> stage == GroupLifecycleStage.Delete
        else -> false
    }
}
