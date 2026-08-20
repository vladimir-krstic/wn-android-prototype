import SwiftUI
import UIKit

struct ConversationAttachmentMenuButton: UIViewRepresentable {
    let onCamera: () -> Void
    let onPhotosAndVideos: () -> Void
    let onFiles: () -> Void
    let onContact: () -> Void
    let onMenuVisibilityChanged: (Bool) -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(
            onCamera: onCamera,
            onPhotosAndVideos: onPhotosAndVideos,
            onFiles: onFiles,
            onContact: onContact,
            onMenuVisibilityChanged: onMenuVisibilityChanged
        )
    }

    func makeUIView(context: Context) -> AttachmentMenuButton {
        let button = AttachmentMenuButton(type: .custom)
        var configuration = UIButton.Configuration.plain()
        configuration.image = UIImage(systemName: "plus")
        configuration.baseForegroundColor = .label
        configuration.contentInsets = .zero
        button.configuration = configuration
        button.setPreferredSymbolConfiguration(
            UIImage.SymbolConfiguration(pointSize: 20, weight: .regular),
            forImageIn: .normal
        )
        button.menu = context.coordinator.makeMenu()
        button.showsMenuAsPrimaryAction = true
        button.preferredMenuElementOrder = .fixed
        button.onMenuVisibilityChanged = { [weak coordinator = context.coordinator] shown in
            coordinator?.onMenuVisibilityChanged(shown)
        }
        button.isAccessibilityElement = true
        button.accessibilityLabel = "Add Attachment"
        button.accessibilityHint = "Opens attachment options."
        button.accessibilityIdentifier = "conversation.attachment-menu"
        return button
    }

    func updateUIView(_ button: AttachmentMenuButton, context: Context) {
        context.coordinator.update(
            onCamera: onCamera,
            onPhotosAndVideos: onPhotosAndVideos,
            onFiles: onFiles,
            onContact: onContact,
            onMenuVisibilityChanged: onMenuVisibilityChanged
        )
    }

    @MainActor
    final class Coordinator {
        private var onCamera: () -> Void
        private var onPhotosAndVideos: () -> Void
        private var onFiles: () -> Void
        private var onContact: () -> Void
        private(set) var onMenuVisibilityChanged: (Bool) -> Void

        init(
            onCamera: @escaping () -> Void,
            onPhotosAndVideos: @escaping () -> Void,
            onFiles: @escaping () -> Void,
            onContact: @escaping () -> Void,
            onMenuVisibilityChanged: @escaping (Bool) -> Void
        ) {
            self.onCamera = onCamera
            self.onPhotosAndVideos = onPhotosAndVideos
            self.onFiles = onFiles
            self.onContact = onContact
            self.onMenuVisibilityChanged = onMenuVisibilityChanged
        }

        func update(
            onCamera: @escaping () -> Void,
            onPhotosAndVideos: @escaping () -> Void,
            onFiles: @escaping () -> Void,
            onContact: @escaping () -> Void,
            onMenuVisibilityChanged: @escaping (Bool) -> Void
        ) {
            self.onCamera = onCamera
            self.onPhotosAndVideos = onPhotosAndVideos
            self.onFiles = onFiles
            self.onContact = onContact
            self.onMenuVisibilityChanged = onMenuVisibilityChanged
        }

        func makeMenu() -> UIMenu {
            UIMenu(children: [
                UIAction(
                    title: "Camera",
                    image: UIImage(systemName: "camera")
                ) { [weak self] _ in
                    self?.onCamera()
                },
                UIAction(
                    title: "Photos and Videos",
                    image: UIImage(systemName: "photo.on.rectangle.angled")
                ) { [weak self] _ in
                    self?.onPhotosAndVideos()
                },
                UIAction(
                    title: "Files",
                    image: UIImage(systemName: "folder")
                ) { [weak self] _ in
                    self?.onFiles()
                },
                UIAction(
                    title: "Contact",
                    image: UIImage(systemName: "person.crop.circle")
                ) { [weak self] _ in
                    self?.onContact()
                }
            ])
        }
    }
}

