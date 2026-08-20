import SwiftUI
import UIKit

private func prototypeSearchHighlightedAttributedString(
    _ attributedString: AttributedString,
    query: String?
) -> AttributedString {
    guard let query else { return attributedString }
    let needle = query.trimmingCharacters(in: .whitespacesAndNewlines)
    guard !needle.isEmpty else { return attributedString }

    var result = attributedString
    var searchStart = result.startIndex
    while searchStart < result.endIndex {
        let remaining = result[searchStart..<result.endIndex]
        guard let match = remaining.range(
            of: needle,
            options: [.caseInsensitive, .diacriticInsensitive]
        ) else { break }
        result[match].backgroundColor = .cyan
        result[match].foregroundColor = .black
        searchStart = match.upperBound
    }
    return result
}

private func prototypeSearchHighlightedText(
    _ text: String,
    query: String?
) -> some View {
    prototypeInlineHighlightedText(
        prototypeSearchHighlightedAttributedString(
            AttributedString(text),
            query: query
        ),
        tagsMentions: false
    )
    .textRenderer(
        PrototypeInlineHighlightTextRenderer(
            mentionBackgroundColor: nil,
            searchBackgroundColor: .cyan
        )
    )
}

private func prototypeInlineHighlightedText(
    _ attributedString: AttributedString,
    tagsMentions: Bool
) -> Text {
    var interpolation = LocalizedStringKey.StringInterpolation(
        literalCapacity: 0,
        interpolationCount: attributedString.runs.count
    )

    for run in attributedString.runs {
        let isSearchMatch = run.backgroundColor != nil
        var segmentAttributes = AttributedString(attributedString[run.range])
        if isSearchMatch {
            segmentAttributes.backgroundColor = nil
        }

        var segment = Text(segmentAttributes)
        if tagsMentions, run.link?.scheme == "whitenoise-person" {
            segment = segment.customAttribute(PrototypeMentionAttribute())
        }
        if isSearchMatch {
            segment = segment.customAttribute(PrototypeSearchMatchAttribute())
        }
        interpolation.appendInterpolation(segment)
    }

    return Text(LocalizedStringKey(stringInterpolation: interpolation))
}

