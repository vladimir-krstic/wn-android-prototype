package dev.ipf.whitenoise.model

import java.math.RoundingMode
import java.util.Locale

enum class SpeechEngineTrust { OnDevice, Unknown }
data class SpeechEngineOption(val packageName: String, val label: String, val trust: SpeechEngineTrust)
data class SpeechVoiceKey(val enginePackage: String, val name: String, val localeTag: String)
data class SpeechVoice(val name: String, val localeTag: String, val installed: Boolean = true, val networkRequired: Boolean = false, val quality: Int = 0)
enum class SpeechVoiceUnavailable { InvalidIdentity, NotInstalled, RequiresNetwork, Ambiguous }
data class SpeechVoiceOption(val key: SpeechVoiceKey, val label: String, val unavailable: SpeechVoiceUnavailable?) {
    val selectable: Boolean get() = unavailable == null
}
data class SpeechVoiceResolution(
    val options: List<SpeechVoiceOption> = emptyList(),
    val requested: SpeechVoiceKey? = null,
    val candidates: List<SpeechVoiceKey> = emptyList(),
    val effective: SpeechVoiceKey? = null,
) {
    val usingFallback: Boolean get() = requested != null && effective != null && requested != effective
    /** A bridge reports success only after the native voice was actually accepted. */
    fun applied(key: SpeechVoiceKey?): SpeechVoiceResolution = copy(effective = key?.takeIf { candidate ->
        candidate in candidates && options.singleOrNull { it.key == candidate }?.selectable == true
    })
}

object SpeechVoices {
    fun sameLanguage(first: String, second: String): Boolean {
        val a = Locale.forLanguageTag(first); val b = Locale.forLanguageTag(second)
        if (a.language.isBlank() || b.language.isBlank()) return false
        return a.language.equals(b.language, true) || runCatching {
            a.isO3Language.isNotBlank() && a.isO3Language.equals(b.isO3Language, true)
        }.getOrDefault(false)
    }
    fun resolve(engine: String, localeTag: String, voices: List<SpeechVoice>, selected: SpeechVoiceKey?): SpeechVoiceResolution {
        val matching = voices.filter { sameLanguage(it.localeTag, localeTag) }
        fun SpeechVoice.key() = SpeechVoiceKey(engine, name, this.localeTag)
        val counts = matching.groupingBy { it.key() }.eachCount()
        val options = matching.map { voice ->
            SpeechVoiceOption(voice.key(), voice.name.ifBlank { voice.localeTag }, when {
                engine.isBlank() || voice.name.isBlank() -> SpeechVoiceUnavailable.InvalidIdentity
                !voice.installed -> SpeechVoiceUnavailable.NotInstalled
                voice.networkRequired -> SpeechVoiceUnavailable.RequiresNetwork
                counts[voice.key()] != 1 -> SpeechVoiceUnavailable.Ambiguous
                else -> null
            })
        }.sortedWith(compareBy<SpeechVoiceOption> { it.key.localeTag }.thenBy { it.label })
        val requested = selected?.takeIf { it.enginePackage == engine }
        val eligible = matching.filter { voice -> options.singleOrNull { it.key == voice.key() }?.selectable == true }
            .sortedWith(compareByDescending<SpeechVoice> { it.quality }.thenBy { it.name }.thenBy { it.localeTag }).map { it.key() }
        val candidates = (listOfNotNull(requested?.takeIf { it in eligible }) + eligible).distinct()
        return SpeechVoiceResolution(options, requested, candidates)
    }
}

object SpeechEngines {
    private val onDevicePackages = setOf("app.grapheneos.speechservices", "com.github.olga_yakovleva.rhvoice.android", "com.reecedunn.espeak")
    fun declaredTrust(packageName: String): SpeechEngineTrust = if (packageName in onDevicePackages) SpeechEngineTrust.OnDevice else SpeechEngineTrust.Unknown
    fun preferred(engines: List<SpeechEngineOption>, systemDefault: String?, selected: String?): String? {
        selected?.takeIf { id -> engines.any { it.packageName == id } }?.let { return it }
        return when {
            engines.any { it.packageName == systemDefault && it.trust == SpeechEngineTrust.OnDevice } -> systemDefault
            engines.size == 1 -> engines.single().packageName
            else -> null
        }
    }
    /** Requested metadata is not proof of the package Android actually bound. */
    fun runtimeTrust(requested: String?, verified: String?): SpeechEngineTrust =
        if (requested != null && requested == verified) declaredTrust(requested) else SpeechEngineTrust.Unknown
}

