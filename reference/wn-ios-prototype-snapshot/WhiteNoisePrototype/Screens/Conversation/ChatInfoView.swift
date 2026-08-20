import PhotosUI
import QuickLook
import SwiftUI
import UniformTypeIdentifiers

struct ChatInfoView: View {
    @Binding var profile: PrototypeProfile
    @Binding var settings: PrototypeSettingsState
    let chatID: String
    let onSearch: () -> Void
    let onOpenMessage: (String) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        if let index = profile.chats.firstIndex(where: { $0.id == chatID }) {
            if profile.chats[index].isGroup {
                GroupInfoView(
                    profile: $profile,
                    settings: $settings,
                    chatID: chatID,
                    onSearch: search,
                    onOpenMessage: openMessage
                )
            } else {
                DirectChatInfoView(
                    profile: $profile,
                    settings: $settings,
                    chatID: chatID,
                    onSearch: search,
                    onOpenMessage: openMessage
                )
            }
        } else {
            ContentUnavailableView("Chat Unavailable", systemImage: "bubble.left")
        }
    }

    private func search() {
        dismiss()
        Task { @MainActor in
            await Task.yield()
            onSearch()
        }
    }

    private func openMessage(_ messageID: String) {
        dismiss()
        Task { @MainActor in
            await Task.yield()
            onOpenMessage(messageID)
        }
    }
}

private struct DirectChatInfoView: View {
    @Binding var profile: PrototypeProfile
    @Binding var settings: PrototypeSettingsState
    let chatID: String
    let onSearch: () -> Void
    let onOpenMessage: (String) -> Void

    @State private var isShowingAbout = false
    @State private var isShowingLeaveConfirmation = false

