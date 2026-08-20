package dev.ipf.whitenoise.model

data class AuditFile(
    val id: String,
    val filename: String,
    val byteCount: Int,
    val createdLabel: String,
    val profileName: String,
)

data class KeyPackage(
    val id: String,
    val published: String,
    val size: String,
) {
    companion object {
        val Fixture = KeyPackage("a17c2e93d8f4b1", "Today at 18:05", "4 KB")
        val PublishedFixture = KeyPackage("d48e1a7c9b320f", "Just now", "4 KB")
    }
}

data class DiagnosticEvent(
    val id: String,
    val text: String,
)

data class DeveloperToolsState(
    val isEnabled: Boolean = false,
    val debugMode: Boolean = false,
    val anonymousTelemetry: Boolean = false,
    val auditLogging: Boolean = false,
    val auditFiles: List<AuditFile> = emptyList(),
    val keyPackage: KeyPackage = KeyPackage.Fixture,
    val diagnosticEvents: List<DiagnosticEvent> = defaultDiagnosticEvents,
) {
    val isConversationDebugEnabled: Boolean
        get() = isEnabled && debugMode

    val auditLogsContainData: Boolean
        get() = auditFiles.any { it.byteCount > 0 }

    fun withEnabled(enabled: Boolean): DeveloperToolsState = if (enabled) {
        copy(isEnabled = true)
    } else {
        copy(
            isEnabled = false,
            debugMode = false,
            anonymousTelemetry = false,
            auditLogging = false,
        )
    }

    companion object {
        private val defaultDiagnosticEvents = listOf(
            DiagnosticEvent("runtime", "18:42:10  runtime started"),
            DiagnosticEvent("relay", "18:42:11  relay connected"),
            DiagnosticEvent("projection", "18:42:12  profile projection ready"),
        )

        fun fixtures(profileId: String, profileName: String): DeveloperToolsState = DeveloperToolsState(
            auditFiles = listOf(
                AuditFile(
                    id = "audit-$profileId-01",
                    filename = "audit-$profileId-20260806-01.jsonl",
                    byteCount = 24_000,
                    createdLabel = "Aug 6, 2026, 6:47 PM",
                    profileName = profileName,
                ),
                AuditFile(
                    id = "audit-$profileId-02",
                    filename = "audit-$profileId-20260805-01.jsonl",
                    byteCount = 8_000,
                    createdLabel = "Aug 5, 2026, 5:47 PM",
                    profileName = profileName,
                ),
            ),
        )
    }
}

enum class ConversationDebugAccess {
    Unavailable,
    Disabled,
    Enabled,
}

data class ConversationPushDebugInfo(
    val notificationsEnabled: Boolean,
    val registrationStatus: String,
    val staleTokenCount: Int,
    val missingRelayHintCount: Int,
)

data class ConversationDebugSnapshot(
    val chatId: String,
    val lifecycle: String,
    val memberCount: Int?,
    val adminCount: Int?,
    val epoch: Int,
    val currentRole: String?,
    val requiredEventKinds: List<Int>,
    val mlsGroupId: String,
    val nostrGroupId: String,
    val relayCount: Int,
    val push: ConversationPushDebugInfo,
) {
    val diagnosticSummary: String
        get() = buildList {
            add("Chat Diagnostic Summary")
            add("State: $lifecycle")
            add("Epoch: $epoch")
            add("Chat Relays: $relayCount")
            add("Notifications: ${if (push.notificationsEnabled) "On" else "Off"}")
            add("Push: ${push.registrationStatus}")
            memberCount?.let { add("MLS Members: $it") }
            adminCount?.let { add("Admins: $it") }
            currentRole?.let { add("Your Role: $it") }
            if (push.staleTokenCount > 0) add("Stale Push Tokens: ${push.staleTokenCount}")
            if (push.missingRelayHintCount > 0) add("Missing Relay Hints: ${push.missingRelayHintCount}")
            add("Required Event Kinds: ${requiredEventKinds.joinToString()}")
        }.joinToString("\n")
}

