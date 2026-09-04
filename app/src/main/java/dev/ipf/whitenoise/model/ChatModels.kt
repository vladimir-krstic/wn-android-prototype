package dev.ipf.whitenoise.model

import java.text.Normalizer

enum class ChatScope(val label: String) {
    Chats("Chats"),
    Unread("Unread"),
    Archived("Archived"),
    Left("Left"),
}

enum class ChatMembership {
    Invited,
    Active,
    Left,
    Removed,
}

enum class MuteDuration(val label: String) {
    OneHour("1 Hour"),
    EightHours("8 Hours"),
    OneDay("1 Day"),
    OneWeek("1 Week"),
    Always("Always"),
}

enum class DisappearingDuration(val label: String) {
    Off("Off"),
    OneDay("1 Day"),
    OneWeek("1 Week"),
    FourWeeks("4 Weeks"),

    ;

    val compactLabel: String
        get() = when (this) {
            Off -> "Off"
            OneDay -> "1d"
            OneWeek -> "1w"
            FourWeeks -> "4w"
        }
}

enum class ChatDeliveryState {
    None,
    Failed,
}

sealed interface AttachmentPreview {
    val label: String

    data object Photo : AttachmentPreview {
        override val label = "Photo"
    }

    data class Photos(val count: Int) : AttachmentPreview {
        override val label = "$count Photos"
    }

    data object Video : AttachmentPreview {
        override val label = "Video"
    }

    data object VoiceMessage : AttachmentPreview {
        override val label = "Voice message"
    }

    data class File(val name: String) : AttachmentPreview {
        override val label = name
    }

    data class Contact(val name: String) : AttachmentPreview {
        override val label = "Contact: $name"
    }

    data object Link : AttachmentPreview {
        override val label = "Link"
    }

    data object Gif : AttachmentPreview {
        override val label = "GIF"
    }
}

sealed interface ChatKind {
    data class Direct(val personId: String) : ChatKind
    data object Group : ChatKind
}

enum class GroupRole {
    Member,
    Admin,
}

data class GroupMember(
    val personId: String,
    val role: GroupRole,
)

sealed interface ChatTimelineEntry {
    val id: String
    val dayOrdinal: Int
    val dayLabel: String
    val minuteOfDay: Int

    data class Event(
        override val id: String,
        val text: String,
        override val dayOrdinal: Int = 0,
        override val dayLabel: String = "Today",
        override val minuteOfDay: Int = 0,
    ) : ChatTimelineEntry

    data class Notice(
        override val id: String,
        val text: String,
        override val dayOrdinal: Int = 0,
        override val dayLabel: String = "Today",
        override val minuteOfDay: Int = 0,
    ) : ChatTimelineEntry

    data class Message(val message: ChatMessage) : ChatTimelineEntry {
        override val id: String get() = message.id
        override val dayOrdinal: Int get() = message.dayOrdinal
        override val dayLabel: String get() = message.dayLabel
        override val minuteOfDay: Int get() = message.minuteOfDay
    }
}

enum class MessageDeliveryState {
    Sending,
    Sent,
    Failed,
    Streaming,
}

enum class MessageDeletionState {
    None,
    DeletedByCurrentProfile,
    DeletedByOther,
}

enum class MessageAttachmentKind {
    Photo,
    Photos,
    Video,
    Voice,
    File,
    Contact,
    Link,
    Gif,
}

enum class VoiceMessageFormat(val label: String) {
    Voice("Voice"),
    Text("Text"),
    Both("Both"),
}

data class MessageAttachment(
    val id: String,
    val kind: MessageAttachmentKind,
    val label: String,
    val images: List<ProfileAvatar> = emptyList(),
    val externalUri: String? = null,
    val linkTitle: String? = null,
    val linkDomain: String? = null,
    val linkSummary: String? = null,
    val transcript: String? = null,
    val durationSeconds: Int? = null,
    val voiceFormat: VoiceMessageFormat? = null,
    val fileSizeBytes: Int? = null,
    val contactPersonId: String? = null,
    val pixelWidth: Int? = null,
    val pixelHeight: Int? = null,
    val isAvailable: Boolean = true,
    val mimeType: String? = null,
    val localSource: AttachmentLocalSource? = null,
    val sourceImages: List<ProfileAvatar> = emptyList(),
    val photoEdits: Map<Int, PhotoEditRecipe> = emptyMap(),
    val photoFrameQualities: Map<Int, PhotoQuality> = emptyMap(),
    val photoQuality: PhotoQuality? = null,
    val metadataPolicy: PhotoMetadataPolicy? = null,
    val deviceContact: SharedDeviceContact? = null,
    val transfer: AttachmentTransfer? = null,
)

data class MessageReaction(
    val emoji: String,
    val personIds: List<String>,
)

data class ChatMessage(
    val id: String,
    val authorId: String,
    val dayOrdinal: Int,
    val dayLabel: String,
    val minuteOfDay: Int,
    val timeLabel: String,
    val text: String = "",
    val attachments: List<MessageAttachment> = emptyList(),
    val replyToMessageId: String? = null,
    val reactions: List<MessageReaction> = emptyList(),
    val deliveryState: MessageDeliveryState = MessageDeliveryState.Sent,
    val deletionState: MessageDeletionState = MessageDeletionState.None,
    val createdAtMillis: Long? = null,
    val receivedAtMillis: Long? = null,
    val expiresAtMillis: Long? = null,
    val editHistory: MessageEditHistory? = null,
    val editAttempt: MessageEditAttempt? = null,
) {
    val isDeleted: Boolean
        get() = deletionState != MessageDeletionState.None
}