    var body: some View {
        List {
            identityHeader
                .listRowInsets(.init(top: 24, leading: 16, bottom: 0, trailing: 16))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)

            ChatInfoSharedContentLinks(
                profile: $profile,
                chatID: chatID,
                onOpenMessage: onOpenMessage
            )

            Section("Chat Actions") {
                ChatInfoTechnicalRows(
                    profile: $profile,
                    settings: settings,
                    chatID: chatID
                )

                Button(
                    chat.listState.isArchived ? "Unarchive" : "Archive",
                    systemImage: "archivebox"
                ) {
                    updateChat { $0.listState.isArchived.toggle() }
                }

                if chat.listState.membershipState == .active {
                    Button(role: .destructive) {
                        isShowingLeaveConfirmation = true
                    } label: {
                        Label(
                            "Leave Chat",
                            systemImage: "rectangle.portrait.and.arrow.right"
                        )
                        .foregroundStyle(.red)
                    }
                }
            }
        }
        .navigationTitle("Chat Info")
        .navigationBarTitleDisplayMode(.inline)
        .navigationDestination(isPresented: $isShowingAbout) {
            PersonProfileView(
                profile: $profile,
                settings: $settings,
                personID: personID,
                onMessagePerson: { _ in },
                showsMessageAction: false
            )
        }
        .alert(
            "Leave \(person.name)?",
            isPresented: $isShowingLeaveConfirmation
        ) {
            Button("Leave Chat", role: .destructive, action: leaveChat)
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(
                "You’ll stop receiving new messages. This chat will remain "
                    + "on this device as read-only history until you delete it."
            )
        }
    }

    private var identityHeader: some View {
        VStack(spacing: 10) {
            ProfileIdentityHeader(
                name: person.name,
                publicKey: person.publicKey,
                nostrAddress: person.nostrAddress,
                isNostrAddressVerified: person.isNostrAddressVerified
            ) { size in
                PrototypeChatAvatarView(
                    avatar: person.avatar,
                    size: size,
                    publicKey: person.publicKey
                )
            }

            HStack(spacing: 12) {
                ChatInfoQuickAction(title: "About") {
                    Button {
                        isShowingAbout = true
                    } label: {
                        ChatInfoQuickActionIcon(systemName: "person.crop.circle")
                    }
                    .buttonStyle(ChatInfoSecondaryActionButtonStyle())
                    .accessibilityLabel("About")
                }

                ChatInfoQuickAction(title: muteActionTitle) {
                    muteMenu
                }

                ChatInfoQuickAction(title: "Disappearing") {
                    disappearingMessagesMenu
                }

                ChatInfoQuickAction(title: "Search") {
                    Button(action: onSearch) {
                        ChatInfoQuickActionIcon(systemName: "magnifyingglass")
                    }
                    .buttonStyle(ChatInfoSecondaryActionButtonStyle())
                    .accessibilityLabel("Search")
                }
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var chatIndex: Int { profile.chats.firstIndex { $0.id == chatID }! }
    private var chat: PrototypeChat { profile.chats[chatIndex] }
    private var personID: String {
        if case let .direct(id) = chat.kind { return id }
        return ""
    }
    private var personIndex: Int { profile.people.firstIndex { $0.id == personID }! }
    private var person: PrototypePerson { profile.people[personIndex] }
    private var muteActionTitle: String {
        chat.listState.muteDuration == nil ? "Mute" : "Unmute"
    }

    private var disappearingMessagesMenu: some View {
        Menu {
            Picker("Disappearing Messages", selection: disappearingMessageDuration) {
                ForEach(PrototypeDisappearingMessageDuration.allCases) { duration in
                    Text(duration.title).tag(duration)
                }
            }
        } label: {
            ChatInfoQuickActionIcon(systemName: "timer")
        }
        .buttonStyle(ChatInfoSecondaryActionButtonStyle())
        .accessibilityLabel("Disappearing Messages")
        .accessibilityValue(chat.disappearingMessageDuration.title)
    }

    private var disappearingMessageDuration: Binding<PrototypeDisappearingMessageDuration> {
        Binding {
            chat.disappearingMessageDuration
        } set: { duration in
            updateChat {
                $0.setDisappearingMessages(duration, actorID: profile.id)
            }
        }
    }

    private var muteMenu: some View {
        Menu {
            if chat.listState.muteDuration != nil {
                Button("Unmute") { updateChat { $0.listState.muteDuration = nil } }
            } else {
                ForEach(ChatListItem.MuteDuration.allCases) { duration in
                    Button(duration.title) { updateChat { $0.listState.muteDuration = duration } }
                }
            }
        } label: {
            ChatInfoQuickActionIcon(
                systemName: chat.listState.muteDuration == nil ? "bell.slash" : "bell"
            )
        }
        .buttonStyle(ChatInfoSecondaryActionButtonStyle())
        .accessibilityLabel(muteActionTitle)
    }

    private func updateChat(_ mutation: (inout PrototypeChat) -> Void) {
        mutation(&profile.chats[chatIndex])
    }

    private func leaveChat() {
        updateChat { _ = $0.leave(currentProfileID: profile.id) }
    }
}

private struct GroupInfoView: View {
    @Binding var profile: PrototypeProfile
    @Binding var settings: PrototypeSettingsState
    let chatID: String
    let onSearch: () -> Void
    let onOpenMessage: (String) -> Void

    @State private var isShowingLeaveConfirmation = false
    @State private var isShowingOnlyAdminAlert = false

    var body: some View {
        List {
            identityHeader
                .listRowInsets(.init(top: 24, leading: 16, bottom: 0, trailing: 16))
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)

            ChatInfoSharedContentLinks(
                profile: $profile,
                chatID: chatID,
                onOpenMessage: onOpenMessage
            )
            ChatInfoTechnicalLinks(
                profile: $profile,
                settings: settings,
                chatID: chatID
            )

            Section("Members") {
                ForEach(chat.members) { member in
                    NavigationLink {
                        GroupMemberView(
                            profile: $profile,
                            settings: $settings,
                            chatID: chatID,
                            personID: member.personID
                        )
                    } label: {
                        HStack {
                            memberAvatar(member.personID)
                            Text(memberName(member.personID))
                            Spacer()
                            if member.role == .admin {
                                Text("Admin")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                    .accessibilityIdentifier("group-member.\(member.personID)")
                }
            }

            if canManage {
                Section {
                    NavigationLink {
                        EditGroupView(profile: $profile, chatID: chatID)
                    } label: {
                        Label("Edit Group", systemImage: "pencil")
                    }
                    NavigationLink {
                        AddPeopleToGroupView(profile: $profile, chatID: chatID)
                    } label: {
                        Label("Add People", systemImage: "person.badge.plus")
                    }
                }
            }

            Section {
                Button(
                    chat.listState.isArchived ? "Unarchive" : "Archive",
                    systemImage: "archivebox"
                ) {
                    updateChat { $0.listState.isArchived.toggle() }
                }

                if chat.listState.membershipState == .active {
                    Button(role: .destructive) {
                        if isOnlyAdmin {
                            isShowingOnlyAdminAlert = true
                        } else {
                            isShowingLeaveConfirmation = true
                        }
                    } label: {
                        Label(
                            "Leave Group",
                            systemImage: "rectangle.portrait.and.arrow.right"
                        )
                        .foregroundStyle(.red)
                    }
                }
            }
        }
        .navigationTitle("Group Info")
        .navigationBarTitleDisplayMode(.inline)
        .alert(
            "Leave \(chat.groupName)?",
            isPresented: $isShowingLeaveConfirmation
        ) {
            Button("Leave Group", role: .destructive, action: leaveGroup)
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("You’ll keep the chat history, but you won’t be able to send messages.")
        }
        .alert("Can’t Leave Group", isPresented: $isShowingOnlyAdminAlert) {
            Button("Done") {}
        } message: {
            Text("You’re the only admin in this group. Make another member an admin before you leave.")
        }
    }

    private var identityHeader: some View {
        VStack(spacing: 24) {
            VStack(spacing: 10) {
                PrototypeChatAvatarView(avatar: chat.avatar, size: 104)
                Text(chat.groupName)
                    .font(.title2.bold())
                    .multilineTextAlignment(.center)
                if !chat.groupDescription.isEmpty {
                    Text(chat.groupDescription)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                }
                Text("\(chat.members.count) members")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }

            HStack(spacing: 12) {
                ChatInfoQuickAction(title: muteActionTitle) {
                    muteMenu
                }

                ChatInfoQuickAction(title: "Disappearing") {
                    disappearingMessagesMenu
                }

                ChatInfoQuickAction(title: "Search") {
                    Button(action: onSearch) {
                        ChatInfoQuickActionIcon(systemName: "magnifyingglass")
                    }
                    .buttonStyle(ChatInfoSecondaryActionButtonStyle())
                    .accessibilityLabel("Search")
                }
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var chatIndex: Int { profile.chats.firstIndex { $0.id == chatID }! }
    private var chat: PrototypeChat { profile.chats[chatIndex] }
    private var canManage: Bool {
        chat.listState.membershipState == .active
            && chat.isCurrentProfileAdmin(profile.id)
    }
    private var isOnlyAdmin: Bool {
        canManage && chat.members.filter { $0.role == .admin }.count == 1
    }
    private var muteActionTitle: String {
        chat.listState.muteDuration == nil ? "Mute" : "Unmute"
    }

    @ViewBuilder
    private func memberAvatar(_ id: String) -> some View {
        if id == profile.id {
            ProfileAvatarView(profile: profile, size: 36)
        } else {
            let person = profile.people.first { $0.id == id }
            PrototypeChatAvatarView(
                avatar: person?.avatar ?? .monogram("?"),
                size: 36,
                publicKey: person?.publicKey
            )
        }
    }

    private func memberName(_ id: String) -> String {
        id == profile.id ? "You" : (profile.people.first { $0.id == id }?.name ?? "Unknown")
    }

    private var disappearingMessagesMenu: some View {
        Menu {
            Picker("Disappearing Messages", selection: disappearingMessageDuration) {
                ForEach(PrototypeDisappearingMessageDuration.allCases) { duration in
                    Text(duration.title).tag(duration)
                }
            }
        } label: {
            ChatInfoQuickActionIcon(systemName: "timer")
        }
        .buttonStyle(ChatInfoSecondaryActionButtonStyle())
        .accessibilityLabel("Disappearing Messages")
        .accessibilityValue(chat.disappearingMessageDuration.title)
    }

    private var disappearingMessageDuration: Binding<PrototypeDisappearingMessageDuration> {
        Binding {
            chat.disappearingMessageDuration
        } set: { duration in
            updateChat {
                $0.setDisappearingMessages(duration, actorID: profile.id)
            }
        }
    }

    private var muteMenu: some View {
        Menu {
            if chat.listState.muteDuration != nil {
                Button("Unmute") { updateChat { $0.listState.muteDuration = nil } }
            } else {
                ForEach(ChatListItem.MuteDuration.allCases) { duration in
                    Button(duration.title) { updateChat { $0.listState.muteDuration = duration } }
                }
            }
        } label: {
            ChatInfoQuickActionIcon(
                systemName: chat.listState.muteDuration == nil ? "bell.slash" : "bell"
            )
        }
        .buttonStyle(ChatInfoSecondaryActionButtonStyle())
        .accessibilityLabel(muteActionTitle)
    }

    private func updateChat(_ mutation: (inout PrototypeChat) -> Void) {
        mutation(&profile.chats[chatIndex])
    }

    private func leaveGroup() {
        updateChat { _ = $0.leave(currentProfileID: profile.id) }
    }
}

private enum ChatInfoSharedContent: String, CaseIterable, Identifiable {
    case photos = "Photos & Videos"
    case links = "Links"
    case documents = "Documents"

    var id: Self { self }

    var systemImage: String {
        switch self {
        case .photos: "photo.on.rectangle.angled"
        case .links: "link"
        case .documents: "doc"
        }
    }
}

private struct ChatInfoQuickAction<Control: View>: View {
    let title: String
    let control: Control

    init(title: String, @ViewBuilder control: () -> Control) {
        self.title = title
        self.control = control()
    }

    var body: some View {
        VStack(spacing: 6) {
            control

            Text(title)
                .font(.footnote)
                .lineLimit(1)
                .accessibilityHidden(true)
        }
    }
}

private struct ChatInfoSecondaryActionButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .foregroundStyle(.primary)
            .padding(10)
            .background(
                configuration.isPressed
                    ? Color(uiColor: .secondarySystemFill)
                    : Color(uiColor: .secondarySystemGroupedBackground),
                in: .circle
            )
            .overlay {
                Circle()
                    .stroke(
                        Color(uiColor: .separator).opacity(0.35),
                        lineWidth: 0.5
                    )
            }
    }
}

private struct ChatInfoQuickActionIcon: View {
    let systemName: String

    var body: some View {
        Image(systemName: systemName)
            .font(.title3)
            .frame(width: 44, height: 44)
    }
}

private struct ChatInfoSharedContentLinks: View {
    @Binding var profile: PrototypeProfile
    let chatID: String
    let onOpenMessage: (String) -> Void

    var body: some View {
        Section("Shared in Chat") {
            ForEach(ChatInfoSharedContent.allCases) { category in
                NavigationLink {
                    ChatInfoSharedContentView(
                        profile: $profile,
                        chatID: chatID,
                        category: category,
                        onOpenMessage: onOpenMessage
                    )
                } label: {
                    Label(category.rawValue, systemImage: category.systemImage)
                }
            }
        }
    }
}

private struct ChatInfoTechnicalLinks: View {
    @Binding var profile: PrototypeProfile
    let settings: PrototypeSettingsState
    let chatID: String

    var body: some View {
        Section("Advanced") {
            ChatInfoTechnicalRows(
                profile: $profile,
                settings: settings,
                chatID: chatID
            )
        }
    }
}

private struct ChatInfoTechnicalRows: View {
    @Binding var profile: PrototypeProfile
    let settings: PrototypeSettingsState
    let chatID: String

    var body: some View {
        Group {
            NavigationLink {
                ChatRelaysView(profile: $profile, chatID: chatID)
            } label: {
                Label("Relays", systemImage: "network")
            }

            NavigationLink {
                ChatDeveloperToolsView(
                    profile: $profile,
                    settings: settings,
                    chatID: chatID
                )
            } label: {
                Label("Developer Tools", systemImage: "wrench.and.screwdriver")
            }
        }
    }
}

private struct ChatInfoSharedContentView: View {
    @Binding var profile: PrototypeProfile
    let chatID: String
    let category: ChatInfoSharedContent
    let onOpenMessage: (String) -> Void
    @State private var mediaSelection: PrototypeMediaSelection?
    @State private var pendingOpenMessageID: String?
    @State private var quickLookURL: URL?

    var body: some View {
        Group {
            switch category {
            case .photos:
                ScrollView {
                    ChatInfoMediaSection(items: mediaItems) {
                        guard $0.isAvailable else { return }
                        mediaSelection = PrototypeMediaSelection(
                            chat: chat,
                            selectedItemID: $0.id
                        )
                    }
                }
                .scrollIndicators(.hidden)
            case .links, .documents:
                List {
                    ChatInfoSharedCategorySections(
                        profile: $profile,
                        chatID: chatID,
                        category: category,
                        quickLookURL: $quickLookURL
                    )
                }
            }
        }
        .navigationTitle(category.rawValue)
        .navigationBarTitleDisplayMode(.inline)
        .sheet(item: $mediaSelection, onDismiss: openPendingMessage) { selection in
            PrototypeMediaViewer(
                profile: $profile,
                sourceChatID: chatID,
                selection: selection,
                onRequestOpenMessage: { messageID in
                    pendingOpenMessageID = messageID
                    mediaSelection = nil
                }
            )
        }
        .quickLookPreview($quickLookURL)
    }

    private var chat: PrototypeChat {
        profile.chats.first { $0.id == chatID }!
    }

    private var mediaItems: [PrototypeMediaItem] {
        PrototypeMediaIndex.allItems(in: chat)
    }

    private func openPendingMessage() {
        guard let messageID = pendingOpenMessageID else { return }
        pendingOpenMessageID = nil
        Task { @MainActor in
            await Task.yield()
            onOpenMessage(messageID)
        }
    }
}

private struct ChatInfoSharedCategorySections: View {
    @Binding var profile: PrototypeProfile
    let chatID: String
    let category: ChatInfoSharedContent
    @Binding var quickLookURL: URL?

    var body: some View {
        switch category {
        case .photos:
            EmptyView()
        case .links:
            ChatInfoLinksSection(attachments: linkAttachments)
        case .documents:
            ChatInfoDocumentsSection(attachments: documentAttachments) { url in
                quickLookURL = url
            }
        }
    }

    private var chat: PrototypeChat {
        profile.chats.first { $0.id == chatID }!
    }
    private var attachments: [PrototypeAttachment] {
        chat.messages
            .filter { !$0.isDeleted }
            .flatMap(\.attachments)
    }
    private var linkAttachments: [PrototypeAttachment] {
        attachments.filter {
            if case .link = $0 { return true }
            return false
        }
    }
    private var documentAttachments: [PrototypeAttachment] {
        attachments.filter {
            if case .file = $0 { return true }
            return false
        }
    }
}

private struct ChatInfoMediaSection: View {
    let items: [PrototypeMediaItem]
    let onOpen: (PrototypeMediaItem) -> Void
    private let columns = Array(
        repeating: GridItem(.flexible(), spacing: 2),
        count: 3
    )

    var body: some View {
        Group {
            if items.isEmpty {
                ContentUnavailableView(
                    "No Photos or Videos",
                    systemImage: "photo.on.rectangle.angled",
                    description: Text("Photos and videos shared in this chat will appear here.")
                )
                .frame(maxWidth: .infinity, minHeight: 420)
            } else {
                LazyVGrid(columns: columns, spacing: 2) {
                    ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                        Group {
                            if item.isAvailable {
                                Button { onOpen(item) } label: { mediaGridTile(item) }
                                    .buttonStyle(.borderless)
                            } else {
                                mediaGridTile(item)
                            }
                        }
                        .accessibilityElement(children: .combine)
                        .accessibilityLabel(
                            "\(item.attachment.accessibilityLabel), \(index + 1) of \(items.count)"
                        )
                        .accessibilityIdentifier("chat-info.media.\(item.id)")
                    }
                }
            }
        }
    }

    private func mediaGridTile(_ item: PrototypeMediaItem) -> some View {
        Color.clear
            .aspectRatio(1, contentMode: .fit)
            .overlay { mediaTile(item.attachment) }
            .clipped()
    }

    @ViewBuilder
    private func mediaTile(_ attachment: PrototypeAttachment) -> some View {
        ZStack {
            Color(uiColor: .secondarySystemBackground)
            switch attachment {
            case let .photo(_, source, _, _):
                PrototypeImageSourceView(source: source)
                    .scaledToFill()
            case let .video(_, _, thumbnail, duration, _):
                PrototypeImageSourceView(source: thumbnail)
                    .scaledToFill()
                Image(systemName: "play.circle.fill")
                    .font(.title)
                    .foregroundStyle(.white)
                    .shadow(radius: 2)
                Text(prototypeDurationString(duration))
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(.white)
                    .padding(4)
                    .background(.black.opacity(0.55), in: .capsule)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
                    .padding(5)
            case let .gif(_, assetName, _):
                Image(assetName)
                    .resizable()
                    .scaledToFill()
                Text("GIF")
                    .font(.caption2.bold())
                    .foregroundStyle(.white)
                    .padding(4)
                    .background(.black.opacity(0.55), in: .capsule)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
                    .padding(5)
            default:
                EmptyView()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .clipped()
    }
}

struct PrototypeMediaViewer: View {
    @Binding var profile: PrototypeProfile
    let sourceChatID: String
    let selection: PrototypeMediaSelection
    let onRequestOpenMessage: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selectedIndex: Int
    @State private var preparedMedia: ChatInfoPreparedMedia?
    @State private var exportDocument: ChatInfoMediaDocument?
    @State private var isShowingFileExporter = false
    @State private var isShowingForwardSheet = false
    @State private var isShowingSaveError = false
    @State private var isChromeVisible = true
    @State private var isCurrentImageZoomed = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    init(
        profile: Binding<PrototypeProfile>,
        sourceChatID: String,
        selection: PrototypeMediaSelection,
        onRequestOpenMessage: @escaping (String) -> Void
    ) {
        _profile = profile
        self.sourceChatID = sourceChatID
        self.selection = selection
        self.onRequestOpenMessage = onRequestOpenMessage
        _selectedIndex = State(
            initialValue: min(max(selection.initialIndex, 0), max(selection.items.count - 1, 0))
        )
    }

    var body: some View {
        NavigationStack {
            ScrollView(.horizontal) {
                LazyHStack(spacing: PrototypeMediaPagerLayout.pageSpacing) {
                    ForEach(
                        Array(selection.items.enumerated()),
                        id: \.element.id
                    ) { index, item in
                        PrototypeSingleMediaView(
                            attachment: item.attachment,
                            isSelected: index == selectedIndex,
                            onZoomStateChange: { isZoomed in
                                guard index == selectedIndex else { return }
                                isCurrentImageZoomed = isZoomed
                            }
                        )
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .containerRelativeFrame(.horizontal)
                        .accessibilityElement(children: .contain)
                        .accessibilityLabel(
                            accessibilityLabel(for: item, at: index)
                        )
                        .id(index)
                    }
                }
                .scrollTargetLayout()
            }
                .scrollIndicators(.hidden)
                .scrollTargetBehavior(
                    .viewAligned(limitBehavior: .alwaysByOne, anchor: .center)
                )
                .scrollPosition(id: selectedScrollIndex, anchor: .center)
                .scrollEdgeEffectHidden(true, for: .top)
                .background(Color(uiColor: .systemBackground))
                .simultaneousGesture(chromeToggleGesture)
                .onChange(of: selectedIndex) { _, _ in
                    isCurrentImageZoomed = false
                }
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button {
                            dismiss()
                        } label: {
                            ChatInfoMediaControlLabel(
                                title: "Close",
                                systemImage: "chevron.backward"
                            )
                        }
                        .accessibilityIdentifier("media-preview.close")
                    }

                    ToolbarItem(placement: .principal) {
                        VStack(spacing: 0) {
                            Text(senderName)
                                .font(.headline)
                            Text(
                                item.sentAt.formatted(
                                    date: .abbreviated,
                                    time: .shortened
                                )
                            )
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        }
                        .accessibilityElement(children: .combine)
                    }

                    ToolbarItem(placement: .primaryAction) {
                        Menu {
                            Button("Save", systemImage: "square.and.arrow.down") {
                                saveToFiles()
                            }
                            .disabled(preparedMedia == nil)

                            Button("Go to Message", systemImage: "bubble.left") {
                                goToMessage()
                            }
                        } label: {
                            ChatInfoMediaControlLabel(
                                title: "More",
                                systemImage: "ellipsis"
                            )
                        }
                        .accessibilityIdentifier("media-preview.more")
                    }

                }
                .safeAreaBar(edge: .bottom, spacing: 0) {
                    if isChromeVisible {
                        HStack {
                            if let preparedMedia {
                                ShareLink(item: preparedMedia.url) {
                                    ChatInfoMediaControlLabel(
                                        title: "Share",
                                        systemImage: "square.and.arrow.up"
                                    )
                                }
                                .buttonStyle(.plain)
                                .modifier(ChatInfoMediaBottomControl())
                                .accessibilityIdentifier("media-preview.share")
                            } else {
                                Button {} label: {
                                    ChatInfoMediaControlLabel(
                                        title: "Share",
                                        systemImage: "square.and.arrow.up"
                                    )
                                }
                                .buttonStyle(.plain)
                                .modifier(ChatInfoMediaBottomControl())
                                .accessibilityIdentifier("media-preview.share")
                                .disabled(true)
                            }

                            Spacer()

                            Button {
                                isShowingForwardSheet = true
                            } label: {
                                ChatInfoMediaControlLabel(
                                    title: "Forward",
                                    systemImage: "arrowshape.turn.up.right"
                                )
                            }
                            .buttonStyle(.plain)
                            .modifier(ChatInfoMediaBottomControl())
                            .accessibilityIdentifier("media-preview.forward")
                        }
                        .frame(maxWidth: .infinity)
                        .safeAreaPadding(.horizontal, 20)
                        .padding(.vertical, 8)
                    }
                }
        }
        .background(Color(uiColor: .systemBackground).ignoresSafeArea())
        .presentationDetents([.large])
        .interactiveDismissDisabled(isCurrentImageZoomed)
        .toolbar(isChromeVisible ? .visible : .hidden, for: .navigationBar)
        .toolbarBackground(.hidden, for: .navigationBar)
        .task(id: item.id) {
            preparedMedia = nil
            let result = await ChatInfoMediaPreparer.prepare(item.attachment)
            guard !Task.isCancelled else { return }
            preparedMedia = result
        }
        .sheet(isPresented: $isShowingForwardSheet) {
            NavigationStack {
                ChatInfoForwardMediaView(
                    profile: $profile,
                    sourceChatID: sourceChatID,
                    item: item
                )
            }
            .presentationDetents([.large])
        }
        .fileExporter(
            isPresented: $isShowingFileExporter,
            document: exportDocument,
            contentType: preparedMedia?.contentType ?? .data,
            defaultFilename: preparedMedia?.filename ?? "Media",
            onCompletion: handleSaveResult
        )
        .alert("Couldn’t Save Media", isPresented: $isShowingSaveError) {
            Button("Dismiss", role: .cancel) {}
        } message: {
            Text("Choose another location and try again.")
        }
    }

    private var item: PrototypeMediaItem {
        selection.items[min(max(selectedIndex, 0), max(selection.items.count - 1, 0))]
    }

    private var selectedScrollIndex: Binding<Int?> {
        Binding(
            get: { selectedIndex },
            set: { index in
                if let index {
                    selectedIndex = index
                }
            }
        )
    }

    private var senderName: String {
        if item.senderID == profile.id { return "You" }
        return profile.people.first { $0.id == item.senderID }?.name ?? "Unknown"
    }

    private var chromeToggleGesture: some Gesture {
        TapGesture().onEnded {
            withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.18)) {
                isChromeVisible.toggle()
            }
        }
    }

    private func accessibilityLabel(for item: PrototypeMediaItem, at index: Int) -> String {
        let kind: String
        let duration: String
        switch item.attachment {
        case .photo:
            kind = "Photo"
            duration = ""
        case let .video(_, _, _, seconds, _):
            kind = "Video"
            duration = ", " + prototypeDurationString(seconds)
        default:
            kind = "Media"
            duration = ""
        }
        let timestamp = item.sentAt.formatted(date: .abbreviated, time: .shortened)
        return "\(kind) from \(senderName), \(index + 1) of \(selection.items.count), \(timestamp)\(duration)"
    }

    private func saveToFiles() {
        guard let preparedMedia else { return }
        exportDocument = ChatInfoMediaDocument(data: preparedMedia.data)
        isShowingFileExporter = true
    }

    private func goToMessage() {
        onRequestOpenMessage(item.messageID)
    }

    private func handleSaveResult(_ result: Result<URL, Error>) {
        if case .failure = result {
            isShowingSaveError = true
        }
    }
}

private struct ChatInfoMediaControlLabel: View {
    let title: LocalizedStringKey
    let systemImage: String

    var body: some View {
        Label(title, systemImage: systemImage)
            .labelStyle(.iconOnly)
            .imageScale(.large)
    }
}

private struct ChatInfoMediaBottomControl: ViewModifier {
    func body(content: Content) -> some View {
        content
            .frame(width: 44, height: 44)
            .contentShape(.circle)
            .glassEffect(.regular.interactive(), in: .circle)
    }
}

private struct ChatInfoForwardMediaView: View {
    @Binding var profile: PrototypeProfile
    let sourceChatID: String
    let item: PrototypeMediaItem
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""
    @State private var message = ""
    @State private var selectedChatIDs: [String] = []
    @FocusState private var isSearchFocused: Bool
    @FocusState private var isMessageFocused: Bool

    var body: some View {
        List {
            if !selectedChats.isEmpty {
                selectedChatsSection
            }

            if filteredChats.isEmpty {
                ContentUnavailableView(
                    query.isEmpty ? "No Chats Available" : "No Results",
                    systemImage: query.isEmpty
                        ? "arrowshape.turn.up.right"
                        : "magnifyingglass",
                    description: Text(
                        query.isEmpty
                            ? "Start another chat before forwarding this media."
                            : "No chats match your search."
                    )
                )
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)
            } else {
                Section("Chats") {
                    ForEach(filteredChats) { chat in
                        Button {
                            toggleSelection(chat.id)
                            isSearchFocused = false
                            isMessageFocused = false
                        } label: {
                            HStack(spacing: 10) {
                                PrototypeChatAvatarView(
                                    avatar: chat.resolvedAvatar(people: profile.people),
                                    size: 40,
                                    publicKey: chat.resolvedAvatarPublicKey(people: profile.people)
                                )
                                Text(chat.title(people: profile.people))
                                    .foregroundStyle(.primary)
                                    .lineLimit(1)
                                Spacer()

                                Image(
                                    systemName: selectedChatIDs.contains(chat.id)
                                        ? "checkmark.circle.fill"
                                        : "circle"
                                )
                                .font(.title2)
                                .foregroundStyle(
                                    selectedChatIDs.contains(chat.id)
                                        ? Color.accentColor
                                        : Color.secondary
                                )
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .contentShape(.rect)
                        }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("forward.chat.\(chat.id)")
                        .accessibilityLabel(chat.title(people: profile.people))
                        .accessibilityValue(
                            selectedChatIDs.contains(chat.id)
                                ? "Selected"
                                : "Not selected"
                        )
                        .accessibilityAddTraits(
                            selectedChatIDs.contains(chat.id) ? .isSelected : []
                        )
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle("Forward To")
        .navigationBarTitleDisplayMode(.inline)
        .searchable(
            text: $query,
            placement: .navigationBarDrawer(displayMode: .always),
            prompt: "Search"
        )
        .searchFocused($isSearchFocused)
        .safeAreaBar(edge: .bottom, spacing: 0) {
            forwardingComposer
        }
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button {
                    dismiss()
                } label: {
                    Label("Close", systemImage: "xmark")
                        .labelStyle(.iconOnly)
                }
            }

            ToolbarItem(placement: .confirmationAction) {
                Button("Forward", action: forwardSelection)
                    .buttonStyle(.glassProminent)
                    .disabled(selectedChatIDs.isEmpty)
                    .accessibilityHint("Forwards the media to the selected chats.")
                    .accessibilityIdentifier("forward.send")
            }
        }
        .onSubmit(of: .search) {
            isSearchFocused = false
        }
    }

    private var selectedChatsSection: some View {
        Section {
            ScrollView(.horizontal) {
                LazyHStack(alignment: .top, spacing: 10) {
                    ForEach(selectedChats) { chat in
                        Button {
                            removeSelection(chat.id)
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                ZStack(alignment: .topTrailing) {
                                    PrototypeChatAvatarView(
                                        avatar: chat.resolvedAvatar(people: profile.people),
                                        size: 64,
                                        publicKey: chat.resolvedAvatarPublicKey(people: profile.people)
                                    )
                                    .frame(
                                        width: 72,
                                        height: 72,
                                        alignment: .bottomLeading
                                    )

                                    Image(systemName: "xmark.circle.fill")
                                        .font(.title3)
                                        .symbolRenderingMode(.palette)
                                        .foregroundStyle(.white, .black)
                                        .offset(x: -6, y: 6)
                                        .accessibilityHidden(true)
                                }
                                .frame(width: 72, height: 72)

                                Text(chat.title(people: profile.people))
                                    .font(.caption)
                                    .lineLimit(1)
                                    .frame(width: 72)
                            }
                            .frame(width: 72, alignment: .leading)
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(
                            "Remove \(chat.title(people: profile.people))"
                        )
                        .accessibilityHint("Removes this chat from the forward recipients.")
                    }
                }
                .padding(.vertical, 4)
            }
            .contentMargins(.horizontal, 20, for: .scrollContent)
            .defaultScrollAnchor(.leading, for: .initialOffset)
            .scrollIndicators(.hidden)
            .listRowInsets(EdgeInsets())
            .listRowBackground(Color.clear)
            .listRowSeparator(.hidden)
        } header: {
            Text("Selected")
                .padding(.horizontal, 20)
        }
        .listSectionMargins([.horizontal, .bottom], 0)
    }

    private var forwardingComposer: some View {
        GlassEffectContainer {
            TextField("Add a message", text: $message, axis: .vertical)
                .lineLimit(1...4)
                .focused($isMessageFocused)
                .submitLabel(.done)
                .onSubmit {
                    isMessageFocused = false
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 10)
                .frame(minHeight: 44)
                .glassEffect(
                    .regular.interactive(),
                    in: .rect(cornerRadius: 22)
                )
                .accessibilityIdentifier("forward.message")
        }
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .padding(.bottom, 8)
    }

    private var filteredChats: [PrototypeChat] {
        let search = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !search.isEmpty else { return destinationChats }
        return destinationChats.filter {
            $0.title(people: profile.people).localizedStandardContains(search)
        }
    }

    private var selectedChats: [PrototypeChat] {
        selectedChatIDs.compactMap { selectedID in
            destinationChats.first { $0.id == selectedID }
        }
    }

    private var destinationChats: [PrototypeChat] {
        profile.chats.filter {
            $0.id != sourceChatID
                && $0.composerAvailability(
                    currentProfileID: profile.id,
                    people: profile.people
                ) == .available
        }
    }

    private func toggleSelection(_ chatID: String) {
        if let index = selectedChatIDs.firstIndex(of: chatID) {
            selectedChatIDs.remove(at: index)
        } else {
            selectedChatIDs.append(chatID)
        }
    }

    private func removeSelection(_ chatID: String) {
        selectedChatIDs.removeAll { $0 == chatID }
    }

    private func forwardSelection() {
        let forwardedMessage = message.trimmingCharacters(in: .whitespacesAndNewlines)

        for chatID in selectedChatIDs {
            guard let index = profile.chats.firstIndex(where: { $0.id == chatID }) else {
                continue
            }
            profile.chats[index].appendMessage(
                authorID: profile.id,
                text: forwardedMessage,
                attachments: [chatInfoForwardedCopy(of: item.attachment)]
            )
        }
        dismiss()
    }
}

private struct ChatInfoPreparedMedia {
    let data: Data
    let contentType: UTType
    let filename: String
    let url: URL
}

private enum ChatInfoMediaPreparer {
    static func prepare(_ attachment: PrototypeAttachment) async -> ChatInfoPreparedMedia? {
        switch attachment {
        case let .photo(id, source, _, _):
            guard let data = await jpegData(for: source) else { return nil }
            return await prepared(
                data: data,
                contentType: .jpeg,
                filename: "Photo-\(id).jpg"
            )
        case let .video(id, url, _, _, _):
            guard let url,
                  let data = await readData(from: url)
            else { return nil }
            let contentType = UTType(filenameExtension: url.pathExtension) ?? .movie
            let filename = url.lastPathComponent.isEmpty
                ? "Video-\(id).mov"
                : url.lastPathComponent
            return await prepared(
                data: data,
                contentType: contentType,
                filename: filename
            )
        case let .gif(id, assetName, _):
            guard let data = await pngData(forAsset: assetName) else { return nil }
            return await prepared(
                data: data,
                contentType: .png,
                filename: "GIF-\(id).png"
            )
        default:
            return nil
        }
    }

    private static func jpegData(for source: PrototypeImageSource) async -> Data? {
        switch source {
        case let .asset(name):
            return await Task.detached(priority: .userInitiated) {
                UIImage(named: name)?.jpegData(compressionQuality: 0.92)
            }.value
        case let .data(data):
            return await Task.detached(priority: .userInitiated) {
                UIImage(data: data)?.jpegData(compressionQuality: 0.92)
            }.value
        }
    }

    private static func pngData(forAsset name: String) async -> Data? {
        await Task.detached(priority: .userInitiated) {
            UIImage(named: name)?.pngData()
        }.value
    }

    private static func readData(from url: URL) async -> Data? {
        await Task.detached(priority: .userInitiated) {
            try? Data(contentsOf: url)
        }.value
    }

    private static func prepared(
        data: Data,
        contentType: UTType,
        filename: String
    ) async -> ChatInfoPreparedMedia? {
        let url = FileManager.default.temporaryDirectory
            .appending(path: "chat-media-\(UUID().uuidString)-\(filename)")
        let wroteFile = await Task.detached(priority: .userInitiated) {
            do {
                try data.write(to: url, options: .atomic)
                return true
            } catch {
                return false
            }
        }.value
        guard wroteFile else { return nil }
        return ChatInfoPreparedMedia(
            data: data,
            contentType: contentType,
            filename: filename,
            url: url
        )
    }
}

private struct ChatInfoMediaDocument: FileDocument {
    static var readableContentTypes: [UTType] {
        [.image, .movie]
    }

    let data: Data

    init(data: Data) {
        self.data = data
    }

    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

private func chatInfoForwardedCopy(
    of attachment: PrototypeAttachment
) -> PrototypeAttachment {
    let id = UUID().uuidString
    switch attachment {
    case let .photo(_, source, label, dimensions):
        return .photo(id: id, source: source, label: label, dimensions: dimensions)
    case let .video(_, url, thumbnail, duration, dimensions):
        return .video(id: id, url: url, thumbnail: thumbnail, duration: duration, dimensions: dimensions)
    case let .gif(_, assetName, label):
        return .gif(id: id, assetName: assetName, label: label)
    default:
        return attachment
    }
}

private struct ChatInfoLinksSection: View {
    let attachments: [PrototypeAttachment]

    var body: some View {
        Section {
            if attachments.isEmpty {
                ContentUnavailableView(
                    "No Links",
                    systemImage: "link",
                    description: Text("Links shared in this chat will appear here.")
                )
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
            } else {
                ForEach(attachments) { attachment in
                    if case let .link(_, title, domain, summary, image) = attachment {
                        if let destination = chatInfoLinkDestination(domain: domain) {
                            Link(destination: destination) {
                                ChatInfoLinkRow(
                                    title: title,
                                    domain: domain,
                                    summary: summary,
                                    image: image
                                )
                            }
                            .buttonStyle(.plain)
                        } else {
                            ChatInfoLinkRow(
                                title: title,
                                domain: domain,
                                summary: summary,
                                image: image
                            )
                            .accessibilityElement(children: .combine)
                            .accessibilityLabel("Link unavailable, \(title), \(domain)")
                        }
                    }
                }
            }
        }
    }
}

private struct ChatInfoLinkRow: View {
    let title: String
    let domain: String
    let summary: String
    let image: PrototypeImageSource?

    var body: some View {
        HStack(spacing: 12) {
            if let image {
                PrototypeImageSourceView(source: image)
                    .scaledToFill()
                    .frame(width: 56, height: 56)
                    .clipped()
                    .clipShape(.rect(cornerRadius: 8))
            } else {
                Image(systemName: "link")
                    .font(.title3)
                    .frame(width: 56, height: 56)
                    .background(Color(uiColor: .secondarySystemBackground))
                    .clipShape(.rect(cornerRadius: 8))
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.headline)
                    .lineLimit(1)
                Text(domain)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                Text(summary)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
            }
            Spacer(minLength: 0)
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}

private struct ChatInfoDocumentsSection: View {
    let attachments: [PrototypeAttachment]
    let onOpen: (URL) -> Void

    var body: some View {
        Section {
            if attachments.isEmpty {
                ContentUnavailableView(
                    "No Documents",
                    systemImage: "doc",
                    description: Text("Documents shared in this chat will appear here.")
                )
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
            } else {
                ForEach(attachments) { attachment in
                    if case let .file(_, name, size, url) = attachment {
                        if let url {
                            Button {
                                onOpen(url)
                            } label: {
                                ChatInfoDocumentRow(name: name, size: size, isAvailable: true)
                            }
                            .buttonStyle(.plain)
                            .accessibilityHint("Opens a preview.")
                        } else {
                            ChatInfoDocumentRow(name: name, size: size, isAvailable: false)
                                .accessibilityElement(children: .combine)
                                .accessibilityLabel("Document, \(name), unavailable")
                        }
                    }
                }
            }
        }
    }
}

private struct ChatInfoDocumentRow: View {
    let name: String
    let size: Int
    let isAvailable: Bool

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "doc")
                .font(.title2)
                .frame(width: 36)
            VStack(alignment: .leading, spacing: 2) {
                Text(name)
                    .font(.headline)
                    .lineLimit(1)
                Text(chatInfoFileMetadata(name: name, size: size))
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: isAvailable ? "chevron.right" : "exclamationmark.triangle")
                .foregroundStyle(.tertiary)
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }
}

private func chatInfoLinkDestination(domain: String) -> URL? {
    guard !domain.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
          let url = URL(string: "https://\(domain)"),
          url.host?.isEmpty == false
    else { return nil }
    return url
}

private func chatInfoFileMetadata(name: String, size: Int) -> String {
    let fileType = URL(fileURLWithPath: name).pathExtension.uppercased()
    let formattedSize = size.formatted(.byteCount(style: .file))
    return fileType.isEmpty ? formattedSize : "\(fileType) • \(formattedSize)"
}

struct ChatRelaysView: View {
    @Binding var profile: PrototypeProfile
    let chatID: String
    @State private var isShowingAddRelay = false
    @State private var isShowingRestoreDefaultsConfirmation = false
    @State private var reconnectingRelayURLs: Set<String> = []
    @State private var addRelayDetent: PresentationDetent = .medium

    var body: some View {
        Form {
            Section {
                if chat.routing.relayURLs.isEmpty {
                    Text("This chat has no relays")
                        .foregroundStyle(.secondary)
                }

                ForEach(chat.routing.relayURLs, id: \.self) { relayURL in
                    NavigationLink {
                        ChatRelayDetailView(
                            profile: $profile,
                            chatID: chatID,
                            relayURL: relayURL
                        )
                    } label: {
                        relayRow(relayURL)
                    }
                }

                Button {
                    addRelayDetent = .medium
                    isShowingAddRelay = true
                } label: {
                    Label("Add Relay", systemImage: "plus.circle")
                }
            } footer: {
                Text("These relays are used only to deliver messages in this chat.")
            }

            Section {
                Button("Restore Default Relays", role: .destructive) {
                    isShowingRestoreDefaultsConfirmation = true
                }
                .disabled(chat.routing.isDefaultConfiguration)
            } footer: {
                Text("Restores the relay list this chat started with.")
            }
        }
        .navigationTitle("Relays")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $isShowingAddRelay) {
            AddChatRelaySheet(
                existingRelayURLs: chat.routing.relayURLs,
                presentationDetent: $addRelayDetent,
                onAdd: addRelay
            )
            .presentationDetents(
                [.medium, .large],
                selection: $addRelayDetent
            )
        }
        .alert(
            "Restore default relays?",
            isPresented: $isShowingRestoreDefaultsConfirmation
        ) {
            Button("Restore Defaults", role: .destructive) {
                reconnectingRelayURLs.removeAll()
                updateChat { $0.routing.restoreDefaults() }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(
                "This replaces this chat’s relay list with the relays it started with. Custom relays will be removed."
            )
        }
    }

    private var chatIndex: Int { profile.chats.firstIndex { $0.id == chatID }! }
    private var chat: PrototypeChat { profile.chats[chatIndex] }

    private func relayRow(_ relayURL: String) -> some View {
        let relay = chatRelayDescriptor(
            for: relayURL,
            profile: profile,
            isReconnecting: reconnectingRelayURLs.contains(relayURL)
        )

        return HStack(alignment: .center) {
            VStack(alignment: .leading, spacing: 2) {
                Text(relay.displayName)

                Text(relay.url)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }

            Spacer()

            ChatRelayConnectionStatusView(state: relay.connectionState)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(relay.displayName), \(relay.url)")
        .accessibilityValue(relay.connectionState.accessibilityTitle)
    }

    private func addRelay(_ relayURL: String) {
        guard let normalized = PrototypeChatRouting.normalized(relayURL) else {
            return
        }
        guard updateRouting({ $0.add(normalized) }) else {
            return
        }

        reconnectingRelayURLs.insert(normalized)

        Task { @MainActor in
            try? await Task.sleep(for: .seconds(1.5))
            _ = withAnimation {
                reconnectingRelayURLs.remove(normalized)
            }
        }
    }

    private func updateChat(_ mutation: (inout PrototypeChat) -> Void) {
        mutation(&profile.chats[chatIndex])
    }

    private func updateRouting(
        _ mutation: (inout PrototypeChatRouting) -> Bool
    ) -> Bool {
        mutation(&profile.chats[chatIndex].routing)
    }
}

private struct ChatRelayDescriptor {
    let displayName: String
    let url: String
    let connectionState: PrototypeRelayConnectionState
}

private func chatRelayDescriptor(
    for relayURL: String,
    profile: PrototypeProfile,
    isReconnecting: Bool = false
) -> ChatRelayDescriptor {
    let normalized = PrototypeChatRouting.normalized(relayURL) ?? relayURL
    let configuredRelay = profile.relayConfiguration.relays.first {
        PrototypeChatRouting.normalized($0.url) == normalized
    }

    return ChatRelayDescriptor(
        displayName: configuredRelay?.displayName
            ?? URL(string: normalized)?.host
            ?? "Custom Relay",
        url: normalized,
        connectionState: isReconnecting
            ? .reconnecting
            : (configuredRelay?.connectionState ?? .connected)
    )
}

private struct ChatRelayConnectionStatusView: View {
    let state: PrototypeRelayConnectionState

    var body: some View {
        Group {
            switch state {
            case .connected:
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
            case .reconnecting:
                ProgressView()
                    .controlSize(.regular)
                    .tint(.secondary)
            case .disconnected:
                Image(systemName: "xmark.circle.fill")
                    .foregroundStyle(.red)
            }
        }
        .imageScale(.medium)
        .accessibilityHidden(true)
    }
}

private extension PrototypeRelayConnectionState {
    var accessibilityTitle: String {
        switch self {
        case .connected: "Connected"
        case .reconnecting: "Reconnecting"
        case .disconnected: "Disconnected"
        }
    }
}

private struct ChatRelayDetailView: View {
    @Binding var profile: PrototypeProfile
    let chatID: String
    let relayURL: String
    @Environment(\.dismiss) private var dismiss
    @State private var isShowingRemovalConfirmation = false

    var body: some View {
        Form {
            Section {
                LabeledContent("Name", value: relay.displayName)

                LabeledContent("URL") {
                    Text(relay.url)
                        .font(.subheadline.monospaced())
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.trailing)
                }

                LabeledContent("Status") {
                    HStack {
                        Text(relay.connectionState.accessibilityTitle)
                            .foregroundStyle(.secondary)
                        ChatRelayConnectionStatusView(
                            state: relay.connectionState
                        )
                    }
                }
            }

            Section {
                Button("Remove Relay", role: .destructive) {
                    isShowingRemovalConfirmation = true
                }
            }
        }
        .navigationTitle("Relay")
        .navigationBarTitleDisplayMode(.inline)
        .alert(
            "Remove \(relay.displayName)?",
            isPresented: $isShowingRemovalConfirmation
        ) {
            Button("Remove Relay", role: .destructive, action: removeRelay)
            Button("Cancel", role: .cancel) {}
        } message: {
            Text(removalMessage)
        }
    }

    private var chatIndex: Int { profile.chats.firstIndex { $0.id == chatID }! }
    private var chat: PrototypeChat { profile.chats[chatIndex] }
    private var relay: ChatRelayDescriptor {
        chatRelayDescriptor(for: relayURL, profile: profile)
    }
    private var removalMessage: String {
        chat.routing.relayURLs.count == 1
            ? "Sending will stop until you add a relay."
            : "This chat will stop using this relay."
    }

    private func removeRelay() {
        profile.chats[chatIndex].routing.remove(relayURL)
        dismiss()
    }
}

private struct AddChatRelaySheet: View {
    let existingRelayURLs: [String]
    @Binding var presentationDetent: PresentationDetent
    let onAdd: (String) -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var relayURL = ""
    @FocusState private var relayURLIsFocused: Bool

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField(
                        "wss://relay.example.com",
                        text: $relayURL
                    )
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .keyboardType(.URL)
                    .focused($relayURLIsFocused)
                    .accessibilityIdentifier("chat-relays.input")
                } header: {
                    Text("Relay URL")
                } footer: {
                    Text("Enter a relay URL beginning with wss://.")
                }
            }
            .navigationTitle("Add Relay")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        onAdd(normalizedRelayURL)
                        dismiss()
                    }
                    .buttonStyle(.glassProminent)
                    .disabled(!canAddRelay)
                }
            }
        }
        .onChange(of: relayURLIsFocused) { _, isFocused in
            if isFocused {
                presentationDetent = .large
            }
        }
    }

    private var canAddRelay: Bool {
        guard PrototypeChatRouting.normalized(normalizedRelayURL) != nil else {
            return false
        }

        return !existingRelayURLs.contains {
            PrototypeChatRouting.normalized($0)
                == PrototypeChatRouting.normalized(normalizedRelayURL)
        }
    }

    private var normalizedRelayURL: String {
        PrototypeChatRouting.normalized(relayURL)
            ?? relayURL.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

private struct EditGroupView: View {
    @Binding var profile: PrototypeProfile
    let chatID: String
    @Environment(\.dismiss) private var dismiss
    @State private var name: String
    @State private var description: String
    @State private var avatar: ChatListItem.Avatar
    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var isWebPickerPresented = false

    init(profile: Binding<PrototypeProfile>, chatID: String) {
        _profile = profile
        self.chatID = chatID
        let chat = profile.wrappedValue.chats.first { $0.id == chatID }!
        _name = State(initialValue: chat.groupName)
        _description = State(initialValue: chat.groupDescription)
        _avatar = State(initialValue: chat.avatar)
    }

    var body: some View {
        Form {
            Section {
                VStack(spacing: 10) {
                    PrototypeChatAvatarView(avatar: avatar, size: 88)
                    Menu {
                        PhotosPicker(selection: $selectedPhotoItem, matching: .images) {
                            Label("Choose Photo", systemImage: "photo.on.rectangle")
                        }
                        Button { isWebPickerPresented = true } label: {
                            Label("Find Image on Web", systemImage: "globe")
                        }
                        if hasPhoto {
                            Button("Remove Photo", systemImage: "trash", role: .destructive) {
                                avatar = .systemSymbol("person.3.fill")
                            }
                        }
                    } label: { Text(hasPhoto ? "Change Photo" : "Add Photo") }
                }
                .frame(maxWidth: .infinity).listRowBackground(Color.clear)
            }
            Section("Group Details") {
                TextField("Group Name", text: $name)
                    .accessibilityIdentifier("edit-group.name")
                TextField("Description (Optional)", text: $description, axis: .vertical).lineLimit(2...5)
            }
        }
        .navigationTitle("Edit Group")
        .navigationBarTitleDisplayMode(.inline)
        .navigationBarBackButtonHidden(true)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) {
                Button("Cancel") { dismiss() }
            }
            ToolbarItem(placement: .confirmationAction) {
                Button("Save", action: save).disabled(!hasValidChanges)
            }
        }
        .task(id: selectedPhotoItem) {
            guard let item = selectedPhotoItem,
                  let data = try? await item.loadTransferable(type: Data.self),
                  let prepared = await ConversationImageProcessor.preparedDataAsync(from: data)
            else { return }
            avatar = .imageData(prepared)
            selectedPhotoItem = nil
        }
        .sheet(isPresented: $isWebPickerPresented) {
            AvatarWebImagePickerView(currentChoice: currentWebChoice) { choice in
                avatar = .asset(choice.assetName)
                isWebPickerPresented = false
            }
        }
    }

    private var chatIndex: Int { profile.chats.firstIndex { $0.id == chatID }! }
    private var original: PrototypeChat { profile.chats[chatIndex] }
    private var trimmedName: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var trimmedDescription: String { description.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var hasValidChanges: Bool {
        !trimmedName.isEmpty && (trimmedName != original.groupName || trimmedDescription != original.groupDescription || avatar != original.avatar)
    }
    private var currentWebChoice: AvatarWebImageChoice? {
        guard case let .asset(name) = avatar else { return nil }
        return AvatarWebImageCatalog.choices.first { $0.assetName == name }
    }
    private var hasPhoto: Bool {
        switch avatar {
        case .asset, .imageData: true
        case .monogram, .systemSymbol: false
        }
    }

    private func save() {
        let old = original
        if trimmedName != old.groupName {
            profile.chats[chatIndex].groupName = trimmedName
            profile.chats[chatIndex].appendEvent(
                .groupNameChanged(actorID: profile.id, name: trimmedName)
            )
        }
        if avatar != old.avatar {
            profile.chats[chatIndex].updateGroupPhoto(
                avatar,
                actorID: profile.id
            )
        }
        if trimmedDescription != old.groupDescription {
            profile.chats[chatIndex].groupDescription = trimmedDescription
            profile.chats[chatIndex].appendEvent(
                trimmedDescription.isEmpty
                    ? .groupDescriptionRemoved(actorID: profile.id)
                    : .groupDescriptionChanged(actorID: profile.id)
            )
        }
        dismiss()
    }
}

