package dev.ipf.whitenoise.model

data class MessageRevision(val id: Long, val text: String, val timestampMillis: Long)
data class MessageEditHistory(val original: String, val originalTimestampMillis: Long, val revisions: List<MessageRevision>)
enum class MessageEditPhase { Pending, Failed }
enum class MessageEditFailure { SaveFailed, Unavailable, Interrupted }
enum class MessageEditScenario(val developerLabel: String) {
    Success("Edit succeeds"), SaveFails("Edit fails once"), Unavailable("Edit target unavailable once")
}
data class MessageEditAttempt(
    val id: Long, val profileId: String, val text: String, val baseText: String, val baseRevision: Int,
    val scenario: MessageEditScenario, val phase: MessageEditPhase = MessageEditPhase.Pending,
    val failure: MessageEditFailure? = null,
)

/** Accepted text stays in ChatMessage.text so every existing content projection agrees. */
object MessageEditing {
    fun eligible(message: ChatMessage, profileId: String) =
        message.authorId == profileId && !message.isDeleted && message.text.isNotBlank()
    fun canSave(message: ChatMessage, text: String) = text.isNotBlank() && text.trim() != message.text
    fun displayedText(message: ChatMessage): String =
        if (message.isDeleted) message.text else message.editAttempt?.takeIf { it.phase == MessageEditPhase.Pending }?.text ?: message.text
    fun accept(message: ChatMessage, attempt: MessageEditAttempt): ChatMessage {
        val originalTime = message.createdAtMillis?.takeIf { it > 0 } ?: GlobalSearchClock.timestamp(message)
        val history = message.editHistory ?: MessageEditHistory(message.text, originalTime, emptyList())
        val timestamp = maxOf(originalTime, history.revisions.lastOrNull()?.timestampMillis ?: originalTime) + 60_000
        return message.copy(text = attempt.text, editAttempt = null, editHistory = history.copy(
            revisions = history.revisions + MessageRevision(attempt.id, attempt.text, timestamp),
        ))
    }
}
