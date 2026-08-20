import SwiftUI

enum ChatsRoute: Hashable {
    case newChat
    case person(String)
    case newGroup
    case newGroupSetup([String])
    case conversation(String)
}

struct ChatsView: View {
    enum ChatScope: String, CaseIterable, Identifiable {
        case all
        case unread
        case archived
        case left

        var id: Self { self }

        var title: String {
            switch self {
            case .all: "Chats"
            case .unread: "Unread"
            case .archived: "Archived"
            case .left: "Left"
            }
        }

        var symbol: String {
            switch self {
            case .all: "bubble.left.and.bubble.right"
            case .unread: "message.badge"
            case .archived: "archivebox"
            case .left: "rectangle.portrait.and.arrow.right"
            }
        }
    }

    @Binding private var profile: PrototypeProfile
    @Binding private var settings: PrototypeSettingsState
    @State private var scope: ChatScope
    @State private var searchText: String
    @State private var isSearchMounted = false
    @State private var isSearchPresented = false
    @State private var isShowingOnlyAdminAlert = false
    @FocusState private var isSearchFocused: Bool

    let onOpenSettings: () -> Void
    let onOpenRoute: (ChatsRoute) -> Void

    init(
        profile: Binding<PrototypeProfile>,
        settings: Binding<PrototypeSettingsState>,
        initialScope: ChatScope = .all,
        initialSearchText: String = "",
        onOpenSettings: @escaping () -> Void = {},
        onOpenRoute: @escaping (ChatsRoute) -> Void = { _ in }
    ) {
        _profile = profile
        _settings = settings
        _scope = State(initialValue: initialScope)
        _searchText = State(initialValue: initialSearchText)
        self.onOpenSettings = onOpenSettings
        self.onOpenRoute = onOpenRoute
    }

    var body: some View {
        let projection = chatProjection

        chatContent(projection.visibleChats)
            .ignoresSafeArea(.container, edges: [.top, .bottom])
            .navigationTitle("")
            .navigationBarTitleDisplayMode(.inline)
            .modifier(
                OnDemandChatSearch(
                    searchText: $searchText,
                    isMounted: $isSearchMounted,
                    isPresented: $isSearchPresented,
                    isFocused: $isSearchFocused
                )
            )
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    profileNavigationLink
                }
                .sharedBackgroundVisibility(.hidden)

