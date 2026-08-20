import SwiftUI

struct SignOutPrototypeView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var shouldWipeData = true
    @State private var confirmationText = ""
    @State private var progress: SignOutProgress?
    @FocusState private var isConfirmationFieldFocused: Bool

    let profile: PrototypeProfile
    let onSignOut: () -> Void
    let onWipeProfile: () -> Void

    var body: some View {
        NavigationStack {
            Group {
                if let progress {
                    ContentUnavailableView {
                        ProgressView()
                    } description: {
                        Text(progress.label)
                    }
                } else {
                    Form {
                        Section {
                            ProfileSummary(
                                profile: profile,
                                avatarSize: 48
                            )

                            Toggle(
                                "Wipe Data From This Device",
                                isOn: $shouldWipeData
                            )
                            .accessibilityIdentifier(
                                "sign-out.wipe-data-toggle"
                            )
                        } footer: {
                            Text(wipeDataExplanation)
                        }

                        if shouldWipeData {
                            Section {
                                TextField(
                                    "Profile name",
                                    text: $confirmationText
                                )
                                .textInputAutocapitalization(.words)
                                .autocorrectionDisabled()
                                .submitLabel(.done)
                                .focused($isConfirmationFieldFocused)
                                .accessibilityIdentifier(
                                    "wipe-profile.confirmation-field"
                                )
                            } header: {
                                Text("Enter Profile Name")
                            } footer: {
                                Text(
                                    "Type “\(profile.name)” to confirm permanent removal of this profile and its local data."
                                )
                            }
                        }

                        Section {
                            signOutButton
                                .listRowInsets(EdgeInsets())
                                .listRowBackground(Color.clear)
                        }
                    }
                }
            }
            .navigationTitle("Sign Out")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                    }
                    .accessibilityLabel("Close")
                    .disabled(progress != nil)
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .interactiveDismissDisabled(progress != nil)
        .onChange(of: shouldWipeData) { _, shouldWipeData in
            if !shouldWipeData {
                confirmationText = ""
                isConfirmationFieldFocused = false
            }
        }
    }

    @ViewBuilder
    private var signOutButton: some View {
        if shouldWipeData {
            Button(role: .destructive) {
                run(
                    .signingOutAndWipingData,
                    completion: onWipeProfile
                )
            } label: {
                Text("Sign Out")
                    .font(.body.weight(.regular))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .tint(.red)
            .frame(maxWidth: .infinity)
            .disabled(!confirmationMatches)
            .accessibilityLabel("Sign Out and Wipe Data")
            .accessibilityValue(
                confirmationMatches ? "Ready" : "Profile name required"
            )
            .accessibilityHint(
                confirmationMatches
                    ? "Permanently removes this profile and its local data."
                    : "Enter \(profile.name) to enable this button."
            )
            .accessibilityIdentifier("wipe-profile.confirm")
        } else {
            Button(role: .destructive) {
                run(.signingOut, completion: onSignOut)
            } label: {
                Text("Sign Out")
                    .font(.body.weight(.regular))
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .tint(.red)
            .frame(maxWidth: .infinity)
            .accessibilityIdentifier("sign-out.keep-data")
        }
    }

    private var wipeDataExplanation: String {
        if shouldWipeData {
            "This profile and all local data will be permanently removed. Previous chats won’t return."
        } else {
            "This profile and its local data will stay on this device."
        }
    }

    private var confirmationMatches: Bool {
        WipeConfirmationPhrase.matches(
            confirmationText,
            expected: profile.name
        )
    }

    private func run(
        _ nextProgress: SignOutProgress,
        completion: @escaping () -> Void
    ) {
        progress = nextProgress

        Task {
            try? await Task.sleep(for: .seconds(1.5))
            guard !Task.isCancelled else {
                return
            }
            completion()
        }
    }
}

struct EraseAllAppDataView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var confirmationText = ""
    @State private var progress: SignOutProgress?

    let onErase: () -> Void
    private let confirmationPhrase: String

    init(
        profileIDs: [String],
        onErase: @escaping () -> Void
    ) {
        self.onErase = onErase
        confirmationPhrase = WipeConfirmationPhrase.make(
            profileIDs: profileIDs
        )
    }

    var body: some View {
        NavigationStack {
            Group {
                if let progress {
                    ContentUnavailableView {
                        ProgressView()
                    } description: {
                        Text(progress.label)
                    }
                } else {
                    Form {
                        Section {
                            Label {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("This can’t be undone")

                                    Text(
                                        "Every profile and all local chats, media, drafts, keys, and settings will be removed from this iPhone."
                                    )
                                    .font(.subheadline)
                                    .foregroundStyle(.secondary)
                                }
                            } icon: {
                                Image(
                                    systemName:
                                        "exclamationmark.triangle.fill"
                                )
                                .foregroundStyle(.orange)
                            }
                        }

                        Section {
                            Text(confirmationPhrase)
                                .font(.body.monospaced())
                                .textSelection(.enabled)
                                .accessibilityIdentifier(
                                    "erase-all.confirmation-phrase"
                                )

                            TextField(
                                "Confirmation phrase",
                                text: $confirmationText
                            )
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .submitLabel(.done)
                            .accessibilityIdentifier(
                                "erase-all.confirmation-field"
                            )
                        } header: {
                            Text("Type These Words to Confirm")
                        } footer: {
                            Text("Enter the three words exactly to continue.")
                        }

                        Section {
                            Button(role: .destructive) {
                                run()
                            } label: {
                                Text("Erase")
                                    .font(.body.weight(.regular))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.borderedProminent)
                            .controlSize(.large)
                            .tint(.red)
                            .frame(maxWidth: .infinity)
                            .disabled(!confirmationMatches)
                            .accessibilityIdentifier("erase-all.confirm")
                            .listRowInsets(EdgeInsets())
                            .listRowBackground(Color.clear)
                        }
                    }
                }
            }
            .navigationTitle("Erase App Data")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                    }
                    .accessibilityLabel("Close")
                    .disabled(progress != nil)
                }
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .interactiveDismissDisabled(progress != nil)
    }

    private var confirmationMatches: Bool {
        WipeConfirmationPhrase.matches(
            confirmationText,
            expected: confirmationPhrase
        )
    }

    private func run() {
        progress = .erasingAllAppData

        Task {
            try? await Task.sleep(for: .seconds(1.5))
            guard !Task.isCancelled else {
                return
            }
            onErase()
        }
    }
}

