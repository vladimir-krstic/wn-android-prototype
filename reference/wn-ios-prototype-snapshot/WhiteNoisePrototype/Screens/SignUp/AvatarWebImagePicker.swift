import SwiftUI
import UIKit

struct AvatarWebImageChoice: Identifiable, Equatable {
    let id: String
    let assetName: String
    let accessibilityLabel: String
}

enum AvatarWebImageCatalog {
    static let choices = [
        AvatarWebImageChoice(
            id: "badger",
            assetName: "FiatjafMediaBadger",
            accessibilityLabel: "Badger"
        ),
        AvatarWebImageChoice(
            id: "3TLl_97HNJo",
            assetName: "AvatarWebAionyHaust",
            accessibilityLabel: "Portrait, blue lighting"
        ),
        AvatarWebImageChoice(
            id: "open-circuit",
            assetName: "ProfileAvatarOpenCircuit",
            accessibilityLabel: "Open circuit"
        ),
        AvatarWebImageChoice(
            id: "fox",
            assetName: "FiatjafMediaFox",
            accessibilityLabel: "Fox"
        ),
        AvatarWebImageChoice(
            id: "rDEOVtE7vOs",
            assetName: "AvatarWebChristopherCampbell",
            accessibilityLabel: "Portrait, red hair by a lake"
        ),
        AvatarWebImageChoice(
            id: "cipher-wheel",
            assetName: "ProfileAvatarCipherWheel",
            accessibilityLabel: "Cipher wheel"
        ),
        AvatarWebImageChoice(
            id: "marmot",
            assetName: "FiatjafMediaMarmot",
            accessibilityLabel: "Marmot"
        ),
        AvatarWebImageChoice(
            id: "d1UPkiFd04A",
            assetName: "AvatarWebIanDooley",
            accessibilityLabel: "Portrait, black hat"
        ),
        AvatarWebImageChoice(
            id: "pebble",
            assetName: "ProfileAvatarPebble",
            accessibilityLabel: "Pebbles"
        ),
        AvatarWebImageChoice(
            id: "ostrich",
            assetName: "FiatjafMediaOstrich",
            accessibilityLabel: "Ostrich"
        ),
        AvatarWebImageChoice(
            id: "c_GmwfHBDzk",
            assetName: "AvatarWebSergioDePaula",
            accessibilityLabel: "Black-and-white portrait, striped shirt"
        ),
        AvatarWebImageChoice(
            id: "open-quill",
            assetName: "ProfileAvatarOpenQuill",
            accessibilityLabel: "Open quill"
        ),
        AvatarWebImageChoice(
            id: "sloth",
            assetName: "FiatjafMediaSloth",
            accessibilityLabel: "Sloth"
        ),
        AvatarWebImageChoice(
            id: "sibVwORYqs0",
            assetName: "AvatarWebAyoOgunseinde",
            accessibilityLabel: "Portrait, colorful mural"
        ),
        AvatarWebImageChoice(
            id: "free-signal",
            assetName: "ProfileAvatarFreeSignal",
            accessibilityLabel: "Free signal"
        ),
        AvatarWebImageChoice(
            id: "garden-club",
            assetName: "AvatarGardenClub",
            accessibilityLabel: "Garden club"
        ),
        AvatarWebImageChoice(
            id: "j3lf-Jn6deo",
            assetName: "AvatarWebVinceFleming",
            accessibilityLabel: "Portrait, patterned shirt"
        ),
        AvatarWebImageChoice(
            id: "liberty-relay",
            assetName: "ProfileAvatarLibertyRelay",
            accessibilityLabel: "Liberty relay"
        ),
        AvatarWebImageChoice(
            id: "public-voice",
            assetName: "ProfileAvatarPublicVoice",
            accessibilityLabel: "Public voice"
        ),
        AvatarWebImageChoice(
            id: "5aGUyCW_PJw",
            assetName: "AvatarWebPhilipMartin",
            accessibilityLabel: "Portrait, red beanie on a rooftop"
        ),
        AvatarWebImageChoice(
            id: "marmota",
            assetName: "ProfileAvatarMarmota",
            accessibilityLabel: "Marmot portrait"
        ),
    ]