struct PrototypeMessageBubble: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.incomingPrototypeMessageColor) private var incomingColor
    @Environment(\.layoutDirection) private var layoutDirection
    @Environment(\.outgoingPrototypeMessageColor) private var outgoingColor
    let message: PrototypeMessage
    let outgoing: Bool
    let isGroup: Bool
    let author: PrototypePerson?
    let profileName: String
    let resolvedReply: PrototypeMessage?
    let replyAuthorName: String
    let showsAuthor: Bool
    let showsAvatar: Bool
    let showsTimestamp: Bool
    let isHighlighted: Bool
    let searchQuery: String?
    let people: [PrototypePerson]
    let currentProfileID: String
    let onSelectReaction: (String) -> Void
    let onOpenReply: () -> Void
    let onOpenPerson: (String) -> Void
    let onOpenMedia: (PrototypeAttachment) -> Void
    let onOpenFile: (URL) -> Void
    let visibleVoiceTranscript: String?
    let readAloudProgress: Double?
    let isContextInteractionEnabled: Bool
    let onShowActions: () -> Void
    let isSwipeToReplyEnabled: Bool
    let onSwipeToReply: () -> Void
    let onContextContentFrameChange: (CGRect) -> Void

    @State private var contextPresentationTask: Task<Void, Never>?
    @State private var isContextPressing = false
    @State private var replySwipeFeedbackTrigger = 0
    @State private var replySwipeIsReady = false
    @State private var replySwipeOffset: CGFloat = 0

    private enum ReplySwipeMetrics {
        static let activationOffset: CGFloat = 55
        static let indicatorRestingInset: CGFloat = 8
        static let indicatorSize: CGFloat = 34
        static let indicatorMovementDivisor: CGFloat = 8
        static let overdragResistance: CGFloat = 6
        static let resetDuration: TimeInterval = 0.2
    }

    private enum ContextPressMetrics {
        static let recognitionDuration: TimeInterval = 0.2
        static let compressionDuration: TimeInterval = 0.2
        static let maximumMovement: CGFloat = 10
    }

    var body: some View {
        messageRow
    }

    private var messageRow: some View {
        HStack(alignment: .bottom, spacing: 0) {
            if outgoing { Spacer(minLength: 48) }

            if !outgoing, isGroup {
                Color.clear.frame(width: 37, height: 1)
            }

            VStack(alignment: outgoing ? .trailing : .leading, spacing: 4) {
                if isGroup, !outgoing, showsAuthor {
                    Button {
                        if let author { onOpenPerson(author.id) }
                    } label: {
                        prototypeSearchHighlightedText(
                            author?.name ?? "Unknown",
                            query: searchQuery
                        )
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(
                                PrototypeAuthorNameColor.color(
                                    for: author?.publicKey ?? message.authorID
                                )
                            )
                    }
                    .buttonStyle(.plain)
                    .padding(
                        .leading,
                        PrototypeMessageBubbleMetrics.textHorizontalInset
                    )
                    .offset(x: replySwipeOffset)
                }

                decoratedBubble
            }

            if !outgoing { Spacer(minLength: 48) }
        }
        .frame(maxWidth: .infinity)
        .modifier(
            PrototypeMessageAccessibilityModifier(
                containsVoice: voiceAttachment != nil,
                summary: accessibilitySummary,
                identifier: "message.\(message.id)"
            )
        )
        .gesture(
            PrototypeMessageReplyPanGesture(
                isEnabled: isSwipeToReplyEnabled,
                onChanged: updateReplySwipe,
                onEnded: finishReplySwipe,
                onCancelled: { resetReplySwipe(animated: true) }
            )
        )
        .sensoryFeedback(
            .impact(weight: .light, intensity: 1),
            trigger: replySwipeFeedbackTrigger
        )
        .onChange(of: isSwipeToReplyEnabled) { _, isEnabled in
            if !isEnabled { resetReplySwipe(animated: false) }
        }
        .onDisappear {
            cancelContextPresentation()
            resetReplySwipe(animated: false)
        }
    }

    private var decoratedBubble: some View {
        contextInteractiveBubbleContent
            .overlay {
                if isHighlighted {
                    shape.stroke(Color.accentColor, lineWidth: 3)
                }
            }
            .offset(x: replySwipeOffset)
            .background(alignment: replyIndicatorAlignment) {
                replySwipeIndicator
                    .offset(
                        x: replyIndicatorRestingOffset
                            + (replySwipeOffset
                                / ReplySwipeMetrics.indicatorMovementDivisor)
                    )
                    .opacity(
                        isSwipeToReplyEnabled && abs(replySwipeOffset) > 0 ? 1 : 0
                    )
            }
            .overlay(alignment: .bottomLeading) {
                if isGroup, !outgoing, showsAvatar {
                    PrototypeChatAvatarView(
                        avatar: author?.avatar ?? .monogram("?"),
                        size: 30,
                        publicKey: author?.publicKey
                    )
                    .offset(x: -37 + replySwipeOffset)
                }
            }
            .overlay(alignment: .bottom) {
                if showsReactions {
                    reactionMetadataRow
                        .padding(
                            .horizontal,
                            PrototypeMessageBubbleMetrics.reactionEdgeInset
                        )
                        .offset(
                            x: replySwipeOffset,
                            y: reactionVerticalOffset
                        )
                }
            }
            .overlay(alignment: visibleTimestampAlignment) {
                if showsVisibleTimestamp {
                    timestamp
                        .padding(
                            visibleTimestampInsetEdge,
                            PrototypeMessageBubbleMetrics.textHorizontalInset
                        )
                        .offset(
                            x: replySwipeOffset,
                            y: PrototypeMessageBubbleMetrics.timestampVerticalOffset
                        )
                } else if showsFailedDeliveryStatus {
                    failedDeliveryStatus
                        .gesture(
                            PrototypeMessageContextLongPressGesture(
                                minimumDuration: ContextPressMetrics.recognitionDuration,
                                maximumMovement: ContextPressMetrics.maximumMovement,
                                onRecognized: beginContextPresentation,
                                onCancelled: cancelContextPresentation
                            )
                        )
                        .offset(x: replySwipeOffset, y: 18)
                }
            }
            .padding(
                .bottom,
                metadataBottomReserve
            )
            .onGeometryChange(for: CGRect.self) { proxy in
                proxy.frame(in: .named("conversationSurface"))
            } action: { frame in
                onContextContentFrameChange(frame)
            }
    }

    private var replySwipeIndicator: some View {
        Image(systemName: "arrowshape.turn.up.left.fill")
            .font(.system(size: 24))
            .symbolRenderingMode(.monochrome)
            .foregroundStyle(.secondary)
            .frame(
                width: ReplySwipeMetrics.indicatorSize,
                height: ReplySwipeMetrics.indicatorSize
            )
            .scaleEffect(reduceMotion || !replySwipeIsReady ? 1 : 1.16)
            .animation(
                reduceMotion
                    ? nil
                    : .spring(response: 0.2, dampingFraction: 0.7),
                value: replySwipeIsReady
            )
            .allowsHitTesting(false)
            .accessibilityHidden(true)
    }

    private var replyIndicatorAlignment: Alignment {
        layoutDirection == .rightToLeft ? .trailing : .leading
    }

    private var replyIndicatorRestingOffset: CGFloat {
        layoutDirection == .rightToLeft
            ? -ReplySwipeMetrics.indicatorRestingInset
            : ReplySwipeMetrics.indicatorRestingInset
    }

    private func updateReplySwipe(_ translation: CGPoint) {
        guard isSwipeToReplyEnabled else { return }

        let semanticOffset = semanticReplyOffset(translation.x)
        let positiveOffset = max(0, semanticOffset)
        let wasReady = replySwipeIsReady
        replySwipeIsReady = positiveOffset >= ReplySwipeMetrics.activationOffset

        if replySwipeIsReady, !wasReady {
            replySwipeFeedbackTrigger += 1
        }

        let resistedOffset: CGFloat
        if positiveOffset > ReplySwipeMetrics.activationOffset {
            resistedOffset = ReplySwipeMetrics.activationOffset
                + ((positiveOffset - ReplySwipeMetrics.activationOffset)
                    / ReplySwipeMetrics.overdragResistance)
        } else {
            resistedOffset = positiveOffset
        }

        replySwipeOffset = layoutDirection == .rightToLeft
            ? -resistedOffset
            : resistedOffset
    }

    private func finishReplySwipe(_ translation: CGPoint) {
        let shouldReply = isSwipeToReplyEnabled
            && replySwipeIsReady
            && semanticReplyOffset(translation.x)
                >= ReplySwipeMetrics.activationOffset

        resetReplySwipe(animated: true)
        if shouldReply { onSwipeToReply() }
    }

    private func semanticReplyOffset(_ physicalOffset: CGFloat) -> CGFloat {
        layoutDirection == .rightToLeft ? -physicalOffset : physicalOffset
    }

    private func resetReplySwipe(animated: Bool) {
        let changes = {
            replySwipeOffset = 0
            replySwipeIsReady = false
        }
        if animated, !reduceMotion {
            withAnimation(.easeOut(duration: ReplySwipeMetrics.resetDuration)) {
                changes()
            }
        } else {
            changes()
        }
    }

    @ViewBuilder
    private var contextInteractiveBubbleContent: some View {
        if isContextInteractionEnabled {
            bubbleContent
                .scaleEffect(
                    reduceMotion || !isContextPressing ? 1 : 0.95,
                    anchor: outgoing ? .trailing : .leading
                )
                .animation(
                    .easeInOut(duration: ContextPressMetrics.compressionDuration),
                    value: isContextPressing
                )
                .gesture(
                    PrototypeMessageContextLongPressGesture(
                        minimumDuration: ContextPressMetrics.recognitionDuration,
                        maximumMovement: ContextPressMetrics.maximumMovement,
                        onRecognized: beginContextPresentation,
                        onCancelled: cancelContextPresentation
                    )
                )
        } else {
            bubbleContent
        }
    }

    private func beginContextPresentation() {
        guard isContextInteractionEnabled, !message.isDeleted else { return }

        contextPresentationTask?.cancel()
        isContextPressing = true
        contextPresentationTask = Task { @MainActor in
            try? await Task.sleep(
                for: .seconds(ContextPressMetrics.compressionDuration)
            )
            guard !Task.isCancelled else { return }

            contextPresentationTask = nil
            isContextPressing = false
            showActionsIfEnabled()
        }
    }

    private func showActionsIfEnabled() {
        guard isContextInteractionEnabled, !message.isDeleted else { return }
        onShowActions()
    }

    private func cancelContextPresentation() {
        contextPresentationTask?.cancel()
        contextPresentationTask = nil
        isContextPressing = false
    }

    private var timestamp: some View {
        HStack(spacing: 4) {
            if message.deliveryState == .sending {
                ProgressView().controlSize(.mini)
            }
            Text(PrototypeDateFormatter.time(for: message.sentAt))
                .font(.caption2)
                .foregroundStyle(.primary)
                .monospacedDigit()
        }
    }

    private var failedDeliveryStatus: some View {
        HStack(alignment: .firstTextBaseline, spacing: 3) {
            Image(systemName: "exclamationmark.circle")

            Text("Not delivered, hold for options")
                .multilineTextAlignment(.leading)
        }
        .font(.caption)
        .foregroundStyle(.primary)
        .padding(
            .leading,
            PrototypeMessageBubbleMetrics.textHorizontalInset
        )
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Not delivered")
        .accessibilityHint("Touch and hold for options, then choose Retry Send.")
    }

    @ViewBuilder
    private var bubbleContent: some View {
        bubbleBody
            .frame(
                width: hasRichContent
                    ? richContentWidth
                    : nil,
                alignment: .leading
            )
            .padding(
                .horizontal,
                hasRichContent
                    ? PrototypeMessageBubbleMetrics.outerContentInset
                    : PrototypeMessageBubbleMetrics.textHorizontalInset
            )
            .padding(.top, bubbleVerticalInset)
            .padding(.bottom, bubbleVerticalInset)
            .foregroundStyle(messageColor.foregroundColor)
            .background(messageColor.color, in: shape)
    }

    private var bubbleBody: some View {
        VStack(
            alignment: .leading,
            spacing: PrototypeMessageBubbleMetrics.contentSpacing
        ) {
            if message.isDeleted {
                Label(deletedText, systemImage: "trash")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            } else {
                if message.replyToMessageID != nil {
                    replyQuote
                }
                if !message.attachments.isEmpty {
                    PrototypeAttachmentCollectionView(
                        attachments: message.attachments,
                        canvasWidth: richContentWidth,
                        people: people,
                        tint: messageColor.foregroundColor,
                        surfaceOpacity: richSurfaceOpacity,
                        searchQuery: searchQuery,
                        accessibilityContext: accessibilitySummary,
                        onOpenMedia: onOpenMedia,
                        onOpenFile: onOpenFile,
                        onOpenPerson: onOpenPerson
                    )
                }
                if let visibleVoiceTranscript {
                    localVoiceTranscript(visibleVoiceTranscript)
                }
                if !message.text.isEmpty {
                    messageText
                }
                if let readAloudProgress {
                    readingAloudProgress(readAloudProgress)
                }
            }
        }
    }

    private func localVoiceTranscript(_ transcript: String) -> some View {
        voiceTranscript {
            Text(transcript)
                .textSelection(.enabled)
        }
    }

    private func readingAloudProgress(_ progress: Double) -> some View {
        let value = min(max(progress, 0), 1)
        return HStack(spacing: 6) {
            Image(systemName: "speaker.wave.2.fill")
                .font(.caption2)

            ProgressView(value: value)
                .progressViewStyle(.linear)
                .tint(messageColor.foregroundColor.opacity(0.75))
        }
        .padding(
            .horizontal,
            hasRichContent ? richTextHorizontalInsetAdjustment : 0
        )
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Reading Aloud")
        .accessibilityValue("\(Int((value * 100).rounded())) percent")
    }

    @ViewBuilder
    private var messageText: some View {
        if voiceAttachment == nil {
            renderedMessageText
                .padding(
                    .horizontal,
                    hasRichContent ? richTextHorizontalInsetAdjustment : 0
                )
                .padding(
                    .bottom,
                    hasRichContent ? richTextVerticalInsetAdjustment : 0
                )
                .frame(
                    maxWidth: hasRichContent ? .infinity : nil,
                    alignment: .leading
                )
        } else {
            voiceTranscript {
                renderedMessageText
            }
        }
    }

    private var renderedMessageText: some View {
        Group {
            if requiresInlineHighlightRenderer {
                styledMessageText
                    .font(.body)
                    .textRenderer(
                        PrototypeInlineHighlightTextRenderer(
                            mentionBackgroundColor: mentionSurfaceColor,
                            searchBackgroundColor: .cyan
                        )
                    )
            } else if requiresAttributedMessageText {
                Text(attributedMessageText)
                    .font(.body)
            } else {
                Text(message.text)
            }
        }
    }

    private var requiresInlineHighlightRenderer: Bool {
        if let searchQuery {
            let query = searchQuery.trimmingCharacters(
                in: .whitespacesAndNewlines
            )
            if !query.isEmpty,
               message.text.localizedStandardContains(query) {
                return true
            }
        }

        return people.contains { person in
            message.text.contains("@\(person.name)")
        }
    }

    private var requiresAttributedMessageText: Bool {
        attributedMessageText.runs.contains { run in
            run.inlinePresentationIntent != nil || run.link != nil
        }
    }

    private func voiceTranscript<Content: View>(
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text("Transcribed")
                .font(.caption2)
                .foregroundStyle(.secondary)

            content()
        }
        .padding(.top, PrototypeMessageBubbleMetrics.contentSpacing)
        .padding(.horizontal, richTextHorizontalInsetAdjustment)
        .padding(.bottom, richTextVerticalInsetAdjustment)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private var richTextHorizontalInsetAdjustment: CGFloat {
        PrototypeMessageBubbleMetrics.textHorizontalInset
            - PrototypeMessageBubbleMetrics.outerContentInset
    }

    private var richTextVerticalInsetAdjustment: CGFloat {
        PrototypeMessageBubbleMetrics.textVerticalInset
            - PrototypeMessageBubbleMetrics.outerContentInset
    }

    private var attributedMessageText: AttributedString {
        attributedText(message.text)
    }

    private var styledMessageText: Text {
        prototypeInlineHighlightedText(
            attributedMessageText,
            tagsMentions: true
        )
    }

    private func attributedText(_ text: String) -> AttributedString {
        var markdown = text
        for person in people {
            markdown = markdown.replacingOccurrences(
                of: "@\(person.name)",
                with: "[@\(person.name)](whitenoise-person://\(person.id))"
            )
        }
        var attributed = (try? AttributedString(
            markdown: markdown,
            options: .init(interpretedSyntax: .inlineOnlyPreservingWhitespace)
        )) ?? AttributedString(text)
        let links = attributed.runs.compactMap { run -> (Range<AttributedString.Index>, URL)? in
            guard let link = run.link else { return nil }
            return (run.range, link)
        }
        for (range, link) in links {
            attributed[range].foregroundColor = messageColor.foregroundColor
            if link.scheme == "whitenoise-person" {
                attributed[range].font = .body.weight(.semibold)
                attributed[range].underlineStyle = nil
            } else {
                attributed[range].underlineStyle = Text.LineStyle(pattern: .solid)
            }
        }
        return prototypeSearchHighlightedAttributedString(
            attributed,
            query: searchQuery
        )
    }

    private func plainText(_ text: String) -> String {
        String(attributedText(text).characters)
    }

    private var replyQuote: some View {
        Button(action: onOpenReply) {
            HStack(alignment: .top, spacing: 7) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(replyAuthorName)
                        .font(.caption.weight(.semibold))
                    Text(replyPreview)
                        .font(.caption)
                        .lineLimit(2, reservesSpace: false)
                        .fixedSize(horizontal: false, vertical: true)
                        .opacity(0.75)
                }
                Spacer(minLength: 4)
                replyThumbnail
            }
            .padding(.leading, 10)
            .overlay(alignment: .leading) {
                Capsule()
                    .fill(
                        messageColor.foregroundColor.opacity(
                            outgoing ? 0.65 : 0.46
                        )
                    )
                    .frame(width: 3)
                    .frame(maxHeight: .infinity)
            }
            .fixedSize(horizontal: false, vertical: true)
            .padding(.leading, PrototypeMessageBubbleMetrics.richComponentCornerRadius)
            .padding(.trailing, PrototypeMessageBubbleMetrics.richComponentInset)
            .padding(.vertical, PrototypeMessageBubbleMetrics.richComponentInset)
            .frame(
                width: PrototypeMessageBubbleMetrics.richContentWidth,
                alignment: .leading
            )
            .background(
                messageColor.foregroundColor.opacity(richSurfaceOpacity),
                in: .rect(
                    cornerRadius: PrototypeMessageBubbleMetrics.richComponentCornerRadius
                )
            )
        }
        .buttonStyle(.plain)
    }

    private var replyPreview: String {
        guard let resolvedReply else { return "Message unavailable" }
        if resolvedReply.isDeleted { return "Message deleted" }
        if !resolvedReply.text.isEmpty { return plainText(resolvedReply.text) }
        return resolvedReply.attachments.first?.accessibilityLabel ?? "Message"
    }

    @ViewBuilder
    private var replyThumbnail: some View {
        if let attachment = resolvedReply?.attachments.first {
            switch attachment {
            case let .photo(_, source, _, _):
                PrototypeImageSourceView(source: source)
                    .scaledToFill()
                    .frame(width: 38, height: 38)
                    .clipShape(.rect(cornerRadius: 8))
            case let .video(_, _, thumbnail, _, _):
                ZStack {
                    PrototypeImageSourceView(source: thumbnail).scaledToFill()
                    Image(systemName: "play.fill")
                        .font(.caption2)
                        .foregroundStyle(.white)
                        .shadow(radius: 1)
                }
                .frame(width: 38, height: 38)
                .clipShape(.rect(cornerRadius: 8))
            case let .gif(_, assetName, _):
                Image(assetName)
                    .resizable()
                    .scaledToFill()
                    .frame(width: 38, height: 38)
                    .clipShape(.rect(cornerRadius: 8))
            case .file:
                Image(systemName: "doc.fill")
                    .frame(width: 38, height: 38)
                    .background(messageColor.foregroundColor.opacity(0.1), in: .rect(cornerRadius: 8))
            case .voice:
                Image(systemName: "waveform")
                    .frame(width: 38, height: 38)
                    .background(messageColor.foregroundColor.opacity(0.1), in: .rect(cornerRadius: 8))
            case .link:
                Image(systemName: "link")
                    .frame(width: 38, height: 38)
                    .background(messageColor.foregroundColor.opacity(0.1), in: .rect(cornerRadius: 8))
            case .contact:
                Image(systemName: "person.crop.circle")
                    .frame(width: 38, height: 38)
                    .background(messageColor.foregroundColor.opacity(0.1), in: .rect(cornerRadius: 8))
            }
        }
    }

    private var reactionMetadataRow: some View {
        ViewThatFits(in: .horizontal) {
            reactionMetadataCandidate(items: reactionSummaryItems(maximumReactionPills: 7))
            reactionMetadataCandidate(items: reactionSummaryItems(maximumReactionPills: 6))
            reactionMetadataCandidate(items: reactionSummaryItems(maximumReactionPills: 5))
            reactionMetadataCandidate(items: reactionSummaryItems(maximumReactionPills: 4))
            reactionMetadataCandidate(items: reactionSummaryItems(maximumReactionPills: 3))
            reactionMetadataCandidate(items: reactionSummaryItems(maximumReactionPills: 2))
            reactionMetadataCandidate(items: reactionSummaryItems(maximumReactionPills: 1))
            reactionMetadataCandidate(items: reactionSummaryItems(maximumReactionPills: 0))
        }
    }

    private func reactionMetadataCandidate(
        items: [PrototypeReactionSummaryItem]
    ) -> some View {
        PrototypeReactionMetadataLayout(outgoing: outgoing, minimumSpacing: 4) {
            reactionPills(items)

            if showsVisibleTimestamp {
                // Preserve the timestamp's horizontal footprint while its
                // visible copy keeps the standard below-bubble position.
                timestamp
                    .fixedSize()
                    .hidden()
                    .accessibilityHidden(true)
            }
        }
    }

    private func reactionPills(
        _ items: [PrototypeReactionSummaryItem]
    ) -> some View {
        HStack(spacing: PrototypeMessageBubbleMetrics.reactionSpacing) {
            ForEach(items) { item in
                switch item {
                case let .reaction(reaction):
                    Button {
                        onSelectReaction(reaction.emoji)
                    } label: {
                        PrototypeReactionChip(
                            emoji: reaction.emoji,
                            count: reaction.personIDs.count,
                            isSelected: reaction.personIDs.contains(currentProfileID)
                        )
                    }
                    .buttonStyle(PrototypeReactionButtonStyle())
                    .frame(
                        minHeight: PrototypeMessageBubbleMetrics.reactionHitTarget
                    )
                    .contentShape(Rectangle())
                    .accessibilityLabel(reactionAccessibilityLabel(reaction))
                    .accessibilityValue(
                        reaction.personIDs.contains(currentProfileID) ? "You reacted" : ""
                    )
                    .accessibilityAddTraits(
                        reaction.personIDs.contains(currentProfileID) ? .isSelected : []
                    )
                    .accessibilityHint(
                        reaction.personIDs.contains(currentProfileID)
                            ? "Show message actions to remove your reaction."
                            : "Adds this reaction."
                    )

                case let .overflow(count, isSelected):
                    PrototypeReactionChip(
                        overflowCount: count,
                        isSelected: isSelected
                    )
                    .frame(
                        minHeight: PrototypeMessageBubbleMetrics.reactionHitTarget
                    )
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel("\(count) more reaction types")
                    .accessibilityValue(isSelected ? "Includes your reaction" : "")
                }
            }
        }
    }

    private func reactionSummaryItems(
        maximumReactionPills: Int
    ) -> [PrototypeReactionSummaryItem] {
        let visibleCount = min(maximumReactionPills, message.reactions.count)
        let visible = message.reactions.prefix(visibleCount).map {
            PrototypeReactionSummaryItem.reaction($0)
        }
        let hidden = message.reactions.dropFirst(visibleCount)
        guard !hidden.isEmpty else { return visible }

        return visible + [
            .overflow(
                count: hidden.count,
                isSelected: hidden.contains {
                    $0.personIDs.contains(currentProfileID)
                }
            ),
        ]
    }

    private func reactionAccessibilityLabel(_ reaction: PrototypeReaction) -> String {
        let noun = reaction.personIDs.count == 1 ? "reaction" : "reactions"
        return "\(reaction.emoji), \(reaction.personIDs.count) \(noun)"
    }

    private var shape: PrototypeMessageBubbleShape {
        PrototypeMessageBubbleShape()
    }

    private var messageColor: PrototypeMessageColor { outgoing ? outgoingColor : incomingColor }
    private var richSurfaceOpacity: Double { outgoing ? 0.16 : 0.09 }
    private var mentionSurfaceColor: Color {
        messageColor.foregroundColor.opacity(outgoing ? 0.2 : 0.1)
    }
    private var showsVisibleTimestamp: Bool {
        showsTimestamp && message.deliveryState != .failed
    }
    private var showsFailedDeliveryStatus: Bool {
        message.deliveryState == .failed && outgoing && !message.isDeleted
    }
    private var showsReactions: Bool {
        !message.reactions.isEmpty && !message.isDeleted
    }
    private var timestampAlignment: Alignment {
        outgoing ? .bottomLeading : .bottomTrailing
    }
    private var visibleTimestampAlignment: Alignment {
        guard showsReactions else { return timestampAlignment }
        return outgoing ? .bottomTrailing : .bottomLeading
    }
    private var visibleTimestampInsetEdge: Edge.Set {
        guard showsReactions else {
            return outgoing ? .leading : .trailing
        }
        return outgoing ? .trailing : .leading
    }
    private var reactionVerticalOffset: CGFloat {
        let pillInsetWithinHitTarget = (
            PrototypeMessageBubbleMetrics.reactionHitTarget
                - PrototypeMessageBubbleMetrics.reactionPillHeight
        ) / 2

        return PrototypeMessageBubbleMetrics.reactionHitTarget
            - pillInsetWithinHitTarget
            - bubbleVerticalInset
            + PrototypeMessageBubbleMetrics.reactionTextGap
    }
    private var metadataBottomReserve: CGFloat {
        if showsFailedDeliveryStatus { return 18 }
        if showsReactions {
            return max(
                reactionVerticalOffset,
                showsVisibleTimestamp
                    ? PrototypeMessageBubbleMetrics.timestampVerticalOffset
                    : 0
            )
        }
        return showsVisibleTimestamp
            ? PrototypeMessageBubbleMetrics.timestampVerticalOffset
            : 0
    }
    private var hasRichContent: Bool {
        !message.isDeleted
            && (message.replyToMessageID != nil || !message.attachments.isEmpty)
    }
    private var bubbleVerticalInset: CGFloat {
        hasRichContent
            ? PrototypeMessageBubbleMetrics.outerContentInset
            : PrototypeMessageBubbleMetrics.textVerticalInset
    }
    private var richContentWidth: CGFloat {
        let media = message.attachments.filter(\.prototypeIsPhotoOrVideo)
        guard media.count == 1, message.attachments.count == 1 else {
            return PrototypeMessageBubbleMetrics.richContentWidth
        }
        return PrototypeMediaLayout.singleSize(
            dimensions: media[0].prototypeMediaDimensions
        ).width
    }
    private var deletedText: String {
        outgoing ? "You deleted this message." : "This message was deleted."
    }
    private var accessibilitySummary: String {
        let sender = outgoing ? profileName : (author?.name ?? "Unknown")
        let authoredText = voiceAttachment == nil
            ? plainText(message.text)
            : accessibilityTranscript(message.text)
        let localTranscript = visibleVoiceTranscript.map(accessibilityTranscript) ?? ""
        let content = message.isDeleted
            ? deletedText
            : ([authoredText, localTranscript]
                + message.attachments.map(\.accessibilityLabel))
                .filter { !$0.isEmpty }.joined(separator: ", ")
        let reactions = message.reactions.isEmpty ? "" : ", reactions: " + message.reactions.map(\.emoji).joined(separator: ", ")
        let reply = message.replyToMessageID == nil
            ? ""
            : ", reply to \(replyAuthorName): \(replyPreview)"
        let failed = message.deliveryState == .failed ? ", not sent" : ""
        let reading = readAloudProgress == nil ? "" : ", reading aloud"
        return "\(sender), \(PrototypeDateFormatter.time(for: message.sentAt)). \(content)\(reply)\(reactions)\(failed)\(reading)"
    }
    private func accessibilityTranscript(_ text: String) -> String {
        let transcript = plainText(text)
        return transcript.isEmpty ? "" : "Transcribed: \(transcript)"
    }
    private var voiceAttachment: (id: String, duration: TimeInterval)? {
        for attachment in message.attachments {
            if case let .voice(id, _, duration) = attachment {
                return (id, duration)
            }
        }
        return nil
    }
}

