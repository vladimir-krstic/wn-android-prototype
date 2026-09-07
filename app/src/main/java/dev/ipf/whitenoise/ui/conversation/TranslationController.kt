package dev.ipf.whitenoise.ui.conversation

import androidx.compose.runtime.*
import dev.ipf.whitenoise.model.*

internal val LocalMessageTranslation = staticCompositionLocalOf<TranslationController?> { null }

/** Ephemeral state, owned by one conversation composition. No translated text enters profile state. */
@Stable
internal class TranslationController(val profileId: String, val chatId: String) {
    var entries by mutableStateOf<Map<String, MessageTranslation>>(emptyMap()); private set
    private var messages = emptyMap<String, ChatMessage>()
    private var configuration: Pair<Boolean, TranslationLanguage>? = null
    private var generation = 0L
    private var suspended = false
    private var preferences = TranslationPreferences()
    fun observe(loaded: List<ChatMessage>, preferences: TranslationPreferences, visible: Set<String> = emptySet(), nextScenario: () -> TranslationScenario = { TranslationScenario.Ready }) {
        this.preferences = preferences
        messages = loaded.associateBy { it.id }
        val config = preferences.automatic(chatId) to preferences.target(chatId)
        if (configuration != null && configuration != config) entries = emptyMap()
        configuration = config
        entries = entries.filter { (id, value) -> messages[id] == value.message && TranslationExamples.eligible(value.message) }
        if (config.first && !suspended) {
            val automaticWindow = loaded.asReversed().sortedByDescending { it.id in visible }
                .filter { it.authorId != profileId && TranslationExamples.canTranslateTo(it, config.second) }.take(MaximumEntries)
            val wanted = automaticWindow.map { it.id }.toSet()
            entries = entries.filter { (_, value) -> !value.automatic || value.message.id in wanted }
            automaticWindow.forEach { message ->
                    if (message.id !in entries && entries.size < MaximumEntries) request(message.id, config.second, true, nextScenario())
                }
        }
    }
    fun request(id: String, target: TranslationLanguage, automatic: Boolean = false, scenario: TranslationScenario = TranslationScenario.Ready) {
        val message = messages[id]?.takeIf { TranslationExamples.canTranslateTo(it, target) } ?: return
        val prior = entries[id]
        if (prior?.target == target && prior.message == message && prior.phase == TranslationPhase.Ready) {
            entries = entries + (id to prior.copy(showOriginal = false)); return
        }
        if (prior?.target == target && prior.message == message && prior.phase in setOf(TranslationPhase.Loading, TranslationPhase.Downloading)) return
        if (entries.size >= MaximumEntries && id !in entries) entries = entries - entries.keys.first()
        entries = entries + (id to MessageTranslation(message, target, ++generation, automatic, scenario,
            phase = if (scenario == TranslationScenario.MissingPack) TranslationPhase.MissingPack else TranslationPhase.Loading))
    }
    fun state(message: ChatMessage): MessageTranslation? = entries[message.id]?.takeIf { it.message == message && TranslationExamples.eligible(message) }
    fun text(message: ChatMessage): String = state(message)?.takeIf { it.phase == TranslationPhase.Ready && !it.showOriginal }?.translated ?: message.text
    fun complete(id: String, expected: Long) {
        val value = entries[id]?.takeIf { it.generation == expected && messages[id] == it.message } ?: return
        if (value.phase == TranslationPhase.Downloading) {
            entries = entries + (id to value.copy(phase = TranslationPhase.Loading, scenario = TranslationScenario.Ready)); return
        }
        if (value.phase != TranslationPhase.Loading || suspended) return
        val result = when (value.scenario) {
            TranslationScenario.Ready, TranslationScenario.MissingPack -> TranslationExamples.translate(value.message.text, value.target)
            else -> TranslationResult(null, null, when (value.scenario) {
                TranslationScenario.Unavailable -> TranslationPhase.Unavailable
                TranslationScenario.Unsupported -> TranslationPhase.Unsupported
                TranslationScenario.Uncertain -> TranslationPhase.Uncertain
                TranslationScenario.Timeout -> TranslationPhase.Timeout
                else -> TranslationPhase.Error
            })
        }
        entries = entries + (id to value.copy(phase = result.phase, source = result.source, translated = result.text))
    }
    fun toggle(id: String) { entries[id]?.takeIf { it.phase == TranslationPhase.Ready }?.let { entries = entries + (id to it.copy(showOriginal = !it.showOriginal)) } }
    fun cancel(id: String) { entries[id]?.let { entries = entries + (id to it.copy(phase = TranslationPhase.Cancelled, translated = null, generation = ++generation)) } }
    fun retry(id: String) { entries[id]?.let { request(id, it.target, it.automatic, TranslationScenario.Ready) } }
    fun download(id: String) { entries[id]?.takeIf { it.phase == TranslationPhase.MissingPack }?.let {
        entries = entries + (id to it.copy(phase = TranslationPhase.Downloading, generation = ++generation))
    } }
    fun pause() { suspended = true; entries = emptyMap(); generation++ }
    fun resume() { suspended = false; observe(messages.values.toList(), preferences) }
    companion object { const val MaximumEntries = 40 }
}
