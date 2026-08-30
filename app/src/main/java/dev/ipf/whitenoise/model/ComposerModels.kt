package dev.ipf.whitenoise.model

import java.net.URI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

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
        val domain = runCatching { URI(value).host?.lowercase() }.getOrNull()
            ?.trimEnd('.')
            ?: return null
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
        durationSeconds: Int = VoiceMessageFixture.durationSeconds,
    ): Pair<String, List<MessageAttachment>> {
        val normalizedTranscript = editedTranscript.trim()
        return when (format) {
            VoiceMessageFormat.Voice -> "" to listOf(attachment(id, format, null, durationSeconds))
            VoiceMessageFormat.Text -> normalizedTranscript to emptyList()
            VoiceMessageFormat.Both -> normalizedTranscript to listOf(
                attachment(id, format, normalizedTranscript, durationSeconds),
            )
        }
    }

    private fun attachment(
        id: String,
        format: VoiceMessageFormat,
        transcript: String?,
        durationSeconds: Int,
    ) = MessageAttachment(
        id = id,
        kind = MessageAttachmentKind.Voice,
        label = "Voice message",
        transcript = transcript,
        durationSeconds = durationSeconds.coerceAtLeast(1),
        voiceFormat = format,
    )
}

data class VoiceDraftSubmission(
    val format: VoiceMessageFormat,
    val transcript: String,
    val durationSeconds: Int,
)

sealed interface ComposerVoiceState {
    data object Idle : ComposerVoiceState

    data class Recording(val elapsedTenths: Int = 0) : ComposerVoiceState

    data class Review(
        val durationSeconds: Int,
        val transcript: String? = null,
        val format: VoiceMessageFormat = VoiceMessageFormat.Voice,
        val isTranscribing: Boolean = false,
        val playbackTenths: Int = 0,
        val isPlaying: Boolean = false,
    ) : ComposerVoiceState
}

object ComposerVoiceReducer {
    fun start(state: ComposerVoiceState): ComposerVoiceState =
        if (state == ComposerVoiceState.Idle) ComposerVoiceState.Recording() else state

    fun tick(state: ComposerVoiceState): ComposerVoiceState = when (state) {
        is ComposerVoiceState.Recording -> state.copy(elapsedTenths = state.elapsedTenths + 1)
        else -> state
    }

    fun stop(state: ComposerVoiceState): ComposerVoiceState = when (state) {
        is ComposerVoiceState.Recording -> ComposerVoiceState.Review(
            durationSeconds = ((state.elapsedTenths + 9) / 10).coerceAtLeast(1),
        )
        else -> state
    }

    fun beginTranscription(state: ComposerVoiceState): ComposerVoiceState = when (state) {
        is ComposerVoiceState.Review -> if (state.transcript == null) {
            state.copy(isTranscribing = true, isPlaying = false)
        } else {
            state
        }
        else -> state
    }

    fun finishTranscription(
        state: ComposerVoiceState,
        transcript: String,
    ): ComposerVoiceState = when (state) {
        is ComposerVoiceState.Review -> state.copy(
            transcript = transcript,
            format = VoiceMessageFormat.Both,
            isTranscribing = false,
        )
        else -> state
    }

    fun selectFormat(
        state: ComposerVoiceState,
        format: VoiceMessageFormat,
    ): ComposerVoiceState = when (state) {
        is ComposerVoiceState.Review -> state.copy(format = format, isPlaying = false)
        else -> state
    }

    fun editTranscript(
        state: ComposerVoiceState,
        transcript: String,
    ): ComposerVoiceState = when (state) {
        is ComposerVoiceState.Review -> state.copy(transcript = transcript)
        else -> state
    }

    fun togglePlayback(state: ComposerVoiceState): ComposerVoiceState = when (state) {
        is ComposerVoiceState.Review -> state.copy(
            playbackTenths = if (state.playbackTenths >= state.durationSeconds * 10) 0 else state.playbackTenths,
            isPlaying = !state.isPlaying,
        )
        else -> state
    }

