package dev.ipf.whitenoise.model

data class ChatFolder(
    val id: String, val name: String, val chatIds: Set<String> = emptySet(),
    val description: String = "", val rule: ChatFolderRule = ChatFolderRule(), val systemKind: ChatFolderKind? = null,
)

object ChatOrganization {
    val order: Comparator<Chat> = compareByDescending<Chat> { it.isPinned }
        .thenBy { if (it.isPinned) it.pinnedOrder ?: it.originalOrder else it.originalOrder }
        .thenBy { it.originalOrder }

    fun pinned(chats: List<Chat>) = chats.filter { it.isPinned && !it.isArchived }.sortedWith(order)

    fun move(chats: List<Chat>, id: String, delta: Int): List<Chat> {
        if (delta != -1 && delta != 1) return chats
        val ids = pinned(chats).map { it.id }.toMutableList()
        val index = ids.indexOf(id)
        if (index < 0 || index + delta !in ids.indices) return chats
        ids[index] = ids[index + delta].also { ids[index + delta] = id }
        return chats.map { chat -> ids.indexOf(chat.id).takeIf { it >= 0 }?.let { chat.copy(pinnedOrder = it) } ?: chat }
    }

    fun actions(chat: Chat, chats: List<Chat>): List<ChatListAction> {
        val ids = pinned(chats).map { it.id }
        val index = ids.indexOf(chat.id)
        return ChatListActionPolicy.all(chat) + buildList {
            if (index > 0) add(ChatListAction.MoveUp)
            if (index >= 0 && index < ids.lastIndex) add(ChatListAction.MoveDown)
        }
    }

    fun reconcile(selected: List<String>, visible: List<Chat>) = selected.distinct().filter { id -> visible.any { it.id == id } }
    fun archiveAction(chats: List<Chat>) = if (chats.isNotEmpty() && chats.all { it.isArchived }) ChatBulkAction.Unarchive else ChatBulkAction.Archive
    fun requiresAdmin(chat: Chat, owner: String) = chat.membership == ChatMembership.Active && chat.groupLifecycle == GroupLifecycle.Active &&
        chat.isSoleAdmin(owner) && chat.members.any { it.personId != owner }
    fun requiresLeave(chat: Chat, owner: String) = chat.membership == ChatMembership.Active && chat.groupLifecycle != GroupLifecycle.Disbanded &&
        (!chat.isGroup || chat.members.singleOrNull()?.personId != owner)
}

enum class ChatBulkAction { Read, Unread, Archive, Unarchive, Folder, Delete }
enum class ChatBatchScenario(val developerLabel: String) {
    Success("Success"), PartialApply("Second chat fails once"), LeaveFailure("Next leave fails"), DeleteFailure("Next local deletion fails")
}
enum class ChatBatchPhase { Applying, Leaving, Deleting, Finished }
enum class ChatBatchFailure { Unavailable, LeaveFailed, DeleteFailed, NeedsAdmin }
data class ChatBatchResult(val chatId: String, val title: String, val failure: ChatBatchFailure? = null, val leftBeforeDeletion: Boolean = false)
data class ChatBatchAttempt(
    val id: Long, val profileId: String, val targets: List<String>, val action: ChatBulkAction,
    val folderId: String? = null, val leaveFirst: Boolean = false,
    val index: Int = 0, val phase: ChatBatchPhase = ChatBatchPhase.Applying,
    val results: List<ChatBatchResult> = emptyList(), val scenario: ChatBatchScenario = ChatBatchScenario.Success,
    val scenarioConsumed: Boolean = false,
) {
    val isBusy get() = phase != ChatBatchPhase.Finished
    val failedIds get() = results.filter { it.failure != null }.map { it.chatId }
    val completedCount get() = results.count { it.failure == null }
}

enum class ChatConnectionPhase { Online, Offline, Connecting, CatchingUp, Failed }
enum class ChatConnectionScenario(val developerLabel: String) {
    Online("Online"), Offline("Offline"), Connecting("Connecting"), CatchingUp("Catching up"), RetryFailure("First retry fails")
}
data class ChatConnectionState(val phase: ChatConnectionPhase = ChatConnectionPhase.Online, val generation: Long = 0, val retryFails: Boolean = false)
