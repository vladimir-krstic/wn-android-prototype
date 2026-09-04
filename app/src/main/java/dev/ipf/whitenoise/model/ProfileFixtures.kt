package dev.ipf.whitenoise.model

object ProfileFixtures {
    const val MARMOTA_ID = "marmota"
    const val PEBBLE_ID = "pebble"
    private val directory by lazy(ChatFixtures::people)
    private val relayUrls = listOf(ChatFixtures.DEFAULT_CHAT_RELAY)
    private val profileRelayUrls = ProfileRelayFixtures.chatMessageUrls(ProfileRelayFixtures.defaults)

    val marmota = Profile(
        id = MARMOTA_ID,
        name = "Marmota",
        publicKey = Person.publicKeyFor(MARMOTA_ID),
        about = "Quietly making plans and sending good links.",
        nostrAddress = "marmota@whitenoise.example",
        isNostrAddressVerified = true,
        avatar = ProfileAvatar.Asset(AvatarAsset.Marmota),
        people = directory,
        chats = ChatFixtures.populatedChats(MARMOTA_ID, relayUrls),
        chatRelayUrls = profileRelayUrls,
    )

    val pebble = Profile(
        id = PEBBLE_ID,
        name = "Pebble",
        publicKey = Person.publicKeyFor(PEBBLE_ID),
        nostrAddress = "pebble@whitenoise.example",
        isNostrAddressVerified = true,
        avatar = ProfileAvatar.Asset(AvatarAsset.Pebble),
        people = directory,
        chatRelayUrls = profileRelayUrls,
    )

    val openCircuit = Profile(
        id = "open-circuit",
        name = "Open Circuit",
        publicKey = Person.publicKeyFor("open-circuit"),
        nostrAddress = "open-circuit@whitenoise.example",
        avatar = ProfileAvatar.Asset(AvatarAsset.OpenCircuit),
        people = directory,
        chatRelayUrls = profileRelayUrls,
    )

    val showcaseProfiles = listOf(
        Profile(
            id = "open-quill",
            name = "Open Quill",
            publicKey = Person.publicKeyFor("open-quill"),
            nostrAddress = "open-quill@whitenoise.example",
            avatar = ProfileAvatar.Asset(AvatarAsset.OpenQuill),
            people = directory,
            chatRelayUrls = profileRelayUrls,
        ),
        Profile(
            id = "cipher-wheel",
            name = "Cipher Wheel",
            publicKey = Person.publicKeyFor("cipher-wheel"),
            nostrAddress = "cipher-wheel@whitenoise.example",
            avatar = ProfileAvatar.Asset(AvatarAsset.CipherWheel),
            people = directory,
            chatRelayUrls = profileRelayUrls,
        ),
        Profile(
            id = "free-signal",
            name = "Free Signal",
            publicKey = Person.publicKeyFor("free-signal"),
            nostrAddress = "free-signal@whitenoise.example",
            avatar = ProfileAvatar.Asset(AvatarAsset.FreeSignal),
            people = directory,
            chatRelayUrls = profileRelayUrls,
        ),
        Profile(
            id = "public-voice",
            name = "Public Voice",
            publicKey = Person.publicKeyFor("public-voice"),
            nostrAddress = "public-voice@whitenoise.example",
            avatar = ProfileAvatar.Asset(AvatarAsset.PublicVoice),
            people = directory,
            chatRelayUrls = profileRelayUrls,
        ),
        Profile(
            id = "liberty-relay",
            name = "Liberty Relay",
            publicKey = Person.publicKeyFor("liberty-relay"),
            nostrAddress = "liberty-relay@whitenoise.example",
            avatar = ProfileAvatar.Asset(AvatarAsset.LibertyRelay),
            people = directory,
            chatRelayUrls = profileRelayUrls,
        ),
    )

    fun initialSignUp(
        name: String,
        about: String,
        avatar: ProfileAvatar?,
    ): Profile = editableCopy(marmota, name, about, avatar)

    fun addedSignUp(
        name: String,
        about: String,
        avatar: ProfileAvatar?,
    ): Profile = editableCopy(pebble, name, about, avatar)

    private fun editableCopy(
        canonical: Profile,
        name: String,
        about: String,
        avatar: ProfileAvatar?,
    ): Profile {
        val normalizedName = name.trim()
        return canonical.copy(
            name = normalizedName.ifEmpty { canonical.name },
            about = about,
            avatar = avatar ?: canonical.avatar,
        )
    }
}
