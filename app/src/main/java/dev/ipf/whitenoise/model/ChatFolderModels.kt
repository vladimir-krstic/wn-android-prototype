package dev.ipf.whitenoise.model

import java.util.Locale

enum class ChatFolderKind { Unread, Archived, Groups }
data class ChatFolderRule(
    val personIds: Set<String> = emptySet(),
    val keyword: String = "",
    val unreadOnly: Boolean = false,
    val groupsOnly: Boolean = false,
    val archivedOnly: Boolean = false,
    val includeMuted: Boolean = false,
)
data class ChatFolderDraft(
    val name: String = "",
    val description: String = "",
    val chatIds: Set<String> = emptySet(),
    val rule: ChatFolderRule = ChatFolderRule(),
) {
    fun normalized() = copy(name = name.trim(), description = description.trim(), rule = rule.copy(keyword = rule.keyword.trim()))
    companion object {
        fun from(folder: ChatFolder?) = if (folder == null) ChatFolderDraft() else ChatFolderDraft(folder.name, folder.description, folder.chatIds, folder.rule)
    }
}

object ChatFolders {
    val defaults: List<ChatFolder> = listOf(
        ChatFolder("system:unread", "Unread", systemKind = ChatFolderKind.Unread, rule = ChatFolderRule(unreadOnly = true, includeMuted = true)),
        ChatFolder("system:archived", "Archived", systemKind = ChatFolderKind.Archived, rule = ChatFolderRule(archivedOnly = true, includeMuted = true)),
        ChatFolder("system:groups", "Groups", systemKind = ChatFolderKind.Groups, rule = ChatFolderRule(groupsOnly = true, includeMuted = true)),
    )

    fun matches(chat: Chat, rule: ChatFolderRule): Boolean {
        val keyword = rule.keyword.trim().lowercase(Locale.ROOT)
        val peopleMatch = chat.members.any { it.personId in rule.personIds } ||
            (chat.kind as? ChatKind.Direct)?.personId?.let { it in rule.personIds } == true
        val keywordMatch = keyword.isNotEmpty() && (chat.title.lowercase(Locale.ROOT).contains(keyword) ||
            (chat.isGroup && chat.description.lowercase(Locale.ROOT).contains(keyword)))
        val base = if (rule.personIds.isEmpty() && keyword.isEmpty()) rule.unreadOnly || rule.groupsOnly || rule.archivedOnly
            else peopleMatch || keywordMatch
        return base && rule.archivedOnly == chat.isArchived && (!rule.unreadOnly || chat.isUnread) &&
            (!rule.groupsOnly || chat.isGroup) && (rule.includeMuted || chat.muteDuration == null)
    }

    fun rows(chats: List<Chat>, folder: ChatFolder, query: String = ""): List<Chat> = chats.filter {
        it.id in folder.chatIds || matches(it, folder.rule)
    }.filter { query.isBlank() || it.title.normalizedSearchText().contains(query.normalizedSearchText()) ||
        it.displayPreview.normalizedSearchText().contains(query.normalizedSearchText()) }.sortedWith(ChatOrganization.order)

    fun preview(profile: Profile, draft: ChatFolderDraft) = rows(profile.chats,
        ChatFolder("preview", draft.name, draft.chatIds, draft.description, draft.rule))

    fun move(folders: List<ChatFolder>, id: String, delta: Int): List<ChatFolder> {
        val index = folders.indexOfFirst { it.id == id }
        if (delta !in setOf(-1, 1) || index < 0 || index + delta !in folders.indices) return folders
        return folders.toMutableList().apply { add(index + delta, removeAt(index)) }
    }
    fun restore(folders: List<ChatFolder>) = folders + defaults.filter { candidate -> folders.none { it.id == candidate.id } }
}
