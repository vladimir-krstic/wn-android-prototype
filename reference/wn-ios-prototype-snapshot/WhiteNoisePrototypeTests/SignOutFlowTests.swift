import Testing
@testable import WhiteNoisePrototype

@Suite("Profile exit flow")
struct SignOutFlowTests {
    @Test("An available profile routes to the profile switcher")
    func remainingProfileRoutesToSwitcher() {
        #expect(
            ProfileExitRouting.destination(
                remainingSignedInProfileIDs: ["open-quill"]
            ) == .profileSwitcher
        )
    }

    @Test("No available profile routes to Welcome")
    func noRemainingProfileRoutesToWelcome() {
        #expect(
            ProfileExitRouting.destination(
                remainingSignedInProfileIDs: []
            ) == .welcome
        )
    }

    @Test("Wipe confirmation phrase is stable and lowercase")
    func confirmationPhraseIsStable() {
        let profileIDs = ["marmota", "open-quill", "cipher-wheel"]
        let first = WipeConfirmationPhrase.make(profileIDs: profileIDs)
        let second = WipeConfirmationPhrase.make(
            profileIDs: Array(profileIDs.reversed())
        )
        let words = first.split(separator: " ")

        #expect(first == second)
        #expect(words.count == 3)
        #expect(Set(words).count == 3)
        #expect(first == first.lowercased())
        #expect(first.allSatisfy { $0.isLowercase || $0 == " " })
    }

    @Test("App-data erasure ignores surrounding whitespace only")
    func confirmationPhraseRequiresExactMatch() {
        let phrase = WipeConfirmationPhrase.make(
            profileIDs: ["marmota", "open-quill"]
        )

        #expect(WipeConfirmationPhrase.matches(phrase, expected: phrase))
        #expect(
            !WipeConfirmationPhrase.matches(
                phrase.uppercased(),
                expected: phrase
            )
        )
        #expect(WipeConfirmationPhrase.matches(" \(phrase)\n", expected: phrase))
        #expect(
            !WipeConfirmationPhrase.matches(
                phrase.replacingOccurrences(of: " ", with: "  "),
                expected: phrase
            )
        )
    }
}