private enum PrototypeReactionSummaryItem: Identifiable {
    case reaction(PrototypeReaction)
    case overflow(count: Int, isSelected: Bool)

    var id: String {
        switch self {
        case let .reaction(reaction):
            "reaction-\(reaction.id)"
        case let .overflow(count, isSelected):
            "overflow-\(count)-\(isSelected)"
        }
    }
}

private struct PrototypeMentionAttribute: TextAttribute {}
private struct PrototypeSearchMatchAttribute: TextAttribute {}

private struct PrototypeInlineHighlightTextRenderer: TextRenderer {
    let mentionBackgroundColor: Color?
    let searchBackgroundColor: Color?

    var displayPadding: EdgeInsets {
        guard mentionBackgroundColor != nil else { return EdgeInsets() }
        return EdgeInsets(top: 0, leading: 2, bottom: 0, trailing: 2)
    }

    func draw(layout: Text.Layout, in context: inout GraphicsContext) {
        for line in layout {
            for run in line {
                let backgroundColor: Color?
                if run[PrototypeSearchMatchAttribute.self] != nil {
                    backgroundColor = searchBackgroundColor
                } else if run[PrototypeMentionAttribute.self] != nil {
                    backgroundColor = mentionBackgroundColor
                } else {
                    backgroundColor = nil
                }

                if let backgroundColor {
                    let bounds = run[PrototypeSearchMatchAttribute.self] != nil
                        ? run.typographicBounds.rect
                        : run.typographicBounds.rect.insetBy(dx: -2, dy: 0)
                    context.fill(
                        Path(
                            roundedRect: bounds,
                            cornerRadius: 4,
                            style: .continuous
                        ),
                        with: .color(backgroundColor)
                    )
                }
                context.draw(run)
            }
        }
    }
}

