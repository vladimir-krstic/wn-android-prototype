import PhotosUI
import SwiftUI
import UniformTypeIdentifiers
import UIKit

struct NewChatView: View {
    @Binding var profile: PrototypeProfile
    @Binding var settings: PrototypeSettingsState
    @State private var query = ""

    var body: some View {
        List {
            Section {
                NavigationLink(value: ChatsRoute.newGroup) {
                    Label("New Group", systemImage: "person.2")
                }
            }

            Section {
                ForEach(filteredPeople) { person in
                    NavigationLink(value: ChatsRoute.person(person.id)) {
                        PersonRow(person: person)
                    }
                }
            }
        }
        .overlay {
            if filteredPeople.isEmpty {
                ContentUnavailableView.search(text: query)
            }
        }
        .navigationTitle("New Chat")
        .navigationBarTitleDisplayMode(.inline)
        .searchable(text: $query, prompt: "Name or npub")
    }

    private var filteredPeople: [PrototypePerson] {
        let value = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty else { return profile.selectableChatPeople }
        return profile.selectableChatPeople.filter {
            $0.name.localizedCaseInsensitiveContains(value)
                || $0.publicKey.localizedCaseInsensitiveContains(value)
        }
    }
}

struct PersonRow: View {
    let person: PrototypePerson
    var showsCheckmark = false

