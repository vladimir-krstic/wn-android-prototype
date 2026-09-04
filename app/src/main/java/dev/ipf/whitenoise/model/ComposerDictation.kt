package dev.ipf.whitenoise.model

import java.text.BreakIterator
import java.util.Locale

/** Exact origin identity is shared by voice capture and dictation, never inferred from the visible route. */
data class ComposerCaptureOwner(val profileId: String, val chatId: String)
enum class ComposerCaptureMode { Voice, Dictation }
data class ComposerCaptureLease(val owner: ComposerCaptureOwner, val requestId: Long, val mode: ComposerCaptureMode)
enum class DictationDeliveryMode { Paste, Send }
data class DictationPreferences(val finishAfterSilenceMillis: Long? = null,
    val delivery: DictationDeliveryMode = DictationDeliveryMode.Paste, val disclosureAccepted: Boolean = false) {
    fun withSilence(value: Long?): DictationPreferences = copy(finishAfterSilenceMillis = value?.takeIf { it in silenceChoices })
    companion object { val silenceChoices = listOf(3_000L, 5_000L, 10_000L) }
}
data class DictationService(val packageName: String, val className: String) {
    companion object {
        fun parse(value: String?): DictationService? {
            val parts = value?.trim()?.split('/') ?: return null
            if (parts.size != 2 || parts.any { it.isBlank() || it.any(Char::isWhitespace) }) return null
            return DictationService(parts[0], if (parts[1].startsWith('.')) parts[0] + parts[1] else parts[1])
        }
        fun available(selected: DictationService?, discovered: List<DictationService>): Boolean = selected != null && selected in discovered
    }
}
enum class DictationFailure { ServiceMissing, PermissionDenied, PermissionPermanentlyDenied, MicrophoneBusy, NoSpeech, Network, ServiceBusy, TimedOut, Unknown }
enum class DictationScenario(val developerLabel: String, val failure: DictationFailure? = null) {
    Success("Recognized speech"), ServiceMissing("Selected service missing", DictationFailure.ServiceMissing),
    WrongService("A different service is installed", DictationFailure.ServiceMissing),
    PermissionDenied("Microphone access denied", DictationFailure.PermissionDenied),
    PermissionPermanentlyDenied("Microphone access permanently denied", DictationFailure.PermissionPermanentlyDenied),
    MicrophoneBusy("Microphone in use", DictationFailure.MicrophoneBusy), ServiceBusy("Speech service busy", DictationFailure.ServiceBusy),
    NoSpeech("No speech", DictationFailure.NoSpeech), Network("Network failure", DictationFailure.Network),
    ReadinessTimeout("Service check timeout", DictationFailure.TimedOut), ProcessingTimeout("Transcription timeout", DictationFailure.TimedOut),
    PartialThenFailure("Speech followed by a service failure", DictationFailure.Network), Failure("Recognition failure", DictationFailure.Unknown),
    CommitFailure("Draft/send rejected"),
}
enum class DictationPhase { Disclosure, Preparing, Listening, Processing, Review, Failed, Complete, Cancelled }
enum class DictationReviewReason { OriginChanged, Interrupted, RecognitionFailure, CommitRejected }
data class DictationDraft(val text: String, val revision: Long, val selectionStart: Int, val selectionEnd: Int,
    val attachments: List<MessageAttachment>, val replyId: String?, val suppressedLink: String?, val quality: PhotoQuality) {
    fun matches(other: DictationDraft): Boolean = revision == other.revision && text == other.text && attachments == other.attachments &&
        replyId == other.replyId && suppressedLink == other.suppressedLink && quality == other.quality
    companion object {
        fun capture(chat: Chat, revision: Long, start: Int = chat.draftText.length, end: Int = start) =
            DictationDraft(chat.draftText, revision, start.coerceIn(0, chat.draftText.length), end.coerceIn(0, chat.draftText.length),
                chat.draftAttachments, chat.draftReplyMessageId, chat.suppressedDraftLinkUrl, chat.draftPhotoQuality)
    }
}
data class DictationTextResult(val text: String, val cursor: Int)
object DictationText {
    /** Intersect platform character boundaries with the existing emoji-cluster rules. */
    private fun boundaries(text: String): List<Int> {
        val emoji = mutableSetOf(0); var offset = 0
        splitEmojiGraphemes(text).forEach { offset += it.length; emoji += offset }
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply { setText(text) }
        return buildList { var index = iterator.first(); while (index != BreakIterator.DONE) {
            if (index in emoji) add(index); index = iterator.next()
        } }
    }
    fun insert(draft: DictationDraft, recognized: String): DictationTextResult {
        val text = recognized.trim()
        if (text.isEmpty()) return DictationTextResult(draft.text, draft.selectionEnd)
        val boundaries = boundaries(draft.text)
        val rawStart = minOf(draft.selectionStart, draft.selectionEnd); val rawEnd = maxOf(draft.selectionStart, draft.selectionEnd)
        val start = boundaries.lastOrNull { it <= rawStart } ?: 0
        val end = if (rawStart == rawEnd) start else boundaries.firstOrNull { it >= rawEnd } ?: draft.text.length
        val left = if (start > 0 && !draft.text[start - 1].isWhitespace()) " " else ""
        val right = if (end < draft.text.length && Character.isLetterOrDigit(draft.text.codePointAt(end))) " " else ""
        val inserted = left + text + right
        return DictationTextResult(draft.text.replaceRange(start, end, inserted), start + inserted.length)
    }
    fun append(current: String, recognized: String): DictationTextResult {
        val text = recognized.trim()
        val joined = if (text.isEmpty()) current else current + (if (current.isEmpty() || current.last().isWhitespace()) "" else " ") + text
        return DictationTextResult(joined, joined.length)
    }
}
data class DictationAttempt(
    val id: Long, val owner: ComposerCaptureOwner, val draft: DictationDraft, val preferences: DictationPreferences,
    val scenario: DictationScenario = DictationScenario.Success,
    val phase: DictationPhase = if (preferences.disclosureAccepted) DictationPhase.Preparing else DictationPhase.Disclosure,
    val revision: Long = 0, val elapsedMillis: Long = 0, val lastSpeechMillis: Long? = null,
    val transcript: String = "", val partial: String = "", val failure: DictationFailure? = null,
    val reviewReason: DictationReviewReason? = null,
) {
    val capturing: Boolean get() = phase in setOf(DictationPhase.Preparing, DictationPhase.Listening, DictationPhase.Processing)
    val retainedText: String get() = DictationText.append(transcript, partial).text.trim()
    val terminal: Boolean get() = phase in setOf(DictationPhase.Complete, DictationPhase.Cancelled)
    fun acceptDisclosure(): DictationAttempt = if (phase == DictationPhase.Disclosure) copy(phase = DictationPhase.Preparing, revision = revision + 1) else this
    fun ready(expectedRevision: Long): DictationAttempt = if (revision == expectedRevision && phase == DictationPhase.Preparing)
        copy(phase = DictationPhase.Listening, revision = revision + 1, elapsedMillis = 0) else this
    fun segment(expectedRevision: Long, text: String, final: Boolean): DictationAttempt {
        if (revision != expectedRevision || phase != DictationPhase.Listening) return this
        return if (final) copy(transcript = DictationText.append(transcript, text).text, partial = "", lastSpeechMillis = elapsedMillis, revision = revision + 1)
        else copy(partial = text.trim(), lastSpeechMillis = elapsedMillis, revision = revision + 1)
    }
    fun tick(expectedRevision: Long, millis: Long): DictationAttempt {
        if (revision != expectedRevision || phase != DictationPhase.Listening || millis <= 0) return this
        val next = copy(elapsedMillis = elapsedMillis + millis, revision = revision + 1)
        return if (preferences.finishAfterSilenceMillis?.let { limit -> lastSpeechMillis?.let { next.elapsedMillis - it >= limit } } == true) next.finish() else next
    }
    fun finish(): DictationAttempt = if (phase == DictationPhase.Listening) copy(phase = DictationPhase.Processing, transcript = retainedText,
        partial = "", revision = revision + 1) else this
    fun fail(expectedRevision: Long, reason: DictationFailure): DictationAttempt {
        if (revision != expectedRevision || !capturing) return this
        return if (retainedText.isNotBlank()) review(DictationReviewReason.RecognitionFailure).copy(failure = reason)
        else copy(phase = DictationPhase.Failed, failure = reason, revision = revision + 1)
    }
    fun review(reason: DictationReviewReason): DictationAttempt = if (terminal) this else
        copy(phase = DictationPhase.Review, transcript = retainedText, partial = "", reviewReason = reason, revision = revision + 1)
    fun interrupt(): DictationAttempt = if (terminal || phase == DictationPhase.Review || phase == DictationPhase.Failed) this else
        if (retainedText.isNotBlank()) review(DictationReviewReason.Interrupted) else cancel()
    fun cancel(): DictationAttempt = if (terminal) this else copy(phase = DictationPhase.Cancelled, transcript = "", partial = "", revision = revision + 1)
    fun complete(): DictationAttempt = if (terminal) this else copy(phase = DictationPhase.Complete, transcript = "", partial = "", revision = revision + 1)
    fun deliveryAllowed(current: DictationDraft, currentOwner: ComposerCaptureOwner?, available: Boolean): Boolean =
        phase == DictationPhase.Processing && owner == currentOwner && available && draft.matches(current) && retainedText.isNotBlank()
}

object DictationExamples {
    const val transcript = "The trail is quiet this morning. Let’s meet by the old bridge at nine."
    val selectedService = DictationService("example.speech", "example.speech.RecognitionService")
    fun services(scenario: DictationScenario): List<DictationService> = when (scenario) {
        DictationScenario.ServiceMissing -> emptyList()
        DictationScenario.WrongService -> listOf(DictationService("example.other", "example.other.RecognitionService"))
        else -> listOf(selectedService)
    }
}