enum class SpeechMixVolume(val fraction: Float) { Quiet(0.35f), Medium(0.60f), Loud(0.85f) }
enum class SpeechAutoReadOverride { On, Off }
data class SpeechPreferences(
    val enginePackage: String? = null,
    val voices: Map<String, SpeechVoiceKey> = emptyMap(),
    val acknowledgedEngines: Set<String> = emptySet(),
    val rate: Float? = null,
    val mixWithMedia: Boolean = false,
    val mixVolume: SpeechMixVolume = SpeechMixVolume.Medium,
    val autoReadDefault: Boolean = false,
    val autoReadOverrides: Map<String, SpeechAutoReadOverride> = emptyMap(),
) {
    fun voice(engine: String): SpeechVoiceKey? = voices[engine]?.takeIf { it.enginePackage == engine }
    fun withVoice(engine: String, key: SpeechVoiceKey?): SpeechPreferences {
        if (engine.isBlank() || (key != null && (key.enginePackage != engine || key.name.isBlank() || key.localeTag.isBlank()))) return this
        return copy(voices = if (key == null) voices - engine else voices + (engine to key))
    }
    fun needsConsent(engine: String, runtimeTrust: SpeechEngineTrust): Boolean = runtimeTrust == SpeechEngineTrust.Unknown && engine.isNotBlank() && engine !in acknowledgedEngines
    fun acknowledge(engine: String): SpeechPreferences = if (engine.isBlank()) this else copy(acknowledgedEngines = acknowledgedEngines + engine)
    fun autoRead(chatId: String): Boolean = autoReadOverrides[chatId]?.let { it == SpeechAutoReadOverride.On } ?: autoReadDefault
    fun withAutoRead(chatId: String, override: SpeechAutoReadOverride?): SpeechPreferences = if (chatId.isBlank()) this else
        copy(autoReadOverrides = if (override == null) autoReadOverrides - chatId else autoReadOverrides + (chatId to override))
    fun withRate(value: Float?): SpeechPreferences = if (value == null) copy(rate = null) else
        SpeechRates.normalize(value)?.let { copy(rate = it) } ?: this
}

object SpeechRates {
    val presets = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f, 2.5f, 3f)
    fun normalize(value: Float): Float? = value.takeIf { it.isFinite() && it in 0.1f..10f }?.let {
        presets.firstOrNull { preset -> preset == it } ?: it.toBigDecimal().setScale(1, RoundingMode.HALF_UP).toFloat()
    }
    fun parse(input: String): Float? {
        val normalized = input.trim().map { c -> c.digitToIntOrNull()?.digitToChar() ?: if (c == ',' || c == '\u066b') '.' else c }.joinToString("")
        if (!Regex("[0-9]+(?:\\.[0-9]+)?").matches(normalized)) return null
        return normalized.toFloatOrNull()?.let(::normalize)
    }
    fun resolved(override: Float?, systemPercent: Float?): Float = override?.let(::normalize) ?: systemPercent
        ?.takeIf { it.isFinite() && it > 0f }?.div(100f) ?: 1f
}

enum class SpeechDiscoveryPhase { Discovering, Ready, Empty, Failed }
data class SpeechDiscovery(
    val revision: Long = 0,
    val phase: SpeechDiscoveryPhase = SpeechDiscoveryPhase.Discovering,
    val engines: List<SpeechEngineOption> = emptyList(),
    val systemDefault: String? = null,
    val activePackage: String? = null,
    val voices: SpeechVoiceResolution = SpeechVoiceResolution(),
    val runtimeTrust: SpeechEngineTrust = SpeechEngineTrust.Unknown,
) {
    val usable: Boolean get() = phase == SpeechDiscoveryPhase.Ready && activePackage != null && engines.any { it.packageName == activePackage } &&
        voices.effective?.let { it.enginePackage == activePackage && it in voices.candidates && voices.options.singleOrNull { option -> option.key == it }?.selectable == true } == true
}

enum class SpeechEngineChangePhase { Consent, Initializing, Failed, Cancelled, Applied }
data class SpeechEngineChange(
    val id: Long,
    val profileId: String,
    val requestedPackage: String,
    val requestedVoice: SpeechVoiceKey? = null,
    val phase: SpeechEngineChangePhase = SpeechEngineChangePhase.Initializing,
    val revision: Long = 0,
) {
    fun consent(): SpeechEngineChange = if (phase == SpeechEngineChangePhase.Consent) copy(phase = SpeechEngineChangePhase.Initializing, revision = revision + 1) else this
    fun cancel(): SpeechEngineChange = if (phase in setOf(SpeechEngineChangePhase.Cancelled, SpeechEngineChangePhase.Applied)) this else copy(phase = SpeechEngineChangePhase.Cancelled, revision = revision + 1)
    fun retry(): SpeechEngineChange = if (phase == SpeechEngineChangePhase.Failed) copy(phase = SpeechEngineChangePhase.Initializing, revision = revision + 1) else this
    fun settle(activeProfileId: String?, expectedRevision: Long, candidate: SpeechDiscovery): SpeechEngineChange {
        if (profileId != activeProfileId || revision != expectedRevision || phase != SpeechEngineChangePhase.Initializing) return this
        return copy(phase = if (candidate.usable && candidate.activePackage == requestedPackage) SpeechEngineChangePhase.Applied else SpeechEngineChangePhase.Failed, revision = revision + 1)
    }
}

enum class SpeechStartRefusal { MediaNotActive, FocusUnavailable }
data class SpeechAudioEnvironment(val mediaActive: Boolean = false, val focusAvailable: Boolean = true)
object SpeechAudioPolicy {
    fun refusal(preferences: SpeechPreferences, environment: SpeechAudioEnvironment): SpeechStartRefusal? = when {
        preferences.mixWithMedia && !environment.mediaActive -> SpeechStartRefusal.MediaNotActive
        !preferences.mixWithMedia && !environment.focusAvailable -> SpeechStartRefusal.FocusUnavailable
        else -> null
    }
    fun volume(preferences: SpeechPreferences): Float = if (preferences.mixWithMedia) preferences.mixVolume.fraction else 1f
    fun requestsFocus(preferences: SpeechPreferences): Boolean = !preferences.mixWithMedia
}
