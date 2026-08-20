import Foundation

enum PrototypeAvatar: Equatable {
    case asset(String)
    case webImage(assetName: String, choiceID: String)
    case imageData(Data)
    case monogram
}

struct PrototypeProfile: Identifiable, Equatable {
    let id: String
    var name: String
    let publicKey: String
    var about: String
    var nostrAddress: String
    var isNostrAddressVerified: Bool
    var lightningAddress: String
    var avatar: PrototypeAvatar
    var people: [PrototypePerson]
    var chats: [PrototypeChat]
    var quickReactionEmoji: [String]
    var relayConfiguration: PrototypeRelayConfiguration
    var developerTools: PrototypeDeveloperToolsState

    init(
        id: String,
        name: String,
        publicKey: String,
        about: String = "",
        nostrAddress: String = "",
        isNostrAddressVerified: Bool = false,
        lightningAddress: String = "",
        avatar: PrototypeAvatar = .monogram,
        people: [PrototypePerson] = PrototypeChatFixtures.people(),
        chats: [PrototypeChat],
        quickReactionEmoji: [String] = PrototypeReaction.defaultQuickEmoji,
        relayConfiguration: PrototypeRelayConfiguration = .fixtures,
        developerTools: PrototypeDeveloperToolsState? = nil
    ) {
        self.id = id
        self.name = name
        self.publicKey = publicKey
        self.about = about
        self.nostrAddress = nostrAddress.isEmpty
            ? PrototypeNostrAddress.defaultValue(for: name)
            : PrototypeNostrAddress.normalized(nostrAddress)
        self.isNostrAddressVerified = isNostrAddressVerified
            && PrototypeNostrAddress.isValid(self.nostrAddress)
        self.lightningAddress = lightningAddress
        self.avatar = avatar
        self.people = people
        self.chats = chats
        self.quickReactionEmoji = quickReactionEmoji
        self.relayConfiguration = relayConfiguration
        self.developerTools = developerTools ?? .fixtures(
            profileID: id,
            profileName: name
        )
    }

    var shortPublicKey: String {
        guard publicKey.count > 17 else {
            return publicKey
        }

        return "\(publicKey.prefix(12))…\(publicKey.suffix(4))"
    }

    var initial: String {
        name
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .first
            .map { String($0).uppercased() }
            ?? "?"
    }

    var unreadCount: Int {
        chats.reduce(into: 0) { count, chat in
            let row = chat.row(people: people, currentProfileID: id)
            guard !row.isArchived, row.isUnread else {
                return
            }

            count += max(row.unreadCount, 1)
        }
    }

    mutating func updateEditableValues(from profile: PrototypeProfile) {
        guard id == profile.id else {
            return
        }

        name = profile.name
        about = profile.about
        avatar = profile.avatar
    }
}

extension PrototypeProfile {
    static let marmotaID = "marmota"

    static let marmota = PrototypeProfile(
        id: marmotaID,
        name: "Marmota",
        publicKey: "npub1m8z7q4k6v2c9r5t3y8p4s7h2d6n9w3x5j8f4u7e2a6k9q8x4k",
        about: "Quietly making plans and sending good links.",
        nostrAddress: "marmota@whitenoise.example",
        isNostrAddressVerified: true,
        lightningAddress: "marmota@pay.example",
        avatar: .asset("ProfileAvatarMarmota"),
        chats: PrototypeChatFixtures.chats(
            profileID: marmotaID,
            relayURLs: PrototypeRelayConfiguration.fixtures
                .availableChatMessageRelayURLs
        )
    )

    static let openQuill = PrototypeProfile(
        id: "open-quill",
        name: "Open Quill",
        publicKey: "npub1q2v9n6t4r7c3x8m5k2w9p6s4y7h3d8f5j2a9e6u4z7n1m2d9",
        avatar: .asset("ProfileAvatarOpenQuill"),
        chats: []
    )

    static let openCircuit = PrototypeProfile(
        id: "open-circuit",
        name: "Open Circuit",
        publicKey: "npub1f6k3r8w2v9c5m7t4y1p8s6h3d9n2x5j7a4e8u6z3q9k1p7v2",
        avatar: .asset("ProfileAvatarOpenCircuit"),
        chats: []
    )

    static let cipherWheel = PrototypeProfile(
        id: "cipher-wheel",
        name: "Cipher Wheel",
        publicKey: "npub1s4h8c2y7v5m9r3t6p1w8d4n7x2j5a9e3u6z8q4k7c2m1f3k8",
        avatar: .asset("ProfileAvatarCipherWheel"),
        chats: []
    )

    static let freeSignal = PrototypeProfile(
        id: "free-signal",
        name: "Free Signal",
        publicKey: "npub1n7d2p5x9v4c8m3t6y1s7h5k2j9a4e8u3z6q1r7w5f2m9w6r4",
        avatar: .asset("ProfileAvatarFreeSignal"),
        chats: []
    )

    static let publicVoice = PrototypeProfile(
        id: "public-voice",
        name: "Public Voice",
        publicKey: "npub1c9m4v7q2r8t5y3p6s1h9d4n7x2j5a8e3u6z1k4w7f9m2x9q2",
        avatar: .asset("ProfileAvatarPublicVoice"),
        chats: []
    )

    static let libertyRelay = PrototypeProfile(
        id: "liberty-relay",
        name: "Liberty Relay",
        publicKey: "npub1t3r8k6z2v9c5m7y4p1s8h3d6n9x2j5a7e4u8q6w3f9k1s4m7",
        avatar: .asset("ProfileAvatarLibertyRelay"),
        chats: []
    )

    static let pebble = PrototypeProfile(
        id: "pebble",
        name: "Pebble",
        publicKey: "npub1p8c4y6m2v9r5t7s3h1d8n4x6j2a9e5u7z3q8w4f6k1m9c5n7",
        nostrAddress: "pebble@whitenoise.example",
        isNostrAddressVerified: true,
        avatar: .asset("ProfileAvatarPebble"),
        chats: []
    )

    static func initialSignUp(
        name: String,
        about: String = "",
        avatar: PrototypeAvatar? = nil
    ) -> PrototypeProfile {
        var profile = marmota
        let normalizedName = name.trimmingCharacters(
            in: .whitespacesAndNewlines
        )

        if !normalizedName.isEmpty {
            profile.name = normalizedName
            profile.developerTools = .fixtures(
                profileID: profile.id,
                profileName: normalizedName
            )
        }

        profile.about = about

        if let avatar {
            profile.avatar = avatar
        }

        return profile
    }

    static func addedSignUp(
        name: String,
        about: String = "",
        avatar: PrototypeAvatar? = nil
    ) -> PrototypeProfile {
        var profile = pebble
        let normalizedName = name.trimmingCharacters(
            in: .whitespacesAndNewlines
        )

        if !normalizedName.isEmpty {
            profile.name = normalizedName
            profile.developerTools = .fixtures(
                profileID: profile.id,
                profileName: normalizedName
            )
        }

        profile.about = about

        if let avatar {
            profile.avatar = avatar
        }

        return profile
    }

    static let initialProfiles: [PrototypeProfile] = [
        .marmota,
    ]

    static let showcasePseudonyms: [PrototypeProfile] = [
        .openQuill,
        .cipherWheel,
        .freeSignal,
        .publicVoice,
        .libertyRelay,
    ]

    static let multipleProfileFixtures: [PrototypeProfile] = [
        .marmota,
    ] + showcasePseudonyms

    static let postAddProfileFixtures: [PrototypeProfile] = [
        .pebble,
        .marmota,
    ] + showcasePseudonyms
}
