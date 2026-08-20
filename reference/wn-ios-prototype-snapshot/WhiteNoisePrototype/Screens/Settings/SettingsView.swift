import SwiftUI

enum SettingsDestination: String, CaseIterable, Hashable {
    case profile
    case profileKeys
    case notifications
    case appearance
    case privacyAndSecurity
    case dataUsage
    case relays
    case support
    case donate
    case developerTools
    case signOut

    var title: String {
        switch self {
        case .profile:
            "Profile"
        case .profileKeys:
            "Profile Keys"
        case .notifications:
            "Notifications"
        case .appearance:
            "Appearance"
        case .privacyAndSecurity:
            "Privacy & Security"
        case .dataUsage:
            "Data Usage"
        case .relays:
            "Relays"
        case .support:
            "Chat with support"
        case .donate:
            "Donate"
        case .developerTools:
            "Developer Tools"
        case .signOut:
            "Sign Out"
        }
    }

    var symbol: String {
        switch self {
        case .profile:
            "person.crop.circle"
        case .profileKeys:
            "key"
        case .notifications:
            "bell"
        case .appearance:
            "circle.lefthalf.filled"
        case .privacyAndSecurity:
            "hand.raised"
        case .dataUsage:
            "externaldrive"
        case .relays:
            "antenna.radiowaves.left.and.right"
        case .support:
            "message"
        case .donate:
            "heart"
        case .developerTools:
            "wrench.and.screwdriver"
        case .signOut:
            "rectangle.portrait.and.arrow.right"
        }
    }
}

struct SettingsView: View {
    @Binding private var profiles: [PrototypeProfile]
    @Binding private var activeProfileID: String
    @Binding private var signedInProfileIDs: Set<String>
    @Binding private var settings: PrototypeSettingsState

    private let onSignOut: (String) -> Void
    private let onWipeProfile: (String) -> Void
    private let onEraseAllAppData: () -> Void
    private let onAddProfile: () -> Void

    @State private var isShowingProfileSwitcher = false
    @State private var isShowingSignOut = false
    @State private var showAddProfileAfterSwitcherCloses = false

    init(
        profiles: Binding<[PrototypeProfile]>,
        activeProfileID: Binding<String>,
        signedInProfileIDs: Binding<Set<String>>,
        settings: Binding<PrototypeSettingsState>,
        onSignOut: @escaping (String) -> Void = { _ in },
        onWipeProfile: @escaping (String) -> Void = { _ in },
        onEraseAllAppData: @escaping () -> Void = {},
        onAddProfile: @escaping () -> Void = {}
    ) {
        _profiles = profiles
        _activeProfileID = activeProfileID
        _signedInProfileIDs = signedInProfileIDs
        _settings = settings
        self.onSignOut = onSignOut
        self.onWipeProfile = onWipeProfile
        self.onEraseAllAppData = onEraseAllAppData
        self.onAddProfile = onAddProfile
    }

