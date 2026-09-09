package dev.ipf.whitenoise.model

/** Coordinates a focus transition with the IME without starting a second keyboard animation. */
internal class ComposerKeyboardMotion(initiallyEditing: Boolean) {
    private var editing = initiallyEditing
    private var followsKeyboard = false
    private var startFraction = 0f
    private var startProgress = if (editing) 1f else 0f
    var progress = startProgress
        private set

    fun sample(editing: Boolean, current: Int, source: Int, target: Int, fallback: Float): Float {
        if (editing != this.editing) {
            this.editing = editing
            followsKeyboard = false
            startProgress = progress
            return progress
        }
        val destination = if (editing) 1f else 0f
        // Switching keyboard layouts can start another inset animation while
        // focus is unchanged. A settled composer must retain its editing state.
        if (progress == destination) return progress
        val moving = source != target
        val matchingDirection = moving && (target > source) == editing
        if (matchingDirection) {
            val fraction = ((current - source).toFloat() / (target - source)).coerceIn(0f, 1f)
            if (!followsKeyboard) {
                followsKeyboard = true
                startFraction = fraction
                startProgress = progress
            }
            val remaining = if (startFraction >= 1f) 1f else
                ((fraction - startFraction) / (1f - startFraction)).coerceIn(0f, 1f)
            progress = startProgress + (destination - startProgress) * remaining
        } else if (followsKeyboard) {
            // Retain the endpoint when Android clears animation source/target. The
            // fallback timer may still be running and must not pull the layout back.
            progress = destination
        } else {
            progress = fallback.coerceIn(0f, 1f)
        }
        return progress
    }
}
