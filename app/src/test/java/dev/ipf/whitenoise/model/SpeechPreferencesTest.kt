package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class SpeechPreferencesTest {
    private val engine = "engine"
    private fun key(name: String, locale: String = "en-US") = SpeechVoiceKey(engine, name, locale)
    private val voices = listOf(SpeechVoice("low", "en-US", quality = 1), SpeechVoice("high", "en-GB", quality = 5))
    private fun resolution(selected: SpeechVoiceKey? = null) = SpeechVoices.resolve(engine, "en-US", voices, selected)
    private fun discovery() = SpeechDiscovery(1, SpeechDiscoveryPhase.Ready, listOf(SpeechEngineOption(engine, "Engine", SpeechEngineTrust.Unknown)),
        activePackage = engine, voices = resolution().applied(key("high", "en-GB")))

    @Test fun savedVoiceWinsAndFallbackOrderIsDeterministicAcrossRegions() {
        val resolved = resolution(key("low"))
        assertEquals(listOf(key("low"), key("high", "en-GB")), resolved.candidates)
        assertNull(resolved.effective)
        assertFalse(resolved.applied(key("low")).usingFallback)
        assertTrue(resolved.applied(key("high", "en-GB")).usingFallback)
    }
    @Test fun missingSavedVoiceRemainsRequestedAndOnlySuccessfulFallbackBecomesEffective() {
        val saved = key("missing"); val resolved = resolution(saved)
        assertEquals(saved, resolved.requested); assertNull(resolved.effective)
        assertNull(resolved.applied(saved).effective)
        val fallback = resolved.applied(resolved.candidates.first())
        assertEquals(saved, fallback.requested); assertTrue(fallback.usingFallback)
    }
    @Test fun unavailableVoiceRowsExplainEveryNonSelectableCase() {
        val resolved = SpeechVoices.resolve(engine, "en", listOf(
            SpeechVoice("", "en"), SpeechVoice("download", "en", installed = false),
            SpeechVoice("cloud", "en", networkRequired = true), SpeechVoice("duplicate", "en"), SpeechVoice("duplicate", "en"),
            SpeechVoice("other language", "de"), SpeechVoice("ready", "en")), null)
        assertEquals(6, resolved.options.size)
        assertEquals(setOf(SpeechVoiceUnavailable.InvalidIdentity, SpeechVoiceUnavailable.NotInstalled, SpeechVoiceUnavailable.RequiresNetwork, SpeechVoiceUnavailable.Ambiguous), resolved.options.mapNotNull { it.unavailable }.toSet())
        assertEquals(listOf(key("ready", "en")), resolved.candidates)
        assertNull(resolved.applied(key("cloud", "en")).effective)
    }
    @Test fun duplicateIdentityCannotBecomeFallbackEvenIfOneCopyIsOffline() {
        val resolved = SpeechVoices.resolve(engine, "en", listOf(SpeechVoice("same", "en"), SpeechVoice("same", "en", networkRequired = true)), key("same", "en"))
        assertTrue(resolved.candidates.isEmpty()); assertNull(resolved.applied(key("same", "en")).effective)
    }
    @Test fun languageMatchingHandlesIsoAliasesWithoutAcceptingMalformedOrUnrelatedLocales() {
        assertTrue(SpeechVoices.sameLanguage("eng-US", "en-GB")); assertFalse(SpeechVoices.sameLanguage("", "en"))
        assertFalse(SpeechVoices.sameLanguage("de", "en")); assertFalse(SpeechVoices.sameLanguage("und", "en"))
    }
    @Test fun perEngineVoiceChoicesNeverOverwriteEachOtherOrAcceptForeignKeys() {
        val selected = SpeechPreferences().withVoice(engine, key("a"))
        val second = SpeechVoiceKey("second", "b", "en")
        val both = selected.withVoice("second", second)
        assertEquals(key("a"), both.voice(engine)); assertEquals(second, both.voice("second"))
        assertEquals(both, both.withVoice(engine, second))
        assertEquals(second, both.withVoice(engine, null).voice("second"))
        assertNull(SpeechPreferences(voices = mapOf(engine to second)).voice(engine))
    }
    @Test fun requestedPackageAndKnownLabelNeverProveRuntimeEngineTrust() {
        val local = "com.reecedunn.espeak"
        assertEquals(SpeechEngineTrust.OnDevice, SpeechEngines.declaredTrust(local))
        assertEquals(SpeechEngineTrust.Unknown, SpeechEngines.runtimeTrust(local, null))
        assertEquals(SpeechEngineTrust.Unknown, SpeechEngines.runtimeTrust(local, "other"))
        assertEquals(SpeechEngineTrust.OnDevice, SpeechEngines.runtimeTrust(local, local))
        assertEquals(SpeechEngineTrust.Unknown, SpeechEngines.declaredTrust("com.reecedunn.espeak.fake"))
    }
    @Test fun consentIsEngineScopedAndIndependentOfOfflineVoiceEligibility() {
        val fresh = SpeechPreferences(); assertTrue(fresh.needsConsent(engine, SpeechEngineTrust.Unknown))
        val acknowledged = fresh.acknowledge(engine)
        assertFalse(acknowledged.needsConsent(engine, SpeechEngineTrust.Unknown))
        assertTrue(acknowledged.needsConsent("other", SpeechEngineTrust.Unknown))
        assertTrue(fresh.needsConsent(engine, SpeechEngineTrust.Unknown))
        assertFalse(fresh.needsConsent(engine, SpeechEngineTrust.OnDevice))
    }
    @Test fun explicitInstalledEngineWinsAndMultipleUnknownDefaultsNeedAChoice() {
        val first = SpeechEngineOption(engine, "Engine", SpeechEngineTrust.Unknown)
        val second = first.copy(packageName = "second")
        assertNull(SpeechEngines.preferred(listOf(first, second), engine, null))
        assertEquals("second", SpeechEngines.preferred(listOf(first, second), engine, "second"))
        assertEquals(engine, SpeechEngines.preferred(listOf(first), engine, "removed"))
        val local = first.copy(trust = SpeechEngineTrust.OnDevice)
        assertEquals(engine, SpeechEngines.preferred(listOf(local, second), engine, null))
    }
    @Test fun staleProfileRevisionCancellationAndWrongNativePackageCannotAdoptASelection() {
        val change = SpeechEngineChange(1, "p", engine)
        assertEquals(change, change.settle("other", 0, discovery()))
        assertEquals(change, change.settle("p", 1, discovery()))
        val cancelled = change.cancel(); assertEquals(cancelled, cancelled.settle("p", cancelled.revision, discovery()))
        assertEquals(SpeechEngineChangePhase.Failed, change.settle("p", 0, discovery().copy(activePackage = "wrong")).phase)
        assertEquals(SpeechEngineChangePhase.Applied, change.settle("p", 0, discovery()).phase)
    }
    @Test fun consentAndFailureRetryEachInvalidatePriorCompletion() {
        val consent = SpeechEngineChange(1, "p", engine, phase = SpeechEngineChangePhase.Consent)
        assertEquals(consent, consent.settle("p", 0, discovery()))
        val loading = consent.consent(); val failed = loading.settle("p", loading.revision, SpeechDiscovery(phase = SpeechDiscoveryPhase.Failed))
        val retry = failed.retry()
        assertEquals(retry, retry.settle("p", loading.revision, discovery()))
        assertEquals(SpeechEngineChangePhase.Applied, retry.settle("p", retry.revision, discovery()).phase)
    }
    @Test fun aForeignVoiceOrUnlistedEngineNeverCountsAsUsable() {
        val available = discovery(); assertTrue(available.usable)
        assertFalse(available.copy(engines = emptyList()).usable)
        assertFalse(available.copy(voices = available.voices.copy(effective = key("unlisted"))).usable)
        assertFalse(available.copy(phase = SpeechDiscoveryPhase.Discovering).usable)
    }
    @Test fun ratesPreservePresetsRoundCustomValuesAndAcceptLocaleDecimals() {
        SpeechRates.presets.forEach { assertEquals(it, SpeechRates.normalize(it)) }
        assertEquals(1.3f, SpeechRates.normalize(1.25f + 0.01f)!!, 0f)
        assertEquals(1.2f, SpeechRates.normalize(1.24f)!!, 0f)
        assertEquals(0.75f, SpeechRates.parse("0,75")!!, 0f)
        assertEquals(1.5f, SpeechRates.parse("١٫٥")!!, 0f)
        assertEquals(0.1f, SpeechRates.parse("0.1")!!, 0f); assertEquals(10f, SpeechRates.parse("10.0")!!, 0f)
    }
    @Test fun malformedOrOutOfRangeRateNeverReplacesSavedPreference() {
        val saved = SpeechPreferences(rate = 1.5f)
        listOf(Float.NaN, Float.POSITIVE_INFINITY, -1f, 0.09f, 10.01f).forEach { assertEquals(saved, saved.withRate(it)) }
        listOf("", "1e1", "NaN", "1.2.3", "1,2,3", "10.01", "-1", "0.09", "1x").forEach { assertNull(it, SpeechRates.parse(it)) }
        assertNull(saved.withRate(null).rate)
        assertEquals(1.7f, SpeechRates.resolved(null, 170f), 0f)
        assertEquals(1f, SpeechRates.resolved(null, Float.NaN), 0f)
    }
    @Test fun mediaMixRequiresPlayingAudioKeepsVolumeAndDoesNotRequestFocus() {
        val base = SpeechPreferences(); val mix = base.copy(mixWithMedia = true, mixVolume = SpeechMixVolume.Quiet)
        assertEquals(SpeechStartRefusal.MediaNotActive, SpeechAudioPolicy.refusal(mix, SpeechAudioEnvironment()))
        assertNull(SpeechAudioPolicy.refusal(mix, SpeechAudioEnvironment(mediaActive = true, focusAvailable = false)))
        assertFalse(SpeechAudioPolicy.requestsFocus(mix)); assertEquals(0.35f, SpeechAudioPolicy.volume(mix), 0f)
        assertEquals(SpeechStartRefusal.FocusUnavailable, SpeechAudioPolicy.refusal(base, SpeechAudioEnvironment(focusAvailable = false)))
        assertEquals(SpeechMixVolume.Quiet, mix.copy(mixWithMedia = false).mixVolume)
        assertEquals(1f, SpeechAudioPolicy.volume(base), 0f)
    }
    @Test fun automaticReadingDefaultsAndOverridesRemainIndependentPerChat() {
        val base = SpeechPreferences(); assertFalse(base.autoRead("a"))
        val selected = base.withAutoRead("a", SpeechAutoReadOverride.On).withAutoRead("b", SpeechAutoReadOverride.Off).copy(autoReadDefault = true)
        assertTrue(selected.autoRead("a")); assertFalse(selected.autoRead("b")); assertTrue(selected.autoRead("c"))
        assertTrue(selected.withAutoRead("b", null).autoRead("b")); assertFalse(base.autoRead("b"))
    }
}
