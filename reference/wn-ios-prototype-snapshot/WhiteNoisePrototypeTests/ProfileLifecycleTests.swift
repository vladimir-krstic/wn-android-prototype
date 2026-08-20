import Testing
@testable import WhiteNoisePrototype

@Suite("Profile lifecycle")
struct ProfileLifecycleTests {
    @Test("Initial Sign Up restores Marmota without borrowing Pebble's identity")
    func initialSignUpUsesMarmotaIdentity() {
        let profile = PrototypeProfile.initialSignUp(name: "Marmota")

        #expect(profile.id == PrototypeProfile.marmota.id)
        #expect(profile.name == "Marmota")
        #expect(profile.avatar == .asset("ProfileAvatarMarmota"))
        #expect(profile.nostrAddress == "marmota@whitenoise.example")
        #expect(profile.isNostrAddressVerified)
    }

    @Test("Add Profile Sign Up uses a distinct Pebble identity")
    func addedSignUpUsesPebbleIdentity() {
        let initialProfile = PrototypeProfile.initialSignUp(name: "Marmota")
        let addedProfile = PrototypeProfile.addedSignUp(name: "Pebble")

        #expect(addedProfile.id == PrototypeProfile.pebble.id)
        #expect(addedProfile.id != initialProfile.id)
        #expect(addedProfile.name == "Pebble")
        #expect(addedProfile.avatar == .asset("ProfileAvatarPebble"))
        #expect(addedProfile.nostrAddress == "pebble@whitenoise.example")
        #expect(addedProfile.isNostrAddressVerified)
    }

    @Test("Quick reactions are independent between profiles")
    func quickReactionsAreProfileScoped() {
        var marmota = PrototypeProfile.marmota
        let pebble = PrototypeProfile.pebble

        marmota.quickReactionEmoji[0] = "🥳"

        #expect(marmota.quickReactionEmoji[0] == "🥳")
        #expect(pebble.quickReactionEmoji == PrototypeReaction.defaultQuickEmoji)
    }

    @Test("Sign Up carries About and an explicit avatar into the profile")
    func signUpCarriesEditableProfileValues() {
        let avatar = PrototypeAvatar.webImage(
            assetName: "AvatarWebAionyHaust",
            choiceID: "3TLl_97HNJo"
        )
        let profile = PrototypeProfile.initialSignUp(
            name: "  River  ",
            about: "Available for quiet conversations.",
            avatar: avatar
        )

        #expect(profile.name == "River")
        #expect(profile.about == "Available for quiet conversations.")
        #expect(profile.nostrAddress == "marmota@whitenoise.example")
        #expect(profile.isNostrAddressVerified)
        #expect(profile.avatar == avatar)
    }

    @Test("Verified Nostr Address accepts an email-shaped value")
    func nostrAddressValidation() {
        #expect(PrototypeNostrAddress.isValid("marmota@whitenoise.example"))
        #expect(!PrototypeNostrAddress.isValid("marmota"))
        #expect(!PrototypeNostrAddress.isValid("marmota@localhost"))
        #expect(!PrototypeNostrAddress.isValid("@whitenoise.example"))
    }

    @Test("Restoring the stored verified address restores verification")
    func restoringVerifiedNostrAddress() {
        let storedAddress = "marmota@whitenoise.example"

        #expect(
            PrototypeNostrAddress.isVerifiedDraft(
                "  marmota@whitenoise.example  ",
                matching: storedAddress,
                storedIsVerified: true
            )
        )
        #expect(
            !PrototypeNostrAddress.isVerifiedDraft(
                "river@example.com",
                matching: storedAddress,
                storedIsVerified: true
            )
        )
        #expect(
            !PrototypeNostrAddress.isVerifiedDraft(
                storedAddress,
                matching: storedAddress,
                storedIsVerified: false
            )
        )
    }

    @Test("Re-onboarding updates editable values without replacing local data")
    func reactivationUpdatesOnlyEditableProfileValues() throws {
        var stored = PrototypeProfile.marmota
        let supportIndex = try #require(stored.chats.firstIndex {
            $0.id == ChatListFixtures.supportChatID
        })
        stored.chats[supportIndex].appendMessage(
            authorID: stored.id,
            text: "Saved locally"
        )
        stored.developerTools.setEnabled(true)
        stored.developerTools.debugMode = true
        stored.quickReactionEmoji[0] = "🥳"
        stored.nostrAddress = "kept@example.com"
        stored.isNostrAddressVerified = false
        let relayConfiguration = stored.relayConfiguration
        let quickReactionEmoji = stored.quickReactionEmoji
        let avatar = PrototypeAvatar.webImage(
            assetName: "AvatarWebAionyHaust",
            choiceID: "3TLl_97HNJo"
        )
        let reactivated = PrototypeProfile.initialSignUp(
            name: "River",
            about: "A refreshed profile.",
            avatar: avatar
        )

        stored.updateEditableValues(from: reactivated)

        #expect(stored.name == "River")
        #expect(stored.about == "A refreshed profile.")
        #expect(stored.nostrAddress == "kept@example.com")
        #expect(!stored.isNostrAddressVerified)
        #expect(stored.avatar == avatar)
        #expect(stored.chats[supportIndex].messages.count == 1)
        #expect(stored.relayConfiguration == relayConfiguration)
        #expect(stored.quickReactionEmoji == quickReactionEmoji)
        #expect(stored.developerTools.isConversationDebugEnabled)
    }

    @Test("Wiping then recreating and adding preserves both canonical profiles")
    func wipeRecreateAndAddPreservesProfiles() {
        var profiles = [PrototypeProfile.marmota]
        profiles.removeAll()

        let recreated = PrototypeProfile.initialSignUp(name: "Marmota")
        profiles.append(recreated)

        let added = PrototypeProfile.addedSignUp(name: "Pebble")
        if !profiles.contains(where: { $0.id == added.id }) {
            profiles.append(added)
        }

        #expect(profiles.map(\.id) == ["marmota", "pebble"])
        #expect(
            profiles.first { $0.id == "marmota" }?.avatar
                == .asset("ProfileAvatarMarmota")
        )
        #expect(
            profiles.first { $0.id == "pebble" }?.avatar
                == .asset("ProfileAvatarPebble")
        )
    }
}
