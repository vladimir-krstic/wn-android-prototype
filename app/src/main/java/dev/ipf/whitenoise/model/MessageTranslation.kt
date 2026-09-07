package dev.ipf.whitenoise.model

/** Fixed local examples only. Language names are localized by the UI locale. */
enum class TranslationLanguage(val tag: String) { English("en"), Spanish("es"), Serbian("sr"), German("de"), French("fr"), Arabic("ar") }
enum class TranslationOverride { Inherit, On, Off }
data class ChatTranslationPreference(val mode: TranslationOverride = TranslationOverride.Inherit, val target: TranslationLanguage? = null)
data class TranslationPreferences(
    val automatic: Boolean = false,
    val target: TranslationLanguage = TranslationLanguage.English,
    val lastManual: TranslationLanguage? = null,
    val chats: Map<String, ChatTranslationPreference> = emptyMap(),
) {
    fun automatic(chatId: String) = when (chats[chatId]?.mode ?: TranslationOverride.Inherit) {
        TranslationOverride.Inherit -> automatic
        TranslationOverride.On -> true
        TranslationOverride.Off -> false
    }
    fun target(chatId: String) = chats[chatId]?.target ?: target
}
enum class TranslationScenario(val developerLabel: String) {
    Ready("Local ready"), Unavailable("Offline engine unavailable once"), MissingPack("Language pack missing"),
    Unsupported("Unsupported language once"), Uncertain("Source detection uncertain once"), Timeout("Timeout once"), Error("Error once"),
}
enum class TranslationPhase { Loading, Ready, MissingPack, Downloading, Unavailable, Unsupported, Uncertain, Timeout, Error, Cancelled }
data class TranslationResult(val source: TranslationLanguage?, val text: String?, val phase: TranslationPhase)
data class MessageTranslation(
    val message: ChatMessage, val target: TranslationLanguage, val generation: Long,
    val automatic: Boolean, val scenario: TranslationScenario, val phase: TranslationPhase = TranslationPhase.Loading,
    val source: TranslationLanguage? = null, val translated: String? = null, val showOriginal: Boolean = false,
)
object TranslationExamples {
    fun eligible(message: ChatMessage) = !message.isDeleted && message.text.isNotBlank() &&
        message.attachments.none { it.kind == MessageAttachmentKind.Voice } &&
        message.editAttempt == null && message.agentOperation == null && message.nostrEvents.isEmpty() &&
        message.text.length <= 8_000 && message.expiresAtMillis?.let { it <= MessageForwarding.nowMillis } != true && LocationSharing.fromMessage(message) == null
    fun detectedLanguage(text: String): TranslationLanguage? = translate(text, TranslationLanguage.English).source
    fun canTranslateTo(message: ChatMessage, target: TranslationLanguage) = eligible(message) && detectedLanguage(message.text) != target
    private val phrases = listOf(
        listOf("This is the entrance.", "Esta es la entrada.", "Ovo je ulaz.", "Das ist der Eingang.", "Voici l’entrée.", "هذا هو المدخل."),
        listOf("Hello.", "Hola.", "Zdravo.", "Hallo.", "Bonjour.", "مرحبًا."),
        listOf("See you tomorrow.", "Hasta mañana.", "Vidimo se sutra.", "Bis morgen.", "À demain.", "أراك غدًا."),
        listOf("Thank you!", "¡Gracias!", "Hvala!", "Danke!", "Merci !", "شكرًا!"),
        listOf("How are you?", "¿Cómo estás?", "Kako si?", "Wie geht es dir?", "Comment vas-tu ?", "كيف حالك؟"),
    )
    private val protected = Regex("```[\\s\\S]*?```|`[^`]*`|https?://\\S+|nostr:\\S+|@[^\\n]+|\\]\\([^)]*\\)")
    fun translate(text: String, target: TranslationLanguage): TranslationResult {
        val ranges = protected.findAll(text).map { it.range }.toList()
        val hits = phrases.flatMap { phrase -> phrase.mapIndexed { index, value ->
            Regex(Regex.escape(value)).findAll(text).filter { match -> ranges.none { match.range.first <= it.last && match.range.last >= it.first } }
                .map { Triple(it.range, TranslationLanguage.entries[index], phrase[target.ordinal]) }.toList()
        }.flatten() }
        val sources = hits.map { it.second }.distinct()
        if (sources.size != 1) return TranslationResult(null, null, TranslationPhase.Uncertain)
        // Never label partially translated prose as a complete translation. Markup and protected tokens survive verbatim.
        var remainder = text
        (hits.map { it.first } + ranges).sortedByDescending { it.first }.forEach { remainder = remainder.removeRange(it) }
        if (remainder.any { it.isLetterOrDigit() }) return TranslationResult(sources.single(), null, TranslationPhase.Unsupported)
        var result = text
        hits.sortedByDescending { it.first.first }.forEach { result = result.replaceRange(it.first, it.third) }
        return TranslationResult(sources.single(), result, TranslationPhase.Ready)
    }
}
