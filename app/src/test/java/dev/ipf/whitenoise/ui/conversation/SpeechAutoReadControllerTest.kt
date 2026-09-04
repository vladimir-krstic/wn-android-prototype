package dev.ipf.whitenoise.ui.conversation

import dev.ipf.whitenoise.model.*
import org.junit.Assert.*
import org.junit.Test

class SpeechAutoReadControllerTest {
    private fun message(id: String, minute: Int, body: String = "$id sentence.") = ChatTimelineEntry.Message(ChatMessage(id, "other", 1, "Today", minute, "Now", body))
    private fun chat(vararg entries: ChatTimelineEntry) = Chat("c", 0, ChatKind.Direct("other"), "Chat", timeline = entries.toList())
    private var active = Profile("p", "Name", "public", settings = ProfileSettings(speech = SpeechPreferences(autoReadDefault = true)),
        chats = listOf(chat(message("m0", 0), message("m1", 1), message("m2", 2))))
    private fun controller(output: (String, SpeechToken) -> Boolean = { _, _ -> true }) = ReadAloudController().apply {
        attachTestOutput(output); profile = { active }
        updatePreferences = { id, reduce -> if (id == active.id) active = active.copy(settings = active.settings.copy(speech = reduce(active.settings.speech))) }
    }
    private fun open(c: ReadAloudController, unread: Set<String> = setOf("m1", "m2")) {
        c.openChat(active.id, active.chats.single(), unread); c.observeChat(active.chats.single())
    }
    private fun add(vararg entries: ChatTimelineEntry) {
        active = active.copy(chats = listOf(active.chats.single().copy(timeline = active.chats.single().timeline + entries)))
    }
    @Test fun capturedBacklogStartsAtUnreadAndSnapshotsDoNotReplay() {
        var requests = 0; val c = controller { _, _ -> requests++; true }; open(c)
        assertEquals(listOf("m1", "m2"), c.session!!.catalog.map { it.id })
        repeat(3) { c.observeChat(active.chats.single()) }
        assertEquals(1, requests)
    }
    @Test fun arrivalAppendsAtLogicalTailWithoutRestartingCurrentUtterance() {
        val c = controller(); open(c); val before = c.session!!
        add(message("history", -1), message("m3", 3)); c.observeChat(active.chats.single())
        assertEquals(listOf("m1", "m2", "m3"), c.session!!.catalog.map { it.id })
        assertEquals(before.token, c.session!!.token)
        assertEquals("m1", c.activeMessageId)
    }
    @Test fun pausedArrivalStaysPausedAndPreparedWindowRemainsBounded() {
        val c = controller(); open(c); c.pause()
        add(*(3..30).map { message("m$it", it) }.toTypedArray()); c.observeChat(active.chats.single())
        assertEquals(SpeechPhase.Paused, c.session!!.phase); assertTrue(c.session!!.window.size <= 8)
        assertEquals("m30", c.session!!.catalog.last().id)
    }
    @Test fun successfulManualStartEndsAutomaticOwnershipAndDisableKeepsManualSession() {
        val c = controller(); open(c); c.startConversation(active, active.chats.single(), "m0")
        val id = c.session!!.id
        c.changePreferences { it.copy(autoReadDefault = false) }; c.reconcile()
        assertEquals(id, c.session!!.id)
        add(message("m3", 3)); c.observeChat(active.chats.single()); assertEquals(id, c.session!!.id)
    }
    @Test fun failedManualStartRetainsPausedAutomaticQueueAndOwnership() {
        var succeed = true; val c = controller { _, _ -> succeed }; open(c); val old = c.session!!
        succeed = false; c.startConversation(active, active.chats.single(), "m0")
        assertEquals(old.id, c.session!!.id); assertEquals(SpeechPhase.Paused, c.session!!.phase)
        c.changePreferences { it.copy(autoReadDefault = false) }; c.reconcile(); assertNull(c.session)
    }
    @Test fun effectiveOffStopsOnlyAutomaticQueueAndOverrideCanKeepItEnabled() {
        val c = controller(); open(c); val old = c.session!!.id
        c.changePreferences { it.withAutoRead("c", SpeechAutoReadOverride.On).copy(autoReadDefault = false) }; c.reconcile()
        assertEquals(old, c.session!!.id)
        c.changePreferences { it.withAutoRead("c", SpeechAutoReadOverride.Off) }; c.reconcile(); assertNull(c.session)
    }
    @Test fun foregroundReturnReadsOnlyNewArrivals() {
        val c = controller(); open(c); c.background(); assertNull(c.session)
        add(message("m3", 3)); c.observeChat(active.chats.single()); assertNull(c.session)
        c.foreground(); c.observeChat(active.chats.single())
        assertEquals(listOf("m3"), c.session!!.catalog.map { it.id })
    }
    @Test fun explicitStopDoesNotRestartOnNewSnapshotOrForegroundReturn() {
        val c = controller(); open(c); c.stop(); c.background(); add(message("m3", 3))
        c.foreground(); c.observeChat(active.chats.single()); assertNull(c.session)
    }
    @Test fun differentProfileCannotResumeCapturedQueue() {
        val c = controller(); open(c); c.background(); active = active.copy(id = "other")
        c.foreground(); c.observeChat(active.chats.single()); assertNull(c.session)
    }
    @Test fun anotherChatCannotAppendIntoExistingAutomaticQueue() {
        val c = controller(); open(c); val old = c.session!!
        val other = chat(message("other0", 0)).copy(id = "other")
        active = active.copy(chats = active.chats + other)
        c.openChat(active.id, other, setOf("other0")); c.observeChat(other)
        assertEquals(old, c.session)
    }
    @Test fun manualSourceNavigationCannotReopenAutomaticBacklog() {
        val c = controller(); c.startConversation(active, active.chats.single(), "m0"); open(c)
        while (c.session!!.phase == SpeechPhase.Speaking) c.done(c.session!!.token!!)
        c.observeChat(active.chats.single()); assertEquals(SpeechPhase.Completed, c.session!!.phase)
    }
}
