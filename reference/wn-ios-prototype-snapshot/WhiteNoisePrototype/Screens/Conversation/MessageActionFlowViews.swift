import SwiftUI
import UIKit

enum PrototypeMessageContextAction: String, Identifiable {
    case retry
    case reply
    case forward
    case readAloud
    case stopReading
    case transcribeVoice
    case showTranscript
    case hideTranscript
    case copyTranscript
    case copy
    case select
    case info
    case delete

    var id: String { rawValue }

    var title: String {
        switch self {
        case .retry: "Retry Send"
        case .reply: "Reply"
        case .forward: "Forward"
        case .readAloud: "Read Aloud"
        case .stopReading: "Stop Reading"
        case .transcribeVoice: "Transcribe"
        case .showTranscript: "Show Transcript"
        case .hideTranscript: "Hide Transcript"
        case .copyTranscript: "Copy Transcript"
        case .copy: "Copy"
        case .select: "Select"
        case .info: "Info"
        case .delete: "Delete"
        }
    }

    var symbol: String {
        switch self {
        case .retry: "arrow.clockwise"
        case .reply: "arrowshape.turn.up.left"
        case .forward: "arrowshape.turn.up.right"
        case .readAloud: "speaker.wave.2"
        case .stopReading: "stop.fill"
        case .transcribeVoice: "text.bubble"
        case .showTranscript: "text.bubble"
        case .hideTranscript: "text.bubble.fill"
        case .copyTranscript: "doc.on.doc"
        case .copy: "doc.on.doc"
        case .select: "checkmark.circle"
        case .info: "info.circle"
        case .delete: "trash"
        }
    }

    var role: ButtonRole? { self == .delete ? .destructive : nil }
}

