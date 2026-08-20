import SwiftUI
import UIKit

struct NativeChatList: UIViewRepresentable {
    struct Actions {
        let canOpen: (String) -> Bool
        let open: (String) -> Void
        let markRead: (String) -> Void
        let markUnread: (String) -> Void
        let togglePinned: (String) -> Void
        let mute: (String, ChatListItem.MuteDuration) -> Void
        let unmute: (String) -> Void
        let toggleArchive: (String) -> Void
        let leave: (String) -> Void
        let delete: (String) -> Void
    }

    let chats: [ChatListItem]
    let actions: Actions

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIView(context: Context) -> UICollectionView {
        var configuration = UICollectionLayoutListConfiguration(
            appearance: .plain
        )
        configuration.backgroundColor = .systemBackground
        configuration.showsSeparators = false
        configuration.leadingSwipeActionsConfigurationProvider = {
            [weak coordinator = context.coordinator] indexPath in
            coordinator?.leadingActions(at: indexPath)
        }
        configuration.trailingSwipeActionsConfigurationProvider = {
            [weak coordinator = context.coordinator] indexPath in
            coordinator?.trailingActions(at: indexPath)
        }

        let layout = UICollectionViewCompositionalLayout.list(
            using: configuration
        )
        let collectionView = UICollectionView(
            frame: .zero,
            collectionViewLayout: layout
        )
        collectionView.backgroundColor = .systemBackground
        collectionView.showsVerticalScrollIndicator = false
        collectionView.contentInsetAdjustmentBehavior = .always
        collectionView.topEdgeEffect.style = .hard
        collectionView.topEdgeEffect.isHidden = true
        collectionView.delegate = context.coordinator

        context.coordinator.configureDataSource(
            for: collectionView
        )
        context.coordinator.applySnapshot(animated: false)

        return collectionView
    }

    func updateUIView(
        _ collectionView: UICollectionView,
        context: Context
    ) {
        context.coordinator.parent = self
        context.coordinator.applySnapshot(animated: true)
    }

