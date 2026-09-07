package dev.ipf.whitenoise.ui.conversation

import dev.ipf.whitenoise.model.*
import org.junit.Assert.*
import org.junit.Test

class TranslationSpeechTest {
    private val message = ChatMessage("one", "them", 1, "Today", 600, "10:00 AM", "Hola.")
    private val chat = Chat("chat", 0, ChatKind.Direct("them"), "Chat", timeline = listOf(ChatTimelineEntry.Message(message)))
    private var active = Profile("me", "Me", "key", chats = listOf(chat))
    @Test fun speaksDisplayedTranslationAndKeepsReturnBoundToOriginal() {
        val spoken = mutableListOf<String>()
        val controller = ReadAloudController().apply { profile = { active }; attachTestOutput({ text, _ -> spoken += text; true }) }
        controller.startDisplayedMessage(active, chat, message.copy(text = "Hello."))
        assertEquals(listOf("Hello."), spoken)
        assertEquals("Hola.", controller.session!!.returnTarget!!.message.originalAuthored)
        active = active.copy(chats = listOf(chat.copy(timeline = listOf(ChatTimelineEntry.Message(message.copy(text = "Edited"))))))
        controller.reconcile()
        assertEquals(SpeechPhase.Unavailable, controller.session!!.phase)
    }
    @Test fun staleRenderedTranslationCannotBindToNewerSource() {
        val old = active
        active = active.copy(chats = listOf(chat.copy(timeline = listOf(ChatTimelineEntry.Message(message.copy(text = "Edited"))))))
        val spoken = mutableListOf<String>()
        val controller = ReadAloudController().apply { profile = { active }; attachTestOutput({ text, _ -> spoken += text; true }) }
        controller.startDisplayedMessage(old, chat, message.copy(text = "Hello."))
        assertTrue(spoken.isEmpty()); assertNull(controller.session)
    }
}