private struct PrototypeAttachmentCollectionView: View {
    let attachments: [PrototypeAttachment]
    let canvasWidth: CGFloat
    let people: [PrototypePerson]
    let tint: Color
    let surfaceOpacity: Double
    let searchQuery: String?
    let accessibilityContext: String
    let onOpenMedia: (PrototypeAttachment) -> Void
    let onOpenFile: (URL) -> Void
    let onOpenPerson: (String) -> Void

    private var media: [PrototypeAttachment] {
        attachments.filter {
            if case .photo = $0 { return true }
            if case .video = $0 { return true }
            return false
        }
    }

    var body: some View {
        VStack(
            alignment: .leading,
            spacing: PrototypeMessageBubbleMetrics.contentSpacing
        ) {
            if !media.isEmpty { mediaGrid }
            ForEach(attachments.filter { !media.contains($0) }) { attachment in
                nonMediaView(attachment)
            }
        }
        .frame(
            width: canvasWidth,
            alignment: .leading
        )
    }

    @ViewBuilder
    private var mediaGrid: some View {
        let layout = resolvedMediaLayout
        ZStack(alignment: .topLeading) {
            ForEach(Array(layout.frames.enumerated()), id: \.offset) { index, frame in
                mediaButton(at: index, frame: frame)
                    .position(x: frame.midX, y: frame.midY)
            }
        }
        .frame(width: layout.size.width, height: layout.size.height)
        .clipShape(
            .rect(
                cornerRadius: PrototypeMessageBubbleMetrics.richComponentCornerRadius
            )
        )
        .accessibilityElement(children: .contain)
        .accessibilityLabel("\(media.count) media items")
    }

