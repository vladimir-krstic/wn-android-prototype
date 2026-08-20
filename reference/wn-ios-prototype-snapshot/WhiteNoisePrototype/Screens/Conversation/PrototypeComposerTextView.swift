import SwiftUI
import UIKit

@MainActor
struct PrototypeComposerTextView: UIViewRepresentable {
    @Binding var text: String

    let mentionNames: [String]
    let isFocused: Bool
    let isEnabled: Bool
    let sendsWithReturn: Bool
    let maximumVisibleLines: Int
    let usesAvailableHeight: Bool
    let onFocusChange: (Bool) -> Void
    let onSubmit: () -> Void

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIView(context: Context) -> UITextView {
        let textView = UITextView(usingTextLayoutManager: true)
        textView.delegate = context.coordinator
        textView.backgroundColor = .clear
        textView.adjustsFontForContentSizeCategory = true
        textView.isScrollEnabled = false
        textView.showsVerticalScrollIndicator = false
        textView.textContainerInset = UIEdgeInsets(
            top: 10,
            left: 4,
            bottom: 10,
            right: 4
        )
        textView.textContainer.lineFragmentPadding = 0
        textView.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        textView.setContentHuggingPriority(.defaultLow, for: .horizontal)
        textView.setContentCompressionResistancePriority(.defaultLow, for: .vertical)
        textView.setContentHuggingPriority(.defaultLow, for: .vertical)
        textView.accessibilityLabel = "Message"
        textView.accessibilityIdentifier = "conversation.composer"
        applyText(
            to: textView,
            mentionExpressions: context.coordinator.mentionExpressions
        )
        return textView
    }

    func updateUIView(_ textView: UITextView, context: Context) {
        context.coordinator.updateParent(self)

        // UIKit interaction must remain independent from editability. Turning
        // it off can force an active responder to dismiss its keyboard.
        textView.isEditable = isEnabled
        textView.isSelectable = isEnabled
        textView.returnKeyType = sendsWithReturn ? .send : .default
        textView.keyboardDismissMode = .none
        textView.showsVerticalScrollIndicator = usesAvailableHeight
        textView.textHighlightAttributes = [
            .foregroundColor: UIColor.label,
            .backgroundColor: UIColor.secondarySystemFill,
        ]

        if textView.text != text || context.coordinator.styleSignature != styleSignature {
            applyText(
                to: textView,
                mentionExpressions: context.coordinator.mentionExpressions
            )
            context.coordinator.styleSignature = styleSignature
        }

        if context.coordinator.maximumVisibleLines != maximumVisibleLines {
            context.coordinator.maximumVisibleLines = maximumVisibleLines
            textView.invalidateIntrinsicContentSize()
        }

        if context.coordinator.usesAvailableHeight != usesAvailableHeight {
            context.coordinator.usesAvailableHeight = usesAvailableHeight
            textView.invalidateIntrinsicContentSize()
            DispatchQueue.main.async { [weak textView] in
                guard let textView else { return }
                textView.scrollRangeToVisible(textView.selectedRange)
            }
        }

        if isFocused != textView.isFirstResponder {
            DispatchQueue.main.async { [weak textView, weak coordinator = context.coordinator] in
                guard let textView, let coordinator else { return }
                if coordinator.parent.isFocused {
                    textView.becomeFirstResponder()
                } else {
                    textView.resignFirstResponder()
                }
            }
        }
    }

    func sizeThatFits(
        _ proposal: ProposedViewSize,
        uiView textView: UITextView,
        context: Context
    ) -> CGSize? {
        guard let width = proposal.width, width.isFinite, width > 0 else {
            return nil
        }

        let measured = textView.sizeThatFits(
            CGSize(width: width, height: .greatestFiniteMagnitude)
        )
        let lineHeight = textView.font?.lineHeight
            ?? UIFont.preferredFont(forTextStyle: .body).lineHeight
        if usesAvailableHeight,
           let height = proposal.height,
           height.isFinite,
           height > 0 {
            textView.isScrollEnabled = true
            return CGSize(width: width, height: max(44, height))
        }

        let maximumHeight = (lineHeight * CGFloat(max(1, maximumVisibleLines)))
            + textView.textContainerInset.top
            + textView.textContainerInset.bottom
        let resolvedHeight = min(max(measured.height, 44), maximumHeight)
        textView.isScrollEnabled = measured.height > maximumHeight

        return CGSize(width: width, height: resolvedHeight)
    }