    static func choice(matching input: String) -> AvatarWebImageChoice? {
        let normalizedInput = input.trimmingCharacters(
            in: .whitespacesAndNewlines
        )

        guard let url = URL(string: normalizedInput),
              let scheme = url.scheme?.lowercased(),
              scheme == "http" || scheme == "https",
              url.host != nil
        else {
            return nil
        }

        let pathChoiceID = url
            .deletingPathExtension()
            .lastPathComponent
        if let exactChoice = choice(forID: pathChoiceID) {
            return exactChoice
        }

        if let containedChoice = choices.first(where: { choice in
            normalizedInput.localizedCaseInsensitiveContains(choice.id)
        }) {
            return containedChoice
        }

        return choices[deterministicOffset(for: normalizedInput)]
    }

    static func results(for query: String) -> [AvatarWebImageChoice] {
        let normalizedQuery = query.trimmingCharacters(
            in: .whitespacesAndNewlines
        )

        guard !normalizedQuery.isEmpty else {
            return choices
        }

        let offset = deterministicOffset(for: normalizedQuery)
        return Array(choices[offset...]) + Array(choices[..<offset])
    }

    static func displayURL(for choice: AvatarWebImageChoice) -> String {
        "https://example.com/images/\(choice.id).jpg"
    }

    static func choice(forID id: String) -> AvatarWebImageChoice? {
        choices.first { $0.id == id }
    }

    private static func deterministicOffset(for value: String) -> Int {
        value.unicodeScalars.reduce(0) {
            ($0 + Int($1.value)) % choices.count
        }
    }
}

struct AvatarWebImagePickerView: View {
    private enum Mode: String, CaseIterable, Identifiable {
        case search
        case url

        var id: Self { self }

        var title: String {
            switch self {
            case .search: "Search"
            case .url: "URL"
            }
        }
    }

    @Environment(\.dismiss) private var dismiss
    @FocusState private var isURLFocused: Bool
    @State private var mode = Mode.search
    @State private var query = ""
    @State private var imageURL = ""
    @State private var selectedChoice: AvatarWebImageChoice?
    @State private var isSearchFocused = false

    let onUseImage: (AvatarWebImageChoice) -> Void

    init(
        currentChoice: AvatarWebImageChoice?,
        onUseImage: @escaping (AvatarWebImageChoice) -> Void
    ) {
        self.onUseImage = onUseImage
        _selectedChoice = State(initialValue: currentChoice)
        _imageURL = State(
            initialValue: currentChoice.map(AvatarWebImageCatalog.displayURL)
                ?? ""
        )
    }