    fun advancePlayback(state: ComposerVoiceState): ComposerVoiceState = when (state) {
        is ComposerVoiceState.Review -> {
            val next = state.playbackTenths + 1
            if (next >= state.durationSeconds * 10) {
                state.copy(playbackTenths = state.durationSeconds * 10, isPlaying = false)
            } else {
                state.copy(playbackTenths = next)
            }
        }
        else -> state
    }

    fun canSend(state: ComposerVoiceState): Boolean = when (state) {
        is ComposerVoiceState.Review -> state.format == VoiceMessageFormat.Voice ||
            !state.transcript.isNullOrBlank()
        else -> false
    }

    fun restore(state: ComposerVoiceState): ComposerVoiceState = when (state) {
        ComposerVoiceState.Idle -> state
        is ComposerVoiceState.Recording -> stop(state)
        is ComposerVoiceState.Review -> state.copy(isTranscribing = false, isPlaying = false)
    }
}

/** Deterministic waveform geometry shared by live recording and voice review. */
object ComposerWaveformPolicy {
    const val QuietSample = 0.08f
    const val VisualSamplePeriodTenths = 2

    /** Keeps the elapsed timer precise while advancing the visible waveform at a calmer cadence. */
    fun visualTick(elapsedTenths: Int): Int =
        elapsedTenths.coerceAtLeast(0) / VisualSamplePeriodTenths

    fun liveSample(tick: Int): Float {
        val first = sin(tick * 0.73)
        val second = sin(tick * 0.19 + 1.4)
        return (0.18 + abs(first * second) * 0.82).toFloat().coerceIn(0.1f, 1f)
    }

    fun liveWindow(latestTick: Int, count: Int): List<Float> =
        List(count.coerceAtLeast(1)) { index ->
            val tick = latestTick - count + index + 1
            if (tick > 0) liveSample(tick) else QuietSample
        }

    fun reviewWindow(count: Int): List<Float> = List(count.coerceAtLeast(1)) { index ->
        val first = sin((index + 37) * 0.61)
        val second = sin((index * 3 + 37) * 0.17)
        (0.2 + abs(first * second) * 0.8).toFloat().coerceIn(0.12f, 1f)
    }
}

object ComposerExpansionPolicy {
    const val ExpandedTopGapDp = 24
    const val ProjectedTravelThresholdDp = 48
    const val CompactTextLines = 10
    const val CompactCaptionLines = 6
    const val CompactTranscriptLines = 8

    fun compactLineLimit(hasAttachments: Boolean): Int =
        if (hasAttachments) CompactCaptionLines else CompactTextLines

    fun clampProgress(progress: Float): Float = progress.coerceIn(0f, 1f)

    fun shouldPushTimeline(newestMessageVisible: Boolean): Boolean = newestMessageVisible

    fun destinationExpanded(progress: Float, projectedTravelDp: Float): Boolean = when {
        projectedTravelDp <= -ProjectedTravelThresholdDp -> true
        projectedTravelDp >= ProjectedTravelThresholdDp -> false
        else -> progress >= 0.5f
    }
}

data class ComposerAttachmentSize(
    val heightDp: Int,
    val widthDp: Int,
)

object ComposerAttachmentSizing {
    const val VisualHeightDp = 112
    const val VisualMinWidthDp = 68
    const val VisualMaxWidthDp = 200
    const val UtilityHeightDp = 72
    const val ContactWidthDp = 104
    const val FileWidthDp = 160
    const val VisualShelfHeightDp = 128
    const val UtilityShelfHeightDp = 88

    fun forKind(kind: MessageAttachmentKind, aspectRatio: Float = 4f / 3f): ComposerAttachmentSize =
        if (kind == MessageAttachmentKind.Photo || kind == MessageAttachmentKind.Photos ||
            kind == MessageAttachmentKind.Video ||
            kind == MessageAttachmentKind.Gif
        ) {
            ComposerAttachmentSize(
                heightDp = VisualHeightDp,
                widthDp = (VisualHeightDp * aspectRatio)
                    .roundToInt()
                    .coerceIn(VisualMinWidthDp, VisualMaxWidthDp),
            )
        } else {
            ComposerAttachmentSize(
                heightDp = UtilityHeightDp,
                widthDp = if (kind == MessageAttachmentKind.Contact) ContactWidthDp else FileWidthDp,
            )
        }
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