    var body: some View {
        HStack {
            PrototypeChatAvatarView(
                avatar: person.avatar,
                size: 44,
                publicKey: person.publicKey
            )
            VStack(alignment: .leading, spacing: 2) {
                Text(person.name).font(.headline)
                Text(person.shortPublicKey).font(.caption).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer()
            if showsCheckmark {
                Image(systemName: "checkmark").foregroundStyle(.tint)
            }
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(showsCheckmark ? .isSelected : [])
    }
}

struct PersonProfileView: View {
    @Binding var profile: PrototypeProfile
    @Binding var settings: PrototypeSettingsState
    @Environment(\.dismiss) private var dismiss
    let personID: String
    var contextGroupID: String?
    let onMessagePerson: (String) -> Void
    var showsMessageAction = true

    @State private var isShowingAddToGroup = false
    @State private var isShowingBlockConfirmation = false
    @State private var pendingGroupAction: GroupMemberAction?

    private enum GroupMemberAction: String, Identifiable {
        case promote
        case demote
        case remove

        var id: Self { self }
    }

    var body: some View {
        List {
            Section {
                ProfileIdentityHeader(
                    name: person.name,
                    publicKey: person.publicKey,
                    nostrAddress: person.nostrAddress,
                    isNostrAddressVerified: person.isNostrAddressVerified,
                    bottomPadding: 0,
                    showsIdentityValues: person.about.isEmpty
                ) { size in
                    PrototypeChatAvatarView(
                        avatar: person.avatar,
                        size: size,
                        publicKey: person.publicKey
                    )
                }
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets())

            if !person.about.isEmpty {
                Section {
                    Text(person.about)
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
            }

            if !person.about.isEmpty {
                Section {
                    ProfileIdentityValues(
                        publicKey: person.publicKey,
                        nostrAddress: person.nostrAddress,
                        isNostrAddressVerified: person.isNostrAddressVerified
                    )
                    .padding(.bottom, 16)
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            }

            Section {
                if !sharedGroups.isEmpty {
                    NavigationLink {
                        GroupsInCommonView(
                            profile: $profile,
                            settings: $settings,
                            personID: personID
                        )
                    } label: {
                        GroupsInCommonLabel(groups: sharedGroups)
                    }
                } else {
                    Button(
                        "Add to Group",
                        systemImage: "person.2.badge.plus"
                    ) {
                        isShowingAddToGroup = true
                    }
                }

                Button(
                    person.isFollowing ? "Remove Contact" : "Add Contact",
                    systemImage: person.isFollowing ? "person.badge.minus" : "person.badge.plus"
                ) {
                    toggleContact()
                }

                Button(role: person.isBlocked ? nil : .destructive) {
                    if person.isBlocked { setBlocked(false) } else { isShowingBlockConfirmation = true }
                } label: {
                    Label(
                        person.isBlocked ? "Unblock" : "Block",
                        systemImage: person.isBlocked
                            ? "person.crop.circle.badge.checkmark"
                            : "person.crop.circle.badge.xmark"
                    )
                    .foregroundStyle(person.isBlocked ? Color.primary : Color.red)
                }
            } header: {
                if contextGroupMember != nil {
                    Text("Profile Actions")
                }
            }

            if showsMessageAction {
                Section {
                    Group {
                        if canOpenDirectChat {
                            Button {
                                onMessagePerson(personID)
                            } label: {
                                Label("Message", systemImage: "plus.bubble")
                                    .symbolRenderingMode(.monochrome)
                                    .foregroundStyle(Color(uiColor: .systemBackground))
                                    .frame(maxWidth: .infinity)
                            }
                            .buttonStyle(.glassProminent)
                            .controlSize(.large)
                            .accessibilityIdentifier("person-profile.message")
                        } else {
                            NavigationLink {
                                RelaysPrototypeView(configuration: $profile.relayConfiguration)
                            } label: {
                                Label(
                                    "Check Profile Relays",
                                    systemImage: "exclamationmark.triangle"
                                )
                            }
                        }
                    }
                    .padding(.top, person.about.isEmpty ? 0 : 16)
                }
                .listRowBackground(Color.clear)
                .listRowInsets(EdgeInsets())
            }

            if canManageContextGroupMember,
               let contextGroupMember {
                Section("Group Actions") {
                    Button {
                        pendingGroupAction = contextGroupMember.role == .admin
                            ? .demote
                            : .promote
                    } label: {
                        Label(
                            contextGroupMember.role == .admin
                                ? "Remove Admin"
                                : "Make Admin",
                            systemImage: contextGroupMember.role == .admin
                                ? "person.crop.circle.badge.minus"
                                : "person.crop.circle.badge.checkmark"
                        )
                    }

                    Button(role: .destructive) {
                        pendingGroupAction = .remove
                    } label: {
                        Label(
                            "Remove from Group",
                            systemImage: "minus.circle"
                        )
                        .foregroundStyle(.red)
                    }
                }
            }
        }
        .listSectionSpacing(person.about.isEmpty ? 24 : 8)
        .navigationTitle(navigationTitle)
        .navigationBarTitleDisplayMode(.inline)
        .alert(
            "Block \(person.name)?",
            isPresented: $isShowingBlockConfirmation
        ) {
            Button("Cancel", role: .cancel) {}
            Button("Block", role: .destructive) { setBlocked(true) }
        } message: {
            Text("You’ll keep the chat history, but you won’t be able to send messages until you unblock them.")
        }
        .alert(
            groupActionTitle,
            isPresented: groupActionIsPresented
        ) {
            Button("Cancel", role: .cancel) {
                pendingGroupAction = nil
            }
            Button(
                groupActionButtonTitle,
                role: pendingGroupAction == .remove ? .destructive : nil,
                action: performGroupAction
            )
        } message: {
            Text(groupActionMessage)
        }
        .sheet(isPresented: $isShowingAddToGroup) {
            NavigationStack {
                AddPersonToGroupView(
                    profile: $profile,
                    personID: personID
                )
            }
        }
    }

    private var personIndex: Int { profile.people.firstIndex { $0.id == personID }! }
    private var person: PrototypePerson { profile.people[personIndex] }
    private var existingDirectChatID: String? {
        profile.chats.first { chat in
            if case let .direct(id) = chat.kind { return id == personID }
            return false
        }?.id
    }
    private var canOpenDirectChat: Bool {
        existingDirectChatID != nil
            || !profile.relayConfiguration.availableChatMessageRelayURLs.isEmpty
    }
    private var sharedGroups: [PrototypeChat] {
        profile.groupsShared(with: personID)
    }
    private var contextGroupIndex: Int? {
        guard let contextGroupID else { return nil }
        return profile.chats.firstIndex { $0.id == contextGroupID }
    }
    private var contextGroup: PrototypeChat? {
        guard let contextGroupIndex else { return nil }
        return profile.chats[contextGroupIndex]
    }
    private var contextGroupMember: PrototypeGroupMember? {
        contextGroup?.members.first { $0.personID == personID }
    }
    private var navigationTitle: String {
        guard let contextGroupMember else { return "User Profile" }
        let role = contextGroupMember.role == .admin ? "Admin" : "Member"
        return "User Profile (\(role))"
    }
    private var canManageContextGroupMember: Bool {
        guard let contextGroup else { return false }
        return personID != profile.id
            && contextGroup.listState.membershipState == .active
            && contextGroup.isCurrentProfileAdmin(profile.id)
            && contextGroupMember != nil
    }
    private var groupActionIsPresented: Binding<Bool> {
        Binding {
            pendingGroupAction != nil
        } set: { isPresented in
            if !isPresented {
                pendingGroupAction = nil
            }
        }
    }
    private var groupActionTitle: String {
        switch pendingGroupAction {
        case .promote:
            "Make \(person.name) an Admin?"
        case .demote:
            "Remove \(person.name) as an Admin?"
        case .remove:
            "Remove \(person.name) from \(contextGroup?.title(people: profile.people) ?? "Group")?"
        case nil:
            ""
        }
    }
    private var groupActionButtonTitle: String {
        switch pendingGroupAction {
        case .promote: "Make Admin"
        case .demote: "Remove Admin"
        case .remove: "Remove from Group"
        case nil: ""
        }
    }
    private var groupActionMessage: String {
        switch pendingGroupAction {
        case .promote:
            "They’ll be able to manage group details and members."
        case .demote:
            "They’ll remain a member of this group."
        case .remove:
            "They’ll be removed from this group and won’t receive new messages."
        case nil:
            ""
        }
    }

    private func toggleContact() { profile.people[personIndex].isFollowing.toggle() }
    private func setBlocked(_ blocked: Bool) { profile.people[personIndex].isBlocked = blocked }
    private func performGroupAction() {
        guard let contextGroupIndex else {
            pendingGroupAction = nil
            return
        }

        switch pendingGroupAction {
        case .promote:
            _ = profile.chats[contextGroupIndex].promoteMember(
                personID: personID,
                actorID: profile.id
            )
        case .demote:
            _ = profile.chats[contextGroupIndex].demoteMember(
                personID: personID,
                actorID: profile.id
            )
        case .remove:
            _ = profile.chats[contextGroupIndex].removeMember(
                personID: personID,
                actorID: profile.id
            )
            pendingGroupAction = nil
            dismiss()
            return
        case nil:
            break
        }

        pendingGroupAction = nil
    }

}

struct NewGroupView: View {
    @Binding var profile: PrototypeProfile
    @Binding var settings: PrototypeSettingsState
    @State private var query = ""
    @State private var selectedIDs: [String] = []

    var body: some View {
        List {
            if !selectedPeople.isEmpty {
                Section {
                    ScrollView(.horizontal) {
                        LazyHStack(alignment: .top, spacing: 10) {
                            ForEach(selectedPeople) { person in
                                Button { removeSelection(person.id) } label: {
                                    VStack(alignment: .leading, spacing: 4) {
                                        ZStack(alignment: .topTrailing) {
                                            PrototypeChatAvatarView(
                                                avatar: person.avatar,
                                                size: 64,
                                                publicKey: person.publicKey
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

                                        Text(person.name)
                                            .font(.caption)
                                            .lineLimit(1)
                                            .frame(width: 72)
                                    }
                                    .frame(width: 72, alignment: .leading)
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel("Remove \(person.name)")
                                .accessibilityHint("Removes this person from the group.")
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

            Section {
                ForEach(filteredPeople) { person in
                    Button {
                        toggleSelection(person.id)
                    } label: {
                        PersonRow(person: person, showsCheckmark: selectedIDs.contains(person.id))
                    }
                    .buttonStyle(.plain)
                }
            }
        }
        .navigationTitle("New Group")
        .navigationBarTitleDisplayMode(.inline)
        .searchable(text: $query, prompt: "Search People")
        .toolbar {
            ToolbarItem(placement: .confirmationAction) {
                NavigationLink(value: ChatsRoute.newGroupSetup(selectedPeople.map(\.id))) {
                    Text("Continue")
                }
                .disabled(selectedIDs.isEmpty)
            }
        }
    }

    private var selectedPeople: [PrototypePerson] {
        selectedIDs.compactMap { id in
            profile.selectableChatPeople.first { $0.id == id }
        }
    }
    private var filteredPeople: [PrototypePerson] {
        let value = query.trimmingCharacters(in: .whitespacesAndNewlines)
        return profile.selectableChatPeople.filter {
            value.isEmpty || $0.name.localizedCaseInsensitiveContains(value)
                || $0.publicKey.localizedCaseInsensitiveContains(value)
        }
    }

    private func toggleSelection(_ personID: String) {
        if selectedIDs.contains(personID) {
            removeSelection(personID)
        } else {
            selectedIDs.append(personID)
        }
    }

    private func removeSelection(_ personID: String) {
        selectedIDs.removeAll { $0 == personID }
    }
}

struct NewGroupSetupView: View {
    @Binding var profile: PrototypeProfile
    @Binding var settings: PrototypeSettingsState
    let selectedPersonIDs: [String]
    let onCreateGroup: (String, String, ChatListItem.Avatar, [String]) -> Void

    @State private var name = ""
    @State private var description = ""
    @State private var selectedAvatar: ChatListItem.Avatar?
    @State private var avatarImage: UIImage?
    @State private var selectedPhotoItem: PhotosPickerItem?
    @State private var importedPhotoURL: URL?
    @State private var selectedWebChoice: AvatarWebImageChoice?
    @State private var isPhotosPickerPresented = false
    @State private var isFileImporterPresented = false
    @State private var isWebImagePickerPresented = false
    @State private var photoError: String?
    @State private var photoPreparationID: UUID?

    @FocusState private var focusedField: Field?

    private enum Field {
        case name
        case description
    }

    var body: some View {
        Form {
            avatarSection
                .frame(maxWidth: .infinity)
                .listRowBackground(Color.clear)
                .listRowSeparator(.hidden)

            Section("Group Details") {
                TextField("Group Name", text: $name)
                    .textContentType(.organizationName)
                    .submitLabel(.next)
                    .focused($focusedField, equals: .name)
                    .onSubmit { focusedField = .description }
                    .accessibilityIdentifier("new-group.name")
                TextField(
                    "Description (Optional)",
                    text: $description,
                    axis: .vertical
                )
                .lineLimit(2...5)
                .focused($focusedField, equals: .description)
            }

            Section("People") {
                ForEach(selectedPeople) { PersonRow(person: $0) }
            }
        }
        .formStyle(.grouped)
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle("Set Up Group")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .primaryAction) {
                Button("Create", action: createGroup)
                    .buttonStyle(.glassProminent)
                    .disabled(trimmedName.isEmpty || isPreparingPhoto)
                    .accessibilityIdentifier("new-group.create")
            }
        }
        .fileImporter(
            isPresented: $isFileImporterPresented,
            allowedContentTypes: [.image]
        ) { result in
            handleImportedFile(result)
        }
        .photosPicker(
            isPresented: $isPhotosPickerPresented,
            selection: $selectedPhotoItem,
            matching: .images
        )
        .task(id: selectedPhotoItem) {
            guard let selectedPhotoItem else { return }
            await prepareSelectedPhoto(selectedPhotoItem)
        }
        .task(id: importedPhotoURL) {
            guard let importedPhotoURL else { return }
            await prepareImportedPhoto(importedPhotoURL)
        }
        .sheet(isPresented: $isWebImagePickerPresented) {
            AvatarWebImagePickerView(
                currentChoice: selectedWebChoice,
                onUseImage: useWebImage
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
        .background {
            GroupSetupKeyboardDismissInstaller {
                focusedField = nil
            }
        }
    }

    private var avatarSection: some View {
        VStack(spacing: 0) {
            ProfileEditorAvatarView(
                name: name,
                image: avatarImage,
                emptySystemImage: "person.2",
                accessibilityName: "Group photo"
            )
            .containerRelativeFrame(
                .horizontal,
                count: 3,
                span: 1,
                spacing: 0
            )

            avatarMenu
                .padding(.top)

            if isPreparingPhoto {
                ProgressView("Preparing Photo")
                    .font(.footnote)
                    .padding(.top)
            }

            if let photoError {
                Text(photoError)
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .multilineTextAlignment(.center)
                    .padding(.top)
            }
        }
    }

    private var avatarMenu: some View {
        Menu {
            Button {
                isPhotosPickerPresented = true
            } label: {
                Label(
                    "Choose from Photos",
                    systemImage: "photo.on.rectangle"
                )
            }

            Button {
                isFileImporterPresented = true
            } label: {
                Label("Choose from Files", systemImage: "folder")
            }

            Button {
                isWebImagePickerPresented = true
            } label: {
                Label("Find Image on Web", systemImage: "globe")
            }

            if selectedAvatar != nil {
                Divider()

                Button(role: .destructive) {
                    removePhoto()
                } label: {
                    Label {
                        Text("Remove Photo")
                    } icon: {
                        Image(uiImage: destructiveTrashSymbol)
                    }
                }
            }
        } label: {
            Text(selectedAvatar == nil ? "Add Photo" : "Change Photo")
        }
        .buttonStyle(.glass)
        .disabled(isPreparingPhoto)
    }

    private var trimmedName: String { name.trimmingCharacters(in: .whitespacesAndNewlines) }
    private var selectedPeople: [PrototypePerson] {
        profile.selectableChatPeople.filter { selectedPersonIDs.contains($0.id) }
    }

    private var destructiveTrashSymbol: UIImage {
        UIImage(systemName: "trash")?
            .withTintColor(.systemRed, renderingMode: .alwaysOriginal)
            ?? UIImage()
    }

    private var isPreparingPhoto: Bool {
        photoPreparationID != nil
    }

    private func createGroup() {
        guard !trimmedName.isEmpty, !isPreparingPhoto else { return }
        focusedField = nil
        onCreateGroup(
            trimmedName,
            description,
            selectedAvatar ?? .monogram(prototypeGroupMonogram(trimmedName)),
            selectedPersonIDs
        )
    }

    private func prepareSelectedPhoto(
        _ selectedPhotoItem: PhotosPickerItem
    ) async {
        let preparationID = UUID()
        photoPreparationID = preparationID
        photoError = nil
        defer { finishPhotoPreparation(preparationID) }

        do {
            guard
                let data = try await selectedPhotoItem.loadTransferable(
                    type: Data.self
                ),
                let preparedData = await ProfileAvatarImageProcessor
                    .preparedDataAsync(from: data),
                !Task.isCancelled,
                photoPreparationID == preparationID,
                let image = UIImage(data: preparedData)
            else {
                if !Task.isCancelled,
                   photoPreparationID == preparationID {
                    showPhotoError()
                }
                return
            }

            avatarImage = image
            selectedAvatar = .imageData(preparedData)
            selectedWebChoice = nil
            self.selectedPhotoItem = nil
            importedPhotoURL = nil
        } catch is CancellationError {
            return
        } catch {
            if photoPreparationID == preparationID {
                showPhotoError()
            }
        }
    }

    private func handleImportedFile(_ result: Result<URL, Error>) {
        switch result {
        case let .success(url):
            importedPhotoURL = url
        case .failure:
            showPhotoError()
        }
    }

    private func prepareImportedPhoto(_ url: URL) async {
        let preparationID = UUID()
        photoPreparationID = preparationID
        photoError = nil

        let hasAccess = url.startAccessingSecurityScopedResource()
        defer {
            if hasAccess {
                url.stopAccessingSecurityScopedResource()
            }
            finishPhotoPreparation(preparationID)
        }

        do {
            guard
                let preparedData = try await ProfileAvatarImageProcessor
                    .preparedData(contentsOf: url),
                !Task.isCancelled,
                photoPreparationID == preparationID,
                let image = UIImage(data: preparedData)
            else {
                if !Task.isCancelled,
                   photoPreparationID == preparationID {
                    showPhotoError()
                }
                return
            }

            avatarImage = image
            selectedAvatar = .imageData(preparedData)
            selectedWebChoice = nil
            selectedPhotoItem = nil
            importedPhotoURL = nil
        } catch is CancellationError {
            return
        } catch {
            if photoPreparationID == preparationID {
                showPhotoError()
            }
        }
    }

    private func useWebImage(_ choice: AvatarWebImageChoice) {
        cancelPhotoPreparation()
        guard let image = UIImage(named: choice.assetName) else {
            showPhotoError()
            return
        }

        avatarImage = image
        selectedAvatar = .asset(choice.assetName)
        selectedWebChoice = choice
        selectedPhotoItem = nil
        importedPhotoURL = nil
        photoError = nil
    }

    private func removePhoto() {
        cancelPhotoPreparation()
        avatarImage = nil
        selectedAvatar = nil
        selectedWebChoice = nil
        selectedPhotoItem = nil
        importedPhotoURL = nil
        photoError = nil
    }

    private func showPhotoError() {
        selectedPhotoItem = nil
        importedPhotoURL = nil
        photoError = "Couldn't use that photo. Choose another image and try again."
    }

    private func finishPhotoPreparation(_ preparationID: UUID) {
        if photoPreparationID == preparationID {
            photoPreparationID = nil
        }
    }

    private func cancelPhotoPreparation() {
        photoPreparationID = nil
    }
}

func prototypeGroupMonogram(_ name: String) -> String {
    name
        .trimmingCharacters(in: .whitespacesAndNewlines)
        .first
        .map { String($0).uppercased() }
        ?? ""
}

private struct GroupSetupKeyboardDismissInstaller: UIViewRepresentable {
    let onDismiss: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(onDismiss: onDismiss)
    }

    func makeUIView(context: Context) -> WindowObserverView {
        let view = WindowObserverView()
        view.isUserInteractionEnabled = false
        let coordinator = context.coordinator
        view.onWindowChange = { window in
            coordinator.install(in: window)
        }
        return view
    }

    func updateUIView(_ uiView: WindowObserverView, context: Context) {
        context.coordinator.onDismiss = onDismiss
        context.coordinator.install(in: uiView.window)
    }

    static func dismantleUIView(
        _ uiView: WindowObserverView,
        coordinator: Coordinator
    ) {
        uiView.onWindowChange = nil
        coordinator.uninstall()
    }

    final class WindowObserverView: UIView {
        var onWindowChange: ((UIWindow?) -> Void)?

        override func didMoveToWindow() {
            super.didMoveToWindow()
            onWindowChange?(window)
        }
    }

    @MainActor
    final class Coordinator: NSObject, UIGestureRecognizerDelegate {
        var onDismiss: () -> Void
        weak var installedWindow: UIWindow?

        private lazy var recognizer: UITapGestureRecognizer = {
            let recognizer = UITapGestureRecognizer(
                target: self,
                action: #selector(handleTap)
            )
            recognizer.cancelsTouchesInView = false
            recognizer.delegate = self
            return recognizer
        }()

        init(onDismiss: @escaping () -> Void) {
            self.onDismiss = onDismiss
        }

        func install(in window: UIWindow?) {
            guard installedWindow !== window else { return }
            uninstall()
            window?.addGestureRecognizer(recognizer)
            installedWindow = window
        }

        func uninstall() {
            installedWindow?.removeGestureRecognizer(recognizer)
            installedWindow = nil
        }

        @objc private func handleTap() {
            onDismiss()
        }

        func gestureRecognizer(
            _ gestureRecognizer: UIGestureRecognizer,
            shouldReceive touch: UITouch
        ) -> Bool {
            var touchedView = touch.view
            while let view = touchedView {
                if view is UITextField || view is UITextView {
                    return false
                }
                touchedView = view.superview
            }
            return true
        }
    }
}

struct AddPersonToGroupView: View {
    @Binding var profile: PrototypeProfile
    let personID: String
    var title = "Add to Group"
    @Environment(\.dismiss) private var dismiss
    @State private var pendingGroupID: String?

    var body: some View {
        List {
            Section {
                ForEach(availableGroups) { chat in
                    Button {
                        pendingGroupID = chat.id
                    } label: {
                        GroupSummaryRow(chat: chat)
                    }
                    .buttonStyle(.plain)
                    .accessibilityHint("Adds this person to the group.")
                }
            }
        }
        .overlay {
            if availableGroups.isEmpty {
                ContentUnavailableView("No Available Groups", systemImage: "person.3")
            }
        }
        .navigationTitle(title)
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
        }
        .alert(
            "Add \(personName) to \(pendingGroupName)?",
            isPresented: isShowingAddConfirmation
        ) {
            Button("Cancel", role: .cancel) {}
            Button("Add", action: confirmAddition)
        } message: {
            Text("They’ll be added as a member of this group.")
        }
    }

    private var isShowingAddConfirmation: Binding<Bool> {
        Binding(
            get: { pendingGroupID != nil },
            set: { isPresented in
                if !isPresented {
                    pendingGroupID = nil
                }
            }
        )
    }

    private var personName: String {
        profile.people.first { $0.id == personID }?.name ?? "This person"
    }

    private var pendingGroupName: String {
        guard let pendingGroupID else { return "this group" }
        return profile.chats.first { $0.id == pendingGroupID }?.groupName
            ?? "this group"
    }

    private func confirmAddition() {
        guard
            let pendingGroupID,
            let index = profile.chats.firstIndex(where: { $0.id == pendingGroupID })
        else {
            self.pendingGroupID = nil
            return
        }

        let groupName = profile.chats[index].groupName
        guard profile.chats[index].addMembers(
            personIDs: [personID],
            actorID: profile.id
        ) else {
            self.pendingGroupID = nil
            return
        }

        self.pendingGroupID = nil
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        UIAccessibility.post(
            notification: .announcement,
            argument: "\(personName) added to \(groupName)"
        )
        dismiss()
    }

    private var availableGroups: [PrototypeChat] {
        profile.chats.filter {
            $0.isGroup && $0.listState.membershipState == .active
                && $0.isCurrentProfileAdmin(profile.id)
                && !$0.members.contains(where: { $0.personID == personID })
        }
    }
}

struct GroupsInCommonView: View {
    @Binding var profile: PrototypeProfile
    @Binding var settings: PrototypeSettingsState
    let personID: String
    @State private var isShowingAddToGroup = false

    var body: some View {
        List {
            Section {
                if sharedGroups.isEmpty {
                    Text("No groups in common.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(sharedGroups) { chat in
                        NavigationLink {
                            ConversationView(
                                profile: $profile,
                                settings: $settings,
                                chatID: chat.id
                            )
                        } label: {
                            GroupSummaryRow(chat: chat)
                        }
                    }
                }

                Button(
                    "Add to Another Group",
                    systemImage: "person.2.badge.plus"
                ) {
                    isShowingAddToGroup = true
                }
            }
        }
        .navigationTitle("Groups in Common")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $isShowingAddToGroup) {
            NavigationStack {
                AddPersonToGroupView(
                    profile: $profile,
                    personID: personID,
                    title: "Add to Another Group"
                )
            }
        }
    }

    private var sharedGroups: [PrototypeChat] {
        profile.groupsShared(with: personID)
    }
}

struct GroupsInCommonLabel: View {
    let groups: [PrototypeChat]

    var body: some View {
        HStack {
            GroupAvatarStack(groups: groups)

            Text("Groups in Common")
                .lineLimit(1)
                .layoutPriority(1)

            Spacer()
        }
        .foregroundStyle(.primary)
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
        .accessibilityValue(accessibilityValue)
    }

    private var accessibilityValue: String {
        switch groups.count {
        case 1:
            "1 group in common"
        default:
            "\(groups.count) groups in common"
        }
    }
}

private struct GroupAvatarStack: View {
    let groups: [PrototypeChat]

    var body: some View {
        HStack(spacing: -10) {
            ForEach(groups.prefix(3)) { chat in
                PrototypeChatAvatarView(
                    avatar: chat.avatar,
                    size: 32
                )
                .groupStackOutline()
            }

            if groups.count > 3 {
                Text("+\(groups.count - 3)")
                    .font(.caption2.weight(.semibold))
                    .monospacedDigit()
                    .foregroundStyle(.primary)
                    .frame(width: 32, height: 32)
                    .background(
                        Color(uiColor: .systemGray5),
                        in: Circle()
                    )
                    .groupStackOutline()
            }
        }
        .accessibilityHidden(true)
    }
}

private extension View {
    func groupStackOutline() -> some View {
        overlay {
            Circle()
                .stroke(
                    Color(uiColor: .secondarySystemGroupedBackground),
                    lineWidth: 2
                )
        }
    }
}

private struct GroupSummaryRow: View {
    let chat: PrototypeChat

    var body: some View {
        HStack {
            PrototypeChatAvatarView(
                avatar: chat.avatar,
                size: 56
            )

            VStack(alignment: .leading, spacing: 2) {
                Text(chat.groupName)
                    .font(.headline)
                    .lineLimit(1)

                Text(memberCountLabel)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }

            Spacer(minLength: 8)
        }
        .contentShape(.rect)
        .accessibilityElement(children: .combine)
    }

    private var memberCountLabel: String {
        chat.members.count == 1
            ? "1 member"
            : "\(chat.members.count) members"
    }
}
