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
        publicKey = "npub1m8z7q4k6v2c9r5t3y8p4s7h2d6n9w3x5j8f4u7e2a6k9q8x4k",
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
        publicKey = "npub1p8c4y6m2v9r5t7s3h1d8n4x6j2a9e5u7z3q8w4f6k1m9c5n7",
        nostrAddress = "pebble@whitenoise.example",
        isNostrAddressVerified = true,
        avatar = ProfileAvatar.Asset(AvatarAsset.Pebble),
        people = directory,
        chatRelayUrls = profileRelayUrls,
    )

    val openCircuit = Profile(
        id = "open-circuit",
        name = "Open Circuit",
        publicKey = "npub1f6k3r8w2v9c5m7t4y1p8s6h3d9n2x5j7a4e8u6z3q9k1p7v2",
        nostrAddress = "open-circuit@whitenoise.example",
        avatar = ProfileAvatar.Asset(AvatarAsset.OpenCircuit),
        people = directory,
        chatRelayUrls = profileRelayUrls,
    )

    val showcaseProfiles = listOf(
        Profile(
            id = "open-quill",
            name = "Open Quill",
            publicKey = "npub1q2v9n6t4r7c3x8m5k2w9p6s4y7h3d8f5j2a9e6u4z7n1m2d9",
            nostrAddress = "open-quill@whitenoise.example",
            avatar = ProfileAvatar.Asset(AvatarAsset.OpenQuill),
            people = directory,
            chatRelayUrls = profileRelayUrls,
        ),
        Profile(
            id = "cipher-wheel",
            name = "Cipher Wheel",
            publicKey = "npub1s4h8c2y7v5m9r3t6p1w8d4n7x2j5a9e3u6z8q4k7c2m1f3k8",
            nostrAddress = "cipher-wheel@whitenoise.example",
            avatar = ProfileAvatar.Asset(AvatarAsset.CipherWheel),
            people = directory,
            chatRelayUrls = profileRelayUrls,
        ),
        Profile(
            id = "free-signal",
            name = "Free Signal",
            publicKey = "npub1n7d2p5x9v4c8m3t6y1s7h5k2j9a4e8u3z6q1r7w5f2m9w6r4",
            nostrAddress = "free-signal@whitenoise.example",
            avatar = ProfileAvatar.Asset(AvatarAsset.FreeSignal),
            people = directory,
            chatRelayUrls = profileRelayUrls,
        ),
        Profile(
            id = "public-voice",
            name = "Public Voice",
            publicKey = "npub1c9m4v7q2r8t5y3p6s1h9d4n7x2j5a8e3u6z1k4w7f9m2x9q2",
            nostrAddress = "public-voice@whitenoise.example",
            avatar = ProfileAvatar.Asset(AvatarAsset.PublicVoice),
            people = directory,
            chatRelayUrls = profileRelayUrls,
        ),
        Profile(
            id = "liberty-relay",
            name = "Liberty Relay",
            publicKey = "npub1t3r8k6z2v9c5m7y4p1s8h3d6n9x2j5a7e4u8q6w3f9k1s4m7",
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
