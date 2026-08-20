import AVKit
import SwiftUI

enum PrototypeAuthorNameColor {
    static let paletteCount = 9

    static func paletteIndex(for publicKey: String) -> Int {
        var hash: UInt64 = 14_695_981_039_346_656_037
        for byte in publicKey.utf8 {
            hash ^= UInt64(byte)
            hash &*= 1_099_511_628_211
        }
        return Int(hash % UInt64(paletteCount))
    }

    @MainActor
    static func color(for publicKey: String) -> Color {
        let index = paletteIndex(for: publicKey)
        return Color(
            uiColor: UIColor { traits in
                let base = baseColor(at: index).resolvedColor(with: traits)
                let label = UIColor.label.resolvedColor(with: traits)
                let background = UIColor.systemBackground.resolvedColor(with: traits)
                return accessibleColor(base: base, toward: label, over: background)
            }
        )
    }

    @MainActor
    static func avatarBackground(for publicKey: String) -> Color {
        let index = paletteIndex(for: publicKey)
        return Color(
            uiColor: UIColor { traits in
                baseColor(at: index).resolvedColor(with: traits)
            }
        )
    }

    private static func baseColor(at index: Int) -> UIColor {
        switch index {
        case 0: .systemRed
        case 1: .systemOrange
        case 2: .systemGreen
        case 3: .systemTeal
        case 4: .systemBlue
        case 5: .systemIndigo
        case 6: .systemPurple
        case 7: .systemPink
        default: .systemBrown
        }
    }

    private static func accessibleColor(
        base: UIColor,
        toward label: UIColor,
        over background: UIColor
    ) -> UIColor {
        let minimumContrast: CGFloat = 4.5
        guard contrastRatio(base, background) < minimumContrast else { return base }

        var lowerBound: CGFloat = 0
        var upperBound: CGFloat = 1
        for _ in 0..<12 {
            let amount = (lowerBound + upperBound) / 2
            let candidate = mix(base, label, amount: amount)
            if contrastRatio(candidate, background) >= minimumContrast {
                upperBound = amount
            } else {
                lowerBound = amount
            }
        }
        return mix(base, label, amount: upperBound)
    }

    private static func mix(_ first: UIColor, _ second: UIColor, amount: CGFloat) -> UIColor {
        let firstComponents = components(first)
        let secondComponents = components(second)
        return UIColor(
            red: firstComponents.red + (secondComponents.red - firstComponents.red) * amount,
            green: firstComponents.green + (secondComponents.green - firstComponents.green) * amount,
            blue: firstComponents.blue + (secondComponents.blue - firstComponents.blue) * amount,
            alpha: firstComponents.alpha + (secondComponents.alpha - firstComponents.alpha) * amount
        )
    }

    private static func contrastRatio(_ first: UIColor, _ second: UIColor) -> CGFloat {
        let firstLuminance = relativeLuminance(first)
        let secondLuminance = relativeLuminance(second)
        return (max(firstLuminance, secondLuminance) + 0.05)
            / (min(firstLuminance, secondLuminance) + 0.05)
    }

    private static func relativeLuminance(_ color: UIColor) -> CGFloat {
        let value = components(color)
        return 0.2126 * linearized(value.red)
            + 0.7152 * linearized(value.green)
            + 0.0722 * linearized(value.blue)
    }

    private static func linearized(_ value: CGFloat) -> CGFloat {
        value <= 0.04045
            ? value / 12.92
            : pow((value + 0.055) / 1.055, 2.4)
    }

    private static func components(
        _ color: UIColor
    ) -> (red: CGFloat, green: CGFloat, blue: CGFloat, alpha: CGFloat) {
        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0
        guard color.getRed(&red, green: &green, blue: &blue, alpha: &alpha) else {
            return (0, 0, 0, 1)
        }
        return (red, green, blue, alpha)
    }
}

@MainActor
enum PrototypePreparedImageCache {
    private static let cache = NSCache<NSData, UIImage>()

    static func image(from data: Data) -> UIImage? {
        let key = data as NSData
        if let cached = cache.object(forKey: key) { return cached }
        guard let image = UIImage(data: data) else { return nil }
        cache.setObject(image, forKey: key)
        return image
    }
}

struct PrototypeChatAvatarView: View {
    let avatar: ChatListItem.Avatar
    var size: CGFloat = 40
    var publicKey: String? = nil

