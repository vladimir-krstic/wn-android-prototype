import Foundation
import SwiftUI
import UniformTypeIdentifiers
import UIKit

struct ProfileKeysSettingsView: View {
    private enum KeyKind: Equatable {
        case publicKey
        case privateKey
    }

    private enum ExportKind {
        case privateKey
        case encryptedPrivateKey

        var defaultFilename: String {
            switch self {
            case .privateKey:
                "White Noise Private Key"
            case .encryptedPrivateKey:
                "White Noise Encrypted Private Key"
            }
        }
    }

    private struct PendingKeyExport {
        let document: KeyExportDocument
        let kind: ExportKind
    }

    @State private var copiedKey: KeyKind?
    @State private var copyFeedbackTrigger = 0
    @State private var copyResetTask: Task<Void, Never>?
    @State private var isPrivateKeyVisible = false
    @State private var isShowingRawExportConfirmation = false
    @State private var isShowingEncryptedPrivateKey = false
    @State private var pendingEncryptedExport: PendingKeyExport?
    @State private var exportDocument: KeyExportDocument?
    @State private var exportFilename = ""
    @State private var isFileExporterPresented = false
    @State private var isShowingExportError = false

    let profile: PrototypeProfile

    var body: some View {
        Form {
            publicKeySection
            privateKeySection
            exportSection
        }
        .navigationTitle("Profile Keys")
        .navigationBarTitleDisplayMode(.inline)
        .sensoryFeedback(.success, trigger: copyFeedbackTrigger)
        .sheet(
            isPresented: $isShowingEncryptedPrivateKey,
            onDismiss: presentPendingEncryptedExport
        ) {
            EncryptedPrivateKeySheet { password in
                pendingEncryptedExport = PendingKeyExport(
                    document: KeyExportDocument(
                        text: encryptedPrivateKey(for: password)
                    ),
                    kind: .encryptedPrivateKey
                )
                isShowingEncryptedPrivateKey = false
            }
        }
        .alert(
            "Keep Your Private Key Safe",
            isPresented: $isShowingRawExportConfirmation
        ) {
            Button("Cancel", role: .cancel) {}
            Button("Export Private Key", role: .destructive) {
                queueRawPrivateKeyExport()
            }
        } message: {
            Text(
                "Store this file somewhere secure. The encrypted export or a trusted password manager is safer."
            )
        }
        .fileExporter(
            isPresented: $isFileExporterPresented,
            document: exportDocument,
            contentType: .plainText,
            defaultFilename: exportFilename,
            onCompletion: handleExportResult
        )
        .alert("Couldn’t Save File", isPresented: $isShowingExportError) {
            Button("Dismiss", role: .cancel) {}
        } message: {
            Text("Choose another location and try again.")
        }
        .onDisappear {
            resetCopyFeedback()
            isPrivateKeyVisible = false
        }
    }

    private var publicKeySection: some View {
        Section {
            HStack(spacing: 12) {
                Text(profile.publicKey)
                    .font(.body.monospaced())
                    .lineLimit(1)
                    .truncationMode(.middle)
                    .accessibilityLabel("Public key")
                    .accessibilityValue(profile.shortPublicKey)

                Spacer(minLength: 0)

                copyButton(for: .publicKey)
            }
        } header: {
            Text("Public Key")
        } footer: {
            Text("Share this key so people can find and connect with you.")
        }
    }

    private var privateKeySection: some View {
        Section {
            HStack(spacing: 12) {
                Group {
                    if isPrivateKeyVisible {
                        Text(rawPrivateKey)
                            .lineLimit(1)
                            .truncationMode(.middle)
                    } else {
                        FittingPrivateKeyBullets()
                    }
                }
                .font(.body.monospaced())
                .frame(maxWidth: .infinity, alignment: .leading)
                .clipped()
                .privacySensitive()
                .accessibilityHidden(true)

                Button {
                    isPrivateKeyVisible.toggle()
                } label: {
                    Image(
                        systemName: isPrivateKeyVisible
                            ? "eye.slash"
                            : "eye"
                    )
                    .contentTransition(.symbolEffect(.replace))
                }
                .buttonStyle(.plain)
                .accessibilityLabel(
                    isPrivateKeyVisible
                        ? "Hide private key"
                        : "Show private key"
                )
            }

            Button {
                copyKey(.privateKey)
            } label: {
                Label(
                    "Copy Private Key",
                    systemImage: copiedKey == .privateKey
                        ? "checkmark"
                        : "doc.on.doc"
                )
                .contentTransition(.symbolEffect(.replace))
            }
            .accessibilityLabel(
                copiedKey == .privateKey
                    ? "Private key copied"
                    : "Copy private key"
            )
        } header: {
            Text("Private Key")
        } footer: {
            Text(
                "Keep this key private. Anyone with it can use your profile, and White Noise can’t recover it."
            )
        }
    }

    private var exportSection: some View {
        Section("Export") {
            Button {
                isShowingEncryptedPrivateKey = true
            } label: {
                Label(
                    "Export Encrypted Private Key",
                    systemImage: "lock.doc"
                )
            }

            Button {
                isShowingRawExportConfirmation = true
            } label: {
                Label(
                    "Export Private Key",
                    systemImage: "arrow.down.document"
                )
            }
        }
    }

    private var rawPrivateKey: String {
        "nsec1p8c4y6m2v9r5t7s3h1d8n4x6j2a9e5u7z3q8w4f6k1m9c5n7"
    }

