import SwiftUI
import UIKit

struct PrototypeComposerLinkPreviewView: View {
    let preview: PrototypeComposerLinkPreview
    let onRemove: () -> Void

    var body: some View {
        ZStack(alignment: .topTrailing) {
            HStack(spacing: 10) {
                artwork

                VStack(alignment: .leading, spacing: 2) {
                    Text(preview.title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(.primary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)

                    Text(preview.domain)
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.trailing, 28)
            }
            .padding(8)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                Color(uiColor: .secondarySystemFill),
                in: .rect(cornerRadius: 14)
            )
            .allowsHitTesting(false)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Link preview, \(preview.title), \(preview.domain)")
            .accessibilityIdentifier("conversation.link-preview")

            PrototypeComposerRemoveButton(
                accessibilityLabel: "Remove Link Preview",
                accessibilityIdentifier: "conversation.link-preview.remove",
                action: onRemove
            )
        }
        .padding(.horizontal, 8)
        .padding(.top, 8)
        .padding(.bottom, 4)
    }

    @ViewBuilder
    private var artwork: some View {
        Group {
            if let image = preview.image {
                PrototypeImageSourceView(source: image)
                    .scaledToFill()
            } else {
                ZStack {
                    Color(uiColor: .tertiarySystemFill)
                    Image(systemName: "link")
                        .font(.title3)
                        .foregroundStyle(.secondary)
                }
            }
        }
        .frame(width: 72, height: 72)
        .clipShape(.rect(cornerRadius: 10))
        .accessibilityHidden(true)
    }
}