    var body: some View {
        ZStack {
            Circle().fill(backgroundColor)
            switch avatar {
            case let .asset(name):
                Image(name).resizable().scaledToFill()
            case let .imageData(data):
                if let image = PrototypePreparedImageCache.image(from: data) {
                    Image(uiImage: image).resizable().scaledToFill()
                }
            case let .monogram(value):
                Text(monogramLetter(value))
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white)
            case let .systemSymbol(name):
                Image(systemName: name).font(.title3)
            }
        }
        .frame(width: size, height: size)
        .clipShape(.circle)
        .accessibilityHidden(true)
    }

    private var backgroundColor: Color {
        guard case .monogram = avatar, let publicKey else {
            return Color(uiColor: .systemGray5)
        }
        return PrototypeAuthorNameColor.avatarBackground(for: publicKey)
    }

    private func monogramLetter(_ value: String) -> String {
        String(
            value.trimmingCharacters(in: .whitespacesAndNewlines).prefix(1)
        ).uppercased()
    }
}

struct PrototypeImageSourceView: View {
    let source: PrototypeImageSource

    var body: some View {
        switch source {
        case let .asset(name):
            Image(name).resizable()
        case let .data(data):
            if let image = PrototypePreparedImageCache.image(from: data) {
                Image(uiImage: image).resizable()
            } else {
                unavailable
            }
        }
    }

    private var unavailable: some View {
        ZStack {
            Color(uiColor: .secondarySystemFill)
            Image(systemName: "photo.badge.exclamationmark")
                .font(.title)
                .foregroundStyle(.secondary)
        }
    }
}

enum PrototypeChatEventFormatter {
    static func text(
        for kind: PrototypeChatEventKind,
        profileID: String,
        people: [PrototypePerson]
    ) -> String {
        func name(_ id: String, capitalized: Bool = false) -> String {
            if id == profileID { return capitalized ? "You" : "you" }
            return people.first { $0.id == id }?.name ?? id
        }
        func actor(_ id: String) -> String { name(id, capitalized: true) }
        func names(_ ids: [String]) -> String {
            let values = ids.map { name($0) }
            guard let last = values.last else { return "" }
            guard values.count > 1 else { return last }
            return values.dropLast().joined(separator: ", ") + " and " + last
        }

        switch kind {
        case let .directChatStarted(actorID):
            return "\(actor(actorID)) started the chat."
        case .directChatLeft:
            return "You left the chat."
        case let .groupCreated(actorID):
            return "\(actor(actorID)) created the group."
        case let .membersAdded(actorID, personIDs):
            return "\(actor(actorID)) added \(names(personIDs))."
        case let .memberJoined(personID):
            return "\(actor(personID)) joined the group."
        case let .memberLeft(personID):
            return "\(actor(personID)) left the group."
        case let .memberRemoved(actorID, personID):
            if personID == profileID {
                return "\(actor(actorID)) removed you from the group."
            }
            return "\(actor(actorID)) removed \(name(personID))."
        case let .adminGranted(actorID, personID):
            return "\(actor(actorID)) made \(name(personID)) an admin."
        case let .adminRevoked(actorID, personID):
            return "\(actor(actorID)) removed \(name(personID)) as an admin."
        case let .groupNameChanged(actorID, value):
            return "\(actor(actorID)) changed the group name to \(value)."
        case let .groupPhotoChanged(actorID):
            return "\(actor(actorID)) changed the group photo."
        case let .groupPhotoRemoved(actorID):
            return "\(actor(actorID)) removed the group photo."
        case let .groupDescriptionChanged(actorID):
            return "\(actor(actorID)) changed the group description."
        case let .groupDescriptionRemoved(actorID):
            return "\(actor(actorID)) removed the group description."
        case let .disappearingMessagesChanged(actorID, duration):
            if duration == .off {
                return "\(actor(actorID)) turned off disappearing messages."
            }
            return "\(actor(actorID)) set disappearing messages to \(duration.title)."
        }
    }
}

