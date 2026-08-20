import SwiftUI

enum PrototypeMessageBubbleMetrics {
    static let cornerRadius: CGFloat = 18
    static let outerContentInset: CGFloat = 6
    static let textHorizontalInset: CGFloat = 12
    static let textVerticalInset: CGFloat = 8
    static let richContentWidth: CGFloat = 256
    static let richComponentInset: CGFloat = 6
    static let richComponentCornerRadius = cornerRadius - outerContentInset
    static let contentSpacing: CGFloat = 6
    static let gallerySpacing: CGFloat = 2

    // Reaction chips follow Signal's compact content metrics, with the
    // user-approved 22-point visible pill and one-point separator border. The
    // larger interaction frame remains available around that visible pill.
    static let reactionSpacing: CGFloat = 3
    static let reactionHitTarget: CGFloat = 40
    static let reactionPillHeight: CGFloat = 22
    static let reactionPillVerticalInset = (reactionHitTarget - reactionPillHeight) / 2
    static let reactionEmojiSize: CGFloat = 14
    static let reactionCountSize: CGFloat = 12
    static let reactionHorizontalInset: CGFloat = 7
    static let reactionContentSpacing: CGFloat = 2
    static let reactionTextGap: CGFloat = 1
    static let timestampVerticalOffset: CGFloat = 15
    static let reactionVerticalOffset = reactionHitTarget
        - reactionPillVerticalInset
        - textVerticalInset
        + reactionTextGap
    static let reactionReservedSpace = reactionVerticalOffset
    static let reactionEdgeInset = textHorizontalInset
}

struct PrototypeReactionChip: View {
    private let emoji: String?
    private let countText: String?
    let isSelected: Bool

    init(emoji: String, count: Int, isSelected: Bool) {
        self.emoji = emoji
        self.countText = count > 1 ? count.formatted() : nil
        self.isSelected = isSelected
    }

    init(overflowCount: Int, isSelected: Bool) {
        emoji = nil
        countText = "+\(overflowCount.formatted())"
        self.isSelected = isSelected
    }

    var body: some View {
        HStack(spacing: PrototypeMessageBubbleMetrics.reactionContentSpacing) {
            if let emoji {
                Text(emoji)
                    .font(
                        .system(
                            size: PrototypeMessageBubbleMetrics.reactionEmojiSize,
                            weight: .bold
                        )
                    )
            }

            if let countText {
                Text(countText)
                    .font(
                        .system(
                            size: PrototypeMessageBubbleMetrics.reactionCountSize,
                            weight: .bold,
                            design: .monospaced
                        )
                    )
                    .monospacedDigit()
            }
        }
        .foregroundStyle(.primary)
        .padding(.horizontal, PrototypeMessageBubbleMetrics.reactionHorizontalInset)
        .frame(height: PrototypeMessageBubbleMetrics.reactionPillHeight)
        .background {
            Capsule()
                .fill(surface)
                .overlay {
                    Capsule()
                        .strokeBorder(Color(uiColor: .separator), lineWidth: 1)
                }
        }
        .fixedSize()
        .transaction { transaction in
            transaction.animation = nil
            transaction.disablesAnimations = true
        }
    }

    private var surface: Color {
        Color(uiColor: isSelected ? .systemGray4 : .secondarySystemBackground)
    }

}

/// Places the reaction summary at the bubble's conversation-center edge while
/// reserving the timestamp's width at the opposite edge. Reporting the row's
/// ideal width allows `ViewThatFits` to choose a progressively shorter reaction
/// summary before either element can collide or wrap.
struct PrototypeReactionMetadataLayout: Layout {
    let outgoing: Bool
    let minimumSpacing: CGFloat

    func sizeThatFits(
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) -> CGSize {
        let sizes = subviews.map { $0.sizeThatFits(.unspecified) }
        let contentWidth = sizes.reduce(0) { $0 + $1.width }
            + (sizes.count > 1 ? minimumSpacing : 0)
        let proposedWidth = proposal.width ?? contentWidth

        return CGSize(
            width: proposedWidth >= contentWidth ? proposedWidth : contentWidth,
            height: sizes.map(\.height).max() ?? 0
        )
    }