struct PrototypeMessageContextPresentation<Preview: View>: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.layoutDirection) private var layoutDirection

    let message: PrototypeMessage
    let outgoing: Bool
    let sourceFrame: CGRect
    let contentFrame: CGRect
    let currentProfileID: String
    let quickReactionEmoji: [String]
    let availableVoiceTranscript: String?
    let isVoiceTranscriptVisible: Bool
    let isReadingAloud: Bool
    let preview: Preview
    let onDismiss: () -> Void
    let onAction: (PrototypeMessageContextAction) -> Void
    let onReaction: (String) -> Void
    let onMoreReactions: () -> Void

    @State private var backdropIsPresented = false
    @State private var previewIsPresented = false
    @State private var menuScaleIsPresented = false
    @State private var menuOpacityIsPresented = false
    @State private var reactionSurfaceIsPresented = false
    @State private var reactionContentsArePresented = false
    @State private var isDismissing = false

    init(
        message: PrototypeMessage,
        outgoing: Bool,
        sourceFrame: CGRect,
        contentFrame: CGRect,
        currentProfileID: String,
        quickReactionEmoji: [String],
        availableVoiceTranscript: String?,
        isVoiceTranscriptVisible: Bool,
        isReadingAloud: Bool,
        @ViewBuilder preview: () -> Preview,
        onDismiss: @escaping () -> Void,
        onAction: @escaping (PrototypeMessageContextAction) -> Void,
        onReaction: @escaping (String) -> Void,
        onMoreReactions: @escaping () -> Void
    ) {
        self.message = message
        self.outgoing = outgoing
        self.sourceFrame = sourceFrame
        self.contentFrame = contentFrame
        self.currentProfileID = currentProfileID
        self.quickReactionEmoji = quickReactionEmoji
        self.availableVoiceTranscript = availableVoiceTranscript
        self.isVoiceTranscriptVisible = isVoiceTranscriptVisible
        self.isReadingAloud = isReadingAloud
        self.preview = preview()
        self.onDismiss = onDismiss
        self.onAction = onAction
        self.onReaction = onReaction
        self.onMoreReactions = onMoreReactions
    }

    var body: some View {
        GeometryReader { proxy in
            let layout = layout(in: proxy.size)

            ZStack {
                Rectangle()
                    .fill(.regularMaterial)
                    .overlay(Color.primary.opacity(backdropIsPresented ? 0.08 : 0))
                    .opacity(backdropIsPresented ? 1 : 0)
                    .ignoresSafeArea()
                    .contentShape(Rectangle())
                    .onTapGesture { dismiss() }

                preview
                    .frame(width: sourceFrame.width, height: sourceFrame.height)
                    .compositingGroup()
                    .scaleEffect(
                        reduceMotion
                            ? layout.previewScale
                            : (previewIsPresented
                                ? layout.previewScale
                                : Layout.initialPreviewScale),
                        anchor: horizontalAnchor
                    )
                    .position(
                        x: sourceFrame.midX,
                        y: reduceMotion
                            ? layout.previewCenterY
                            : (previewIsPresented ? layout.previewCenterY : sourceFrame.midY)
                    )
                    .opacity(reduceMotion ? (previewIsPresented ? 1 : 0) : 1)
                    .shadow(
                        color: .black.opacity(backdropIsPresented ? 0.2 : 0),
                        radius: 12,
                        y: 4
                    )
                    .allowsHitTesting(false)
                    .accessibilityHidden(true)

                reactionBar
                    .frame(width: layout.reactionWidth, height: Layout.reactionHeight)
                    .position(x: layout.accessoryCenterX, y: layout.reactionCenterY)
                    .allowsHitTesting(reactionSurfaceIsPresented && !isDismissing)

                actionMenu
                    .frame(width: Layout.menuWidth, height: layout.menuHeight)
                    .compositingGroup()
                    .scaleEffect(
                        reduceMotion ? 1 : (menuScaleIsPresented ? 1 : 0.2),
                        anchor: menuAnchor
                    )
                    .opacity(
                        reduceMotion
                            ? (menuOpacityIsPresented ? 1 : 0)
                            : (isDismissing ? 0 : (menuOpacityIsPresented ? 1 : 0.2))
                    )
                    .animation(.easeInOut(duration: 0.3), value: isDismissing)
                    .allowsHitTesting(menuScaleIsPresented && !isDismissing)
                    .position(
                        x: reduceMotion
                            ? layout.menuCenterX
                            : (previewIsPresented
                                ? layout.menuCenterX
                                : layout.sourceMenuCenterX),
                        y: reduceMotion
                            ? layout.menuCenterY
                            : (previewIsPresented
                                ? layout.menuCenterY
                                : layout.sourceMenuCenterY)
                    )
            }
            .onAppear {
                present(
                    previewWillShift: abs(layout.previewCenterY - sourceFrame.midY) > 0.5
                )
            }
        }
        .accessibilityElement(children: .contain)
    }

    private var contextActions: [PrototypeMessageContextAction] {
        var actions: [PrototypeMessageContextAction] = []
        if outgoing, message.deliveryState == .failed {
            actions.append(.retry)
        }
        actions.append(contentsOf: [.reply, .forward])
        if !outgoing, hasVoiceMessage, message.text.isEmpty {
            if availableVoiceTranscript == nil {
                actions.append(.transcribeVoice)
            } else if isVoiceTranscriptVisible {
                actions.append(contentsOf: [.hideTranscript, .copyTranscript])
            } else {
                actions.append(.showTranscript)
            }
        } else if !outgoing, !message.text.isEmpty {
            actions.append(isReadingAloud ? .stopReading : .readAloud)
            actions.append(hasVoiceMessage ? .copyTranscript : .copy)
        } else if !message.text.isEmpty {
            actions.append(hasVoiceMessage ? .copyTranscript : .copy)
        }
        actions.append(contentsOf: [.select, .info, .delete])
        return actions
    }

    private var hasVoiceMessage: Bool {
        message.attachments.contains { attachment in
            if case .voice = attachment { return true }
            return false
        }
    }

    private var localReaction: String? {
        message.reactions.first {
            $0.personIDs.contains(currentProfileID)
        }?.emoji
    }

    private var displayedQuickReactionEmoji: [String] {
        guard let localReaction,
              !quickReactionEmoji.contains(localReaction)
        else { return quickReactionEmoji }
        return quickReactionEmoji + [localReaction]
    }

    private var reactionBar: some View {
        ZStack {
            Color.clear
                .glassEffect(.regular.interactive(), in: .capsule)
                .opacity(reactionSurfaceIsPresented ? 1 : 0)
                .animation(
                    .easeInOut(
                        duration: reduceMotion
                            ? 0.12
                            : (reactionSurfaceIsPresented ? 0.2 : 0.4)
                    ),
                    value: reactionSurfaceIsPresented
                )

            HStack(spacing: 0) {
                ForEach(
                    Array(displayedQuickReactionEmoji.enumerated()),
                    id: \.offset
                ) { index, emoji in
                    Button {
                        dismiss { onReaction(emoji) }
                    } label: {
                        Text(emoji)
                            .font(.system(size: 28))
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .background {
                                if localReaction == emoji {
                                    Circle().fill(Color(uiColor: .systemFill))
                                }
                            }
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(emoji)
                    .accessibilityValue(localReaction == emoji ? "Selected" : "")
                    .accessibilityAddTraits(localReaction == emoji ? .isSelected : [])
                    .reactionEntryMotion(
                        isPresented: reactionContentsArePresented,
                        isDismissing: isDismissing,
                        index: index,
                        reduceMotion: reduceMotion
                    )
                }

                Button(action: onMoreReactions) {
                    Image(systemName: "ellipsis")
                        .font(.body.weight(.semibold))
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .background(Color(uiColor: .tertiarySystemFill), in: .circle)
                        .padding(5)
                }
                .buttonStyle(.plain)
                .accessibilityLabel("More Reactions")
                .reactionEntryMotion(
                    isPresented: reactionContentsArePresented,
                    isDismissing: isDismissing,
                    index: displayedQuickReactionEmoji.count,
                    reduceMotion: reduceMotion
                )
            }
            .padding(6)
        }
    }

    private var actionMenu: some View {
        VStack(spacing: 0) {
            ForEach(contextActions) { action in
                Button(role: action.role) {
                    dismiss { onAction(action) }
                } label: {
                    Label(action.title, systemImage: action.symbol)
                        .font(.body)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .frame(minHeight: Layout.actionHeight)
                        .padding(.horizontal, 24)
                        .foregroundStyle(action == .delete ? Color.red : Color.primary)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.vertical, 10)
        .glassEffect(.regular.interactive(), in: .rect(cornerRadius: 32))
    }

    private func present(previewWillShift: Bool) {
        if reduceMotion {
            withAnimation(.easeOut(duration: 0.15)) {
                backdropIsPresented = true
                previewIsPresented = true
                menuScaleIsPresented = true
                menuOpacityIsPresented = true
                reactionSurfaceIsPresented = true
                reactionContentsArePresented = true
            }
            return
        }

        withAnimation(.easeInOut(duration: 0.2)) {
            backdropIsPresented = true
        }
        withAnimation(
            .interpolatingSpring(
                duration: 0.4,
                bounce: 0.2,
                initialVelocity: 1
            )
        ) {
            previewIsPresented = true
            menuScaleIsPresented = true
        }
        withAnimation(.easeInOut(duration: 0.4)) {
            menuOpacityIsPresented = true
        }

        Task { @MainActor in
            if previewWillShift {
                try? await Task.sleep(for: .milliseconds(100))
            }
            guard !isDismissing else { return }
            withAnimation(.easeInOut(duration: 0.2)) {
                reactionSurfaceIsPresented = true
            }
            reactionContentsArePresented = true
        }
    }

    private func dismiss(completion: (() -> Void)? = nil) {
        guard !isDismissing else { return }
        isDismissing = true

        if reduceMotion {
            withAnimation(.easeIn(duration: 0.12)) {
                backdropIsPresented = false
                previewIsPresented = false
                menuOpacityIsPresented = false
                reactionSurfaceIsPresented = false
            }
        } else {
            withAnimation(.easeInOut(duration: 0.4)) {
                backdropIsPresented = false
                reactionSurfaceIsPresented = false
            }
            withAnimation(
                .interpolatingSpring(
                    duration: 0.4,
                    bounce: 0.2,
                    initialVelocity: 1
                )
            ) {
                previewIsPresented = false
            }
            withAnimation(.easeInOut(duration: 0.4)) {
                menuScaleIsPresented = false
            }
        }

        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(reduceMotion ? 120 : 400))
            onDismiss()
            completion?()
        }
    }

    private func layout(in size: CGSize) -> Layout {
        let verticalMargin: CGFloat = 12
        let gap: CGFloat = 12
        let menuHeight = CGFloat(contextActions.count) * Layout.actionHeight + 20
        let availablePreviewHeight = max(
            100,
            size.height
                - (verticalMargin * 2)
                - Layout.reactionHeight
                - menuHeight
                - (gap * 2)
        )
        let previewScale = min(1, availablePreviewHeight / max(sourceFrame.height, 1))
        let previewHeight = sourceFrame.height * previewScale
        let visibleContentBottom = contentFrame.maxY
            - (showsVisibleReactions
                ? PrototypeMessageBubbleMetrics.reactionPillVerticalInset
                : 0)
        let contentBottomOffset = visibleContentBottom - sourceFrame.midY
        let scaledContentBottomFromPreviewTop = (visibleContentBottom - sourceFrame.minY)
            * previewScale
        let groupHeight = Layout.reactionHeight
            + gap
            + scaledContentBottomFromPreviewTop
            + gap
            + menuHeight
        let preferredTop = sourceFrame.minY - Layout.reactionHeight - gap
        let maximumTop = max(verticalMargin, size.height - verticalMargin - groupHeight)
        let groupTop = min(max(preferredTop, verticalMargin), maximumTop)
        let reactionWidth = min(
            size.width - (Layout.horizontalMargin * 2),
            CGFloat(displayedQuickReactionEmoji.count + 1) * 44 + 12
        )
        let sourceAlignedEdgeX = alignedContentEdgeX(scale: Layout.initialPreviewScale)
        let alignedEdgeX = alignedContentEdgeX(scale: previewScale)
        let accessoryCenterX = alignedCenterX(
            edgeX: alignedEdgeX,
            width: reactionWidth,
            containerWidth: size.width
        )
        let sourceMenuCenterX = alignedCenterX(
            edgeX: sourceAlignedEdgeX,
            width: Layout.menuWidth,
            containerWidth: size.width
        )
        let menuCenterX = alignedCenterX(
            edgeX: alignedEdgeX,
            width: Layout.menuWidth,
            containerWidth: size.width
        )
        let previewCenterY = groupTop
            + Layout.reactionHeight
            + gap
            + (previewHeight / 2)

        return Layout(
            reactionWidth: reactionWidth,
            menuHeight: menuHeight,
            previewScale: previewScale,
            accessoryCenterX: accessoryCenterX,
            sourceMenuCenterX: sourceMenuCenterX,
            menuCenterX: menuCenterX,
            reactionCenterY: groupTop + (Layout.reactionHeight / 2),
            previewCenterY: previewCenterY,
            sourceMenuCenterY: sourceFrame.midY
                + (contentBottomOffset * Layout.initialPreviewScale)
                + gap
                + (menuHeight / 2),
            menuCenterY: previewCenterY
                + (contentBottomOffset * previewScale)
                + gap
                + (menuHeight / 2)
        )
    }

    private var showsVisibleReactions: Bool {
        !message.reactions.isEmpty && !message.isDeleted
    }

    private func alignedContentEdgeX(scale: CGFloat) -> CGFloat {
        if alignsToRightEdge {
            return sourceFrame.maxX
                - ((sourceFrame.maxX - contentFrame.maxX) * scale)
        }
        return sourceFrame.minX
            + ((contentFrame.minX - sourceFrame.minX) * scale)
    }

    private func alignedCenterX(
        edgeX: CGFloat,
        width: CGFloat,
        containerWidth: CGFloat
    ) -> CGFloat {
        let proposed = alignsToRightEdge
            ? edgeX - (width / 2)
            : edgeX + (width / 2)
        return min(
            max(proposed, Layout.horizontalMargin + (width / 2)),
            containerWidth - Layout.horizontalMargin - (width / 2)
        )
    }

    private var alignsToRightEdge: Bool {
        switch layoutDirection {
        case .leftToRight: outgoing
        case .rightToLeft: !outgoing
        @unknown default: outgoing
        }
    }

    private var horizontalAnchor: UnitPoint {
        alignsToRightEdge ? .trailing : .leading
    }

    private var menuAnchor: UnitPoint {
        alignsToRightEdge ? .topTrailing : .topLeading
    }

    private struct Layout {
        static var initialPreviewScale: CGFloat { 0.95 }
        static var horizontalMargin: CGFloat { 16 }
        static var reactionHeight: CGFloat { 56 }
        static var menuWidth: CGFloat { 250 }
        static var actionHeight: CGFloat { 50 }

        let reactionWidth: CGFloat
        let menuHeight: CGFloat
        let previewScale: CGFloat
        let accessoryCenterX: CGFloat
        let sourceMenuCenterX: CGFloat
        let menuCenterX: CGFloat
        let reactionCenterY: CGFloat
        let previewCenterY: CGFloat
        let sourceMenuCenterY: CGFloat
        let menuCenterY: CGFloat
    }
}