    private func copyButton(for key: KeyKind) -> some View {
        Button {
            copyKey(key)
        } label: {
            Image(
                systemName: copiedKey == key
                    ? "checkmark"
                    : "doc.on.doc"
            )
            .contentTransition(.symbolEffect(.replace))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(
            copiedKey == key ? "Public key copied" : "Copy public key"
        )
    }

    private func copyKey(_ key: KeyKind) {
        switch key {
        case .publicKey:
            UIPasteboard.general.string = profile.publicKey
        case .privateKey:
            UIPasteboard.general.string = rawPrivateKey
        }

        copiedKey = key
        copyFeedbackTrigger += 1
        UIAccessibility.post(
            notification: .announcement,
            argument: key == .publicKey
                ? "Public key copied"
                : "Private key copied"
        )

        copyResetTask?.cancel()
        copyResetTask = Task { @MainActor in
            try? await Task.sleep(for: .seconds(2))
            guard !Task.isCancelled else {
                return
            }

            copiedKey = nil
            copyResetTask = nil
        }
    }

    private func resetCopyFeedback() {
        copyResetTask?.cancel()
        copyResetTask = nil
        copiedKey = nil
    }

    private func queueRawPrivateKeyExport() {
        Task { @MainActor in
            await Task.yield()
            prepareExport(
                PendingKeyExport(
                    document: KeyExportDocument(text: rawPrivateKey),
                    kind: .privateKey
                )
            )
        }
    }

    private func presentPendingEncryptedExport() {
        guard let pendingEncryptedExport else {
            return
        }

        self.pendingEncryptedExport = nil
        prepareExport(pendingEncryptedExport)
    }

    private func prepareExport(_ export: PendingKeyExport) {
        exportDocument = export.document
        exportFilename = export.kind.defaultFilename
        isFileExporterPresented = true
    }

    private func handleExportResult(_ result: Result<URL, Error>) {
        exportDocument = nil
        exportFilename = ""
        if case .failure = result {
            isShowingExportError = true
        }
    }

    private func encryptedPrivateKey(for password: String) -> String {
        let passwordLength = String(format: "%02d", password.count)
        return "ncryptsec1\(passwordLength)q8w4f6k1m9c5n7p3v2x6z8t4r1y9d5h7s3j6a2e8u4"
    }
}

private struct FittingPrivateKeyBullets: View {
    var body: some View {
        Text("•")
            .font(.body.monospaced())
            .hidden()
            .frame(maxWidth: .infinity, alignment: .leading)
            .overlay {
                Canvas { context, size in
                    let bullet = context.resolve(
                        Text("•").font(.body.monospaced())
                    )
                    let bulletSize = bullet.measure(in: size)
                    guard bulletSize.width > 0 else {
                        return
                    }

                    let bulletCount = Int(size.width / bulletSize.width)
                    for index in 0..<bulletCount {
                        context.draw(
                            bullet,
                            at: CGPoint(
                                x: CGFloat(index) * bulletSize.width,
                                y: size.height / 2
                            ),
                            anchor: .leading
                        )
                    }
                }
            }
    }
}

private struct EncryptedPrivateKeySheet: View {
    @Environment(\.dismiss) private var dismiss

    @State private var password = ""
    @State private var confirmation = ""

    let onExport: (String) -> Void

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    SecureField("Password", text: $password)
                        .textContentType(.newPassword)
                    SecureField(
                        "Confirm Password",
                        text: $confirmation
                    )
                    .textContentType(.newPassword)
                } footer: {
                    if passwordsDoNotMatch {
                        Label(
                            "Passwords don’t match.",
                            systemImage: "exclamationmark.circle.fill"
                        )
                        .foregroundStyle(.red)
                    } else {
                        Text(
                            "Use a long, unique password. You’ll need it to open the encrypted file."
                        )
                    }
                }

                if !password.isEmpty {
                    Section("Strength") {
                        LabeledContent(passwordStrength.label) {
                            ProgressView(
                                value: Double(passwordStrength.rawValue),
                                total: 3
                            )
                            .tint(passwordStrength.color)
                            .frame(maxWidth: 120)
                        }
                    }
                }
            }
            .navigationTitle("Encrypted Private Key")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") {
                        dismiss()
                    }
                }

                ToolbarItem(placement: .confirmationAction) {
                    Button("Export") {
                        onExport(password)
                    }
                    .buttonStyle(.glassProminent)
                    .disabled(!isValid)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private var isValid: Bool {
        !password.isEmpty && password == confirmation
    }

    private var passwordsDoNotMatch: Bool {
        !confirmation.isEmpty && password != confirmation
    }

    private var passwordStrength: PasswordStrength {
        guard password.count >= 12 else {
            return .low
        }

        let hasLetters = password.rangeOfCharacter(
            from: .letters
        ) != nil
        let hasDigits = password.rangeOfCharacter(
            from: .decimalDigits
        ) != nil
        let hasSymbols = password.rangeOfCharacter(
            from: .alphanumerics.inverted
        ) != nil

        if password.count >= 16 && hasLetters && hasDigits && hasSymbols {
            return .strong
        }

        return .fair
    }
}

private enum PasswordStrength: Int {
    case low = 1
    case fair = 2
    case strong = 3

    var label: String {
        switch self {
        case .low:
            "Low"
        case .fair:
            "Fair"
        case .strong:
            "Strong"
        }
    }

    var color: Color {
        switch self {
        case .low:
            .red
        case .fair:
            .yellow
        case .strong:
            .green
        }
    }
}

private struct KeyExportDocument: FileDocument {
    static let readableContentTypes: [UTType] = [.plainText]

    let data: Data

    init(text: String) {
        data = Data(text.utf8)
    }

    init(configuration: ReadConfiguration) throws {
        data = configuration.file.regularFileContents ?? Data()
    }

    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper {
        FileWrapper(regularFileWithContents: data)
    }
}

#Preview("Profile Keys") {
    NavigationStack {
        ProfileKeysSettingsView(profile: .marmota)
    }
    .tint(Color("AccentColor"))
}
