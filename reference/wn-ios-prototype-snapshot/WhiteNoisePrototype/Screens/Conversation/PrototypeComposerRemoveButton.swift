import SwiftUI

enum PrototypeComposerRemoveButtonAppearance {
    case secondary
    case mediaOverlay
}

struct PrototypeComposerRemoveButton: View {
    let accessibilityLabel: String
    let accessibilityIdentifier: String
    var appearance: PrototypeComposerRemoveButtonAppearance = .secondary
    let action: () -> Void

    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    var body: some View {
        Button(action: action) {
            visibleControl
                .padding([.top, .trailing], 6)
                .frame(width: 44, height: 44, alignment: .topTrailing)
                .contentShape(.rect)
        }
        .buttonStyle(PrototypeComposerRemoveButtonStyle())
        .accessibilityLabel(accessibilityLabel)
        .accessibilityIdentifier(accessibilityIdentifier)
    }

    private var visibleControl: some View {
        ZStack {
            Circle()
                .fill(controlFill)
            Circle()
                .stroke(controlStroke, lineWidth: 0.5)
            Image(systemName: "xmark")
                .font(.caption2.weight(.semibold))
                .foregroundStyle(glyphStyle)
        }
        .frame(width: 20, height: 20)
        .shadow(
            color: appearance == .mediaOverlay
                ? .black.opacity(0.16)
                : .clear,
            radius: 1,
            y: 0.5
        )
    }

    private var controlFill: Color {
        switch appearance {
        case .secondary:
            .secondary.opacity(0.22)
        case .mediaOverlay:
            .black.opacity(colorSchemeContrast == .increased ? 0.74 : 0.58)
        }
    }

    private var controlStroke: Color {
        switch appearance {
        case .secondary:
            .primary.opacity(0.08)
        case .mediaOverlay:
            .white.opacity(colorSchemeContrast == .increased ? 0.52 : 0.32)
        }
    }

    private var glyphStyle: Color {
        switch appearance {
        case .secondary:
            .primary.opacity(0.72)
        case .mediaOverlay:
            .white.opacity(0.96)
        }
    }
}

private struct PrototypeComposerRemoveButtonStyle: ButtonStyle {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .opacity(configuration.isPressed ? 0.72 : 1)
            .scaleEffect(
                configuration.isPressed && !reduceMotion ? 0.94 : 1
            )
            .animation(.smooth(duration: 0.12), value: configuration.isPressed)
    }
}