    var body: some View {
        Form {
            Section {
                activeProfileNavigationRow
                profileManagementRow
            }

            destinationSection([
                .profile,
                .profileKeys,
                .notifications,
                .appearance,
                .privacyAndSecurity,
                .dataUsage,
                .relays,
            ])

            destinationSection([
                .support,
                .donate,
                .developerTools,
            ])

            Section {
                Button {
                    isShowingSignOut = true
                } label: {
                    SettingsDestinationLabel(destination: .signOut)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .contentShape(.rect)
                }
                .accessibilityIdentifier("settings.signOut")
            } footer: {
                Text(appVersion)
                    .frame(maxWidth: .infinity)
            }
        }
        .accessibilityIdentifier("settings.screen")
        .navigationTitle("Settings")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $isShowingSignOut) {
            if let activeProfile {
                SignOutPrototypeView(
                    profile: activeProfile,
                    onSignOut: {
                        onSignOut(activeProfile.id)
                    },
                    onWipeProfile: {
                        onWipeProfile(activeProfile.id)
                    }
                )
            }
        }
        .sheet(
            isPresented: $isShowingProfileSwitcher,
            onDismiss: {
                guard showAddProfileAfterSwitcherCloses else {
                    return
                }

                showAddProfileAfterSwitcherCloses = false
                onAddProfile()
            }
        ) {
            ProfileSwitcherSheet(
                profiles: signedInProfiles,
                activeProfileID: activeProfileID,
                onSelectProfile: switchProfile,
                onAddProfile: beginAddingProfile
            )
        }
    }

    @ViewBuilder
    private var activeProfileNavigationRow: some View {
        if let activeProfile {
            NavigationLink {
                ShareAndConnectView(profile: activeProfile)
            } label: {
                HStack {
                    ProfileSummary(
                        profile: activeProfile,
                        avatarSize: 56
                    )

                    Spacer()

                    Image(systemName: "qrcode")
                        .foregroundStyle(.primary)
                        .accessibilityHidden(true)
                }
            }
            .accessibilityLabel(
                "Open Share and Connect for \(activeProfile.name)"
            )
        }
    }

    @ViewBuilder
    private var profileManagementRow: some View {
        if inactiveProfiles.isEmpty {
            addProfileButton
        } else if inactiveProfiles.count == 1,
                  let alternateProfile = inactiveProfiles.first {
            alternateProfileButton(alternateProfile)
        } else {
            switchProfileButton
        }
    }

    private var addProfileButton: some View {
        Button {
            onAddProfile()
        } label: {
            Label(
                "Add Profile",
                systemImage: "person.crop.circle.badge.plus"
            )
            .foregroundStyle(.primary)
            .contentShape(.rect)
        }
    }

    private func alternateProfileButton(
        _ profile: PrototypeProfile
    ) -> some View {
        Button {
            isShowingProfileSwitcher = true
        } label: {
            HStack {
                ProfileSummary(
                    profile: profile,
                    avatarSize: 56
                )

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
                    .accessibilityHidden(true)
            }
            .foregroundStyle(.primary)
            .contentShape(.rect)
        }
        .accessibilityLabel("Switch to \(profile.name)")
        .accessibilityValue(profile.shortPublicKey)
    }

    private var switchProfileButton: some View {
        Button {
            isShowingProfileSwitcher = true
        } label: {
            HStack {
                ProfileAvatarStack(
                    profiles: inactiveProfiles
                )

                Text("Switch Profile")
                    .lineLimit(1)
                    .layoutPriority(1)

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.footnote.weight(.semibold))
                    .foregroundStyle(.tertiary)
                    .accessibilityHidden(true)
            }
            .foregroundStyle(.primary)
            .contentShape(.rect)
        }
        .accessibilityLabel("Switch Profile")
        .accessibilityValue("\(signedInProfiles.count) profiles")
    }

    private func destinationSection(
        _ destinations: [SettingsDestination]
    ) -> some View {
        Section {
            ForEach(destinations, id: \.self) { destination in
                destinationLink(destination)
            }
        }
    }

    private func destinationLink(
        _ destination: SettingsDestination
    ) -> some View {
        NavigationLink {
            destinationView(destination)
        } label: {
            SettingsDestinationLabel(destination: destination)
        }
        .accessibilityIdentifier("settings.\(destination.rawValue)")
    }

    @ViewBuilder
    private func destinationView(
        _ destination: SettingsDestination
    ) -> some View {
        switch destination {
        case .profile:
            if let activeProfileBinding {
                ProfileSettingsView(profile: activeProfileBinding)
            }
        case .profileKeys:
            if let activeProfile {
                ProfileKeysSettingsView(profile: activeProfile)
            }
        case .notifications:
            NotificationSettingsPrototypeView(settings: $settings)
        case .appearance:
            AppearanceSettingsPrototypeView(settings: $settings)
        case .privacyAndSecurity:
            PrivacySecurityPrototypeView(
                settings: $settings,
                profiles: profiles,
                onEraseAllAppData: onEraseAllAppData
            )
        case .dataUsage:
            DataUsagePrototypeView(settings: $settings)
        case .relays:
            if let activeProfileBinding {
                RelaysPrototypeView(
                    configuration: activeProfileBinding.relayConfiguration
                )
            }
        case .support:
            if let activeProfileBinding {
                SupportPrototypeView(
                    profile: activeProfileBinding,
                    settings: $settings
                )
            }
        case .donate:
            DonatePrototypeView()
        case .developerTools:
            if let activeProfileBinding {
                DeveloperToolsPrototypeView(
                    developerTools: activeProfileBinding.developerTools,
                    profile: activeProfileBinding.wrappedValue
                )
            }
        case .signOut:
            EmptyView()
        }
    }

    private func switchProfile(to profileID: String) {
        guard profileID != activeProfileID,
              signedInProfileIDs.contains(profileID),
              profiles.contains(where: { $0.id == profileID })
        else {
            return
        }

        activeProfileID = profileID
        isShowingProfileSwitcher = false
    }

    private func beginAddingProfile() {
        showAddProfileAfterSwitcherCloses = true
        isShowingProfileSwitcher = false
    }

    private var activeProfile: PrototypeProfile? {
        profiles.first { $0.id == activeProfileID }
    }

    private var activeProfileBinding: Binding<PrototypeProfile>? {
        guard let index = profiles.firstIndex(
            where: { $0.id == activeProfileID }
        ) else {
            return nil
        }

        return $profiles[index]
    }

    private var inactiveProfiles: [PrototypeProfile] {
        signedInProfiles.filter {
            $0.id != activeProfileID
        }
    }

    private var signedInProfiles: [PrototypeProfile] {
        profiles.filter { signedInProfileIDs.contains($0.id) }
    }

    private var appVersion: String {
        let version = Bundle.main.object(
            forInfoDictionaryKey: "CFBundleShortVersionString"
        ) as? String ?? "—"
        let build = Bundle.main.object(
            forInfoDictionaryKey: "CFBundleVersion"
        ) as? String ?? "—"

        return "White Noise · \(version) (\(build))"
    }
}

