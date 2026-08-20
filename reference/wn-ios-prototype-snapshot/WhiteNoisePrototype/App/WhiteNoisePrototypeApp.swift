import SwiftUI

@main
struct WhiteNoisePrototypeApp: App {
    var body: some Scene {
        WindowGroup {
            PrototypeRootView()
            .tint(Color("AccentColor"))
        }
    }
}

private struct PrototypeRootView: View {
    private enum RootDestination {
        case welcome
        case chats
        case profileSwitcher
    }

    private enum OnboardingPresentation: String, Identifiable {
        case signIn
        case signUp
        case addProfile

        var id: Self { self }
    }

    @State private var rootDestination: RootDestination
    @State private var onboardingPresentation: OnboardingPresentation?
    @State private var profiles: [PrototypeProfile]
    @State private var activeProfileID: String
    @State private var signedInProfileIDs: Set<String>
    @State private var settings = PrototypeSettingsState()
    @State private var isShowingSettings = false
    @State private var chatsPath: [ChatsRoute] = []
    @State private var dismissesAddProfileAfterSettingsRemoval = false

    init() {
        let opensChats = ProcessInfo.processInfo.arguments
            .contains("-ui-testing-chats")
        let initialProfiles = opensChats
            ? PrototypeProfile.initialProfiles
            : []

        _rootDestination = State(
            initialValue: opensChats ? .chats : .welcome
        )
        _profiles = State(initialValue: initialProfiles)
        _activeProfileID = State(initialValue: PrototypeProfile.marmotaID)
        _signedInProfileIDs = State(
            initialValue: Set(initialProfiles.map(\.id))
        )
    }

    var body: some View {
        Group {
            switch rootDestination {
            case .chats:
                NavigationStack(path: $chatsPath) {
                    ChatsView(
                        profile: activeProfileBinding,
                        settings: $settings,
                        onOpenSettings: {
                            isShowingSettings = true
                        },
                        onOpenRoute: { route in
                            chatsPath.append(route)
                        }
                    )
                    .navigationDestination(for: ChatsRoute.self) { route in
                        switch route {
                        case .newChat:
                            NewChatView(
                                profile: activeProfileBinding,
                                settings: $settings
                            )
                        case let .person(personID):
                            PersonProfileView(
                                profile: activeProfileBinding,
                                settings: $settings,
                                personID: personID,
                                onMessagePerson: openOrCreateDirectChat
                            )
                        case .newGroup:
                            NewGroupView(
                                profile: activeProfileBinding,
                                settings: $settings
                            )
                        case let .newGroupSetup(personIDs):
                            NewGroupSetupView(
                                profile: activeProfileBinding,
                                settings: $settings,
                                selectedPersonIDs: personIDs,
                                onCreateGroup: createGroup
                            )
                        case let .conversation(chatID):
                            ConversationView(
                                profile: activeProfileBinding,
                                settings: $settings,
                                chatID: chatID,
                                authoritativeChatReplacement: replaceChat
                            )
                        }
                    }
                    .navigationDestination(
                        isPresented: $isShowingSettings
                    ) {
                            SettingsView(
                                profiles: $profiles,
                                activeProfileID: $activeProfileID,
                                signedInProfileIDs: $signedInProfileIDs,
                                settings: $settings,
                                onSignOut: signOut,
                                onWipeProfile: wipeProfile,
                                onEraseAllAppData: eraseAllAppData,
                                onAddProfile: {
                                    onboardingPresentation = .addProfile
                                }
                            )
                    }
                }
            case .profileSwitcher:
                ProfileSwitcherSheet(
                    profiles: signedInProfiles,
                    activeProfileID: nil,
                    showsCloseButton: false,
                    onSelectProfile: activateProfile,
                    onAddProfile: nil
                )
            case .welcome:
                NavigationStack {
                    WelcomeView(
                        onLogin: {
                            onboardingPresentation = .signIn
                        },
                        onSignUp: {
                            onboardingPresentation = .signUp
                        }
                    )
                }
            }
        }
        .preferredColorScheme(settings.appearance.colorScheme)
        .sheet(item: $onboardingPresentation) { presentation in
            switch presentation {
            case .signIn:
                InitialSignInSheet {
                    completeInitialSignIn()
                }
            case .signUp:
                InitialSignUpSheet { name, about, avatar in
                    completeInitialSignUp(
                        name: name,
                        about: about,
                        avatar: avatar
                    )
                }
            case .addProfile:
                AddProfileFlow { profile, updatesStoredProfile in
                    completeAddedProfile(
                        profile,
                        updatesStoredProfile: updatesStoredProfile
                    )
                }
            }
        }
        .onChange(of: isShowingSettings) { _, isShowingSettings in
            guard !isShowingSettings,
                  dismissesAddProfileAfterSettingsRemoval
            else {
                return
            }

            dismissesAddProfileAfterSettingsRemoval = false

            Task { @MainActor in
                await Task.yield()
                guard onboardingPresentation == .addProfile else {
                    return
                }
                onboardingPresentation = nil
            }
        }
    }

    private var activeProfile: PrototypeProfile {
        profiles.first { $0.id == activeProfileID }
            ?? PrototypeProfile.marmota
    }

    private var activeProfileBinding: Binding<PrototypeProfile> {
        Binding {
            activeProfile
        } set: { profile in
            guard let index = profiles.firstIndex(
                where: { $0.id == activeProfileID }
            ) else {
                return
            }

            profiles[index] = profile
        }
    }