enum PrototypeDateFormatter {
    private static let recentDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.setLocalizedDateFormatFromTemplate("EE, MMM d")
        return formatter
    }()

    private static let oldDateFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter
    }()

    static func separator(for date: Date, now: Date = .now) -> String {
        let calendar = Calendar.autoupdatingCurrent
        let displayDate = min(date, now)
        let monthDistance = calendar.dateComponents([.month], from: displayDate, to: now).month ?? 0
        if monthDistance >= 6 {
            return oldDateFormatter.string(from: displayDate)
        }

        let startOfToday = calendar.startOfDay(for: now)
        let dayDistance = calendar.dateComponents(
            [.day],
            from: calendar.startOfDay(for: displayDate),
            to: startOfToday
        ).day ?? 0
        if dayDistance == 0 {
            return "Today"
        }
        if dayDistance == 1 {
            return "Yesterday"
        }
        if dayDistance > 1 {
            return recentDateFormatter.string(from: displayDate)
        }
        return "Today"
    }

    static func time(for date: Date) -> String {
        date.formatted(date: .omitted, time: .shortened)
    }
}

struct PrototypeMediaSelection: Identifiable {
    let items: [PrototypeMediaItem]
    let initialItemID: String

    init?(chat: PrototypeChat, selectedItemID: String) {
        let availableItems = PrototypeMediaIndex.availableItems(in: chat)
        guard availableItems.contains(where: { $0.id == selectedItemID }) else {
            return nil
        }
        items = availableItems
        initialItemID = selectedItemID
    }

    init?(
        chat: PrototypeChat,
        messageID: String,
        attachmentID: String
    ) {
        let availableItems = PrototypeMediaIndex.availableItems(in: chat)
        guard let selectedItem = availableItems.first(where: {
            $0.messageID == messageID && $0.attachmentID == attachmentID
        }) else {
            return nil
        }
        items = availableItems
        initialItemID = selectedItem.id
    }

    var id: String { initialItemID }

    var initialIndex: Int {
        items.firstIndex { $0.id == initialItemID } ?? 0
    }
}

struct PrototypeSingleMediaView: View {
    let attachment: PrototypeAttachment
    var isSelected = true
    var onZoomStateChange: (Bool) -> Void = { _ in }

    @ViewBuilder
    var body: some View {
        switch attachment {
        case let .photo(_, source, label, _):
            if prototypeImage(source) != nil {
                ZoomablePrototypeImage(
                    source: source,
                    isSelected: isSelected,
                    onZoomStateChange: onZoomStateChange
                )
                    .accessibilityLabel(label)
            } else {
                ContentUnavailableView(
                    "Photo Unavailable",
                    systemImage: "photo.badge.exclamationmark",
                    description: Text("This photo is no longer available.")
                )
                .foregroundStyle(.white)
                .accessibilityLabel("Photo unavailable, \(label)")
            }
        case let .video(_, url, thumbnail, duration, _):
            if let url {
                PrototypeVideoPage(
                    url: url,
                    duration: duration,
                    isSelected: isSelected
                )
            } else {
                ZStack(alignment: .bottom) {
                    PrototypeImageSourceView(source: thumbnail)
                        .scaledToFit()
                    ContentUnavailableView(
                        "Video Unavailable",
                        systemImage: "video.slash",
                        description: Text("This video is no longer available.")
                    )
                    .foregroundStyle(.white)
                    .padding(.bottom, 24)
                }
                .accessibilityLabel("Video unavailable")
            }
        case let .gif(_, assetName, label):
            Image(assetName)
                .resizable()
                .scaledToFit()
                .accessibilityLabel("GIF, \(label)")
        default:
            ContentUnavailableView("Preview Unavailable", systemImage: "doc")
        }
    }

    private func prototypeImage(_ source: PrototypeImageSource) -> UIImage? {
        switch source {
        case let .asset(name):
            UIImage(named: name)
        case let .data(data):
            PrototypePreparedImageCache.image(from: data)
        }
    }
}

private struct PrototypeVideoPage: View {
    let url: URL
    let duration: TimeInterval
    let isSelected: Bool
    @State private var player: AVPlayer

    init(url: URL, duration: TimeInterval, isSelected: Bool) {
        self.url = url
        self.duration = duration
        self.isSelected = isSelected
        _player = State(initialValue: AVPlayer(url: url))
    }

    var body: some View {
        VideoPlayer(player: player)
            .accessibilityLabel(
                "Video, \(prototypeDurationString(duration))"
            )
        .onAppear {
            updatePlayback()
        }
        .onChange(of: isSelected) { _, _ in
            updatePlayback()
        }
        .onDisappear {
            player.pause()
            player.seek(to: .zero)
        }
        .onReceive(
            NotificationCenter.default.publisher(
                for: AVPlayerItem.didPlayToEndTimeNotification,
                object: player.currentItem
            )
        ) { _ in
            player.seek(to: .zero)
        }
    }