struct ProfileSwitcherSheet: View {
    @Environment(\.dismiss) private var dismiss

    let profiles: [PrototypeProfile]
    let activeProfileID: String?
    var showsCloseButton = true
    let onSelectProfile: (String) -> Void
    let onAddProfile: (() -> Void)?

    var body: some View {
        NavigationStack {
            List {
                Section {
                    ForEach(orderedProfiles) { profile in
                        Button {
                            onSelectProfile(profile.id)
                        } label: {
                            HStack {
                                ProfileSummary(
                                    profile: profile,
                                    avatarSize: 48
                                )

                                Spacer()

                                trailingState(for: profile)
                            }
                            .contentShape(.rect)
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier(
                            "profile-switcher.profile.\(profile.id)"
                        )
                    }
                }
            }
            .accessibilityIdentifier("profile-switcher.list")
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(Color(uiColor: .systemGroupedBackground))
            .safeAreaBar(edge: .bottom) {
                if let onAddProfile {
                    Button(action: onAddProfile) {
                        Label(
                            "Add Profile",
                            systemImage: "person.crop.circle.badge.plus"
                        )
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.glassProminent)
                    .controlSize(.extraLarge)
                    .padding()
                }
            }
            .scrollEdgeEffectStyle(.soft, for: .bottom)
            .navigationTitle("Switch Profile")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if showsCloseButton {
                    ToolbarItem(placement: .cancellationAction) {
                        Button {
                            dismiss()
                        } label: {
                            Label("Close", systemImage: "xmark")
                                .labelStyle(.iconOnly)
                        }
                    }
                }
            }
        }
        .background(Color(uiColor: .systemGroupedBackground))
        .presentationBackground(
            Color(uiColor: .systemGroupedBackground)
        )
        .presentationDetents([.medium, .large])
        .presentationDragIndicator(.visible)
        .presentationContentInteraction(.resizes)
    }

    @ViewBuilder
    private func trailingState(
        for profile: PrototypeProfile
    ) -> some View {
        if let activeProfileID,
           profile.id == activeProfileID {
            Image(systemName: "checkmark")
                .fontWeight(.semibold)
                .accessibilityLabel("Current profile")
        } else if profile.unreadCount > 0 {
            ProfileUnreadBadge(count: profile.unreadCount)
        }
    }

    private var orderedProfiles: [PrototypeProfile] {
        guard let activeProfileID else {
            return profiles
        }

        return profiles.filter { $0.id == activeProfileID }
            + profiles.filter { $0.id != activeProfileID }
    }
}

private struct SettingsDestinationLabel: View {
    let destination: SettingsDestination

    var body: some View {
        Label(destination.title, systemImage: destination.symbol)
            .foregroundStyle(.primary)
    }
}