                ToolbarItemGroup(placement: .topBarTrailing) {
                    filterMenu

                    Button {
                        isSearchMounted = true
                    } label: {
                        Label("Search Chats", systemImage: "magnifyingglass")
                            .labelStyle(.iconOnly)
                    }

                    if !profile.relayConfiguration.needsAttention {
                        Button {
                            onOpenRoute(.newChat)
                        } label: {
                            Label("New Message", systemImage: "plus.bubble")
                                .labelStyle(.iconOnly)
                        }
                        .accessibilityHint("Creates a new chat.")
                        .accessibilityIdentifier("chats.new")
                    } else {
                        RelayWarningLink(
                            configuration: $profile.relayConfiguration
                        )
                    }
                }
            }
            .modifier(
                ReadAllBottomBar(
                    isVisible: scope == .unread && projection.hasUnreadChats,
                    action: markAllChatsRead
                )
            )
            .alert("Can’t Leave Group", isPresented: $isShowingOnlyAdminAlert) {
                Button("Done") {}
            } message: {
                Text("You’re the only admin in this group. Make another member an admin before you leave.")
            }
    }

    @ViewBuilder
    private func chatContent(_ visibleChats: [ChatListItem]) -> some View {
        ZStack {
            NativeChatList(
                chats: visibleChats,
                actions: NativeChatList.Actions(
                    canOpen: { _ in true },
                    open: openConversation,
                    markRead: markChatRead,
                    markUnread: markChatUnread,
                    togglePinned: togglePinned,
                    mute: muteChat,
                    unmute: unmuteChat,
                    toggleArchive: toggleArchive,
                    leave: leaveChat,
                    delete: deleteChat
                )
            )

            if visibleChats.isEmpty {
                ContentUnavailableView {
                    Label(emptyTitle, systemImage: emptySymbol)
                } description: {
                    Text(emptyDescription)
                }
                .allowsHitTesting(false)
            }
        }
    }

    private var profileNavigationLink: some View {
        Button(action: onOpenSettings) {
            ProfileAvatarView(profile: profile, size: 44)
        }
        .buttonStyle(ProfileAvatarNavigationStyle())
        .accessibilityLabel("Profile")
        .accessibilityIdentifier("chats.profile")
    }

    private func markChatRead(_ id: String) {
        updateChat(id: id) {
            $0.listState.unreadCount = 0
            $0.listState.isMarkedUnread = false
        }
    }

    private func markAllChatsRead() {
        for index in profile.chats.indices where !profile.chats[index].listState.isArchived {
            profile.chats[index].listState.unreadCount = 0
            profile.chats[index].listState.isMarkedUnread = false
        }
    }

    private func markChatUnread(_ id: String) {
        updateChat(id: id) {
            $0.listState.unreadCount = 0
            $0.listState.isMarkedUnread = true
        }
    }

    private func togglePinned(_ id: String) {
        updateChat(id: id) { $0.listState.isPinned.toggle() }
    }

    private func muteChat(_ id: String, _ duration: ChatListItem.MuteDuration) {
        updateChat(id: id) { $0.listState.muteDuration = duration }
    }

    private func unmuteChat(_ id: String) {
        updateChat(id: id) { $0.listState.muteDuration = nil }
    }

    private func toggleArchive(_ id: String) {
        updateChat(id: id) { $0.listState.isArchived.toggle() }
    }

    private func leaveChat(_ id: String) {
        guard let index = profile.chats.firstIndex(where: { $0.id == id }) else {
            return
        }
        if !profile.chats[index].leave(currentProfileID: profile.id) {
            isShowingOnlyAdminAlert = true
        }
    }

    private func deleteChat(_ id: String) {
        profile.chats.removeAll { $0.id == id }
    }

    private func openConversation(_ id: String) {
        isSearchFocused = false
        isSearchPresented = false
        isSearchMounted = false
        searchText = ""
        onOpenRoute(.conversation(id))
    }

    private func updateChat(id: String, mutation: (inout PrototypeChat) -> Void) {
        guard let index = profile.chats.firstIndex(where: { $0.id == id }) else {
            return
        }
        mutation(&profile.chats[index])
    }

    private struct ChatProjection {
        let visibleChats: [ChatListItem]
        let hasUnreadChats: Bool
    }

    private var chatProjection: ChatProjection {
        let rows = profile.chats.map {
            $0.row(people: profile.people, currentProfileID: profile.id)
        }
        let scopedChats = rows.filter { chat in
            switch scope {
            case .all: !chat.isArchived
            case .unread: !chat.isArchived && chat.isUnread
            case .archived: chat.isArchived
            case .left: !chat.isArchived && chat.hasEndedMembership
            }
        }

        let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        let matchingChats = query.isEmpty ? scopedChats : scopedChats.filter { chat in
            chat.title.localizedCaseInsensitiveContains(query)
                || chat.searchablePreview.localizedCaseInsensitiveContains(query)
        }
        return ChatProjection(
            visibleChats: matchingChats.filter(\.isPinned)
                + matchingChats.filter { !$0.isPinned },
            hasUnreadChats: rows.contains { !$0.isArchived && $0.isUnread }
        )
    }

    private var filterMenu: some View {
        Menu {
            Picker("Filter Chats", selection: $scope) {
                ForEach(ChatScope.allCases) { scope in
                    Label(scope.title, systemImage: scope.symbol).tag(scope)
                }
            }
        } label: {
            filterMenuLabel.accessibilityHidden(true)
        }
        .menuIndicator(.hidden)
        .accessibilityLabel("Filter Chats")
        .accessibilityValue(scope.title)
    }

    @ViewBuilder
    private var filterMenuLabel: some View {
        if scope == .all {
            Image(systemName: "line.3.horizontal.decrease")
                .frame(width: 34, height: 34)
        } else {
            HStack {
                Image(systemName: "line.3.horizontal.decrease")
                Text(scope.title)
            }
            .font(.subheadline)
            .foregroundStyle(Color(uiColor: .systemBackground))
            .padding(.trailing, 10)
            .frame(height: 34)
            .background {
                Capsule().fill(Color("AccentColor")).padding(.leading, -5)
            }
        }
    }

    private var emptyTitle: LocalizedStringKey {
        if !searchText.isEmpty { return "No Results" }
        return switch scope {
        case .all: "No Chats"
        case .unread: "No Unread Chats"
        case .archived: "No Archived Chats"
        case .left: "No Left Chats"
        }
    }

    private var emptyDescription: LocalizedStringKey {
        if !searchText.isEmpty { return "Check the spelling or try a different search." }
        return switch scope {
        case .all: "Start a new chat to send a message."
        case .unread: "You’re all caught up."
        case .archived: "Chats you archive will appear here."
        case .left: "Chats you leave or are removed from will appear here."
        }
    }

    private var emptySymbol: String { searchText.isEmpty ? scope.symbol : "magnifyingglass" }
}

private struct ProfileAvatarNavigationStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View { configuration.label }
}

private struct ReadAllBottomBar: ViewModifier {
    let isVisible: Bool
    let action: () -> Void

    @ViewBuilder
    func body(content: Content) -> some View {
        if isVisible {
            content.toolbar {
                ToolbarItem(placement: .bottomBar) { Button("Read All", action: action) }
                ToolbarSpacer(.flexible, placement: .bottomBar)
            }
        } else {
            content
        }
    }
}

private struct OnDemandChatSearch: ViewModifier {
    @Binding var searchText: String
    @Binding var isMounted: Bool
    @Binding var isPresented: Bool
    var isFocused: FocusState<Bool>.Binding

    @ViewBuilder
    func body(content: Content) -> some View {
        if isMounted {
            content
                .searchable(
                    text: $searchText,
                    isPresented: $isPresented,
                    placement: .navigationBarDrawer(displayMode: .always),
                    prompt: "Search Chats"
                )
                .searchFocused(isFocused)
                .task {
                    await Task.yield()
                    isPresented = true
                    await Task.yield()
                    isFocused.wrappedValue = true
                }
                .onChange(of: isPresented) { _, presented in
                    if !presented {
                        isFocused.wrappedValue = false
                        isMounted = false
                    }
                }
        } else {
            content
        }
    }
}

#Preview("Chats") {
    @Previewable @State var profile = PrototypeProfile.marmota
    @Previewable @State var settings = PrototypeSettingsState()
    NavigationStack {
        ChatsView(profile: $profile, settings: $settings)
    }
    .tint(Color("AccentColor"))
}