private extension View {
    func reactionEntryMotion(
        isPresented: Bool,
        isDismissing: Bool,
        index: Int,
        reduceMotion: Bool
    ) -> some View {
        opacity(isPresented ? 1 : 0)
            .offset(y: reduceMotion || isPresented ? 0 : 24)
            .animation(
                reduceMotion
                    ? .easeOut(duration: 0.15)
                    : .easeIn(duration: 0.2).delay(Double(index) * 0.01),
                value: isPresented
            )
            .opacity(isDismissing ? 0 : 1)
            .animation(
                .easeInOut(duration: reduceMotion ? 0.12 : 0.4),
                value: isDismissing
            )
    }
}

struct PrototypeEmojiPickerView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    @Binding var quickReactionEmoji: [String]
    var showsConfigurationAction = true
    let onSelect: (String) -> Void

    @State private var query = ""
    @State private var selectedSectionID = "recents"
    @State private var isConfiguringReactions = false
    @State private var isSearchFocused = false

    var body: some View {
        ScrollViewReader { proxy in
            VStack(spacing: 0) {
                searchHeader
                    .padding(.top, 8)

                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 16) {
                        ForEach(filteredSections) { section in
                            VStack(alignment: .leading, spacing: 4) {
                                Text(section.title)
                                    .font(.subheadline.weight(.semibold))
                                    .foregroundStyle(.secondary)
                                    .accessibilityAddTraits(.isHeader)

                                LazyVGrid(columns: columns, spacing: 2) {
                                    ForEach(section.emoji, id: \.self) { emoji in
                                        Button {
                                            onSelect(emoji)
                                            dismiss()
                                        } label: {
                                            Text(emoji)
                                                .font(.system(size: 30))
                                                .frame(maxWidth: .infinity)
                                                .frame(height: 44)
                                                .contentShape(Rectangle())
                                        }
                                        .buttonStyle(.plain)
                                        .accessibilityLabel(emoji)
                                    }
                                }
                            }
                            .id(section.id)
                        }

                        if filteredSections.isEmpty {
                            ContentUnavailableView.search(text: query)
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .safeAreaPadding(.horizontal)
                    .padding(.vertical, 12)
                }
                .scrollDismissesKeyboard(.interactively)
                .safeAreaBar(edge: .bottom, spacing: 0) {
                    if query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        GlassEffectContainer(spacing: 0) {
                            HStack(spacing: 0) {
                                ForEach(visibleSections) { section in
                                    let isSelected = selectedSectionID == section.id

                                    Button {
                                        isSearchFocused = false
                                        selectedSectionID = section.id
                                        withAnimation(.easeInOut(duration: 0.2)) {
                                            proxy.scrollTo(section.id, anchor: .top)
                                        }
                                    } label: {
                                        Image(systemName: section.symbol)
                                            .font(.body.weight(.medium))
                                            .foregroundStyle(.primary)
                                            .frame(maxWidth: .infinity, minHeight: 44)
                                            .background {
                                                if isSelected {
                                                    Circle()
                                                        .fill(Color(uiColor: .systemFill))
                                                        .frame(width: 36, height: 36)
                                                }
                                            }
                                            .contentShape(Rectangle())
                                    }
                                    .buttonStyle(.plain)
                                    .accessibilityLabel(section.title)
                                    .accessibilityValue(isSelected ? "Selected" : "")
                                    .accessibilityAddTraits(isSelected ? .isSelected : [])
                                }
                            }
                            .padding(4)
                            .frame(maxWidth: .infinity)
                            .glassEffect(.regular.interactive(), in: .capsule)
                            .animation(.easeInOut(duration: 0.2), value: selectedSectionID)
                        }
                        .safeAreaPadding(.horizontal)
                        .padding(.vertical, 8)
                    }
                }
            }
        }
        .accessibilityAction(.escape) {
            dismiss()
        }
        .sheet(isPresented: $isConfiguringReactions) {
            PrototypeConfigureReactionsView(
                quickReactionEmoji: $quickReactionEmoji
            )
            .presentationDetents([.large])
            .presentationDragIndicator(.visible)
        }
    }

    private var searchHeader: some View {
        HStack(spacing: 4) {
            PrototypeEmojiSearchBar(
                text: $query,
                isFocused: $isSearchFocused
            )
            .frame(maxWidth: .infinity)

            if showsConfigurationAction {
                Button {
                    isSearchFocused = false
                    isConfiguringReactions = true
                } label: {
                    Image(systemName: "gearshape")
                }
                .buttonStyle(.glass)
                .buttonBorderShape(.circle)
                .controlSize(.large)
                .accessibilityLabel("Configure Reactions")
            }
        }
        .safeAreaPadding(.leading, 8)
        .safeAreaPadding(.trailing, showsConfigurationAction ? nil : 8)
    }

    private var columns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: 0),
            count: dynamicTypeSize.isAccessibilitySize ? 6 : 8
        )
    }

    private var visibleSections: [PrototypeEmojiSection] {
        var result = [PrototypeEmojiCatalog.recents]
        result.append(contentsOf: PrototypeEmojiCatalog.categories)
        return result
    }

    private var filteredSections: [PrototypeEmojiSection] {
        let search = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !search.isEmpty else { return visibleSections }
        return visibleSections.compactMap { section in
            let matchesWholeSection = section.title.localizedCaseInsensitiveContains(search)
                || section.keywords.localizedCaseInsensitiveContains(search)
            let emoji = matchesWholeSection
                ? section.emoji
                : section.emoji.filter { value in
                    value.contains(search)
                        || (PrototypeEmojiCatalog.aliases[value] ?? "")
                            .localizedCaseInsensitiveContains(search)
                }
            guard !emoji.isEmpty else { return nil }
            return PrototypeEmojiSection(
                id: section.id,
                title: section.title,
                symbol: section.symbol,
                keywords: section.keywords,
                emoji: emoji
            )
        }
    }
}