    var body: some View {
        NavigationStack {
            modeContent
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button {
                            dismiss()
                        } label: {
                            Image(systemName: "xmark")
                        }
                        .accessibilityLabel("Close")
                    }

                    ToolbarItem(placement: .principal) {
                        Picker("Image Source", selection: $mode) {
                            ForEach(Mode.allCases) { mode in
                                Text(mode.title)
                                    .tag(mode)
                            }
                        }
                        .labelsHidden()
                        .pickerStyle(.palette)
                        .controlSize(.extraLarge)
                        .frame(width: 180)
                    }

                    ToolbarItem(placement: .confirmationAction) {
                        Button("Done") {
                            guard let activeChoice else {
                                return
                            }

                            onUseImage(activeChoice)
                            dismiss()
                        }
                        .disabled(activeChoice == nil)
                    }
                }
                .onChange(of: mode) {
                    isSearchFocused = false
                    if mode == .url,
                       imageURL.isEmpty,
                       let selectedChoice {
                        imageURL = AvatarWebImageCatalog.displayURL(
                            for: selectedChoice
                        )
                    }
                    isURLFocused = mode == .url
                }
        }
        .overlay {
            KeyboardOcclusionBackdrop()
                .ignoresSafeArea(edges: .bottom)
                .allowsHitTesting(false)
                .accessibilityHidden(true)
        }
    }

    @ViewBuilder
    private var modeContent: some View {
        switch mode {
        case .search:
            searchContent
        case .url:
            urlContent
        }
    }

    private var searchContent: some View {
        Form {
            Section {
                privacyDisclosure(
                    title: "Search privacy",
                    detail: "Your search is sent to DuckDuckGo. Image providers can see your IP address when results load."
                )
            }

            Section {
                if normalizedQuery.isEmpty {
                    ContentUnavailableView(
                        "Search Images",
                        systemImage: "photo.on.rectangle.angled",
                        description: Text("Enter a search to find an image.")
                    )
                    .frame(maxWidth: .infinity)
                } else {
                    LazyVGrid(columns: columns, spacing: 1) {
                        ForEach(
                            AvatarWebImageCatalog.results(for: normalizedQuery)
                        ) { choice in
                            choiceButton(choice)
                        }
                    }
                    .frame(maxWidth: .infinity)
                }
            }
            .listRowInsets(EdgeInsets())
            .listRowBackground(Color.clear)
        }
        .formStyle(.grouped)
        .scrollBounceBehavior(.basedOnSize)
        .scrollEdgeEffectStyle(.soft, for: .bottom)
        .scrollDismissesKeyboard(.interactively)
        .safeAreaBar(edge: .bottom) {
            HStack(spacing: isSearchFocused ? 4 : nil) {
                AvatarImageSearchBar(
                    text: $query,
                    isFocused: $isSearchFocused
                )
                .frame(maxWidth: .infinity)

                if isSearchFocused {
                    Button {
                        isSearchFocused = false
                    } label: {
                        Image(systemName: "xmark")
                    }
                    .buttonStyle(.glass)
                    .buttonBorderShape(.circle)
                    .controlSize(.extraLarge)
                    .accessibilityLabel("Dismiss Keyboard")
                }
            }
            .safeAreaPadding(.leading, isSearchFocused ? 0 : nil)
            .safeAreaPadding(.trailing, isSearchFocused ? 8 : nil)
        }
    }

    private var urlContent: some View {
        Form {
            Section {
                privacyDisclosure(
                    title: "Image privacy",
                    detail: "The image provider can see your IP address when the preview loads."
                )
            }

            Section {
                TextField(
                    "https://example.com/image.jpg",
                    text: $imageURL
                )
                .textContentType(.URL)
                .keyboardType(.URL)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .focused($isURLFocused)
            } header: {
                Text("Image URL")
            } footer: {
                Text(urlHelpText)
            }

            if let urlChoice {
                Section("Preview") {
                    imageTile(urlChoice, showsSelection: false)
                        .listRowInsets(EdgeInsets())
                        .accessibilityLabel(urlChoice.accessibilityLabel)
                }
            }
        }
        .formStyle(.grouped)
        .scrollDismissesKeyboard(.interactively)
    }

    private func privacyDisclosure(
        title: String,
        detail: String
    ) -> some View {
        HStack(alignment: .top) {
            Image(systemName: "hand.raised")
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)

            VStack(alignment: .leading) {
                Text(title)
                    .foregroundStyle(.primary)

                Text(detail)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .accessibilityElement(children: .combine)
    }

    private var columns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: 1),
            count: 3
        )
    }

    private var urlChoice: AvatarWebImageChoice? {
        AvatarWebImageCatalog.choice(matching: imageURL)
    }

    private var normalizedQuery: String {
        query.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var activeChoice: AvatarWebImageChoice? {
        switch mode {
        case .search: selectedChoice
        case .url: urlChoice
        }
    }

    private var urlHelpText: String {
        guard imageURL.isEmpty || urlChoice != nil else {
            return "Enter a valid web address."
        }

        return "Enter an image URL to preview it below."
    }

    private func choiceButton(_ choice: AvatarWebImageChoice) -> some View {
        Button {
            selectedChoice = choice
            imageURL = AvatarWebImageCatalog.displayURL(for: choice)
            isSearchFocused = false
        } label: {
            imageTile(
                choice,
                showsSelection: selectedChoice == choice
            )
            .contentShape(.rect)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(choice.accessibilityLabel)
        .accessibilityValue(selectedChoice == choice ? "Selected" : "")
        .accessibilityAddTraits(
            selectedChoice == choice ? .isSelected : []
        )
    }

    private func imageTile(
        _ choice: AvatarWebImageChoice,
        showsSelection: Bool
    ) -> some View {
        Color.clear
            .aspectRatio(1, contentMode: .fit)
            .overlay {
                Image(choice.assetName)
                    .resizable()
                    .scaledToFill()
            }
            .clipped()
            .overlay(alignment: .bottomTrailing) {
                if showsSelection {
                    ZStack {
                        Circle()
                            .fill(Color.accentColor)

                        Circle()
                            .stroke(Color.white, lineWidth: 2)

                        Image(systemName: "checkmark")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(.white)
                    }
                    .frame(width: 26, height: 26)
                    .padding(6)
                }
            }
    }
}

private struct AvatarImageSearchBar: UIViewRepresentable {
    @Binding var text: String
    @Binding var isFocused: Bool

    func makeCoordinator() -> Coordinator {
        Coordinator(parent: self)
    }

    func makeUIView(context: Context) -> UISearchBar {
        let searchBar = UISearchBar()
        searchBar.delegate = context.coordinator
        searchBar.searchBarStyle = .minimal
        searchBar.placeholder = "Search Images"
        searchBar.showsCancelButton = false
        searchBar.searchTextField.clearButtonMode = .never
        searchBar.searchTextField.returnKeyType = .search
        return searchBar
    }

    func updateUIView(_ uiView: UISearchBar, context: Context) {
        context.coordinator.parent = self

        if uiView.text != text {
            uiView.text = text
        }
        uiView.searchTextField.clearButtonMode = text.isEmpty ? .never : .always

        if isFocused, !uiView.searchTextField.isFirstResponder {
            uiView.searchTextField.becomeFirstResponder()
        } else if !isFocused, uiView.searchTextField.isFirstResponder {
            uiView.searchTextField.resignFirstResponder()
        }
    }

    final class Coordinator: NSObject, UISearchBarDelegate {
        var parent: AvatarImageSearchBar

        init(parent: AvatarImageSearchBar) {
            self.parent = parent
        }

        func searchBar(
            _ searchBar: UISearchBar,
            textDidChange searchText: String
        ) {
            parent.text = searchText
        }

        func searchBarTextDidBeginEditing(_ searchBar: UISearchBar) {
            parent.isFocused = true
        }

        func searchBarTextDidEndEditing(_ searchBar: UISearchBar) {
            parent.isFocused = false
        }

        func searchBarSearchButtonClicked(_ searchBar: UISearchBar) {
            parent.isFocused = false
            searchBar.searchTextField.resignFirstResponder()
        }
    }
}

private struct KeyboardOcclusionBackdrop: UIViewRepresentable {
    func makeUIView(context: Context) -> KeyboardOcclusionBackdropView {
        KeyboardOcclusionBackdropView()
    }

    func updateUIView(
        _ uiView: KeyboardOcclusionBackdropView,
        context: Context
    ) {}
}

private final class KeyboardOcclusionBackdropView: UIView {
    private let backdropView = UIView()

    override init(frame: CGRect) {
        super.init(frame: frame)
        isUserInteractionEnabled = false
        backgroundColor = .clear

        backdropView.translatesAutoresizingMaskIntoConstraints = false
        backdropView.backgroundColor = .systemGroupedBackground
        backdropView.isUserInteractionEnabled = false
        addSubview(backdropView)

        NSLayoutConstraint.activate([
            backdropView.topAnchor.constraint(
                equalTo: keyboardLayoutGuide.topAnchor
            ),
            backdropView.leadingAnchor.constraint(equalTo: leadingAnchor),
            backdropView.trailingAnchor.constraint(equalTo: trailingAnchor),
            backdropView.bottomAnchor.constraint(equalTo: bottomAnchor),
        ])
    }

    override func layoutSubviews() {
        super.layoutSubviews()

        let keyboardIsVisible = keyboardLayoutGuide.layoutFrame.minY
            < safeAreaLayoutGuide.layoutFrame.maxY
        backdropView.isHidden = !keyboardIsVisible
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

#Preview("Find Image on Web") {
    AvatarWebImagePickerView(currentChoice: nil) { _ in }
}