object ConversationDebugPolicy {
    private val requiredKinds = listOf(32769, 32771, 32772, 32774, 32777, 32779, 32780)
    private val toolbarChatIds = setOf("fiatjaf", ChatFixtures.SUPPORT_CHAT_ID)

    fun showsToolbarAction(profile: Profile, chatId: String): Boolean =
        profile.developerTools.isConversationDebugEnabled && chatId in toolbarChatIds

    fun access(profile: Profile, chatId: String): ConversationDebugAccess = when {
        profile.chats.none { it.id == chatId } -> ConversationDebugAccess.Unavailable
        !profile.developerTools.isConversationDebugEnabled -> ConversationDebugAccess.Disabled
        else -> ConversationDebugAccess.Enabled
    }

    fun snapshot(profile: Profile, chatId: String): ConversationDebugSnapshot? {
        val chat = profile.chats.firstOrNull { it.id == chatId } ?: return null
        val participantCount = when (chat.kind) {
            is ChatKind.Direct -> 2
            ChatKind.Group -> (chat.members.map(GroupMember::personId) + profile.id).distinct().size
        }
        val canRegister = chat.membership == ChatMembership.Active &&
            chat.relayUrls.isNotEmpty() && profile.settings.nativePushNotifications
        val seed = stableNumber(chat.id)
        return ConversationDebugSnapshot(
            chatId = chat.id,
            lifecycle = when (chat.membership) {
                ChatMembership.Invited -> "Invitation Pending"
                ChatMembership.Active -> "Active"
                ChatMembership.Left -> "Left"
                ChatMembership.Removed -> "Removed"
            },
            memberCount = chat.members.size.takeIf { chat.isGroup },
            adminCount = chat.members.count { it.role == GroupRole.Admin }.takeIf { chat.isGroup },
            epoch = maxOf(1, seed % 24),
            currentRole = if (chat.isGroup) currentRole(chat, profile.id) else null,
            requiredEventKinds = requiredKinds,
            mlsGroupId = "mls-${chat.id}-$seed",
            nostrGroupId = "nostr-${chat.id}-${seed + 41}",
            relayCount = chat.relayUrls.size,
            push = ConversationPushDebugInfo(
                notificationsEnabled = profile.settings.nativePushNotifications,
                registrationStatus = if (canRegister) "Registered" else "Not Registered",
                staleTokenCount = if (chat.id == "weekend-walks") 1 else 0,
                missingRelayHintCount = if (chat.relayUrls.isEmpty()) participantCount else 0,
            ),
        )
    }

    private fun currentRole(chat: Chat, profileId: String): String = when (chat.membership) {
        ChatMembership.Invited -> "Invited"
        ChatMembership.Left, ChatMembership.Removed -> "Former Member"
        ChatMembership.Active -> when (chat.members.firstOrNull { it.personId == profileId }?.role) {
            GroupRole.Admin -> "Admin"
            GroupRole.Member -> "Member"
            null -> "Not a Member"
        }
    }

    private fun stableNumber(value: String): Int = value.encodeToByteArray().withIndex().fold(17) { result, item ->
        (result * 31 + item.value.toInt() + item.index) % 9_973
    }
}

enum class ProfileExitDestination {
    ProfileSwitcher,
    Welcome,
}

object WipeConfirmationPhrase {
    private val words = listOf(
        "anchor", "apple", "bridge", "cactus", "harbor", "kitten",
        "maple", "planet", "river", "window", "yellow", "zebra",
    )

    fun make(profileIds: Collection<String>): String {
        val source = profileIds.sorted().joinToString("|")
        val seed = source.withIndex().fold(0) { result, item ->
            (result + ((item.index + 1) * item.value.code)) % 1_000_003
        }
        val indexes = mutableListOf(
            seed % words.size,
            ((seed / 7) + 3) % words.size,
            ((seed / 17) + 7) % words.size,
        )
        for (index in 1 until indexes.size) {
            while (indexes.take(index).contains(indexes[index])) {
                indexes[index] = (indexes[index] + 1) % words.size
            }
        }
        return indexes.joinToString(" ") { words[it] }
    }

    fun matches(input: String, expected: String): Boolean = input.trim() == expected
}