private struct PrototypeEmojiSearchBar: UIViewRepresentable {
    @Binding var text: String
    @Binding var isFocused: Bool

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIView(context: Context) -> UISearchBar {
        let searchBar = UISearchBar()
        searchBar.delegate = context.coordinator
        searchBar.searchBarStyle = .minimal
        searchBar.placeholder = "Search"
        searchBar.showsCancelButton = false
        searchBar.searchTextField.clearButtonMode = .never
        searchBar.searchTextField.returnKeyType = .search
        searchBar.searchTextField.accessibilityLabel = "Search Emoji"
        return searchBar
    }

    func updateUIView(_ searchBar: UISearchBar, context: Context) {
        context.coordinator.parent = self

        if searchBar.text != text {
            searchBar.text = text
        }
        searchBar.searchTextField.clearButtonMode = text.isEmpty ? .never : .always

        if isFocused, !searchBar.searchTextField.isFirstResponder {
            searchBar.searchTextField.becomeFirstResponder()
        } else if !isFocused, searchBar.searchTextField.isFirstResponder {
            searchBar.searchTextField.resignFirstResponder()
        }
    }

    final class Coordinator: NSObject, UISearchBarDelegate {
        var parent: PrototypeEmojiSearchBar

        init(parent: PrototypeEmojiSearchBar) {
            self.parent = parent
        }