private struct AddPeopleToGroupView: View {
    @Binding var profile: PrototypeProfile
    let chatID: String
    @Environment(\.dismiss) private var dismiss
    @State private var query = ""
    @State private var selectedIDs: Set<String> = []

    var body: some View {
        List(filteredPeople) { person in
            Button {
                if selectedIDs.contains(person.id) { selectedIDs.remove(person.id) }
                else { selectedIDs.insert(person.id) }
            } label: {
                PersonRow(person: person, showsCheckmark: selectedIDs.contains(person.id))
            }
            .buttonStyle(.plain)
        }
        .overlay {
            if filteredPeople.isEmpty { ContentUnavailableView.search(text: query) }
        }
        .navigationTitle("Add People")
        .navigationBarTitleDisplayMode(.inline)
        .searchable(text: $query, prompt: "Search People")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                Button("Add", action: add).disabled(selectedIDs.isEmpty)
            }
        }
    }

    private var chatIndex: Int { profile.chats.firstIndex { $0.id == chatID }! }
    private var currentIDs: Set<String> { Set(profile.chats[chatIndex].members.map(\.personID)) }
    private var filteredPeople: [PrototypePerson] {
        profile.selectableChatPeople.filter {
            !currentIDs.contains($0.id) && (query.isEmpty || $0.name.localizedCaseInsensitiveContains(query) || $0.publicKey.localizedCaseInsensitiveContains(query))
        }
    }
    private func add() {
        let ids = profile.people.filter { selectedIDs.contains($0.id) }.map(\.id)
        _ = profile.chats[chatIndex].addMembers(personIDs: ids, actorID: profile.id)
        dismiss()
    }
}