    private var resolvedMediaLayout: PrototypeMediaLayout {
        if media.count == 1 {
            let size = PrototypeMediaLayout.singleSize(
                dimensions: media[0].prototypeMediaDimensions
            )
            return PrototypeMediaLayout(
                size: size,
                frames: [CGRect(origin: .zero, size: size)],
                overflowCount: 0
            )
        }
        return PrototypeMediaLayout(count: media.count)
    }

    @ViewBuilder
    private func mediaButton(at index: Int, frame: CGRect) -> some View {
        let attachment = media[index]
        let isOverflow = resolvedMediaLayout.isOverflowTile(at: index)

        if attachment.prototypeMediaIsAvailable {
            Button {
                onOpenMedia(attachment)
            } label: {
                mediaTile(
                    attachment,
                    size: frame.size,
                    isOverflow: isOverflow
                )
                .contentShape(.rect)
            }
            .buttonStyle(.plain)
            .frame(width: frame.width, height: frame.height)
            .contentShape(.rect)
            .accessibilityLabel(
                "\(attachment.accessibilityLabel), \(index + 1) of \(media.count)"
            )
            .accessibilityHint("Opens a preview.")
        } else {
            mediaTile(
                attachment,
                size: frame.size,
                isOverflow: isOverflow
            )
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(attachment.accessibilityLabel), unavailable")
        }
    }