        func searchBar(
            _ searchBar: UISearchBar,
            textDidChange searchText: String
        ) {
            parent.text = searchText
        }

        func searchBarTextDidBeginEditing(_ searchBar: UISearchBar) {
            parent.isFocused = true
        }

        func searchBarTextDidEndEditing(_ searchBar: UISearchBar) {
            parent.isFocused = false
        }

        func searchBarSearchButtonClicked(_ searchBar: UISearchBar) {
            parent.isFocused = false
            searchBar.searchTextField.resignFirstResponder()
        }
    }
}

private struct PrototypeReactionConfigurationSlot: Identifiable {
    let index: Int

    var id: Int { index }
}

private struct PrototypeConfigureReactionsView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    @Binding private var quickReactionEmoji: [String]
    @State private var draftQuickReactionEmoji: [String]
    @State private var replacementSlot: PrototypeReactionConfigurationSlot?
    @State private var replacementWiggleAngle = 0.0

    init(quickReactionEmoji: Binding<[String]>) {
        _quickReactionEmoji = quickReactionEmoji
        _draftQuickReactionEmoji = State(
            initialValue: Self.normalized(quickReactionEmoji.wrappedValue)
        )
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Spacer()

                HStack(spacing: 0) {
                    ForEach(draftQuickReactionEmoji.indices, id: \.self) { index in
                        let isReplacing = replacementSlot != nil
                        let isFocused = replacementSlot?.index == index

                        Button {
                            replacementSlot = PrototypeReactionConfigurationSlot(index: index)
                        } label: {
                            Text(draftQuickReactionEmoji[index])
                                .font(.system(size: 32))
                                .frame(maxWidth: .infinity, minHeight: 52)
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)
                        .opacity(!isReplacing || isFocused ? 1 : 0.3)
                        .scaleEffect(
                            !isReplacing ? 1 : (isFocused ? 1.3 : 0.8)
                        )
                        .rotationEffect(
                            .radians(
                                isFocused && !reduceMotion
                                    ? replacementWiggleAngle
                                    : 0
                            )
                        )
                        .animation(
                            reduceMotion ? nil : .easeInOut(duration: 0.3),
                            value: replacementSlot?.index
                        )
                        .accessibilityLabel(
                            "Reaction \(index + 1), \(draftQuickReactionEmoji[index])"
                        )
                        .accessibilityValue(isFocused ? "Selected for replacement" : "")
                        .accessibilityHint("Opens the emoji picker to replace this reaction")
                    }
                }
                .padding(6)
                .frame(maxWidth: 360)
                .glassEffect(.regular.interactive(), in: .capsule)

                Text("Tap an emoji to replace it.")
                    .font(.body)
                    .foregroundStyle(.secondary)

                Spacer()
                Spacer()
            }
            .padding()
            .navigationTitle("Configure Reactions")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("Reset") {
                        withAnimation(.easeInOut(duration: 0.2)) {
                            draftQuickReactionEmoji = PrototypeReaction.defaultQuickEmoji
                        }
                    }
                    .disabled(draftQuickReactionEmoji == PrototypeReaction.defaultQuickEmoji)
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button("Done", systemImage: "checkmark") {
                        quickReactionEmoji = draftQuickReactionEmoji
                        dismiss()
                    }
                }
            }
        }
        .sheet(item: $replacementSlot) { slot in
            PrototypeEmojiPickerView(
                quickReactionEmoji: $draftQuickReactionEmoji,
                showsConfigurationAction: false
            ) { emoji in
                replaceReaction(at: slot.index, with: emoji)
            }
            .presentationDetents([.medium, .large])
            .presentationDragIndicator(.visible)
        }
        .task(id: replacementSlot?.index) {
            var transaction = Transaction()
            transaction.disablesAnimations = true
            withTransaction(transaction) {
                replacementWiggleAngle = 0
            }

            guard let replacementIndex = replacementSlot?.index,
                  !reduceMotion else { return }

            try? await Task.sleep(for: .milliseconds(300))
            guard !Task.isCancelled,
                  replacementSlot?.index == replacementIndex else { return }

            withTransaction(transaction) {
                replacementWiggleAngle = -0.08
            }
            withAnimation(
                .easeInOut(duration: 0.2).repeatForever(autoreverses: true)
            ) {
                replacementWiggleAngle = 0.08
            }
        }
    }

    private func replaceReaction(at index: Int, with emoji: String) {
        guard draftQuickReactionEmoji.indices.contains(index) else { return }

        if let existingIndex = draftQuickReactionEmoji.firstIndex(of: emoji),
           existingIndex != index {
            draftQuickReactionEmoji.swapAt(existingIndex, index)
        } else {
            draftQuickReactionEmoji[index] = emoji
        }
    }

    private static func normalized(_ emoji: [String]) -> [String] {
        var result = emoji.reduce(into: [String]()) { values, value in
            if !values.contains(value) { values.append(value) }
        }
        for value in PrototypeReaction.defaultQuickEmoji where !result.contains(value) {
            result.append(value)
        }
        return Array(result.prefix(PrototypeReaction.defaultQuickEmoji.count))
    }
}

private struct PrototypeEmojiSection: Identifiable {
    let id: String
    let title: String
    let symbol: String
    let keywords: String
    let emoji: [String]
}

