package dev.ipf.whitenoise.model

data class DeterministicLinkPreview(
    val url: String,
    val title: String,
    val domain: String,
    val summary: String,
    val image: ProfileAvatar? = null,
) {
    fun attachment(id: String): MessageAttachment = MessageAttachment(
        id = id,
        kind = MessageAttachmentKind.Link,
        label = "Link",
        images = listOfNotNull(image),
        externalUri = url,
        linkTitle = title,
        linkDomain = domain,
        linkSummary = summary,
    )
}

object LinkPreviewDetector {
    private val httpsPattern = Regex("https://[^\\s]+", RegexOption.IGNORE_CASE)

    fun first(text: String): DeterministicLinkPreview? {
        val value = httpsPattern.find(text)?.value?.trimEnd('.', ',', ')', ']', '}') ?: return null
        val domain = value.removePrefix("https://").substringBefore('/').lowercase()
        if (domain.isBlank() || !domain.contains('.')) return null
        return when (domain) {
            "whitenoise.chat", "www.whitenoise.chat" -> DeterministicLinkPreview(
                value,
                "White Noise",
                domain,
                "Private, resilient conversations.",
                ProfileAvatar.Asset(AvatarAsset.Marmota),
            )
            "developer.android.com" -> DeterministicLinkPreview(
                value,
                "Android Developers",
                domain,
                "Guidance for building high-quality Android experiences.",
                ProfileAvatar.Asset(AvatarAsset.OpenCircuit),
            )
            "developer.apple.com" -> DeterministicLinkPreview(
                value,
                "Apple Developer",
                domain,
                "Design and development guidance.",
                ProfileAvatar.Asset(AvatarAsset.OpenCircuit),
            )
            else -> DeterministicLinkPreview(
                value,
                domain,
                domain,
                "A link shared in White Noise.",
            )
        }
    }
}

enum class MediaGridLayout {
    Single,
    Two,
    Three,
    Four,
    Five,
    FiveWithOverflow,
}

object MediaLayout {
    fun forCount(count: Int): MediaGridLayout = when {
        count <= 1 -> MediaGridLayout.Single
        count == 2 -> MediaGridLayout.Two
        count == 3 -> MediaGridLayout.Three
        count == 4 -> MediaGridLayout.Four
        count == 5 -> MediaGridLayout.Five
        else -> MediaGridLayout.FiveWithOverflow
    }

    fun visibleCount(count: Int): Int = count.coerceIn(0, 5)

    fun overflowCount(count: Int): Int = (count - 5).coerceAtLeast(0)
}

object VoiceMessageFixture {
    const val transcript = "The trail is quiet this morning. Let’s meet by the old bridge at nine."
    const val durationSeconds = 8

    fun result(
        id: String,
        format: VoiceMessageFormat,
        editedTranscript: String = transcript,
    ): Pair<String, List<MessageAttachment>> {
        val normalizedTranscript = editedTranscript.trim()
        return when (format) {
            VoiceMessageFormat.Voice -> "" to listOf(attachment(id, format, null))
            VoiceMessageFormat.Text -> normalizedTranscript to emptyList()
            VoiceMessageFormat.Both -> normalizedTranscript to listOf(
                attachment(id, format, normalizedTranscript),
            )
        }
    }

    private fun attachment(
        id: String,
        format: VoiceMessageFormat,
        transcript: String?,
    ) = MessageAttachment(
        id = id,
        kind = MessageAttachmentKind.Voice,
        label = "Voice message",
        transcript = transcript,
        durationSeconds = durationSeconds,
        voiceFormat = format,
    )
}

data class ComposerSeed(
    val text: String = "",
    val attachments: List<MessageAttachment> = emptyList(),
    val suppressedLinkUrl: String? = null,
    val replyMessageId: String? = null,
)

object ComposerFixtures {
    fun seed(chatId: String, fallbackText: String = ""): ComposerSeed = when (chatId) {
        "catalog-direct-new-draft" -> ComposerSeed("STATE-01: Unsent draft")
        "catalog-composer-text" -> ComposerSeed("Here’s the updated plan.")
        "catalog-composer-multiline" -> ComposerSeed(
            "I pulled together the notes:\n• Confirm the route\n• Pack water\n• Leave by nine",
        )
        "catalog-composer-link" -> ComposerSeed(
            text = "https://whitenoise.chat",
            suppressedLinkUrl = "https://whitenoise.chat",
        )
        "catalog-composer-link-preview" -> ComposerSeed(
            "Worth a look: https://developer.apple.com/design/human-interface-guidelines",
        )
        "catalog-composer-photo" -> ComposerSeed(
            attachments = listOf(photo("CMP-PHOTO", AvatarAsset.Marmot, "Photo ready to send")),
        )
        "catalog-composer-photo-album" -> ComposerSeed(
            text = "A few from today.",
            attachments = listOf(
                photo("CMP-ALBUM-1", AvatarAsset.Marmot, "Marmot"),
                photo("CMP-ALBUM-2", AvatarAsset.Badger, "Badger"),
                photo("CMP-ALBUM-3", AvatarAsset.Fox, "Fox"),
                photo("CMP-ALBUM-4", AvatarAsset.Sloth, "Sloth"),
            ),
        )
        "catalog-composer-mixed-media" -> ComposerSeed(
            text = "Photos and a short clip from the walk.",
            attachments = listOf(
                photo("CMP-MIXED-1", AvatarAsset.Marmot, "Trail photo"),
                photo("CMP-MIXED-2", AvatarAsset.Ostrich, "Walk photo"),
                MessageAttachment(
                    id = "CMP-MIXED-3",
                    kind = MessageAttachmentKind.Video,
                    label = "Trail video, 0:12",
                    images = listOf(ProfileAvatar.Asset(AvatarAsset.GardenClub)),
                ),
            ),
        )
        "catalog-composer-file" -> ComposerSeed(
            text = "Here’s the brief.",
            attachments = listOf(
                MessageAttachment("CMP-FILE", MessageAttachmentKind.File, "Project Brief.pdf"),
            ),
        )
        "catalog-composer-gif" -> ComposerSeed(
            attachments = listOf(
                MessageAttachment(
                    "CMP-GIF",
                    MessageAttachmentKind.Gif,
                    "Marmot looking around",
                    images = listOf(ProfileAvatar.Asset(AvatarAsset.Marmot)),
                ),
            ),
        )
        "catalog-composer-contact" -> ComposerSeed(
            text = "Maya can help with this.",
            attachments = listOf(
                MessageAttachment("CMP-CONTACT", MessageAttachmentKind.Contact, "Contact: Maya Chen"),
            ),
        )
        "catalog-composer-reply" -> ComposerSeed(
            text = "Yes—Thursday afternoon works for me.",
            replyMessageId = "CMP-REPLY-source",
        )
        "catalog-composer-mention" -> ComposerSeed("@Maya Chen can you take a look?")
        else -> ComposerSeed(text = fallbackText)
    }

    private fun photo(id: String, asset: AvatarAsset, label: String) = MessageAttachment(
        id = id,
        kind = MessageAttachmentKind.Photo,
        label = label,
        images = listOf(ProfileAvatar.Asset(asset)),
    )
}
