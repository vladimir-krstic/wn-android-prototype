package dev.ipf.whitenoise.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ComposerKeyboardMotionTest {
    @Test fun openingUsesKeyboardProgressInsteadOfTheFallbackClock() {
        val motion = ComposerKeyboardMotion(false)
        motion.sample(true, 0, 0, 300, 0f)
        motion.sample(true, 0, 0, 300, 0f)
        assertEquals(0.2f, motion.sample(true, 60, 0, 300, 0.9f), 0.001f)
        assertEquals(0.7f, motion.sample(true, 210, 0, 300, 1f), 0.001f)
        assertEquals(1f, motion.sample(true, 300, 0, 300, 1f), 0.001f)
    }

    @Test fun clearingInsetsAfterCompletionDoesNotReturnToLaggingFallback() {
        val motion = ComposerKeyboardMotion(false)
        motion.sample(true, 0, 0, 300, 0f)
        motion.sample(true, 0, 0, 300, 0f)
        motion.sample(true, 240, 0, 300, 0.1f)
        assertEquals(1f, motion.sample(true, 300, 300, 300, 0.2f), 0.001f)
    }

    @Test fun closingUsesTheSameKeyboardCurveInReverse() {
        val motion = ComposerKeyboardMotion(true)
        motion.sample(false, 300, 300, 0, 1f)
        motion.sample(false, 300, 300, 0, 1f)
        assertEquals(0.6f, motion.sample(false, 180, 300, 0, 0f), 0.001f)
        assertEquals(0f, motion.sample(false, 0, 0, 0, 0.5f), 0.001f)
    }

    @Test fun lateKeyboardStartsAtThePresentedHeightWithoutSnappingBack() {
        val motion = ComposerKeyboardMotion(false)
        motion.sample(true, 0, 0, 0, 0f)
        motion.sample(true, 0, 0, 0, 0.3f)
        assertEquals(0.3f, motion.sample(true, 60, 0, 300, 0.4f), 0.001f)
        assertEquals(0.65f, motion.sample(true, 180, 0, 300, 0.5f), 0.001f)
    }

    @Test fun focusReversalStartsAtTheCurrentHeight() {
        val motion = ComposerKeyboardMotion(false)
        motion.sample(true, 0, 0, 300, 0f)
        motion.sample(true, 0, 0, 300, 0f)
        motion.sample(true, 120, 0, 300, 0f)
        assertEquals(0.4f, motion.sample(false, 120, 120, 0, 1f), 0.001f)
        motion.sample(false, 120, 120, 0, 0.4f)
        assertEquals(0.2f, motion.sample(false, 60, 120, 0, 0.4f), 0.001f)
    }

    @Test fun hardwareKeyboardUsesTheFallback() {
        val motion = ComposerKeyboardMotion(false)
        motion.sample(true, 0, 0, 0, 0f)
        assertEquals(0.5f, motion.sample(true, 0, 0, 0, 0.5f), 0.001f)
        assertEquals(1f, motion.sample(true, 0, 0, 0, 1f), 0.001f)
    }

    @Test fun hidingKeyboardDoesNotCollapseAFocusedOrNonemptyComposer() {
        val motion = ComposerKeyboardMotion(true)
        assertEquals(1f, motion.sample(true, 150, 300, 0, 1f), 0.001f)
    }

    @Test fun resizingAnOpenKeyboardDoesNotRestartComposerExpansion() {
        val motion = ComposerKeyboardMotion(false)
        motion.sample(true, 0, 0, 300, 0f)
        motion.sample(true, 0, 0, 300, 0f)
        motion.sample(true, 300, 0, 300, 0f)
        assertEquals(1f, motion.sample(true, 300, 300, 450, 1f), 0.001f)
        assertEquals(1f, motion.sample(true, 375, 300, 450, 1f), 0.001f)
    }
}