private enum PrototypeEmojiCatalog {
    static let aliases: [String: String] = [
        "❤": "heart love red",
        "🤘": "horns rock hand",
        "🔥": "fire flame hot",
        "😂": "laugh tears joy",
        "🦫": "beaver animal",
        "🚀": "rocket launch space",
        "👍": "thumbs up yes",
        "👎": "thumbs down no",
        "😭": "cry sob tears",
        "🎉": "party celebrate",
        "💯": "hundred perfect",
        "🙏": "please thanks pray",
        "👀": "eyes look",
        "✅": "check done yes",
        "❌": "cross no",
    ]

    static let recents = PrototypeEmojiSection(
        id: "recents",
        title: "Recents",
        symbol: "clock",
        keywords: "recent frequently used",
        emoji: values("😂😭🎉👀🤦🤭👍💀🫂💯🏃⚫🍴🤮😢🆗🤘🤨🛏🦫🥶😌🚀💋🙅😅")
    )

    static let categories: [PrototypeEmojiSection] = [
        .init(
            id: "smileys",
            title: "Smileys & People",
            symbol: "face.smiling",
            keywords: "face smile people emotion hand body",
            emoji: values("😀😃😄😁😆😅😂🤣🥲😊😇🙂🙃😉😌😍🥰😘😗😙😚😋😛😝😜🤪🤨🧐🤓😎🥸🤩🥳🙂‍↕️😏😒🙂‍↔️😞😔😟😕🙁☹️😣😖😫😩🥺😢😭😤😠😡🤬🤯😳🥵🥶😶‍🌫️😱😨😰😥😓🤗🤔🫣🤭🫢🫡🤫🫠🤥😶🫥😐🫤😑🫨😬🙄😯😦😧😮😲🥱😴🤤😪😮‍💨😵😵‍💫🤐🥴🤢🤮🤧😷🤒🤕🤑🤠😈👿👹👺🤡💩👻💀☠️👽👾🤖🎃😺😸😹😻😼😽🙀😿😾👋🤚🖐️✋🖖🫱🫲🫳🫴👌🤌🤏✌️🤞🫰🤟🤘🤙👈👉👆👇☝️🫵👍👎✊👊🤛🤜👏🙌🫶👐🤲🤝🙏✍️💅🤳💪🦾🦿🦵🦶👂👃🧠🫀🫁🦷🦴👀👁️👅👄🫦💋")
        ),
        .init(
            id: "animals",
            title: "Animals & Nature",
            symbol: "pawprint",
            keywords: "animal nature plant weather pet",
            emoji: values("🐶🐱🐭🐹🐰🦊🐻🐼🐻‍❄️🐨🐯🦁🐮🐷🐽🐸🐵🙈🙉🙊🐒🐔🐧🐦🐤🐣🐥🦆🦅🦉🦇🐺🐗🐴🦄🐝🪱🐛🦋🐌🐞🐜🪰🪲🪳🦟🦗🕷️🕸️🦂🐢🐍🦎🦖🦕🐙🦑🪼🦐🦞🦀🐡🐠🐟🐬🐳🐋🦈🦭🐊🐅🐆🦓🫏🦍🦧🦣🐘🦛🦏🐪🐫🦒🦘🦬🐃🐂🐄🫎🐎🐖🐏🐑🦙🐐🦌🦫🦥🐕🐩🦮🐕‍🦺🐈🐈‍⬛🪽🪶🐓🦃🦤🦚🦜🦢🪿🦩🕊️🐇🦝🦨🦡🦦🦫🐁🐀🐿️🦔🐉🐲🌵🎄🌲🌳🌴🪹🪺🪵🌱🌿☘️🍀🎍🪴🎋🍃🍂🍁🍄🐚🪨🌾💐🌷🌹🥀🌺🌸🪷🌼🌻🌞🌝🌛🌜🌚🌕🌖🌗🌘🌑🌒🌓🌔🌙🌎🌍🌏🪐💫⭐️🌟✨⚡️☄️💥🔥🌪️🌈☀️🌤️⛅️🌥️☁️🌦️🌧️⛈️🌩️🌨️❄️☃️⛄️🌬️💨💧💦🫧☔️☂️🌊")
        ),
        .init(
            id: "food",
            title: "Food & Drink",
            symbol: "takeoutbag.and.cup.and.straw",
            keywords: "food drink fruit meal restaurant",
            emoji: values("🍏🍎🍐🍊🍋🍋‍🟩🍌🍉🍇🍓🫐🍈🍒🍑🥭🍍🥥🥝🍅🍆🥑🫛🥦🥬🥒🌶️🫑🌽🥕🫒🧄🧅🫚🥔🍠🫘🥐🥯🍞🥖🥨🧀🥚🍳🧈🥞🧇🥓🥩🍗🍖🦴🌭🍔🍟🍕🫓🥪🥙🧆🌮🌯🫔🥗🥘🫕🥫🍝🍜🍲🍛🍣🍱🥟🦪🍤🍙🍚🍘🍥🥠🥮🍢🍡🍧🍨🍦🥧🧁🍰🎂🍮🍭🍬🍫🍿🍩🍪🌰🥜🍯🥛🍼🫖☕️🍵🧃🥤🧋🍶🍺🍻🥂🍷🫗🥃🍸🍹🧉🍾🧊🥄🍴🍽️🥣🥡🥢🧂")
        ),
        .init(
            id: "activities",
            title: "Activities",
            symbol: "soccerball",
            keywords: "activity sport game music art celebration",
            emoji: values("⚽️🏀🏈⚾️🥎🎾🏐🏉🥏🎱🪀🏓🏸🏒🏑🥍🏏🪃🥅⛳️🪁🏹🎣🤿🥊🥋🎽🛹🛼🛷⛸️🥌🎿⛷️🏂🪂🏋️🤼🤸⛹️🤺🤾🏌️🏇🧘🏄🏊🤽🚣🧗🚵🚴🏆🥇🥈🥉🏅🎖️🏵️🎗️🎫🎟️🎪🤹🎭🩰🎨🎬🎤🎧🎼🎹🥁🪘🎷🎺🪗🎸🪕🎻🪈🎲♟️🎯🎳🎮🎰🧩🎉🎊🎈🎁🪄🪅")
        ),
        .init(
            id: "travel",
            title: "Travel & Places",
            symbol: "car",
            keywords: "travel place transport building space",
            emoji: values("🚗🚕🚙🚌🚎🏎️🚓🚑🚒🚐🛻🚚🚛🚜🦯🦽🦼🛴🚲🛵🏍️🛺🚨🚔🚍🚘🚖🛞🚡🚠🚟🚃🚋🚞🚝🚄🚅🚈🚂🚆🚇🚊🚉✈️🛫🛬🛩️💺🛰️🚀🛸🚁🛶⛵️🚤🛥️🛳️⛴️🚢⚓️🛟⛽️🚧🚦🚥🗺️🗿🗽🗼🏰🏯🏟️🎡🎢🛝🎠⛲️⛱️🏖️🏝️🏜️🌋⛰️🏔️🗻🏕️⛺️🛖🏠🏡🏘️🏚️🏗️🏭🏢🏬🏣🏤🏥🏦🏨🏪🏫🏩💒🏛️⛪️🕌🕍🛕🕋⛩️🛤️🛣️🗾🎑🏞️🌅🌄🌠🎇🎆🌇🌆🏙️🌃🌌🌉🌁")
        ),
        .init(
            id: "objects",
            title: "Objects",
            symbol: "lightbulb",
            keywords: "object tool technology clothing household",
            emoji: values("⌚️📱📲💻⌨️🖥️🖨️🖱️🖲️🕹️🗜️💽💾💿📀📼📷📸📹🎥📽️🎞️📞☎️📟📠📺📻🎙️🎚️🎛️🧭⏱️⏲️⏰🕰️⌛️⏳📡🔋🪫🔌💡🔦🕯️🪔🧯🛢️💸💵💴💶💷🪙💰💳💎⚖️🪜🧰🪛🔧🔨⚒️🛠️⛏️🪚🔩⚙️🪤🧱⛓️⛓️‍💥🧲🔫💣🧨🪓🔪🗡️⚔️🛡️🚬⚰️🪦⚱️🏺🔮📿🧿🪬💈⚗️🔭🔬🕳️🩹🩺🩻🩼💊💉🩸🧬🦠🧫🧪🌡️🧹🪠🧺🧻🚽🚰🚿🛁🛀🧼🪥🪒🧽🪣🧴🛎️🔑🗝️🚪🪑🛋️🛏️🛌🧸🪆🖼️🪞🪟🛍️🛒🎁🎈🎏🎀🪄🪩🎎🏮🎐🧧✉️📩📨📧💌📥📤📦🏷️🪧📪📫📬📭📮📯📜📃📄📑🧾📊📈📉🗒️🗓️📆📅🗑️📇🗃️🗳️🗄️📋📁📂🗂️🗞️📰📓📔📒📕📗📘📙📚📖🔖🧷🔗📎🖇️📐📏🧮📌📍✂️🖊️🖋️✒️🖌️🖍️📝✏️🔍🔎🔏🔐🔒🔓")
        ),
        .init(
            id: "symbols",
            title: "Symbols",
            symbol: "plus.forwardslash.minus",
            keywords: "symbol sign heart arrow number",
            emoji: values("❤️🧡💛💚💙🩵💜🖤🩶🤍🤎💔❤️‍🔥❤️‍🩹❣️💕💞💓💗💖💘💝💟☮️✝️☪️🕉️☸️✡️🔯🕎☯️☦️🛐⛎♈️♉️♊️♋️♌️♍️♎️♏️♐️♑️♒️♓️🆔⚛️🉑☢️☣️📴📳🈶🈚️🈸🈺🈷️✴️🆚💮🉐㊙️㊗️🈴🈵🈹🈲🅰️🅱️🆎🆑🅾️🆘❌⭕️🛑⛔️📛🚫💯💢♨️🚷🚯🚳🚱🔞📵🚭❗️❕❓❔‼️⁉️🔅🔆〽️⚠️🚸🔱⚜️🔰♻️✅🈯️💹❇️✳️❎🌐💠Ⓜ️🌀💤🏧🚾♿️🅿️🛗🈳🈂️🛂🛃🛄🛅🚹🚺🚼⚧️🚻🚮🎦📶🈁🔣ℹ️🔤🔡🔠🆖🆗🆙🆒🆕🆓0️⃣1️⃣2️⃣3️⃣4️⃣5️⃣6️⃣7️⃣8️⃣9️⃣🔟🔢#️⃣*️⃣⏏️▶️⏸️⏯️⏹️⏺️⏭️⏮️⏩⏪⏫⏬◀️🔼🔽➡️⬅️⬆️⬇️↗️↘️↙️↖️↕️↔️↪️↩️⤴️⤵️🔀🔁🔂🔄🔃🎵🎶➕➖➗✖️🟰♾️💲💱™️©️®️〰️➰➿🔚🔙🔛🔝🔜✔️☑️🔘🔴🟠🟡🟢🔵🟣⚫️⚪️🟤🔺🔻🔸🔹🔶🔷🔳🔲▪️▫️◾️◽️◼️◻️🟥🟧🟨🟩🟦🟪⬛️⬜️🟫")
        ),
        .init(
            id: "flags",
            title: "Flags",
            symbol: "flag",
            keywords: "flag country nation",
            emoji: values("🏁🚩🎌🏴🏳️🏳️‍🌈🏳️‍⚧️🏴‍☠️🇺🇳🇦🇺🇦🇹🇧🇪🇧🇷🇨🇦🇨🇳🇭🇷🇨🇾🇨🇿🇩🇰🇪🇪🇫🇮🇫🇷🇩🇪🇬🇷🇭🇺🇮🇸🇮🇳🇮🇩🇮🇪🇮🇱🇮🇹🇯🇵🇰🇷🇱🇻🇱🇹🇱🇺🇲🇹🇲🇽🇲🇪🇳🇱🇳🇿🇲🇰🇳🇴🇵🇱🇵🇹🇷🇴🇷🇸🇸🇬🇸🇰🇸🇮🇿🇦🇪🇸🇸🇪🇨🇭🇹🇷🇺🇦🇬🇧🇺🇸🇻🇦")
        ),
    ]

