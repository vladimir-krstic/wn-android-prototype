package dev.ipf.whitenoise.model

enum class GroupLifecycle { Active, Unrecoverable, Disbanding, Disbanded }
enum class DisbandBlocker { UnsupportedMembers, PendingInvitations, UpdateInProgress }
data class GroupDisbandCapability(
    val canEnable: Boolean = true,
    val enabled: Boolean = false,
    val canDisband: Boolean = true,
    val blockers: Set<DisbandBlocker> = emptySet(),
    val requestFailed: Boolean = false,
)
enum class GroupLifecycleAction { Transfer, StepDown, Leave, Delete, EnableDisband, Disband, Acknowledge, Recover }
enum class GroupLifecycleStage { Grant, StepDown, Leave, Delete, Enable, AcceptDisband, Converge, Acknowledge, Recover, Complete }
enum class GroupLifecycleFailure { Unavailable, SourceChanged, Interrupted }
enum class GroupLifecycleScenario(val developerLabel: String) {
    Success("Administration succeeds"), GrantFailure("Transfer grant fails"), StepDownFailure("Step-down fails"),
    LeaveFailure("Leave fails after step-down"), EnableFailure("Enable disbanding fails"),
    DisbandFailure("Disband request fails"), ConvergenceFailure("Accepted disband fails"),
    AcknowledgeFailure("Failure acknowledgment fails"), RecoveryFailure("Group repair fails"), DeleteFailure("Local deletion fails")
}
enum class GroupStateScenario(val developerLabel: String) {
    Active("Active group"), Frozen("Unrecoverable group"), Disbanding("Disbanding group"), Ended("Ended group"),
    Unsupported("Disbanding blocked by members"), PendingInvitations("Disbanding blocked by invitations"), CapabilityUnavailable("Disband capability unavailable")
}
data class GroupLifecycleWork(
    val id: Long, val owner: GroupOwner, val action: GroupLifecycleAction, val targetId: String?,
    val thenLeave: Boolean, val stage: GroupLifecycleStage, val expectedMembers: List<GroupMember>,
    val revision: Long, val scenario: GroupLifecycleScenario, val failure: GroupLifecycleFailure? = null,
    val granted: Boolean = false, val steppedDown: Boolean = false,
) {
    val running get() = failure == null && stage != GroupLifecycleStage.Complete
}

fun Chat.hasVerifiedSelf(profileId: String): Boolean = isGroup && membership == ChatMembership.Active &&
    groupRoster.status == GroupRosterStatus.Ready && members.any { it.personId == profileId }
fun Chat.isSoleMember(profileId: String): Boolean = hasVerifiedSelf(profileId) && members.map { it.personId }.distinct() == listOf(profileId)
fun Chat.canLeaveWithoutTransfer(profileId: String): Boolean = hasVerifiedSelf(profileId) &&
    groupLifecycle == GroupLifecycle.Active && (!isSoleAdmin(profileId) || isSoleMember(profileId))

/** Stage guards are repeated immediately before each accepted local commit. */
object GroupLifecyclePolicy {
    fun permits(chat: Chat, profileId: String, stage: GroupLifecycleStage, targetId: String?): Boolean {
        val active = chat.groupLifecycle == GroupLifecycle.Active
        val admin = chat.hasAuthoritativeGroupAdmin(profileId)
        val capability = chat.disbandCapability
        return when (stage) {
            GroupLifecycleStage.Grant -> admin && targetId != profileId && targetId != "white-noise-support" &&
                chat.members.any { it.personId == targetId && it.role == GroupRole.Member }
            GroupLifecycleStage.StepDown -> admin && chat.members.any { it.personId != profileId && it.role == GroupRole.Admin }
            GroupLifecycleStage.Leave -> active && chat.hasVerifiedSelf(profileId) && !chat.isSoleAdmin(profileId)
            GroupLifecycleStage.Delete -> chat.isSoleMember(profileId) && active || chat.hasEndedMembership || chat.groupLifecycle == GroupLifecycle.Disbanded
            GroupLifecycleStage.Enable -> admin && capability.canEnable && !capability.enabled && capability.blockers.isEmpty() && !capability.requestFailed
            GroupLifecycleStage.AcceptDisband -> admin && capability.enabled && capability.canDisband && capability.blockers.isEmpty() && !capability.requestFailed
            GroupLifecycleStage.Converge -> chat.groupLifecycle == GroupLifecycle.Disbanding
            GroupLifecycleStage.Acknowledge -> chat.hasVerifiedSelf(profileId) && capability.requestFailed
            GroupLifecycleStage.Recover -> chat.hasVerifiedSelf(profileId) && chat.groupLifecycle == GroupLifecycle.Unrecoverable
            GroupLifecycleStage.Complete -> false
        }
    }

    /** Null is used only for an explicitly permitted local deletion. */
    fun apply(chat: Chat, profileId: String, stage: GroupLifecycleStage, targetId: String?, convergenceFailed: Boolean = false): Chat? {
        require(permits(chat, profileId, stage, targetId))
        val result = when (stage) {
            GroupLifecycleStage.Grant -> chat.copy(members = chat.members.map { if (it.personId == targetId) it.copy(role = GroupRole.Admin) else it })
            GroupLifecycleStage.StepDown -> chat.copy(members = chat.members.map { if (it.personId == profileId) it.copy(role = GroupRole.Member) else it })
            GroupLifecycleStage.Leave -> chat.copy(membership = ChatMembership.Left, members = chat.members.filterNot { it.personId == profileId },
                isPinned = false, isMarkedUnread = false, unreadCount = 0, readState = chat.readState?.copy(unreadIds = emptySet()))
            GroupLifecycleStage.Delete -> return null
            GroupLifecycleStage.Enable -> chat.copy(disbandCapability = chat.disbandCapability.copy(enabled = true))
            GroupLifecycleStage.AcceptDisband -> chat.copy(groupLifecycle = GroupLifecycle.Disbanding)
            GroupLifecycleStage.Converge -> chat.copy(groupLifecycle = if (convergenceFailed) GroupLifecycle.Active else GroupLifecycle.Disbanded,
                disbandCapability = chat.disbandCapability.copy(requestFailed = convergenceFailed))
            GroupLifecycleStage.Acknowledge -> chat.copy(disbandCapability = chat.disbandCapability.copy(requestFailed = false))
            GroupLifecycleStage.Recover -> chat.copy(groupLifecycle = GroupLifecycle.Active)
            GroupLifecycleStage.Complete -> error("No complete-stage mutation")
        }
        return if (result.members != chat.members) result.copy(groupRoster = result.groupRoster.copy(revision = chat.groupRoster.revision + 1)) else result
    }
}
