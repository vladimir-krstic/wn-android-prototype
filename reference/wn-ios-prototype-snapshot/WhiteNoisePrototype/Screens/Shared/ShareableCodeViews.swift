import SwiftUI
import UIKit

struct ShareableQRCodeView: View {
    let image: UIImage
    let accessibilityLabel: String

    var body: some View {
        Image(uiImage: image)
            .resizable()
            .interpolation(.none)
            .scaledToFit()
            .padding(12)
            .containerRelativeFrame(.horizontal) { length, _ in
                length * 0.81
            }
            .aspectRatio(1, contentMode: .fit)
            .background(
                .white,
                in: RoundedRectangle(
                    cornerRadius: 16,
                    style: .continuous
                )
            )
            .accessibilityLabel(accessibilityLabel)
    }
}

struct CompactCopyValueLabel: View {
    let value: String
    let isCopied: Bool
    var fillsAvailableWidth = false

    var body: some View {
        HStack {
            Text(value)
                .lineLimit(1)
                .truncationMode(.middle)
                .font(.subheadline.monospaced())
                .foregroundStyle(.secondary)

            Image(systemName: isCopied ? "checkmark" : "doc.on.doc")
                .font(.caption2.weight(.medium))
                .foregroundStyle(.secondary)
                .frame(width: 14, height: 14)
                .contentTransition(.symbolEffect(.replace))
                .animation(.default, value: isCopied)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 6)
        .frame(
            maxWidth: fillsAvailableWidth ? .infinity : nil
        )
        .background(
            Color(uiColor: .secondarySystemFill),
            in: .capsule
        )
        .contentShape(Rectangle())
    }
}

struct ProfileIdentityHeader<Avatar: View>: View {
    let name: String
    let publicKey: String
    let nostrAddress: String?
    let isNostrAddressVerified: Bool
    let bottomPadding: CGFloat
    let showsIdentityValues: Bool

    private let avatar: (CGFloat) -> Avatar

    init(
        name: String,
        publicKey: String,
        nostrAddress: String? = nil,
        isNostrAddressVerified: Bool = false,
        bottomPadding: CGFloat = 14,
        showsIdentityValues: Bool = true,
        @ViewBuilder avatar: @escaping (CGFloat) -> Avatar
    ) {
        self.name = name
        self.publicKey = publicKey
        self.nostrAddress = nostrAddress
        self.isNostrAddressVerified = isNostrAddressVerified
        self.bottomPadding = bottomPadding
        self.showsIdentityValues = showsIdentityValues
        self.avatar = avatar
    }

    var body: some View {
        VStack(spacing: 8) {
            GeometryReader { geometry in
                avatar(geometry.size.width)
            }
            .aspectRatio(1, contentMode: .fit)
            .containerRelativeFrame(
                .horizontal,
                count: 3,
                span: 1,
                spacing: 0
            )

            VStack(spacing: 3) {
                Text(name)
                    .font(.title2.weight(.semibold))

                if showsIdentityValues,
                   let nostrAddress,
                   !nostrAddress.isEmpty {
                    InlineVerifiedNostrAddressValue(
                        address: nostrAddress,
                        isVerified: isNostrAddressVerified
                    )
                }
            }

            if showsIdentityValues {
                ProfilePublicKeyCopyButton(publicKey: publicKey)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.top)
        .padding(.bottom, bottomPadding)
    }
}

struct ProfileIdentityValues: View {
    let publicKey: String
    let nostrAddress: String?
    let isNostrAddressVerified: Bool

    var body: some View {
        VStack(spacing: 8) {
            if let nostrAddress, !nostrAddress.isEmpty {
                InlineVerifiedNostrAddressValue(
                    address: nostrAddress,
                    isVerified: isNostrAddressVerified
                )
            }

            ProfilePublicKeyCopyButton(publicKey: publicKey)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct ProfilePublicKeyCopyButton: View {
    let publicKey: String

    @State private var copied = false
    @State private var copyFeedbackTrigger = 0
    @State private var copyResetTask: Task<Void, Never>?

    var body: some View {
        Button(action: copyPublicKey) {
            CompactCopyValueLabel(
                value: compactPublicKey,
                isCopied: copied
            )
        }
        .buttonStyle(.plain)
        .accessibilityLabel(
            copied ? "Public key copied" : "Copy public key"
        )
        .accessibilityValue(compactPublicKey)
        .sensoryFeedback(.success, trigger: copyFeedbackTrigger)
        .onDisappear {
            resetCopyFeedback()
        }
    }

    private var compactPublicKey: String {
        guard publicKey.count > 19 else {
            return publicKey
        }

        return "\(publicKey.prefix(14))…\(publicKey.suffix(4))"
    }

    private func copyPublicKey() {
        UIPasteboard.general.string = publicKey
        copied = true
        copyFeedbackTrigger += 1
        UIAccessibility.post(
            notification: .announcement,
            argument: "Public key copied"
        )

        copyResetTask?.cancel()
        copyResetTask = Task { @MainActor in
            try? await Task.sleep(for: .seconds(2))

            guard !Task.isCancelled else {
                return
            }

            copied = false
            copyResetTask = nil
        }
    }

    private func resetCopyFeedback() {
        copyResetTask?.cancel()
        copyResetTask = nil
        copied = false
    }
}
