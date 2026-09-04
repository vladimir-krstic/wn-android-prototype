package dev.ipf.whitenoise.model

data class GroupOwner(val profileId: String, val chatId: String)
enum class GroupRosterStatus { Unknown, Loading, Ready, Failed, Inconsistent }
data class GroupRoster(val status: GroupRosterStatus = GroupRosterStatus.Ready, val revision: Long = 0, val seededSelfMember: Boolean = false)
fun Chat.hasGroupAdmin(profileId: String): Boolean = isGroup && groupLifecycle == GroupLifecycle.Active && membership == ChatMembership.Active && members.any { it.personId == profileId && it.role == GroupRole.Admin }
fun Chat.hasAuthoritativeGroupAdmin(profileId: String): Boolean = groupRoster.status == GroupRosterStatus.Ready && hasGroupAdmin(profileId)
fun Chat.canPresentMemberAdministration(profileId: String): Boolean = hasGroupAdmin(profileId) &&
    (groupRoster.status == GroupRosterStatus.Ready || (groupRoster.status == GroupRosterStatus.Loading && groupRoster.seededSelfMember))

enum class GroupRosterScenario(val developerLabel: String) {
    Ready("Verified roster"), WarmLoading("Known member, refreshing"), ColdLoading("Checking membership"),
    Unknown("Membership unknown"), Failed("Roster load fails"), Inconsistent("Roster is inconsistent")
}
enum class GroupMutationScenario(val developerLabel: String) { Success("Member update succeeds"), Failure("Member update fails"), RosterChanged("Roster changes before commit") }
enum class GroupImageScenario(val developerLabel: String) { Success("Images available"), Loading("Image loading"), LoadFailure("Image unavailable"), UploadFailure("Image upload fails"), SaveFailure("Group save fails"), UnsupportedEmoji("Emoji unavailable"), RenderFailure("Emoji image fails") }
enum class GroupCreateScenario(val developerLabel: String) { Success("Group creation succeeds"), CreateFailure("Creation fails"), TimerFailure("Timer fails after creation"), OpenFailure("Opening fails after creation") }
enum class GroupMemberAction { Invite, Promote, Revoke, Remove }
enum class GroupWorkPhase { Applying, Converging, Complete, Failed }
enum class GroupWorkFailure { Unavailable, SourceChanged, Interrupted, Upload }
data class GroupMemberWork(val id: Long, val owner: GroupOwner, val action: GroupMemberAction, val personIds: List<String>,
    val rosterRevision: Long, val expectedMembers: List<GroupMember>, val scenario: GroupMutationScenario,
    val phase: GroupWorkPhase = GroupWorkPhase.Applying, val failure: GroupWorkFailure? = null) {
    val running get() = phase == GroupWorkPhase.Applying || phase == GroupWorkPhase.Converging
    fun eligible(profile: Profile, chat: Chat): Boolean = profile.id == owner.profileId && chat.id == owner.chatId &&
        chat.hasAuthoritativeGroupAdmin(profile.id) && chat.groupRoster.revision == rosterRevision && chat.members == expectedMembers &&
        personIds.isNotEmpty() && personIds.all { id -> id != profile.id && id != "white-noise-support" && when (action) {
            GroupMemberAction.Invite -> profile.people.any { it.id == id } && chat.members.none { it.personId == id }
            GroupMemberAction.Promote -> chat.members.any { it.personId == id && it.role == GroupRole.Member }
            GroupMemberAction.Revoke -> chat.members.any { it.personId == id && it.role == GroupRole.Admin } && chat.members.count { it.role == GroupRole.Admin } > 1
            GroupMemberAction.Remove -> chat.members.any { it.personId == id } && !(chat.members.first { it.personId == id }.role == GroupRole.Admin && chat.members.count { it.role == GroupRole.Admin } <= 1)
        } }
}
data class GroupEditDraft(val name: String, val description: String, val image: ProfileAvatar, val publicImage: ProfileAvatar) {
    companion object { fun from(chat: Chat) = GroupEditDraft(chat.title, chat.description, chat.avatar, chat.publicInviteAvatar) }
}
data class GroupEditWork(val id: Long, val owner: GroupOwner, val expected: GroupEditDraft, val draft: GroupEditDraft,
    val scenario: GroupImageScenario, val rosterRevision: Long, val phase: GroupWorkPhase = GroupWorkPhase.Applying, val failure: GroupWorkFailure? = null)
enum class GroupCreatePhase { Creating, ApplyingTimer, Opening, Ready, Failed, TimerFailed, OpenFailed }
data class GroupCreateWork(val id: Long, val profileId: String, val origin: String, val draft: GroupEditDraft,
    val personIds: List<String>, val timer: DisappearingDuration, val scenario: GroupCreateScenario,
    val phase: GroupCreatePhase = GroupCreatePhase.Creating, val chatId: String? = null, val timerApplied: Boolean = false) {
    val running get() = phase in setOf(GroupCreatePhase.Creating, GroupCreatePhase.ApplyingTimer, GroupCreatePhase.Opening)
}
data class GroupRosterLoad(val id: Long, val owner: GroupOwner, val revision: Long, val scenario: GroupRosterScenario)

/** Selection preserves catalog graphemes; duplicate emoji are intentional and count as separate slots. */
data class GroupEmojiSelection(val emojis: List<String> = emptyList(), val limitReached: Boolean = false) {
    fun add(emoji: String): GroupEmojiSelection = when {
        emoji.isBlank() -> this
        emojis.size >= 2 -> copy(limitReached = true)
        else -> copy(emojis = emojis + emoji, limitReached = false)
    }
    fun remove(index: Int) = copy(emojis = emojis.filterIndexed { i, _ -> i != index }, limitReached = false)
}
