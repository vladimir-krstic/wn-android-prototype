package dev.ipf.whitenoise.ui.conversation

import dev.ipf.whitenoise.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SpeechPreferenceControllerTest {
    private var active = Profile("p", "Name", "public", developerTools = DeveloperToolsState(isEnabled = true),
        chats = listOf(Chat("c", 0, ChatKind.Direct("other"), "Chat", timeline = listOf(
            ChatTimelineEntry.Message(ChatMessage("m", "other", 1, "Today", 1, "Now", "First sentence. Second sentence."))))))
    private fun catalog() = SpeechCatalogExamples.discovery(SpeechCatalogScenario.Voices, Locale.getDefault().toLanguageTag(), active.settings.speech)
    private fun controller(output: (String, SpeechToken) -> Boolean = { _, _ -> true }) = ReadAloudController().apply {
        profile = { active }; attachTestOutput(output, catalog())
        updatePreferences = { id, reduce -> if (active.id == id) active = active.copy(settings = active.settings.copy(speech = reduce(active.settings.speech))) }
    }
    private fun start(c: ReadAloudController) = c.startConversation(active, active.chats.single(), "m")
    @Test fun unknownEngineReceivesNoTextBeforeConsentAndCancelStartsNothing() {
        var requests = 0; val c = controller { _, _ -> requests++; true }; start(c)
        assertNotNull(c.pendingConsent); assertEquals(0, requests); assertNull(c.session)
        val id = c.pendingConsent!!.id; c.cancelConsent(); c.confirmConsent(id)
        assertNull(c.session); assertEquals(0, requests); assertTrue(active.settings.speech.acknowledgedEngines.isEmpty())
    }
    @Test fun acceptanceIsProfileScopedAndSubsequentStartsDoNotReprompt() {
        var requests = 0; val c = controller { _, _ -> requests++; true }; start(c)
        c.confirmConsent(c.pendingConsent!!.id)
        assertEquals(1, requests); assertTrue(SpeechCatalogExamples.primary in active.settings.speech.acknowledgedEngines)
        start(c); assertNull(c.pendingConsent); assertEquals(2, requests)
        active = active.copy(id = "other", settings = ProfileSettings()); c.reconcile(); start(c)
        assertNotNull(c.pendingConsent); assertEquals(2, requests)
    }
    @Test fun backgroundProfileChangeAndEditedSourceInvalidatePendingUse() {
        var requests = 0; val c = controller { _, _ -> requests++; true }; start(c); val first = c.pendingConsent!!.id
        c.background(); c.foreground(); c.confirmConsent(first); assertEquals(0, requests)
        start(c); val second = c.pendingConsent!!.id
        active = active.copy(chats = listOf(active.chats.single().copy(timeline = emptyList())))
        c.confirmConsent(second); assertEquals(0, requests); assertNull(c.session)
    }
    @Test fun selectionCancelKeepsPriorEngineAndSavedVoiceThenConfirmationAdoptsAtomically() {
        val c = controller(); val before = c.discovery
        c.selectEngine(SpeechCatalogExamples.secondary); c.cancelConsent()
        assertEquals(before, c.discovery); assertNull(active.settings.speech.enginePackage)
        c.selectEngine(SpeechCatalogExamples.secondary); c.confirmConsent(c.pendingConsent!!.id)
        assertEquals(SpeechCatalogExamples.secondary, c.discovery.activePackage)
        assertEquals(SpeechCatalogExamples.secondary, active.settings.speech.enginePackage)
        assertEquals(SpeechEngineChangePhase.Applied, c.engineChange!!.phase)
    }
    @Test fun failedSelectionRetainsPriorEngineAndPreferences() {
        val c = controller(); c.chooseCatalogScenario(SpeechCatalogScenario.SelectionFailure)
        val before = c.discovery; c.selectEngine(SpeechCatalogExamples.secondary)
        c.confirmConsent(c.pendingConsent!!.id)
        assertEquals(SpeechEngineChangePhase.Failed, c.engineChange!!.phase)
        assertEquals(before, c.discovery); assertNull(active.settings.speech.enginePackage)
    }
    @Test fun disabledVoiceCannotBecomeASelectionRequest() {
        val c = controller(); val cloud = c.discovery.voices.options.first { it.unavailable == SpeechVoiceUnavailable.RequiresNetwork }
        c.selectEngine(SpeechCatalogExamples.primary, cloud.key, changingVoice = true)
        assertNull(c.engineChange); assertNull(c.pendingConsent)
    }
    @Test fun automaticArrivalCannotReplacePendingManualConsent() {
        val c = controller(); active = active.copy(settings = active.settings.copy(speech = active.settings.speech.copy(autoReadDefault = true)))
        c.openChat(active.id, active.chats.single(), setOf("m")); start(c)
        val consent = c.pendingConsent
        c.observeChat(active.chats.single()); assertEquals(consent, c.pendingConsent)
        c.confirmConsent(consent!!.id); assertNotNull(c.session)
    }
    @Test fun mediaRefusalDoesNotReplaceExistingQueueAndFocusLossPauses() {
        val c = controller(); start(c); c.confirmConsent(c.pendingConsent!!.id); val old = c.session!!.id
        c.changePreferences { it.copy(mixWithMedia = true) }; c.chooseAudioScenario(SpeechAudioEnvironment(mediaActive = false)); start(c)
        assertEquals(SpeechStartRefusal.MediaNotActive, c.startRefusal); assertEquals(old, c.session!!.id)
        c.changePreferences { it.copy(mixWithMedia = false) }; c.chooseAudioScenario(SpeechAudioEnvironment(focusAvailable = false)); c.pause(); c.resume()
        assertEquals(old, c.session!!.id); assertEquals(SpeechPhase.EngineError, c.session!!.phase)
        assertEquals(SpeechStartRefusal.FocusUnavailable, c.startRefusal)
    }
}