data class Chat(
    val id: String,
    val originalOrder: Int,
    val kind: ChatKind,
    val title: String,
    val avatar: ProfileAvatar = ProfileAvatar.Monogram,
    val preview: String = "",
    val previewAuthor: String? = null,
    val attachmentPreview: AttachmentPreview? = null,
    val timestamp: String = "Now",
    val membership: ChatMembership = ChatMembership.Active,
    val invitationInviterName: String? = null,
    val isArchived: Boolean = false,
    val isPinned: Boolean = false,
    val unreadCount: Int = 0,
    val isMarkedUnread: Boolean = false,
    val muteDuration: MuteDuration? = null,
    val disappearingDuration: DisappearingDuration = DisappearingDuration.Off,
    val isDraft: Boolean = false,
    val deliveryState: ChatDeliveryState = ChatDeliveryState.None,
    val description: String = "",
    val members: List<GroupMember> = emptyList(),
    val relayUrls: List<String> = emptyList(),
    val defaultRelayUrls: List<String> = relayUrls,
    val timeline: List<ChatTimelineEntry> = emptyList(),
    val draftText: String = "",
    val draftAttachments: List<MessageAttachment> = emptyList(),
    val draftPhotoQuality: PhotoQuality = PhotoQuality.High,
    val suppressedDraftLinkUrl: String? = null,
    val draftReplyMessageId: String? = null,
    val pinnedOrder: Int? = null,
    val readState: ConversationReadState? = null,
    val collapseLongMessages: Boolean = true,
    val messageDeletion: MessageDeleteOperation? = null,
    val groupRoster: GroupRoster = GroupRoster(),
    val publicInviteAvatar: ProfileAvatar = avatar,
    val groupLifecycle: GroupLifecycle = GroupLifecycle.Active,
    val disbandCapability: GroupDisbandCapability = GroupDisbandCapability(),
) {
    val visibleAvatar: ProfileAvatar get() = if (isGroup && membership == ChatMembership.Invited) publicInviteAvatar else avatar

    val isGroup: Boolean
        get() = kind == ChatKind.Group

    val isUnread: Boolean
        get() = unreadCount > 0 || isMarkedUnread

    val hasEndedMembership: Boolean
        get() = membership == ChatMembership.Left || membership == ChatMembership.Removed

    val hasDraft: Boolean
        get() = isDraft || draftText.isNotBlank() || draftAttachments.isNotEmpty() || draftReplyMessageId != null

    val visiblePreview: String
        get() = when (membership) {
            ChatMembership.Invited -> "Invited to chat by ${invitationInviterName ?: "Someone"}"
            ChatMembership.Left -> if (isGroup) "You left this group." else "You left this chat."
            ChatMembership.Removed -> if (isGroup) {
                "You were removed from this group."
            } else {
                "You were removed from this chat."
            }
            ChatMembership.Active -> preview.ifEmpty { attachmentPreview?.label.orEmpty() }
        }

    val displayPreview: String
        get() = ChatListPresentation.from(this).searchableText

    fun isSoleAdmin(profileId: String): Boolean =
        isGroup &&
            members.firstOrNull { it.personId == profileId }?.role == GroupRole.Admin &&
            members.count { it.role == GroupRole.Admin } == 1
}

data class Person(
    val id: String,
    val name: String,
    val publicKey: String = publicKeyFor(id),
    val about: String = "",
    val nostrAddress: String = defaultNostrAddress(name),
    val isNostrAddressVerified: Boolean = false,
    val avatar: ProfileAvatar = ProfileAvatar.Monogram,
    val isFollowing: Boolean = true,
    val isBlocked: Boolean = false,
    val nickname: String = "",
    val privateNotes: String = "",
    val banner: ProfileAvatar? = null,
    val lightningAddress: String = "",
) {
    val displayName: String get() = nickname.ifBlank { name }

    val shortPublicKey: String
        get() = if (publicKey.length <= 17) publicKey else "${publicKey.take(12)}…${publicKey.takeLast(4)}"

    companion object {
        private const val KEY_ALPHABET = "023456789acdefghjklmnpqrstuvwxyz"

        fun publicKeyFor(id: String): String {
            val seed = id.map(Char::code).ifEmpty { listOf(0) }
            val body = buildString {
                repeat(58) { index ->
                    val alphabetIndex = (seed[index % seed.size] + index) % KEY_ALPHABET.length
                    append(KEY_ALPHABET[alphabetIndex])
                }
            }
            return "npub1$body"
        }

        private fun defaultNostrAddress(name: String): String {
            val local = name.lowercase()
                .split(Regex("[^a-z0-9]+"))
                .filter(String::isNotEmpty)
                .joinToString(".")
                .ifEmpty { "profile" }
            return "$local@whitenoise.example"
        }
    }
}

object ChatProjection {
    fun rows(
        chats: List<Chat>,
        scope: ChatScope,
        query: String = "",
    ): List<Chat> {
        val scoped = chats.filter { chat ->
            when (scope) {
                ChatScope.Chats -> !chat.isArchived
                ChatScope.Unread -> !chat.isArchived && chat.isUnread
                ChatScope.Archived -> chat.isArchived
                ChatScope.Left -> !chat.isArchived && chat.hasEndedMembership
            }
        }
        val normalizedQuery = query.normalizedSearchText()
        return scoped
            .filter { chat ->
                normalizedQuery.isEmpty() ||
                    chat.title.normalizedSearchText().contains(normalizedQuery) ||
                    chat.displayPreview.normalizedSearchText().contains(normalizedQuery)
            }
            .sortedWith(ChatOrganization.order)
    }
}

internal fun String.normalizedSearchText(): String = Normalizer
    .normalize(trim().lowercase(), Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")
