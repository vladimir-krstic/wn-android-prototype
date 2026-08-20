import SwiftUI

struct AddProfileFlow: View {
    @Environment(\.dismiss) private var dismiss
    @State private var isShowingLogin = false
    @State private var isShowingSignUp = false
    @State private var selectedDetent = PresentationDetent.large

    let onCompletion: (PrototypeProfile, Bool) -> Void

    var body: some View {
        NavigationStack {
            WelcomeView(
                onLogin: {
                    selectedDetent = .medium
                    isShowingLogin = true
                },
                onSignUp: {
                    selectedDetent = .large
                    isShowingSignUp = true
                }
            )
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .navigationDestination(isPresented: $isShowingLogin) {
                LoginView(
                    onScannerPresentationChange: { isPresented in
                        selectedDetent = isPresented ? .large : .medium
                    },
                    onInputFocusChange: { isFocused in
                        selectedDetent = isFocused ? .large : .medium
                    },
                    onSignIn: {
                        onCompletion(.openCircuit, false)
                    }
                )
            }
            .navigationDestination(isPresented: $isShowingSignUp) {
                SignUpView(initialName: "Pebble") { name, about, avatar in
                    onCompletion(
                        .addedSignUp(
                            name: name,
                            about: about,
                            avatar: avatar
                        ),
                        true
                    )
                }
            }
        }
        .tint(Color("AccentColor"))
        .presentationDetents(
            supportedDetents,
            selection: $selectedDetent
        )
        .presentationDragIndicator(.visible)
        .presentationContentInteraction(.resizes)
        .onChange(of: isShowingLogin) {
            if !isShowingLogin {
                selectedDetent = .large
            }
        }
    }

    private var supportedDetents: Set<PresentationDetent> {
        isShowingLogin ? [.medium, .large] : [.large]
    }
}
