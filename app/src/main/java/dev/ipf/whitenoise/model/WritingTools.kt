package dev.ipf.whitenoise.model

enum class WritingOperation { Proofread, Rewrite, Summarize }
enum class WritingScenario(val developerLabel: String) {
    Ready("Local ready"), Unavailable("Local model unavailable once"), DownloadRequired("Model download required"),
    Timeout("Timeout once"), Refused("Refusal once"), TooLong("Input too long once"), NetworkConsent("External provider disclosure"),
}
enum class WritingPhase { Choose, Loading, Preview, Unavailable, DownloadRequired, Downloading, NetworkConsent, Timeout, Refused, TooLong, Stale }
data class WritingDraft(
    val profileId: String, val chatId: String, val text: String, val selectionStart: Int, val selectionEnd: Int,
    val context: String = "", val editable: Boolean = true,
)
data class WritingSession(
    val id: Long, val draft: WritingDraft, val revision: Long, val start: Int, val end: Int,
    val scenario: WritingScenario, val operation: WritingOperation? = null,
    val phase: WritingPhase = WritingPhase.Choose, val suggestion: String? = null, val selectionOnly: Boolean = false,
) {
    val original: String get() = draft.text.substring(start, end)
}
data class WritingReplacement(val text: String, val selectionStart: Int, val selectionEnd: Int)

/** Deterministic local prototype transformations. No provider, network, model or draft mutation. */
object WritingTools {
    const val MaximumInput = 8_000
    fun begin(id: Long, draft: WritingDraft, revision: Long, selected: Boolean, scenario: WritingScenario): WritingSession? {
        if (!draft.editable || draft.text.isBlank()) return null
        val start = if (selected) minOf(draft.selectionStart, draft.selectionEnd).coerceIn(0, draft.text.length) else 0
        val end = if (selected) maxOf(draft.selectionStart, draft.selectionEnd).coerceIn(start, draft.text.length) else draft.text.length
        if (start == end || draft.text.substring(start, end).isBlank()) return null
        // Do not permit replacing half a UTF-16 character. Rich-token boundaries are checked before transforming.
        if (splitsSurrogate(draft.text, start) || splitsSurrogate(draft.text, end)) return null
        return WritingSession(id, draft, revision, start, end, scenario,
            phase = if (end - start > MaximumInput) WritingPhase.TooLong else WritingPhase.Choose, selectionOnly = selected)
    }
    fun valid(session: WritingSession, current: WritingDraft, revision: Long): Boolean =
        current.editable && session.draft == current && session.revision == revision

    fun choose(session: WritingSession, operation: WritingOperation): WritingSession = session.copy(operation = operation,
        phase = if (session.original.length > MaximumInput) WritingPhase.TooLong else when (session.scenario) {
            WritingScenario.DownloadRequired -> WritingPhase.DownloadRequired
            WritingScenario.NetworkConsent -> WritingPhase.NetworkConsent
            else -> WritingPhase.Loading
        })

    fun finish(session: WritingSession, current: WritingDraft, revision: Long): WritingSession {
        if (!valid(session, current, revision)) return session.copy(phase = WritingPhase.Stale, suggestion = null)
        if (session.phase == WritingPhase.Downloading) return session.copy(phase = WritingPhase.Loading, scenario = WritingScenario.Ready)
        if (session.phase != WritingPhase.Loading || session.operation == null) return session
        val phase = when (session.scenario) {
            WritingScenario.Unavailable -> WritingPhase.Unavailable
            WritingScenario.Timeout -> WritingPhase.Timeout
            WritingScenario.Refused -> WritingPhase.Refused
            WritingScenario.TooLong -> WritingPhase.TooLong
            else -> WritingPhase.Preview
        }
        return session.copy(phase = phase, suggestion = if (phase == WritingPhase.Preview) transform(session) else null)
    }

    fun apply(session: WritingSession, current: WritingDraft, revision: Long): WritingReplacement? {
        if (!valid(session, current, revision) || session.phase != WritingPhase.Preview) return null
        val replacement = session.suggestion?.takeIf { it != session.original } ?: return null
        val text = current.text.replaceRange(session.start, session.end, replacement)
        val end = session.start + replacement.length
        return WritingReplacement(text, if (session.selectionOnly) session.start else end, end)
    }

    private fun splitsSurrogate(text: String, offset: Int) = offset in 1 until text.length &&
        text[offset - 1].isHighSurrogate() && text[offset].isLowSurrogate()
    // Protect complete markup, mentions, links and code from phrase substitutions or partial selection.
    private val protected = Regex("```[\\s\\S]*?```|`[^`]*`|\\[[^]\\n]*]\\([^)]*\\)|https?://\\S+|nostr:\\S+|@[^\\n]+|\\*\\*[^*]+\\*\\*|__[^_]+__|\\*[^*\\n]+\\*|_[^_\\n]+_")
    private fun transform(session: WritingSession): String {
        val text = session.original
        val tokens = protected.findAll(session.draft.text).map { it.range }.toList()
        if (tokens.any { (session.start > it.first && session.start <= it.last) || (session.end > it.first && session.end <= it.last) }) return text
        if (session.operation == WritingOperation.Summarize) {
            if (tokens.isNotEmpty() || text.contains('\n') || text.contains('>')) return text
            val sentences = Regex("(?<=[.!?])\\s+").split(text)
            return if (sentences.size > 1) sentences.first() else text
        }
        val edits = if (session.operation == WritingOperation.Proofread) listOf(
            "\\bteh\\b" to "the", "\\brecieve\\b" to "receive", "\\btommorow\\b" to "tomorrow",
            "\\bi\\b" to "I", "\\bdont\\b" to "don't", "\\bim\\b" to "I'm",
        ) else listOf("\\bcan you\\b" to "could you", "\\bI want to\\b" to "I'd like to", "\\basap\\b" to "when you can")
        var result = text
        val protectedLocal = protected.findAll(text).map { it.range }.toList()
        val replacements = edits.flatMap { (pattern, replacement) -> Regex(pattern, RegexOption.IGNORE_CASE).findAll(text)
            .filter { match -> protectedLocal.none { match.range.first <= it.last && match.range.last >= it.first } }
            .map { it.range to replacement }.toList() }.sortedByDescending { it.first.first }
        for ((range, replacement) in replacements) result = result.replaceRange(range, replacement)
        return result
    }
}