private struct ProfileUnreadBadge: View {
    let count: Int

    private var label: String {
        count > 99 ? "99+" : count.formatted()
    }

    var body: some View {
        Text(label)
            .font(.caption2.weight(.bold))
            .monospacedDigit()
            .foregroundStyle(Color(uiColor: .systemBackground))
            .padding(.horizontal, 6)
            .frame(minWidth: 22, minHeight: 22)
            .background(Color("AccentColor"), in: Capsule())
            .accessibilityLabel("\(count) unread messages")
    }
}

private struct ProfileAvatarStack: View {
    let profiles: [PrototypeProfile]

    var body: some View {
        HStack(spacing: -10) {
            ForEach(profiles.prefix(3)) { profile in
                ProfileAvatarView(
                    profile: profile,
                    size: 32
                )
                .stackOutline()
            }

            if profiles.count > 3 {
                Text("+\(profiles.count - 3)")
                    .font(.caption2.weight(.semibold))
                    .monospacedDigit()
                    .foregroundStyle(.primary)
                    .frame(width: 32, height: 32)
                    .background(
                        Color(uiColor: .systemGray5),
                        in: Circle()
                    )
                    .stackOutline()
            }
        }
        .accessibilityHidden(true)
    }
}

private extension View {
    func stackOutline() -> some View {
        overlay {
            Circle()
                .stroke(
                    Color(uiColor: .secondarySystemGroupedBackground),
                    lineWidth: 2
                )
        }
    }
}

private struct SettingsPreviewHost: View {
    @State private var profiles: [PrototypeProfile]
    @State private var activeProfileID: String
    @State private var signedInProfileIDs: Set<String>
    @State private var settings = PrototypeSettingsState()

    init(
        profiles: [PrototypeProfile] = PrototypeProfile.initialProfiles,
        activeProfileID: String = PrototypeProfile.marmota.id
    ) {
        _profiles = State(initialValue: profiles)
        _activeProfileID = State(initialValue: activeProfileID)
        _signedInProfileIDs = State(
            initialValue: Set(profiles.map(\.id))
        )
    }

    var body: some View {
        NavigationStack {
            SettingsView(
                profiles: $profiles,
                activeProfileID: $activeProfileID,
                signedInProfileIDs: $signedInProfileIDs,
                settings: $settings
            )
        }
        .tint(Color("AccentColor"))
    }
}

#Preview("Settings") {
    SettingsPreviewHost()
}

#Preview("Settings — One Profile") {
    SettingsPreviewHost(profiles: [.marmota])
}

#Preview("Settings — Two Profiles") {
    SettingsPreviewHost(
        profiles: [
            .marmota,
            .pebble,
        ],
        activeProfileID: PrototypeProfile.pebble.id
    )
}

#Preview("Settings — Seven Profiles") {
    SettingsPreviewHost(
        profiles: PrototypeProfile.postAddProfileFixtures,
        activeProfileID: PrototypeProfile.pebble.id
    )
}

#Preview("Profile Switcher") {
    ProfileSwitcherSheet(
        profiles: PrototypeProfile.postAddProfileFixtures,
        activeProfileID: PrototypeProfile.pebble.id,
        onSelectProfile: { _ in },
        onAddProfile: {}
    )
    .tint(Color("AccentColor"))
}

#Preview("Settings — Open Quill") {
    SettingsAlternateProfilePreview()
}

private struct SettingsAlternateProfilePreview: View {
    @State private var profiles =
        PrototypeProfile.multipleProfileFixtures
    @State private var activeProfileID = PrototypeProfile.openQuill.id
    @State private var signedInProfileIDs = Set(
        PrototypeProfile.multipleProfileFixtures.map(\.id)
    )
    @State private var settings = PrototypeSettingsState()

    var body: some View {
        NavigationStack {
            SettingsView(
                profiles: $profiles,
                activeProfileID: $activeProfileID,
                signedInProfileIDs: $signedInProfileIDs,
                settings: $settings
            )
        }
        .tint(Color("AccentColor"))
    }
}

#Preview("Share & Connect") {
    NavigationStack {
        ShareAndConnectView(profile: .marmota)
    }
    .tint(Color("AccentColor"))
}