@MainActor
final class AttachmentMenuButton: UIButton {
    var onMenuVisibilityChanged: (Bool) -> Void = { _ in }
    var keepsSourceVisibleDuringMenuPresentation = false {
        didSet {
            if keepsSourceVisibleDuringMenuPresentation {
                installContextMenuPreviewAnchorIfNeeded()
            }
        }
    }

    private weak var contextMenuPreviewAnchor: UIView?

    override func layoutSubviews() {
        super.layoutSubviews()
        contextMenuPreviewAnchor?.center = CGPoint(
            x: bounds.midX,
            y: bounds.midY
        )
    }

    private func installContextMenuPreviewAnchorIfNeeded() {
        guard contextMenuPreviewAnchor == nil else { return }
        let view = UIView(frame: CGRect(x: 0, y: 0, width: 1, height: 1))
        view.backgroundColor = .clear
        view.isUserInteractionEnabled = false
        view.isAccessibilityElement = false
        addSubview(view)
        contextMenuPreviewAnchor = view
        setNeedsLayout()
    }

    override func contextMenuInteraction(
        _ interaction: UIContextMenuInteraction,
        previewForHighlightingMenuWithConfiguration configuration: UIContextMenuConfiguration
    ) -> UITargetedPreview? {
        guard keepsSourceVisibleDuringMenuPresentation else {
            return super.contextMenuInteraction(
                interaction,
                previewForHighlightingMenuWithConfiguration: configuration
            )
        }
        return sourcePreservingMenuPreview()
    }

    override func contextMenuInteraction(
        _ interaction: UIContextMenuInteraction,
        previewForDismissingMenuWithConfiguration configuration: UIContextMenuConfiguration
    ) -> UITargetedPreview? {
        guard keepsSourceVisibleDuringMenuPresentation else {
            return super.contextMenuInteraction(
                interaction,
                previewForDismissingMenuWithConfiguration: configuration
            )
        }
        // No target means UIKit dismisses the menu without pivot-morphing it
        // into a composer view that SwiftUI may update during selection.
        return nil
    }

    override func contextMenuInteraction(
        _ interaction: UIContextMenuInteraction,
        willDisplayMenuFor configuration: UIContextMenuConfiguration,
        animator: (any UIContextMenuInteractionAnimating)?
    ) {
        super.contextMenuInteraction(
            interaction,
            willDisplayMenuFor: configuration,
            animator: animator
        )
        onMenuVisibilityChanged(true)
    }

    override func contextMenuInteraction(
        _ interaction: UIContextMenuInteraction,
        willEndFor configuration: UIContextMenuConfiguration,
        animator: (any UIContextMenuInteractionAnimating)?
    ) {
        super.contextMenuInteraction(
            interaction,
            willEndFor: configuration,
            animator: animator
        )
        guard let animator else {
            onMenuVisibilityChanged(false)
            return
        }
        animator.addCompletion { [weak self] in
            self?.onMenuVisibilityChanged(false)
        }
    }

    private func sourcePreservingMenuPreview() -> UITargetedPreview? {
        guard let anchor = contextMenuPreviewAnchor,
              anchor.window != nil,
              bounds.width.isFinite,
              bounds.height.isFinite,
              bounds.width > 0,
              bounds.height > 0 else {
            return nil
        }

        anchor.center = CGPoint(x: bounds.midX, y: bounds.midY)

        let parameters = UIPreviewParameters()
        parameters.backgroundColor = .clear
        let finitePath = UIBezierPath(rect: anchor.bounds)
        parameters.visiblePath = finitePath
        parameters.shadowPath = finitePath
        return UITargetedPreview(view: anchor, parameters: parameters)
    }
}
