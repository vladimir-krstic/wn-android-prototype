package dev.ipf.whitenoise.model

/** Same stable order as the prototype timeline; production reconnects its authoritative order key. */
data class SpeechArrivalOrder(val day: Int, val minute: Int, val id: String) : Comparable<SpeechArrivalOrder> {
    override fun compareTo(other: SpeechArrivalOrder): Int = compareValuesBy(this, other, SpeechArrivalOrder::day, SpeechArrivalOrder::minute, SpeechArrivalOrder::id)
}
data class SpeechArrivalCursor(val anchorId: String? = null, val order: SpeechArrivalOrder? = null, val knownIds: Set<String> = emptySet()) {
    fun after(chat: Chat): List<ChatTimelineEntry> {
        val entries = ConversationProjection.orderedEntries(chat)
        val index = entries.indexOfFirst { it.id == anchorId }
        return (when {
            index >= 0 -> entries.drop(index + 1)
            anchorId == null -> entries
            order == null -> emptyList()
            else -> entries.filter { SpeechArrivalOrder(it.dayOrdinal, it.minuteOfDay, it.id) > order }
        }).filter { it.id !in knownIds }
    }
    fun advance(chat: Chat): SpeechArrivalCursor {
        val entries = ConversationProjection.orderedEntries(chat)
        val tail = entries.lastOrNull()
        val latest = tail?.let { SpeechArrivalOrder(it.dayOrdinal, it.minuteOfDay, it.id) }
        val known = knownIds + entries.map { it.id }
        return if (latest != null && (order == null || latest >= order)) SpeechArrivalCursor(tail.id, latest, known) else copy(knownIds = known)
    }
    companion object { fun capture(chat: Chat): SpeechArrivalCursor = SpeechArrivalCursor().advance(chat) }
}

object SpeechAutoRead {
    const val maximumMessages = 50
    /** Capture entryUnreadIds before visible-reading callbacks retire the chat's watermark. */
    fun backlog(chat: Chat, entryUnreadIds: Set<String>, claimedIds: Set<String> = emptySet()): List<SpeechItem> {
        val entries = ConversationProjection.orderedEntries(chat)
        val start = entries.indexOfFirst { it.id in entryUnreadIds }
        if (start < 0) return emptyList()
        return speakable(entries.drop(start).take(maximumMessages * 2), claimedIds)
    }
    fun arrivals(chat: Chat, cursor: SpeechArrivalCursor, claimedIds: Set<String> = emptySet()): List<SpeechItem> =
        speakable(cursor.after(chat).take(maximumMessages * 2), claimedIds)
    private fun speakable(entries: List<ChatTimelineEntry>, claimed: Set<String>): List<SpeechItem> = entries
        .filterIsInstance<ChatTimelineEntry.Message>().map { it.message }
        .filter { it.id !in claimed && SpeechOwnership.eligible(it) }.distinctBy { it.id }
        .take(maximumMessages).map { SpeechItem(it.id, it.text) }
    fun mayResume(owner: SpeechOwner, activeProfileId: String?, foreground: Boolean, locked: Boolean,
        capturedManualGeneration: Long, currentManualGeneration: Long, activeSession: SpeechSession?): Boolean =
        owner.profileId == activeProfileId && foreground && !locked && capturedManualGeneration == currentManualGeneration &&
            (activeSession == null || activeSession.phase in setOf(SpeechPhase.Completed, SpeechPhase.Unavailable))
}

enum class SpeechLifecyclePhase { Foreground, Background, Locked, Failed, Ended }
enum class SpeechControlAction { Pause, Resume, Stop, Source }
data class SpeechControlCommand(val profileId: String, val sessionId: Long, val action: SpeechControlAction)

/** App-owned developer example only; this state never creates Android notifications or services. */
data class SpeechBackgroundExample(
    val profileId: String,
    val sessionId: Long,
    val phase: SpeechLifecyclePhase = SpeechLifecyclePhase.Foreground,
    val paused: Boolean = false,
    val notificationVisible: Boolean = false,
    val lockDeadlineMillis: Long? = null,
    val sourceRequested: Boolean = false,
) {
    val active: Boolean get() = phase == SpeechLifecyclePhase.Foreground || phase == SpeechLifecyclePhase.Background
    fun notificationStarted(success: Boolean): SpeechBackgroundExample = if (!active) this else
        if (success) copy(notificationVisible = true) else copy(phase = SpeechLifecyclePhase.Failed, notificationVisible = false, lockDeadlineMillis = null)
    fun background(nowMillis: Long, lockDelayMillis: Long?): SpeechBackgroundExample {
        if (!active) return this
        return copy(phase = SpeechLifecyclePhase.Background, lockDeadlineMillis = lockDelayMillis?.let { nowMillis + it.coerceAtLeast(0) }).tick(nowMillis)
    }
    fun foreground(nowMillis: Long): SpeechBackgroundExample {
        val timed = tick(nowMillis)
        return if (!timed.active) timed else timed.copy(phase = SpeechLifecyclePhase.Foreground, lockDeadlineMillis = null)
    }
    fun tick(nowMillis: Long): SpeechBackgroundExample = if (phase == SpeechLifecyclePhase.Background && lockDeadlineMillis?.let { nowMillis >= it } == true)
        copy(phase = SpeechLifecyclePhase.Locked, paused = false, notificationVisible = false, lockDeadlineMillis = null, sourceRequested = false) else this
    fun profileChanged(activeProfileId: String?): SpeechBackgroundExample = if (profileId == activeProfileId || !active) this else
        copy(phase = SpeechLifecyclePhase.Ended, paused = false, notificationVisible = false, lockDeadlineMillis = null, sourceRequested = false)
    fun command(command: SpeechControlCommand): SpeechBackgroundExample {
        if (!active || command.profileId != profileId || command.sessionId != sessionId) return this
        return when (command.action) {
            SpeechControlAction.Pause -> copy(paused = true)
            SpeechControlAction.Resume -> copy(paused = false)
            SpeechControlAction.Stop -> copy(phase = SpeechLifecyclePhase.Ended, paused = false, notificationVisible = false, lockDeadlineMillis = null, sourceRequested = false)
            SpeechControlAction.Source -> copy(sourceRequested = true)
        }
    }
}
