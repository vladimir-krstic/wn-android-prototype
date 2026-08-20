import SwiftUI
import UIKit

enum LoginPrototypeData {
    static let privateKey = "nsec1"
        + String(repeating: "q", count: 58)
}

struct LoginView: View {
    @State private var privateKey = ""
    @State private var isSigningIn = false
    @State private var isShowingScanner = false
    @State private var simulatedCameraPermission =
        SimulatedCameraPermissionState.requesting
    @State private var nextSimulatedScanOutcome =
        SimulatedQRScanOutcome.wrongContent
    @FocusState private var isKeyFocused: Bool

    let onScannerPresentationChange: (Bool) -> Void
    let onInputFocusChange: (Bool) -> Void
    let onSignIn: () -> Void

    init(
        onScannerPresentationChange: @escaping (Bool) -> Void = { _ in },
        onInputFocusChange: @escaping (Bool) -> Void = { _ in },
        onSignIn: @escaping () -> Void
    ) {
        self.onScannerPresentationChange = onScannerPresentationChange
        self.onInputFocusChange = onInputFocusChange
        self.onSignIn = onSignIn
    }

    private enum KeyState {
        case empty
        case invalid
        case valid
    }

    private var normalizedKey: String {
        privateKey.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var keyState: KeyState {
        guard !normalizedKey.isEmpty else {
            return .empty
        }

        return normalizedKey.hasPrefix("nsec") ? .valid : .invalid
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading) {
                Text("Private Key")
                    .font(.headline)
                    .padding(.leading)

                HStack {
                    ProfileKeyInput(
                        text: $privateKey,
                        isEmpty: normalizedKey.isEmpty,
                        isBusy: isSigningIn,
                        isFocused: $isKeyFocused,
                        onFocusRequest: {
                            onInputFocusChange(true)
                        },
                        onSubmit: signIn,
                        onAccessory: pasteOrClear
                    )

                    if !isSigningIn
                        && (isKeyFocused || normalizedKey.isEmpty) {
                        Button {
                            if isKeyFocused {
                                isKeyFocused = false
                            } else {
                                isShowingScanner = true
                            }
                        } label: {
                            Image(
                                systemName: isKeyFocused
                                    ? "xmark"
                                    : "qrcode.viewfinder"
                            )
                            .contentTransition(.symbolEffect(.replace))
                        }
                        .buttonStyle(.glass)
                        .buttonBorderShape(.circle)
                        .controlSize(.large)
                        .transition(.opacity)
                        .accessibilityLabel(
                            isKeyFocused
                                ? "Dismiss Keyboard"
                                : "Scan QR Code"
                        )
                        .disabled(isSigningIn)
                    }
                }
                .animation(.default, value: isKeyFocused)
                .animation(.default, value: normalizedKey.isEmpty)

                if keyState == .invalid {
                    Text(
                        "That private key isn't valid. "
                            + "Check it and try again."
                    )
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .padding(.leading)
                } else {
                    Text("It starts with nsec.")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .padding(.leading)
                }
            }
            .safeAreaPadding(.horizontal)
            .safeAreaPadding(.top)
        }
        .scrollDismissesKeyboard(.interactively)
        .navigationTitle("Sign In")
        .navigationBarTitleDisplayMode(.inline)
        .safeAreaInset(edge: .bottom) {
            Button(action: signIn) {
                Text("Sign In")
                    .hidden()
            }
            .buttonStyle(.glassProminent)
            .controlSize(.extraLarge)
            .buttonSizing(.flexible)
            .disabled(keyState != .valid)
            .overlay {
                OnboardingPrimaryActionLabel(
                    title: "Sign In",
                    isLoading: isSigningIn,
                    isActionEnabled: keyState == .valid
                )
                .allowsHitTesting(false)
            }
            .allowsHitTesting(!isSigningIn)
            .accessibilityLabel(isSigningIn ? "Signing In" : "Sign In")
            .accessibilityValue(isSigningIn ? "In progress" : "")
            .safeAreaPadding(.horizontal)
            .safeAreaPadding(.bottom)
        }
        .navigationDestination(isPresented: $isShowingScanner) {
            QRScannerView(
                simulatedPermission: $simulatedCameraPermission,
                simulatedOutcome: nextSimulatedScanOutcome,
                onSimulatedAttempt: {
                    nextSimulatedScanOutcome =
                        nextSimulatedScanOutcome.next
                }
            ) { payload in
                privateKey = payload.trimmingCharacters(
                    in: .whitespacesAndNewlines
                )
            }
        }
        .onChange(of: isShowingScanner) {
            onScannerPresentationChange(isShowingScanner)
        }
        .onChange(of: isKeyFocused) {
            if !isKeyFocused {
                onInputFocusChange(false)
            }
        }
        .task(id: isSigningIn) {
            guard isSigningIn else {
                return
            }

            do {
                try await Task.sleep(for: .seconds(2))
            } catch {
                return
            }

            guard !Task.isCancelled else {
                return
            }

            isSigningIn = false
            onSignIn()
        }
        .background(.background)
    }

    private func pasteOrClear() {
        if normalizedKey.isEmpty {
            privateKey = LoginPrototypeData.privateKey
        } else {
            privateKey = ""
        }
    }

    private func signIn() {
        guard keyState == .valid, !isSigningIn else {
            return
        }

        isKeyFocused = false
        isSigningIn = true

    }
}

#Preview("Sign In — Empty") {
    NavigationStack {
        LoginView(onSignIn: {})
    }
    .tint(Color("AccentColor"))
}

#Preview("Sign In — Dark") {
    NavigationStack {
        LoginView(onSignIn: {})
    }
    .tint(Color("AccentColor"))
    .preferredColorScheme(.dark)
}

private struct ProfileKeyInput: View {
    private enum Metrics {
        static let height: CGFloat = 50
        static let accessoryTarget: CGFloat = 44
    }

    @Binding var text: String
    let isEmpty: Bool
    let isBusy: Bool
    @FocusState.Binding var isFocused: Bool
    let onFocusRequest: () -> Void
    let onSubmit: () -> Void
    let onAccessory: () -> Void

    var body: some View {
        HStack(spacing: 0) {
            SecureField("Enter private key", text: $text)
                .textFieldStyle(.plain)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .submitLabel(.go)
                .privacySensitive()
                .focused($isFocused)
                .simultaneousGesture(
                    TapGesture()
                        .onEnded {
                            onFocusRequest()
                        }
                )
                .onSubmit(onSubmit)

            if !isBusy {
                Button(action: onAccessory) {
                    Image(
                        systemName: isEmpty
                            ? "doc.on.clipboard"
                            : "xmark.circle.fill"
                    )
                    .contentTransition(.symbolEffect(.replace))
                }
                .buttonStyle(.plain)
                .frame(
                    width: Metrics.accessoryTarget,
                    height: Metrics.accessoryTarget
                )
                .accessibilityLabel(isEmpty ? "Paste" : "Clear")
                .transition(.opacity)
            }
        }
        .padding(.leading)
        .frame(height: Metrics.height)
        .background(
            Color(uiColor: .secondarySystemFill),
            in: .capsule
        )
        .disabled(isBusy)
        .contentShape(.capsule)
        .onTapGesture {
            guard !isBusy else {
                return
            }

            onFocusRequest()
            isFocused = true
        }
    }
}
