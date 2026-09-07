package dev.ipf.whitenoise.model

import dev.ipf.whitenoise.ui.conversation.TranslationController
import org.junit.Assert.*
import org.junit.Test

class MessageTranslationTest {
    private fun message(id: String = "one", text: String = "Hola.", author: String = "them") = ChatMessage(id, author, 1, "Today", 600, "10:00 AM", text)
    private fun controller(vararg messages: ChatMessage, preferences: TranslationPreferences = TranslationPreferences()) =
        TranslationController("me", "chat").apply { observe(messages.toList(), preferences) }
    private fun TranslationController.finish(id: String = "one") = complete(id, entries.getValue(id).generation)

    @Test fun sameLanguageIsBlockedBeforeManualOrAutomaticWork() {
        val source = message(); val c = controller(source)
        c.request(source.id, TranslationLanguage.Spanish)
        assertTrue(c.entries.isEmpty())
        var requests = 0
        c.observe(listOf(source), TranslationPreferences(automatic = true, target = TranslationLanguage.Spanish), nextScenario = { requests++; TranslationScenario.Ready })
        assertTrue(c.entries.isEmpty()); assertEquals(0, requests)
    }
    @Test fun selectingExistingTranslationRestoresItWithoutAnotherRequest() {
        val source = message(); val c = controller(source); c.request(source.id, TranslationLanguage.English); c.finish()
        val generation = c.entries.getValue(source.id).generation
        c.toggle(source.id); assertEquals(source.text, c.text(source))
        c.request(source.id, TranslationLanguage.English)
        assertEquals("Hello.", c.text(source)); assertEquals(generation, c.entries.getValue(source.id).generation)
    }
    @Test fun captionPreviewDetectionAndTranslationUseTheOriginalLanguage() {
        val source = message(text = "This is the entrance."); val c = controller(source)
        assertEquals(TranslationLanguage.English, TranslationExamples.detectedLanguage(source.text))
        c.request(source.id, TranslationLanguage.Serbian); c.finish()
        assertEquals("Ovo je ulaz.", c.text(source))
        assertEquals(TranslationLanguage.English, c.entries.getValue(source.id).source)
    }
    @Test fun voiceTranscriptsAreNotTreatedAsAuthoredMessageText() {
        val voice = message().copy(attachments = listOf(MessageAttachment("voice", MessageAttachmentKind.Voice, "Voice message")))
        assertFalse(TranslationExamples.eligible(voice))
        assertFalse(MessageAction.Translate in MessageActionPolicy.available(voice, "me"))
    }
    @Test fun manualTranslationKeepsOriginalAndCanToggleInstantly() {
        val source = message(); val c = controller(source)
        c.request(source.id, TranslationLanguage.English); assertEquals(source.text, c.text(source))
        c.finish(); assertEquals("Hello.", c.text(source)); assertEquals("Hola.", source.text)
        c.toggle(source.id); assertEquals(source.text, c.text(source))
        c.toggle(source.id); assertEquals("Hello.", c.text(source))
    }
    @Test fun inheritanceAndLanguageOverridesResolveWithoutChangingAnotherChat() {
        val prefs = TranslationPreferences(automatic = true, target = TranslationLanguage.German,
            chats = mapOf("off" to ChatTranslationPreference(TranslationOverride.Off), "on" to ChatTranslationPreference(TranslationOverride.On, TranslationLanguage.French)))
        assertTrue(prefs.automatic("default")); assertFalse(prefs.automatic("off")); assertTrue(prefs.automatic("on"))
        assertEquals(TranslationLanguage.French, prefs.target("on")); assertEquals(TranslationLanguage.German, prefs.target("default"))
        assertFalse(TranslationPreferences().automatic("chat"))
    }
    @Test fun autoSkipsOwnDeletedAndExpiredMessagesAndDeduplicates() {
        val input = listOf(message(), message("own", author = "me"), message("deleted").copy(deletionState = MessageDeletionState.DeletedByOther), message("expired").copy(expiresAtMillis = 1))
        val prefs = TranslationPreferences(automatic = true); val c = TranslationController("me", "chat")
        c.observe(input, prefs); val entries = c.entries
        c.observe(input, prefs); assertEquals(entries, c.entries); assertEquals(setOf("one"), c.entries.keys)
    }
    @Test fun autoIsBoundedAndMovesItsWindowToVisibleMessages() {
        val input = (1..100).map { message(it.toString()) }; val prefs = TranslationPreferences(automatic = true)
        val c = TranslationController("me", "chat"); c.observe(input, prefs, setOf("1"))
        assertEquals(40, c.entries.size); assertTrue("1" in c.entries)
        c.observe(input, prefs, setOf("2")); assertTrue("2" in c.entries); assertEquals(40, c.entries.size)
    }
    @Test fun editsAndUndoCannotReviveOldCallbacks() {
        val source = message(); val c = controller(source); c.request(source.id, TranslationLanguage.English)
        val old = c.entries.getValue(source.id).generation
        c.observe(listOf(source.copy(text = "Hasta mañana.")), TranslationPreferences())
        c.observe(listOf(source), TranslationPreferences()); c.request(source.id, TranslationLanguage.English)
        c.complete(source.id, old); assertEquals(TranslationPhase.Loading, c.entries.getValue(source.id).phase)
        c.finish(); assertEquals("Hello.", c.text(source))
    }
    @Test fun leavingWindowCancelsWorkAndOriginalRemainsAvailable() {
        val source = message(); val c = controller(source); c.request(source.id, TranslationLanguage.English)
        val old = c.entries.getValue(source.id).generation
        c.observe(emptyList(), TranslationPreferences()); c.complete(source.id, old)
        assertTrue(c.entries.isEmpty()); assertEquals(source.text, c.text(source))
    }
    @Test fun cancelAndTargetChangeRejectLateCompletion() {
        val source = message(); val c = controller(source); c.request(source.id, TranslationLanguage.English)
        val old = c.entries.getValue(source.id).generation; c.cancel(source.id); c.complete(source.id, old)
        assertEquals(source.text, c.text(source)); c.request(source.id, TranslationLanguage.German)
        c.complete(source.id, old); assertEquals(TranslationPhase.Loading, c.entries.getValue(source.id).phase)
        c.finish(); assertEquals("Hallo.", c.text(source))
    }
    @Test fun backgroundClearsRenditionsAndResumesOnlyAutomaticWork() {
        val source = message(); val prefs = TranslationPreferences(automatic = true); val c = controller(source, preferences = prefs)
        val old = c.entries.getValue(source.id).generation; c.pause(); c.complete(source.id, old)
        c.observe(listOf(source), prefs); assertTrue(c.entries.isEmpty())
        c.resume(); assertEquals(TranslationPhase.Loading, c.entries.getValue(source.id).phase)
        c.complete(source.id, old); assertEquals(TranslationPhase.Loading, c.entries.getValue(source.id).phase)
    }
    @Test fun automaticOffAndLanguageChangesClearRenditions() {
        val source = message(); val c = controller(source, preferences = TranslationPreferences(automatic = true))
        c.finish(); c.observe(listOf(source), TranslationPreferences()); assertEquals(source.text, c.text(source)); assertTrue(c.entries.isEmpty())
    }
    @Test fun lastManualLanguageDoesNotInvalidateTheRequest() {
        val source = message(); val c = controller(source); c.request(source.id, TranslationLanguage.French)
        c.observe(listOf(source), TranslationPreferences(lastManual = TranslationLanguage.French)); c.finish()
        assertEquals("Bonjour.", c.text(source))
    }
    @Test fun failuresNeverProduceTextAndRetryUsesSameLocalPath() {
        val source = message()
        listOf(TranslationScenario.Unavailable, TranslationScenario.Unsupported, TranslationScenario.Uncertain, TranslationScenario.Timeout, TranslationScenario.Error).forEach { scenario ->
            val c = controller(source); c.request(source.id, TranslationLanguage.English, scenario = scenario); c.finish()
            assertNull(c.entries.getValue(source.id).translated); assertEquals(source.text, c.text(source))
            c.retry(source.id); c.finish(); assertEquals("Hello.", c.text(source))
        }
    }
    @Test fun missingPackWaitsForExplicitDownloadAndCanBeCancelled() {
        val source = message(); val c = controller(source); c.request(source.id, TranslationLanguage.English, scenario = TranslationScenario.MissingPack)
        c.finish(); assertEquals(TranslationPhase.MissingPack, c.entries.getValue(source.id).phase)
        c.download(source.id); c.finish(); assertEquals(TranslationPhase.Loading, c.entries.getValue(source.id).phase)
        c.finish(); assertEquals("Hello.", c.text(source))
    }
    @Test fun profilesAndChatsCannotShareRenditions() {
        val source = message(); val c = controller(source); c.request(source.id, TranslationLanguage.English); c.finish()
        assertEquals(source.text, TranslationController("other", "chat").text(source))
        assertEquals(source.text, TranslationController("me", "other").text(source))
    }
    @Test fun markupQuotesLinksAndCodeArePreservedWithoutPartialTranslations() {
        val source = "> **Hola.**\n[Hasta mañana.](https://example.com)\n`Hola.`\n@Maya"
        val result = TranslationExamples.translate(source, TranslationLanguage.English)
        assertEquals("> **Hello.**\n[See you tomorrow.](https://example.com)\n`Hola.`\n@Maya", result.text)
        assertEquals(TranslationPhase.Unsupported, TranslationExamples.translate("Hola. Other unknown text", TranslationLanguage.English).phase)
        assertEquals(TranslationPhase.Uncertain, TranslationExamples.translate("Unknown input", TranslationLanguage.English).phase)
    }
    @Test fun rtlAndMultilineExamplesTranslateWithoutLosingLayout() {
        val result = TranslationExamples.translate("Hola.\nHasta mañana.", TranslationLanguage.Arabic)
        assertEquals("مرحبًا.\nأراك غدًا.", result.text)
        assertEquals("Hello.", TranslationExamples.translate("مرحبًا.", TranslationLanguage.English).text)
    }
    @Test fun translatedSpeechIsBoundToOriginalAndInvalidatedByEditOrDeletion() {
        val source = message(); val chat = Chat("chat", 0, ChatKind.Direct("them"), "Chat", timeline = listOf(ChatTimelineEntry.Message(source)))
        val profile = Profile("me", "Me", "key", chats = listOf(chat))
        val target = SpeechReturnTarget(1, SpeechOwner("me", "chat"), SpeechItem("one", "Hello.", originalAuthored = source.text))
        assertTrue(SpeechOwnership.owns(profile, target))
        val edited = profile.copy(chats = listOf(chat.copy(timeline = listOf(ChatTimelineEntry.Message(source.copy(text = "Edited"))))))
        assertFalse(SpeechOwnership.owns(edited, target))
        assertFalse(SpeechOwnership.owns(profile.copy(id = "other"), target))
    }
}
