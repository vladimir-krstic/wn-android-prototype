import SwiftUI
import UIKit

enum PrototypeMediaPagerLayout {
    static let pageSpacing: CGFloat = 32
}

struct PrototypeComposerMediaSelection: Identifiable {
    let attachments: [PrototypeAttachment]
    let initialItemID: String

    init?(
        attachments: [PrototypeAttachment],
        initialItemID: String
    ) {
        let visualAttachments = attachments.filter(
            \.prototypeIsComposerVisualMedia
        )
        guard visualAttachments.contains(where: { $0.id == initialItemID })
        else { return nil }
        self.attachments = visualAttachments
        self.initialItemID = initialItemID
    }

    var id: String { initialItemID }
}

struct PrototypeComposerMediaViewer: View {
    let selection: PrototypeComposerMediaSelection
    let onConfirm: (Set<String>) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var selectedItemID: String
    @State private var includedItemIDs: Set<String>

    private enum Layout {
        static let inclusionControlSize: CGFloat = 22
        static let inclusionControlHitSize: CGFloat = 44
        static let inclusionControlEdgePadding: CGFloat = 0
        static let thumbnailHeight: CGFloat = 44
        static let thumbnailMinimumWidth: CGFloat = 32
        static let thumbnailMaximumWidth: CGFloat = 72
        static let thumbnailCornerRadius: CGFloat = 7
        static let thumbnailSpacing: CGFloat = 6
        static let selectedThumbnailHorizontalPadding: CGFloat = 6
        static let thumbnailHorizontalMargin: CGFloat = 32
        static let thumbnailVerticalPadding: CGFloat = 14
        static let selectedThumbnailScale: CGFloat = 1.08

        static var navigatorHeight: CGFloat {
            thumbnailHeight * selectedThumbnailScale
                + thumbnailVerticalPadding * 2
        }
    }

    init(
        selection: PrototypeComposerMediaSelection,
        onConfirm: @escaping (Set<String>) -> Void
    ) {
        self.selection = selection
        self.onConfirm = onConfirm
        _selectedItemID = State(initialValue: selection.initialItemID)
        _includedItemIDs = State(
            initialValue: Set(selection.attachments.map(\.id))
        )
    }

