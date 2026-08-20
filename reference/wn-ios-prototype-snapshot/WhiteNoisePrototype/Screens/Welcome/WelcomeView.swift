import SwiftUI

struct WelcomeView: View {
    @Environment(\.colorScheme) private var colorScheme

    let onLogin: () -> Void
    let onSignUp: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            Spacer()

            Image("WhiteNoiseMark")
                .resizable()
                .scaledToFit()
                .containerRelativeFrame(
                    .horizontal,
                    count: 2,
                    span: 1,
                    spacing: 0
                )
                .accessibilityLabel("White Noise")

            Spacer()

            VStack {
                Button(action: onLogin) {
                    Text("Sign In")
                }
                    .buttonStyle(.glass)
                    .accessibilityIdentifier("welcome.sign-in")

                Button(action: onSignUp) {
                    Text("Sign Up")
                        .foregroundStyle(
                            colorScheme == .dark ? Color.black : Color.white
                        )
                }
                    .buttonStyle(.glassProminent)
                    .accessibilityIdentifier("welcome.sign-up")
            }
            .controlSize(.extraLarge)
            .buttonSizing(.flexible)
        }
        .safeAreaPadding(.horizontal)
        .safeAreaPadding(.bottom)
        .background(.background)
    }
}

#Preview("Welcome — Light") {
    WelcomeView(onLogin: {}, onSignUp: {})
        .tint(Color("AccentColor"))
}

#Preview("Welcome — Dark") {
    WelcomeView(onLogin: {}, onSignUp: {})
        .tint(Color("AccentColor"))
        .preferredColorScheme(.dark)
}
