package dev.ipf.whitenoise.model

import org.junit.Assert.*
import org.junit.Test

class VoiceCaptureTest {
    private fun recording(id: Long = 7) = ComposerVoiceReducer.start(ComposerVoiceState.Idle, requestId = id)
    @Test fun logicalStartDragCancelsInLtrAndRtlAfterUiTranslation() {
        val state = VoiceCapture.drag(recording(), 7, 121f, 0f) as ComposerVoiceState.Recording
        assertTrue(state.willCancel); assertFalse(state.locked); assertEquals(ComposerVoiceState.Idle, VoiceCapture.release(state, 7))
    }
    @Test fun upwardDragLocksAndReleaseCannotFinalizeIt() {
        val state = VoiceCapture.drag(recording(), 7, 0f, 81f) as ComposerVoiceState.Recording
        assertTrue(state.locked); assertFalse(state.willCancel); assertEquals(state, VoiceCapture.release(state, 7))
        assertEquals(ComposerVoiceState.Idle, VoiceCapture.cancel(state, 7))
    }
    @Test fun cancelDirectionWinsWhenBothThresholdsAreCrossed() {
        val state = VoiceCapture.drag(recording(), 7, 140f, 100f) as ComposerVoiceState.Recording
        assertTrue(state.willCancel); assertFalse(state.locked)
    }
    @Test fun ordinaryHoldReleaseMovesIntoExistingReview() {
        val state = VoiceCapture.release(recording(), 7)
        assertTrue(state is ComposerVoiceState.Review); assertEquals(1, (state as ComposerVoiceState.Review).durationSeconds)
    }
    @Test fun accessibleTapStartsLockedAndRequiresExplicitStop() {
        val state = ComposerVoiceReducer.start(ComposerVoiceState.Idle, locked = true, requestId = 9) as ComposerVoiceState.Recording
        assertEquals(state, VoiceCapture.release(state, 9)); assertTrue(ComposerVoiceReducer.stop(state) is ComposerVoiceState.Review)
    }
    @Test fun staleReleaseDragLockAndCancelCannotAffectReplacement() {
        val state = recording(8)
        assertEquals(state, VoiceCapture.release(state, 7)); assertEquals(state, VoiceCapture.drag(state, 7, 200f, 0f))
        assertEquals(state, VoiceCapture.lock(state, 7)); assertEquals(state, VoiceCapture.cancel(state, 7))
    }
    @Test fun malformedDragValuesAreIgnored() {
        val state = recording(); assertEquals(state, VoiceCapture.drag(state, 7, Float.NaN, 0f)); assertEquals(state, VoiceCapture.drag(state, 7, 0f, Float.POSITIVE_INFINITY))
    }
    @Test fun recordingHasProductionFiveMinuteSafetyCap() {
        var state: ComposerVoiceState = ComposerVoiceState.Recording(VoiceCapture.maximumTenths - 1)
        repeat(10) { state = ComposerVoiceReducer.tick(state) }
        assertEquals(VoiceCapture.maximumTenths, (state as ComposerVoiceState.Recording).elapsedTenths)
    }
    @Test fun scenariosSeparateStartAndFinalizationFailures() {
        assertEquals(VoiceCaptureFailure.PermissionDenied, VoiceCaptureScenario.PermissionDenied.startFailure)
        assertEquals(VoiceCaptureFailure.TooShort, VoiceCaptureScenario.TooShort.finishFailure)
        assertNull(VoiceCaptureScenario.Success.startFailure); assertNull(VoiceCaptureScenario.Success.finishFailure)
    }
}