    @ViewBuilder
    private func mediaTile(
        _ attachment: PrototypeAttachment,
        size: CGSize,
        isOverflow: Bool
    ) -> some View {
        ZStack {
            switch attachment {
            case let .photo(_, source, _, _):
                PrototypeImageSourceView(source: source)
                    .scaledToFill()
                    .frame(width: size.width, height: size.height)
                    .clipped()
            case let .video(_, _, thumbnail, duration, _):
                PrototypeImageSourceView(source: thumbnail)
                    .scaledToFill()
                    .frame(width: size.width, height: size.height)
                    .clipped()
                if attachment.prototypeMediaIsAvailable, !isOverflow {
                    Image(systemName: "play.circle.fill")
                        .font(.largeTitle).foregroundStyle(.white)
                        .shadow(radius: 2)
                    Text(prototypeDurationString(duration))
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(.white)
                        .padding(5)
                        .background(.black.opacity(0.55), in: .capsule)
                        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottomTrailing)
                        .padding(6)
                } else if !attachment.prototypeMediaIsAvailable, !isOverflow {
                    Color.black.opacity(0.16)
                    Image(systemName: "video.badge.exclamationmark")
                        .font(.title2)
                        .foregroundStyle(.white)
                        .shadow(radius: 2)
                }
            default:
                EmptyView()
            }
            if isOverflow {
                Color.black.opacity(0.5)
                Text("+\(media.count - PrototypeMediaLayout.maximumVisibleItems)")
                    .font(.title2.bold()).foregroundStyle(.white)
            }
        }
        .frame(width: size.width, height: size.height)
        .clipped()
    }

    @ViewBuilder
    private func nonMediaView(_ attachment: PrototypeAttachment) -> some View {
        switch attachment {
        case let .file(_, name, size, url):
            if let url {
                Button {
                    onOpenFile(url)
                } label: {
                    fileRow(name: name, size: size, isAvailable: true)
                }
                .buttonStyle(.plain)
                .accessibilityHint("Opens a preview.")
            } else {
                fileRow(name: name, size: size, isAvailable: false)
                    .accessibilityElement(children: .combine)
                    .accessibilityLabel("File, \(name), unavailable")
            }
        case let .voice(id, _, duration):
            PrototypeVoiceBubble(
                id: id,
                duration: duration,
                tintColor: UIColor(tint),
                accessibilityContext: accessibilityContext
            )
        case let .link(_, title, domain, summary, image):
            if let destination = safeLinkDestination(domain: domain) {
                Link(destination: destination) {
                    linkPreview(title: title, domain: domain, summary: summary, image: image)
                }
                .buttonStyle(.plain)
            } else {
                linkPreview(title: title, domain: domain, summary: summary, image: image)
                    .accessibilityElement(children: .combine)
                    .accessibilityLabel("Link preview unavailable, \(title), \(domain)")
            }
        case let .gif(_, assetName, label):
            ZStack(alignment: .bottomLeading) {
                Image(assetName)
                    .resizable()
                    .scaledToFill()
                    .frame(
                        width: PrototypeMessageBubbleMetrics.richContentWidth,
                        height: 188
                    )
                    .clipped()
                Label("GIF", systemImage: "play.fill")
                    .font(.caption.bold()).padding(6).background(.black.opacity(0.6), in: .capsule)
                    .foregroundStyle(.white).padding(7)
            }
            .clipShape(
                .rect(
                    cornerRadius: PrototypeMessageBubbleMetrics.richComponentCornerRadius
                )
            )
            .accessibilityLabel("GIF, \(label)")
        case let .contact(_, personID):
            if let person = people.first(where: { $0.id == personID }) {
                Button { onOpenPerson(personID) } label: {
                    HStack(spacing: 10) {
                        PrototypeChatAvatarView(
                            avatar: person.avatar,
                            size: 38,
                            publicKey: person.publicKey
                        )
                        VStack(alignment: .leading, spacing: 2) {
                            Text(person.name).font(.headline)
                            Text(person.shortPublicKey).font(.caption).foregroundStyle(tint.opacity(0.72))
                        }
                        Spacer(minLength: 4)
                        Image(systemName: "chevron.right")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(tint.opacity(0.72))
                            .frame(width: 20, height: 24)
                            .padding(.trailing, 4)
                    }
                    .padding(PrototypeMessageBubbleMetrics.richComponentInset)
                    .frame(width: PrototypeMessageBubbleMetrics.richContentWidth)
                    .background(
                        tint.opacity(surfaceOpacity),
                        in: .rect(
                            cornerRadius: PrototypeMessageBubbleMetrics.richComponentCornerRadius
                        )
                    )
                }
                .buttonStyle(.plain)
            }
        case .photo, .video:
            EmptyView()
        }
    }

    private func fileRow(name: String, size: Int, isAvailable: Bool) -> some View {
        HStack(spacing: 8) {
            Image(systemName: fileSymbol(for: name))
                .font(.title3.weight(.medium))
                .frame(width: 24, alignment: .leading)
            VStack(alignment: .leading, spacing: 2) {
                prototypeSearchHighlightedText(name, query: searchQuery)
                    .font(.subheadline.weight(.semibold))
                    .lineLimit(1)
                Text(fileMetadata(name: name, size: size))
                    .font(.caption).foregroundStyle(tint.opacity(0.72))
            }
            Spacer(minLength: 8)
            Image(systemName: isAvailable ? "chevron.right" : "exclamationmark.triangle")
                .font(.caption.weight(.semibold))
                .foregroundStyle(tint.opacity(0.72))
                .frame(width: 20, height: 24)
                .padding(.trailing, 4)
        }
        .padding(PrototypeMessageBubbleMetrics.richComponentInset)
        .frame(width: PrototypeMessageBubbleMetrics.richContentWidth)
        .background(
            tint.opacity(surfaceOpacity),
            in: .rect(
                cornerRadius: PrototypeMessageBubbleMetrics.richComponentCornerRadius
            )
        )
    }

    private func linkPreview(
        title: String,
        domain: String,
        summary: String,
        image: PrototypeImageSource?
    ) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            if let image {
                PrototypeImageSourceView(source: image)
                    .scaledToFill()
                    .frame(
                        width: PrototypeMessageBubbleMetrics.richContentWidth,
                        height: 124
                    )
                    .clipped()
            }
            VStack(alignment: .leading, spacing: 4) {
                Label {
                    prototypeSearchHighlightedText(domain, query: searchQuery)
                } icon: {
                    Image(systemName: "link")
                }
                    .font(.caption2.weight(.medium))
                    .foregroundStyle(tint.opacity(0.72))
                    .lineLimit(1)
                prototypeSearchHighlightedText(title, query: searchQuery)
                    .font(.headline)
                    .lineLimit(2)
                prototypeSearchHighlightedText(summary, query: searchQuery)
                    .font(.subheadline)
                    .lineLimit(3)
                    .opacity(0.78)
            }
            .padding(PrototypeMessageBubbleMetrics.richComponentInset)
        }
        .frame(
            width: PrototypeMessageBubbleMetrics.richContentWidth,
            alignment: .leading
        )
        .background(
            tint.opacity(surfaceOpacity),
            in: .rect(
                cornerRadius: PrototypeMessageBubbleMetrics.richComponentCornerRadius
            )
        )
        .clipShape(
            .rect(
                cornerRadius: PrototypeMessageBubbleMetrics.richComponentCornerRadius
            )
        )
    }

    private func safeLinkDestination(domain: String) -> URL? {
        guard !domain.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let url = URL(string: "https://\(domain)"),
              url.host?.isEmpty == false
        else { return nil }
        return url
    }

    private func fileMetadata(name: String, size: Int) -> String {
        let fileType = URL(fileURLWithPath: name).pathExtension.uppercased()
        let formattedSize = size.formatted(.byteCount(style: .file))
        return fileType.isEmpty ? formattedSize : "\(fileType) • \(formattedSize)"
    }

    private func fileSymbol(for name: String) -> String {
        switch URL(fileURLWithPath: name).pathExtension.lowercased() {
        case "pdf": "doc.richtext"
        case "doc", "docx", "txt": "doc.text"
        case "xls", "xlsx": "tablecells"
        case "zip": "archivebox"
        default: "doc"
        }
    }
}

