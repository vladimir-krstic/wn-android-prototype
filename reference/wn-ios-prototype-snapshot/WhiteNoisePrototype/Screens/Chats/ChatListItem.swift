import Foundation

struct ChatListItem: Identifiable, Equatable {
    enum Avatar: Equatable {
        case asset(String)
        case imageData(Data)
        case monogram(String)
        case systemSymbol(String)
    }

    enum MembershipState: Equatable {
        case invited
        case active
        case left
        case removed
    }

    enum MuteDuration: String, CaseIterable, Identifiable {
        case oneHour
        case eightHours
        case oneDay
        case oneWeek
        case always

        var id: Self {
            self
        }

        var title: String {
            switch self {
            case .oneHour:
                "1 Hour"
            case .eightHours:
                "8 Hours"
            case .oneDay:
                "1 Day"
            case .oneWeek:
                "1 Week"
            case .always:
                "Always"
            }
        }
    }

    enum DeliveryState: Equatable {
        case none
        case failed
    }

    enum AttachmentPreview: Equatable {
        case photo
        case photos(Int)
        case video
        case voiceMessage
        case file(String)
        case contact(String)
        case link
        case gif

        var symbol: String {
            switch self {
            case .photo:
                "photo"
            case .photos:
                "photo.stack"
            case .video:
                "video.fill"
            case .voiceMessage:
                "waveform"
            case .file:
                "doc"
            case .contact:
                "person.crop.circle"
            case .link:
                "link"
            case .gif:
                "play.rectangle.fill"
            }
        }

        var label: String {
            switch self {
            case .photo:
                "Photo"
            case let .photos(count):
                "\(count) Photos"
            case .video:
                "Video"
            case .voiceMessage:
                "Voice message"
            case let .file(name):
                name
            case let .contact(name):
                "Contact: \(name)"
            case .link:
                "Link"
            case .gif:
                "GIF"
            }
        }
    }

    let id: String
    let title: String
    let avatar: Avatar
    let avatarPublicKey: String?
    let isGroup: Bool
    var preview: String
    var previewAuthor: String?
    var attachmentPreview: AttachmentPreview?
    var timestamp: String
    var membershipState: MembershipState
    var invitationInviterName: String?
    var isArchived: Bool
    var isPinned: Bool
    var unreadCount: Int
    var isMarkedUnread: Bool
    var muteDuration: MuteDuration?
    var disappearingMessageDuration: PrototypeDisappearingMessageDuration
    let isDraft: Bool
    var deliveryState: DeliveryState

    init(
        id: String,
        title: String,
        avatar: Avatar,
        avatarPublicKey: String? = nil,
        isGroup: Bool = false,
        preview: String,
        previewAuthor: String? = nil,
        attachmentPreview: AttachmentPreview? = nil,
        timestamp: String,
        membershipState: MembershipState = .active,
        invitationInviterName: String? = nil,
        isArchived: Bool,
        isPinned: Bool = false,
        unreadCount: Int,
        isMarkedUnread: Bool = false,
        isMuted: Bool,
        disappearingMessageDuration: PrototypeDisappearingMessageDuration = .off,
        isDraft: Bool,
        deliveryState: DeliveryState
    ) {
        self.id = id
        self.title = title
        self.avatar = avatar
        self.avatarPublicKey = avatarPublicKey
        self.isGroup = isGroup
        self.preview = preview
        self.previewAuthor = previewAuthor
        self.attachmentPreview = attachmentPreview
        self.timestamp = timestamp
        self.membershipState = membershipState
        self.invitationInviterName = invitationInviterName
        self.isArchived = isArchived
        self.isPinned = isPinned
        self.unreadCount = unreadCount
        self.isMarkedUnread = isMarkedUnread
        self.muteDuration = isMuted ? .always : nil
        self.disappearingMessageDuration = disappearingMessageDuration
        self.isDraft = isDraft
        self.deliveryState = deliveryState
    }

    var isUnread: Bool {
        unreadCount > 0 || isMarkedUnread
    }

    var isMuted: Bool {
        muteDuration != nil
    }

    var hasDisappearingMessages: Bool {
        disappearingMessageDuration.isEnabled
    }

    var hasEndedMembership: Bool {
        switch membershipState {
        case .left, .removed: true
        case .invited, .active: false
        }
    }

    var isInvitationPending: Bool {
        membershipState == .invited
    }

    var visiblePreviewAuthor: String? {
        membershipState == .active ? previewAuthor : nil
    }

    var searchablePreview: String {
        let content = visiblePreview

        if let previewAuthor = visiblePreviewAuthor {
            return "\(previewAuthor): \(content)"
        } else {
            return content
        }
    }

    var visiblePreview: String {
        switch membershipState {
        case .invited:
            return "Invited to chat by \(invitationInviterName ?? "Someone")"
        case .left:
            return isGroup ? "You left this group." : "You left this chat."
        case .removed:
            return isGroup
                ? "You were removed from this group."
                : "You were removed from this chat."
        case .active:
            break
        }

        if preview.isEmpty, let attachmentPreview {
            return attachmentPreview.label
        } else {
            return preview
        }
    }
}
