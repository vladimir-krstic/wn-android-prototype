package dev.ipf.whitenoise.model

enum class AgentConnector {
    Hermes,
    OpenClaw,
    OpenCode,
    Codex,
}

object AgentSetupPolicy {
    val connectors: List<AgentConnector> = AgentConnector.entries

    fun publicKeyOrNull(profile: Profile?): String? = profile?.publicKey
        ?.trim()
        ?.takeIf { it.startsWith("npub1") && it.length > 20 }
}

enum class AgentOperationPhase {
    Queued,
    Running,
    Succeeded,
    Failed,
    Cancelled,
    Unavailable,
}

data class AgentOperation(
    val name: String,
    val summary: String,
    val phase: AgentOperationPhase,
    val arguments: String? = null,
    val result: String? = null,
    val statusDetail: String? = null,
    val completedSteps: Int = 0,
    val totalSteps: Int? = null,
    val durationMillis: Long? = null,
) {
    val boundedCompletedSteps: Int
        get() = completedSteps.coerceIn(0, totalSteps?.coerceAtLeast(0) ?: 0)

    val progress: Float?
        get() = totalSteps?.takeIf { it > 0 }?.let { boundedCompletedSteps.toFloat() / it }

    val isInProgress: Boolean
        get() = phase == AgentOperationPhase.Queued || phase == AgentOperationPhase.Running

    val canExpand: Boolean
        get() = listOf(arguments, result, statusDetail).any { !it.isNullOrBlank() } || durationMillis != null
}

/** Local examples exercise ordinary agent presentation without creating transport or agent accounts. */
object AgentConversationExamples {
    const val IdPrefix = "agent-example-"

    fun add(chat: Chat, profile: Profile): Chat? {
        if (chat.membership != ChatMembership.Active) return null
        val authorId = (chat.kind as? ChatKind.Direct)?.personId
            ?: chat.members.firstOrNull { it.personId != profile.id }?.personId
            ?: return null
        val latest = chat.timeline.maxWithOrNull(
            compareBy<ChatTimelineEntry> { it.dayOrdinal }.thenBy { it.minuteOfDay },
        )
        val nextPosition = (latest?.minuteOfDay ?: -6) + 6
        val day = (latest?.dayOrdinal ?: 0) + nextPosition / 1_440
        val minute = nextPosition % 1_440
        val messages = examples(authorId, day, minute)
        val withoutEarlierExamples = chat.timeline.filterNot { it.id.startsWith(IdPrefix) }
        val author = profile.people.firstOrNull { it.id == authorId }?.displayName ?: chat.title
        return chat.copy(
            timeline = withoutEarlierExamples + messages.map(ChatTimelineEntry::Message),
            preview = messages.last().text,
            previewAuthor = author,
            timestamp = "Now",
        )
    }

    private fun examples(authorId: String, day: Int, firstMinute: Int): List<ChatMessage> {
        fun message(
            suffix: String,
            text: String,
            offset: Int,
            operation: AgentOperation? = null,
            streaming: Boolean = false,
        ) = ChatMessage(
            id = "$IdPrefix$suffix",
            authorId = authorId,
            dayOrdinal = day + (firstMinute + offset * 6) / 1_440,
            dayLabel = "Today",
            minuteOfDay = (firstMinute + offset * 6) % 1_440,
            timeLabel = "Now",
            text = text,
            deliveryState = if (streaming) MessageDeliveryState.Streaming else MessageDeliveryState.Sent,
            agentOperation = operation,
        )

        return listOf(
            message(
                suffix = "streaming",
                text = "I’m reviewing the project notes and checking the open questions…",
                offset = 0,
                streaming = true,
            ),
            message(
                suffix = "queued",
                text = "Waiting to inspect the project files",
                offset = 1,
                streaming = true,
                operation = AgentOperation(
                    name = "Inspect project files",
                    summary = "Waiting to inspect the project files",
                    phase = AgentOperationPhase.Queued,
                    arguments = "Folder: Project Notes\nInclude: Markdown and text files",
                ),
            ),
            message(
                suffix = "running",
                text = "Reviewing the project files",
                offset = 2,
                streaming = true,
                operation = AgentOperation(
                    name = "Review project files",
                    summary = "Reviewing the project files",
                    phase = AgentOperationPhase.Running,
                    completedSteps = 2,
                    totalSteps = 4,
                    arguments = "Folder: Project Notes\nQuestion: Which decisions are still open?",
                    statusDetail = "Reading the remaining documents",
                ),
            ),
            message(
                suffix = "succeeded",
                text = "Prepared the project summary",
                offset = 3,
                operation = AgentOperation(
                    name = "Prepare project summary",
                    summary = "Prepared the project summary",
                    phase = AgentOperationPhase.Succeeded,
                    completedSteps = 4,
                    totalSteps = 4,
                    result = "Three decisions are ready for review.",
                    durationMillis = 1_840,
                ),
            ),
            message(
                suffix = "failed",
                text = "Couldn’t read one project file",
                offset = 4,
                operation = AgentOperation(
                    name = "Read project brief",
                    summary = "Couldn’t read one project file",
                    phase = AgentOperationPhase.Failed,
                    statusDetail = "The file is no longer available.",
                    durationMillis = 620,
                ),
            ),
            message(
                suffix = "cancelled",
                text = "Project comparison was cancelled",
                offset = 5,
                operation = AgentOperation(
                    name = "Compare project drafts",
                    summary = "Project comparison was cancelled",
                    phase = AgentOperationPhase.Cancelled,
                    statusDetail = "No changes were made.",
                ),
            ),
            message(
                suffix = "unavailable",
                text = "Calendar access is unavailable",
                offset = 6,
                operation = AgentOperation(
                    name = "Check calendar",
                    summary = "Calendar access is unavailable",
                    phase = AgentOperationPhase.Unavailable,
                    statusDetail = "This agent does not have calendar access.",
                ),
            ),
        )
    }
}