    private func updatePlayback() {
        if isSelected {
            PrototypePlaybackCoordinator.shared.stopAll()
            player.play()
        } else {
            player.pause()
        }
    }
}

private struct ZoomablePrototypeImage: View {
    let source: PrototypeImageSource
    let isSelected: Bool
    let onZoomStateChange: (Bool) -> Void

    var body: some View {
        PrototypeZoomScrollView(
            source: source,
            isSelected: isSelected,
            onZoomStateChange: onZoomStateChange
        )
            .accessibilityHint("Pinch or double-tap to zoom.")
    }
}

private struct PrototypeZoomScrollView: UIViewRepresentable {
    let source: PrototypeImageSource
    let isSelected: Bool
    let onZoomStateChange: (Bool) -> Void

    final class Coordinator: NSObject, UIScrollViewDelegate {
        let imageView = UIImageView()
        var currentSource: PrototypeImageSource?
        var onZoomStateChange: (Bool) -> Void = { _ in }

        func viewForZooming(in scrollView: UIScrollView) -> UIView? {
            imageView
        }

        func scrollViewDidZoom(_ scrollView: UIScrollView) {
            onZoomStateChange(scrollView.zoomScale > scrollView.minimumZoomScale + 0.01)
        }

        @objc func toggleZoom(_ recognizer: UITapGestureRecognizer) {
            guard let scrollView = recognizer.view as? UIScrollView else { return }
            if scrollView.zoomScale > scrollView.minimumZoomScale {
                scrollView.setZoomScale(scrollView.minimumZoomScale, animated: true)
                return
            }

            let point = recognizer.location(in: imageView)
            let targetScale = min(2, scrollView.maximumZoomScale)
            let width = scrollView.bounds.width / targetScale
            let height = scrollView.bounds.height / targetScale
            scrollView.zoom(
                to: CGRect(
                    x: point.x - width / 2,
                    y: point.y - height / 2,
                    width: width,
                    height: height
                ),
                animated: true
            )
        }
    }

    func makeCoordinator() -> Coordinator {
        let coordinator = Coordinator()
        coordinator.onZoomStateChange = onZoomStateChange
        return coordinator
    }

    func makeUIView(context: Context) -> UIScrollView {
        let scrollView = UIScrollView()
        scrollView.delegate = context.coordinator
        scrollView.minimumZoomScale = 1
        scrollView.maximumZoomScale = 5
        scrollView.bouncesZoom = true
        scrollView.backgroundColor = .clear

        let imageView = context.coordinator.imageView
        imageView.translatesAutoresizingMaskIntoConstraints = false
        imageView.contentMode = .scaleAspectFit
        imageView.isAccessibilityElement = false
        scrollView.addSubview(imageView)
        NSLayoutConstraint.activate([
            imageView.leadingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.leadingAnchor),
            imageView.trailingAnchor.constraint(equalTo: scrollView.contentLayoutGuide.trailingAnchor),
            imageView.topAnchor.constraint(equalTo: scrollView.contentLayoutGuide.topAnchor),
            imageView.bottomAnchor.constraint(equalTo: scrollView.contentLayoutGuide.bottomAnchor),
            imageView.widthAnchor.constraint(equalTo: scrollView.frameLayoutGuide.widthAnchor),
            imageView.heightAnchor.constraint(equalTo: scrollView.frameLayoutGuide.heightAnchor),
        ])

        let doubleTap = UITapGestureRecognizer(
            target: context.coordinator,
            action: #selector(Coordinator.toggleZoom(_:))
        )
        doubleTap.numberOfTapsRequired = 2
        scrollView.addGestureRecognizer(doubleTap)
        return scrollView
    }

    func updateUIView(_ scrollView: UIScrollView, context: Context) {
        context.coordinator.onZoomStateChange = onZoomStateChange
        if !isSelected, scrollView.zoomScale != scrollView.minimumZoomScale {
            scrollView.setZoomScale(scrollView.minimumZoomScale, animated: false)
        }
        guard context.coordinator.currentSource != source else { return }
        context.coordinator.currentSource = source
        context.coordinator.imageView.image = image(for: source)
        scrollView.setZoomScale(scrollView.minimumZoomScale, animated: false)
    }

    private func image(for source: PrototypeImageSource) -> UIImage? {
        switch source {
        case let .asset(name):
            UIImage(named: name)
        case let .data(data):
            PrototypePreparedImageCache.image(from: data)
        }
    }
}
