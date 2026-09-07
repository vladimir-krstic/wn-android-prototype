package dev.ipf.whitenoise.ui.conversation

import dev.ipf.whitenoise.model.*
import org.junit.Assert.*
import org.junit.Test

class WritingToolsControllerTest {
    private val draft = WritingDraft("me", "chat", "teh note", 8, 8)
    private fun controller(scenario: WritingScenario = WritingScenario.Ready) = WritingToolsController().apply {
        observe(draft); open(false, scenario); choose(WritingOperation.Proofread)
    }
    @Test fun cancellationAndNewRequestRejectLateCompletion() {
        val controller = controller()
        val oldId = controller.session!!.id
        controller.close(); controller.complete(oldId)
        assertNull(controller.session)
        controller.open(false, WritingScenario.Ready)
        controller.complete(oldId)
        assertEquals(WritingPhase.Choose, controller.session!!.phase)
    }
    @Test fun editAndUndoInvalidatesTheOriginalRequest() {
        val controller = controller()
        val id = controller.session!!.id
        controller.observe(draft.copy(text = "new text"))
        controller.observe(draft)
        controller.complete(id)
        assertEquals(WritingPhase.Stale, controller.session!!.phase)
        assertNull(controller.apply())
    }
    @Test fun applyCanOnlyHappenOnce() {
        val controller = controller()
        controller.complete(controller.session!!.id)
        assertEquals("the note", controller.apply()!!.text)
        assertNull(controller.apply())
    }
    @Test fun localFailureRetriesWithoutExternalConsentOrFallback() {
        val controller = controller(WritingScenario.Unavailable)
        controller.complete(controller.session!!.id)
        assertEquals(WritingPhase.Unavailable, controller.session!!.phase)
        controller.proceed()
        assertEquals(WritingScenario.Ready, controller.session!!.scenario)
        controller.complete(controller.session!!.id)
        assertEquals("the note", controller.session!!.suggestion)
    }
}