    var body: some View {
        NavigationStack {
            GeometryReader { proxy in
                VStack(spacing: 0) {
                    mediaPager
                        .frame(
                            width: proxy.size.width,
                            height: max(
                                1,
                                proxy.size.height - thumbnailNavigatorHeight
                            )
                        )

                    if selection.attachments.count > 1 {
                        thumbnailNavigator
                            .frame(height: Layout.navigatorHeight)
                    }
                }
                .frame(
                    width: proxy.size.width,
                    height: proxy.size.height,
                    alignment: .top
                )
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button {
                        dismiss()
                    } label: {
                        Label("Cancel Media Changes", systemImage: "xmark")
                            .labelStyle(.iconOnly)
                    }
                    .accessibilityIdentifier(
                        "conversation.composer-media-preview.cancel"
                    )
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button {
                        onConfirm(includedItemIDs)
                        dismiss()
                    } label: {
                        Label("Apply Media Selection", systemImage: "checkmark")
                            .labelStyle(.iconOnly)
                    }
                    .buttonStyle(.glassProminent)
                    .tint(Color("AccentColor"))
                    .accessibilityIdentifier(
                        "conversation.composer-media-preview.apply"
                    )
                }
            }
            .navigationTitle("Preview")
            .navigationBarTitleDisplayMode(.inline)
            .toolbarBackgroundVisibility(.hidden, for: .navigationBar)
        }
        .background(Color(uiColor: .systemBackground).ignoresSafeArea())
    }

    private var thumbnailNavigatorHeight: CGFloat {
        selection.attachments.count > 1 ? Layout.navigatorHeight : 0
    }

    private var mediaPager: some View {
        ScrollView(.horizontal) {
            LazyHStack(spacing: PrototypeMediaPagerLayout.pageSpacing) {
                ForEach(
                    Array(selection.attachments.enumerated()),
                    id: \.element.id
                ) { index, attachment in
                    mediaPage(for: attachment, at: index)
                        .containerRelativeFrame(.horizontal)
                        .id(attachment.id)
                }
            }
            .scrollTargetLayout()
        }
        .scrollIndicators(.hidden)
        .scrollTargetBehavior(
            .viewAligned(limitBehavior: .alwaysByOne, anchor: .center)
        )
        .scrollPosition(id: selectedScrollItemID, anchor: .center)
        .scrollEdgeEffectHidden(true, for: .top)
    }

    private var selectedScrollItemID: Binding<String?> {
        Binding(
            get: { selectedItemID },
            set: { itemID in
                if let itemID {
                    selectedItemID = itemID
                }
            }
        )
    }

    private func mediaPage(
        for attachment: PrototypeAttachment,
        at index: Int
    ) -> some View {
        GeometryReader { proxy in
            let availableSize = CGSize(
                width: max(1, proxy.size.width),
                height: max(1, proxy.size.height)
            )
            let mediaSize = fittedMediaSize(
                for: attachment,
                in: availableSize
            )

            ZStack(alignment: .bottomTrailing) {
                PrototypeSingleMediaView(
                    attachment: attachment,
                    isSelected: attachment.id == selectedItemID
                )
                .frame(width: mediaSize.width, height: mediaSize.height)

                inclusionButton(for: attachment)
                    .padding(Layout.inclusionControlEdgePadding)
            }
            .frame(width: mediaSize.width, height: mediaSize.height)
            .frame(
                width: proxy.size.width,
                height: proxy.size.height,
                alignment: .center
            )
            .accessibilityElement(children: .contain)
            .accessibilityLabel(
                "\(attachment.accessibilityLabel), \(index + 1) of \(selection.attachments.count)"
            )
            .accessibilityHint(
                selection.attachments.count > 1
                    ? "Swipe left or right to view other media."
                    : ""
            )
        }
    }

    private func inclusionButton(
        for attachment: PrototypeAttachment
    ) -> some View {
        let isIncluded = includedItemIDs.contains(attachment.id)

        return Button {
            withAnimation(.snappy) {
                if isIncluded {
                    includedItemIDs.remove(attachment.id)
                } else {
                    includedItemIDs.insert(attachment.id)
                }
            }
        } label: {
            ZStack {
                Circle()
                    .fill(
                        isIncluded
                            ? Color("AccentColor")
                            : Color.black.opacity(0.36)
                    )
                Circle()
                    .stroke(.white, lineWidth: 1.5)

                if isIncluded {
                    Image(systemName: "checkmark")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(.white)
                }
            }
            .frame(
                width: Layout.inclusionControlSize,
                height: Layout.inclusionControlSize
            )
            .shadow(
                color: .black.opacity(0.22),
                radius: 1.5,
                y: 0.5
            )
            .frame(
                width: Layout.inclusionControlHitSize,
                height: Layout.inclusionControlHitSize
            )
            .contentShape(.circle)
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(attachment.accessibilityLabel) in message")
        .accessibilityValue(isIncluded ? "Included" : "Not included")
        .accessibilityHint(
            "Double-tap to \(isIncluded ? "exclude" : "include") this item. Changes apply when you confirm."
        )
        .accessibilityIdentifier(
            "conversation.composer-media-preview.include.\(attachment.id)"
        )
    }

    private func fittedMediaSize(
        for attachment: PrototypeAttachment,
        in availableSize: CGSize
    ) -> CGSize {
        let aspectRatio = attachment.prototypeComposerMediaAspectRatio
        let availableAspectRatio = availableSize.width / availableSize.height

        if availableAspectRatio > aspectRatio {
            return CGSize(
                width: availableSize.height * aspectRatio,
                height: availableSize.height
            )
        }

        return CGSize(
            width: availableSize.width,
            height: availableSize.width / aspectRatio
        )
    }

    private var thumbnailNavigator: some View {
        GeometryReader { geometry in
            ScrollViewReader { proxy in
                ScrollView(.horizontal) {
                    LazyHStack(spacing: Layout.thumbnailSpacing) {
                        ForEach(Array(selection.attachments.enumerated()), id: \.element.id) {
                            index,
                            attachment in
                            Button {
                                withAnimation(.snappy) {
                                    selectedItemID = attachment.id
                                }
                            } label: {
                                PrototypeComposerMediaThumbnail(
                                    attachment: attachment,
                                    height: Layout.thumbnailHeight,
                                    minimumWidth: Layout.thumbnailMinimumWidth,
                                    maximumWidth: Layout.thumbnailMaximumWidth,
                                    cornerRadius: Layout.thumbnailCornerRadius
                                )
                                .scaleEffect(
                                    attachment.id == selectedItemID
                                        ? Layout.selectedThumbnailScale
                                        : 1
                                )
                                .padding(
                                    .horizontal,
                                    attachment.id == selectedItemID
                                        ? Layout.selectedThumbnailHorizontalPadding
                                        : 0
                                )
                                .animation(.snappy, value: selectedItemID)
                                .zIndex(attachment.id == selectedItemID ? 1 : 0)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel(
                                "Show \(attachment.accessibilityLabel), \(index + 1) of \(selection.attachments.count)"
                            )
                            .accessibilityValue(
                                thumbnailAccessibilityValue(for: attachment)
                            )
                            .id(attachment.id)
                        }
                    }
                    .frame(
                        minWidth: max(
                            0,
                            geometry.size.width
                                - Layout.thumbnailHorizontalMargin * 2
                        )
                    )
                    .padding(.horizontal, Layout.thumbnailHorizontalMargin)
                    .padding(.vertical, Layout.thumbnailVerticalPadding)
                }
                .scrollIndicators(.hidden)
                .onChange(of: selectedItemID) { _, itemID in
                    withAnimation(.snappy) {
                        proxy.scrollTo(itemID, anchor: .center)
                    }
                }
                .task {
                    await Task.yield()
                    proxy.scrollTo(selectedItemID, anchor: .center)
                }
            }
        }
        .background(Color(uiColor: .systemBackground))
    }

    private func thumbnailAccessibilityValue(
        for attachment: PrototypeAttachment
    ) -> String {
        let inclusionValue = includedItemIDs.contains(attachment.id)
            ? "Included"
            : "Not included"
        return attachment.id == selectedItemID
            ? "Current, \(inclusionValue.lowercased())"
            : inclusionValue
    }
}

struct PrototypeComposerMediaThumbnail: View {
    let attachment: PrototypeAttachment
    let height: CGFloat
    let minimumWidth: CGFloat
    let maximumWidth: CGFloat
    let cornerRadius: CGFloat

    var body: some View {
        content
            .frame(width: previewWidth, height: height)
            .clipShape(.rect(cornerRadius: cornerRadius))
            .contentShape(.rect(cornerRadius: cornerRadius))
    }

    @ViewBuilder
    private var content: some View {
        switch attachment {
        case let .photo(_, source, _, _):
            PrototypeImageSourceView(source: source)
                .scaledToFill()
        case let .video(_, _, thumbnail, _, _):
            ZStack {
                PrototypeImageSourceView(source: thumbnail)
                    .scaledToFill()
                Image(systemName: "play.fill")
                    .font(.caption.bold())
                    .foregroundStyle(.white)
                    .padding(8)
                    .background(.black.opacity(0.58), in: .circle)
            }
        case let .gif(_, assetName, _):
            Image(assetName)
                .resizable()
                .scaledToFill()
                .overlay(alignment: .bottomLeading) {
                    Text("GIF")
                        .font(.caption2.bold())
                        .foregroundStyle(.white)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 3)
                        .background(.black.opacity(0.68), in: .capsule)
                        .padding(6)
                }
        case .file, .voice, .link, .contact:
            Color(uiColor: .secondarySystemFill)
        }
    }

    private var previewWidth: CGFloat {
        min(max(height * aspectRatio, minimumWidth), maximumWidth)
    }

    private var aspectRatio: CGFloat {
        attachment.prototypeComposerMediaAspectRatio
    }
}

