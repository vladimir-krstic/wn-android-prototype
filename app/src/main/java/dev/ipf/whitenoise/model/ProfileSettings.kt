package dev.ipf.whitenoise.model

enum class AppearancePreference(val label: String) {
    System("System default"),
    Light("Light"),
    Dark("Dark"),
}

enum class LanguagePreference(val label: String) {
    System("System default"),
    English("English"),
    German("German"),
    Spanish("Spanish"),
    French("French"),
    Italian("Italian"),
    Portuguese("Portuguese"),
    Serbian("Serbian"),
}

enum class NotificationPreviewMode(val label: String) {
    SenderAndMessage("Sender and message"),
    SenderOnly("Sender only"),
    Generic("New message only"),
    ;

    val example: String
        get() = when (this) {
            SenderAndMessage -> "Maya Chen · Can you send the latest version?"
            SenderOnly -> "Maya Chen · New message"
            Generic -> "White Noise · New message"
        }
}

enum class SentMediaQuality(val label: String, val photoQuality: PhotoQuality, val voiceBitrate: Int) {
    Low("Low", PhotoQuality.Low, 32_000),
    Standard("Standard", PhotoQuality.Standard, 64_000),
    High("High", PhotoQuality.High, 96_000),
    Original("Original", PhotoQuality.Original, 96_000),
}

enum class AutoLockDuration(val label: String, val delayMillis: Long) {
    Immediately("Immediately", 0L),
    OneMinute("After 1 minute", 60_000L),
    FiveMinutes("After 5 minutes", 5 * 60_000L),
    FifteenMinutes("After 15 minutes", 15 * 60_000L),
    ThirtyMinutes("After 30 minutes", 30 * 60_000L),
}

enum class RelayRole(val label: String) {
    Profile("Profile"),
    Inbox("Inbox"),
    ChatMessages("Chat Messages"),
}

enum class RelayConnectionStatus(val label: String) {
    Connected("Connected"),
    Reconnecting("Reconnecting"),
    Disconnected("Disconnected"),
}

enum class RelayRoleAvailability(val label: String) {
    Available("Available"),
    Reconnecting("Reconnecting"),
    Disconnected("Disconnected"),
    Unassigned("Unassigned"),
}

data class ProfileRelay(
    val id: String,
    val name: String,
    val url: String,
    val status: RelayConnectionStatus,
    val isReadOnly: Boolean = false,
    val roles: Set<RelayRole> = emptySet(),
)

data class ProfileSettings(
    val localNotifications: Boolean = true,
    val nativePushNotifications: Boolean = true,
    val notificationPreviewMode: NotificationPreviewMode = NotificationPreviewMode.Generic,
    val appearance: AppearancePreference = AppearancePreference.System,
    val language: LanguagePreference = LanguagePreference.System,
    val hideScreenInRecents: Boolean = false,
    val blockScreenshotsInChats: Boolean = false,
    val requireDeviceAuthentication: Boolean = false,
    val incognitoKeyboard: Boolean = false,
    val autoLockDuration: AutoLockDuration = AutoLockDuration.Immediately,
    val downloadMatrix: MediaDownloadMatrix = MediaDownloadMatrix(),
    val automaticDownloadsPaused: Boolean = false,
    val sentMediaQuality: SentMediaQuality = SentMediaQuality.High,
    val relays: List<ProfileRelay> = ProfileRelayFixtures.defaults,
    val speech: SpeechPreferences = SpeechPreferences(),
    val dictation: DictationPreferences = DictationPreferences(),
)

object ProfileRelayFixtures {
    const val DEFAULT_CHAT_RELAY_ID = "primal"

    val defaults = listOf(
        ProfileRelay(
            id = DEFAULT_CHAT_RELAY_ID,
            name = "Primal",
            url = ChatFixtures.DEFAULT_CHAT_RELAY,
            status = RelayConnectionStatus.Connected,
            roles = setOf(RelayRole.Profile, RelayRole.Inbox, RelayRole.ChatMessages),
        ),
        ProfileRelay(
            id = "damus",
            name = "Damus",
            url = "wss://relay.damus.io",
            status = RelayConnectionStatus.Connected,
            roles = setOf(RelayRole.Profile, RelayRole.ChatMessages),
        ),
        ProfileRelay(
            id = "nos-lol",
            name = "nos.lol",
            url = "wss://nos.lol",
            status = RelayConnectionStatus.Connected,
            roles = setOf(RelayRole.Profile, RelayRole.Inbox, RelayRole.ChatMessages),
        ),
        ProfileRelay(
            id = "nostr-band",
            name = "Nostr.Band",
            url = "wss://relay.nostr.band",
            status = RelayConnectionStatus.Connected,
            roles = setOf(RelayRole.Profile),
        ),
        ProfileRelay(
            id = "vertex",
            name = "Vertex",
            url = "wss://relay.vertexlab.io",
            status = RelayConnectionStatus.Connected,
            isReadOnly = true,
            roles = emptySet(),
        ),
        ProfileRelay(
            id = "white-noise-profile",
            name = "White Noise Profile",
            url = "wss://relay.whitenoise.chat",
            status = RelayConnectionStatus.Reconnecting,
            roles = setOf(RelayRole.Profile, RelayRole.ChatMessages),
        ),
        ProfileRelay(
            id = "white-noise-inbox",
            name = "White Noise Inbox",
            url = "wss://inbox.whitenoise.chat",
            status = RelayConnectionStatus.Disconnected,
            roles = setOf(RelayRole.Inbox),
        ),
    )

