package dev.ipf.whitenoise.model

import dev.ipf.whitenoise.ui.components.BackgroundKeyboardDismissal
import org.junit.Assert.*
import org.junit.Test

class BackgroundKeyboardDismissalTest {
    @Test fun focusIsReleasedOnlyForTheCurrentDismissal() {
        val dismissal = BackgroundKeyboardDismissal()
        val request = dismissal.begin()
        assertTrue(dismissal.requested)
        assertTrue(dismissal.finish(request))
        assertFalse(dismissal.requested)
        assertFalse(dismissal.finish(request))
    }

    @Test fun retappingTheEditorInvalidatesThePendingFocusClear() {
        val dismissal = BackgroundKeyboardDismissal()
        val oldRequest = dismissal.begin()
        dismissal.cancel()
        assertFalse(dismissal.finish(oldRequest))
        assertFalse(dismissal.requested)
    }

    @Test fun lateCompletionCannotFinishANewerDismissal() {
        val dismissal = BackgroundKeyboardDismissal()
        val oldRequest = dismissal.begin()
        dismissal.cancel()
        val newRequest = dismissal.begin()
        assertFalse(dismissal.finish(oldRequest))
        assertTrue(dismissal.requested)
        assertTrue(dismissal.finish(newRequest))
    }
}
