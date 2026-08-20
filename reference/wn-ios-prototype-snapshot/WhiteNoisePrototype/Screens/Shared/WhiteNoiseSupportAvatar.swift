import SwiftUI

struct WhiteNoiseSupportAvatar: View {
    let size: CGFloat

    var body: some View {
        Image(systemName: "questionmark.bubble")
            .font(.system(size: size * 0.42, weight: .medium))
            .foregroundStyle(.primary)
            .frame(width: size, height: size)
            .background(
                Color(uiColor: .secondarySystemFill),
                in: Circle()
            )
            .accessibilityHidden(true)
    }
}