    private var styleSignature: Int {
        var hasher = Hasher()
        hasher.combine(mentionNames)
        hasher.combine(colorScheme)
        hasher.combine(dynamicTypeSize)
        return hasher.finalize()
    }

    private func applyText(
        to textView: UITextView,
        mentionExpressions: [NSRegularExpression]
    ) {
        guard textView.markedTextRange == nil else { return }

        let selection = textView.selectedRange
        let bodyFont = UIFont.preferredFont(forTextStyle: .body)
        let mentionFont = UIFont.systemFont(
            ofSize: bodyFont.pointSize,
            weight: .semibold
        )
        let baseAttributes: [NSAttributedString.Key: Any] = [
            .font: bodyFont,
            .foregroundColor: UIColor.label,
        ]
        let attributedText = NSMutableAttributedString(
            string: text,
            attributes: baseAttributes
        )

        for range in mentionRanges(
            in: text,
            expressions: mentionExpressions
        ) {
            attributedText.addAttributes(
                [
                    .font: mentionFont,
                    .textHighlightStyle: NSAttributedString.TextHighlightStyle.default,
                    .textHighlightColorScheme: NSAttributedString.TextHighlightColorScheme.default,
                ],
                range: range
            )
        }

        textView.attributedText = attributedText
        textView.typingAttributes = baseAttributes
        textView.selectedRange = NSRange(
            location: min(selection.location, attributedText.length),
            length: min(
                selection.length,
                max(0, attributedText.length - min(selection.location, attributedText.length))
            )
        )
    }

    private func mentionRanges(
        in value: String,
        expressions: [NSRegularExpression]
    ) -> [NSRange] {
        let fullRange = NSRange(value.startIndex..., in: value)
        var ranges: [NSRange] = []

        for expression in expressions {
            for match in expression.matches(in: value, range: fullRange) {
                guard !ranges.contains(where: { NSIntersectionRange($0, match.range).length > 0 })
                else { continue }
                ranges.append(match.range)
            }
        }

        return ranges
    }

    private static func makeMentionExpressions(
        for names: [String]
    ) -> [NSRegularExpression] {
        names.sorted(by: { $0.count > $1.count }).compactMap { name in
            let token = NSRegularExpression.escapedPattern(for: "@\(name)")
            let pattern = "(?<![\\p{L}\\p{N}_])\(token)(?=$|[^\\p{L}\\p{N}_])"
            return try? NSRegularExpression(pattern: pattern)
        }
    }

    final class Coordinator: NSObject, UITextViewDelegate {
        var parent: PrototypeComposerTextView
        private(set) var mentionExpressions: [NSRegularExpression]
        private var mentionNames: [String]
        var styleSignature: Int?
        var maximumVisibleLines: Int?
        var usesAvailableHeight: Bool?

        init(parent: PrototypeComposerTextView) {
            self.parent = parent
            mentionNames = parent.mentionNames
            mentionExpressions = PrototypeComposerTextView
                .makeMentionExpressions(for: parent.mentionNames)
        }

        func updateParent(_ parent: PrototypeComposerTextView) {
            self.parent = parent
            guard mentionNames != parent.mentionNames else { return }
            mentionNames = parent.mentionNames
            mentionExpressions = PrototypeComposerTextView
                .makeMentionExpressions(for: parent.mentionNames)
        }

        func textViewDidChange(_ textView: UITextView) {
            let updatedText = textView.text ?? ""
            if parent.text != updatedText {
                parent.text = updatedText
            }
            parent.applyText(
                to: textView,
                mentionExpressions: mentionExpressions
            )
            styleSignature = parent.styleSignature
            textView.invalidateIntrinsicContentSize()
        }

        func textViewDidBeginEditing(_ textView: UITextView) {
            if !parent.isFocused {
                parent.onFocusChange(true)
            }
        }

        func textViewDidEndEditing(_ textView: UITextView) {
            if parent.isFocused {
                parent.onFocusChange(false)
            }
        }

        func textView(
            _ textView: UITextView,
            shouldChangeTextIn range: NSRange,
            replacementText replacement: String
        ) -> Bool {
            guard replacement == "\n", parent.sendsWithReturn else { return true }
            parent.onSubmit()
            return false
        }
    }
}