    fun normalize(value: String): String? = ChatRelayPolicy.normalize(value)

    fun importedAddressNeedsAttention(relay: ProfileRelay): Boolean = normalize(relay.url) == null
    fun importedAddressesNeedingAttention(relays: List<ProfileRelay>): List<ProfileRelay> =
        relays.filter(::importedAddressNeedsAttention)

    fun add(
        relays: List<ProfileRelay>,
        value: String,
        roles: Set<RelayRole> = RelayRole.entries.toSet(),
    ): List<ProfileRelay>? {
        val normalized = normalize(value) ?: return null
        if (roles.isEmpty() || relays.any { normalize(it.url) == normalized }) return null
        val slug = normalized.removePrefix("wss://").replace(Regex("[^a-zA-Z0-9]+"), "-").trim('-')
        val existingIds = relays.mapTo(mutableSetOf(), ProfileRelay::id)
        val baseId = "custom-$slug"
        var id = baseId
        var suffix = 2
        while (id in existingIds) {
            id = "$baseId-${suffix++}"
        }
        return relays + ProfileRelay(
            id = id,
            name = normalized.removePrefix("wss://"),
            url = normalized,
            status = RelayConnectionStatus.Reconnecting,
            roles = roles,
        )
    }

    fun chatMessageUrls(relays: List<ProfileRelay>): List<String> = relays
        .filter {
            !it.isReadOnly && RelayRole.ChatMessages in it.roles &&
                it.status == RelayConnectionStatus.Connected
        }
        .map(ProfileRelay::url)

    fun availability(relays: List<ProfileRelay>, role: RelayRole): RelayRoleAvailability {
        val assigned = relays.filter { !it.isReadOnly && role in it.roles }
        return when {
            assigned.isEmpty() -> RelayRoleAvailability.Unassigned
            assigned.any { it.status == RelayConnectionStatus.Connected } -> RelayRoleAvailability.Available
            assigned.any { it.status == RelayConnectionStatus.Reconnecting } -> RelayRoleAvailability.Reconnecting
            else -> RelayRoleAvailability.Disconnected
        }
    }

    fun unavailableRoles(relays: List<ProfileRelay>): Map<RelayRole, RelayRoleAvailability> =
        RelayRole.entries.associateWith { availability(relays, it) }
            .filterValues { it != RelayRoleAvailability.Available }

    fun recoverySummary(relays: List<ProfileRelay>): String? {
        val unavailable = unavailableRoles(relays)
        if (unavailable.isEmpty()) return null
        val causes = listOf(
            RelayRoleAvailability.Unassigned to "Choose a relay for",
            RelayRoleAvailability.Disconnected to "No connected relay for",
            RelayRoleAvailability.Reconnecting to "Relays are reconnecting for",
        ).mapNotNull { (availability, prefix) ->
            val names = RelayRole.entries.filter { unavailable[it] == availability }.joinToString { it.label }
            names.takeIf(String::isNotEmpty)?.let { "$prefix $it." }
        }
        val impacts = buildList {
            if (RelayRole.Profile in unavailable) add("publishing")
            if (RelayRole.Inbox in unavailable) add("invitations")
            if (RelayRole.ChatMessages in unavailable) add("new chats")
        }
        return (causes + "Unavailable: ${impacts.joinToString()}.").joinToString(" ")
    }
}

object ProfileKeyFixtures {
    val PRIVATE_KEY: String = LoginPrototypeData.privateKey

    fun rawExport(profile: Profile): String = buildString {
        appendLine("White Noise key export")
        appendLine("Profile: ${profile.name}")
        appendLine("Public key: ${profile.publicKey}")
        appendLine("Private key: $PRIVATE_KEY")
    }

    fun encryptedExport(profile: Profile, password: String): String = buildString {
        appendLine("WHITE NOISE PROTECTED KEY PACKAGE")
        appendLine("Profile: ${profile.name}")
        appendLine("Public key: ${profile.publicKey}")
        appendLine("Password check: ${password.length} characters")
    }
}

enum class ExportPasswordStrength(
    val label: String,
    val completedSteps: Int,
) {
    Low("Low", 1),
    Fair("Fair", 2),
    Strong("Strong", 3),
}

object ProfileSettingsPolicy {
    fun isValidNostrAddress(value: String): Boolean {
        val trimmed = value.trim()
        val parts = trimmed.split('@')
        return parts.size == 2 && parts.all { it.isNotBlank() } && trimmed.none(Char::isWhitespace)
    }

    fun isValidExportPassword(password: String, confirmation: String): Boolean =
        password.length >= 8 && password == confirmation

    /** Mirrors the pinned iOS export-strength projection without changing export validity. */
    fun exportPasswordStrength(password: String): ExportPasswordStrength? {
        if (password.isEmpty()) return null
        if (password.length < 12) return ExportPasswordStrength.Low

        val isStrong = password.length >= 16 &&
            password.any(Char::isLetter) &&
            password.any(Char::isDigit) &&
            password.any { !it.isLetterOrDigit() }
        return if (isStrong) ExportPasswordStrength.Strong else ExportPasswordStrength.Fair
    }

    fun hasChatMessageRelay(settings: ProfileSettings): Boolean =
        ProfileRelayFixtures.availability(settings.relays, RelayRole.ChatMessages) ==
            RelayRoleAvailability.Available

    fun canPublishProfile(settings: ProfileSettings): Boolean =
        ProfileRelayFixtures.availability(settings.relays, RelayRole.Profile) ==
            RelayRoleAvailability.Available
}