struct GroupMemberView: View {
    @Binding var profile: PrototypeProfile
    @Binding var settings: PrototypeSettingsState
    let chatID: String
    let personID: String

    var body: some View {
        if member == nil {
            ContentUnavailableView(
                "Member Unavailable",
                systemImage: "person.crop.circle.badge.questionmark"
            )
            .navigationTitle("User Profile")
            .navigationBarTitleDisplayMode(.inline)
        } else if personID == profile.id {
            currentProfileView
        } else if profile.people.contains(where: { $0.id == personID }) {
            PersonProfileView(
                profile: $profile,
                settings: $settings,
                personID: personID,
                contextGroupID: chatID,
                onMessagePerson: { _ in },
                showsMessageAction: false
            )
        } else {
            ContentUnavailableView(
                "Profile Unavailable",
                systemImage: "person.crop.circle.badge.questionmark"
            )
            .navigationTitle("User Profile")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private var currentProfileView: some View {
        List {
            Section {
                ProfileIdentityHeader(
                    name: profile.name,
                    publicKey: profile.publicKey,
                    nostrAddress: profile.nostrAddress,
                    isNostrAddressVerified: profile.isNostrAddressVerified,
                    bottomPadding: 0,
                    showsIdentityValues: profile.about.isEmpty
                ) { size in
                    ProfileAvatarView(profile: profile, size: size)
                }
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets())

            if !profile.about.isEmpty {
                Section {
                    Text(profile.about)
                        .font(.subheadline)
                        .italic()
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                        .frame(maxWidth: .infinity, alignment: .center)
                }
                .listRowBackground(
                    Color(uiColor: .quaternarySystemFill).opacity(0.5)
                )

                Section {
                    ProfileIdentityValues(
                        publicKey: profile.publicKey,
                        nostrAddress: profile.nostrAddress,
                        isNostrAddressVerified: profile.isNostrAddressVerified
                    )
                    .padding(.bottom, 16)
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            }
        }
        .listSectionSpacing(profile.about.isEmpty ? 24 : 8)
        .navigationTitle(navigationTitle)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var navigationTitle: String {
        guard let member else { return "User Profile" }
        let role = member.role == .admin ? "Admin" : "Member"
        return "User Profile (\(role))"
    }

    private var member: PrototypeGroupMember? {
        guard let chat = profile.chats.first(where: { $0.id == chatID }) else {
            return nil
        }
        return chat.members.first { $0.personID == personID }
    }
}