private struct PrototypeMessageContextLongPressGesture: UIGestureRecognizerRepresentable {
    let minimumDuration: TimeInterval
    let maximumMovement: CGFloat
    let onRecognized: () -> Void
    let onCancelled: () -> Void

    final class Coordinator: NSObject, UIGestureRecognizerDelegate {
        var recognitionLocation: CGPoint?
        var cancelledForMovement = false

        func gestureRecognizer(
            _ gestureRecognizer: UIGestureRecognizer,
            shouldRecognizeSimultaneouslyWith otherGestureRecognizer: UIGestureRecognizer
        ) -> Bool {
            otherGestureRecognizer is UIPanGestureRecognizer
        }

        func gestureRecognizer(
            _ gestureRecognizer: UIGestureRecognizer,
            shouldBeRequiredToFailBy otherGestureRecognizer: UIGestureRecognizer
        ) -> Bool {
            guard !(otherGestureRecognizer is UIPanGestureRecognizer),
                  let gestureView = gestureRecognizer.view,
                  let otherView = otherGestureRecognizer.view
            else { return false }

            return otherView === gestureView
                || otherView.isDescendant(of: gestureView)
        }
    }

    func makeCoordinator(converter: CoordinateSpaceConverter) -> Coordinator {
        Coordinator()
    }

    func makeUIGestureRecognizer(context: Context) -> UILongPressGestureRecognizer {
        let recognizer = UILongPressGestureRecognizer()
        configure(recognizer, coordinator: context.coordinator)
        return recognizer
    }

    func updateUIGestureRecognizer(
        _ recognizer: UILongPressGestureRecognizer,
        context: Context
    ) {
        configure(recognizer, coordinator: context.coordinator)
    }