    private static func values(_ value: String) -> [String] {
        value.map(String.init).reduce(into: [String]()) { result, emoji in
            if !result.contains(emoji) { result.append(emoji) }
        }
    }
}

struct PrototypeForwardMessagesView: View {
    @Environment(\.dismiss) private var dismiss

    let chats: [PrototypeChat]
    let people: [PrototypePerson]
    let currentProfileID: String
    let onForward: ([String]) -> Void

    @State private var selectedChatIDs: Set<String> = []
    @State private var query = ""

    var body: some View {
        NavigationStack {
            List(filteredChats) { chat in
                Button {
                    toggle(chat.id)
                } label: {
                    HStack(spacing: 12) {
                        PrototypeChatAvatarView(
                            avatar: chat.resolvedAvatar(people: people),
                            size: 40,
                            publicKey: chat.resolvedAvatarPublicKey(people: people)
                        )

                        Text(chat.title(people: people))
                            .foregroundStyle(.primary)

                        Spacer()

                        Image(
                            systemName: selectedChatIDs.contains(chat.id)
                                ? "checkmark.circle.fill"
                                : "circle"
                        )
                        .font(.title3)
                        .foregroundStyle(
                            selectedChatIDs.contains(chat.id) ? Color.accentColor : .secondary
                        )
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .disabled(selectedChatIDs.count == 5 && !selectedChatIDs.contains(chat.id))
                .accessibilityValue(selectedChatIDs.contains(chat.id) ? "Selected" : "Not selected")
            }
            .searchable(text: $query, prompt: "Search Chats")
            .navigationTitle("Forward")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("Cancel", role: .cancel) { dismiss() }
                }
            }
            .safeAreaBar(edge: .bottom, spacing: 0) {
                Button(forwardTitle) {
                    onForward(chats.map(\.id).filter(selectedChatIDs.contains))
                    dismiss()
                }
                .buttonStyle(.glassProminent)
                .controlSize(.large)
                .disabled(selectedChatIDs.isEmpty)
                .frame(maxWidth: .infinity)
                .padding(.horizontal)
                .padding(.vertical, 8)
            }
        }
    }