    func placeSubviews(
        in bounds: CGRect,
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) {
        guard let reactions = subviews.first else { return }
        let reactionSize = reactions.sizeThatFits(.unspecified)
        let reactionOrigin = CGPoint(
            x: outgoing ? bounds.minX : bounds.maxX - reactionSize.width,
            y: bounds.midY - reactionSize.height / 2
        )
        reactions.place(
            at: reactionOrigin,
            anchor: .topLeading,
            proposal: ProposedViewSize(
                width: reactionSize.width,
                height: reactionSize.height
            )
        )

        guard subviews.count > 1 else { return }
        let timestampIndex = subviews.index(after: subviews.startIndex)
        let timestamp = subviews[timestampIndex]
        let timestampSize = timestamp.sizeThatFits(.unspecified)
        let timestampOrigin = CGPoint(
            x: outgoing ? bounds.maxX - timestampSize.width : bounds.minX,
            y: bounds.midY - timestampSize.height / 2
        )
        timestamp.place(
            at: timestampOrigin,
            anchor: .topLeading,
            proposal: ProposedViewSize(
                width: timestampSize.width,
                height: timestampSize.height
            )
        )
    }
}

struct PrototypeReactionButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(1)
            .transaction { transaction in
                transaction.animation = nil
                transaction.disablesAnimations = true
            }
    }
}

struct PrototypeMessageBubbleShape: Shape {
    static let cornerRadius = PrototypeMessageBubbleMetrics.cornerRadius

    func path(in rect: CGRect) -> Path {
        let resolvedCornerRadius = min(rect.height / 2, Self.cornerRadius)

        return RoundedRectangle(
            cornerRadius: resolvedCornerRadius,
            style: .continuous
        ).path(in: rect)
    }
}

struct MessageRow<Content: View>: View {
    let outgoing: Bool
    let time: String
    let reaction: String?
    @ViewBuilder let content: Content

    init(
        outgoing: Bool,
        time: String,
        reaction: String? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.outgoing = outgoing
        self.time = time
        self.reaction = reaction
        self.content = content()
    }

    var body: some View {
        HStack(alignment: .bottom, spacing: 0) {
            if outgoing {
                Spacer(minLength: 56)
            }

            VStack(alignment: timestampAlignment, spacing: 4) {
                MessageBubble(outgoing: outgoing) {
                    content
                }
                .overlay(alignment: reactionAlignment) {
                    if let reaction {
                        PrototypeReactionChip(
                            emoji: reaction,
                            count: 1,
                            isSelected: false
                        )
                            .frame(
                                minHeight: PrototypeMessageBubbleMetrics.reactionHitTarget
                            )
                            .offset(
                                x: outgoing
                                    ? PrototypeMessageBubbleMetrics.reactionEdgeInset
                                    : -PrototypeMessageBubbleMetrics.reactionEdgeInset,
                                y: PrototypeMessageBubbleMetrics.reactionVerticalOffset
                            )
                            .accessibilityHidden(true)
                    }
                }
                .padding(
                    .bottom,
                    reaction == nil ? 0 : PrototypeMessageBubbleMetrics.reactionReservedSpace
                )

                Text(time)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .padding(
                        outgoing ? .leading : .trailing,
                        PrototypeMessageBubbleMetrics.textHorizontalInset
                    )
            }

            if !outgoing {
                Spacer(minLength: 56)
            }
        }
        .frame(maxWidth: .infinity)
    }

    private var timestampAlignment: HorizontalAlignment {
        outgoing ? .leading : .trailing
    }

    private var reactionAlignment: Alignment {
        outgoing ? .bottomLeading : .bottomTrailing
    }
}

private struct MessageBubble<Content: View>: View {
    @Environment(\.incomingPrototypeMessageColor)
    private var incomingMessageColor
    @Environment(\.outgoingPrototypeMessageColor)
    private var outgoingMessageColor

    let outgoing: Bool
    @ViewBuilder let content: Content

    var body: some View {
        content
            .padding(.horizontal, PrototypeMessageBubbleMetrics.textHorizontalInset)
            .padding(.vertical, PrototypeMessageBubbleMetrics.textVerticalInset)
            .foregroundStyle(foregroundStyle)
            .background(
                fill,
                in: PrototypeMessageBubbleShape()
            )
    }

    private var fill: Color {
        messageColor.color
    }

    private var foregroundStyle: Color {
        messageColor.foregroundColor
    }

    private var messageColor: PrototypeMessageColor {
        outgoing ? outgoingMessageColor : incomingMessageColor
    }

}

private struct IncomingPrototypeMessageColorKey: EnvironmentKey {
    static let defaultValue = PrototypeMessageColor.gray
}

private struct OutgoingPrototypeMessageColorKey: EnvironmentKey {
    static let defaultValue = PrototypeMessageColor.black
}

extension EnvironmentValues {
    var incomingPrototypeMessageColor: PrototypeMessageColor {
        get { self[IncomingPrototypeMessageColorKey.self] }
        set { self[IncomingPrototypeMessageColorKey.self] = newValue }
    }

    var outgoingPrototypeMessageColor: PrototypeMessageColor {
        get { self[OutgoingPrototypeMessageColorKey.self] }
        set { self[OutgoingPrototypeMessageColorKey.self] = newValue }
    }
}