    func handleUIGestureRecognizerAction(
        _ recognizer: UILongPressGestureRecognizer,
        context: Context
    ) {
        switch recognizer.state {
        case .began:
            context.coordinator.recognitionLocation = recognizer.location(in: recognizer.view)
            context.coordinator.cancelledForMovement = false
            onRecognized()

        case .changed:
            guard !context.coordinator.cancelledForMovement,
                  let recognitionLocation = context.coordinator.recognitionLocation
            else { return }

            let location = recognizer.location(in: recognizer.view)
            let movement = hypot(
                location.x - recognitionLocation.x,
                location.y - recognitionLocation.y
            )
            guard movement > maximumMovement else { return }

            context.coordinator.cancelledForMovement = true
            onCancelled()

        case .ended, .cancelled, .failed:
            context.coordinator.recognitionLocation = nil
            context.coordinator.cancelledForMovement = false
            onCancelled()

        case .possible:
            break

        @unknown default:
            onCancelled()
        }
    }

    private func configure(
        _ recognizer: UILongPressGestureRecognizer,
        coordinator: Coordinator
    ) {
        recognizer.minimumPressDuration = minimumDuration
        recognizer.allowableMovement = maximumMovement
        recognizer.cancelsTouchesInView = true
        recognizer.delegate = coordinator
    }
}

private struct PrototypeMessageReplyPanGesture: UIGestureRecognizerRepresentable {
    let isEnabled: Bool
    let onChanged: (CGPoint) -> Void
    let onEnded: (CGPoint) -> Void
    let onCancelled: () -> Void

    final class Coordinator: NSObject, UIGestureRecognizerDelegate {
        var isEnabled = true

        func gestureRecognizerShouldBegin(_ gestureRecognizer: UIGestureRecognizer) -> Bool {
            guard isEnabled,
                  let panGestureRecognizer = gestureRecognizer as? UIPanGestureRecognizer
            else { return false }

            let velocity = panGestureRecognizer.velocity(in: panGestureRecognizer.view)
            return abs(velocity.x) > abs(velocity.y)
        }
    }

    func makeCoordinator(converter: CoordinateSpaceConverter) -> Coordinator {
        Coordinator()
    }

    func makeUIGestureRecognizer(context: Context) -> UIPanGestureRecognizer {
        let recognizer = UIPanGestureRecognizer()
        recognizer.maximumNumberOfTouches = 1
        recognizer.cancelsTouchesInView = true
        configure(recognizer, coordinator: context.coordinator)
        return recognizer
    }

    func updateUIGestureRecognizer(
        _ recognizer: UIPanGestureRecognizer,
        context: Context
    ) {
        configure(recognizer, coordinator: context.coordinator)
    }

    func handleUIGestureRecognizerAction(
        _ recognizer: UIPanGestureRecognizer,
        context: Context
    ) {
        let translation = recognizer.translation(in: recognizer.view)

        switch recognizer.state {
        case .began, .changed:
            onChanged(translation)

        case .ended:
            onEnded(translation)

        case .cancelled, .failed:
            onCancelled()

        case .possible:
            break

        @unknown default:
            onCancelled()
        }
    }

    private func configure(
        _ recognizer: UIPanGestureRecognizer,
        coordinator: Coordinator
    ) {
        coordinator.isEnabled = isEnabled
        recognizer.delegate = coordinator
        recognizer.isEnabled = isEnabled
        recognizer.cancelsTouchesInView = true
    }
}

private struct PrototypeVoicePlaybackButton: UIViewRepresentable {
    let id: String
    let isPlaying: Bool
    let tintColor: UIColor
    let elapsed: TimeInterval
    let duration: TimeInterval
    let accessibilityContext: String
    let action: () -> Void

    final class Coordinator: NSObject {
        var action: () -> Void

        init(action: @escaping () -> Void) {
            self.action = action
        }

        @objc func activate() {
            action()
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(action: action)
    }

    func makeUIView(context: Context) -> UIButton {
        let button = UIButton(type: .system)
        button.addTarget(
            context.coordinator,
            action: #selector(Coordinator.activate),
            for: .touchUpInside
        )
        return button
    }

    func updateUIView(_ button: UIButton, context: Context) {
        context.coordinator.action = action
        button.tintColor = tintColor
        button.setImage(
            UIImage(systemName: isPlaying ? "pause.fill" : "play.fill"),
            for: .normal
        )
        button.isAccessibilityElement = true
        button.accessibilityIdentifier = "voice.\(id).toggle"
        button.accessibilityLabel = isPlaying
            ? "Pause Voice Message"
            : "Play Voice Message"
        button.accessibilityValue = "\(prototypeDurationString(elapsed)) of \(prototypeDurationString(duration))"
        button.accessibilityHint = accessibilityContext
    }
}

private struct PrototypeVoiceBubble: View {
    let id: String
    let duration: TimeInterval
    let tintColor: UIColor
    let accessibilityContext: String
    @ObservedObject private var playback = PrototypePlaybackCoordinator.shared

    private var isActive: Bool { playback.activeVoiceID == id }
    private var isPlaying: Bool { isActive && !playback.isPaused }
    private var elapsed: TimeInterval { isActive ? playback.elapsed : 0 }
    private var progress: Double {
        guard isActive, duration > 0 else { return 0 }
        return min(max(elapsed / duration, 0), 1)
    }

    var body: some View {
        HStack(spacing: 12) {
            HStack(spacing: 0) {
                PrototypeVoicePlaybackButton(
                    id: id,
                    isPlaying: isPlaying,
                    tintColor: tintColor,
                    elapsed: elapsed,
                    duration: duration,
                    accessibilityContext: accessibilityContext,
                    action: {
                        playback.toggleVoice(id: id, duration: duration)
                    }
                )
                .frame(width: 44, height: 44)

                PrototypeAudioWaveform(
                    samples: playback.waveform(for: id),
                    progress: progress
                )
                .frame(minWidth: 120, maxWidth: .infinity)
                .frame(height: 32)
                .accessibilityHidden(true)
            }
            .frame(maxWidth: .infinity)

            Text(
                prototypeDurationString(
                    isActive ? max(0, duration - elapsed) : duration
                )
            )
            .font(.caption.monospacedDigit())
            .lineLimit(1)
            .fixedSize(horizontal: true, vertical: false)
            .accessibilityHidden(true)
        }
        .frame(width: PrototypeMessageBubbleMetrics.richContentWidth)
    }
}

private struct PrototypeMessageAccessibilityModifier: ViewModifier {
    let containsVoice: Bool
    let summary: String
    let identifier: String

    @ViewBuilder
    func body(content: Content) -> some View {
        if containsVoice {
            content
        } else {
            content
                .accessibilityElement(children: .combine)
                .accessibilityLabel(summary)
                .accessibilityIdentifier(identifier)
        }
    }
}

func prototypeDurationString(_ seconds: TimeInterval) -> String {
    let total = max(0, Int(seconds.rounded()))
    return String(format: "%d:%02d", total / 60, total % 60)
}