    private var filteredChats: [PrototypeChat] {
        let value = query.trimmingCharacters(in: .whitespacesAndNewlines)
        let eligibleChats = chats.filter {
            $0.composerAvailability(
                currentProfileID: currentProfileID,
                people: people
            ) == .available
        }
        guard !value.isEmpty else { return eligibleChats }
        return eligibleChats.filter {
            $0.title(people: people).localizedCaseInsensitiveContains(value)
        }
    }

    private var forwardTitle: String {
        selectedChatIDs.count > 1
            ? "Forward to \(selectedChatIDs.count) Chats"
            : "Forward"
    }

    private func toggle(_ id: String) {
        if selectedChatIDs.contains(id) {
            selectedChatIDs.remove(id)
        } else if selectedChatIDs.count < 5 {
            selectedChatIDs.insert(id)
        }
    }
}

struct PrototypeMessageDetailsView: View {
    let message: PrototypeMessage
    let chat: PrototypeChat
    let profile: PrototypeProfile

    var body: some View {
        List {
            Section {
                messagePreview

                LabeledContent(message.authorID == profile.id ? "Sent" : "Received") {
                    Text(message.sentAt.formatted(date: .abbreviated, time: .shortened))
                }
            }

            if message.authorID == profile.id {
                Section(deliveryTitle) {
                    ForEach(recipients) { person in
                        personRow(person)
                    }
                }
            } else if let author {
                Section("Sent from") {
                    personRow(author)
                }
            }
        }
        .navigationTitle("Message Details")
        .navigationBarTitleDisplayMode(.inline)
    }

    private var messagePreview: some View {
        PrototypeMessageBubble(
            message: message,
            outgoing: message.authorID == profile.id,
            isGroup: false,
            author: author,
            profileName: profile.name,
            resolvedReply: resolvedReply,
            replyAuthorName: resolvedReply.map(authorName) ?? "Message",
            showsAuthor: false,
            showsAvatar: false,
            showsTimestamp: true,
            isHighlighted: false,
            searchQuery: nil,
            people: profile.people,
            currentProfileID: profile.id,
            onSelectReaction: { _ in },
            onOpenReply: {},
            onOpenPerson: { _ in },
            onOpenMedia: { _ in },
            onOpenFile: { _ in },
            visibleVoiceTranscript: nil,
            readAloudProgress: nil,
            isContextInteractionEnabled: false,
            onShowActions: {},
            isSwipeToReplyEnabled: false,
            onSwipeToReply: {},
            onContextContentFrameChange: { _ in }
        )
        .allowsHitTesting(false)
    }

    private var author: PrototypePerson? {
        profile.people.first { $0.id == message.authorID }
    }

    private var resolvedReply: PrototypeMessage? {
        guard let id = message.replyToMessageID else { return nil }
        return chat.messages.first { $0.id == id }
    }

    private var recipients: [PrototypePerson] {
        let ids: [String]
        switch chat.kind {
        case let .direct(personID):
            ids = [personID]
        case .group:
            ids = chat.members.map(\.personID).filter { $0 != profile.id }
        }
        return ids.compactMap { id in profile.people.first { $0.id == id } }
    }

    private var deliveryTitle: String {
        switch message.deliveryState {
        case .sending: "Sending"
        case .sent: "Sent"
        case .failed: "Not Delivered"
        }
    }

    private func authorName(_ message: PrototypeMessage) -> String {
        message.authorID == profile.id
            ? "You"
            : (profile.people.first { $0.id == message.authorID }?.name ?? "Unknown")
    }

    private func personRow(_ person: PrototypePerson) -> some View {
        HStack(spacing: 12) {
            PrototypeChatAvatarView(
                avatar: person.avatar,
                size: 36,
                publicKey: person.publicKey
            )
            Text(person.name)
        }
    }
}
