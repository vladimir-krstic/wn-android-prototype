package dev.ipf.whitenoise.model

enum class AvatarAsset {
    Badger,
    WebAionyHaust,
    OpenCircuit,
    Fox,
    WebChristopherCampbell,
    CipherWheel,
    Marmot,
    WebIanDooley,
    Pebble,
    Ostrich,
    WebSergioDePaula,
    OpenQuill,
    Sloth,
    WebAyoOgunseinde,
    FreeSignal,
    GardenClub,
    WebVinceFleming,
    LibertyRelay,
    PublicVoice,
    WebPhilipMartin,
    Marmota,
    MayaChen,
    EliasMoreno,
    MinaPark,
    LeoMartins,
    NoraBennett,
    TheoGrant,
    AishaRahman,
    LenaOrtiz,
    JonahReed,
    TessaMorgan,
    MarcusBell,
    SofiaAlvarez,
    DanielKim,
    Fiatjaf,
    LegacyDavidChaum,
    LegacyEricHughes,
    LegacyHalFinney,
    LegacyJudithMilhon,
    LegacyMarmots,
    LegacyNostrDevs,
    LegacyRadiaPerlman,
    LegacyRichardStallman,
    LegacySatoshiNakamoto,
    LegacyWhitfieldDiffie,
}

sealed interface ProfileAvatar {
    data class Asset(val asset: AvatarAsset) : ProfileAvatar

    data class WebImage(
        val asset: AvatarAsset,
        val choiceId: String,
    ) : ProfileAvatar

    class DeviceImage(val bytes: ByteArray) : ProfileAvatar {
        override fun equals(other: Any?): Boolean =
            other is DeviceImage && bytes.contentEquals(other.bytes)

        override fun hashCode(): Int = bytes.contentHashCode()
    }

    data object Monogram : ProfileAvatar
}

data class Profile(
    val id: String,
    val name: String,
    val publicKey: String,
    val about: String = "",
    val nostrAddress: String = "",
    val isNostrAddressVerified: Boolean = false,
    val avatar: ProfileAvatar = ProfileAvatar.Monogram,
    val people: List<Person> = emptyList(),
    val chats: List<Chat> = emptyList(),
    val chatRelayUrls: List<String> = emptyList(),
    val quickReactions: List<String> = ReactionCatalog.defaults,
    val settings: ProfileSettings = ProfileSettings(),
    val developerTools: DeveloperToolsState = DeveloperToolsState(),
    val diagnostics: DiagnosticsState = DiagnosticsState(),
    val signingMode: ProfileSigningMode = ProfileSigningMode.LocalKey,
    val localKeyAvailable: Boolean = true,
    val connectionInformationPublished: Boolean = true,
    val banner: ProfileAvatar? = null,
    val lightningAddress: String = "",
    val chatFolders: List<ChatFolder> = emptyList(),
    val chatConnection: ChatConnectionState = ChatConnectionState(),
) {
    val initial: String
        get() = name.trim().firstOrNull()?.uppercase() ?: "?"

    val shortPublicKey: String
        get() = if (publicKey.length <= 17) {
            publicKey
        } else {
            "${publicKey.take(12)}…${publicKey.takeLast(4)}"
        }

    fun updateEditableValues(from: Profile): Profile {
        if (id != from.id) return this
        return copy(
            name = from.name,
            about = from.about,
            avatar = from.avatar,
            diagnostics = diagnostics.copy(
                records = diagnostics.records.map { it.copy(profileName = from.name) },
            ),
        )
    }
}