enum WipeConfirmationPhrase {
    private static let words = [
        "anchor",
        "apple",
        "bridge",
        "cactus",
        "harbor",
        "kitten",
        "maple",
        "planet",
        "river",
        "window",
        "yellow",
        "zebra",
    ]

    static func make(profileIDs: [String]) -> String {
        let source = profileIDs.sorted().joined(separator: "|")
        let seed = source.unicodeScalars.enumerated().reduce(0) {
            partialResult,
            element in
            let (offset, scalar) = element
            return (
                partialResult
                    + ((offset + 1) * Int(scalar.value))
            ) % 1_000_003
        }

        var indexes = [
            seed % words.count,
            ((seed / 7) + 3) % words.count,
            ((seed / 17) + 7) % words.count,
        ]

        for index in indexes.indices.dropFirst() {
            while indexes[..<index].contains(indexes[index]) {
                indexes[index] = (indexes[index] + 1) % words.count
            }
        }

        return indexes.map { words[$0] }.joined(separator: " ")
    }

    static func matches(_ input: String, expected phrase: String) -> Bool {
        input.trimmingCharacters(in: .whitespacesAndNewlines) == phrase
    }
}

enum ProfileExitDestination: Equatable {
    case profileSwitcher
    case welcome
}

enum ProfileExitRouting {
    static func destination(
        remainingSignedInProfileIDs: Set<String>
    ) -> ProfileExitDestination {
        remainingSignedInProfileIDs.isEmpty
            ? .welcome
            : .profileSwitcher
    }
}

private enum SignOutProgress {
    case signingOut
    case signingOutAndWipingData
    case erasingAllAppData

    var label: String {
        switch self {
        case .signingOut:
            "Signing out…"
        case .signingOutAndWipingData:
            "Signing out and wiping data…"
        case .erasingAllAppData:
            "Erasing app data…"
        }
    }
}

#Preview("Sign Out") {
    SignOutPrototypeView(
        profile: .marmota,
        onSignOut: {},
        onWipeProfile: {}
    )
}

#Preview("Erase App Data") {
    EraseAllAppDataView(
        profileIDs: PrototypeProfile.multipleProfileFixtures.map(\.id),
        onErase: {}
    )
}