extension PrototypeAttachment {
    var prototypeIsComposerVisualMedia: Bool {
        switch self {
        case .photo, .video, .gif: true
        case .file, .voice, .link, .contact: false
        }
    }

    var prototypeComposerMediaAspectRatio: CGFloat {
        switch self {
        case let .photo(_, source, _, dimensions):
            if let ratio = dimensions?.aspectRatio,
               ratio.isFinite,
               ratio > 0 {
                return CGFloat(ratio)
            }
            let sourceSize: CGSize? = switch source {
            case let .asset(name):
                UIImage(named: name)?.size
            case let .data(data):
                UIImage(data: data)?.size
            }

            guard let sourceSize,
                  sourceSize.width.isFinite,
                  sourceSize.height.isFinite,
                  sourceSize.width > 0,
                  sourceSize.height > 0
            else { return prototypeStoredMediaAspectRatio }
            return sourceSize.width / sourceSize.height
        case .video:
            return prototypeStoredMediaAspectRatio
        case let .gif(_, assetName, _):
            guard let size = UIImage(named: assetName)?.size,
                  size.height > 0
            else { return 1 }
            return size.width / size.height
        case .file, .voice, .link, .contact:
            return 1
        }
    }

    private var prototypeStoredMediaAspectRatio: CGFloat {
        guard let ratio = prototypeMediaDimensions?.aspectRatio,
              ratio.isFinite,
              ratio > 0
        else { return 1 }
        return CGFloat(ratio)
    }
}