    @MainActor
    final class Coordinator: NSObject,
        UICollectionViewDelegate,
        UIAdaptivePresentationControllerDelegate
    {
        private enum Section {
            case main
        }

        var parent: NativeChatList

        private var dataSource:
            UICollectionViewDiffableDataSource<Section, String>?
        private weak var collectionView: UICollectionView?
        private var pendingCompletion: ((Bool) -> Void)?
        private var pendingSwipeClosedAction: (() -> Void)?
        private var swipeStateDisplayLink: CADisplayLink?
        private var displayedChatsByID: [String: ChatListItem] = [:]

        init(parent: NativeChatList) {
            self.parent = parent
        }

        func scrollViewDidScroll(_ scrollView: UIScrollView) {
            updateTopEdgeEffectVisibility(in: scrollView)
        }

        func scrollViewDidChangeAdjustedContentInset(
            _ scrollView: UIScrollView
        ) {
            updateTopEdgeEffectVisibility(in: scrollView)
        }

        func collectionView(
            _ collectionView: UICollectionView,
            shouldSelectItemAt indexPath: IndexPath
        ) -> Bool {
            guard let chat = chat(at: indexPath) else {
                return false
            }

            return parent.actions.canOpen(chat.id)
        }

        func collectionView(
            _ collectionView: UICollectionView,
            didSelectItemAt indexPath: IndexPath
        ) {
            guard let chat = chat(at: indexPath) else {
                return
            }

            collectionView.deselectItem(
                at: indexPath,
                animated: true
            )
            parent.actions.open(chat.id)
        }

        func configureDataSource(
            for collectionView: UICollectionView
        ) {
            self.collectionView = collectionView

            let registration = UICollectionView.CellRegistration<
                ChatListCell,
                String
            > { [weak self] cell, _, chatID in
                guard let chat = self?.chat(id: chatID) else {
                    return
                }

                cell.contentConfiguration = UIHostingConfiguration {
                    ChatListRow(chat: chat)
                }
                cell.backgroundConfiguration =
                    UIBackgroundConfiguration.clear()
                cell.accessories = []
                cell.accessibilityIdentifier = "chat.\(chatID)"
                cell.accessibilityLabel = Self.accessibilityLabel(for: chat)
            }

            dataSource = UICollectionViewDiffableDataSource<
                Section,
                String
            >(collectionView: collectionView) {
                collectionView,
                indexPath,
                chatID in
                collectionView.dequeueConfiguredReusableCell(
                    using: registration,
                    for: indexPath,
                    item: chatID
                )
            }
        }

        private static func accessibilityLabel(for chat: ChatListItem) -> String {
            var components = [chat.title]
            if chat.isMuted {
                components.append("Muted")
            }
            if chat.hasDisappearingMessages {
                components.append("Disappearing messages on")
            }
            if let author = chat.visiblePreviewAuthor {
                components.append("\(author): \(chat.visiblePreview)")
            } else if chat.isDraft && !chat.hasEndedMembership {
                components.append("Draft: \(chat.visiblePreview)")
            } else {
                components.append(chat.visiblePreview)
            }
            components.append(chat.timestamp)
            return components.joined(separator: ", ")
        }

        func applySnapshot(animated: Bool) {
            guard let dataSource else {
                return
            }

            let nextChatsByID = Dictionary(
                uniqueKeysWithValues: parent.chats.map { chat in
                    (chat.id, chat)
                }
            )
            let currentSnapshot = dataSource.snapshot()
            let currentIDs = currentSnapshot.itemIdentifiers
            let currentIDSet = Set(currentIDs)
            let nextIDs = parent.chats.map(\.id)
            let changedIDs = nextIDs.filter { id in
                guard
                    currentIDSet.contains(id),
                    let displayedChat = displayedChatsByID[id],
                    let nextChat = nextChatsByID[id]
                else {
                    return false
                }

                return displayedChat != nextChat
            }
            let pinChangedIDs = changedIDs.filter { id in
                guard
                    let displayedChat = displayedChatsByID[id],
                    let nextChat = nextChatsByID[id]
                else {
                    return false
                }

                return displayedChat.isPinned != nextChat.isPinned
            }
            let isPinReorder =
                !pinChangedIDs.isEmpty
                && currentIDs.count == nextIDs.count
                && Set(currentIDs) == Set(nextIDs)

            var snapshot = NSDiffableDataSourceSnapshot<Section, String>()
            snapshot.appendSections([.main])
            snapshot.appendItems(nextIDs)
            displayedChatsByID = nextChatsByID

            guard currentIDs != nextIDs else {
                guard !changedIDs.isEmpty else {
                    return
                }

                snapshot.reconfigureItems(changedIDs)
                dataSource.apply(
                    snapshot,
                    animatingDifferences: false
                )
                return
            }

            if isPinReorder {
                snapshot.reconfigureItems(changedIDs)
                UIView.performWithoutAnimation {
                    dataSource.applySnapshotUsingReloadData(snapshot)
                    collectionView?.layoutIfNeeded()
                }
                return
            }

            dataSource.apply(
                snapshot,
                animatingDifferences: animated
            ) { [weak dataSource] in
                guard
                    let dataSource,
                    !changedIDs.isEmpty
                else {
                    return
                }

                var refreshSnapshot = dataSource.snapshot()
                let visibleChangedIDs = changedIDs.filter {
                    refreshSnapshot.indexOfItem($0) != nil
                }

                guard !visibleChangedIDs.isEmpty else {
                    return
                }

                refreshSnapshot.reconfigureItems(visibleChangedIDs)
                dataSource.apply(
                    refreshSnapshot,
                    animatingDifferences: false
                )
            }
        }

        func leadingActions(
            at indexPath: IndexPath
        ) -> UISwipeActionsConfiguration? {
            guard let chat = chat(at: indexPath) else {
                return nil
            }

            let readAction: UIContextualAction
            if chat.isUnread {
                readAction = contextualAction(
                    title: "Read",
                    symbol: "message.fill",
                    color: .systemBlue
                ) { [weak self] completion in
                    self?.parent.actions.markRead(chat.id)
                    completion(true)
                }
            } else if !chat.isArchived {
                readAction = contextualAction(
                    title: "Unread",
                    symbol: "message.badge",
                    color: .systemBlue
                ) { [weak self] completion in
                    self?.parent.actions.markUnread(chat.id)
                    completion(true)
                }
            } else {
                return nil
            }

            var actions = [readAction]
            if !chat.isArchived {
                actions.append(pinAction(for: chat))
            }

            let configuration = UISwipeActionsConfiguration(
                actions: actions
            )
            configuration.performsFirstActionWithFullSwipe = true
            return configuration
        }

        func trailingActions(
            at indexPath: IndexPath
        ) -> UISwipeActionsConfiguration? {
            guard let chat = chat(at: indexPath) else {
                return nil
            }

            var contextualActions: [UIContextualAction] = []

            if chat.hasEndedMembership {
                contextualActions.append(deleteAction(for: chat))
            } else if chat.membershipState == .active && chat.isGroup {
                contextualActions.append(leaveAction(for: chat))
            }

            contextualActions.append(archiveAction(for: chat))

            if chat.membershipState == .active && !chat.isArchived {
                contextualActions.append(muteAction(for: chat))
            }

            let configuration = UISwipeActionsConfiguration(
                actions: contextualActions
            )
            configuration.performsFirstActionWithFullSwipe = false
            return configuration
        }

        func presentationControllerDidDismiss(
            _ presentationController: UIPresentationController
        ) {
            completePendingAction(performed: false)
        }

        private func chat(at indexPath: IndexPath) -> ChatListItem? {
            guard let chatID = dataSource?.itemIdentifier(
                for: indexPath
            ) else {
                return nil
            }

            return chat(id: chatID)
        }

        private func chat(id: String) -> ChatListItem? {
            displayedChatsByID[id]
        }

        private func archiveAction(
            for chat: ChatListItem
        ) -> UIContextualAction {
            contextualAction(
                title: chat.isArchived ? "Unarchive" : "Archive",
                symbol: "archivebox.fill",
                color: .systemGray
            ) { [weak self] completion in
                self?.parent.actions.toggleArchive(chat.id)
                completion(true)
            }
        }

        private func pinAction(
            for chat: ChatListItem
        ) -> UIContextualAction {
            contextualAction(
                title: chat.isPinned ? "Unpin" : "Pin",
                symbol: chat.isPinned ? "pin.slash.fill" : "pin.fill",
                color: .systemOrange
            ) { [weak self] completion in
                guard let self else {
                    completion(false)
                    return
                }

                performAfterContextualActionCloses(
                    completion: completion
                ) { [weak self] in
                    self?.parent.actions.togglePinned(chat.id)
                }
            }
        }

        private func muteAction(
            for chat: ChatListItem
        ) -> UIContextualAction {
            if chat.isMuted {
                return contextualAction(
                    title: "Unmute",
                    symbol: "bell.fill",
                    color: .systemIndigo
                ) { [weak self] completion in
                    self?.parent.actions.unmute(chat.id)
                    completion(true)
                }
            }

            return contextualAction(
                title: "Mute",
                symbol: "bell.slash.fill",
                color: .systemIndigo
            ) { [weak self] sourceView, completion in
                self?.presentMuteDialog(
                    for: chat,
                    sourceView: sourceView,
                    completion: completion
                )
            }
        }

        private func leaveAction(
            for chat: ChatListItem
        ) -> UIContextualAction {
            contextualAction(
                title: "Leave",
                symbol: "rectangle.portrait.and.arrow.right",
                color: .systemRed
            ) { [weak self] sourceView, completion in
                self?.presentDestructiveDialog(
                    title: "Leave “\(chat.title)”?",
                    message: "You’ll stop receiving new messages. "
                        + "This chat will remain on this device as read-only "
                        + "history until you delete it.",
                    confirmationTitle: "Leave Chat",
                    sourceView: sourceView,
                    completion: completion
                ) { [weak self] in
                    self?.parent.actions.leave(chat.id)
                }
            }
        }

        private func deleteAction(
            for chat: ChatListItem
        ) -> UIContextualAction {
            contextualAction(
                title: "Delete",
                symbol: "trash.fill",
                color: .systemRed
            ) { [weak self] sourceView, completion in
                self?.presentDestructiveDialog(
                    title: "Delete “\(chat.title)” from this device?",
                    message: "This permanently removes the chat and its "
                        + "messages from this device. Signing in again "
                        + "won’t restore them.",
                    confirmationTitle: "Delete Chat",
                    sourceView: sourceView,
                    completion: completion
                ) { [weak self] in
                    self?.parent.actions.delete(chat.id)
                }
            }
        }

        private func presentMuteDialog(
            for chat: ChatListItem,
            sourceView: UIView,
            completion: @escaping (Bool) -> Void
        ) {
            let alert = UIAlertController(
                title: "Mute Notifications",
                message: "Choose how long to mute \(chat.title).",
                preferredStyle: .actionSheet
            )

            for duration in ChatListItem.MuteDuration.allCases {
                alert.addAction(
                    UIAlertAction(
                        title: duration.title,
                        style: .default
                    ) { [weak self] _ in
                        self?.parent.actions.mute(chat.id, duration)
                        self?.completePendingAction(performed: true)
                    }
                )
            }

            addCancelAction(to: alert)
            present(
                alert,
                from: sourceView,
                completion: completion
            )
        }

        private func presentDestructiveDialog(
            title: String,
            message: String,
            confirmationTitle: String,
            sourceView: UIView,
            completion: @escaping (Bool) -> Void,
            action: @escaping () -> Void
        ) {
            let alert = UIAlertController(
                title: title,
                message: message,
                preferredStyle: .actionSheet
            )
            alert.addAction(
                UIAlertAction(
                    title: confirmationTitle,
                    style: .destructive
                ) { [weak self] _ in
                    action()
                    self?.completePendingAction(performed: true)
                }
            )

            addCancelAction(to: alert)
            present(
                alert,
                from: sourceView,
                completion: completion
            )
        }

        private func addCancelAction(to alert: UIAlertController) {
            alert.addAction(
                UIAlertAction(title: "Cancel", style: .cancel) {
                    [weak self] _ in
                    self?.completePendingAction(performed: false)
                }
            )
        }

        private func present(
            _ alert: UIAlertController,
            from sourceView: UIView,
            completion: @escaping (Bool) -> Void
        ) {
            guard let presenter = presenter(for: sourceView) else {
                completion(false)
                return
            }

            pendingCompletion = completion

            if let popover = alert.popoverPresentationController {
                popover.sourceView = sourceView
                popover.sourceRect = sourceView.bounds
                popover.permittedArrowDirections = []
            }

            presenter.present(alert, animated: true) {
                alert.presentationController?.delegate = self
            }
        }

        private func presenter(
            for sourceView: UIView
        ) -> UIViewController? {
            var presenter = sourceView.window?.rootViewController

            while let presented = presenter?.presentedViewController {
                presenter = presented
            }

            if let navigation = presenter as? UINavigationController {
                return navigation.visibleViewController
            }

            if let tab = presenter as? UITabBarController {
                return tab.selectedViewController
            }

            return presenter
        }

        private func completePendingAction(performed: Bool) {
            let completion = pendingCompletion
            pendingCompletion = nil
            completion?(performed)
        }

        private func performAfterContextualActionCloses(
            completion: @escaping (Bool) -> Void,
            action: @escaping () -> Void
        ) {
            pendingSwipeClosedAction = action
            completion(true)

            swipeStateDisplayLink?.invalidate()
            let displayLink = CADisplayLink(
                target: self,
                selector: #selector(checkForClosedSwipeAction)
            )
            swipeStateDisplayLink = displayLink
            displayLink.add(to: .main, forMode: .common)
        }

        @objc
        private func checkForClosedSwipeAction() {
            let hasOpenSwipe = collectionView?.visibleCells.contains {
                $0.configurationState.isSwiped
            } ?? false

            guard !hasOpenSwipe else {
                return
            }

            swipeStateDisplayLink?.invalidate()
            swipeStateDisplayLink = nil

            let action = pendingSwipeClosedAction
            pendingSwipeClosedAction = nil
            action?()
        }

        private func updateTopEdgeEffectVisibility(
            in scrollView: UIScrollView
        ) {
            let restingOffset = -scrollView.adjustedContentInset.top
            scrollView.topEdgeEffect.isHidden =
                scrollView.contentOffset.y <= restingOffset
        }

        private func contextualAction(
            title: String,
            symbol: String,
            color: UIColor,
            handler: @escaping (@escaping (Bool) -> Void) -> Void
        ) -> UIContextualAction {
            contextualAction(
                title: title,
                symbol: symbol,
                color: color
            ) { _, completion in
                handler(completion)
            }
        }

        private func contextualAction(
            title: String,
            symbol: String,
            color: UIColor,
            handler: @escaping (UIView, @escaping (Bool) -> Void) -> Void
        ) -> UIContextualAction {
            let action = UIContextualAction(
                style: .normal,
                title: nil
            ) { _, sourceView, completion in
                handler(sourceView, completion)
            }
            let image = UIImage(systemName: symbol)
            image?.accessibilityLabel = title
            action.image = image
            action.accessibilityLabel = title
            action.backgroundColor = color
            return action
        }
    }
}

@MainActor
private final class ChatListCell: UICollectionViewListCell {
    private var configuredHeight: CGFloat = 0

    override func updateConfiguration(
        using state: UICellConfigurationState
    ) {
        super.updateConfiguration(using: state)

        backgroundConfiguration = UIBackgroundConfiguration.clear()

        if state.isSwiped {
            contentView.backgroundColor = .secondarySystemFill
            contentView.layer.cornerRadius = bounds.height / 2
            contentView.layer.masksToBounds = true
        } else {
            contentView.backgroundColor = .systemBackground
            contentView.layer.cornerRadius = 0
            contentView.layer.masksToBounds = false
        }
    }

    override func layoutSubviews() {
        super.layoutSubviews()

        guard configuredHeight != bounds.height else {
            return
        }

        configuredHeight = bounds.height
        setNeedsUpdateConfiguration()
    }

    override func prepareForReuse() {
        super.prepareForReuse()
        configuredHeight = 0
    }
}