    private func openOrCreateDirectChat(personID: String) {
        var profile = activeProfile
        guard let chatID = profile.openOrCreateDirectChat(personID: personID) else {
            return
        }
        finishChatCreation(chatID, updatedProfile: profile)
    }

    private func createGroup(
        name: String,
        description: String,
        avatar: ChatListItem.Avatar,
        selectedPersonIDs: [String]
    ) {
        var profile = activeProfile
        guard let chatID = profile.createGroup(
            name: name,
            description: description,
            avatar: avatar,
            selectedPersonIDs: selectedPersonIDs
        ) else {
            return
        }
        finishChatCreation(chatID, updatedProfile: profile)
    }

    private func replaceChat(
        chatID: String,
        chat: PrototypeChat
    ) {
        var profile = activeProfile
        guard let index = profile.chats.firstIndex(where: { $0.id == chatID }) else {
            return
        }
        profile.chats[index] = chat
        activeProfileBinding.wrappedValue = profile
    }

    private func finishChatCreation(
        _ chatID: String,
        updatedProfile: PrototypeProfile
    ) {
        activeProfileBinding.wrappedValue = updatedProfile
        chatsPath.removeAll()
        Task { @MainActor in
            await Task.yield()
            chatsPath = [.conversation(chatID)]
        }
    }

    private func completeInitialSignIn() {
        activateStoredOrNewProfile(.marmota)

        onboardingPresentation = nil
        isShowingSettings = false
        rootDestination = .chats
    }

    private func completeInitialSignUp(
        name: String,
        about: String,
        avatar: PrototypeAvatar?
    ) {
        let profile = PrototypeProfile.initialSignUp(
            name: name,
            about: about,
            avatar: avatar
        )
        activateStoredOrNewProfile(profile, updatesStoredProfile: true)

        onboardingPresentation = nil
        isShowingSettings = false
        rootDestination = .chats
    }

    private func signOut(_ profileID: String) {
        signedInProfileIDs.remove(profileID)
        finishProfileExit()
    }

    private func wipeProfile(_ profileID: String) {
        signedInProfileIDs.remove(profileID)
        profiles.removeAll { $0.id == profileID }
        finishProfileExit()
    }

    private func eraseAllAppData() {
        signedInProfileIDs.removeAll()
        profiles.removeAll()
        activeProfileID = PrototypeProfile.marmotaID
        settings = PrototypeSettingsState()
        onboardingPresentation = nil
        isShowingSettings = false
        rootDestination = .welcome
    }

    private func completeAddedProfile(
        _ profile: PrototypeProfile,
        updatesStoredProfile: Bool
    ) {
        activateStoredOrNewProfile(
            profile,
            updatesStoredProfile: updatesStoredProfile
        )

        for pseudonym in PrototypeProfile.showcasePseudonyms {
            if !profiles.contains(where: { $0.id == pseudonym.id }) {
                profiles.append(pseudonym)
            }
            signedInProfileIDs.insert(pseudonym.id)
        }

        dismissesAddProfileAfterSettingsRemoval = true

        var transaction = Transaction()
        transaction.disablesAnimations = true

        withTransaction(transaction) {
            rootDestination = .chats
            isShowingSettings = false
        }
    }

    private func activateStoredOrNewProfile(
        _ profile: PrototypeProfile,
        updatesStoredProfile: Bool = false
    ) {
        if let index = profiles.firstIndex(
            where: { $0.id == profile.id }
        ) {
            if updatesStoredProfile {
                profiles[index].updateEditableValues(from: profile)
            }
        } else {
            profiles.append(profile)
        }

        activeProfileID = profile.id
        signedInProfileIDs.insert(profile.id)
    }

    private func finishProfileExit() {
        isShowingSettings = false

        switch ProfileExitRouting.destination(
            remainingSignedInProfileIDs: signedInProfileIDs
        ) {
        case .profileSwitcher:
            rootDestination = .profileSwitcher
        case .welcome:
            rootDestination = .welcome
        }
    }

    private func activateProfile(_ profileID: String) {
        guard profiles.contains(where: { $0.id == profileID }) else {
            return
        }

        signedInProfileIDs.insert(profileID)
        activeProfileID = profileID
        rootDestination = .chats
        isShowingSettings = true
    }

    private var signedInProfiles: [PrototypeProfile] {
        profiles.filter { signedInProfileIDs.contains($0.id) }
    }
}

private struct InitialSignInSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var selectedDetent = PresentationDetent.medium

    let onSignIn: () -> Void

    var body: some View {
        NavigationStack {
            LoginView(
                onScannerPresentationChange: { isPresented in
                    selectedDetent = isPresented ? .large : .medium
                },
                onInputFocusChange: { isFocused in
                    selectedDetent = isFocused ? .large : .medium
                },
                onSignIn: onSignIn
            )
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark")
                    }
                    .accessibilityLabel("Close")
                }
            }
        }
        .presentationDetents(
            [.medium, .large],
            selection: $selectedDetent
        )
        .presentationDragIndicator(.visible)
        .presentationContentInteraction(.resizes)
    }
}

private struct InitialSignUpSheet: View {
    @Environment(\.dismiss) private var dismiss

    let onSignUp: (String, String, PrototypeAvatar?) -> Void

    var body: some View {
        NavigationStack {
            SignUpView(onSignUp: onSignUp)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button {
                            dismiss()
                        } label: {
                            Image(systemName: "xmark")
                        }
                        .accessibilityLabel("Close")
                    }
                }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }
}
