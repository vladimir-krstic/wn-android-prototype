package dev.ipf.whitenoise.model

enum class VoiceCaptureFailure { TooShort, MicrophoneBusy, PermissionDenied, PermissionPermanentlyDenied, RecordingFailed }
enum class VoiceCaptureScenario(val developerLabel: String, val startFailure: VoiceCaptureFailure? = null, val finishFailure: VoiceCaptureFailure? = null) {
    Success("Record and review"), TooShort("Recording too short", finishFailure = VoiceCaptureFailure.TooShort),
    MicrophoneBusy("Microphone in use", VoiceCaptureFailure.MicrophoneBusy),
    PermissionDenied("Microphone access denied", VoiceCaptureFailure.PermissionDenied),
    PermissionPermanentlyDenied("Microphone access permanently denied", VoiceCaptureFailure.PermissionPermanentlyDenied),
    StartFailure("Recorder start failed", VoiceCaptureFailure.RecordingFailed),
    FinishFailure("Recorder finalization failed", finishFailure = VoiceCaptureFailure.RecordingFailed),
}
object VoiceCapture {
    const val maximumTenths = 3_000
    const val cancelDistanceDp = 120f
    const val lockDistanceDp = 80f
    fun drag(state: ComposerVoiceState, requestId: Long, towardStartDp: Float, upwardDp: Float): ComposerVoiceState {
        val current = (state as? ComposerVoiceState.Recording)?.takeIf { it.requestId == requestId && !it.locked } ?: return state
        if (!towardStartDp.isFinite() || !upwardDp.isFinite()) return state
        val cancel = towardStartDp > cancelDistanceDp
        return current.copy(willCancel = cancel, locked = !cancel && upwardDp > lockDistanceDp)
    }
    fun lock(state: ComposerVoiceState, requestId: Long): ComposerVoiceState = (state as? ComposerVoiceState.Recording)
        ?.takeIf { it.requestId == requestId }?.copy(locked = true, willCancel = false) ?: state
    fun release(state: ComposerVoiceState, requestId: Long): ComposerVoiceState {
        val current = (state as? ComposerVoiceState.Recording)?.takeIf { it.requestId == requestId } ?: return state
        return when { current.locked -> current; current.willCancel -> ComposerVoiceState.Idle; else -> ComposerVoiceReducer.stop(current) }
    }
    fun cancel(state: ComposerVoiceState, requestId: Long): ComposerVoiceState = if ((state as? ComposerVoiceState.Recording)?.requestId == requestId)
        ComposerVoiceState.Idle else state
}
