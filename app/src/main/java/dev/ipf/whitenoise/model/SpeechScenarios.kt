package dev.ipf.whitenoise.model

/** These catalogs are selected only through the active profile's Developer Tools. */
enum class SpeechCatalogScenario(val developerLabel: String) {
    Device("Device engines"), Checking("Checking engines"), Empty("No engines"), Failed("Discovery failure"),
    Voices("Offline and unavailable voices"), MissingVoice("Missing saved voice"), SelectionFailure("Engine selection failure")
}
object SpeechCatalogExamples {
    const val primary = "example.offline.speech"
    const val secondary = "example.alternative.speech"
    val engines = listOf(SpeechEngineOption(primary, "Offline Speech", SpeechEngineTrust.Unknown),
        SpeechEngineOption(secondary, "Alternative Speech", SpeechEngineTrust.Unknown))
    fun voices(locale: String) = listOf(SpeechVoice("Clear", locale, quality = 400), SpeechVoice("Calm", locale, quality = 300),
        SpeechVoice("Download required", locale, installed = false), SpeechVoice("Online voice", locale, networkRequired = true),
        SpeechVoice("Duplicate", locale), SpeechVoice("Duplicate", locale, quality = 100))
    fun discovery(scenario: SpeechCatalogScenario, locale: String, preferences: SpeechPreferences, engine: String = primary): SpeechDiscovery {
        val resolution = SpeechVoices.resolve(engine, locale, voices(locale), preferences.voice(engine))
        return when (scenario) {
            SpeechCatalogScenario.Checking -> SpeechDiscovery(phase = SpeechDiscoveryPhase.Discovering)
            SpeechCatalogScenario.Empty -> SpeechDiscovery(phase = SpeechDiscoveryPhase.Empty)
            SpeechCatalogScenario.Failed -> SpeechDiscovery(phase = SpeechDiscoveryPhase.Failed)
            else -> SpeechDiscovery(phase = SpeechDiscoveryPhase.Ready, engines = engines, systemDefault = primary,
                activePackage = engine, voices = resolution.applied(resolution.candidates.firstOrNull()))
        }
    }
}
