package dev.ipf.whitenoise.model

/** Local developer-only inspection data; never a transport, credential or logging sink. */
data class DeveloperHealth(
    val total: Int, val connected: Int, val connecting: Int, val disconnected: Int,
    val attempts: Int, val successes: Int, val profiles: Int, val bootstrapRelays: Int,
)

enum class PackageInventoryExample { Published, Retained, Empty, RelayOnly, Mixed }

enum class DeveloperOutcome { Success, Failure, Partial, Unavailable }
enum class DeveloperOperation { RefreshPackages, Republish, PublishNew, DeletePackage, RefreshHealth, SendToSelf, RefreshPush }
enum class DeveloperPhase { Confirm, Running, Complete, Partial, Failed, Unavailable }

data class DeveloperWork(
    val id: Long,
    val profileId: String,
    val surface: String,
    val operation: DeveloperOperation,
    val outcome: DeveloperOutcome,
    val phase: DeveloperPhase,
    val candidate: KeyPackage? = null,
    val targetId: String? = null,
    val revision: Int = 0,
    val publishedBefore: Boolean = true,
    val relayUrls: List<String> = emptyList(),
)

data class PushMemberDebug(
    val memberId: String, val leaf: Int, val platform: String,
    val fingerprint: String, val serverKey: String, val relayHint: Boolean,
    val activeLeaf: Boolean, val matchesLeaf: Boolean, val localMember: Boolean,
    val updatedAt: String,
)

object DeveloperInspection {
    const val PerformanceDurationMillis = 30 * 60 * 1000L
    val requiredComponents = listOf("MLS group state", "Group relays", "Message subscription", "Push token list")
    fun packages(profile: Profile): List<KeyPackage> = (profile.developerTools.keyPackages ?: listOf(
        profile.developerTools.keyPackage.copy(relays = profile.settings.relays.map { it.url }.distinct()),
    )).map { if (profile.connectionInformationPublished) it else it.copy(relays = emptyList()) }.filter { it.local || it.relays.isNotEmpty() }
    fun health(profile: Profile, profileCount: Int): DeveloperHealth {
        val count = profile.settings.relays.map { it.url }.distinct().size
        val connected = if (profile.chatConnection.phase == ChatConnectionPhase.Online) count else 0
        val connecting = if (profile.chatConnection.phase == ChatConnectionPhase.Connecting) count else 0
        return DeveloperHealth(count, connected, connecting, count - connected - connecting, count * 2, connected, profileCount, count)
    }
    fun streamDetails(message: ChatMessage): String? = if (message.deliveryState == MessageDeliveryState.Streaming) {
        "Stream signaling · Receiving chunks\nRecord ${message.id}\nStatus: streaming · Display only"
    } else null
}
