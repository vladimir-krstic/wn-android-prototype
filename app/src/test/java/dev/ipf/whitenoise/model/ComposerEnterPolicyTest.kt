package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class ComposerEnterPolicyTest {
    private fun decision(preference: EnterKeyBehavior = EnterKeyBehavior.SendMessage,
        enter: Boolean = true, down: Boolean = true, shift: Boolean = false, modified: Boolean = false,
        repeat: Boolean = false, composing: Boolean = false, enabled: Boolean = true, sendable: Boolean = true) =
        ComposerEnterPolicy.decide(preference,enter,down,shift,modified,repeat,composing,enabled,sendable)
    @Test fun bareEnterSendsOnlyWhenOptedIn() {
        assertEquals(ComposerEnterDecision.Send,decision())
        assertEquals(ComposerEnterDecision.Native,decision(preference = EnterKeyBehavior.NewLine))
        assertEquals(ComposerEnterDecision.Native,decision(enter = false))
    }
    @Test fun shiftAndOtherShortcutsRemainNative() {
        assertEquals(ComposerEnterDecision.Native,decision(shift = true))
        assertEquals(ComposerEnterDecision.Native,decision(modified = true))
    }
    @Test fun activeCompositionMustFinishBeforeAnotherPressCanSend() {
        assertEquals(ComposerEnterDecision.Native,decision(composing = true))
        assertEquals(ComposerEnterDecision.Consume,decision(down = false))
        assertEquals(ComposerEnterDecision.Send,decision())
    }
    @Test fun heldEnterAndReleaseCannotSendAgain() {
        assertEquals(ComposerEnterDecision.Send,decision())
        repeat(4) { assertEquals(ComposerEnterDecision.Consume,decision(repeat = true)) }
        assertEquals(ComposerEnterDecision.Consume,decision(down = false))
    }
    @Test fun disabledOrEmptyDraftCannotSendOrInjectNewlinesInSendMode() {
        assertEquals(ComposerEnterDecision.Consume,decision(enabled = false))
        assertEquals(ComposerEnterDecision.Consume,decision(sendable = false))
        assertEquals(ComposerEnterDecision.Native,decision(preference = EnterKeyBehavior.NewLine,sendable = false))
    }
}
